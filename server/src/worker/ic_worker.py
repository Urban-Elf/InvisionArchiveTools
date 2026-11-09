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
from ..file_tree import ROOT_PATH
from pathlib import Path
from .. import main
from .. import proto_model
from .. import util
from ..content.content import Meta
from .ic import *
from .state.worker_state import *
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.common.action_chains import ActionChains
from selenium.common.exceptions import *
import undetected_chromedriver
import screeninfo

def load_credentials():
    creds_file = ROOT_PATH / 'auth.txt'
    if not creds_file.is_file():
        raise FileNotFoundError()

    creds = {}
    with creds_file.open() as f:
        for line in f:
            line = line.strip()
            if line.startswith("username: "):
                creds['username'] = line[len("username: "):].strip()
            elif line.startswith("password: "):
                creds['password'] = line[len("password: "):].strip()

    return creds

class RefreshSpec(serializable.JSONSerializable):
    def __init__(self, refresh_url: str, userdata_from_posts: bool=False):
        """
        Spec to refresh an ICWorker session. 
        """
        self.refresh_url = refresh_url
        self.userdata_from_posts = userdata_from_posts

    def __serialize__(self):
        return {
            "refresh_url": self.refresh_url,
            "userdata_from_posts": self.userdata_from_posts
        }
    
    @staticmethod
    def deserialize(path: Path):
        with path.open(encoding="utf-8") as f:
            data = json.load(f)
            assert type(data) is dict, "RefreshSpec JSON must be a dictionary"
            refresh_url = data.get("refresh_url", "RefreshUrlCorrupted")
            userdata_from_posts = data.get("userdata_from_posts", False)
            return RefreshSpec(refresh_url=refresh_url, userdata_from_posts=userdata_from_posts)
        return None

class ICWorker(threading.Thread):
    def __init__(self, worker_id: str, ic: IC, output_dir: Path):
        super().__init__(name=worker_id)
        self.worker_id = worker_id
        self.ic = ic
        self.output_dir = output_dir
        self.stored_r_spec: RefreshSpec = None
        self._r_spec: RefreshSpec = None
        self.driver = None
        self.actions = None
        self.shutdown_event = threading.Event()

    @classmethod
    def load_from_spec(cls, worker_id: str, ic: IC, r_spec: RefreshSpec, input_dir: Path):
        worker = cls(worker_id, ic, input_dir)
        worker._r_spec = r_spec
        worker.stored_r_spec = r_spec
        return worker

    @classmethod
    def load_from_input_dir(cls, worker_id: str, ic: IC, input_dir: Path):
        refresh_spec_path = input_dir / "refresh_spec.json"
        if not refresh_spec_path.exists():
            raise FileNotFoundError(f"Refresh spec not found in {input_dir}")
        refresh_spec = RefreshSpec.deserialize(refresh_spec_path)
        # Construct from spec
        return cls.load_from_spec(worker_id, ic, refresh_spec, input_dir)

    def execute(self):
        raise NotImplementedError()

    def authenticate(self):
        raise NotImplementedError()
    
    def get_stored_refresh_spec(self) -> RefreshSpec:
        return self.stored_r_spec

    def driver_await(self, condition: EC, time=30):
        if time == 30 and main.DEBUG:
            time=15
        util.log(util.LogLevel.INFO, f"driver_await (timeout={time}s)" + (" [DEBUG]" if main.DEBUG else ""))
        WebDriverWait(self.driver, time).until(condition)

    def run(self):
        try:
            # Import IC_ChromeDriver here to avoid circular imports
            from .ic_chromedriver import IC_ChromeDriver
            # Set initial state
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
            while not self.shutdown_event.is_set():
                if main.DEBUG and util.strip_url(self.ic.root_url) in util.strip_url(self.driver.current_url):
                    break # Should be logged in
                if not util.strip_url(self.ic.auth()) in util.strip_url(self.driver.current_url):
                    self.driver.get(self.ic.auth())

                # Write packet
                proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.CHROMEDRIVER_STARTED))

                # Auto login (for testing)
                if main.DEBUG:
                    try:
                        creds = load_credentials()

                        self.driver_await(EC.all_of(
                            EC.presence_of_element_located((By.ID, "auth")),
                            EC.presence_of_element_located((By.NAME, "password")),
                            EC.presence_of_element_located((By.NAME, "_processLogin"))))

                        auth = self.driver.find_element(By.ID, "auth")
                        password = self.driver.find_element(By.NAME, "password")

                        time.sleep(1)
                        auth.send_keys(creds["username"])
                        time.sleep(1.4)
                        password.send_keys(creds["password"])

                        login = self.driver.find_element(By.NAME, "_processLogin")

                        self.driver_await(lambda d: login.is_displayed())

                        login.click()

                        time.sleep(2) # Wait for login to process
                    except FileNotFoundError:
                        util.log(util.LogLevel.WARNING, "Auth file not found, resorting to manual login.")
                        self.set_state(SharedState.AUTH_REQUIRED)
                else:
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
            util.log(util.LogLevel.INFO, "Session valid, proceeding...")
            # Start worker
            self.execute()
        except BaseException:
            util.log(util.LogLevel.FATAL, "An internal error occurred: ", error_trace=True)
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
            while self.client_object == None and not self.shutdown_event.is_set():
                time.sleep(0.1)
        return self.client_object

    def on_shutdown(self):
        pass

    def shutdown(self, callback: bool=True):
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
    def __init__(self, worker_id, ic, output_dir):
        super().__init__(worker_id, ic, output_dir)

class IC5Worker(ICWorker):
    def __init__(self, worker_id, ic, output_dir):
        super().__init__(worker_id, ic, output_dir)

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
        width = min(767, width) # Clamp width to IC RWD width for mobile
        height = screen.height
        driver.set_window_rect(0, 0, width, height)
    return driver