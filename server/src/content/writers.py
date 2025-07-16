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

from pathlib import Path
from .content import Content
import hashlib
from datetime import UTC, datetime
from ..file_tree import EXPORT_PATH
import json

def hash_instant() -> str:
    now = datetime.now(UTC).isoformat()
    return hashlib.sha256(now.encode()).hexdigest()

class Writer:
    @staticmethod
    def write(content: Content) -> Path:
        raise NotImplementedError()

class JSONWriter(Writer):
    @staticmethod
    def write(content) -> str:
        path: Path = EXPORT_PATH / (hash_instant() + ".json")
        path.write_text(json.dumps(content.__serialize__()) + '\n')
        return str(path)
