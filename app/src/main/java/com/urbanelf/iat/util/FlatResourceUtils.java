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

package com.urbanelf.iat.util;

import com.urbanelf.iat.Core;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class FlatResourceUtils {
    private static final String TAG = FlatResourceUtils.class.getSimpleName();

    private FlatResourceUtils() {} // prevent instantiation

    public static String flattenResource(String url, Path resPath) {
        final String resourceName = URLUtils.getResourceName(url);
        final HttpClient client = HttpClient.newHttpClient();
        final HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .build();
        try {
            client.send(request, HttpResponse.BodyHandlers.ofFile(resPath.resolve(resourceName)));
        } catch (IOException | InterruptedException e) {
            Core.error(TAG, "Error flattening resource '" + url + "' to " + resPath, e);
            return url;
        }
        return Paths.get("..", resPath.getFileName().toString(), resourceName).toString();
    }
}
