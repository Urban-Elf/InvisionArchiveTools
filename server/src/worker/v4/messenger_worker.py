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

from ...worker.ic_worker import IC4Worker, RefreshSpec
from ...worker.state.worker_state import *
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import *
from ...content.content import Post, UserData, PageContent
from ...content.messenger import MessengerMeta
from ...content.writers import JSONWriter
from ... import proto_model
from ... import util
from ...shared_constants import ButtonCallbackSA
import re
from pathlib import Path

class MessengerWorkerV4State:
    SELECT_MESSENGER = ICWorkerState(note="Select a messenger to archive.",
                                     hint="Click on a messenger in the browser and press 'Next'",
                                     button_configs=[ButtonConfig("Next")])
    ANALYZING = ICWorkerState(note="Preparing...")
    INVALID_MESSENGER = ICWorkerState("No messenger selected. Please try again.",
                                      button_configs=[ButtonConfig("OK")])
    SPEC_CORRUPT = ICWorkerState("Spec is corrupt! Contact developer.",
                                      button_configs=[ButtonConfig("Close", shared_action=ButtonCallbackSA.TERMINATE)])


def locate_input_page(directory: Path) -> int:
    if not directory.is_dir():
        raise NotADirectoryError(f"Not a directory: {directory}")

    pattern = re.compile(r"^page_(\d+)\.json$")
    numbers = [
        int(m.group(1))
        for f in directory.iterdir()
        if (m := pattern.match(f.name))
    ]

    if not numbers:
        raise FileNotFoundError(f"No page_x.json files found in {directory}")

    return max(numbers)


class MessengerWorkerV4(IC4Worker):
    def __init__(self, worker_id, ic, output_dir):
        super().__init__(worker_id, ic, output_dir)
        self.page = 1
        self.metadata: MessengerMeta = None
        self.userdata_from_posts: bool = False

    # TODO: Might want to make an execute_start and an execute_refresh to have cleaner code and one r_spec check in execute
    def execute(self):
        loaded_from_spec = self._r_spec != None

        util.log(util.LogLevel.INFO, "Starting MessengerWorkerV4 with output_dir: " + str(self.output_dir) + ", loaded_from_spec: " + str(loaded_from_spec))

        # If this worker was launched in refresh mode, skip selection phase
        if loaded_from_spec:
            if self.ic.is_messenger(self._r_spec.refresh_url):
                util.log(util.LogLevel.INFO, "Refreshing messenger: " + self._r_spec.refresh_url)
                self.driver.get(self._r_spec.refresh_url)
                try:
                    self.driver_await(EC.url_contains(self._r_spec.refresh_url))
                except TimeoutException:
                    pass
            else:
                util.log(util.LogLevel.WARNING, "Refresh spec URL is not a messenger: " + self._r_spec.refresh_url + " Cannot proceed.")
                self.set_state(MessengerWorkerV4State.SPEC_CORRUPT)
                return
        else:
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
        #messenger = self.archive_posts()
        self.archive_posts(loaded_from_spec)

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

        # Export to temporary JSON file (DEPRECATED)
        #path = JSONWriter.write(messenger)

        # Notify client
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.RESULT_AVAILABLE)
                                 .add_data("path", str(self.output_dir)))
        
        self.set_state(SharedState.RESULT_AVAILABLE)

        # Don't shutdown, as client will send shutdown command

    def archive_posts(self, loaded_from_spec):
        # Preparation
        self.page = 1
        if loaded_from_spec:
            persist_page = locate_input_page(self.output_dir)
            self.page = persist_page + 1

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

        # Early exit condition for refreshing finished workers
        if loaded_from_spec and self.page == final_page:
            util.log(util.LogLevel.INFO, "Refresh spec indicates messenger is already fully archived. Reporting completion.")
            return

        ### PREPARE METADATA ###

        self.metadata = None

        # Retrieve from refresh spec if present (refresh mode), otherwise extract from page
        if loaded_from_spec:
            self.metadata = self.load_metadata()
        else:
            title_str = "Invision Community Messenger"
            self.userdata_from_posts = False

            # Get messenger title
            try:
                title = self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[contains(@class, \"ipsPageHeader\")]//h1[contains(@class, \"ipsType_pageTitle\")]")
                title_str = util.strip_tags(title.get_attribute("innerHTML")).strip()
            except NoSuchElementException:
                util.log(util.LogLevel.WARNING, "Failed to find topic title, using default.")
                pass

            # Construct metadata object
            self.metadata = MessengerMeta(title=title_str)

            # Extract userdata
            try: 
                members_container = self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[contains(@class, \"cMessage_members\")]")
                members = members_container.find_elements(By.XPATH, "//ol//li[contains(@class, \"ipsPhotoPanel\")]")
                for member in members:
                    profile_url = member.find_element(By.XPATH, ".//a").get_attribute("href")
                    avatar = member.find_element(By.XPATH, ".//a//img")
                    author_str = avatar.get_attribute("alt").strip()
                    avatar_url = avatar.get_attribute("src").strip()
                    userdata = UserData(profile_url=profile_url, avatar_url=avatar_url)
                    # Add to messenger construct
                    self.metadata.userdata[author_str] = userdata
            except NoSuchElementException:
                # No members section, try to extract from posts
                util.log(util.LogLevel.WARNING, "No members section found, extracting user data from posts.")
                self.userdata_from_posts = True

            self.write_metadata(self.metadata)

        # Refresh spec can now be written
        if not loaded_from_spec:
            self.stored_r_spec = self.create_refresh_spec()
            if self.stored_r_spec is not None:
                self.write_refresh_spec(self.stored_r_spec)

        ### Archive ###
        self.set_state(SharedState.ARCHIVING)

        # Set flag
        if loaded_from_spec and self._r_spec.userdata_from_posts:
            self.userdata_from_posts = True

        while not self.shutdown_event.is_set():
            next_page_url = page_url + str(self.page) + "/"

            if self.page > 1:
                self.driver.get(next_page_url)
                self.driver_await(EC.url_contains("?page=" + str(self.page)))
                #li.ipsPagination_pageJump <- check value later ig

            # Comments feed
            self.driver_await(EC.presence_of_element_located((By.XPATH, "//div[@id=\"elMessageViewer\"]//div[@data-role=\"commentFeed\"]")))
            comments_feed = self.driver.find_element(By.XPATH, "//div[@id=\"elMessageViewer\"]//div[@data-role=\"commentFeed\"]")

            self.driver_await(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "article")))
            articles = comments_feed.find_elements(By.CSS_SELECTOR, "article")

            util.log(util.LogLevel.INFO, "Deconstructing articles (" + str(self.page) + "/" + str(final_page) + ")...")
            util.log(util.LogLevel.DEBUG, "Found " + str(articles.__len__()) + " posts")

            # Group posts by page (25/page by IC default)
            page_obj = PageContent()

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
                link = page_url + str(self.page) + "#findComment-" + comment_id
                util.log(util.LogLevel.DEBUG, "Constructed link content: " + link)

                util.log(util.LogLevel.DEBUG, "Constructing post model...")
                # Post construct
                post = Post(author=author_str, datetime=datetime_str, link=link, content=content_str)
                # Crucial
                post.convert_content(self.ic)
                # Add post to page
                page_obj.posts.append(post)

                # User data (if not already in members section)
                if self.userdata_from_posts and not author_str in self.metadata.userdata:
                    profile_url = author.find_element(By.XPATH, "//a[img]").get_attribute("href")
                    avatar_url = article.find_element(By.CSS_SELECTOR, f"img[alt=\"{author_str}\"]").get_attribute("src")
                    user_data = UserData(profile_url=profile_url, avatar_url=avatar_url)
                    # Add to messenger construct
                    self.metadata.userdata[author_str] = user_data
                    util.log(util.LogLevel.DEBUG, f"Extracted user data for {author_str}: " + util.to_json(user_data.__serialize__()))
                    # Update metadata every time
                    self.write_metadata(self.metadata)

            # Write page
            path = self.output_dir / f"page_{self.page}.json"
            if not path.exists():
                JSONWriter.write_ndjson(page_obj, path)
            else:
                util.log(util.LogLevel.WARNING, "Page file already exists, skipping write: " + str(path))

            # Append page (deprecated)
            #messenger.pages.append(page_obj.posts)

            # Update progress
            self.update_progress(float(self.page/final_page))

            self.page += 1

            if self.page > final_page:
                break

    def create_refresh_spec(self):
        if self.ic.is_messenger(self.driver.current_url):
            messenger_url = util.safe_url_base(self.driver.current_url)
            r_spec = RefreshSpec(messenger_url, self.userdata_from_posts)
            util.log(util.LogLevel.INFO, "Created refresh spec for messenger: " + messenger_url)
            return r_spec
        util.log(util.LogLevel.WARNING, "Failed to create refresh spec: current URL is not a messenger: " + self.driver.current_url)
        return None
    
    def write_refresh_spec(self, r_spec: RefreshSpec):
        # Write refresh spec to file
        path = self.output_dir / "refresh_spec.json"
        if path.exists():
            util.log(util.LogLevel.INFO, "Refresh spec file already exists, skipping write.")
            return
        JSONWriter.write_json(r_spec, path)
        util.log(util.LogLevel.INFO, "Wrote refresh spec to " + str(path))

    def load_metadata(self) -> MessengerMeta:
        # Load metadata from file
        path = self.output_dir / "metadata.json"
        if not path.exists():
            util.log(util.LogLevel.WARNING, "Metadata file does not exist: " + str(path))
            return None
        meta = MessengerMeta.deserialize(path)
        util.log(util.LogLevel.INFO, "Loaded metadata from " + str(path))
        return meta

    def write_metadata(self, meta: MessengerMeta):
        # Write metadata to file
        path = self.output_dir / "metadata.json"
        JSONWriter.write_json(meta, path)
        util.log(util.LogLevel.INFO, "Wrote metadata to " + str(path))
