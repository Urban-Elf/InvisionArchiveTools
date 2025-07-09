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

from worker.ic_worker import IC4Worker
from worker.state.worker_state import *
from selenium.webdriver.support import expected_conditions as EC
from selenium.webdriver.common.by import By
from selenium.common.exceptions import *
from worker.ic_exception import ICWorkerException
import util
import re

class MessengerWorkerState:
    SELECT_MESSENGER = ICWorkerState(note="Select a messenger to archive.",
                                     hint="Click on a messenger in the browser and press 'Next'",
                                     button_configs=[ButtonConfig("Next")])
    ANALYZING = ICWorkerState(note="Preparing...")
    INVALID_MESSENGER = ICWorkerState("No messenger selected. Please try again.",
                                      button_configs=[ButtonConfig("OK")])
    ARCHIVING = ICWorkerState(note="Archiving... This may take a while.",
                              hint="Do not use the browser.",
                              indeterminate=False)


class MessengerWorkerV4(IC4Worker):
    def __init__(self, worker_id, ic, result_consumer):
        super().__init__(worker_id, ic, result_consumer)

    def execute(self):
        # Select messenger
        while not self.shutdown_event.is_set():
            self.driver.get(self.ic.messenger())
            self.set_state(MessengerWorkerState.SELECT_MESSENGER)

            self.set_state(MessengerWorkerState.ANALYZING)
            try:
                self.driver_await(EC.presence_of_element_located((By.XPATH,
                    "//*[@id=\"elMessageViewer\"]//*[contains(text(), \"No message selected\")]")), time=5)
            except TimeoutException:
                # Valid session
                break
            self.set_state(MessengerWorkerState.INVALID_MESSENGER)

        # Archive posts

    def archive_posts(self):
        page = 1
        page_url = self.driver.current_url + "page/"
        final_page_url = None

        try:
            pagination_last = self.driver.find_element(By.CSS_SELECTOR, "ul.ipsPagination > .ipsPagination_last > a")
            final_page_url = re.sub(r"#.*", "", pagination_last.get_attribute("href"))
        except NoSuchElementException as e:
            raise ICWorkerException("Could not assess pagination length of messenger (" + page_url + ")")

        while not self.shutdown_event.is_set():
            next_page_url = page_url + str(page) + "/"

            if page > 1:
                self.driver.get(next_page_url)
                self.driver_await(self.driver, EC.url_changes(page_url + str(page) + "/"))

            #try:
            #    self.driver_await(self.driver,
            #        EC.presence_of_element_located((By.CSS_SELECTOR, ".ipsResponsive_pull[data-tableid=\"topics\"]")))
            #except TimeoutException:
            #    print("Forum empty (topics)")
            #    break # No topics found
                
            #topic_table = self.driver.find_element(By.CSS_SELECTOR, ".ipsResponsive_pull[data-tableid=\"topics\"]")
            
            #try:
            #    self.driver_await(self.driver, EC.all_of(
            #        EC.presence_of_all_elements_located((By.CSS_SELECTOR, "h4.ipsDataItem_title"))))
            #except TimeoutException:
            #    print("Forum empty (container)")
            #    break # No topics found

            #_topics = topic_table.find_elements(By.CSS_SELECTOR, "h4.ipsDataItem_title")

            #for _topic in _topics:
                #topic_obj = Topic(_topic)
                #topics.append(topic_obj)
                #print("Found topic " + topic_obj.get_name() + " (" + self.ips.id + ")")

            page += 1

            if final_page_url == None or next_page_url == final_page_url:
                break










