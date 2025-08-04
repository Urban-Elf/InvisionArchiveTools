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

package com.urbanelf.iat.content.model.topic;

import org.json.JSONObject;

public class RecommendedPost {
    private final String author;
    private final String contentPreview;
    private final String link;

    public RecommendedPost(JSONObject jsonObject) {
        author = jsonObject.getString("author");
        contentPreview = jsonObject.getString("contentPreview");
        link = jsonObject.getString("link");
    }

    public String getAuthor() {
        return author;
    }

    public String getContentPreview() {
        return contentPreview;
    }

    public String getLink() {
        return link;
    }
}
