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

import com.urbanelf.iat.proto.constants.ButtonCallbackSA;

import org.json.JSONArray;
import org.json.JSONObject;

public class ICWorkerState {
    public static final ICWorkerState CONNECTING = new ICWorkerState(
            "Connecting to ChromeDriver...", "This may take a while on the first run.", null, true);

    private final String note;
    private final String hint;
    private final ButtonConfig[] buttonConfigs;
    private final boolean indeterminate;

    public ICWorkerState(String note, String hint, ButtonConfig[] buttonConfigs, boolean indeterminate) {
        this.note = note;
        this.hint = hint;
        this.buttonConfigs = buttonConfigs;
        this.indeterminate = indeterminate;
    }

    public ICWorkerState(JSONObject jsonObject) {
        note = jsonObject.getString("note");
        hint = jsonObject.getString("hint");
        final Object configObject = jsonObject.get("button_configs");
        if (configObject instanceof JSONArray configArray) {
            buttonConfigs = new ButtonConfig[configArray.length()];
            for (int i = 0; i < configArray.length(); i++) {
                final JSONObject configData = configArray.getJSONObject(i);
                final String text = configData.getString("text");
                final ButtonCallbackSA sharedAction = Enum.valueOf(ButtonCallbackSA.class, configData.getString("shared_action"));
                final Object clientObject = configData.get("client_object");
                buttonConfigs[i] = new ButtonConfig(text, sharedAction, clientObject);
            }
        } else {
            buttonConfigs = null;
        }
        indeterminate = jsonObject.getBoolean("indeterminate");
    }

    public String getNote() {
        return note;
    }

    public String getHint() {
        return hint;
    }

    public boolean isProgressive() {
        return getButtonConfigs() == null;
    }

    public ButtonConfig[] getButtonConfigs() {
        return buttonConfigs;
    }

    public boolean isIndeterminate() {
        return indeterminate;
    }

    public record ButtonConfig(String text, ButtonCallbackSA sharedAction, Object clientObject) {
    }
}