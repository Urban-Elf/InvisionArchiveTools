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
from ...content.content import Post, UserData
from ...content.messenger import Messenger
from ...content.writers import JSONWriter
from ... import proto_model
from ... import util
import re

class MessengerWorkerV4State:
    SELECT_MESSENGER = ICWorkerState(note="Select a messenger to archive.",
                                     hint="Click on a messenger in the browser and press 'Next'",
                                     button_configs=[ButtonConfig("Next")])
    ANALYZING = ICWorkerState(note="Preparing...")
    INVALID_MESSENGER = ICWorkerState("No messenger selected. Please try again.",
                                      button_configs=[ButtonConfig("OK")])

class MessengerWorkerV4(IC4Worker):
    def __init__(self, worker_id, ic):
        super().__init__(worker_id, ic)

    def execute(self):
        # Select messenger
        while not self.shutdown_event.is_set():
            if not util.strip_url(self.ic.messenger()) in util.strip_url(self.driver.current_url):
                self.driver.get(self.ic.messenger())
            self.set_state(MessengerWorkerV4State.SELECT_MESSENGER)

            self.set_state(MessengerWorkerV4State.ANALYZING)
            try:
                self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[@class=\"ipsLoading\"]")
                self.set_state(MessengerWorkerV4State.INVALID_MESSENGER)
                continue
            except:
                pass

            try:
                self.driver_await(EC.presence_of_element_located((By.XPATH,
                    "//div[@id=\"elMessageViewer\"]//div[@data-role=\"commentFeed\"]//article")), time=3)
                # Valid session
                break
            except TimeoutException:
                self.set_state(MessengerWorkerV4State.INVALID_MESSENGER)

        # Clean url
        clean_url = re.sub(r"\?page=\d+.*$", "", self.driver.current_url)
        #util.log(util.LogLevel.INFO, clean_url)
        self.driver.get(clean_url)
        try:
            self.driver_await(EC.url_contains(clean_url))
        except TimeoutException:
            pass

        # Archive posts
        messenger = self.archive_posts()

        ############### DEBUG ################
        #util.log(util.LogLevel.INFO, "Found " + str(messenger.posts.__len__()) + " posts, " + str(messenger.avatar_map.__len__()) + " avatars.")
        #for post in messenger.posts:
        #        util.log(util.LogLevel.INFO, post.__serialize__().__str__() + "\n")
        #for key in messenger.avatar_map:
        #    util.log(util.LogLevel.INFO, f"{key}: {messenger.avatar_map[key]}")
        ######################################

        # Do not proceed with export if early shutdown occurs
        if self.shutdown_event.is_set():
            return

        # Export to temporary JSON file
        path = JSONWriter.write(messenger)

        # Notify client
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.RESULT_AVAILABLE)
                                 .add_data("path", path))
        
        self.set_state(SharedState.RESULT_AVAILABLE)

        # Don't shutdown, as client will send shutdown command

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

        # Archive
        self.set_state(SharedState.ARCHIVING)

        # Get messenger title
        title_str = "Invision Community Messenger"
        try:
            title = self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[contains(@class, \"ipsPageHeader\")]//h1[contains(@class, \"ipsType_pageTitle\")]")
            title_str = util.strip_tags(title.get_attribute("innerHTML")).strip()
        except NoSuchElementException:
            util.log(util.LogLevel.WARNING, "Failed to find topic title, using default.")
            pass

        messenger = Messenger(title=title_str)

        # Extract userdata
        userdata_from_posts = False
        try: 
            members_container = self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[contains(@class, \"cMessage_members\")]")
            members = members_container.find_elements(By.XPATH, "//ol//li[contains(@class, \"ipsPhotoPanel\")]")
            for member in members:
                profile_url = member.find_element(By.XPATH, ".//a").get_attribute("href")
                avatar = member.find_element(By.XPATH, ".//a//img")
                author_str = avatar.get_attribute("alt").strip()
                avatar_url = avatar.get_attribute("src").strip()
                user_data = UserData(profile_url=profile_url, avatar_url=avatar_url)
                # Add to messenger construct
                messenger.user_data[author_str] = user_data
        except NoSuchElementException:
            # No members section, try to extract from posts
            util.log(util.LogLevel.WARNING, "No members section found, extracting user data from posts.")
            userdata_from_posts = True

        while not self.shutdown_event.is_set():
            next_page_url = page_url + str(page) + "/"

            if page > 1:
                self.driver.get(next_page_url)
                self.driver_await(EC.url_contains("?page=" + str(page)))

            # Comments feed
            self.driver_await(EC.presence_of_element_located((By.XPATH, "//div[@id=\"elMessageViewer\"]//div[@data-role=\"commentFeed\"]")))
            comments_feed = self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[@data-role=\"commentFeed\"]")

            self.driver_await(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "article")))
            articles = comments_feed.find_elements(By.CSS_SELECTOR, "article")

            util.log(util.LogLevel.INFO, "Deconstructing articles (" + str(page) + "/" + str(final_page) + ")...")
            util.log(util.LogLevel.DEBUG, "Found " + str(articles.__len__()) + " posts")

            # Group posts by page (25/page by IC default)
            page_posts: list[Post] = []

            for article in articles:
                util.log(util.LogLevel.DEBUG, "Processing " + str(article))
                author = article.find_element(By.CSS_SELECTOR, "h3.ipsComment_author")
                util.log(util.LogLevel.DEBUG, "Found author element: " + str(author))
                datetime = article.find_element(By.CSS_SELECTOR, "time")
                util.log(util.LogLevel.DEBUG, "Found datetime element: " + str(datetime))
                content = article.find_element(By.CSS_SELECTOR, "div[data-role=\"commentContent\"]")
                util.log(util.LogLevel.DEBUG, "Found content element: " + str(content))

                author_str: str = util.strip_tags(author.get_attribute("innerHTML"))
                author_str = author_str.strip()
                datetime_str = datetime.get_attribute("datetime").strip()
                content_str = content.get_attribute("innerHTML").strip()
                util.log(util.LogLevel.DEBUG, "Converted element contents: author='" + author_str + "', datetime='" + datetime_str + "', content='" + content_str + "'")

                comment_id = article.get_attribute("id").replace("elComment_", "")
                link = page_url + str(page) + "#findComment-" + comment_id
                util.log(util.LogLevel.DEBUG, "Constructed link content: " + link)

                util.log(util.LogLevel.DEBUG, "Constructing post model...")
                # Post construct
                post = Post(author=author_str, datetime=datetime_str, link=link, content=content_str)
                # Crucial
                post.convert_content(self.ic)
                # Add post to page
                page_posts.append(post)

                # User data (if not already in members section)
                if userdata_from_posts and not author_str in messenger.user_data:
                    profile_url = author.find_element(By.XPATH, "//a[img]").get_attribute("href")
                    avatar_url = article.find_element(By.CSS_SELECTOR, f"img[alt=\"{author_str}\"]").get_attribute("src")
                    user_data = UserData(profile_url=profile_url, avatar_url=avatar_url)
                    # Add to messenger construct
                    messenger.user_data[author_str] = user_data
                    util.log(util.LogLevel.DEBUG, f"Extracted user data for {author_str}: " + util.to_json(user_data.__serialize__()))

            # Append page
            messenger.pages.append(page_posts)

            # Update progress
            self.update_progress(float(page/final_page))

            page += 1

            if page > final_page:
                break

        return messenger
