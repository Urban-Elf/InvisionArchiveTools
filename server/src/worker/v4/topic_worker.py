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

from ...worker.ic_worker import IC4Worker
from ...worker.state.worker_state import *
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import *
from ...worker.ic_exceptions import ICWorkerException
from ...content.content import Messenger, Post
from ...content.writers import JSONWriter
from ... import proto_model
from ... import util
import re

class TopicWorkerV4State:
    SELECT_MESSENGER = ICWorkerState(note="Navigate to any topic to begin archiving.",
                                     hint="Click on a messenger in the browser and press 'Next'",
                                     button_configs=[ButtonConfig("Next")])
    ANALYZING = ICWorkerState(note="Preparing...")
    INVALID_MESSENGER = ICWorkerState("No messenger selected. Please try again.",
                                      button_configs=[ButtonConfig("OK")])
    ARCHIVING = ICWorkerState(note="Archiving...",
                              hint="Please do not interfere with the browser.",
                              indeterminate=False)

class TopicWorkerV4(IC4Worker):
    def __init__(self, worker_id, ic):
        super().__init__(worker_id, ic)

    def execute(self):
        pass