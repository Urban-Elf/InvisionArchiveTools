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

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.extras.FlatAnimatedLafChange;
import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import com.urbanelf.iat.Core;

import org.json.JSONException;

import javax.swing.*;

public class ThemeManager {
    private static final String TAG = ThemeManager.class.getSimpleName();

    private static final String LS_THEME = "theme";

    private static final FlatDarkLaf FLAT_DARK_LAF;
    private static final FlatLightLaf FLAT_LIGHT_LAF;
    private static final FlatMacDarkLaf FLAT_MAC_DARK_LAF;
    private static final FlatMacLightLaf FLAT_MAC_LIGHT_LAF;

    private static <T extends FlatLaf> T patchTheme(T t) {
        // Might use later
        return t;
    }

    static {
        // Setup themes
        FLAT_DARK_LAF = patchTheme(new FlatDarkLaf());
        FLAT_LIGHT_LAF = patchTheme(new FlatLightLaf());
        FLAT_MAC_DARK_LAF = patchTheme(new FlatMacDarkLaf());
        FLAT_MAC_LIGHT_LAF = patchTheme(new FlatMacLightLaf());
    }

    private static boolean usingDark = false;

    public static void toggleTheme() {
        toggleTheme(() -> {});
    }

    public static void toggleTheme(Runnable postUpdateRunnable) {
        try {
            FlatAnimatedLafChange.showSnapshot();

            if (PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac) {
                usingDark = !usingDark;
                if (usingDark)
                    UIManager.setLookAndFeel(FLAT_MAC_DARK_LAF);
                else
                    UIManager.setLookAndFeel(FLAT_MAC_LIGHT_LAF);
            } else {
                usingDark = !usingDark;
                if (usingDark)
                    UIManager.setLookAndFeel(FLAT_DARK_LAF);
                else
                    UIManager.setLookAndFeel(FLAT_LIGHT_LAF);
            }

            FlatLaf.updateUI();
            postUpdateRunnable.run();
            FlatAnimatedLafChange.hideSnapshotWithAnimation();
            LocalStorage.getJsonObject().put(LS_THEME, isDark() ? "dark" : "light");
            LocalStorage.serialize();
            Core.info(TAG, "Theme changed to " + (isDark() ? "'dark'" : "'light'"));
        } catch (Exception e) {
            Core.error(TAG, "Failed to switch theme (dark: " + isDark() + ")", e);
        }
    }

    public static void applyDefaultTheme() {
        try {
            try {
                usingDark = LocalStorage.getJsonObject().has(LS_THEME) && LocalStorage.getJsonObject().getString(LS_THEME).equals("dark");
            } catch (JSONException e) {
                final boolean macDark = PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac && isMacDarkMode();
                final String value = macDark ? "dark" : "light";
                Core.warning(TAG, "LS_THEME attribute absent or invalid, using default '" + value + "'");
                usingDark = macDark;
                LocalStorage.getJsonObject().put(LS_THEME, value);
                LocalStorage.serialize();
            }

            if (PlatformUtils.getRunningPlatform() == PlatformUtils.Platform.Mac) {
                if (usingDark)
                    UIManager.setLookAndFeel(FLAT_MAC_DARK_LAF);
                else
                    UIManager.setLookAndFeel(FLAT_MAC_LIGHT_LAF);
            } else {
                if (usingDark)
                    UIManager.setLookAndFeel(FLAT_DARK_LAF);
                else
                    UIManager.setLookAndFeel(FLAT_LIGHT_LAF);
            }
        } catch (Exception e) {
            Core.error(TAG, "Failed to set initial theme", e);
        }
    }

    public static boolean isDark() {
        return usingDark;
    }

    private static boolean isMacDarkMode() {
        String appearance = System.getProperty("apple.awt.application.appearance", "");
        return appearance.toLowerCase().contains("dark");
    }
}

