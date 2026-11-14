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

import com.urbanelf.iat.content.model.Meta;
import com.urbanelf.iat.content.model.PostCount;
import com.urbanelf.iat.content.model.RefreshSpec;
import com.urbanelf.iat.ic.ICUnsupportedException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class SpecParser {
    protected JSONObject parseJsonFile(Path path) throws IOException, JSONException {
        BufferedReader reader = Files.newBufferedReader(path);
        String line = reader.readLine();
        if (line == null)
            throw new JSONException("input line is null (cannot parse)");
        JSONObject json = new JSONObject(line);
        // Close reader
        reader.close();
        return json;
    }

    protected Meta parseMetadata(Path path) throws IOException, JSONException {
        return new Meta(parseJsonFile(path));
    }
    public RefreshSpec parseRefreshSpec(Path path) throws IOException, JSONException, ICUnsupportedException {
        return new RefreshSpec(parseJsonFile(path));
    }
    public PostCount parsePostCount(Path path) throws IOException, JSONException {
        return new PostCount(parseJsonFile(path));
    }
}
