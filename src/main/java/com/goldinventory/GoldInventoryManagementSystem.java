package com.goldinventory;

import com.goldinventory.database.DBConnection;
import com.goldinventory.service.AuthService;
import com.goldinventory.ui.LoginFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentListener;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class GoldInventoryManagementSystem {

    public static void main(String[] args) {
        setLookAndFeel();
        SplashScreen splash = new SplashScreen();
        splash.showSplash();

        // Quick DB check in background then open Welcome
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() {
                splash.setStatus("Checking database connectivity...");
                try (Connection conn = DBConnection.getConnection()) {
                    if (conn == null || conn.isClosed()) return false;
                } catch (Exception e) {
                    return false;
                }
                return true;
            }

            @Override
            protected void done() {
                splash.hideSplash();
                SwingUtilities.invokeLater(() -> new WelcomeFrame().setVisible(true));
            }
        }.execute();
    }

    private static void setLookAndFeel() {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    return;
                }
            }
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}
    }

    // ---------- Attractive Splash ----------
    private static class SplashScreen {
        private final JWindow window;
        private final JLabel statusLabel;

        SplashScreen() {
            window = new JWindow();
            JPanel root = new JPanel(new BorderLayout(12, 12));
            root.setBorder(new EmptyBorder(18, 18, 18, 18));
            root.setBackground(Color.WHITE);

            JPanel top = new JPanel(new BorderLayout());
            top.setOpaque(false);
            JLabel brand = new JLabel("<html><span style='color:#D4AF37;font-weight:700;font-size:20px;'>GoldInventory</span><br><span style='font-size:11px;color:#333;'>Management System - DKTE</span></html>");
            brand.setBorder(new EmptyBorder(6,6,6,6));
            top.add(brand, BorderLayout.WEST);

            JLabel team = new JLabel("<html><span style='color:#666;font-size:11px;'>Team: Gold (Navin, Sarthak, Prasenjeet)</span></html>");
            team.setHorizontalAlignment(SwingConstants.RIGHT);
            top.add(team, BorderLayout.EAST);

            JLabel logo = new JLabel();
            logo.setHorizontalAlignment(SwingConstants.CENTER);

            JPanel bottom = new JPanel(new BorderLayout(6,6));
            bottom.setOpaque(false);
            statusLabel = new JLabel("Starting...");
            JProgressBar bar = new JProgressBar();
            bar.setIndeterminate(true);
            bottom.add(statusLabel, BorderLayout.NORTH);
            bottom.add(bar, BorderLayout.SOUTH);

            root.add(top, BorderLayout.NORTH);
            root.add(logo, BorderLayout.CENTER);
            root.add(bottom, BorderLayout.SOUTH);

            window.getContentPane().add(root);
            window.pack();
            window.setLocationRelativeTo(null);
        }

        void showSplash() { SwingUtilities.invokeLater(() -> window.setVisible(true)); }
        void setStatus(String s) { SwingUtilities.invokeLater(() -> statusLabel.setText(s)); }
        void hideSplash() { SwingUtilities.invokeLater(() -> window.dispose()); }
    }

    // ---------- Welcome Frame with animated gold coins ----------
    private static class WelcomeFrame extends JFrame {
        private final Color BG = new Color(245, 245, 247);
        private final Color ACCENT = new Color(13, 94, 163);
        private final CoinPanel coinPanel = new CoinPanel();

        WelcomeFrame() {
            setTitle("GoldInventory · Welcome");
            setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            setSize(880, 520);
            setLocationRelativeTo(null);
            initUI();
        }

        private void initUI() {
            JPanel root = new JPanel(new BorderLayout());
            root.setBackground(BG);
            root.setBorder(new EmptyBorder(18,18,18,18));

            JLabel header = new JLabel("<html><span style='color:#0A1F3C;font-size:26px;font-weight:700;'>GoldInventory</span><br><span style='color:#6b6b6b;'>Smart Inventory & Sales</span></html>");
            header.setBorder(new EmptyBorder(4,4,12,4));

            // Layered pane: coin animation behind content
            JLayeredPane layered = new JLayeredPane();
            layered.setPreferredSize(new Dimension(820, 380));

            JPanel contentHolder = new JPanel(new GridLayout(1, 2, 14, 14));
            contentHolder.setOpaque(false);
            contentHolder.setBounds(0, 0, 820, 380);

            // Left: features
            JPanel feats = new JPanel();
            feats.setLayout(new BoxLayout(feats, BoxLayout.Y_AXIS));
            feats.setOpaque(false);
            feats.add(featureCard("Manage Gold Items", "Add / Edit / Delete with safety checks"));
            feats.add(Box.createVerticalStrut(12));
            feats.add(featureCard("Secure Auth", "SHA-256 hashed passwords and role-based access"));
            feats.add(Box.createVerticalStrut(12));
            feats.add(featureCard("Sales & Reports", "Save sales, view history, export Excel"));

            // Right: actions
            JPanel actions = new JPanel();
            actions.setLayout(new BoxLayout(actions, BoxLayout.Y_AXIS));
            actions.setOpaque(false);
            actions.setBorder(new EmptyBorder(12,12,12,12));

            JLabel welcome = new JLabel("<html><span style='font-size:16px;font-weight:600;color:#0A1F3C;'>Welcome back</span><br><span style='color:#555;'>Login or create an account to continue</span></html>");
            welcome.setAlignmentX(Component.CENTER_ALIGNMENT);

            JButton loginBtn = styledButton("Login", new Color(212,175,55), Color.BLACK);
            loginBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            loginBtn.addActionListener(e -> {
                dispose();
                SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
            });

            JButton signupBtn = styledButton("Create Account", ACCENT, Color.WHITE);
            signupBtn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
            signupBtn.addActionListener(this::openSignup);

            // small tip strip
            JLabel tip = new JLabel("<html><small style='color:#666;'>Tip: Use strong passwords. Customers are default role.</small></html>");
            tip.setBorder(new EmptyBorder(10,0,0,0));
            tip.setAlignmentX(Component.CENTER_ALIGNMENT);

            actions.add(welcome);
            actions.add(Box.createVerticalStrut(18));
            actions.add(loginBtn);
            actions.add(Box.createVerticalStrut(12));
            actions.add(signupBtn);
            actions.add(Box.createVerticalStrut(8));
            actions.add(tip);
            actions.add(Box.createVerticalGlue());

            contentHolder.add(feats);
            contentHolder.add(actions);

            // place coin panel behind and content in front using layered pane
            coinPanel.setBounds(0, 0, 820, 380);
            layered.add(coinPanel, Integer.valueOf(0)); // bottom layer
            layered.add(contentHolder, Integer.valueOf(1)); // top layer

            root.add(header, BorderLayout.NORTH);
            root.add(layered, BorderLayout.CENTER);
            add(root);

            // start coin animation when window shows
            addWindowListener(new WindowAdapter() {
                @Override
                public void windowOpened(WindowEvent e) { coinPanel.start(); }
                @Override
                public void windowClosing(WindowEvent e) { coinPanel.stop(); }
            });
        }

        private JPanel featureCard(String title, String subtitle) {
            JPanel p = new JPanel(new BorderLayout());
            p.setBackground(new Color(255, 255, 255, 220));
            p.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(230,230,230)),
                    new EmptyBorder(12,12,12,12)
            ));
            JLabel t = new JLabel("<html><b style='color:#0A1F3C;'>" + title + "</b></html>");
            JLabel s = new JLabel("<html><span style='color:#666;'>" + subtitle + "</span></html>");
            p.add(t, BorderLayout.NORTH);
            p.add(s, BorderLayout.SOUTH);
            return p;
        }

        private JButton styledButton(String text, Color bg, Color fg) {
            JButton b = new JButton(text);
            b.setBackground(bg);
            b.setForeground(fg);
            b.setFocusPainted(false);
            b.setFont(new Font("Segoe UI", Font.BOLD, 14));
            b.setBorder(BorderFactory.createEmptyBorder(10,14,10,14));
            b.setCursor(new Cursor(Cursor.HAND_CURSOR));
            // hover effect
            b.addMouseListener(new MouseAdapter() {
                Color orig = b.getBackground();
                @Override public void mouseEntered(MouseEvent e) { b.setBackground(orig.darker()); }
                @Override public void mouseExited(MouseEvent e) { b.setBackground(orig); }
            });
            return b;
        }

        // Signup dialog with better strength meter
        private void openSignup(ActionEvent ev) {
            JDialog d = new JDialog(this, "Create Account", true);
            d.setSize(460, 420);
            d.setLocationRelativeTo(this);

            JPanel root = new JPanel(new BorderLayout(8,8));
            root.setBorder(new EmptyBorder(12,12,12,12));
            root.setBackground(Color.WHITE);

            JPanel form = new JPanel();
            form.setLayout(new GridBagLayout());
            form.setBackground(Color.WHITE);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.insets = new Insets(8,8,8,8);
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridx = 0; gbc.gridy = 0;

            form.add(new JLabel("Username:"), gbc);
            gbc.gridx = 1;
            JTextField usernameField = new JTextField();
            form.add(usernameField, gbc);

            gbc.gridx = 0; gbc.gridy++;
            form.add(new JLabel("Password:"), gbc);
            gbc.gridx = 1;
            JPasswordField pwd = new JPasswordField();
            form.add(pwd, gbc);

            gbc.gridx = 0; gbc.gridy++;
            form.add(new JLabel("Confirm Password:"), gbc);
            gbc.gridx = 1;
            JPasswordField pwd2 = new JPasswordField();
            form.add(pwd2, gbc);

            gbc.gridx = 0; gbc.gridy++;
            form.add(new JLabel("Role (default):"), gbc);
            gbc.gridx = 1;
            JComboBox<String> roleCombo = new JComboBox<>(new String[]{"customer"});
            roleCombo.setEnabled(false);
            form.add(roleCombo, gbc);

            // Strength UI
            gbc.gridx = 0; gbc.gridy++;
            gbc.gridwidth = 2;
            JPanel strengthRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            strengthRow.setOpaque(false);
            JProgressBar strengthBar = new JProgressBar(0, 100);
            strengthBar.setPreferredSize(new Dimension(220, 14));
            JLabel strengthText = new JLabel("Strength: Weak");
            strengthRow.add(strengthBar);
            strengthRow.add(strengthText);
            form.add(strengthRow, gbc);
            gbc.gridwidth = 1;

            JLabel note = new JLabel("<html><small style='color:#777;'>Staff/admin accounts must be created by an administrator.</small></html>");
            note.setBorder(new EmptyBorder(6,0,6,0));

            JButton create = styledButton("Create Account", new Color(34,139,34), Color.WHITE);
            JButton cancel = styledButton("Cancel", Color.LIGHT_GRAY, Color.BLACK);
            JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            btns.setOpaque(false);
            btns.add(cancel);
            btns.add(create);

            root.add(note, BorderLayout.NORTH);
            root.add(form, BorderLayout.CENTER);
            root.add(btns, BorderLayout.SOUTH);

            // Strength listener (simple)
            DocumentListener upd = new DocumentListener() {
                void run() { updateStrength(new String(pwd.getPassword()), strengthBar, strengthText); }
                public void insertUpdate(javax.swing.event.DocumentEvent e) { run(); }
                public void removeUpdate(javax.swing.event.DocumentEvent e) { run(); }
                public void changedUpdate(javax.swing.event.DocumentEvent e) { run(); }
            };
            pwd.getDocument().addDocumentListener(upd);
            pwd2.getDocument().addDocumentListener(upd);

            cancel.addActionListener(e -> d.dispose());

            create.addActionListener(e -> {
                String username = usernameField.getText().trim();
                String password = new String(pwd.getPassword());
                String confirm = new String(pwd2.getPassword());
                String role = (String) roleCombo.getSelectedItem();

                if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
                    JOptionPane.showMessageDialog(d, "Please fill all fields", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (!password.equals(confirm)) {
                    JOptionPane.showMessageDialog(d, "Passwords do not match", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                if (password.length() < 8) {
                    JOptionPane.showMessageDialog(d, "Password must be at least 8 characters", "Validation", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                create.setEnabled(false);
                new SwingWorker<Boolean, Void>() {
                    private String message = "Creating account...";
                    @Override
                    protected Boolean doInBackground() {
                        try (Connection conn = DBConnection.getConnection()) {
                            try (PreparedStatement check = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
                                check.setString(1, username);
                                try (ResultSet rs = check.executeQuery()) {
                                    if (rs.next()) {
                                        message = "Username exists";
                                        return false;
                                    }
                                }
                            }
                            String hashed = AuthService.hashPassword(password);
                            try (PreparedStatement ins = conn.prepareStatement("INSERT INTO users (username, password, role) VALUES (?, ?, ?)")) {
                                ins.setString(1, username);
                                ins.setString(2, hashed);
                                ins.setString(3, role);
                                ins.executeUpdate();
                            }
                            return true;
                        } catch (Exception ex) {
                            message = ex.getMessage();
                            ex.printStackTrace();
                            return false;
                        }
                    }
                    @Override
                    protected void done() {
                        try {
                            boolean ok = get();
                            if (ok) {
                                JOptionPane.showMessageDialog(d, "Account created. You can now login.", "Success", JOptionPane.INFORMATION_MESSAGE);
                                d.dispose();
                            } else {
                                JOptionPane.showMessageDialog(d, "Could not create account: " + message, "Error", JOptionPane.ERROR_MESSAGE);
                                create.setEnabled(true);
                            }
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(d, "Error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
                            create.setEnabled(true);
                        }
                    }
                }.execute();
            });

            d.setResizable(false);
            d.add(root);
            d.setVisible(true);
        }

        private void updateStrength(String pwd, JProgressBar bar, JLabel text) {
            int score = 0;
            if (pwd.length() >= 8) score += 30;
            if (pwd.length() >= 12) score += 20;
            if (pwd.matches(".*\\d.*")) score += 20;
            if (pwd.matches(".*[a-z].*") && pwd.matches(".*[A-Z].*")) score += 15;
            if (pwd.matches(".*[^a-zA-Z0-9].*")) score += 15;
            score = Math.min(100, score);
            bar.setValue(score);
            if (score < 40) {
                bar.setForeground(new Color(220, 53, 69));
                text.setText("Strength: Weak");
            } else if (score < 75) {
                bar.setForeground(new Color(255, 193, 7));
                text.setText("Strength: Good");
            } else {
                bar.setForeground(new Color(46, 204, 113));
                text.setText("Strength: Strong");
            }
        }
    }

    // ---------- CoinPanel: animated raining gold-coin effect ----------
    private static class CoinPanel extends JComponent {
        private final List<Coin> coins = new ArrayList<>();
        private final Timer timer;
        private final ScheduledExecutorService spawner = Executors.newSingleThreadScheduledExecutor();
        private final int MAX_COINS = 120;
        private final Object lock = new Object();

        CoinPanel() {
            setOpaque(false);
            // animation timer
            timer = new Timer(30, e -> {
                updatePhysics();
                repaint();
            });
            // spawn a few initial coins
            for (int i = 0; i < 18; i++) spawnCoin(true);
            // periodic spawner to imitate shower
            spawner.scheduleAtFixedRate(() -> {
                if (coins.size() < MAX_COINS) spawnCoin(false);
            }, 0, 200, TimeUnit.MILLISECONDS);

            // small parallax on mouse move
            addMouseMotionListener(new MouseMotionAdapter() {
                @Override public void mouseMoved(MouseEvent e) {
                    // nudge velocities for subtle interactivity
                    synchronized (lock) {
                        for (int i = 0; i < Math.min(6, coins.size()); i++) {
                            coins.get(i).vx += (e.getX() - getWidth()/2) * 0.0008;
                        }
                    }
                }
            });
        }

        void start() { timer.start(); }
        void stop() {
            timer.stop();
            spawner.shutdownNow();
        }

        private void spawnCoin(boolean randomY) {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            int x = (int)(Math.random() * w);
            int y = randomY ? (int)(Math.random() * h) : -10 - (int)(Math.random() * 60);
            double vx = (Math.random() - 0.5) * 2.2;
            double vy = Math.random() * 1.6 + 1.0;
            float size = 10 + (float)(Math.random() * 18); // coin size
            synchronized (lock) { coins.add(new Coin(x, y, vx, vy, size)); }
        }

        private void updatePhysics() {
            int w = Math.max(1, getWidth());
            int h = Math.max(1, getHeight());
            synchronized (lock) {
                for (int i = coins.size() - 1; i >= 0; i--) {
                    Coin c = coins.get(i);
                    c.x += c.vx;
                    c.y += c.vy;
                    c.vy += 0.12; // gravity
                    c.angle += c.spin;
                    c.vx *= 0.999;
                    // remove off-screen
                    if (c.y - c.size > h + 40 || c.x < -80 || c.x > w + 80) {
                        coins.remove(i);
                    }
                }
            }
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            synchronized (lock) {
                for (Coin c : coins) {
                    AffineTransform old = g2.getTransform();
                    g2.translate(c.x, c.y);
                    g2.rotate(c.angle);
                    drawCoin(g2, c.size);
                    g2.setTransform(old);
                }
            }
            g2.dispose();
        }

        // draw stylized coin with gradient and rim
        private void drawCoin(Graphics2D g2, float size) {
            int s = Math.max(6, Math.round(size));
            // main gradient
            GradientPaint gp = new GradientPaint(0, 0, new Color(255, 220, 85),
                    s, s, new Color(200, 150, 40));
            g2.setPaint(gp);
            g2.fillOval(-s/2, -s/2, s, s);

            // rim
            g2.setColor(new Color(120, 80, 20, 160));
            g2.setStroke(new BasicStroke(Math.max(1f, s*0.08f)));
            g2.drawOval(-s/2, -s/2, s, s);

            // highlight
            g2.setColor(new Color(255,255,255,120));
            int hs = Math.max(1, s/4);
            g2.fillOval(-s/2 + hs, -s/2 + hs, hs, hs);

            // decorative mark (small circle or star)
            g2.setColor(new Color(255,230,150,210));
            int mark = Math.max(1, s/5);
            g2.fillOval(-mark/2, -mark/2, mark, mark);
        }

        private static class Coin {
            float x, y;
            double vx, vy;
            float size;
            double angle, spin;
            Coin(float x, float y, double vx, double vy, float size) {
                this.x = x; this.y = y;
                this.vx = vx; this.vy = vy;
                this.size = size;
                this.angle = Math.random() * Math.PI * 2;
                this.spin = (Math.random() - 0.5) * 0.2;
            }
        }
    }
}
