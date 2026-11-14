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

import com.urbanelf.iat.proto.constants.ContentType;
import com.urbanelf.iat.util.FlatResource;

import org.json.JSONObject;

import java.nio.file.Path;
import java.util.HashMap;

public class Meta implements FlatResource {
    /** Accessed preemptively by {@link com.urbanelf.iat.content.parser.ParserDispatcher for resolving content parserse} */
    public static final String CONTENT_TYPE = "type";

    private static final String TITLE = "title";
    private static final String USER_DATA = "user_data";

    private final ContentType contentType;
    private final String title;
    private final HashMap<String, UserData> userData;

    public Meta(JSONObject json) {
        contentType = json.getEnum(ContentType.class, CONTENT_TYPE);
        title = json.getString(TITLE);
        userData = new HashMap<>();
        // Parse user data
        final JSONObject userDataObject = json.getJSONObject(USER_DATA);
        userDataObject.keys().forEachRemaining(
                key -> userData.put(key,
                        new UserData(userDataObject.getJSONObject(key))));
    }

    public Meta(Meta meta) {
        contentType = meta.contentType;
        title = meta.title;
        userData = meta.userData;
    }

    @Override
    public void flatten(Path resPath) {
        // Flatten UD
        userData.values().forEach(value -> value.flatten(resPath));
    }

    public ContentType getContentType() {
        return contentType;
    }

    public String getTitle() {
        return title;
    }

    public HashMap<String, UserData> getUserData() {
        return userData;
    }
}
