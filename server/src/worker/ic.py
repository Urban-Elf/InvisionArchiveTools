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

class IC:
    def __init__(self, root_url):
        self.root_url = root_url

    def auth(self):
        """Returns a URL to the login page for the community."""
        raise NotImplementedError()

    def messenger(self):
        """Returns a URL to the messenger app on the community."""
        raise NotImplementedError()
    
    def profile(self, user_id: str):
        """Returns a URL to the profile page for the given user ID."""
        raise NotImplementedError()

    def get_version_number(self):
        raise NotImplementedError()

    @staticmethod
    def PHP_data(url: str, data: str):
        url_suffix = ""
        if not url.endswith("/"):
            url_suffix = "/"
        data_prefix = ""
        if not data.startswith("?"):
            data_prefix = "?"
        return url + url_suffix + data_prefix + data

class IC4(IC):
    def __init__(self, json_data):
        super().__init__(json_data)
        self.auth_url = self.root_url + "/login"
        self.messenger_url = self.root_url + "/messenger"

    def auth(self):
        return self.auth_url
    
    def messenger(self):
        return self.messenger_url
    
    def profile(self, user_id: str, username: str):
        return f"{self.root_url}/profile/{user_id}-{username.lower()}"

    def get_version_number(self):
        return 4
    
class IC5(IC):
    def __init__(self, json_data):
        super().__init__(json_data)
        self.auth_url = self.root_url + "/login"
        self.messenger_url = self.root_url + "/messenger"

    def auth(self):
        return self.auth_url
    
    def messenger(self):
        return self.messenger_url
    
    def profile(self, user_id, username):
        return f"{self.root_url}/profile/{user_id}-{username}"

    def get_version_number(self):
        return 5