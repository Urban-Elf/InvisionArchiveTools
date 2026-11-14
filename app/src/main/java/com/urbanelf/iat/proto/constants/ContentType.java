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
import com.urbanelf.iat.content.parser.MessengerPageParser;
import com.urbanelf.iat.content.parser.MessengerSpecParser;
import com.urbanelf.iat.content.parser.PageParser;
import com.urbanelf.iat.content.parser.SpecParser;
import com.urbanelf.iat.content.parser.TopicPostParser;
import com.urbanelf.iat.content.writer.Writer;
import com.urbanelf.iat.content.writer.WriterFactory;
import com.urbanelf.iat.content.writer.html.MessengerHTMLWriter;

public enum ContentType implements WriterFactory {
    MESSENGER("Messenger", new MessengerSpecParser(), new MessengerPageParser()) {
        @Override
        public Writer createWriter(ArchiveFormat format) {
            if (format == ArchiveFormat.HTML) {
                return new MessengerHTMLWriter();
            }
            return null;
        }
    },
    TOPIC("Topic", new TopicPostParser(), null),
    FORUM("Forum", null, null),
    BLOG_ENTRY("Blog Entry", null, null);

    private final String title;
    private final SpecParser specParser;
    private final PageParser pageParser;

    ContentType(String name, SpecParser specParser, PageParser pageParser) {
        this.title = name;
        this.specParser = specParser;
        this.pageParser = pageParser;
    }

    public String getTitle() {
        return title;
    }

    public SpecParser getSpecParser() {
        return specParser;
    }

    public PageParser getPageParser() {
        return pageParser;
    }

    @Override
    public Writer createWriter(ArchiveFormat format) {
        throw new UnsupportedOperationException();
    }
}
