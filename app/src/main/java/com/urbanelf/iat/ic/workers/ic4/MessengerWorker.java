package com.urbanelf.ima.ic.workers.ic4;

import com.urbanelf.ima.content.Post;
import com.urbanelf.ima.ic.workers.state.ICWorkerState;

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