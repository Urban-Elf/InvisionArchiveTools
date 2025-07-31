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

import java.text.DecimalFormat;

public class NumberUtils {
    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        DECIMAL_FORMAT = new DecimalFormat("#,###");
    }

    public static String formatDelimiter(int value) {
        return DECIMAL_FORMAT.format(value);
    }

    public static String formatDelimiter(long value) {
        return DECIMAL_FORMAT.format(value);
    }

    public static String formatDelimiter(float value) {
        return DECIMAL_FORMAT.format(value);
    }

    public static String formatDelimiter(double value) {
        return DECIMAL_FORMAT.format(value);
    }
}
