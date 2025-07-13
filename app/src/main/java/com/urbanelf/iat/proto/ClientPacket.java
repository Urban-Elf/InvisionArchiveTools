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

import com.urbanelf.iat.proto.constants.ClientSA;
import com.urbanelf.iat.util.JSONSerializable;

import org.json.JSONObject;

import java.util.HashMap;

public class ClientPacket implements JSONSerializable {
    private final String workerId;
    private final ClientSA sharedAction;
    private final HashMap<String, Object> data;

    public ClientPacket(ClientSA sharedAction) {
        this(null, sharedAction);
    }

    public ClientPacket(String workerId, ClientSA sharedAction) {
        this.workerId = workerId;
        this.sharedAction = sharedAction;
        data = new HashMap<>();
    }

    public ClientPacket addData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    @Override
    public JSONObject toJson() {
        return new JSONObject()
                .put("worker_id", workerId != null ? workerId : JSONObject.NULL)
                .put("shared_action", sharedAction.name())
                .put("data", data);
    }

    public String getWorkerId() {
        return workerId;
    }

    public ClientSA getSharedAction() {
        return sharedAction;
    }

    public HashMap<String, Object> getData() {
        return data;
    }
}
