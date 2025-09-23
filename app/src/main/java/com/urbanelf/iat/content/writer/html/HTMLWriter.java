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
import com.urbanelf.iat.content.model.Content;
import com.urbanelf.iat.content.model.UserData;
import com.urbanelf.iat.content.writer.Writer;
import com.urbanelf.iat.util.ResourceUtils;
import com.urbanelf.iat.util.URLUtils;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public abstract class HTMLWriter extends Writer {
    protected static final String TAG = HTMLWriter.class.getSimpleName();

    protected static final String PATH_PREFIX = "templates/html/";

    private static final TemplateEngine TEMPLATE_ENGINE;

    static {
        TEMPLATE_ENGINE = new TemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix(PATH_PREFIX);
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        TEMPLATE_ENGINE.setTemplateResolver(resolver);
    }

    @Override
    protected File write(Content content, File dst) throws IOException {
        return writeHTML(TEMPLATE_ENGINE, content, dst);
    }

    public abstract File writeHTML(TemplateEngine engine, Content content, File dst) throws IOException;

    protected void processUserData(HashMap<String, UserData> userData, Path resPath) {
        userData.forEach((key, value) -> {
            final String avatarName = URLUtils.getResourceName(value.getAvatarUrl());
            final HttpClient client = HttpClient.newHttpClient();
            final HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(value.getAvatarUrl()))
                    .build();
            try {
                client.send(request, HttpResponse.BodyHandlers.ofFile(resPath.resolve(avatarName)));
            } catch (IOException | InterruptedException e) {
                Core.warning(TAG, "");
                return; // Continue loop
            }
            // Update user data
            value.setAvatarUrl(Paths.get("..", resPath.getFileName().toString(), avatarName).toString());
        });
    }

    protected void copyResources(String srcPath, Path dstPath) {
        try {
            final List<String> index = ResourceUtils.loadResourceIndex(srcPath, "index.txt");
            ResourceUtils.copyResources(srcPath, index, dstPath);
        } catch (IOException e) {
            Core.fatal(TAG, "Failed to resolve internal resources", e);
            throw new RuntimeException(e);
        }
    }
}
