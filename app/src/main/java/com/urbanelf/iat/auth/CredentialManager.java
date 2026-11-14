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

import com.urbanelf.iat.Core;
import com.urbanelf.iat.proto.ClientPacket;
import com.urbanelf.iat.proto.PythonServer;
import com.urbanelf.iat.proto.constants.ClientSA;
import com.urbanelf.iat.util.FileTree;
import com.urbanelf.iat.util.UIUtils;

import java.awt.Component;
import java.awt.Desktop;
import java.awt.GridLayout;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;

public final class CredentialManager {
    private static final String TAG = CredentialManager.class.getSimpleName();
    private static final Path STORE_FILE = FileTree.getRootPath().resolve("auth.bin");

    private static final CredentialStore credentialStore = new CredentialStore(STORE_FILE);
    private static final AtomicBoolean deleteCredentialOnExit = new AtomicBoolean(false);

    private CredentialManager() {}

    public static void showSaveCredentialDialog(Component parent) {
        UIUtils.executeOnSwingThread(() -> {
            JTextField nameField = new JTextField(20);
            JPasswordField passwordField = new JPasswordField(20);
            JCheckBox deleteOnExit = new JCheckBox("Automatically purge on exit", true);
            JCheckBox autoLoad = new JCheckBox("Load for this app session", true);

            JPanel valuePanel = new JPanel(new GridLayout(0, 1, 6, 6));
            valuePanel.add(new JPanel() {
                {
                    add(new JLabel("Username:"));
                    add(nameField);
                }
            });
            valuePanel.add(new JPanel() {
                {
                    add(new JLabel("Password:"));
                    add(passwordField);
                }
            });

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(new JLabel("""
                    <html><body style='width: 310px'>
                    Enter your login for the Invision Community you plan on archiving from.
                    This feature allows for automatic authentication, which is useful when running
                    long operations that require multiple refreshes.
                    <br><br>
                    <b>NOTE:</b> This is saved to your local computer in a SINGLE password-protected file.
                    You can delete it at any time via <b>Session -> Purge Credential</b>.
                    To use it, you must decrypt the credential each time you start the app via
                    <b>Session -> Load Credential</b>. Only the file password will be stored in RAM.
                    <br><br>
                    If you are concerned, you can always change your password on the community
                    once the archiving is finished. Or simply opt out altogether.
                    </body></html>"""));
            mainPanel.add(Box.createVerticalStrut(8));
            mainPanel.add(valuePanel);
            mainPanel.add(Box.createVerticalStrut(8));
            mainPanel.add(deleteOnExit);
            mainPanel.add(Box.createVerticalStrut(4));
            mainPanel.add(autoLoad);

            int result = JOptionPane.showConfirmDialog(
                    parent, mainPanel, "Set Temporary Login Credential",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                clear(passwordField);
                return;
            }

            char[] passwordChars = passwordField.getPassword();
            clear(passwordField);

            // Validate username
            if (nameField.getText().isBlank()) {
                showError(parent, "Username cannot be empty. Try again.");
                return;
            }
            // Validate password
            if (passwordChars.length == 0) {
                showError(parent, "Password cannot be empty. Try again.");
                return;
            }

            showFilePasswordDialog(parent, nameField.getText(), passwordChars, deleteOnExit.isSelected(), autoLoad.isSelected());

            // Clear array (parameter copies handled in above method)
            Arrays.fill(passwordChars, '\0');
        });
    }

    private static void showFilePasswordDialog(Component parent, String username, char[] passwordChars, boolean deleteOnExit, boolean autoLoad) {
        JPasswordField keyField = new JPasswordField(20);
        JPasswordField confirmField = new JPasswordField(20);

        JPanel keyPanel = new JPanel(new GridLayout(0, 1, 0, 6));
        keyPanel.add(new JLabel("Set file password:"));
        keyPanel.add(keyField);
        keyPanel.add(new JLabel("Confirm file password:"));
        keyPanel.add(confirmField);

        int result = JOptionPane.showConfirmDialog(
                parent, keyPanel, "Protect Credential File",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        char[] keyChars = keyField.getPassword();
        char[] confirmChars = confirmField.getPassword();
        clear(keyField);
        clear(confirmField);

        if (result != JOptionPane.OK_OPTION) return;

        if (keyChars.length == 0) {
            showError(parent, "File password cannot be empty.");
            UIUtils.executeOnSwingThread(() -> showFilePasswordDialog(parent, username, passwordChars, deleteOnExit, autoLoad));
            return;
        }

        if (!Arrays.equals(keyChars, confirmChars)) {
            showError(parent, "The file passwords do not match.");
            UIUtils.executeOnSwingThread(() -> showFilePasswordDialog(parent, username, passwordChars, deleteOnExit, autoLoad));
            return;
        }

        try {
            // Write credential
            credentialStore.save(username.toCharArray(), passwordChars, keyChars);
            // Update flag only after it has successfully written (i.e., on a cred-by-cred basis)
            deleteCredentialOnExit.set(deleteOnExit);

            JOptionPane.showMessageDialog(parent,
                    "Credential file " + (autoLoad ? "saved, encrypted, and loaded." : "saved and encrypted.")
                            +"\nAutomatic sign-in will now work on relevant communities."
                    + "\n\n(Hint: You can delete it at any time via Session -> Purge Credential)",
                    "Operation Successful",
                    JOptionPane.INFORMATION_MESSAGE);

            // Load automatically (only if specified)
            if (autoLoad) {
                loadCredential(keyChars);
            }

        } catch (Exception e) {
            Core.error(TAG, "CredentialStore operation failed (autoLoad: " + autoLoad + ")", e);
            showError(parent, "Error writing" + (autoLoad ? "/loading" : "") + " encrypted file. Check log for details.");
        } finally {
            Arrays.fill(keyChars, '\0');
            Arrays.fill(confirmChars, '\0');
            Arrays.fill(passwordChars, '\0');
        }
    }

    private static void clear(JPasswordField field) {
        char[] chars = field.getPassword();
        Arrays.fill(chars, '\0');
        field.setText("");
    }

    private static void showError(Component parent, String message) {
        JOptionPane.showMessageDialog(
                parent, message, "Operation Cancelled", JOptionPane.ERROR_MESSAGE);
    }

    private static void showFileMissingError(Component parent, String title) {
        JOptionPane.showMessageDialog(parent,
                "Credential file does not exist.\n\nYou can optionally configure one (Session -> Set Login Credential)",
                title,
                JOptionPane.INFORMATION_MESSAGE);
    }

    public static void showLoadCredentialDialog(Component parent) {
        UIUtils.executeOnSwingThread(() -> {
            if (!Files.exists(STORE_FILE)) {
                showFileMissingError(parent, "Load Credential");
                return;
            }

            JPasswordField keyField = new JPasswordField(20);

            JPanel mainPanel = new JPanel();
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.add(new JLabel("""
                    <html><body style='width: 310px'>
                    Input the file password to decrypt your login credential.
                    <br><br>
                    <b>NOTE:</b> This will store the FILE password in RAM for the remainder of the app session.
                    You can always delete your credential file via <b>Session -> Purge Credential</b>.
                    </body></html>"""));
            mainPanel.add(Box.createVerticalStrut(8));
            mainPanel.add(new JPanel() {
                {
                    add(new JLabel("Password:"));
                    add(keyField);
                }
            });

            int result = JOptionPane.showConfirmDialog(
                    parent, mainPanel, "Load Credential",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

            if (result != JOptionPane.OK_OPTION) {
                clear(keyField);
                return;
            }

            char[] keyChars = keyField.getPassword();

            try {
                loadCredential(keyChars);
            } catch (GeneralSecurityException e) {
                Core.warning(TAG, "Failed authentication for credential decryption.");
                JOptionPane.showMessageDialog(
                        parent, "Decryption failed: incorrect password", "Operation Cancelled", JOptionPane.WARNING_MESSAGE);
                return;
            } catch (IOException e) {
                Core.error(TAG, "Error while loading credential", e);
                showError(parent, "Error decrypting file. Check log for details.");
                return;
            } finally {
                Arrays.fill(keyChars, '\0');
            }

            JOptionPane.showMessageDialog(parent,
                    """
                            Credential decrypted and loaded successfully.
                            Automatic sign-in will now work on relevant communities.""",
                    "Operation Successful",
                    JOptionPane.INFORMATION_MESSAGE);
        });
    }

    public static void showDeleteCredentialDialog(Component parent) {
        UIUtils.executeOnSwingThread(() -> {
            if (!Files.exists(STORE_FILE)) {
                showFileMissingError(parent, "Purge Credential");
                return;
            }

            // File exists, prompt to confirm purge
            int result = JOptionPane.showConfirmDialog(parent,
                    "Do you want to purge the stored credential?",
                    "Purge Credential",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (result == JOptionPane.YES_OPTION) {
                boolean success = deleteCredential();
                if (success) {
                    JOptionPane.showMessageDialog(parent,
                            "Credential file deleted successfully.",
                            "Purge Credential",
                            JOptionPane.INFORMATION_MESSAGE);
                } else {
                    Object[] options2 = {"Open Log", "Close"};
                    int result2 = JOptionPane.showOptionDialog(
                            parent,
                            "FAILED to delete credential (check log for details)."
                                    + "\n\nIt is advisable to delete it manually via the path given below, by whatever means necessary."
                                    + "\n\nCredential path: " + STORE_FILE,
                            "Purge Credential",
                            JOptionPane.DEFAULT_OPTION,
                            JOptionPane.WARNING_MESSAGE,
                            null,
                            options2,
                            options2[0]
                    );

                    if (result2 == 0) {
                        try {
                            Desktop.getDesktop().open(Core.getLogFile());
                        } catch (Exception e) {
                            Core.error(TAG, "Error opening log: '" + Core.getLogFile().getName() + "'", e);
                        }
                    }
                }
            }
        });
    }

    private static void loadCredential(char[] keyChars) throws GeneralSecurityException, IOException {
        try (CredentialStore.Credentials credentials = credentialStore.load(keyChars)) {
            // Only write to server if credentials decrypt successfully (automatically closed)
            PythonServer.silentWritePacket(new ClientPacket(ClientSA.CREDENTIAL_LOADED)
                    .addData("key", new String(keyChars)));
        } finally {
            Arrays.fill(keyChars, '\0');
        }
    }

    private static boolean deleteCredential() {
        try {
            Files.delete(STORE_FILE);
            Core.info(TAG, "Deleted credential successfully.");
            // Notify server
            PythonServer.silentWritePacket(new ClientPacket(ClientSA.CREDENTIAL_LOADED)
                    .addData("key", null));
            return true;
        } catch (IOException e) {
            Core.error(TAG, "FAILED to delete credential '" + STORE_FILE + "'", e);
            return false;
        }
    }

    public static void shutdown() {
        if (deleteCredentialOnExit.get()) {
            deleteCredential();
        }
    }
}
