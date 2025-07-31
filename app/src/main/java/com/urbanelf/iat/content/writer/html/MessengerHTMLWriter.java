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

import com.urbanelf.iat.Core;
import com.urbanelf.iat.content.Content;
import com.urbanelf.iat.content.PostContent;
import com.urbanelf.iat.content.struct.Post;
import com.urbanelf.iat.content.struct.UserData;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.NumberUtils;
import com.urbanelf.iat.util.ResourceUtils;

import org.json.JSONObject;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MessengerHTMLWriter extends HTMLWriter {
    private static final String RES_PATH = PATH_PREFIX + "messenger/res";
    // Path prefix is automatically prepended by engine ↓
    private static final String INDEX_TEMPLATE_PATH = "messenger/index";
    private static final String PAGE_TEMPLATE_PATH =  "messenger/page";

    @Override
    public File writeHTML(TemplateEngine engine, Content content, File dst) throws IOException {
        // Content should always be PostContent for messengers
        final PostContent postContent = (PostContent) content;

        // Establish destination directory
        final File dstDirectory = resolveDestinationDirectory(dst);
        // Build hierarchy
        dstDirectory.mkdirs();
        final File resDirectory = new File(dstDirectory, "res");
        resDirectory.mkdir();
        final File pageDirectory = new File(dstDirectory, "page");
        pageDirectory.mkdir();

        // Create context
        final Context context = new Context();

        // Generic access fields
        final ArrayList<ArrayList<? extends Post>> pages = postContent.getPages();
        final HashMap<String, UserData> userData = postContent.getUserData();

        // Populate generic fields
        context.setVariable("title", postContent.getTitle());
        context.setVariable("userData", userData);

        // Statistics
        final int totalPosts = pages.stream()
                .mapToInt(List::size)
                .sum();
        final String statistics = userData.size()
                + " participant" + (userData.size() == 1 ? "" : "s")
                + " • " + NumberUtils.formatDelimiter(totalPosts) + " posts";
        // Set variable
        context.setVariable("stats", statistics);

        // Render page partials
        try {
            final AtomicInteger counter = new AtomicInteger(1);
            pages.forEach(page -> {
                // Manifest
                final JSONObject manifest = new JSONObject();
                manifest.put("totalPages", pages.size());
                manifest.put("page", counter.get());
                context.setVariable("manifest", manifest.toString(4));

                // Update page
                context.setVariable("posts", page);

                // Render page
                try (Writer writer = new FileWriter(new File(pageDirectory, counter.get() + ".html"))) {
                    engine.process(PAGE_TEMPLATE_PATH, context, writer);
                } catch (IOException e) {
                    // Smuggle exception
                    throw new RuntimeException(e);
                }

                // Increment counter
                counter.getAndIncrement();
            });
        } catch (RuntimeException ex) {
            if (ex.getCause() instanceof IOException) {
                throw (IOException) ex.getCause(); // unwrap it
            }
            throw ex;
        }

        // Copy resources (COMMENT OUT WHILE DEBUGGING JS/CSS)
        try {
            final List<String> index = ResourceUtils.loadResourceIndex(RES_PATH, "index.txt");
            ResourceUtils.copyResources(RES_PATH, index, resDirectory.toPath());
            //FileTree.copyDir(Path.of(ClassLoader.getSystemResource(RES_PATH).toURI()),
            //        resDirectory.toPath());
        } catch (IOException e) {
            Core.fatal(TAG, "Failed to resolve internal resources", e);
            throw new RuntimeException(e);
        }

        // Render template
        try (Writer writer = new FileWriter(new File(dstDirectory, dst.getName()))) {
            engine.process(INDEX_TEMPLATE_PATH, new Context(), writer);
        }

        return dstDirectory;
    }
}
