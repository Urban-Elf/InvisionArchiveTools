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
import com.urbanelf.iat.content.Content;
import com.urbanelf.iat.content.parser.ContentSpec;
import com.urbanelf.iat.content.parser.ParserDispatcher;
import com.urbanelf.iat.proto.constants.ContentType;

import org.json.JSONException;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class WriterDispatcher {
    public static File write(ContentSpec spec, File dst, ArchiveFormat format) throws IOException, JSONException {
        final ContentType contentType = spec.type();
        // Write content to dst
        return contentType.getWriterMap().get(format).write(spec.content(), dst);
    }
}
