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

from .content import PostContent, Post
from .. import util

class TopicPost(Post):
    def __init__(self, author: str, group_raw: str, datetime: str, link: str, content: str):
        super().__init__(author, datetime, link, content)
        self.group_raw = group_raw

    def get_group(self) -> str:
        return util.strip_tags(self.group_raw)
    
    def __serialize__(self) -> dict:
        map = super().__serialize__()
        map["group_raw"] = self.group_raw
        return map

class Topic(PostContent):
    def __init__(self, title):
        super().__init__(title)
