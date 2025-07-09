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
import traceback
from enum import Enum, auto
import proto_model
import undetected_chromedriver
import screeninfo
#from selenium.webdriver.chrome.options import Options
#from webdriver_manager.chrome import ChromeDriverManager

def validate_json_data(json_data: str):
    if not isinstance(json_data, str):
        raise TypeError("json_data must be of type string")
    if len(json_data.strip()) == 0:
        raise TypeError("json_data cannot be empty")

def create_chromedriver():
    options = undetected_chromedriver.ChromeOptions()
    options.add_argument("--disable-infobars")

    # TODO: Use this to check whether the user to installed chrome: print(undetected_chromedriver.find_chrome_executable())
    driver = undetected_chromedriver.Chrome(options=options)

    # Get screen width and height
    screen = screeninfo.get_monitors()[0]  # primary monitor
    width = screen.width // 2.4
    height = screen.height
    driver.set_window_rect(0, 0, width, height)
    return driver

def write_packet(packet: proto_model.ServerPacket):
    sys.stdout.write(str(packet) + "\n")
    sys.stdout.flush()

class LogLevel(Enum):
    INFO = auto()
    WARNING = auto()
    ERROR = auto()
    FATAL = auto()

def log(level: LogLevel, message: str, error_trace: bool=False):
    error_string = ""
    if error_trace:
        error_string = traceback.format_exc()
    sys.stderr.write("[" + threading.current_thread().getName() + "] " + level.name + " - " + message + error_string + "\n")
    sys.stderr.flush()