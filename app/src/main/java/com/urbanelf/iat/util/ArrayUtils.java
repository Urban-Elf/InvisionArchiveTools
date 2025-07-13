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

public class ArrayUtils {
    public static Integer[] box(int[] array) {
        final Integer[] boxed = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
            boxed[i] = array[i]; // autobox
        return boxed;
    }

    public static int[] unbox(Integer[] array) {
        final int[] unboxed = new int[array.length];
        for (int i = 0; i < array.length; i++)
            unboxed[i] = array[i]; // auto(un)box
        return unboxed;
    }

    public static Short[] box(short[] array) {
        final Short[] boxed = new Short[array.length];
        for (int i = 0; i < array.length; i++)
            boxed[i] = array[i]; // autobox
        return boxed;
    }

    public static short[] unbox(Short[] array) {
        final short[] unboxed = new short[array.length];
        for (int i = 0; i < array.length; i++)
            unboxed[i] = array[i]; // auto(un)box
        return unboxed;
    }

    public static Byte[] box(byte[] array) {
        final Byte[] boxed = new Byte[array.length];
        for (int i = 0; i < array.length; i++)
            boxed[i] = array[i]; // autobox
        return boxed;
    }

    public static byte[] unbox(Byte[] array) {
        final byte[] unboxed = new byte[array.length];
        for (int i = 0; i < array.length; i++)
            unboxed[i] = array[i]; // auto(un)box
        return unboxed;
    }

    public static <T> boolean contains(Iterable<T> iterable, T object, boolean identify) {
        for (T item : iterable) {
            if (identify && item == object)
                return true;
            else if (!identify && item.equals(object))
                return true;
        }
        return false;
    }

    public static <T> int instancesOf(Iterable<T> iterable, Class<?> clazz) {
        return instancesOf(iterable, clazz, true);
    }

    public static <T> int instancesOf(Iterable<T> iterable, Class<?> clazz, boolean recursive) {
        int count = 0;
        for (T object : iterable) {
            if (object.getClass() == clazz)
                count++;
            else if (recursive && object instanceof Iterable)
                count += instancesOf(iterable, clazz, true);
        }
        return count;
    }
}
