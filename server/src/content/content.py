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

from .. import serializable
from ..shared_constants import ContentType

class Content(serializable.JSONSerializable):
    pass
    
class Post(serializable.JSONSerializable):
    def __init__(self, author: str, datetime: str, link: str, content: str):
        self.author = author
        self.datetime = datetime
        self.link = link
        self.content = content

    def __serialize__(self) -> dict:
        return {
            "author": self.author,
            "datetime": self.datetime,
            "link": self.link,
            "content": self.content
        }
    
class PostContent(Content):
    def __init__(self, title: str, type: ContentType):
        self.title = title
        self.type = type
        self.avatar_map: dict[str, str] = {}
        self.pages: list[list[Post]] = []

    def __serialize__(self) -> dict:
        return {
            "title": self.title,
            "type": self.type.name,
            "avatar_map": self.avatar_map,
            "posts": [[post.__serialize__() for post in page] for page in self.pages]
        }
