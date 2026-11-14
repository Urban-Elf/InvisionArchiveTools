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

import com.urbanelf.iat.Core;
import com.urbanelf.iat.content.model.Post;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public abstract class PageParser {
    public static final String TAG = PageParser.class.getSimpleName();

    protected static final String PAGE_CONTENT = "content";
    private static final Pattern PAGE_PATTERN = Pattern.compile("^page_(\\d+)\\.json$");

    public long countPages(Path path) throws IOException {
        try (Stream<Path> paths = Files.list(path)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(p -> PAGE_PATTERN.matcher(p.getFileName().toString()).matches())
                    .count();
        }
    }

    public void parseDirectory(Path path, Consumer<ArrayList<Post>> consumer) throws IOException {
        try (Stream<Path> pageFiles = Files.list(path)
                .filter(Files::isRegularFile)
                .filter(p -> PAGE_PATTERN.matcher(p.getFileName().toString()).matches())
                .sorted(Comparator.comparingLong(p -> {
                    Matcher matcher = PAGE_PATTERN.matcher(p.getFileName().toString());
                    matcher.matches();
                    return Long.parseLong(matcher.group(1));
                }))) {
            pageFiles.forEach(pagePath -> {
                try {
                    ArrayList<Post> posts = parse(pagePath);
                    consumer.accept(posts);
                } catch (IOException | ArchiveParseException e) {
                    Core.error(TAG, "Failed to parse page " + pagePath.toString(), e);
                }
            });
        }
    }

    public abstract ArrayList<Post> parse(Path pagePath) throws IOException, ArchiveParseException;
}
