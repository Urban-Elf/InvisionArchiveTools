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

package com.urbanelf.iat.ic.workers.ic4;

import com.urbanelf.iat.content.Post;
import com.urbanelf.iat.ic.workers.state.ICWorkerState;

import java.util.ArrayList;
import java.util.function.Consumer;

public class MessengerWorker extends IC4Worker<ArrayList<Post>> {
    public MessengerWorker(Consumer<ArrayList<Post>> onCompletion) {
        super("Archive Messenger", onCompletion);
    }

    @Override
    public void run() {
        super.run();



        //final IC4 ic4 = getIC4();

        //driver.get(ic4.messenger(messengerId));
    }

    enum MessengerWorkerState implements ICWorkerState {
        ARCHIVING_MESSAGES("Archiving messages...");

        private final String text;

        MessengerWorkerState(String text) {
            this.text = text;
        }

        @Override
        public String getNote() {
            return text;
        }
    }
}