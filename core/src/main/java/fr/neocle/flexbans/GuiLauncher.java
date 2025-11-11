package fr.neocle.flexbans;

import javax.swing.*;
import java.awt.*;

public class GuiLauncher {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("FlexBans Plugin");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setSize(450, 250);
            frame.setLocationRelativeTo(null);

            JLabel title = new JLabel("Oops! That's not how to use FlexBans.", SwingConstants.CENTER);
            title.setFont(new Font("Segoe UI", Font.BOLD, 16));
            title.setBorder(BorderFactory.createEmptyBorder(20, 10, 10, 10));
            frame.add(title, BorderLayout.NORTH);

            JTextArea message = new JTextArea(
                    """
                    FlexBans is a Minecraft server plugin, not a standalone program.
    
                    To use it:
                    1. Place FlexBans.jar in your server's /plugins folder.
                    2. Start your Bukkit/Spigot/Paper, Bungee or Velocity server.
                    3. The plugin will load automatically and create its config files.
    
                    Double-clicking this file won't start the plugin.
                    """
            );
            message.setEditable(false);
            message.setFocusable(false);
            message.setWrapStyleWord(true);
            message.setLineWrap(true);
            message.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            message.setBackground(frame.getBackground());
            message.setBorder(BorderFactory.createEmptyBorder(0, 20, 10, 20));
            frame.add(message, BorderLayout.CENTER);

            JButton closeButton = new JButton("OK, got it!");
            closeButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            closeButton.addActionListener(e -> frame.dispose());
            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);
            frame.add(buttonPanel, BorderLayout.SOUTH);

            frame.setVisible(true);
        });
    }
}
