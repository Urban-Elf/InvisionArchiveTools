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

import com.urbanelf.iat.Version;

public interface Logger {
    int HEADER_WIDTH = 86;

    default void printHeader() {
        final String title = "Invision Archive Tools v" + Version.VERSION;
        final String OSSpec = System.getProperty("os.name")
            + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch");
        final String year = DateUtils.year();
        final String copyright = "Copyright (C) Mark Fisher, " + (year.equals("2025") ? "" : "2025-") + year + ". All rights reserved.";
        final String info = "See Terms of Service for more information.";

        System.out.println("┌" + StringUtils.repeat("─", HEADER_WIDTH - 2) + "┐\n"
            + "│" + StringUtils.fill(StringUtils.repeat(" ", HEADER_WIDTH / 2 - title.length() / 2) + title, ' ', HEADER_WIDTH - 2) + "│\n"
            + "│" + StringUtils.fill(StringUtils.repeat(" ", HEADER_WIDTH / 2 - OSSpec.length() / 2) + OSSpec, ' ', HEADER_WIDTH - 2) + "│\n"
            + "│" + StringUtils.repeat(" ", HEADER_WIDTH - 2) + "│\n"
            + "│" + StringUtils.fill(StringUtils.repeat(" ", HEADER_WIDTH / 2 - copyright.length() / 2) + copyright, ' ', HEADER_WIDTH - 2) + "│\n"
            + "│" + StringUtils.fill(StringUtils.repeat(" ", HEADER_WIDTH / 2 - info.length() / 2) + info, ' ', HEADER_WIDTH - 2) + "│\n"
            + "└" + StringUtils.repeat("─", HEADER_WIDTH - 2) + "┘\n"
            + "Vendor: " + System.getProperty("java.vendor") + '\n'
            + "  |_ " + System.getProperty("java.vendor.url") + "\n"
            + "JRE: " + System.getProperty("java.runtime.name") + '\n'
            + "  |_ " + System.getProperty("java.runtime.version") + '\n'
            + "Library Path: " + System.getProperty("java.library.path"));
        printSeparator();
    }

    default void printSeparator() {
        System.out.println(StringUtils.repeat("-", HEADER_WIDTH));
    }

    void info(String tag, String message);

    void warning(String tag, String message);

    default void error(String tag, Throwable throwable) {
        error(tag, null, throwable);
    }

    default void error(String tag, String message) {
        error(tag, message, null);
    }

    void error(String tag, String message, Throwable throwable);

    default void fatal(String tag, Throwable throwable) {
        fatal(tag, null, throwable);
    }

    default void fatal(String tag, String message) {
        fatal(tag, message, null);
    }

    void fatal(String tag, String message, Throwable throwable);

    void debug(String tag, String message);

    default String getMessageString(Severity severity, String tag, String message) {
        return getMessageString(severity, tag, message, null);
    }

    default String getMessageString(Severity severity, String tag, String message, Throwable throwable) {
        return "[" + Thread.currentThread().getName() + "] " + severity + " " + DateUtils.timestamp() + " " + tag + " - "
            + (message == null || message.isEmpty() ? "" : message) + (throwable != null ?
            (message == null || message.isEmpty() ? "" : ":\n") + LogUtils.stackTrace(throwable) : "");
    }

    enum Severity {
        INFO,
        WARNING,
        ERROR,
        FATAL,
        DEBUG,
    }
}
