package dev.eatgrapes.live2d.example;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public class ControlPanel {
    private static final HttpClient client = HttpClient.newHttpClient();

    public static void show() {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Live2D Controller");
            frame.setLayout(new GridLayout(0, 1, 10, 10));
            frame.setSize(300, 300);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            JPanel motionPanel = new JPanel(new FlowLayout());
            motionPanel.setBorder(BorderFactory.createTitledBorder("Motions"));
            addMotionButton(motionPanel, "Idle", "idle");
            addMotionButton(motionPanel, "Tap Body", "tap_body");
            frame.add(motionPanel);

            JPanel exprPanel = new JPanel(new FlowLayout());
            exprPanel.setBorder(BorderFactory.createTitledBorder("Expressions"));
            addExprButton(exprPanel, "Default", "Default");
            frame.add(exprPanel);

            frame.setVisible(true);
        });
    }

    private static void addMotionButton(JPanel panel, String label, String id) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> sendRequest("/motion?id=" + id));
        panel.add(btn);
    }

    private static void addExprButton(JPanel panel, String label, String name) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> sendRequest("/expression?name=" + name));
        panel.add(btn);
    }

    private static void sendRequest(String path) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080" + path))
                    .GET()
                    .build();
            client.sendAsync(request, HttpResponse.BodyHandlers.ofString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}