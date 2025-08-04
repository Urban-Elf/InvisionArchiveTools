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

public class PlatformUtils {
    private static final Platform PLATFORM;

    private PlatformUtils() {
    }

    static {
        final String osName = System.getProperty("os.name").toLowerCase();
        if (osName.contains("linux")) {
            PLATFORM = Platform.Linux;
        } else if (osName.contains("windows")) {
            PLATFORM = Platform.Windows;
        } else if (osName.contains("mac")) {
            PLATFORM = Platform.Mac;
        } else {
            PLATFORM = Platform.Unknown;
        }
    }

    public static void initialize() {
    }

    public static Platform getRunningPlatform() {
        if (PLATFORM == null)
            throw new RuntimeException("PLATFORM should never be null");
        return PLATFORM;
    }

    public enum Platform {
        Linux,
        Windows,
        Mac,
        Unknown
    }
}
