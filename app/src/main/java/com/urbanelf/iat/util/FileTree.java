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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;

import javax.swing.UIManager;

public class FileTree {
    private static final String TAG = FileTree.class.getSimpleName();

    private static final Path rootPath;
    private static final Path logPath;
    private static final Path serverPath;
    private static final Path exportPath;

    private FileTree() {
    }

    static {
        switch (PlatformUtils.getRunningPlatform()) {
            case Windows -> rootPath = Paths.get(System.getProperty("user.home"), "AppData", "Roaming", "IAT");
            case Mac -> rootPath = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "IAT");
            // Linux, Unknown
            default -> rootPath = Paths.get(System.getProperty("user.home"), ".iat");
        }

        logPath = rootPath.resolve("logs");
        serverPath = rootPath.resolve("server");
        exportPath = rootPath.resolve("export");

        try {
            Files.createDirectories(logPath); // Build hierarchy
            // Server path is created automatically
            Files.createDirectories(exportPath);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void deleteRecursively(Path path) throws IOException {
        if (!Files.exists(path)) return;
        final Stream<Path> walk = Files.walk(path)
                .sorted(Comparator.reverseOrder()); // delete children before parents
        walk.forEach(p -> {
            try {
                Files.deleteIfExists(p);
            } catch (IOException e) {
                Core.warning(TAG, "Failed to delete file + '" + p + "'");
            }
        });
        walk.close();
    }

    public static Path getRootPath() {
        return rootPath;
    }

    public static Path getLogPath() {
        return logPath;
    }

    public static Path getServerPath() {
        return serverPath;
    }
}
