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
import undetected_chromedriver

from ..shared_constants import ButtonCallbackSA
from .ic_worker import ICWorker
from .state.worker_state import *
from .. import util
from selenium.common.exceptions import WebDriverException
from urllib.error import URLError
import re

class IC_ChromeDriver(undetected_chromedriver.Chrome):
    def __init__(self, ic_worker: ICWorker, options=None, user_data_dir=None, driver_executable_path=None, browser_executable_path=None, port=0, enable_cdp_events=False, desired_capabilities=None, advanced_elements=False, keep_alive=True, log_level=0, headless=False, version_main=None, patcher_force_close=False, suppress_welcome=True, use_subprocess=True, debug=False, no_sandbox=True, user_multi_procs = False, **kw):
        self.initialized = False
        self.network_err_state = ICWorkerState(note="Please check your network connection.",
                                                hint="",
                                                button_configs=[
                                                    ButtonConfig("Retry"),
                                                    ButtonConfig("Close", shared_action=ButtonCallbackSA.TERMINATE)])
        self.connecting_state = ICWorkerState(note="Reconnecting...", progressive=True)
        self.shutdown_event = threading.Event()
        
        while not self.shutdown_event.is_set():
            try: 
                super().__init__(options, user_data_dir, driver_executable_path, browser_executable_path, port, enable_cdp_events, desired_capabilities, advanced_elements, keep_alive, log_level, headless, version_main, patcher_force_close, suppress_welcome, use_subprocess, debug, no_sandbox, user_multi_procs, **kw)
                self.initialized = True
                break
            except URLError:
                ic_worker.set_state(self.network_err_state)
                # set_state is blocking
                ic_worker.set_state(self.connecting_state)
                time.sleep(1)
        self.ic_worker = ic_worker

    def is_initialized(self):
        return self.initialized

    def get(self, url):
        while not self.ic_worker.shutdown_event.is_set():
            try:
                # Log
                util.log(util.LogLevel.INFO, "current_url => " + url)
                # super invocation
                super().get(url)
                # Success
                break
            except WebDriverException as e:
                msg = str(e)
                if not "net::" in msg:
                    raise
                # Extract error code, e.g., ERR_ADDRESS_UNREACHABLE
                match = re.search(r'net::([A-Z_]+)', msg)
                if match:
                    net_error = match.group(1)
                else:
                    net_error = "ERR_UNKNOWN"
                    self.network_err_state.hint = net_error
                self.ic_worker.set_state(self.network_err_state)
                # "Retry" clicked
                self.ic_worker.set_state(self.connecting_state)
                time.sleep(1)

    def quit(self):
        self.shutdown_event.set()
        super().quit()
