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

package com.urbanelf.iat.content.parser;

import com.urbanelf.iat.content.Content;
import com.urbanelf.iat.content.PostContent;
import com.urbanelf.iat.content.struct.Post;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

public class PostParser implements Parser {
    @Override
    public Content parse(JSONObject header, BufferedReader reader) throws IOException {
        // Parse header (handled by content)
        final PostContent content = new PostContent(header);

        // Parse pages (NDJSON)
        String line;
        while ((line = reader.readLine()) != null) {
            final JSONObject pageObject = new JSONObject(line);
            final JSONArray postsObject = pageObject.getJSONArray(Content.PAGE_CONTENT);
            final ArrayList<Post> page = new ArrayList<>();
            postsObject.forEach(o -> page.add(new Post((JSONObject) o)));
            content.getPages().add(page);
        }

        return content;
    }
}
