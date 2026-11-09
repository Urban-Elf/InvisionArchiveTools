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

import ssl
import certifi
import urllib.request

# Save original
_original_urlopen = urllib.request.urlopen

# Custom SSL context with certifi CA
_certifi_context = ssl.create_default_context(cafile=certifi.where())

# Monkey-patch urlopen to always use certifi context
def patched_urlopen(*args, **kwargs):
    if 'context' not in kwargs:
        kwargs['context'] = _certifi_context
    return _original_urlopen(*args, **kwargs)

urllib.request.urlopen = patched_urlopen

import sys
import uuid
import threading
from . import util
from . import file_tree
from pathlib import Path
import undetected_chromedriver
from . import shared_constants
from .worker import ic_worker
from .worker.v4 import messenger_worker
from .worker.v4 import topic_worker
from . import proto_model
from .worker.ic import *

DEBUG = True
SINGLE_WORKER = True

IC_VERSION_MAP = {
    4: IC4,
    5: IC5
}

WORKER_VERSION_MAP = {
    shared_constants.WorkerType.MESSENGER_WORKER: {
        4: messenger_worker.MessengerWorkerV4,
        5: None # FIXME
    },
    shared_constants.WorkerType.TOPIC_WORKER: {
        4: topic_worker.TopicWorkerV4,
        5: None
    },
    shared_constants.WorkerType.FORUM_WORKER: {
        4: None,
        5: None
    },
    shared_constants.WorkerType.BLOG_WORKER: {
        4: None,
        5: None
    },
}

def get_worker_spec_by_class(worker_class):
    for worker_type, version_map in WORKER_VERSION_MAP.items():
        # Sanity check
        assert type(version_map) is dict, "version_map must be a dictionary"
        for version, cls in version_map.items():
            if cls == worker_class:
                return {"type": worker_type, "version": version}
    return None

ACTIVE_WORKERS = dict()
ACTIVE_WORKERS_LOCK = threading.Lock()

def process_client_packet(packet: proto_model.ClientPacket) -> bool:
    """
    Returns true if server should terminate
    """
    # Process client command
    if packet.shared_action == proto_model.ClientSA.DISPATCH_WORKER:
        dispatch_worker(packet)

    elif packet.shared_action == proto_model.ClientSA.LOAD_FROM_SPEC:
        input_dir = Path(packet.data["input_dir"])
        assert not input_dir.is_absolute(), "input_dir cannot be absolute"
        # Make relative to EXPORT_PATH
        input_dir = file_tree.EXPORT_PATH / input_dir
        # Assert exists
        assert input_dir.exists(), "input_dir does not exist"
        # Dispatch from spec (load from input_dir)
        dispatch_worker(packet, spec_input_dir=input_dir)

    elif packet.shared_action == proto_model.ClientSA.REFRESH_WORKER:
        _worker = get_worker_by_uuid(packet.worker_id)
        if (_worker == None):
            return False
        # Obtain stored refresh spec from worker
        r_spec = _worker.get_stored_refresh_spec()
        if r_spec is None:
            util.log(util.LogLevel.WARNING, "Worker UUID " + packet.worker_id + " has no stored refresh spec. Cannot refresh.")
            return False
        
        # Create a packet with existing data (kind of hacky, but works)
        _spec = get_worker_spec_by_class(_worker.__class__)
        # FIXME: version should be obtained from IC_VERSION_MAP not _spec.
        _dummy_packet = proto_model.ClientPacket("""{"shared_action":"%s", "data":{"ic":{"root_url":"%s","version":%s},"worker_type":"%s"}}"""
                                          % (shared_constants.ClientSA.DUMMY.name, _worker.ic.root_url, _spec["version"], _spec["type"].name))
        # clone before worker shutdown
        input_dir = Path(_worker.output_dir)
        # Shutdown worker (thread joined to main), do NOT remove from ACTIVE_WORKERS (callback=False)
        terminate_worker(packet.worker_id, callback=False)
        # Dispatch from spec with existing UUID (load from spec)
        dispatch_worker(_dummy_packet, spec_input_dir=input_dir, existing_uuid=packet.worker_id)

    elif packet.shared_action == proto_model.ClientSA.STATE_INPUT:
        _worker = get_worker_by_uuid(packet.worker_id)
        if (_worker == None):
            return False
        # Update client object
        _worker.set_client_object(packet.data["client_object"])
        pass
    elif packet.shared_action == proto_model.ClientSA.TERMINATE_WORKER:
        terminate_worker(packet.worker_id)
    elif packet.shared_action == proto_model.ClientSA.TERMINATE:
        return True

def dispatch_worker(packet: proto_model.ClientPacket, spec_input_dir=None, existing_uuid=None):
    """
    Dispatches a new worker based on packet data for type and version.
    The spec_input_dir is used if you are starting a worker from a saved spec.
    The existing_uuid is used if you are refreshing a worker (restarting ChromeDriver) and want to keep the same UUID.
    """

    ic = packet.data["ic"]
    # IC version
    version = ic["version"]
    assert type(version) is int, "'version' must be of type int"
    version = int(version)
    assert version == 4 or version == 5, "'version' must be of value 4 or 5"

    # worker type
    worker_type = shared_constants.WorkerType[packet.data["worker_type"]]

    ## DEBUG ##
    util.log(util.LogLevel.INFO, "Root URL: " + str(ic["root_url"]))

    # Initialize IC and Worker
    _ic = IC_VERSION_MAP[version](str(ic["root_url"]))
    _uuid = None
    _worker = None
    
    if DEBUG and SINGLE_WORKER:
        # Fixed value for convenience
        _uuid = "ae2b7ad4-66c1-4118-8aba-8445b272d997"
    else:
        _uuid = str(uuid.uuid4())

    # for REFRESH client cmd it is desired to keep the same UUID instead of resetting
    if spec_input_dir is not None and existing_uuid is not None:
        _uuid = existing_uuid
    else: # do NOT relay if existing_uuid is provided, as it is already known to the client
        # Relay NEW worker uuid back to client
        proto_model.write_packet(proto_model.ServerPacket(worker_id=None, shared_action=proto_model.ServerSA.UUID_AVAILABLE)
                            .add_data("uuid", _uuid))

    # Construct worker from spec or from fresh start
    if spec_input_dir is not None:
        ## Load from spec
        
        # Determine class
        worker_class: ic_worker.ICWorker = WORKER_VERSION_MAP[worker_type][version]
        
        _worker = worker_class.load_from_input_dir(_uuid, _ic, spec_input_dir)
    else:
        assert not Path(packet.data["output_dir"]).is_absolute(), "output_dir cannot be absolute"
        output_dir: Path = file_tree.EXPORT_PATH / packet.data["output_dir"]
        output_dir.mkdir(parents=True, exist_ok=True)
        _worker: ic_worker.ICWorker = WORKER_VERSION_MAP[worker_type][version](_uuid, _ic, output_dir)

    if spec_input_dir is not None and existing_uuid is not None:
        # Refreshing worker, do NOT remove from ACTIVE_WORKERS on shutdown
        def _on_shutdown():
            util.log(util.LogLevel.INFO, "Worker UUID " + _uuid + " (refreshed) shutdown successfully.")
        _worker.on_shutdown = _on_shutdown
    else:
        # Fixed value
        static_uuid = _uuid
        def _on_shutdown():
            # remove from list
            with ACTIVE_WORKERS_LOCK:
                ACTIVE_WORKERS.pop(static_uuid, None)
            util.log(util.LogLevel.INFO, "Worker UUID " + static_uuid + " shutdown successfully.")
        # Remove worker from active group on exit
        _worker.on_shutdown = _on_shutdown
    
    # Add to active group
    with ACTIVE_WORKERS_LOCK:
        ACTIVE_WORKERS[_uuid] = _worker

    # Start worker
    _worker.start()


def terminate_worker(uuid, callback: bool=True):
    _worker = get_worker_by_uuid(uuid)
    if (_worker == None):
        return False
    try:
        _worker.shutdown(callback=callback)
        _worker.join()
    except:
        util.log(util.LogLevel.WARNING, "Error shutting down worker: ", error_trace=True)

def get_worker_by_uuid(uuid: str) -> ic_worker.ICWorker:
    if uuid == None:
        return None
    with ACTIVE_WORKERS_LOCK:
        if ACTIVE_WORKERS.__contains__(uuid):
            return ACTIVE_WORKERS[uuid]
    util.log(util.LogLevel.WARNING, "Attempted to access non-existent worker (uuid: " + str(uuid) + ")")
    return None

def main(args: list[str]):
    global DEBUG, SINGLE_WORKER

    # Parse command-line arguments
    if "--debug" in args:
        DEBUG = True
        util.DEBUG = True

    if "--single-worker" in args:
        SINGLE_WORKER = True

    certifi_path = certifi.where()
    if certifi_path is not None:
        util.log(util.LogLevel.INFO, "CERTIFI location: " + certifi_path)
    else:
        util.log(util.LogLevel.WARNING, "CERTIFI not found in server installation.")
    chrome_path = undetected_chromedriver.find_chrome_executable()
    if chrome_path is not None:
        util.log(util.LogLevel.INFO, "ChromeDriver path: " + chrome_path)
    else:
        util.log(util.LogLevel.WARNING, "ChromeDriver not present on this system.")

    util.log(util.LogLevel.INFO, "IAT server started successfully.")
    if DEBUG:
        util.log(util.LogLevel.INFO, "Debug mode is enabled.")
    util.log(util.LogLevel.INFO, "Waiting for client commands...")

    # Main loop
    for line in sys.stdin:
        try:
            packet = proto_model.ClientPacket(line)
            if process_client_packet(packet):
                break
        except:
            util.log(util.LogLevel.WARNING, "Failed to deconstruct client packet: ", error_trace=True)

    util.log(util.LogLevel.INFO, "Shutting down workers...")
    for worker_id in ACTIVE_WORKERS:
        worker: ic_worker.ICWorker = get_worker_by_uuid(worker_id)
        util.log(util.LogLevel.INFO, "KILLSIG >> " + worker_id)
        worker.shutdown(callback=False)
        worker.join()
