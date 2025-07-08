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
import util
from shared_constants import *
from serializable import Serializable
from worker.state.worker_state import *

class CustomEncoder(json.JSONEncoder):
    def default(self, obj):
        if isinstance(obj, Enum):
            return obj.name
        elif isinstance(obj, Serializable):
            return obj.__serialize__()
        return super().default(obj)

# Data structure to encapsualte incoming data from Java client
class ClientPacket:
    def __init__(self, json_data: str):
        util.validate_json_data(json_data)
        # Decode into python object
        json_obj: dict = json.loads(json_data)
        # Process identifier
        if json_obj.__contains__("worker_id"):
            self.worker_id = json_obj["worker_id"]
        # High-level worker instructions
        assert json_obj.__contains__("shared_action"), "packet missing 'shared_action'"
        self.shared_action = ClientSA[json_obj["shared_action"]]
        # Relevant data to the command
        if json_data.__contains__("data"):
            data = json_obj["data"]
            if not isinstance(data, dict):
                raise TypeError("'data' must be a dictionary")
            self.data = data

# Server is this python module
class ServerPacket(Serializable):
    def __init__(self, worker_id: str, shared_action: ServerSA):
        # Process identifier
        self.worker_id = worker_id
        # The action (state change, response code)
        self.shared_action = shared_action
        # Relevant data to the command
        self.data = {}
    
    def add_data(self, id, data):
        # Relevant data to the action
        self.data[id] = data
        return self
    
    def __serialize__(self):
        return {
            "worker_id": self.worker_id,
            "shared_action": self.shared_action,
            "data": self.data
        }

    def __str__(self):
        return json.dumps(self, cls=CustomEncoder)

    def __repr__(self):
        return self.__str__()
