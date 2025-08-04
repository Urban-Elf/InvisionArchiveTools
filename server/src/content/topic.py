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

from .content import PostContent
from ..serializable import JSONSerializable
from ..shared_constants import ContentType
from .. import util

class PollOption(JSONSerializable):
    def __init__(self, text: str, value: int):
        self.text = text
        self.value = value

    def __serialize__(self):
        return {
            "text": self.text,
            "value": self.value
        }

class PollQuestion(JSONSerializable):
    def __init__(self, text: str, options: list[PollOption]):
        self.text = text
        self.options = options

    def __serialize__(self):
        return {
            "text": self.text,
            "options": [option.__serialize__() for option in self.options]
        }

class Poll(JSONSerializable):
    def __init__(self, title, participants: int):
        self.participants = participants
        self.title = title
        self.questions: list[PollQuestion] = {}

    def add_question(self, question: PollQuestion):
        self.questions.append(question)

    def __serialize__(self):
        return {
            "title": self.title,
            "participants": self.participants,
            "questions": [question.__serialize__() for question in self.questions]
        }
    
class RecommendedPost(JSONSerializable):
    def __init__(self, author: str, content_preview: str, link: str):
        self.author = author
        self.content_preview = content_preview
        self.link = link

    def __serialize__(self):
        return {
            "author": self.author,
            "content_preview": self.content_preview,
            "link": self.link
        }

class TopicPostContent(PostContent):
    def __init__(self, title, type):
        super().__init__(title, type)
        self.poll = None
        self.recommended_posts: list[RecommendedPost] = []
        self.top_posters: dict[str, int] = {}

    def set_poll(self, poll: Poll):
        self.poll = poll

    def add_recommended_post(self, post: RecommendedPost):
        self.recommended_posts.append(post)

    def add_top_poster(self, name: str, post_count: int):
        self.top_posters[name] = post_count

    def __serialize_metadata__(self):
        metadata = super().__serialize_metadata__()
        metadata["poll"] = self.poll.__serialize__()
        metadata["recommended_posts"] = [post.__serialize__() for post in self.recommended_posts]
        metadata["top_posters"] = self.top_posters
        return metadata

class Topic(PostContent):
    def __init__(self, title):
        super().__init__(title, type=ContentType.TOPIC)
