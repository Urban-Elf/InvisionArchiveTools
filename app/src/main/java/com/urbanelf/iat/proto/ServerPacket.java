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

package com.urbanelf.iat.proto;

import com.urbanelf.iat.proto.constants.ServerSA;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

public class ServerPacket {
    private final String workerId;
    private final ServerSA sharedAction;
    private final HashMap<String, Object> data;

    public ServerPacket(String jsonData) throws JSONException {
        final JSONObject jsonObject = new JSONObject(jsonData);
        final Object workerIdObject = jsonObject.get("worker_id");
        if (workerIdObject instanceof String)
            workerId = (String) workerIdObject;
        else
            workerId = null;
        sharedAction = jsonObject.getEnum(ServerSA.class, "shared_action");
        data = new HashMap<>();
        final JSONObject dataObject = jsonObject.getJSONObject("data");
        dataObject.keySet().forEach(key -> data.put(key, dataObject.get(key)));
    }

    public String getWorkerId() {
        return workerId;
    }

    public ServerSA getSharedAction() {
        return sharedAction;
    }

    public HashMap<String, Object> getData() {
        return data;
    }
}
