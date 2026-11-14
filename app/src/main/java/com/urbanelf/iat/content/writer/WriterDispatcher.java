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

package com.urbanelf.iat.content.writer;

import com.urbanelf.iat.content.ArchiveFormat;
import com.urbanelf.iat.content.parser.ContentSpec;
import com.urbanelf.iat.proto.constants.ContentType;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class WriterDispatcher {
    public static Path write(ContentSpec spec, Path dst, ArchiveFormat format, WriterTracker tracker) throws IOException, JSONException {
        final ContentType contentType = spec.meta().getContentType();
        // Write content to dst
        return contentType.createWriter(format).write(spec, dst, tracker);
    }
}
