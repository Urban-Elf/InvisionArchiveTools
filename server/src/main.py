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
import uuid
from . import util
from . import shared_constants
from .worker import ic_worker
from .worker.v4 import messenger_worker
from . import proto_model
from .worker.ic import *

DEBUG = False
SINGLE_WORKER = False

IC_VERSION_MAP = {
    4: IC4,
    5: IC5
}

WORKER_VERSION_MAP = {
    shared_constants.WorkerType.MESSENGER_WORKER: {
        4: messenger_worker.MessengerWorkerV4,
        5: None # FIXME
    },
    shared_constants.WorkerType.TOPIC_WORKER: None,
    shared_constants.WorkerType.FORUM_WORKER: None,
    shared_constants.WorkerType.BLOG_WORKER: None,
}

ACTIVE_WORKERS = {}

def process_client_cmd():
    for line in sys.stdin:
        try:
            packet = proto_model.ClientPacket(line)

            # Process client command
            if packet.shared_action == proto_model.ClientSA.DISPATCH_WORKER:
                ic = packet.data["ic"]
                # IC version
                version = ic["version"]
                assert type(version) is int, "'version' must be of type int"
                version = int(version)
                assert version == 4 or version == 5, "'version' must be of value 4 or 5"
                ## DEBUG ##
                util.log(util.LogLevel.INFO, "Root URL: " + str(ic["root_url"]))
                # Initialize IC and Worker
                _ic = IC_VERSION_MAP[version](str(ic["root_url"]))
                if DEBUG and SINGLE_WORKER:
                    # Fixed value for convenience
                    _uuid = "ae2b7ad4-66c1-4118-8aba-8445b272d997"
                else:
                    _uuid = str(uuid.uuid4())
                # Relay worker uuid back to client
                proto_model.write_packet(proto_model.ServerPacket(worker_id=None, shared_action=proto_model.ServerSA.UUID_AVAILABLE)
                                 .add_data("uuid", _uuid))
                _worker: ic_worker.ICWorker = WORKER_VERSION_MAP[shared_constants.WorkerType[packet.data["worker_type"]]][version](_uuid, _ic)
                # Remove worker from active group on exit
                _worker.on_shutdown = lambda: ACTIVE_WORKERS.pop(_uuid, None)
                # Add to active group
                ACTIVE_WORKERS[_uuid] = _worker
                # Start worker
                _worker.start()
            elif packet.shared_action == proto_model.ClientSA.STATE_INPUT:
                _worker = get_worker_by_uuid(packet.worker_id)
                if (_worker == None):
                    continue
                # Update client object
                _worker.set_client_object(packet.data["client_object"])
                pass
            elif packet.shared_action == proto_model.ClientSA.TERMINATE_WORKER:
                _worker = get_worker_by_uuid(packet.worker_id)
                if (_worker == None):
                    continue
                try:
                    _worker.shutdown()
                    _worker.join()
                except:
                    util.log(util.LogLevel.WARNING, "Error shutting down worker: ", error_trace=True)
            elif packet.shared_action == proto_model.ClientSA.TERMINATE:
                break
        except:
            util.log(util.LogLevel.WARNING, "Failed to deconstruct client packet: ", error_trace=True)

def get_worker_by_uuid(uuid: str) -> ic_worker.ICWorker:
    if uuid == None:
        return None
    if ACTIVE_WORKERS.__contains__(uuid):
        return ACTIVE_WORKERS[uuid]
    util.log(util.LogLevel.WARNING, "Attempted to access non-existent worker (uuid: " + str(uuid) + ")")
    return None

def main(args: list[str]):
    global DEBUG, SINGLE_WORKER

    # Parse command-line arguments
    if "--debug" in args:
        DEBUG = True

    if "--single-worker" in args:
        SINGLE_WORKER = True

    util.log(util.LogLevel.INFO, "IAT server started successfully.")
    if DEBUG:
        util.log(util.LogLevel.INFO, "Debug mode is enabled.")
    util.log(util.LogLevel.INFO, "Waiting for client commands...")

    process_client_cmd()

    util.log(util.LogLevel.INFO, "Shutting down workers...")
    for worker_id in ACTIVE_WORKERS:
        worker: ic_worker.ICWorker = get_worker_by_uuid(worker_id)
        util.log(util.LogLevel.INFO, "KILLSIG >> " + worker_id)
        worker.shutdown()
        worker.join()

if __name__ == "__main__":
    main()
