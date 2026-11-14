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

package com.urbanelf.iat.content.writer.html;

import com.urbanelf.iat.content.model.Meta;
import com.urbanelf.iat.content.model.UserData;
import com.urbanelf.iat.content.parser.ContentSpec;
import com.urbanelf.iat.content.parser.PageParser;
import com.urbanelf.iat.content.writer.WriterTracker;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.NumberUtils;

import org.json.JSONObject;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class MessengerHTMLWriter extends HTMLWriter {
    private static final String RES_PATH = PATH_PREFIX + "messenger/res";
    // Path prefix is automatically prepended by engine ↓
    private static final String INDEX_TEMPLATE_PATH = "messenger/index";
    private static final String PAGE_TEMPLATE_PATH =  "messenger/page";

    @Override
    public Path writeHTML(TemplateEngine engine, ContentSpec spec, Path dst, WriterTracker tracker) throws IOException {
        // Content should always be PostContent for messengers

        Path path = spec.path();
        Meta meta = spec.meta();

        // Establish destination directory
        final Path dstPath = FileTree.resolveDestinationDirectory(dst);
        // Build hierarchy
        Files.createDirectories(dstPath);
        final Path resPath = dstPath.resolve( "res");
        Files.createDirectories(resPath);
        final Path pagePath = dstPath.resolve("page");
        Files.createDirectories(pagePath);

        // Create context
        final Context context = new Context();

        // Generic access fields
        final HashMap<String, UserData> userData = meta.getUserData();

        // Process resources (localize avatar paths, etc)
        meta.flatten(resPath);

        // Populate generic fields
        context.setVariable("title", meta.getTitle());
        context.setVariable("userData", userData);

        // Render page partials
        PageParser pageParser = spec.meta().getContentType().getPageParser();
        long totalPages = pageParser.countPages(path);
        final AtomicLong counter = new AtomicLong(1);

        // Tracker scoring
        AtomicLong trackerScore = new AtomicLong();
        final long trackerScoreTotal = totalPages + 1; // +1 is res copy & index render
        tracker.maximumCalculated(trackerScoreTotal);

        // Statistics
        int totalPostCount = Math.toIntExact(totalPages * POSTS_PER_PAGE);
        if (spec.postCount() != null)
            totalPostCount = spec.postCount().count();

        final String statistics = userData.size()
                + " participant" + (userData.size() == 1 ? "" : "s")
                + " • " + NumberUtils.formatDelimiter(totalPostCount) + " posts";
        // Set variable
        context.setVariable("stats", statistics);

        try {
            tracker.messageUpdate("Writing pages...");

            pageParser.parseDirectory(path, page -> {
                // Manifest
                final JSONObject manifest = new JSONObject();
                manifest.put("totalPages", totalPages);
                manifest.put("page", counter.get());
                context.setVariable("manifest", manifest.toString(4));

                // Update page
                context.setVariable("posts", page);

                // Render page
                try (BufferedWriter writer = Files.newBufferedWriter(pagePath.resolve(counter.get() + ".html"))) {
                    engine.process(PAGE_TEMPLATE_PATH, context, writer);
                } catch (IOException e) {
                    // Smuggle exception
                    throw new RuntimeException(e);
                }

                // Increment counter
                counter.getAndIncrement();

                // Tracking score
                tracker.messageUpdate("Writing pages... (" + (int) (100L * counter.get() / totalPages) + "%)");

                trackerScore.getAndIncrement();
                tracker.progressChanged(trackerScore.get());
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause(); // unwrap it
            }
            throw ex;
        }

        tracker.messageUpdate("Copying resources...");
        // Copy resources (COMMENT OUT WHILE DEBUGGING THE JS/CSS)
        copyResources(RES_PATH, resPath);

        tracker.messageUpdate("Writing index...");
        // Render template
        try (BufferedWriter writer = Files.newBufferedWriter(dstPath.resolve(dst.getFileName()))) {
            engine.process(INDEX_TEMPLATE_PATH, new Context(), writer);
        }

        // Tracking score update (final)
        trackerScore.getAndIncrement();
        tracker.progressChanged(trackerScore.get());

        tracker.completed();

        return dstPath;
    }
}
