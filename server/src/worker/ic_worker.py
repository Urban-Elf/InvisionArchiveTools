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

import threading
import time
import traceback
from ..file_tree import ROOT_PATH
from .. import main
from .. import proto_model
from .. import util
from .ic import *
from .state.worker_state import *
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
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

class ICWorker(threading.Thread):
    def __init__(self, worker_id: str, ic: IC):
        super().__init__(name=worker_id)
        self.worker_id = worker_id
        self.ic = ic
        self.driver = None
        self.shutdown_event = threading.Event()

    def execute(self):
        raise NotImplementedError()

    def authenticate(self):
        raise NotImplementedError()
    
    def driver_await(self, condition: EC, time=15):
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
            except:
                proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.CHROMEDRIVER_ERROR)
                                         .add_data("stacktrace", traceback.format_exc()))
                util.log(util.LogLevel.FATAL, "Failed to start chromedriver: ", error_trace=True)
                # Must be assigned before shutdown in this context
                # Shutdown worker
                self.shutdown()
                return

            # Authenticate
            while not self.shutdown_event.is_set():
                if not util.strip_url(self.ic.auth()) in util.strip_url(self.driver.current_url):
                    self.driver.get(self.ic.auth())

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

                        auth.send_keys(creds["username"])
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
                self.set_state(SharedState.VALIDATING_SESSION)
                try:
                    self.driver_await(EC.presence_of_element_located((By.CLASS_NAME, "elMobileDrawer__user-panel")), time=3)
                except TimeoutException:
                    self.set_state(SharedState.SESSION_INVALID)
                    continue
                # Valid session
                break
            util.log(util.LogLevel.INFO, "Session valid, proceeding...")
            # Start worker
            self.execute()
        except BaseException:
            util.log(util.LogLevel.FATAL, "An internal error occurred: ", error_trace=True)
            self.set_state(SharedState.INTERNAL_EXCEPTION)

    def set_state(self, state: ICWorkerState):
        self.state = state
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.STATE_CHANGED)
            .add_data("state", state))
        self.client_object = None
        if not state.is_progressive():
            while self.client_object == None and not self.shutdown_event.is_set():
                time.sleep(0.1)
        return self.client_object

    def on_shutdown():
        pass

    def shutdown(self):
        self.shutdown_event.set()
        if (self.driver is not None):
            self.driver.quit()
            self.driver.close()
        self.on_shutdown()

    def set_client_object(self, client_object):
        self.client_object = client_object

    def update_progress(self, progress: float):
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.PROGRESS_UPDATE)
            .add_data("progress", progress))

class IC4Worker(ICWorker):
    def __init__(self, worker_id, ic):
        super().__init__(worker_id, ic)

class IC5Worker(ICWorker):
    def __init__(self, worker_id, ic):
        super().__init__(worker_id, ic)

from .ic_chromedriver import IC_ChromeDriver

def create_chromedriver(ic_worker: ICWorker) -> IC_ChromeDriver:
    options = undetected_chromedriver.ChromeOptions()
    options.add_argument("--disable-infobars")

    # TODO: Use this to check whether the user to installed chrome: print(undetected_chromedriver.find_chrome_executable())
    driver = IC_ChromeDriver(ic_worker=ic_worker, options=options)

    # Get screen width and height
    screen = screeninfo.get_monitors()[0]  # primary monitor
    width = screen.width // 2.4
    height = screen.height
    driver.set_window_rect(0, 0, width, height)
    return driver