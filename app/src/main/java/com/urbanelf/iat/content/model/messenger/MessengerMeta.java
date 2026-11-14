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

package com.urbanelf.iat.content.model.messenger;

import com.urbanelf.iat.content.model.Meta;
import com.urbanelf.iat.content.model.UserData;
import com.urbanelf.iat.proto.constants.ContentType;
import com.urbanelf.iat.util.FlatResource;

import org.json.JSONObject;

import java.nio.file.Path;
import java.util.HashMap;

public class MessengerMeta extends Meta {
    public MessengerMeta(JSONObject json) {
        super(json);
    }

    public MessengerMeta(Meta meta) {
        super(meta);
    }
}
