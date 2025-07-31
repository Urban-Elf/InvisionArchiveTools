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

package com.urbanelf.iat.content;

import javafx.stage.FileChooser;

public enum ArchiveFormat {
    HTML(new FileChooser.ExtensionFilter("HTML files", "*.html"), "html", true),
    JSON(new FileChooser.ExtensionFilter("JSON files", "*.json"), "json");

    private final FileChooser.ExtensionFilter extensionFilter;
    private final String extension;
    private final boolean recommended;

    ArchiveFormat(FileChooser.ExtensionFilter extensionFilter, String extension) {
        this(extensionFilter, extension, false);
    }

    ArchiveFormat(FileChooser.ExtensionFilter extensionFilter, String extension, boolean recommended) {
        this.extensionFilter = extensionFilter;
        this.extension = extension;
        this.recommended = recommended;
    }

    public FileChooser.ExtensionFilter getExtensionFilter() {
        return extensionFilter;
    }

    public String getExtension() {
        return extension;
    }

    @Override
    public String toString() {
        return super.toString() + (recommended ? " (recommended)" : "");
    }
}
