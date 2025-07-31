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

package com.urbanelf.iat.content.writer.html;

import com.urbanelf.iat.content.Content;
import com.urbanelf.iat.content.writer.Writer;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

import java.io.File;
import java.io.IOException;

public abstract class HTMLWriter extends Writer {
    protected static final String TAG = HTMLWriter.class.getSimpleName();

    protected static final String PATH_PREFIX = "templates/html/";

    private static final TemplateEngine TEMPLATE_ENGINE;

    static {
        TEMPLATE_ENGINE = new TemplateEngine();
        final ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setPrefix(PATH_PREFIX);
        resolver.setSuffix(".html");
        resolver.setCharacterEncoding("UTF-8");
        TEMPLATE_ENGINE.setTemplateResolver(resolver);
    }

    @Override
    protected File write(Content content, File dst) throws IOException {
        return writeHTML(TEMPLATE_ENGINE, content, dst);
    }

    public abstract File writeHTML(TemplateEngine engine, Content content, File dst) throws IOException;
}
