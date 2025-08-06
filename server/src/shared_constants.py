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

from enum import Enum, auto

class ClientSA(Enum):
    # data[ic, worker_type]
    DISPATCH_WORKER = auto()
    # worker_id, data[client_object]
    STATE_INPUT = auto()
    # worker_id
    TERMINATE_WORKER = auto()
    # <none>
    TERMINATE = auto()

class ServerSA(Enum):
    # data[uuid]
    UUID_AVAILABLE = auto()
    # worker_id
    CHROMEDRIVER_STARTED = auto()
    # worker_id, data[stacktrace]
    CHROMEDRIVER_ERROR = auto()
    # worker_id, data[state]
    STATE_CHANGED = auto()
    # worker_id, data[progress]
    PROGRESS_UPDATE = auto()
    # worker_id, data[path]
    RESULT_AVAILABLE = auto()

class WorkerType(Enum):
    MESSENGER_WORKER = auto()
    TOPIC_WORKER = auto()
    FORUM_WORKER = auto()
    BLOG_WORKER = auto()

class ContentType(Enum):
    MESSENGER = auto()
    TOPIC = auto()
    FORUM = auto()
    BLOG = auto()
    BLOG_ENTRY = auto()

class ButtonCallbackSA(Enum):
    NONE = auto()
    OPEN_LOG = auto()
    EXPORT_ARCHIVE = auto()
    TERMINATE = auto()