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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtils {
    private static final String TAG = HashUtils.class.getSimpleName();

    public static String hashSHA256(String src) {
        return hash("SHA-256", src);
    }

    public static String hashMD5(String src) {
        return hash("MD5", src);
    }

    private static String hash(String algorithm, String src) {
        try {
            MessageDigest mg = MessageDigest.getInstance(algorithm);
            byte[] result = mg.digest(src.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : result)
                sb.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            Core.error(TAG, "Algorithm " + algorithm + " not found", e);
        }
        return "(unknown algorithm: " + algorithm + ")";
    }
}
