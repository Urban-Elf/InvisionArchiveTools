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

package com.urbanelf.iat.content.struct;

import org.json.JSONObject;

public class Post {
    private final String author;
    private final String dateTime;
    private final String link;
    private final String content;

    public Post(JSONObject jsonObject) {
        this.author = jsonObject.getString("author");
        this.dateTime = jsonObject.getString("datetime");
        this.link = jsonObject.getString("link");
        this.content = jsonObject.getString("content");
    }

    public String getAuthor() {
        return author;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getLink() {
        return link;
    }

    public String getContent() {
        return content;
    }
}
