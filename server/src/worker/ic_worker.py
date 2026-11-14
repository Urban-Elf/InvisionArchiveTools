#  This file is part of Invision Archive Tools (IAT).
#
#  Copyright (C) 2025 Mark Fisher
#
#  This program is free software: you can redistribute it and/or modify
#  it under the terms of the GNU General Public License as published by
#  the Free Software Foundation, either version 3 of the License, or
#  (at your option) any later version.
#
#  This program is distributed in the hope that it will be useful,
#  but WITHOUT ANY WARRANTY; without even the implied warranty of
#  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
#  GNU General Public License for more details.
#
#  You should have received a copy of the GNU General Public License
#  along with this program. If not, see https://www.gnu.org/licenses/.

import json
import threading
import time
import traceback
import random
from pathlib import Path
from .. import main
from .. import proto_model
from .. import util
from .ic import *
from .. import file_tree
from .state.worker_state import *
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.action_chains import ActionChains
from selenium.common.exceptions import *
import undetected_chromedriver
import screeninfo

from ..shared_constants import WorkerType
from ..auth import load_password_file, load_debug_credentials

class RefreshSpec(serializable.JSONSerializable):
    def __init__(self, ic: IC, worker_type: WorkerType, refresh_url: str, userdata_from_posts: bool=False):
        """
        Spec to refresh an ICWorker session. 
        """
        self.ic = ic
        self.worker_type = worker_type
        # FIXME: refresh url should be replaced with a MessengerRefreshSpec class that saves the messenger ID instead
        self.refresh_url = refresh_url
        self.userdata_from_posts = userdata_from_posts

    def __serialize__(self):
        return {
            "ic": self.ic.__serialize__(),
            "worker_type": self.worker_type.name,
            "refresh_url": self.refresh_url,
            "userdata_from_posts": self.userdata_from_posts
        }
    
    @staticmethod
    def deserialize(path: Path):
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
            assert type(data) is dict, "RefreshSpec JSON must be a dictionary"
            ic = data.get("ic")
            _ic = get_ic_by_version(ic["version"])(ic["root_url"])
            worker_type = data.get("worker_type")
            _worker_type = WorkerType[worker_type]
            refresh_url = data.get("refresh_url", "RefreshUrlCorrupted")
            userdata_from_posts = data.get("userdata_from_posts", False)
            return RefreshSpec(ic=_ic, worker_type=_worker_type, refresh_url=refresh_url, userdata_from_posts=userdata_from_posts)
        return None

class ICWorker(threading.Thread):
    def __init__(self, worker_id: str, ic: IC, worker_type: WorkerType, output_dir: Path, credential_key=None):
        super().__init__(name=worker_id)
        self.worker_id = worker_id
        self.ic = ic
        self.worker_type = worker_type
        self.output_dir = output_dir
        self.credential_key = credential_key
        self.stored_r_spec: RefreshSpec = None
        self._r_spec: RefreshSpec = None
        self._refreshing = False
        self.driver = None
        self.actions = None
        self.shutdown_event = threading.Event()
        self.refresh_event = threading.Event()

    @classmethod
    def _load_from_spec(cls, worker_id: str, ic: IC, input_dir: Path):
        raise NotImplementedError("_load_from_spec must be implemented in worker subclasses")

    @staticmethod
    def load_refresh_spec(input_dir: Path):
        refresh_spec_path = input_dir / "refresh_spec.json"
        if not refresh_spec_path.exists():
            raise FileNotFoundError(f"Refresh spec not found in {input_dir}")
        return RefreshSpec.deserialize(refresh_spec_path)

    @classmethod
    def load_from_spec(cls, worker_id: str, ic: IC, r_spec: RefreshSpec, input_dir: Path, refreshing: bool=False, credential_key=None):
        # Construct from spec
        worker = cls._load_from_spec(worker_id, ic, input_dir)
        if not isinstance(worker, ICWorker):
            raise TypeError(f"{cls.__name__}._load_from_spec must return an instance of ICWorker, not {type(worker)}")
        worker._r_spec = r_spec
        worker.stored_r_spec = r_spec
        worker._refreshing = refreshing
        worker.credential_key = credential_key
        return worker

    def get_stored_refresh_spec(self) -> RefreshSpec:
        return self.stored_r_spec

    def load_credentials(self):
        """Returns a tuple of the form (username, password). Returns None if load fails."""

        #if main.DEBUG:
        #    try:
        #        return load_debug_credentials()
        #    except FileNotFoundError:
        #        util.log(util.LogLevel.DEBUG, "Auth file not found, resorting to manual login.")
        #        return None

        if self.credential_key is not None:
            try:
                path = file_tree.ROOT_PATH / "auth.bin"
                credential = load_password_file(path, self.credential_key)
                util.log(util.LogLevel.INFO, "Credential decrypted successfully.")
                return credential
            except FileNotFoundError:
                util.log(util.LogLevel.WARNING, "No credential file found; proceeding with manual login")
            except:
                util.log(util.LogLevel.ERROR, "Failed to load credentials", error_trace=True)
        
        util.log(util.LogLevel.INFO, "No credential key available.")

        return None


    def driver_await(self, condition: EC, time=30):
        if time == 30 and main.DEBUG:
            time=15
        util.log(util.LogLevel.INFO, f"driver_await (timeout={time}s)" + (" [DEBUG]" if main.DEBUG else ""))
        WebDriverWait(self.driver, time).until(condition)

    def should_shutdown(self) -> bool:
        return self.shutdown_event.is_set()

    def execute(self):
        raise NotImplementedError()

    def run(self):
        try:
            # Import IC_ChromeDriver here to avoid circular imports
            from .ic_chromedriver import IC_ChromeDriver
            # Set initial state (only on non-refresh)
            if not self._refreshing:
                self.set_state(SharedState.INITIALIZATION)
            # Create chromedriver
            util.log(util.LogLevel.INFO, "Starting chromedriver...")
            try:
                # Attempt to start the browser
                self.driver: IC_ChromeDriver = create_chromedriver(self)
                # Shutdown if a checked failure occurred in initialization
                if not self.driver.is_initialized():
                    self.shutdown()
                    return
                # Create action chains
                self.actions = ActionChains(self.driver)
            except:
                proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.CHROMEDRIVER_ERROR)
                                         .add_data("stacktrace", traceback.format_exc()))
                util.log(util.LogLevel.FATAL, "Failed to start chromedriver: ", error_trace=True)
                # Must be assigned before shutdown in this context
                # Shutdown worker
                self.shutdown()
                return

            # Notify of chromedriver start
            util.log(util.LogLevel.INFO, "Chromedriver started successfully.")
            
            # Authenticate
            while not self.should_shutdown():
                if main.DEBUG and util.strip_url(self.ic.root_url) in util.strip_url(self.driver.current_url):
                    break # Should be logged in
                if not util.strip_url(self.ic.auth()) in util.strip_url(self.driver.current_url):
                    self.driver.get(self.ic.auth())

                # Write packet
                proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.CHROMEDRIVER_STARTED))

                # load credentials
                credentials = self.load_credentials()

                # Auto login (when configured)
                if credentials is not None:
                    util.log(util.LogLevel.INFO, "Performing authentication via loaded credential...")

                    self.driver_await(EC.all_of(
                        EC.presence_of_element_located((By.ID, "auth")),
                        EC.presence_of_element_located((By.NAME, "password")),
                        EC.presence_of_element_located((By.NAME, "_processLogin"))))

                    auth = self.driver.find_element(By.ID, "auth")
                    password = self.driver.find_element(By.NAME, "password")

                    time.sleep(0.8 + (random.random() * 1))
                    auth.send_keys(credentials[0])
                    time.sleep(1 + (random.random() * 1))
                    password.send_keys(credentials[1])

                    login = self.driver.find_element(By.NAME, "_processLogin")

                    self.driver_await(lambda d: login.is_displayed())

                    login.click()

                    time.sleep(2) # Wait for login to process
                else:
                    util.log(util.LogLevel.INFO, "Performing manual authentication...")
                    self.set_state(SharedState.AUTH_REQUIRED)

                # Validate session
                self.set_state(SharedState.VALIDATING_AUTH)
                try:
                    self.driver_await(EC.presence_of_element_located((By.CLASS_NAME, "elMobileDrawer__user-panel")), time=3)
                except TimeoutException:
                    self.set_state(SharedState.AUTH_INVALID)
                    continue
                # Valid session
                break

            if self.should_shutdown():
                return # May have arrived here by the loop above breaking (when shutdown flag is set)
            
            util.log(util.LogLevel.INFO, "Session valid, proceeding...")
            # Start worker
            self.execute()
        except BaseException:
            util.log(util.LogLevel.FATAL, "An internal error occurred: ", error_trace=True)
            if self.refresh_event.is_set():
                util.log(util.LogLevel.INFO, "Refresh event set, suppressing error state changes (not relevant).")
            else:
                if (traceback.format_exc().__contains__("InvalidSessionIdException")):
                    self.set_state(SharedState.SESSION_INTERRUPTED)
                else:
                    self.set_state(SharedState.INTERNAL_EXCEPTION)

    def set_state(self, state: ICWorkerState):
        # Prevents cases such as tuples being passed in by trailing commas in state definitions
        if not isinstance(state, ICWorkerState):
            raise TypeError("state must be an instance of ICWorkerState, not " + str(type(state)))
        self.state = state
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.STATE_CHANGED)
            .add_data("state", state))
        self.client_object = None
        if not state.is_progressive():
            while self.client_object == None and not self.should_shutdown():
                time.sleep(0.1)
        return self.client_object

    def on_shutdown(self):
        pass

    def shutdown(self, callback: bool=True, refreshing=False):
        # If refreshing, lock error state changes (ChromeDriver often throws when shutdown during activity) 
        if refreshing:
            self.refresh_event.set()
        # Set shutdown event to signal the thread to stop
        self.shutdown_event.set()
        if self.driver != None and self.driver.is_initialized():
            self.driver.quit()
        if callback:
            self.on_shutdown()

    def set_client_object(self, client_object):
        self.client_object = client_object

    def update_progress(self, progress: float):
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.PROGRESS_UPDATE)
            .add_data("progress", progress))

class IC4Worker(ICWorker):
    def __init__(self, worker_id, ic, worker_type: WorkerType, output_dir, credential_key=None):
        super().__init__(worker_id, ic, worker_type, output_dir, credential_key)

class IC5Worker(ICWorker):
    def __init__(self, worker_id, ic, worker_type: WorkerType, output_dir, credential_key=None):
        super().__init__(worker_id, ic, worker_type, output_dir, credential_key)

from .ic_chromedriver import IC_ChromeDriver
import shutil
import os
import sys

def find_chrome_binary():
    linux_paths = [
        "/opt/google/chrome/google-chrome",
        "/usr/bin/google-chrome",
        "/usr/bin/chromium-browser",
        "/usr/bin/chromium",
    ]

    if sys.platform == "linux":
        for path in linux_paths:
            if os.path.isfile(path) and os.access(path, os.X_OK):
                return path

        # Try PATH as fallback
        return shutil.which("google-chrome") or shutil.which("chrome") or shutil.which("chromium")

    return None

def create_chromedriver(ic_worker: ICWorker) -> IC_ChromeDriver:
    options = undetected_chromedriver.ChromeOptions()
    options.add_argument("--disable-infobars")

    chrome_path = find_chrome_binary()
    if chrome_path:
        options.binary_location = chrome_path

    driver = IC_ChromeDriver(ic_worker=ic_worker, options=options)

    if driver.is_initialized():
        # Get screen width and height
        screen = screeninfo.get_monitors()[0]  # primary monitor
        width = screen.width // 2.4
        width = min(767, width) # Clamp width to IC RWD width for mobile (essential for workers here)
        height = screen.height
        driver.set_window_rect(0, 0, width, height)
    return driver