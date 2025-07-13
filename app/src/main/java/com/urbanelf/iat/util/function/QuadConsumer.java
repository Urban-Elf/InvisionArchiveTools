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

package com.urbanelf.iat.util.function;

import java.util.Objects;

@FunctionalInterface
public interface QuadConsumer<T, S, U, V> {
    void accept(T var1, S var2, U var3, V var4);

    default QuadConsumer<T, S, U, V> andThen(QuadConsumer<? super T, ? super S, ? super U, ? super V> var1) {
        Objects.requireNonNull(var1);
        return (var2, var3, var4, var5) -> {
            this.accept(var2, var3, var4, var5);
            var1.accept(var2, var3, var4, var5);
        };
    }
}
