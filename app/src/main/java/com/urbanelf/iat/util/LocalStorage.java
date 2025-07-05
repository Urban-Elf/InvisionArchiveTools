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

import org.json.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;

public class LocalStorage {
    private static final Path FILE;
    private static JSONObject json;

    private LocalStorage() {
    }

    static {
        FILE = FileTree.getRootPath().resolve("shared.json");
        json = new JSONObject();

        deserialize();
    }

    public static JSONObject getJson() {
        return json;
    }

    public static void serialize() {
        try (FileWriter file = new FileWriter(FILE.toFile())) {
            file.write(json.toString(4)); // 4 spaces indent
            // Try-with-resources auto closes (meaning auto flush as well)
        } catch (IOException e) {
            e.printStackTrace();
            // FIXME: Log error
        }
    }

    private static void deserialize() {
        try {
            final String content = Files.readString(FILE);
            json = new JSONObject(content);
        } catch (NoSuchFileException e) {
            // Contingency for corrupted file
        } catch (IOException e) {
            e.printStackTrace();
            // FIXME: Log error
        }
    }
}
