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
import java.util.ArrayList;
import java.util.HashMap;

@Deprecated
public class PostContent implements Content, FlatResource {
    public static final String HEADER_USER_DATA = "user_data";

    private final String title;
    private final HashMap<String, UserData> userData;
    private final ArrayList<ArrayList<? extends Post>> pages;

    public PostContent(JSONObject header) {
        this.title = header.getString(HEADER_TITLE);
        this.userData = new HashMap<>();
        // Parse user data
        final JSONObject userDataObject = header.getJSONObject(HEADER_USER_DATA);
        userDataObject.keys().forEachRemaining(
                key -> userData.put(key,
                        new UserData(userDataObject.getJSONObject(key))));
        // Pages are populated by the NDJSON SpecParser
        this.pages = new ArrayList<>();
    }

    public PostContent(PostContent content) {
        this.title = content.title;
        this.userData = content.userData;
        this.pages = content.pages;
    }

    @Override
    public void flatten(Path resPath) {
        // Flatten UD
        userData.values().forEach(value -> value.flatten(resPath));

        // Flatten posts
        //FIXME: pages.forEach(page ->
        //        page.forEach(post -> post.flatten(resPath)));
    }

    @Override
    public String getTitle() {
        return title;
    }

    public HashMap<String, UserData> getUserData() {
        return userData;
    }

    public ArrayList<ArrayList<? extends Post>> getPages() {
        return pages;
    }
}
