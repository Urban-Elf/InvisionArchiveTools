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

import com.urbanelf.iat.content.model.Content;
import com.urbanelf.iat.content.model.PostContent;
import com.urbanelf.iat.content.model.topic.TopicPostContent;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;

public class TopicPostParser implements Parser {
    @Override
    public Content parse(JSONObject header, BufferedReader reader) throws IOException {
        // Parse header (handled by content)
        final PostContent content = new PostContent(header);
        // Further header parsing
        return new TopicPostContent(content, header);
    }
}
