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
from ...worker.ic_chromedriver import IC_ChromeDriver
from ...content.topic import Topic, TopicPost
from ...content.writers import JSONWriter
from ... import proto_model
from ... import util
import re

class TopicWorkerV4State:
    SELECT_TOPIC = ICWorkerState(note="Navigate to a topic to begin archiving.",
                                     hint="Locate a topic on the browser, and press 'Next'",
                                     button_configs=[ButtonConfig("Next")])
    ANALYZING = ICWorkerState(note="Preparing...")
    INVALID_TOPIC = ICWorkerState("No topic open. Please try again.",
                                      button_configs=[ButtonConfig("OK")])
    ARCHIVING = ICWorkerState(note="Archiving...",
                              hint="Please do not interfere with the browser.",
                              indeterminate=False)

class TopicWorkerV4(IC4Worker):
    def __init__(self, worker_id, ic):
        super().__init__(worker_id, ic)

    def execute(self):
        # Select messenger
        while not self.shutdown_event.is_set():
            self.driver.get(self.ic.root_url)

            self.set_state(TopicWorkerV4State.SELECT_TOPIC)

            self.set_state(TopicWorkerV4State.ANALYZING)
            try:
                loading = self.driver.find_element(By.XPATH, "//div[@id=\"elAjaxLoading\"]")
                if loading.is_displayed():
                    self.set_state(TopicWorkerV4State.INVALID_TOPIC)
                    continue
            except:
                pass

            try:
                self.driver_await(EC.presence_of_element_located((By.XPATH,
                    "//div[@data-role=\"commentFeed\"]//article")), time=3)
                # Valid session
                break
            except TimeoutException:
                self.set_state(TopicWorkerV4State.INVALID_TOPIC)

        clean_url = re.sub(r"/page/\d+.*$", "", self.driver.current_url)
        util.log(util.LogLevel.INFO, clean_url)
        self.driver.get(clean_url)
        try:
            self.driver_await(EC.url_contains(clean_url))
        except TimeoutException:
            pass

        # Archive posts
        topic = self.archive_posts(self)

        # Export to temporary JSON file
        path = JSONWriter.write(topic)

        # Notify client
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.RESULT_AVAILABLE)
                                 .add_data("path", path))
        
        self.set_state(SharedState.RESULT_AVAILABLE)

        self.shutdown()

    @staticmethod
    def archive_posts(worker: IC4Worker) -> Topic:
        # Preparation
        page = 1
        final_page = 1
        page_url = worker.driver.current_url + "/page/"

        try:
            pagination_last = worker.driver.find_element(By.XPATH, "//ul[contains(@class, \"ipsPagination\")]//li[contains(@class, \"ipsPagination_last\")]/a")
            util.log(util.LogLevel.INFO, "Found pagination last: " + pagination_last.get_attribute("href"))
            final_page_url = re.sub(r"#.*", "", pagination_last.get_attribute("href"))
            match = re.search(r'/page/(\d+)', final_page_url)
            if match:
                final_page = int(match.group(1))
        except NoSuchElementException:
            util.log(util.LogLevel.WARNING, "No pagination found, defaulting to single-paged topic.")

        # Archive
        worker.set_state(TopicWorkerV4State.ARCHIVING)

        # Get topic title
        title_str = "Invision Community Topic"
        try:
            title = worker.driver.find_element(By.XPATH, "//div[contains(@class, \"ipsPageHeader\")]//h1[contains(@class, \"ipsType_pageTitle\")]")
            title_str = util.strip_tags(title.get_attribute("innerHTML")).strip()
        except NoSuchElementException:
            util.log(util.LogLevel.WARNING, "Failed to find topic title, using default.")
            pass

        topic = Topic(title=title_str)

        while not worker.shutdown_event.is_set():
            next_page_url = page_url + str(page) + "/"

            if page > 1:
                worker.driver.get(next_page_url)
                worker.driver_await(EC.url_contains("/page/" + str(page)))

            # Comments feed
            worker.driver_await(EC.presence_of_element_located((By.CSS_SELECTOR, "div[data-role=\"commentFeed\"]")))
            comments_feed = worker.driver.find_element(By.CSS_SELECTOR, "div[data-role=\"commentFeed\"]")

            worker.driver_await(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "article")))
            articles = comments_feed.find_elements(By.CSS_SELECTOR, "article")

            util.log(util.LogLevel.INFO, "Deconstructing articles (" + str(page) + "/" + str(final_page) + ")...")

            # Group posts by page (25/page by IC default)
            page_posts: list[TopicPost] = []

            for article in articles:
                author = article.find_element(By.CSS_SELECTOR, "h3.cAuthorPane_author")
                group = article.find_element(By.CSS_SELECTOR, "li[data-role=\"group\"]")
                group_icon = article.find_element(By.XPATH, "//li[@data-role=\"group-icon\"]//img")
                datetime = article.find_element(By.CSS_SELECTOR, "time")
                content = article.find_element(By.CSS_SELECTOR, "div[data-role=\"commentContent\"]")

                author_str: str = util.strip_tags(author.get_attribute("innerHTML"))
                author_str = author_str.strip()
                group_str = group.get_attribute("innerHTML").strip()
                group_icon_str = "http:" + group_icon.get_attribute("src").strip()
                datetime_str = datetime.get_attribute("title").strip()
                content_str = content.get_attribute("innerHTML").strip()

                comment_id = article.get_attribute("id").replace("elComment_", "")
                link = page_url + str(page) + "#findComment-" + comment_id

                post = TopicPost(author=author_str, group_raw=group_str, datetime=datetime_str, link=link, content=content_str)
                page_posts.append(post)
                
                if not author_str in topic.avatar_map:
                    avatar = article.find_element(By.CSS_SELECTOR, f"img[alt=\"{author_str}\"]").get_attribute("src")
                    topic.avatar_map[author_str] = avatar

                if not post.get_group() in topic.group_icon_map:
                    topic.group_icon_map[post.get_group()] = group_icon_str

            # Append page
            topic.pages.append(page_posts)

            # Update progress
            worker.update_progress(float(page/final_page))

            page += 1

            if page > final_page:
                break

        return topic
