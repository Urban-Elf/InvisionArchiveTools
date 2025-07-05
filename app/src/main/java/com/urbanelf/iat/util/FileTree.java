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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.UIManager;

public class FileTree {
    private static final Path rootPath;
    private static final Path logPath;

    private FileTree() {
    }

    static {
        switch (PlatformUtils.getRunningPlatform()) {
            case Windows -> rootPath = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "ICT");
            case Mac -> rootPath = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "ICT");
            // Linux, Unknown
            default -> rootPath = Paths.get(System.getProperty("user.home"), ".ict");
        }

        logPath = rootPath.resolve("logs");

        try {
            Files.createDirectories(logPath); // Build hierarchy
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Path getRootPath() {
        return rootPath;
    }

    public static Path getLogPath() {
        return logPath;
    }
}
