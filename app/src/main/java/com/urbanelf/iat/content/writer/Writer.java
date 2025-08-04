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

import com.urbanelf.iat.content.model.Content;
import com.urbanelf.iat.util.StringUtils;

import java.io.File;
import java.io.IOException;

public abstract class Writer {
    protected abstract File write(Content content, File dst) throws IOException;

    protected static File resolveDestinationDirectory(File dst) {
        File dstDirectory;
        for (int i = 0; true; i++) {
            dstDirectory = new File(dst.getParentFile(),
                    StringUtils.removeExtension(dst.getName())
                            + (i == 0 ? "" : " (" + i + ")"));
            if (!dstDirectory.exists())
                break;
        }
        return dstDirectory;
    }
}
