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

import com.urbanelf.iat.proto.ClientPacket;
import com.urbanelf.iat.proto.constants.ClientSA;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

public class ClientPacketTest {
    @Test
    void testToJsonIncludesWorkerId() {
        ClientPacket packet = new ClientPacket("abc123", ClientSA.DISPATCH_WORKER);
        packet.addData("k", "v");

        JSONObject json = packet.toJson();

        assertEquals("abc123", json.getString("worker_id"));
        assertEquals(ClientSA.DISPATCH_WORKER.name(), json.getString("shared_action"));
        assertTrue(json.getJSONObject("data").has("k"));
    }
}
