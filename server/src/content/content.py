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
from .. import serializable
from typing import TextIO
import json
from ..shared_constants import ContentType
from ..worker.ic import IC
from .converter import ContentConverter

class Content(serializable.NDJSONSerializable):
    pass
    
class Reactor(serializable.JSONSerializable):
    def __init__(self, avatar_url: str, name: str, datetime: str):
        self.avatar_url = avatar_url
        self.name = name
        self.datetime = datetime
        
    def __serialize__(self):
        return {
            "avatar_url": self.avatar_url,
            "name": self.name,
            "datetime": self.datetime
        }

class Reaction(serializable.JSONSerializable):
    def __init__(self, image: str, name: str, count: int, reactors: list[Reactor]):
        self.image = image
        self.name = name
        self.count = count
        self.reactors: list[Reactor] = reactors

    def __serialize__(self):
        return {
            "image": self.image,
            "name": self.name,
            "count": self.count,
            "reactors": [reactor.__serialize__() for reactor in self.reactors]
        }

class Reactions(serializable.JSONSerializable):
    def __init__(self):
        self.overview: str = ""
        self.reactions: list[Reaction] = []

    def set_overview(self, overview: str):
        self.overview = overview

    def add_reaction(self, reaction: Reaction):
        self.reactions.append(reaction)

    def __serialize__(self):
        return {
            "overview": self.overview,
            "reactions": [reaction.__serialize__() for reaction in self.reactions]
        }

class Post(serializable.JSONSerializable):
    def __init__(self, author: str, datetime: str, link: str, content: str):
        self.author = author
        self.datetime = datetime
        self.link = link
        self.content = content
        self.reactions: Reactions = None
        self.recommended = False
        self.hidden = False

    def set_reactions(self, reactions: list[Reaction]):
        self.reactions = reactions

    def convert_content(self, ic: IC):
        """Convert the content of the post using ContentConverter."""
        self.content = ContentConverter(self.content, ic).convert()

    def __serialize__(self) -> dict:
        return {
            "author": self.author,
            "datetime": self.datetime,
            "link": self.link,
            "content": self.content,
            "reactions": self.reactions.__serialize__() if self.reactions else None
        }

class Meta(serializable.JSONSerializable):
    def __init__(self, title: str, type: ContentType):
        self.title = title
        self.type = type
        self.userdata: dict[str, UserData] = {}

    def __serialize__(self) -> dict:
        return {
            "type": self.type.name,
            "title": self.title,
            "user_data": {name: self.userdata[name].__serialize__() for name in self.userdata}
        }
    
    @staticmethod
    def from_dict(data: dict):
        meta = Meta(data["title"], ContentType[data["type"]])
        userdata = data["user_data"]
        for name in userdata:
            s_entry = userdata[name]
            _userdata_entry = UserData(s_entry["profile_url"], s_entry["avatar_url"])
            _userdata_entry.set_group(s_entry["group"])
            _userdata_entry.set_group(s_entry["group"])
            meta.userdata[name] = _userdata_entry
        return meta
    
    @staticmethod
    def deserialize(file: Path):
        with open(file, "r", encoding="utf-8") as f:
            data = json.load(f)
            return Meta.from_dict(data)
        return None

class UserData(serializable.JSONSerializable):
    def __init__(self, profile_url: str, avatar_url: str):
        self.profile_url: str = profile_url
        self.avatar_url: str = avatar_url
        self.group: str = ""
        self.group_icon_url: str = ""

    def set_group(self, group: str):
        self.group = group
        return self

    def set_group_icon_url(self, group_icon_url: str):
        self.group_icon_url = group_icon_url
        return self

    def __serialize__(self):
        return {
            "profile_url": self.profile_url,
            "avatar_url": self.avatar_url,
            "group": self.group,
            "group_icon_url": self.group_icon_url
        }

class PageContent(Content):
    def __init__(self):
        self.posts: list[Post] = []

    def __ndjson_write__(self, file_obj: TextIO):
        file_obj.write(json.dumps({"content":[post.__serialize__() for post in self.posts]}) + "\n")
        file_obj.flush()

class PostCount(serializable.JSONSerializable):
    def __init__(self, count: int):
        self.count = count

    def __serialize__(self) -> dict:
        return {
            "total_posts": self.count
        }

    @staticmethod
    def deserialize(file: Path):
        with open(file, "r", encoding="utf-8") as f:
            data = json.load(f)
            count = data["total_posts"]
            return PostCount(count)
        return None

# DEPRECATED
class PostContent(Content):
    """
    Deprecated.
    """
    def __init__(self, title: str, type: ContentType):
        self.title = title
        self.type = type
        self.user_data: dict[str, UserData] = {}
        self.pages: list[list[Post]] = []

    def __serialize_metadata__(self) -> dict:
        return {
            "type": self.type.name,
            "title": self.title,
            "user_data": {name: self.user_data[name].__serialize__() for name in self.user_data},
        }

    def __ndjson_write__(self, file_obj: TextIO):
        # Metadata
        file_obj.write(json.dumps(self.__serialize_metadata__()) + "\n")
        # Content (page chunks)
        for page in self.pages:
            file_obj.write(json.dumps({"content":[post.__serialize__() for post in page]}) + "\n")
            file_obj.flush()
