/*
 * This file is part of Invision Archive Tools (IAT).
 *
 * Copyright (C) 2025 Mark Fisher
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see https://www.gnu.org/licenses/.
 */

package com.urbanelf.iat.content.model;

import org.json.JSONObject;

public class UserData {
    private final String profileUrl;
    private String avatarUrl;
    private final String group;
    private final String groupIconUrl;

    public UserData(JSONObject jsonObject) {
        this.profileUrl = jsonObject.getString("profile_url");
        this.avatarUrl = jsonObject.getString("avatar_url");
        this.group = jsonObject.getString("group");
        this.groupIconUrl = jsonObject.getString("group_icon_url");
    }

    public String getProfileUrl() {
        return profileUrl;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getGroup() {
        return group;
    }

    public String getGroupIconUrl() {
        return groupIconUrl;
    }

    @Override
    public String toString() {
        return String.format("""
                {
                    profile_url: %s,
                    avatar_url: %s,
                    group: %s,
                    group_icon_url: %s
                }
                """, profileUrl, avatarUrl, group, groupIconUrl);
    }
}
