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

package com.urbanelf.iat.ic.workers.state;

// Marker interface
public interface ICWorkerState {
    ICWorkerState INITIALIZATION = () -> "Initializing... Please wait...";
    ICWorkerState AUTH_REQUIRED = new ICWorkerState() {
        @Override
        public String getNote() {
            return "Please sign in to continue.";
        }

        @Override
        public String getHint() {
            return "Sign in on the browser before pressing 'Proceed'";
        }

        @Override
        public ButtonConfig[] getButtonConfigs() {
            return new ButtonConfig[] { new ButtonConfig("Proceed", () -> {}, true) };
        }
    };
    ICWorkerState CLIENT_EXCEPTION = new ICWorkerState() {
        @Override
        public String getNote() {
            return "An error occurred during a client action. Please do not navigate away from ";
        }

        @Override
        public ButtonConfig[] getButtonConfigs() {
            return new ButtonConfig[] { new ButtonConfig("OK", () -> {}, true) };
        }
    };
    ICWorkerState INTERNAL_EXCEPTION = new ICWorkerState() {
        @Override
        public String getNote() {
            return "An internal error occurred. Open log?";
        }

        @Override
        public ButtonConfig[] getButtonConfigs() {
            return new ButtonConfig[] {
                    new ButtonConfig("OK", () -> {}, true),
                    new ButtonConfig("Cancel", () -> {}, false)
            };
        }
    };
    ICWorkerState COMPLETED = new ICWorkerState() {
        @Override
        public String getNote() {
            return "Operation completed successfully.";
        }

        @Override
        public ButtonConfig[] getButtonConfigs() {
            return new ButtonConfig[] { new ButtonConfig("OK", () -> {}, true) };
        }
    };

    String getNote();

    default String getHint() {
        return "";
    }

    default ButtonConfig[] getButtonConfigs() {
        return null;
    }

    default boolean isProgressive() {
        return getButtonConfigs() == null;
    }

    default boolean isIndeterminate() {
        return true;
    }

    record ButtonConfig(String text, Runnable callback, Object clientObject) {
    }
}