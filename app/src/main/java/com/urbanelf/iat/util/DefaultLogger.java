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

public class DefaultLogger implements Logger {
    @Override
    public void info(String tag, String message) {
        System.out.println(getMessageString(Severity.INFO, tag, message));
    }

    @Override
    public void warning(String tag, String message) {
        System.out.println(getMessageString(Severity.WARNING, tag, message));
    }

    @Override
    public void error(String tag, String message, Throwable throwable) {
        System.err.println(getMessageString(Severity.ERROR, tag, message, throwable));
    }

    @Override
    public void fatal(String tag, String message, Throwable throwable) {
        System.err.println(getMessageString(Severity.FATAL, tag, message, throwable));
    }

    @Override
    public void debug(String tag, String message) {
        System.out.println(getMessageString(Severity.DEBUG, tag, message));
    }
}