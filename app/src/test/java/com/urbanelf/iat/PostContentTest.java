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

package com.urbanelf.iat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.urbanelf.iat.content.PostContent;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class PostContentTest {
    @Test
    void testHeaderAttributes() {
        final String json = """
                {
                    "title":"Test",
                    "user_data":{
                        "bob": {"profileUrl": "", "avatarUrl": "", "group": "", "groupIconUrl": ""},
                        "jim": {"profileUrl": "", "avatarUrl": "", "group": "", "groupIconUrl": ""}
                    }
                }
                """;
        final JSONObject jsonObject = new JSONObject(json);
        // Will throw JSONException if parsing fails
        new PostContent(jsonObject);
    }
}
