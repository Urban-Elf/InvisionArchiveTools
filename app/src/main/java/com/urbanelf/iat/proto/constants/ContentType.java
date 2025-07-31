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

package com.urbanelf.iat.proto.constants;

import com.urbanelf.iat.content.ArchiveFormat;
import com.urbanelf.iat.content.parser.PostParser;
import com.urbanelf.iat.content.parser.Parser;
import com.urbanelf.iat.content.writer.Writer;
import com.urbanelf.iat.content.writer.html.MessengerHTMLWriter;

import java.util.HashMap;

public enum ContentType {
    MESSENGER(new PostParser()) {{ getWriterMap().put(ArchiveFormat.HTML, new MessengerHTMLWriter()); }},
    TOPIC(new PostParser()),
    FORUM(null),
    BLOG_ENTRY(null);

    private final Parser parser;
    private final HashMap<ArchiveFormat, Writer> writerMap;

    ContentType(Parser parser) {
        this.parser = parser;
        this.writerMap = new HashMap<>();
    }

    public Parser getParser() {
        return parser;
    }

    public HashMap<ArchiveFormat, Writer> getWriterMap() {
        return writerMap;
    }
}
