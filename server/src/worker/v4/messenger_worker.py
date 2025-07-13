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
from ...content.messenger import *
from ... import util
import re

class MessengerWorkerState:
    SELECT_MESSENGER = ICWorkerState(note="Select a messenger to archive.",
                                     hint="Click on a messenger in the browser and press 'Next'",
                                     button_configs=[ButtonConfig("Next")])
    ANALYZING = ICWorkerState(note="Preparing...")
    INVALID_MESSENGER = ICWorkerState("No messenger selected. Please try again.",
                                      button_configs=[ButtonConfig("OK")])
    ARCHIVING = ICWorkerState(note="Archiving...",
                              hint="Please do not interfere with the browser.",
                              indeterminate=False)

class MessengerWorkerV4(IC4Worker):
    def __init__(self, worker_id, ic, result_consumer):
        super().__init__(worker_id, ic, result_consumer)

    def execute(self):
        # Select messenger
        while not self.shutdown_event.is_set():
            if not self.driver.current_url == self.ic.auth():
                self.driver.get(self.ic.messenger())
            self.set_state(MessengerWorkerState.SELECT_MESSENGER)

            self.set_state(MessengerWorkerState.ANALYZING)
            try:
                self.driver_await(EC.presence_of_element_located((By.XPATH,
                    "//div[@data-role=\"commentFeed\"]//article")), time=3)
                # Valid session
                break
            except TimeoutException:
                self.set_state(MessengerWorkerState.INVALID_MESSENGER)

        # Archive posts
        messenger = self.archive_posts()

        for post in messenger.posts:
                util.log(util.LogLevel.INFO, post.__serialize__().__str__() + "\n")

        for key in messenger.avatar_map:
            util.log(util.LogLevel.INFO, f"{key}: {messenger.avatar_map[key]}")

    def archive_posts(self):
        # Preparation
        page = 1
        current_url = self.driver.current_url
        page_url = current_url + "page/"
        final_page_url = None

        try:
            pagination_last = self.driver.find_element(By.CSS_SELECTOR, "ul.ipsPagination > .ipsPagination_last > a")
            final_page_url = re.sub(r"#.*", "", pagination_last.get_attribute("href"))
        except NoSuchElementException as e:
            raise ICWorkerException(f"Could not assess pagination length of messenger ({page_url})")

        # Archive
        self.set_state(MessengerWorkerState.ARCHIVING)

        while not self.shutdown_event.is_set():
            next_page_url = page_url + str(page) + "/"

            if page > 1:
                self.driver.get(next_page_url)
                self.driver_await(EC.url_changes(page_url + str(page) + "/"))

            messenger = Messenger()

            # Comments feed
            self.driver_await(EC.presence_of_element_located((By.CSS_SELECTOR, "div[data-role=\"commentFeed\"]")))
            comments_feed = self.driver.find_element(By.CSS_SELECTOR, "div[data-role=\"commentFeed\"]")

            self.driver_await(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "article")))
            articles = comments_feed.find_elements(By.CSS_SELECTOR, "article")

            for article in articles:
                author = article.find_element(By.CSS_SELECTOR, "h3.ipsComment_author")
                datetime = article.find_element(By.CSS_SELECTOR, "time")
                content = article.find_element(By.CSS_SELECTOR, "div[data-role=\"commentContent\"]")

                author_str: str = re.sub(util.TAG_REGEX, "", author.get_attribute("innerHTML"))
                author_str = author_str.strip()
                datetime_str = datetime.get_attribute("title").strip()
                content_str = content.get_attribute("innerHTML").strip()

                comment_id = article.get_attribute("id").replace("elComment_", "")
                link = current_url
                if not link.endswith("/"):
                    link += "/"
                link += f"?page={page}#findComment-{comment_id}"

                messenger.posts.append(Post(author=author_str, datetime=datetime_str, link=link, content=content_str))
                
                if not author_str in messenger.avatar_map:
                    avatar = article.find_element(By.CSS_SELECTOR, f"img[alt=\"{author_str}\"]").get_attribute("src")
                    # TODO: HTMLWriter should download these and store them in a local dir (as they're transient)
                    messenger.avatar_map[author_str] = avatar

            page += 1

            if final_page_url == None or next_page_url == final_page_url:
                break

        return messenger








