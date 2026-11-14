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

import com.urbanelf.iat.content.model.Post;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class MessengerPageParser extends PageParser {
    @Override
    public ArrayList<Post> parse(Path pagePath) throws IOException, ArchiveParseException {
        try (BufferedReader reader = Files.newBufferedReader(pagePath)) {
            String line = reader.readLine();
            if (line == null)
                throw new ArchiveParseException("page parse failed: input is null");
            final JSONObject json = new JSONObject(line);
            final JSONArray postsObject = json.getJSONArray(PAGE_CONTENT);
            final ArrayList<Post> page = new ArrayList<>();
            postsObject.forEach(o -> page.add(new Post((JSONObject) o)));
            return page;
        } catch (JSONException e) {
            throw new ArchiveParseException("page parse failed: corrupt json");
        }
    }
}
