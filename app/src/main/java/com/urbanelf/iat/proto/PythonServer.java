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

import com.urbanelf.iat.Core;
import com.urbanelf.iat.proto.constants.ClientSA;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.PlatformUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class PythonServer {
    private static final String TAG = "Server";

    private static final Path SERVER_EXECUTABLE_PATH;

    static {
        switch (PlatformUtils.getRunningPlatform()) {
            case Windows -> SERVER_EXECUTABLE_PATH = FileTree.getServerPath().resolve("server.exe");
            default -> SERVER_EXECUTABLE_PATH = FileTree.getServerPath().resolve("server");
        }
    }

    private static final ArrayList<ServerListener> listeners = new ArrayList<>();
    private static Process process;

    public static void initialize() throws IOException {
        if (process != null)
            return;

        // Setup server directory
        ServerExtractor.initialize();

        if (PlatformUtils.getRunningPlatform() != PlatformUtils.Platform.Windows) {
            final Set<PosixFilePermission> permissions = Files.getPosixFilePermissions(SERVER_EXECUTABLE_PATH);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);

            Files.setPosixFilePermissions(SERVER_EXECUTABLE_PATH, permissions);
        }

        // Start server process
        process = new ProcessBuilder(SERVER_EXECUTABLE_PATH.toAbsolutePath().toString()) // , "--debug"
                .start();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Send TERMINATE signal
            PythonServer.writePacket(new ClientPacket(ClientSA.TERMINATE));

            try {
                // Wait for process to finish
                process.waitFor(3, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
            }

            process.destroy();

            try {
                if (!process.waitFor(5, TimeUnit.SECONDS)) {
                    // Didn't exit gracefully
                    process.destroyForcibly();
                }
            } catch (InterruptedException e) {
            }
        }, "Server-ShutdownHook"));

        // Input thread
        new Thread(() -> {
            final InputStream in = process.getInputStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            String line;
            while (true) {
                try {
                    if ((line = reader.readLine()) == null)
                        break; // EOF
                    Core.debug(TAG, "stdout << " + line);
                    // Client packet
                    final ServerPacket packet = new ServerPacket(line);
                    // Notify listeners
                    listeners.forEach(listener -> listener.packetReceived(packet));
                } catch (Exception e) {
                    Core.error(TAG, "Failed to deconstruct server packet", e);
                }
            }
        }, "stdout").start();

        // Error input thread
        new Thread(() -> {
            final InputStream err = process.getErrorStream();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(err));
            String line;
            while (true) {
                try {
                    if ((line = reader.readLine()) == null)
                        break; // EOF
                    System.err.println("[PyServer] " + line);
                } catch (IOException e) {
                    listeners.forEach(listener -> listener.exceptionThrown(e));
                }
            }
        }, "stderr").start();
    }

    public static void writePacket(ClientPacket packet) {
        if (process == null)
            throw new IllegalStateException("Not initialized, call #initialize() first");
        try {
            final OutputStream out = process.getOutputStream();
            final String jsonString = packet.toJson().toString();
            out.write(jsonString.getBytes());
            out.write("\n".getBytes()); // Crucial
            out.flush();
            Core.debug(TAG, "stdin >> " + jsonString);
        } catch (IOException e) {
            listeners.forEach(listener -> listener.exceptionThrown(e));
        }
    }

    public static void addListener(ServerListener listener) {
        listeners.add(listener);
    }

    public static void removeListener(ServerListener listener) {
        listeners.remove(listener);
    }

    public interface ServerListener {
        void packetReceived(ServerPacket packet);
        void exceptionThrown(Throwable throwable);
    }
}
