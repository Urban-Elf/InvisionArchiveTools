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

package com.urbanelf.iat.content.parser;

import com.urbanelf.iat.Core;
import com.urbanelf.iat.content.model.Meta;
import com.urbanelf.iat.content.model.PostCount;
import com.urbanelf.iat.content.model.RefreshSpec;
import com.urbanelf.iat.ic.ICUnsupportedException;
import com.urbanelf.iat.proto.constants.ContentType;
import com.urbanelf.iat.util.FileTree;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class ParserDispatcher {
    private static final String TAG = ParserDispatcher.class.getSimpleName();

    public static final String METADATA_FILE = "metadata.json";
    public static final String REFRESH_SPEC_FILE = "refresh_spec.json";
    public static final String POST_COUNT_FILE = "post_count.json";

    /** Processes an archive path and collects relevant meta information to prepare for
     * a writing operation.
     *
     * @param recoveryMode If true, the path must contain a {code refresh_spec.json}, thus making it able to recover in a worker with {@link com.urbanelf.iat.proto.constants.ClientSA#LOAD_FROM_SPEC}
     * @return the {@link ContentSpec} for the archive. When {@code recoveryMode} is true, {@link ContentSpec#refreshSpec()} is guaranteed non-null.
     */
    public static ContentSpec process(Path path, boolean recoveryMode) throws IOException, JSONException, ArchiveParseException {
        if (path == null)
            throw new ArchiveParseException("path cannot be null");

        Path metaPath = path.resolve(METADATA_FILE);
        Path refreshSpecPath = path.resolve(REFRESH_SPEC_FILE);
        Path postCountPath = path.resolve(POST_COUNT_FILE);

        // Read meta file to determine ContentType (necessary for resolving parsers and writers)
        try (BufferedReader reader = Files.newBufferedReader(metaPath)) {
            String line;
            if ((line = reader.readLine()) != null) {
                try {
                    JSONObject json = new JSONObject(line);
                    ContentType contentType = json.getEnum(ContentType.class, Meta.CONTENT_TYPE);

                    // Resolve parser
                    SpecParser parser = contentType.getSpecParser();

                    // Parse meta
                    Meta meta = parser.parseMetadata(metaPath);

                    // Sanity check
                    if (contentType != meta.getContentType())
                        throw new ArchiveParseException("Content type of parsed meta file does not match with established content type");

                    // Parse post count (optional, alternatively is resolved by estimation)
                    PostCount postCount = null;
                    if (Files.exists(postCountPath))
                        postCount = parser.parsePostCount(postCountPath);

                    // Parse Refresh Spec (recovery mode only, throws exceptions and handles properly)
                    RefreshSpec refreshSpec = null;
                    if (recoveryMode) {
                        refreshSpec = parser.parseRefreshSpec(refreshSpecPath);
                    }

                    return new ContentSpec(meta, path, postCount, refreshSpec);
                } catch (ICUnsupportedException | IllegalArgumentException e) {
                    throw new ArchiveParseException(e);
                }
            }
        }

        return null;
    }

    public static boolean isArchiveLoadable(Path path) {
        return Files.exists(path.resolve(REFRESH_SPEC_FILE));
    }

    /** Parses generic metadata from an archive at a given path.
     * @param validate runs {@link #validateArchive(Path)} before parsing
     * @return A {@link Meta} object or null */
    public static Meta parseGenericMetadata(Path path, boolean validate) throws IOException, JSONException {
        if (validate) {
            if (!validateArchive(path)) {
                return null;
            }
        }
        return new SpecParser().parseMetadata(path.resolve(METADATA_FILE));
    }

    public static boolean validateArchive(Path path) throws ArchiveParseException {
        if (path == FileTree.getExportPath()) {
            return false;
        }
        Core.info(TAG, "Validating archive... (" + path.toString() + ")");
        if (!Files.isDirectory(path)) {
            Core.warning(TAG, "Validation failed: not a directory");
            return false;
        }
        try (Stream<Path> walk = Files.walk(path)
                .filter(Files::isRegularFile)) {
            // Check for metadata (designed this way to be expandable for future criterion)
            AtomicBoolean metadataFound = new AtomicBoolean();

            Iterator<Path> iterator = walk.iterator();
            iterator.forEachRemaining(filePath -> {
                if (filePath.getFileName().toString().equals(METADATA_FILE))
                    metadataFound.set(true);
            });

            if (!metadataFound.get())
                Core.warning(TAG, METADATA_FILE + " not found in archive. Cannot recover.");

            return metadataFound.get();

        } catch (NullPointerException | IOException e) {
            Core.error(TAG, "Error while validating recovered archive", e);
        }
        return false;
    }

    public record ValidationResult(boolean successful, String contentTitle) {

    }
}
