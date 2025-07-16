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

import java.util.regex.Pattern;

public class URLUtils {
    private static final Pattern PROTOCOL_PATTERN = Pattern.compile("^(https?://)?([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}/?$", Pattern.CASE_INSENSITIVE);

    public static boolean isURLHTTP(String url) {
        return PROTOCOL_PATTERN.matcher(url).find();
    }

    public static String normalizeURLString(String url) {
        url = url.toLowerCase();
        if (url.startsWith("http://"))
            url = url.replaceFirst("http", "https");
        if (!url.startsWith("https://")) {
            url = "https://" + url;
        }
        if (url.endsWith("/"))
            url = url.substring(0, url.length()-1);
        return url;
    }
}
