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

            JPanel paramPanel = new JPanel(new GridLayout(0, 1));
            paramPanel.setBorder(BorderFactory.createTitledBorder("Parameters"));
            addSlider(paramPanel, "Angle X", "ParamAngleX", -30, 30, 0);
            addSlider(paramPanel, "Angle Y", "ParamAngleY", -30, 30, 0);
            addSlider(paramPanel, "Eye L Open", "ParamEyeLOpen", 0, 1, 1);
            addSlider(paramPanel, "Eye R Open", "ParamEyeROpen", 0, 1, 1);
            addSlider(paramPanel, "Mouth Open", "ParamMouthOpenY", 0, 1, 0);
            addSlider(paramPanel, "Body X", "ParamBodyAngleX", -10, 10, 0);
            frame.add(paramPanel);

            JPanel viewPanel = new JPanel(new GridLayout(0, 1));
            viewPanel.setBorder(BorderFactory.createTitledBorder("View"));
            addScaleSlider(viewPanel, "Scale", 0.1f, 3.0f, 1.0f);
            addModelSelector(viewPanel);
            frame.add(viewPanel);

            frame.setVisible(true);
        });
    }

    private static void addModelSelector(JPanel panel) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel("Model");
        p.add(l, BorderLayout.WEST);
        String[] models = {"Hiyori", "Haru", "Mao", "Mark", "Natori", "Rice", "Wanko"};
        JComboBox<String> box = new JComboBox<>(models);
        box.addActionListener(e -> {
            String m = (String) box.getSelectedItem();
            sendRequest("/model?name=" + m);
        });
        p.add(box, BorderLayout.CENTER);
        panel.add(p);
    }

    private static void addScaleSlider(JPanel panel, String label, float min, float max, float initial) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel(label);
        p.add(l, BorderLayout.WEST);
        JSlider slider = new JSlider((int)(min * 100), (int)(max * 100), (int)(initial * 100));
        slider.addChangeListener(e -> {
            float val = slider.getValue() / 100.0f;
            sendRequest("/scale?value=" + val);
        });
        p.add(slider, BorderLayout.CENTER);
        panel.add(p);
    }

    private static void addSlider(JPanel panel, String label, String id, float min, float max, float initial) {
        JPanel p = new JPanel(new BorderLayout());
        JLabel l = new JLabel(label);
        p.add(l, BorderLayout.WEST);
        JSlider slider = new JSlider((int)(min * 100), (int)(max * 100), (int)(initial * 100));
        slider.addChangeListener(e -> {
            float val = slider.getValue() / 100.0f;
            sendRequest("/parameter?id=" + id + "&value=" + val);
        });
        p.add(slider, BorderLayout.CENTER);
        panel.add(p);
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