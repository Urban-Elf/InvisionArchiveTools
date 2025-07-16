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

package com.urbanelf.iat.ic;

import org.json.JSONObject;

public final class IC4 extends IC {
    public IC4(String rootUrl) {
        super(rootUrl);
    }

    @Override
    public int getVersionNumber() {
        return 4;
    }

    @Override
    public String toString() {
        return getRootUrl() + " (v4)";
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject()
                .put("root_url", getRootUrl())
                .put("version", getVersionNumber());
    }
}
