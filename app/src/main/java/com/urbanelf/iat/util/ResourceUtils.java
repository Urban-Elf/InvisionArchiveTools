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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.*;
import java.util.List;
import java.util.stream.Collectors;

public class ResourceUtils {

    /**
     * Loads the resource index file from inside the classpath,
     * returning a list of relative file paths contained in it.
     *
     * @param baseResourceDir The base resource directory containing the index file (e.g. "templates/html/messenger/res")
     * @param indexFileName The name of the index file, e.g. "resource_index.txt"
     * @return List of relative resource file paths listed inside the index file
     * @throws IOException if reading the index resource fails
     */
    public static List<String> loadResourceIndex(String baseResourceDir, String indexFileName) throws IOException {
        String indexResourcePath = baseResourceDir + "/" + indexFileName;

        try (InputStream in = ClassLoader.getSystemResourceAsStream(indexResourcePath)) {
            if (in == null) {
                throw new IOException("Resource index file not found: " + indexResourcePath);
            }
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in))) {
                return reader.lines()
                        .filter(line -> !line.isBlank())
                        .collect(Collectors.toList());
            }
        }
    }

    /**
     * Copies a list of resource files from a base resource directory inside the classpath
     * to a target directory on the filesystem by streaming their content.
     *
     * @param baseResourceDir The base resource directory inside the classpath (e.g. "templates/html/messenger/res")
     * @param resourceFiles Relative paths of resource files under baseResourceDir to copy
     * @param targetDir Filesystem target directory to copy resources into
     * @throws IOException on IO errors
     */
    public static void copyResources(String baseResourceDir,
                                     Iterable<String> resourceFiles,
                                     Path targetDir) throws IOException {
        for (String resourceFile : resourceFiles) {
            String fullResourcePath = baseResourceDir + "/" + resourceFile;

            try (InputStream in = ClassLoader.getSystemResourceAsStream(fullResourcePath)) {
                if (in == null) {
                    throw new IOException("Resource not found: " + fullResourcePath);
                }

                Path outFile = targetDir.resolve(resourceFile);
                Files.createDirectories(outFile.getParent());
                Files.copy(in, outFile, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }
}
