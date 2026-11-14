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

// FIXME: Future notice: this class is not to be mistaken for a subclass of iat.content.model.Meta
public class TopicMeta {
    private final String title;
    private final boolean pinned;
    private final String author;
    private final String datetime;
    private final String forum;
    private final String forumUrl;

    public TopicMeta(JSONObject jsonObject) {
        this.title = jsonObject.getString("title");
        this.pinned = jsonObject.getBoolean("pinned");
        this.author = jsonObject.getString("author");
        this.datetime = jsonObject.getString("datetime");
        this.forum = jsonObject.getString("forum");
        this.forumUrl = jsonObject.getString("forum_url");
    }

    public String getTitle() {
        return title;
    }

    public boolean isPinned() {
        return pinned;
    }

    public String getAuthor() {
        return author;
    }

    public String getDatetime() {
        return datetime;
    }

    public String getForum() {
        return forum;
    }

    public String getForumUrl() {
        return forumUrl;
    }
}
