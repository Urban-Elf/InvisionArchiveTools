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

import sys
import threading
import time
import psutil
from .. import proto_model
from .. import util
from .ic import *
from .state.worker_state import *
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.webdriver.support.ui import WebDriverWait
from selenium.common.exceptions import *

class ICWorkerConsumer:
    def accept(self, obj):
        pass

class ICWorker(threading.Thread):
    def __init__(self, worker_id: str, ic: IC, result_consumer: ICWorkerConsumer):
        super().__init__(name=worker_id)
        self.worker_id = worker_id
        self.ic = ic
        self.result_consumer = result_consumer
        self.shutdown_event = threading.Event()

    def execute(self):
        raise NotImplementedError()

    def authenticate(self):
        raise NotImplementedError()
    
    def driver_await(self, condition: EC, time=15):
        WebDriverWait(self.driver, time).until(condition)

    def run(self):
        try:
            self.set_state(SharedState.INITIALIZATION)
            # Create chromedriver
            util.log(util.LogLevel.INFO, "Starting chromedriver...")
            try:
                # Attempt to start the browser
                self.driver = util.create_chromedriver()
            except:
                proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.CHROMEDRIVER_ERROR))
                util.log(util.LogLevel.FATAL, "Failed to start chromedriver: ", error_trace=True)
                util.log(util.LogLevel.INFO, "Please ensure your Chrome version matches the required ChromeDriver version.")
                self.driver.quit()
                return

            # Authenticate
            while not self.shutdown_event.is_set():
                if not self.driver.current_url == self.ic.auth():
                    self.driver.get(self.ic.auth())
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
            util.log(util.LogLevel.INFO, "Session validated, proceeding...")
            # Start worker
            self.execute()
        except BaseException:
            util.log(util.LogLevel.FATAL, "An internal error occurred: ", error_trace=True)

    def set_state(self, state: ICWorkerState):
        self.state = state
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.STATE_CHANGED)
            .add_data("state", state))
        self.client_object = None
        if not state.is_progressive():
            while self.client_object == None and not self.shutdown_event.is_set():
                time.sleep(0.1)
        return self.client_object

    def shutdown(self):
        self.shutdown_event.set()
        self.driver.quit()
        self.driver.close()

    def set_client_object(self, client_object):
        self.client_object = client_object

    def update_progress(self, progress: float):
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.PROGRESS_UPDATE)
            .add_data("progress", progress))

class IC4Worker(ICWorker):
    def __init__(self, worker_id, ic, result_consumer):
        super().__init__(worker_id, ic, result_consumer)

class IC5Worker(ICWorker):
    def __init__(self, worker_id, ic, result_consumer):
        super().__init__(worker_id, ic, result_consumer)
