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

class Post(serializable.Serializable):
    def __init__(self, author: str, datetime: str, link: str, content: str):
        self.author = author
        self.datetime = datetime
        self.link = link
        self.content = content

    def __serialize__(self):
        return {
            "author": self.author,
            "datetime": self.datetime,
            "link": self.link,
            "content": self.content
        }

class Messenger:
    def __init__(self):
        self.posts: list[Post] = []
        self.avatar_map = {}