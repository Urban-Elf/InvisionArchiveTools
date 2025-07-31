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

public class StringUtils {
    public static String repeat(String src, int repetitions) {
        return new String(new char[repetitions]).replace("\0", src);
    }

    public static String fill(String string, char whitespace, int length) {
        StringBuilder out = new StringBuilder(string);
        while (out.length() < length)
            out.append(whitespace);
        return out.toString();
    }

    public static String concat(CharSequence[] charSequences) {
        return concat(charSequences, ", ");
    }

    public static String concat(CharSequence[] charSequences, CharSequence separator) {
        final StringBuilder builder = new StringBuilder();
        for (int i = 0; i < charSequences.length; i++) {
            builder.append(charSequences[i]);
            if (i < charSequences.length - 1)
                builder.append(separator);
        }
        return builder.toString();
    }

    public static String cleanFileName(String fileName) {
        if (fileName == null) return "";
        String cleaned = fileName.replaceAll("[\\\\/:*?\"<>|]", "");
        cleaned = cleaned.trim().replaceAll("^[.\\s]+|[.\\s]+$", "");
        return cleaned;
    }

    public static String removeExtension(String fileName) {
        return fileName.replaceAll("\\.[^.]+$", "");
    }
}
