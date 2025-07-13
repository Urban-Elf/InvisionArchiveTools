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

import java.io.PrintWriter;
import java.io.StringWriter;

public class LogUtils {
    public static String stackTrace(Throwable throwable) {
        final StringWriter elements = new StringWriter();
        throwable.printStackTrace(new PrintWriter(elements));
        return elements.getBuffer().toString();
    }

    public static String simpleObjectName(Object object) {
        return object.getClass().getSimpleName() + "@" + Integer.toHexString(object.hashCode());
    }
}
