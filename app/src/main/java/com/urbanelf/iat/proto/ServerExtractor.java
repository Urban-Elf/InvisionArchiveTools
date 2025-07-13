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

package com.urbanelf.iat.proto;

import com.urbanelf.iat.Core;
import com.urbanelf.iat.Version;
import com.urbanelf.iat.util.Benchmark;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.HashUtils;

import org.json.JSONObject;

import java.io.*;
import java.nio.file.*;
import java.util.zip.*;

public class ServerExtractor {
    private static final String TAG = ServerExtractor.class.getSimpleName();

    private static final String ARCHIVE_PATH = "server/server.zip";

    private static boolean isInstallationValid() throws IOException {
        final Path serverPath = FileTree.getServerPath();
        if (!Files.exists(serverPath)) {
            Core.info(TAG, "No server installation found.");
            return false;
        }
        try {
            final Path metadata = serverPath.resolve("metadata.json");
            if (!Files.exists(metadata))
                throw new FileNotFoundException();
            final JSONObject jsonObject = new JSONObject(Files.readString(metadata));
            final String version = jsonObject.getString("version");
            final String[] versionSplits = Version.VERSION.split("\\.");
            final String[] metaSplits = version.split("\\.");
            for (int i = 0; i < versionSplits.length; i++) {
                if (Integer.parseInt(versionSplits[i]) > Integer.parseInt(metaSplits[i])) {
                    Core.warning(TAG, "An outdated server installation was detected [" + version + " < " + Version.VERSION + "]");
                    FileTree.deleteRecursively(serverPath);
                    return false;
                }
            }
        } catch (Exception e) {
            Core.warning(TAG, "An invalid server installation was detected.");
            FileTree.deleteRecursively(serverPath);
            return false;
        }
        Core.info(TAG, "Server installation is up-to-date.");
        return true;
    }

    private static void copyZipFromJar(Path destinationPath) throws IOException {
        try (InputStream in = ClassLoader.getSystemResourceAsStream(ServerExtractor.ARCHIVE_PATH)) {
            if (in == null) {
                throw new FileNotFoundException("Resource not found: " + ServerExtractor.ARCHIVE_PATH);
            }
            Core.info(TAG, "Cloning from internal archive [v" + Version.VERSION + "]...");
            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private static void extractZip(Path zipPath, Path extractToDir) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zipPath))) {
            Core.info(TAG, "Extracting...");
            Benchmark.begin();
            // Extract zip
            ZipEntry entry;
            while ((entry = zipIn.getNextEntry()) != null) {
                Path filePath = extractToDir.resolve(entry.getName());
                if (entry.isDirectory()) {
                    Files.createDirectories(filePath);
                } else {
                    Files.createDirectories(filePath.getParent());
                    try (OutputStream out = Files.newOutputStream(filePath)) {
                        zipIn.transferTo(out);
                    }
                }
                zipIn.closeEntry();
            }
            Core.info(TAG, "Server archive extracted successfully [" + Benchmark.end() + "ms]");
        }
    }

    public static void initialize() throws IOException {
        if (isInstallationValid())
            return;

        final String hash = HashUtils.hashMD5(Long.toString(System.currentTimeMillis()));
        final Path tempZipPath = FileTree.getRootPath().resolve(hash + ".zip");

        // 1. Copy zip to disk
        copyZipFromJar(tempZipPath);

        // 2. Extract
        extractZip(tempZipPath, FileTree.getRootPath());

        Files.deleteIfExists(tempZipPath);
    }
}