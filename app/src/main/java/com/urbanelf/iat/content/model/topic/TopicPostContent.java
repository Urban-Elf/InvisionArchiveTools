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

import com.urbanelf.iat.content.model.PostContent;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

public class TopicPostContent extends PostContent {
    private static final String HEADER_POLL = "poll";
    private static final String HEADER_RECOMMENDED_POSTS = "recommended_posts";
    private static final String HEADER_TOP_POSTERS = "top_posters";

    private final Poll poll;
    private final ArrayList<RecommendedPost> recommendedPosts;
    private final HashMap<String, Integer> topPosters;

    public TopicPostContent(PostContent content, JSONObject header) {
        super(content);
        poll = new Poll(header.getJSONObject(HEADER_POLL));
        // Recommended posts
        recommendedPosts = new ArrayList<>();
        header.getJSONArray(HEADER_RECOMMENDED_POSTS)
                .forEach(jsonPost -> recommendedPosts.add(new RecommendedPost((JSONObject) jsonPost)));
        // Top posters
        topPosters = new HashMap<>();
        final JSONObject topPostersObject = header.getJSONObject(HEADER_TOP_POSTERS);
        topPostersObject.keys().forEachRemaining(key -> topPosters.put(key, topPostersObject.getInt(key)));
    }

    public Poll getPoll() {
        return poll;
    }

    public ArrayList<RecommendedPost> getRecommendedPosts() {
        return recommendedPosts;
    }

    public HashMap<String, Integer> getTopPosters() {
        return topPosters;
    }
}
