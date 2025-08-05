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
import sys
import re
import threading
import traceback
from enum import Enum, auto

DEBUG = False

TAG_REGEX = r'<[^>]+>'
URL_REGEX = r'^https?://(www\.)?'

def strip_url(url: str):
    return re.sub(URL_REGEX, '', url)

def strip_tags(text: str):
    """
    Strips HTML tags from the given text.
    """
    return re.sub(TAG_REGEX, '', text)

def validate_json_data(json_data: str):
    if not isinstance(json_data, str):
        raise TypeError("json_data must be of type string")
    if len(json_data.strip()) == 0:
        raise TypeError("json_data cannot be empty")

class LogLevel(Enum):
    INFO = auto()
    WARNING = auto()
    ERROR = auto()
    FATAL = auto()
    DEBUG = auto()

def log(level: LogLevel, message: str, error_trace: bool=False):
    if level == LogLevel.DEBUG and not DEBUG:
        return
    error_string = ""
    if error_trace:
        error_string = traceback.format_exc()
    sys.stderr.write("[" + threading.current_thread().getName() + "] " + level.name + " - " + message + error_string + "\n")
    sys.stderr.flush()
    
def to_json(obj: dict):
    return json.dumps(obj, ensure_ascii=False)
