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

import com.urbanelf.iat.Core;

import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class LocalStorage {
    private static final String TAG = LocalStorage.class.getSimpleName();

    private static final Path FILE;
    private static JSONObject JSON_OBJECT;

    private LocalStorage() {
    }

    static {
        FILE = FileTree.getRootPath().resolve("shared.json");
        JSON_OBJECT = new JSONObject();

        if (Files.exists(FILE))
            deserialize();
    }

    public static JSONObject getJsonObject() {
        return JSON_OBJECT;
    }

    public static void serialize() {
        try (FileWriter file = new FileWriter(FILE.toFile())) {
            file.write(JSON_OBJECT.toString(4));
            // auto-flushed and closed
        } catch (IOException e) {
            Core.error(TAG, "Error serializing " + FILE, e);
        }
    }

    private static void deserialize() {
        try {
            final String content = Files.readString(FILE);
            JSON_OBJECT = new JSONObject(content);
        } catch (IOException e) {
            Core.error(TAG, "Error deserializing " + FILE, e);
        }
    }
}
