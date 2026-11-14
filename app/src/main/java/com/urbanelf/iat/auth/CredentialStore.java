/*
 * This file is part of Invision Archive Tools (IAT).
 *
 * Copyright (C) 2025 - 2026 Mark Fisher
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

package com.urbanelf.iat.auth;

import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;

public final class CredentialStore {
    private static final int SALT_LENGTH = 16;
    private static final int NONCE_LENGTH = 12;
    private static final int TAG_LENGTH = 16; // Bytes
    private static final int TAG_BITS = TAG_LENGTH * 8;
    private static final int KEY_BITS = 256;
    private static final int ITERATIONS = 600_000;

    // Header: iterations | username byte length | password byte length | salt | nonce
    private static final int FIXED_HEADER_LENGTH =
            4 + 4 + 4 + SALT_LENGTH + NONCE_LENGTH;

    private final Path file;

    public CredentialStore(Path file) {
        this.file = file;
    }

    public void save(char[] username, char[] passwordToStore, char[] keyPassword)
            throws IOException, GeneralSecurityException {
        if (username == null || passwordToStore == null
                || keyPassword == null || keyPassword.length == 0) {
            throw new IllegalArgumentException("Username, stored password, and key password are required.");
        }

        byte[] usernameBytes = null;
        byte[] passwordBytes = null;
        byte[] plaintext = null;
        byte[] ciphertext = null;
        byte[] salt = new byte[SALT_LENGTH];
        byte[] nonce = new byte[NONCE_LENGTH];

        try {
            usernameBytes = encode(username);
            passwordBytes = encode(passwordToStore);

            SecureRandom random = new SecureRandom();
            random.nextBytes(salt);
            random.nextBytes(nonce);

            byte[] header = makeHeader(
                    ITERATIONS, usernameBytes.length, passwordBytes.length, salt, nonce);

            plaintext = new byte[usernameBytes.length + passwordBytes.length];
            System.arraycopy(usernameBytes, 0, plaintext, 0, usernameBytes.length);
            System.arraycopy(passwordBytes, 0, plaintext, usernameBytes.length,
                    passwordBytes.length);

            ciphertext = crypt(Cipher.ENCRYPT_MODE, plaintext, keyPassword,
                    salt, nonce, ITERATIONS, header);

            ByteBuffer fileData = ByteBuffer.allocate(header.length + ciphertext.length);
            fileData.put(header).put(ciphertext);
            Files.write(file, fileData.array());
        } finally {
            clear(usernameBytes);
            clear(passwordBytes);
            clear(plaintext);
            clear(ciphertext);
        }
    }

    public Credentials load(char[] keyPassword)
            throws IOException, GeneralSecurityException {
        if (keyPassword == null || keyPassword.length == 0) {
            throw new IllegalArgumentException("Key password is required.");
        }

        byte[] fileData = Files.readAllBytes(file);
        byte[] plaintext = null;
        byte[] ciphertext = null;
        byte[] usernameBytes = null;
        byte[] passwordBytes = null;

        try {
            if (fileData.length < FIXED_HEADER_LENGTH + TAG_LENGTH) {
                throw new IOException("Invalid or truncated encrypted file.");
            }

            ByteBuffer input = ByteBuffer.wrap(fileData);
            int iterations = input.getInt();
            int usernameLength = input.getInt();
            int passwordLength = input.getInt();

            if (iterations < 1 || iterations > 5_000_000
                    || usernameLength < 0 || passwordLength < 0) {
                throw new IOException("Invalid encrypted-file header.");
            }

            long expectedLength = (long) FIXED_HEADER_LENGTH
                    + usernameLength + passwordLength + TAG_LENGTH;
            if (expectedLength != fileData.length) {
                throw new IOException("Invalid encrypted-file length.");
            }

            byte[] salt = new byte[SALT_LENGTH];
            byte[] nonce = new byte[NONCE_LENGTH];
            input.get(salt).get(nonce);

            byte[] header = Arrays.copyOfRange(fileData, 0, FIXED_HEADER_LENGTH);
            ciphertext = new byte[input.remaining()];
            input.get(ciphertext);

            plaintext = crypt(Cipher.DECRYPT_MODE, ciphertext, keyPassword,
                    salt, nonce, iterations, header);

            if (plaintext.length != usernameLength + passwordLength) {
                throw new IOException("Decrypted data has an invalid length.");
            }

            usernameBytes = Arrays.copyOfRange(plaintext, 0, usernameLength);
            passwordBytes = Arrays.copyOfRange(
                    plaintext, usernameLength, usernameLength + passwordLength);

            return new Credentials(decode(usernameBytes), decode(passwordBytes));
        } finally {
            clear(fileData);
            clear(plaintext);
            clear(ciphertext);
            clear(usernameBytes);
            clear(passwordBytes);
        }
    }

    private static byte[] makeHeader(int iterations, int usernameLength,
                                     int passwordLength, byte[] salt, byte[] nonce) {
        ByteBuffer header = ByteBuffer.allocate(FIXED_HEADER_LENGTH);
        header.putInt(iterations)
                .putInt(usernameLength)
                .putInt(passwordLength)
                .put(salt)
                .put(nonce);
        return header.array();
    }

    private static byte[] crypt(int mode, byte[] input, char[] keyPassword,
                                byte[] salt, byte[] nonce, int iterations,
                                byte[] authenticatedHeader)
            throws GeneralSecurityException {
        PBEKeySpec spec = new PBEKeySpec(keyPassword, salt, iterations, KEY_BITS);
        byte[] keyBytes = null;
        try {
            keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                    .generateSecret(spec).getEncoded();

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(mode, new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(authenticatedHeader);
            return cipher.doFinal(input);
        } finally {
            spec.clearPassword();
            clear(keyBytes);
        }
    }

    private static byte[] encode(char[] value) throws CharacterCodingException {
        ByteBuffer encoded = StandardCharsets.UTF_8.newEncoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .encode(CharBuffer.wrap(value));

        byte[] bytes = new byte[encoded.remaining()];
        encoded.get(bytes);
        if (encoded.hasArray()) Arrays.fill(encoded.array(), (byte) 0);
        return bytes;
    }

    private static char[] decode(byte[] value) throws CharacterCodingException {
        CharBuffer decoded = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT)
                .onUnmappableCharacter(CodingErrorAction.REPORT)
                .decode(ByteBuffer.wrap(value));

        char[] chars = new char[decoded.remaining()];
        decoded.get(chars);
        if (decoded.hasArray()) Arrays.fill(decoded.array(), '\0');
        return chars;
    }

    private static void clear(byte[] value) {
        if (value != null) Arrays.fill(value, (byte) 0);
    }

    public static final class Credentials implements AutoCloseable {
        private final char[] username;
        private final char[] password;

        Credentials(char[] username, char[] password) {
            this.username = username;
            this.password = password;
        }

        public char[] username() {
            return username;
        }

        public char[] password() {
            return password;
        }

        @Override
        public void close() {
            Arrays.fill(username, '\0');
            Arrays.fill(password, '\0');
        }
    }
}

