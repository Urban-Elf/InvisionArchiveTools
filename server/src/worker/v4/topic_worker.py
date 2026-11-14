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
from selenium.webdriver.support.ui import WebDriverWait
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import *
from ...content.content import Post, Reactor, Reaction, Reactions, UserData
from ...content.topic import Topic, RecommendedPost
from ...content.writers import JSONWriter
from ... import proto_model
from ... import util
from ...shared_constants import WorkerType
from bs4 import BeautifulSoup
from urllib.parse import urlparse, parse_qs
import time
import re

class TopicWorkerV4State:
    SELECT_TOPIC = ICWorkerState(note="Navigate to a topic to begin archiving.",
                                     hint="Locate a topic on the browser, and press 'Next'",
                                     button_configs=[ButtonConfig("Next")])
    ANALYZING = ICWorkerState(note="Preparing...", progressive=True)
    INVALID_TOPIC = ICWorkerState("No topic open. Please try again.",
                                      button_configs=[ButtonConfig("OK")])

class TopicWorkerV4(IC4Worker):
    def __init__(self, worker_id, ic, output_dir):
        super().__init__(worker_id, ic, WorkerType.TOPIC_WORKER, output_dir)

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
                    "//div[@id=\"comments\"]//div[@data-role=\"commentFeed\"]//article")), time=3)
                # Valid session
                break
            except TimeoutException:
                self.set_state(TopicWorkerV4State.INVALID_TOPIC)

        clean_url = re.sub(r"/page/\d+.*$", "", self.driver.current_url)
        #util.log(util.LogLevel.INFO, clean_url)
        self.driver.get(clean_url)
        try:
            self.driver_await(EC.url_contains(clean_url))
        except TimeoutException:
            pass

        # Archive posts
        topic = self.archive_posts(self)

        # Do not proceed with export if early shutdown occurs
        if self.shutdown_event.is_set():
            return

        # Export to temporary JSON file
        path = JSONWriter.write(topic)

        # Notify client
        proto_model.write_packet(proto_model.ServerPacket(self.worker_id, proto_model.ServerSA.RESULT_AVAILABLE)
                                 .add_data("path", path))
        
        self.set_state(SharedState.RESULT_AVAILABLE)

        # Don't shutdown, as client will send shutdown command

    @staticmethod
    def archive_posts(worker: IC4Worker) -> Topic:
        # Preparation
        page = 1
        final_page = 1
        topic_url = util.safe_url_base(worker.driver.current_url)
        page_url = topic_url + "/page/"

        util.log(util.LogLevel.DEBUG, "Base page URL: " + page_url)

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
        worker.set_state(SharedState.ARCHIVING)

        # Get topic meta
        title_str = "Invision Community Topic"
        datetime = ""
        try:
            header = worker.driver.find_element(By.XPATH, "//div[contains(@class, \"ipsPageHeader\")]")
            try:
                title = header.find_element(By.XPATH, "//h1[contains(@class, \"ipsType_pageTitle\")]")
                title_str = util.strip_tags(title.get_attribute("innerHTML")).strip()
            except NoSuchElementException:
                util.log(util.LogLevel.WARNING, "Failed to find topic title, using default.")
            
        except NoSuchElementException:
            util.log(util.LogLevel.WARNING, "Failed to extract metadata header, using defaults.")

        # Get top posters
        top_posters = {}

        try:
            top_poster_elements = worker.driver.find_elements(By.XPATH, "//div[@class='cTopicOverview']//li[@class='cTopicOverview__dataItem']")
            for element in top_poster_elements:
                name = element.find_element(By.XPATH, "//strong/a").text
                count = element.find_element(By.XPATH, "//span").text
                top_posters[name] = count                
        except NoSuchElementException:
            util.log(util.LogLevel.INFO, "Unable to locate top posters container.")


        # Get recommended posts
        recommended_posts: list[RecommendedPost] = []
        
        try:
            pass
        except NoSuchElementException:
            util.log(util.LogLevel.INFO, "No recommended posts found.")

        topic = Topic(title=title_str)

        if len(top_posters) > 0:
            topic.set_top_posters(top_posters)
    
        while not worker.shutdown_event.is_set():
            next_page_url = page_url + str(page) + "/"

            if page > 1:
                worker.driver.get(next_page_url)
                worker.driver_await(EC.url_contains("/page/" + str(page)))

            # Comments feed
            worker.driver_await(EC.presence_of_element_located((By.XPATH, "//div[@id=\"comments\"]//div[@data-role=\"commentFeed\"]")))
            comments_feed = worker.driver.find_element(By.XPATH, "//div[@id=\"comments\"]//div[@data-role=\"commentFeed\"]")

            worker.driver_await(EC.presence_of_all_elements_located((By.CSS_SELECTOR, "article.cPost")))
            articles = comments_feed.find_elements(By.CSS_SELECTOR, "article.cPost")

            util.log(util.LogLevel.INFO, "Deconstructing articles (" + str(page) + "/" + str(final_page) + ")...")

            # Group posts by page (25/page by IC default)
            page_posts: list[Post] = []

            for article_index in range(len(articles)):
                try:
                    # 🔁 Re-fetch the article fresh each iteration
                    articles = worker.driver.find_elements(By.CSS_SELECTOR, "article.cPost")
                    article = articles[article_index]

                    util.log(util.LogLevel.DEBUG, f"Article {article_index} refresh successful")

                    author = article.find_element(By.CSS_SELECTOR, "h3.cAuthorPane_author")
                    datetime_el = article.find_element(By.CSS_SELECTOR, "time")
                    content = article.find_element(By.CSS_SELECTOR, "div[data-role='commentContent']")

                    author_str = util.strip_tags(author.get_attribute("innerHTML")).strip()
                    datetime_str = datetime_el.get_attribute("datetime").strip()
                    content_str = content.get_attribute("innerHTML").strip()

                    comment_id = article.get_attribute("id").replace("elComment_", "")
                    link = f"{page_url}{page}#findComment-{comment_id}"

                    if not author_str in topic.user_data:
                        # Collect user data
                        profile_url = author.find_element(By.CSS_SELECTOR, "a").get_attribute("href")
                        avatar_url = article.find_element(By.CSS_SELECTOR, f"img[alt=\"{author_str}\"]").get_attribute("src")
                        group = article.find_element(By.CSS_SELECTOR, "li[data-role=\"group\"]")
                        group_icon = article.find_element(By.XPATH, "//li[@data-role=\"group-icon\"]//img")
                        group_str = group.get_attribute("innerHTML").strip()
                        group_icon_str = "http:" + group_icon.get_attribute("src").strip()
                        # Create UD
                        user_data = UserData(profile_url=profile_url, avatar_url=avatar_url)
                        user_data.set_group(group_str)
                        user_data.set_group_icon_url(group_icon_str)
                        # Add to messenger construct
                        topic.user_data[author_str] = user_data

                    reactions = Reactions()

                    util.log(util.LogLevel.DEBUG, f"Parsing reactions...")

                    try:
                        # --- Find this article’s reaction container only (anchor by ID!) ---
                        reactions_container = article.find_element(
                            By.XPATH, f".//div[contains(@class, 'ipsReact_blurb')]//ul[contains(@class, 'ipsReact_reactions')]"
                        )

                        # --- Extract overview text dynamically (simple enough) ---
                        overview_el = reactions_container.find_element(
                            By.XPATH, ".//li[contains(@class, 'ipsReact_overview')]"
                        )
                        overview_html = overview_el.get_attribute("innerHTML").strip()
                        reactions.set_overview(overview_html)

                        # --- Snapshot the whole container HTML for static parse ---
                        container_html = reactions_container.get_attribute("innerHTML")
                        soup = BeautifulSoup(container_html, "html.parser")

                        reaction_links = []

                        # --- Parse all <li class="ipsReact_reactCount"> items statically ---
                        for li in soup.select("li.ipsReact_reactCount a"):
                            try:
                                href = li.get("href", "")
                                if not href:
                                    continue

                                # Reaction ID from ?reaction= param
                                parsed = urlparse(href)
                                query = parse_qs(parsed.query)
                                reaction_id = query.get("reaction", [""])[0]

                                # Reaction image + name
                                img_tag = li.select_one("img")
                                reaction_img = img_tag["src"].strip() if img_tag and img_tag.has_attr("src") else ""
                                reaction_name = img_tag["alt"].strip() if img_tag and img_tag.has_attr("alt") else ""

                                # Count (last <span>)
                                count_tag = li.select_one("span:last-child")
                                count = 0
                                if count_tag and count_tag.text.strip().isdigit():
                                    count = int(count_tag.text.strip())

                                reaction_links.append({
                                    "id": reaction_id,
                                    "name": reaction_name,
                                    "img": "https" + reaction_img,
                                    "count": count
                                })
                            except Exception as e:
                                util.log(util.LogLevel.WARNING, f"Failed to parse reaction statically: {e}")
                                continue

                        util.log(util.LogLevel.DEBUG, f"Found {len(reaction_links)} reaction links for comment {comment_id}")

                        # --- Visit each reaction link directly ---
                        for link_data in reaction_links:
                            id = link_data["id"]
                            reaction_name = link_data["name"]
                            reaction_img = link_data["img"]
                            reaction_count = link_data["count"]

                            util.log(util.LogLevel.INFO, f"Fetching reactors for '{reaction_name}'")

                            reactors = []
                            page = 1

                            while not worker.shutdown_event.is_set():
                                paged_url = page_url + str(page) + f"/?do=showReactionsComment&comment={comment_id}&reaction={id}"
                                worker.driver.get(paged_url)
                                
                                reactor_elements_xpath = "//ol[@itemtype='http://schema.org/ItemList']//li"
                                worker.driver_await(EC.presence_of_element_located((By.XPATH, reactor_elements_xpath)))

                                # Check if there’s actually a reactor list on this page
                                try:
                                    reactor_elements = worker.driver.find_elements(By.XPATH, reactor_elements_xpath)
                                    if not reactor_elements:
                                        util.log(util.LogLevel.DEBUG, f"No reactors found on page {page} for {reaction_name}. Assuming last page.")
                                        break

                                    for li in reactor_elements:
                                        try:
                                            name_el = li.find_element(By.XPATH, ".//h3/a")
                                            time_el = li.find_element(By.XPATH, ".//time")
                                            reactors.append(Reactor(
                                                name=name_el.text.strip(),
                                                datetime=time_el.get_attribute("datetime").strip()
                                            ))
                                        except Exception:
                                            continue

                                    # Look for pagination next
                                    try:
                                        next_li = worker.driver.find_element(By.XPATH, "//li[contains(@class,'ipsPagination_next')]")
                                        if "ipsPagination_inactive" in next_li.get_attribute("class"):
                                            break  # invisible next button = end
                                        page += 1
                                    except NoSuchElementException:
                                        break  # Shouldn't reach here, but just in case

                                except NoSuchElementException:
                                    break  # end of pagination

                            reaction = Reaction(
                                image=reaction_img,
                                name=reaction_name,
                                count=reaction_count,
                                reactors=reactors
                            )
                            reactions.add_reaction(reaction)
                            util.log(util.LogLevel.DEBUG, f"Added reaction: {reaction.__serialize__()}")

                            # --- Return to the original article page ---
                            worker.driver.get(page_url + str(page) + "/")
                            worker.driver_await(EC.presence_of_element_located((By.CSS_SELECTOR, "article.cPost")))
                    except NoSuchElementException:
                        util.log(util.LogLevel.INFO, "No reactions section found.")

                    # Post construct
                    post = Post(author=author_str, datetime=datetime_str, link=link, content=content_str)
                    # Crucial to convert content (quotes, relative resources, etc)
                    post.convert_content(worker.ic)
                    # Add reactions to post
                    post.set_reactions(reactions)
                    # Add post to page
                    page_posts.append(post)

                except StaleElementReferenceException:
                    util.log(util.LogLevel.WARNING, f"Article {article_index} went stale", error_trace=True)
                    continue

            # Append page
            topic.pages.append(page_posts)

            # Update progress
            worker.update_progress(float(page/final_page))

            page += 1

            if page > final_page:
                break

        return topic
