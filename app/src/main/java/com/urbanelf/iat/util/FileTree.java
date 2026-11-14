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

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Comparator;
import java.util.stream.Stream;

import javax.swing.UIManager;

public class FileTree {
    private static final String TAG = FileTree.class.getSimpleName();

    private static final Path rootPath;
    private static final Path logPath;
    private static final Path serverPath;
    private static final Path exportPath;
    private static final Path corruptedExportsPath;

    private FileTree() {
    }

    static {
        switch (PlatformUtils.getRunningPlatform()) {
            case Windows -> rootPath = Paths.get(System.getProperty("user.home"), "AppData", "Local", "IAT");
            case Mac -> rootPath = Paths.get(System.getProperty("user.home"), "Library", "Application Support", "IAT");
            // Linux, Unknown
            default -> rootPath = Paths.get(System.getProperty("user.home"), ".iat");
        }

        logPath = rootPath.resolve("logs");
        serverPath = rootPath.resolve("server");
        exportPath = rootPath.resolve("export");
        corruptedExportsPath = rootPath.resolve("corrupted_exports");

        try {
            Files.createDirectories(logPath); // Build hierarchy
            // Server path is created automatically
            Files.createDirectories(exportPath);
            Files.createDirectories(corruptedExportsPath);
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

    public static void copyDir(Path source, Path target) throws IOException {
        Files.walkFileTree(source, new SimpleFileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                Path targetDir = target.resolve(source.relativize(dir));
                if (!Files.exists(targetDir)) {
                    Files.createDirectories(targetDir);
                }
                return FileVisitResult.CONTINUE;
            }
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
                return FileVisitResult.CONTINUE;
            }
        });
    }

    /** Checks for safe destination paths, appending " (1)", " (2)", etc. to the name. */
    public static Path resolveDestinationDirectory(Path dst) {
        Path fileName = dst.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Destination must have a file name: " + dst);
        }

        String baseName;
        if (Files.isDirectory(dst)) {
            baseName = dst.getFileName().toString();
        } else {
            baseName = StringUtils.removeExtension(fileName.toString());
        }

        for (int i = 0; ; i++) {
            String candidateName = baseName + (i == 0 ? "" : " (" + i + ")");
            Path candidate = dst.resolveSibling(candidateName);

            if (!Files.exists(candidate)) {
                return candidate;
            }
        }
    }

    public static boolean isEmptyDirectory(Path path) throws IOException {
        try (var entries = Files.list(path)) {
            return entries.findAny().isEmpty();
        }
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

    public static Path getExportPath() {
        return exportPath;
    }

    public static Path getCorruptedExportsPath() {
        return corruptedExportsPath;
    }
}
