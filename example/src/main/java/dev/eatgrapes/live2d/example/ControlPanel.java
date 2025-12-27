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
            frame.setLayout(new FlowLayout());
            frame.setSize(300, 200);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            addButton(frame, "Idle Motion", "m01");
            addButton(frame, "Tap Body", "m04");

            frame.setVisible(true);
        });
    }

    private static void addButton(JFrame frame, String label, String motionId) {
        JButton btn = new JButton(label);
        btn.addActionListener(e -> sendRequest("/motion?id=" + motionId));
        frame.add(btn);
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
