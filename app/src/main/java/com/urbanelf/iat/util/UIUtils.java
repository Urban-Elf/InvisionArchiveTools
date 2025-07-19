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

import java.awt.Component;
import java.awt.Dimension;

public class UIUtils {
    public static void prefWidth(Component component, int width) {
        final Dimension dimension = component.getPreferredSize();
        dimension.width = width;
        component.setPreferredSize(dimension);
    }

    public static void maxWidth(Component component, int width) {
        final Dimension dimension = component.getMaximumSize();
        dimension.width = width;
        component.setMaximumSize(dimension);
    }

    public static void minWidth(Component component, int width) {
        final Dimension dimension = component.getMinimumSize();
        dimension.width = width;
        component.setMinimumSize(dimension);
    }

    public static void width(Component component, int width) {
        prefWidth(component, width);
        minWidth(component, width);
        maxWidth(component, width);
    }

    public static void prefHeight(Component component, int height) {
        final Dimension dimension = component.getPreferredSize();
        dimension.height = height;
        component.setPreferredSize(dimension);
    }

    public static void maxHeight(Component component, int height) {
        final Dimension dimension = component.getMaximumSize();
        dimension.height = height;
        component.setMaximumSize(dimension);
    }

    public static void minHeight(Component component, int height) {
        final Dimension dimension = component.getMinimumSize();
        dimension.height = height;
        component.setMinimumSize(dimension);
    }

    public static void height(Component component, int height) {
        prefHeight(component, height);
        minHeight(component, height);
        maxHeight(component, height);
    }
}
