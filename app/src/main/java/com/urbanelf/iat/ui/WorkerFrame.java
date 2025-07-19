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

package com.urbanelf.iat.ui;

import com.urbanelf.iat.Core;
import com.urbanelf.iat.ic.IC;
import com.urbanelf.iat.ic.state.ICWorkerState;
import com.urbanelf.iat.proto.ClientPacket;
import com.urbanelf.iat.proto.PacketException;
import com.urbanelf.iat.proto.PythonServer;
import com.urbanelf.iat.proto.ServerPacket;
import com.urbanelf.iat.proto.constants.ClientSA;
import com.urbanelf.iat.proto.constants.WorkerType;
import com.urbanelf.iat.util.PlatformUtils;

import org.json.JSONObject;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.math.BigDecimal;
import java.net.URI;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;

public class WorkerFrame extends JFrame {
    private static final String TAG = WorkerFrame.class.getSimpleName();

    private final IC ic;
    private final WorkerType workerType;
    private final PythonServer.ServerListener serverListener;
    private String workerId;

    private final JPanel statePanelWrapper;
    private final JPanel progressiveStatePanel;
    private final JPanel selectiveStatePanel;

    private final JLabel stateNote;
    private final JLabel stateHint;
    private final JProgressBar stateProgress;

    public WorkerFrame(IC ic, WorkerType workerType) {
        this.ic = ic;
        this.workerType = workerType;

        serverListener = new PythonServer.ServerListener() {
            @Override
            public void packetReceived(ServerPacket packet) {
                if (workerId != null && !packet.getWorkerId().equals(workerId))
                    return;

                try {
                    switch (packet.getSharedAction()) {
                        case UUID_AVAILABLE -> workerId = (String) packet.getData().get("uuid");
                        case CHROMEDRIVER_ERROR -> {
                            final String stacktrace = (String) packet.getData().get("stacktrace");

                            // Network failure
                            final String networkText = """
                                    IAT failed to start chromedriver.
                                    
                                    Please verify that you're connected to the internet, and try again.
                                    
                                    """;

                            // Fallback (Version or exe issue)
                            final String link = "https://www.google.com/chrome/";
                            final String auxLink;

                            switch (PlatformUtils.getRunningPlatform()) {
                                case Windows -> auxLink = "https://google-chrome.en.uptodown.com/windows/versions";
                                case Mac ->  auxLink = "https://google-chrome.en.uptodown.com/mac/versions";
                                default -> auxLink = "";
                            }

                            final String generalText = "IAT failed to start chromedriver.\n"
                                    + "Please verify you have Chrome installed on your machine, and try again.\n\n"
                                    + "Download at: " + link + "\n\n"
                                    + (auxLink.isEmpty() ? "" : "If the issue persists, try downloading it instead from:\n\n"
                                    + auxLink + "\n");

                            // Determine exception
                            final String text;
                            final String[] options;
                            final Runnable okAction;
                            if (stacktrace.contains("Network is unreachable")) {
                                options = new String[] {"Close", "OK"};
                                text = networkText;
                                okAction = () -> {};
                            } else {
                                options = new String[] {"Close", auxLink.isEmpty() ? "Open Link" : "Open Links"};
                                text = generalText;
                                okAction = () -> {
                                    try {
                                        Desktop.getDesktop().browse(URI.create(link));
                                        Desktop.getDesktop().browse(URI.create(auxLink));
                                    } catch (Exception e) {
                                        Core.error(TAG, e);
                                    }
                                };
                            }

                            SwingUtilities.invokeLater(() -> {
                                int n = JOptionPane.showOptionDialog(null,
                                        text,
                                        "An error occurred",
                                        JOptionPane.YES_NO_OPTION,
                                        JOptionPane.ERROR_MESSAGE,
                                        null,
                                        options,
                                        options[1]);

                                if (n == 1)
                                    okAction.run();

                                // Destroy frame
                                dispatchEvent(new WindowEvent(WorkerFrame.this, WindowEvent.WINDOW_CLOSING));
                            });
                        }
                        case STATE_CHANGED -> {
                            final ICWorkerState state = new ICWorkerState((JSONObject) packet.getData().get("state"));

                            SwingUtilities.invokeLater(() -> {
                                stateNote.setText(state.getNote());
                                stateHint.setText(state.getHint());
                                statePanelWrapper.removeAll();
                                if (state.isProgressive()) {
                                    stateProgress.setIndeterminate(state.isIndeterminate());
                                    statePanelWrapper.add(progressiveStatePanel);
                                } else {
                                    layoutSelectiveStatePanel(state);
                                    statePanelWrapper.add(selectiveStatePanel);
                                }
                            });
                        }
                        case PROGRESS_UPDATE -> {
                            final BigDecimal object = (BigDecimal) packet.getData().get("progress");
                            SwingUtilities.invokeLater(() -> stateProgress.setValue((int) (object.floatValue() * stateProgress.getMaximum())));
                        }
                        case RESULT_AVAILABLE -> {
                        }
                    }
                } catch (Exception e) {
                    Core.error(TAG, "Malformed packet for " + workerType.name(), e);
                }
            }

            @Override
            public void exceptionThrown(Throwable throwable) {
                Core.error(TAG, throwable);
            }
        };

        PythonServer.addListener(serverListener);

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                PythonServer.writePacket(new ClientPacket(workerId, ClientSA.TERMINATE_WORKER));
                PythonServer.removeListener(serverListener);
            }
        });

        setTitle(workerType.getUiTitle());
        setMinimumSize(new Dimension(320, (int) (320 * 1.7f)));
        setLocationByPlatform(true);

        stateNote = new JLabel();
        stateNote.setHorizontalAlignment(JLabel.CENTER);
        stateHint = new JLabel();
        stateHint.setHorizontalAlignment(JLabel.RIGHT);
        stateHint.setFont(Fonts.SMALL);
        stateProgress = new JProgressBar();

        statePanelWrapper = new JPanel();
        progressiveStatePanel = layoutProgressiveStatePanel();
        selectiveStatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));

        final JPanel panel = new JPanel(new GridBagLayout());
        panel.add(stateNote, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.insets = new Insets(0, 0, 8, 0);
                this.weightx = 1;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        panel.add(statePanelWrapper, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 1;
                this.weightx = 1;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });

        final JPanel panelWrapper = new JPanel(new BorderLayout(0, 8));
        panelWrapper.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        panelWrapper.add(panel, BorderLayout.CENTER);
        panelWrapper.add(stateHint, BorderLayout.SOUTH);

        setContentPane(panelWrapper);

        //pack();

        /////////////////////////////////////////////////////////////

        // Start worker
        PythonServer.writePacket(new ClientPacket(ClientSA.DISPATCH_WORKER)
                .addData("ic", ic.toJson())
                .addData("worker_type", workerType));

        //stateNote.setText("Please sign in to continue.");
        //stateProgress.setIndeterminate(true);
        //layoutSelectiveStatePanel(ICWorkerState.AUTH_REQUIRED);
        //statePanelWrapper.add(selectiveStatePanel);
    }

    private JPanel layoutProgressiveStatePanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        panel.add(stateProgress, new GridBagConstraints() {
            {
                this.gridx = 0;
                this.gridy = 0;
                this.weightx = 1;
                this.fill = GridBagConstraints.HORIZONTAL;
            }
        });
        return panel;
    }

    private void layoutSelectiveStatePanel(ICWorkerState state) {
        selectiveStatePanel.removeAll();
        for (ICWorkerState.ButtonConfig config : state.getButtonConfigs()) {
            final JButton button = new JButton(config.text());
            button.setFocusable(false);
            button.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent mouseEvent) {
                    switch (config.sharedAction()) {
                        case OPEN_LOG -> {
                            try {
                                Desktop.getDesktop().open(Core.getLogFile());
                            } catch (Exception e) {
                                Core.error(TAG, "Error opening log: '" + Core.getLogFile().getName() + "'", e);
                            }
                        }
                        case EXPORT_CONTENT -> {
                        }
                        case TERMINATE -> {
                            // Destroy frame
                            dispatchEvent(new WindowEvent(WorkerFrame.this, WindowEvent.WINDOW_CLOSING));
                        }
                    }
                    PythonServer.writePacket(new ClientPacket(workerId, ClientSA.STATE_INPUT)
                            .addData("client_object", config.clientObject()));
                }
            });
            selectiveStatePanel.add(button);
        }
    }

    public IC getIc() {
        return ic;
    }

    public WorkerType getWorkerType() {
        return workerType;
    }
}
