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
    def __init__(self, worker_id, ic):
        super().__init__(worker_id, ic)

    def execute(self):
        # Select messenger
        while not self.shutdown_event.is_set():
            if not util.strip_url(self.ic.messenger()) in util.strip_url(self.driver.current_url):
                self.driver.get(self.ic.messenger())
            self.set_state(MessengerWorkerState.SELECT_MESSENGER)

            self.set_state(MessengerWorkerState.ANALYZING)
            try:
                self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[@class=\"ipsLoading\"]")
                self.set_state(MessengerWorkerState.INVALID_MESSENGER)
                continue
            except:
                pass

            try:
                self.driver_await(EC.presence_of_element_located((By.XPATH,
                    "//div[@data-role=\"commentFeed\"]//article")), time=3)
                # Valid session
                break
            except TimeoutException:
                self.set_state(MessengerWorkerState.INVALID_MESSENGER)

        # Archive posts
        messenger = self.archive_posts()

        ############### DEBUG ################
        #util.log(util.LogLevel.INFO, "Found " + str(messenger.posts.__len__()) + " posts, " + str(messenger.avatar_map.__len__()) + " avatars.")
        #for post in messenger.posts:
        #        util.log(util.LogLevel.INFO, post.__serialize__().__str__() + "\n")
        #for key in messenger.avatar_map:
        #    util.log(util.LogLevel.INFO, f"{key}: {messenger.avatar_map[key]}")
        ######################################

        # Export to temporary JSON file
        path = JSONWriter.write(messenger)

        # Notify client
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.RESULT_AVAILABLE)
                                 .add_data("path", path))
        
        self.set_state(SharedState.RESULT_AVAILABLE)

    def archive_posts(self):
        # Preparation
        page = 1
        final_page = 1
        page_url = self.driver.current_url + "?page="

        try:
            pagination_last = self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//li[@class=\"ipsPagination_last\"]/a")
            util.log(util.LogLevel.INFO, "Found pagination last: " + pagination_last.get_attribute("href"))
            final_page_url = re.sub(r"#.*", "", pagination_last.get_attribute("href"))
            match = re.search(r'\?page=(\d+)', final_page_url)
            if match:
                final_page = int(match.group(1))
        except NoSuchElementException as e:
            util.log(util.LogLevel.WARNING, "No pagination found, defaulting to single-paged messenger.")
            pass

        # Archive
        self.set_state(MessengerWorkerState.ARCHIVING)

        messenger = Messenger()

        while not self.shutdown_event.is_set():
            next_page_url = page_url + str(page) + "/"

            if page > 1:
                self.driver.get(next_page_url)
                self.driver_await(EC.url_contains("?page=" + str(page)))

            # Comments feed
            self.driver_await(EC.presence_of_element_located((By.CSS_SELECTOR, "div[data-role=\"commentFeed\"]")))
            comments_feed = self.driver.find_element(By.CSS_SELECTOR, "div[data-role=\"commentFeed\"]")

            self.driver_await(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "article")))
            articles = comments_feed.find_elements(By.CSS_SELECTOR, "article")

            util.log(util.LogLevel.INFO, "Deconstructing articles (" + str(page) + "/" + str(final_page) + ")...")

            for article in articles:
                author = article.find_element(By.CSS_SELECTOR, "h3.ipsComment_author")
                datetime = article.find_element(By.CSS_SELECTOR, "time")
                content = article.find_element(By.CSS_SELECTOR, "div[data-role=\"commentContent\"]")

                author_str: str = re.sub(util.TAG_REGEX, "", author.get_attribute("innerHTML"))
                author_str = author_str.strip()
                datetime_str = datetime.get_attribute("title").strip()
                content_str = content.get_attribute("innerHTML").strip()

                comment_id = article.get_attribute("id").replace("elComment_", "")
                link = page_url + str(page) + "#findComment-" + comment_id

                messenger.posts.append(Post(author=author_str, datetime=datetime_str, link=link, content=content_str))
                
                if not author_str in messenger.avatar_map:
                    avatar = article.find_element(By.CSS_SELECTOR, f"img[alt=\"{author_str}\"]").get_attribute("src")
                    messenger.avatar_map[author_str] = avatar

            self.update_progress(float(page/final_page))

            page += 1

            if page > final_page:
                break

        return messenger
