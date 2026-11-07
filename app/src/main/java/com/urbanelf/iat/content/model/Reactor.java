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

import com.urbanelf.iat.util.FlatResource;
import com.urbanelf.iat.util.FlatResourceUtils;

import org.json.JSONObject;

import java.nio.file.Path;

public class Reactor implements FlatResource {
    private String avatarUrl;
    private final String name;
    private final String datetime;

    public Reactor(JSONObject jsonObject) {
        avatarUrl = jsonObject.getString("avatar_url");
        name = jsonObject.getString("name");
        datetime = jsonObject.getString("datetime");
    }

    @Override
    public void flatten(Path resPath) {
        avatarUrl = FlatResourceUtils.flattenResource(avatarUrl, resPath);
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public String getName() {
        return name;
    }

    public String getDatetime() {
        return datetime;
    }
}
