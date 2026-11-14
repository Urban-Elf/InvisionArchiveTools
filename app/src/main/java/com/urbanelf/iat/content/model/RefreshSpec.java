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

package com.urbanelf.iat.content.model;

import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.ICUnsupportedException;
import com.urbanelf.iat.proto.constants.WorkerType;

import org.json.JSONException;
import org.json.JSONObject;

public class RefreshSpec {
    private static final String IC_MAP = "ic";
    private static final String IC_VERSION = "version";
    private static final String IC_ROOT_URL = "root_url";
    private static final String WORKER_TYPE = "worker_type";
    private static final String REFRESH_URL = "refresh_url";
    private static final String USERDATA_FROM_POSTS = "userdata_from_posts";

    private final IC ic;
    private final WorkerType workerType;
    private final String refreshUrl;
    private final boolean userdataFromPosts;

    public RefreshSpec(JSONObject json) throws JSONException, ICUnsupportedException {
        JSONObject icMap = json.getJSONObject(IC_MAP);
        int version = icMap.getInt(IC_VERSION);
        String rootUrl = icMap.getString(IC_ROOT_URL);
        ic = IC.instantiateByVersion(version, rootUrl);
        // Worker type
        String workerTypeString = json.getString(WORKER_TYPE);
        workerType = WorkerType.valueOf(workerTypeString);
        // Remaining information
        refreshUrl = json.getString(REFRESH_URL);
        userdataFromPosts = json.getBoolean(USERDATA_FROM_POSTS);
    }

    public IC getIc() {
        return ic;
    }

    public WorkerType getWorkerType() {
        return workerType;
    }

    public String getRefreshUrl() {
        return refreshUrl;
    }

    public boolean isUserdataFromPosts() {
        return userdataFromPosts;
    }
}
