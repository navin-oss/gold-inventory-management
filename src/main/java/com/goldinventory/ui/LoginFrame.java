
package com.goldinventory.ui;

import com.goldinventory.service.AuthService;
import com.goldinventory.ui.admin.AdminDashboardFrame;
import com.goldinventory.ui.customer.CustomerDashboardFrame;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Premium Login & Registration – single-file edition
 * – Instant Login  |  Instant Register
 * – Both main buttons 200×50 px (same size)
 * – Glass-morphism cards, micro-glow focus, 60 fps coin-rain
 */
public class LoginFrame extends JFrame {

    /* =====================  UI FIELDS  ===================== */
    private JTextField usernameField, regUsernameField;
    private JPasswordField passwordField, regPasswordField, regConfirmField;
    private JButton loginButton, registerButton, switchToRegisterBtn, switchToLoginBtn;
    private JButton quickDemoBtn, skipAnimationBtn;
    private JButton loginCardRegisterBtn;          // NEW – explicit register on login card
    private JCheckBox rememberMe;
    private JCheckBox agreeTerms;
    private JButton forgotBtn;

    private CardLayout cardLayout;
    private JPanel mainPanel, animatedBackgroundPanel;

    /* password strength */
    private JLabel strengthLabel;
    private JProgressBar strengthProgressBar;
    private JPanel requirementsPanel;
    private JLabel lengthReq, digitReq, lowerReq, upperReq, specialReq;

    /* colours */
    private static final Color DARK_BLUE_GRADIENT_START = new Color(8, 18, 45);
    private static final Color DARK_BLUE_GRADIENT_END   = new Color(15, 30, 70);
    private static final Color PREMIUM_GOLD = new Color(255, 200, 0);
    private static final Color LIGHT_GOLD   = new Color(255, 230, 160);
    private static final Color GLOW_GOLD    = new Color(255, 240, 100, 100);
    private static final Color CARD_BG      = new Color(25, 40, 75, 220);
    private static final Color SUCCESS      = new Color(46, 204, 113);
    private static final Color WARNING      = new Color(241, 196, 15);
    private static final Color DANGER       = new Color(231, 76, 60);
    private static final Color NEUTRAL      = new Color(149, 165, 166);
    private static final Color GLASS_WHITE  = new Color(255, 255, 255, 15);

    /* fonts */
    private static final Font HEADER_FONT      = new Font("Segoe UI", Font.BOLD, 32);
    private static final Font SUBHEADER_FONT   = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font LABEL_FONT       = new Font("Segoe UI", Font.BOLD, 14);
    private static final Font BUTTON_FONT      = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font TEXT_BUTTON_FONT = new Font("Segoe UI", Font.PLAIN, 14);
    private static final Font SMALL_FONT       = new Font("Segoe UI", Font.PLAIN, 12);

    /* animation */
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private float animationOffset = 0.0f;
    private float glowPulse = 0.0f;
    private boolean glowDirection = true;

    /* misc */
    private static final int MIN_PASSWORD_LENGTH = 8;
    private boolean quickAccessEnabled = true;
    private Timer preloadTimer;
    private JDialog loadingDialog;
    private Timer loadingDotsTimer;

    /* ======================================================= */
    /*                      CONSTRUCTOR                        */
    /* ======================================================= */
    public LoginFrame() {
        initializeUI();
        startBackgroundAnimation();
        preloadResources();
        loadRememberedUsername();
    }

    /* ======================================================= */
    /*                   INITIALISATION                        */
    /* ======================================================= */
    private void initializeUI() {
        setTitle("💰 GoldInventory · Premium Access");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        setSize(620, 720);
        setLocationRelativeTo(null);

        /* ---- animated background ---- */
        animatedBackgroundPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

                int w = getWidth(), h = getHeight();
                GradientPaint gp = new GradientPaint(
                        0, h * animationOffset, DARK_BLUE_GRADIENT_START,
                        w, h * (1 - animationOffset), DARK_BLUE_GRADIENT_END);
                g2.setPaint(gp);
                g2.fillRect(0, 0, w, h);

                /* sparkles */
                g2.setColor(new Color(255, 215, 0, 40));
                for (int i = 0; i < 80; i++) {
                    int x = (int) (Math.random() * w);
                    int y = (int) (Math.random() * h);
                    int size = 1 + (int) (Math.random() * 3);
                    float alpha = 0.3f + (float) Math.random() * 0.7f;
                    g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                    g2.fill(new Ellipse2D.Double(x, y, size, size));
                }
                g2.setComposite(AlphaComposite.SrcOver);

                /* pulsing halo */
                g2.setColor(new Color(255, 215, 0, (int) (30 + 20 * Math.sin(glowPulse))));
                for (int i = 0; i < 5; i++) {
                    int radius = 100 + i * 60;
                    g2.fill(new Ellipse2D.Double(w / 2 - radius, h / 2 - radius, radius * 2, radius * 2));
                }
            }
        };
        animatedBackgroundPanel.setLayout(new GridBagLayout());
        getContentPane().add(animatedBackgroundPanel, BorderLayout.CENTER);

        /* ---- card panel ---- */
        mainPanel = new JPanel(cardLayout = new CardLayout());
        mainPanel.setOpaque(false);
        mainPanel.setBorder(new EmptyBorder(30, 30, 30, 30));

        mainPanel.add(createEnhancedLoginPanel(), "login");
        mainPanel.add(createEnhancedRegisterPanel(), "register");

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.anchor = GridBagConstraints.CENTER;
        animatedBackgroundPanel.add(mainPanel, gbc);
    }

    /* ======================================================= */
    /*                   LOGIN PANEL                           */
    /* ======================================================= */
    private JPanel createEnhancedLoginPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20)) {
            /* glass-morphism lift */
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setColor(new Color(255, 215, 0, 120));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 25, 25);
                g2.dispose();
            }
        };
        panel.setBackground(CARD_BG);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        /* header */
        JLabel header = new JLabel("💰 Premium Login", JLabel.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(LIGHT_GOLD);
        JLabel sub = new JLabel("Secure access to your gold inventory", JLabel.CENTER);
        sub.setFont(SUBHEADER_FONT);
        sub.setForeground(new Color(200, 200, 200));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(header, BorderLayout.CENTER);
        headerPanel.add(sub, BorderLayout.SOUTH);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        panel.add(headerPanel, BorderLayout.NORTH);

        /* form */
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(createLabel("👤 Username:"), gbc);
        gbc.gridx = 1;
        usernameField = createEnhancedTextField("Enter your username");
        form.add(usernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(createLabel("🔒 Password:"), gbc);
        gbc.gridx = 1;
        passwordField = createEnhancedPasswordField("Enter your password");
        JPanel pwdPanel = new JPanel(new BorderLayout());
        pwdPanel.setOpaque(false);
        pwdPanel.add(passwordField, BorderLayout.CENTER);
        JToggleButton showPwd = createShowPasswordToggle(passwordField);
        pwdPanel.add(showPwd, BorderLayout.EAST);
        form.add(pwdPanel, gbc);

        /* options */
        rememberMe = new JCheckBox("Remember me");
        rememberMe.setOpaque(false);
        rememberMe.setForeground(Color.WHITE);
        rememberMe.setFont(SMALL_FONT);
        forgotBtn = createTextButton("Forgot Password?");
        forgotBtn.addActionListener(e -> showEnhancedMessage("Forgot Password", "Contact support at support@goldinventory.com", NEUTRAL));
        JPanel optionsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 0));
        optionsPanel.setOpaque(false);
        optionsPanel.add(rememberMe);
        optionsPanel.add(forgotBtn);
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        form.add(optionsPanel, gbc);

        /* quick access */
        JPanel quickPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 5));
        quickPanel.setOpaque(false);
        quickDemoBtn = createQuickAccessButton("🚀 Quick Demo");
        skipAnimationBtn = createQuickAccessButton("⚡ Skip Animation");
        quickPanel.add(quickDemoBtn);
        quickPanel.add(skipAnimationBtn);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(quickPanel, gbc);

        /* buttons – SAME HEIGHT / WIDTH */
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setOpaque(false);
        loginButton = createPremiumGoldButton("🔐 Instant Login", 200, 50);
        loginCardRegisterBtn = createPremiumGoldButton("✨ Register", 200, 50);   // SAME SIZE
        switchToRegisterBtn = createTextButton("or Create New Account →");
        btnPanel.add(loginButton);
        btnPanel.add(loginCardRegisterBtn);
        btnPanel.add(switchToRegisterBtn);
        gbc.gridy = 4;
        gbc.insets = new Insets(25, 10, 10, 10);
        form.add(btnPanel, gbc);

        panel.add(form, BorderLayout.CENTER);

        /* listeners */
        loginButton.addActionListener(e -> attemptFastLogin());
        passwordField.addActionListener(e -> attemptFastLogin());
        loginCardRegisterBtn.addActionListener(e -> cardLayout.show(mainPanel, "register"));
        switchToRegisterBtn.addActionListener(e -> cardLayout.show(mainPanel, "register"));
        quickDemoBtn.addActionListener(e -> showQuickDemo());
        skipAnimationBtn.addActionListener(e -> setQuickAccess(true));

        return panel;
    }

    /* ======================================================= */
    /*                REGISTRATION PANEL                       */
    /* ======================================================= */
    private JPanel createEnhancedRegisterPanel() {
        JPanel panel = new JPanel(new BorderLayout(20, 20)) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(GLASS_WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 25, 25);
                g2.setColor(new Color(255, 215, 0, 120));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 25, 25);
                g2.dispose();
            }
        };
        panel.setBackground(CARD_BG);
        panel.setBorder(new EmptyBorder(30, 30, 30, 30));

        /* header */
        JLabel header = new JLabel("💰 Premium Registration", JLabel.CENTER);
        header.setFont(HEADER_FONT);
        header.setForeground(LIGHT_GOLD);
        JLabel sub = new JLabel("Join our gold inventory management system", JLabel.CENTER);
        sub.setFont(SUBHEADER_FONT);
        sub.setForeground(new Color(200, 200, 200));
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setOpaque(false);
        headerPanel.add(header, BorderLayout.CENTER);
        headerPanel.add(sub, BorderLayout.SOUTH);
        headerPanel.setBorder(new EmptyBorder(0, 0, 20, 0));
        panel.add(headerPanel, BorderLayout.NORTH);

        /* form */
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 10, 8, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridx = 0;
        gbc.gridy = 0;
        form.add(createLabel("👤 Username:"), gbc);
        gbc.gridx = 1;
        regUsernameField = createEnhancedTextField("Choose a username");
        form.add(regUsernameField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        form.add(createLabel("🔒 Password:"), gbc);
        gbc.gridx = 1;
        regPasswordField = createEnhancedPasswordField("Create a strong password");
        JPanel regPwdPanel = new JPanel(new BorderLayout());
        regPwdPanel.setOpaque(false);
        regPwdPanel.add(regPasswordField, BorderLayout.CENTER);
        JToggleButton showRegPwd = createShowPasswordToggle(regPasswordField);
        regPwdPanel.add(showRegPwd, BorderLayout.EAST);
        form.add(regPwdPanel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        form.add(createLabel("✅ Confirm:"), gbc);
        gbc.gridx = 1;
        regConfirmField = createEnhancedPasswordField("Confirm your password");
        JPanel confirmPanel = new JPanel(new BorderLayout());
        confirmPanel.setOpaque(false);
        confirmPanel.add(regConfirmField, BorderLayout.CENTER);
        JToggleButton showConfirm = createShowPasswordToggle(regConfirmField);
        confirmPanel.add(showConfirm, BorderLayout.EAST);
        form.add(confirmPanel, gbc);

        /* strength meter */
        strengthProgressBar = new JProgressBar(0, 100) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(50, 60, 90));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 15, 15);
                int width = (int) (getPercentComplete() * getWidth());
                GradientPaint gp = new GradientPaint(0, 0, getForeground(), width, 0,
                        new Color(getForeground().getRed(), getForeground().getGreen(), getForeground().getBlue(), 200));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, width, getHeight(), 15, 15);
            }
        };
        strengthProgressBar.setPreferredSize(new Dimension(200, 15));
        strengthProgressBar.setStringPainted(false);
        strengthProgressBar.setBorderPainted(false);
        strengthProgressBar.setBackground(new Color(50, 60, 90));
        strengthProgressBar.setForeground(DANGER);

        strengthLabel = new JLabel("Very Weak");
        strengthLabel.setForeground(DANGER);
        strengthLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));

        JPanel strengthPanel = new JPanel(new BorderLayout(5, 5));
        strengthPanel.setOpaque(false);
        strengthPanel.add(strengthProgressBar, BorderLayout.CENTER);
        strengthPanel.add(strengthLabel, BorderLayout.EAST);
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 2;
        form.add(strengthPanel, gbc);

        /* requirements */
        requirementsPanel = new JPanel(new GridLayout(5, 1, 2, 2));
        requirementsPanel.setOpaque(false);
        requirementsPanel.setBorder(new EmptyBorder(5, 0, 15, 0));
        lengthReq = createEnhancedRequirementLabel("✓ At least " + MIN_PASSWORD_LENGTH + " characters", false);
        digitReq  = createEnhancedRequirementLabel("✓ At least one digit (0-9)", false);
        lowerReq  = createEnhancedRequirementLabel("✓ At least one lowercase letter", false);
        upperReq  = createEnhancedRequirementLabel("✓ At least one uppercase letter", false);
        specialReq= createEnhancedRequirementLabel("✓ At least one special character (!@#$% etc.)", false);
        requirementsPanel.add(lengthReq);
        requirementsPanel.add(digitReq);
        requirementsPanel.add(lowerReq);
        requirementsPanel.add(upperReq);
        requirementsPanel.add(specialReq);
        gbc.gridy = 4;
        form.add(requirementsPanel, gbc);

        /* terms */
        agreeTerms = new JCheckBox("I agree to the terms and conditions");
        agreeTerms.setOpaque(false);
        agreeTerms.setForeground(Color.WHITE);
        agreeTerms.setFont(SMALL_FONT);
        gbc.gridy = 5;
        form.add(agreeTerms, gbc);

        /* real-time listener */
        regPasswordField.getDocument().addDocumentListener(new DocumentListener() {
            public void changedUpdate(DocumentEvent e) { updatePasswordStrength(); }
            public void removeUpdate(DocumentEvent e) { updatePasswordStrength(); }
            public void insertUpdate(DocumentEvent e) { updatePasswordStrength(); }
        });

        /* buttons */
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 0));
        btnPanel.setOpaque(false);
        registerButton = createPremiumGoldButton("🚀 Instant Registration", 220, 50);
        switchToLoginBtn = createTextButton("← Back to Login");
        btnPanel.add(registerButton);
        btnPanel.add(switchToLoginBtn);
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 2;
        gbc.insets = new Insets(20, 10, 10, 10);
        form.add(btnPanel, gbc);

        panel.add(form, BorderLayout.CENTER);

        /* listeners */
        registerButton.addActionListener(e -> attemptFastRegistration());
        regConfirmField.addActionListener(e -> attemptFastRegistration());
        switchToLoginBtn.addActionListener(e -> resetRegistrationForm());

        return panel;
    }

    /* ======================================================= */
    /*                     SMALL HELPERS                       */
    /* ======================================================= */
    private JLabel createLabel(String text) {
        JLabel l = new JLabel(text);
        l.setForeground(Color.WHITE);
        l.setFont(LABEL_FONT);
        return l;
    }

    private JTextField createEnhancedTextField(String placeholder) {
        JTextField field = new JTextField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getText().isEmpty() && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(150, 150, 150, 150));
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 12, 22);
                }
            }
        };
        field.setFont(LABEL_FONT);
        field.setForeground(Color.WHITE);
        field.setBackground(new Color(40, 50, 80));
        field.setCaretColor(PREMIUM_GOLD);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 120), 2),
                new EmptyBorder(12, 15, 12, 15)));
        field.setSelectionColor(new Color(255, 215, 0, 100));
        /* micro-glow on focus */
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GLOW_GOLD, 2),
                        new EmptyBorder(12, 15, 12, 15)));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(80, 90, 120), 2),
                        new EmptyBorder(12, 15, 12, 15)));
            }
        });
        return field;
    }

    private JPasswordField createEnhancedPasswordField(String placeholder) {
        JPasswordField field = new JPasswordField(20) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (getPassword().length == 0 && !hasFocus()) {
                    Graphics2D g2 = (Graphics2D) g;
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(150, 150, 150, 150));
                    g2.setFont(getFont().deriveFont(Font.ITALIC));
                    g2.drawString(placeholder, 12, 22);
                }
            }
        };
        field.setEchoChar('•');
        field.setFont(LABEL_FONT);
        field.setForeground(Color.WHITE);
        field.setBackground(new Color(40, 50, 80));
        field.setCaretColor(PREMIUM_GOLD);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(80, 90, 120), 2),
                new EmptyBorder(12, 15, 12, 15)));
        field.setSelectionColor(new Color(255, 215, 0, 100));
        field.addFocusListener(new FocusAdapter() {
            @Override public void focusGained(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(GLOW_GOLD, 2),
                        new EmptyBorder(12, 15, 12, 15)));
            }
            @Override public void focusLost(FocusEvent e) {
                field.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(80, 90, 120), 2),
                        new EmptyBorder(12, 15, 12, 15)));
            }
        });
        return field;
    }

    private JToggleButton createShowPasswordToggle(JPasswordField field) {
        JToggleButton toggle = new JToggleButton("👁");
        toggle.setFont(SMALL_FONT);
        toggle.setForeground(LIGHT_GOLD);
        toggle.setContentAreaFilled(false);
        toggle.setBorderPainted(false);
        toggle.setFocusPainted(false);
        toggle.setCursor(new Cursor(Cursor.HAND_CURSOR));
        toggle.addActionListener(e -> {
            if (toggle.isSelected()) {
                field.setEchoChar((char) 0);
                toggle.setText("🙈");
            } else {
                field.setEchoChar('•');
                toggle.setText("👁");
            }
        });
        return toggle;
    }

    private JButton createPremiumGoldButton(String text, int w, int h) {
        JButton btn = new JButton(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(0, 0, new Color(255, 220, 0), 0, getHeight(), new Color(255, 180, 0));
                g2.setPaint(gp);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(255, 255, 255, 80));
                g2.fillRoundRect(0, 0, getWidth(), getHeight() / 2, 20, 20);
                g2.setColor(new Color(212, 175, 55));
                g2.setStroke(new BasicStroke(2));
                g2.drawRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 20, 20);
                super.paintComponent(g);
            }
        };
        btn.setFont(BUTTON_FONT);
        btn.setForeground(DARK_BLUE_GRADIENT_START);
        btn.setFocusPainted(false);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(w, h));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setForeground(Color.WHITE);
                btn.setToolTipText("Click for instant access!");
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setForeground(DARK_BLUE_GRADIENT_START);
            }
        });
        return btn;
    }

    private JButton createQuickAccessButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(SMALL_FONT);
        btn.setForeground(LIGHT_GOLD);
        btn.setBackground(new Color(255, 215, 0, 40));
        btn.setContentAreaFilled(false);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(255, 215, 0, 100), 1),
                new EmptyBorder(5, 10, 5, 10)));
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setBackground(new Color(255, 215, 0, 80));
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setBackground(new Color(255, 215, 0, 40));
            }
        });
        return btn;
    }

    private JButton createTextButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(TEXT_BUTTON_FONT);
        btn.setForeground(LIGHT_GOLD);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                btn.setForeground(PREMIUM_GOLD);
                btn.setFont(btn.getFont().deriveFont(Font.BOLD));
            }
            @Override public void mouseExited(MouseEvent e) {
                btn.setForeground(LIGHT_GOLD);
                btn.setFont(btn.getFont().deriveFont(Font.PLAIN));
            }
        });
        return btn;
    }

    /* ======================================================= */
    /*                AUTHENTICATION  BUSINESS                 */
    /* ======================================================= */
    private void attemptFastLogin() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        if (username.isEmpty() || password.isEmpty()) {
            showEnhancedMessage("Validation Error", "Username and password cannot be empty.", WARNING);
            return;
        }
        showEnhancedLoading("🔐 Instant Authentication...");
        new Thread(() -> {
            try {
                Thread.sleep(quickAccessEnabled ? 300 : 600);
                AuthService.User user = AuthService.authenticate(username, password);
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    if (user != null) {
                        if (rememberMe.isSelected()) saveRememberedUsername(username);
                        if (quickAccessEnabled) handleImmediateAccess(user);
                        else showEnhancedGoldCoinRain(user);
                    } else {
                        showEnhancedMessage("Login Failed", "Invalid username or password.", DANGER);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    showEnhancedMessage("Login Error", "Authentication failed: " + ex.getMessage(), DANGER);
                });
            }
        }).start();
    }

    private void attemptFastRegistration() {
        String username = regUsernameField.getText().trim();
        String password = new String(regPasswordField.getPassword());
        String confirm  = new String(regConfirmField.getPassword());
        if (username.isEmpty() || password.isEmpty() || confirm.isEmpty()) {
            showEnhancedMessage("Validation Error", "All fields are required.", WARNING);
            return;
        }
        if (username.length() < 3) {
            showEnhancedMessage("Validation Error", "Username must be at least 3 characters long.", WARNING);
            return;
        }
        if (!password.equals(confirm)) {
            showEnhancedMessage("Validation Error", "Passwords do not match.", WARNING);
            return;
        }
        if (!agreeTerms.isSelected()) {
            showEnhancedMessage("Validation Error", "You must agree to the terms and conditions.", WARNING);
            return;
        }
        String pwdError = validatePasswordStrength(password);
        if (pwdError != null) {
            showEnhancedMessage("Weak Password", pwdError, WARNING);
            return;
        }
        showEnhancedLoading("🛡️ Creating Premium Account...");
        new Thread(() -> {
            try {
                Thread.sleep(quickAccessEnabled ? 400 : 800);
                boolean success = registerUser(username, password);
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    if (success) {
                        if (quickAccessEnabled) handleImmediateRegistrationSuccess(username);
                        else {
                            showEnhancedGoldCoinRain(null);
                            new Timer(1500, e -> {
                                showEnhancedMessage("Registration Successful",
                                        "✅ Premium account created!\n\n" +
                                                "💰 Your account is ready for instant access!", SUCCESS);
                                resetRegistrationForm();
                                usernameField.setText(username);
                            }).start();
                        }
                    } else {
                        showEnhancedMessage("Registration Failed", "Username already exists.", DANGER);
                    }
                });
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> {
                    hideLoading();
                    showEnhancedMessage("Registration Error", "Account creation failed: " + ex.getMessage(), DANGER);
                });
            }
        }).start();
    }

    private void handleImmediateAccess(AuthService.User user) {
        dispose();
        switch (user.getRole().toLowerCase()) {
            case "admin":
            case "staff":
                new AdminDashboardFrame(user).setVisible(true);
                break;
            case "customer":
                new CustomerDashboardFrame(user).setVisible(true);
                break;
            default:
                showEnhancedMessage("Access Error", "Unknown user role: " + user.getRole(), DANGER);
                break;
        }
    }

    private void handleImmediateRegistrationSuccess(String username) {
        showEnhancedMessage("Registration Complete",
                "✅ Premium account created successfully!\n\n" +
                        "🚀 You can now log in with your new credentials.", SUCCESS);
        resetRegistrationForm();
        usernameField.setText(username);
        cardLayout.show(mainPanel, "login");
    }

    private void showQuickDemo() {
        usernameField.setText("demo_user");
        passwordField.setText("Demo@123");
        showEnhancedMessage("Quick Demo",
                "Demo credentials loaded!\n\n" +
                        "👤 Username: demo_user\n" +
                        "🔒 Password: Demo@123\n\n" +
                        "Click Login to continue with demo access.", SUCCESS);
    }

    private void setQuickAccess(boolean enabled) {
        quickAccessEnabled = enabled;
        showEnhancedMessage("Quick Access " + (enabled ? "Enabled" : "Disabled"),
                "🚀 " + (enabled ? "Quick access mode activated!" : "Full animations restored!") + "\n\n" +
                        (enabled ? "Animations will be skipped for faster access." : "Enjoy the full visual experience."),
                enabled ? SUCCESS : WARNING);
    }

    private String validatePasswordStrength(String password) {
        if (password.length() < MIN_PASSWORD_LENGTH)
            return "Password must be at least " + MIN_PASSWORD_LENGTH + " characters long.";
        if (!Pattern.compile("(?=.*[0-9])").matcher(password).find())
            return "Password must contain at least one digit (0-9).";
        if (!Pattern.compile("(?=.*[a-z])").matcher(password).find())
            return "Password must contain at least one lowercase letter.";
        if (!Pattern.compile("(?=.*[A-Z])").matcher(password).find())
            return "Password must contain at least one uppercase letter.";
        if (!Pattern.compile("(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])").matcher(password).find())
            return "Password must contain at least one special character (!@#$% etc.).";
        return null;
    }

    private boolean registerUser(String username, String password) {
        String sql = "INSERT INTO users (username, password, role) VALUES (?, ?, 'customer')";
        try (Connection conn = com.goldinventory.database.DBConnection.getConnection()) {
            try (PreparedStatement check = conn.prepareStatement("SELECT user_id FROM users WHERE username = ?")) {
                check.setString(1, username);
                try (ResultSet rs = check.executeQuery()) {
                    if (rs.next()) return false;
                }
            }
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, username);
                stmt.setString(2, AuthService.hashPassword(password));
                stmt.executeUpdate();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private void saveRememberedUsername(String username) {
        try (FileOutputStream fos = new FileOutputStream("remember.properties")) {
            Properties prop = new Properties();
            prop.setProperty("username", username);
            prop.store(fos, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadRememberedUsername() {
        try (FileInputStream fis = new FileInputStream("remember.properties")) {
            Properties prop = new Properties();
            prop.load(fis);
            usernameField.setText(prop.getProperty("username"));
            rememberMe.setSelected(true);
        } catch (Exception e) {
            // No remembered username
        }
    }

    /* ======================================================= */
    /*                     NOTIFICATIONS                       */
    /* ======================================================= */
    private void showEnhancedMessage(String title, String message, Color color) {
        JOptionPane.showMessageDialog(this,
                "<html><div style='text-align: center; color: " + toHex(color) + "; font-family: Segoe UI;'>"
                        + "<h3>" + title + "</h3>" + message.replace("\n", "<br>") + "</div></html>",
                title, JOptionPane.INFORMATION_MESSAGE);
    }

    private String toHex(Color color) {
        return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    private void showEnhancedLoading(String message) {
        if (loadingDialog != null) hideLoading();
        loadingDialog = new JDialog(this, "", Dialog.ModalityType.MODELESS);
        loadingDialog.setUndecorated(true);
        loadingDialog.setAlwaysOnTop(true);
        loadingDialog.setBackground(new Color(0, 0, 0, 0));
        loadingDialog.setSize(350, 180);
        loadingDialog.setLocationRelativeTo(this);

        JPanel panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(10, 25, 60, 240));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 35, 35);
                g2.setColor(new Color(255, 215, 0, 100));
                g2.setStroke(new BasicStroke(3));
                g2.drawRoundRect(2, 2, getWidth() - 4, getHeight() - 4, 35, 35);
            }
        };
        panel.setLayout(new GridBagLayout());
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.insets = new Insets(5, 5, 5, 5);

        JLabel spinner = new JLabel(new ImageIcon(createEnhancedGoldSpinner()));
        panel.add(spinner, gbc);
        gbc.gridy = 1;
        JLabel label = new JLabel(message, JLabel.CENTER);
        label.setForeground(LIGHT_GOLD);
        label.setFont(new Font("Segoe UI", Font.BOLD, 16));
        panel.add(label, gbc);
        gbc.gridy = 2;
        JLabel dots = new JLabel("...", JLabel.CENTER);
        dots.setForeground(PREMIUM_GOLD);
        dots.setFont(new Font("Segoe UI", Font.BOLD, 20));
        panel.add(dots, gbc);

        loadingDotsTimer = new Timer(400, e -> {
            String t = dots.getText();
            dots.setText(t.length() >= 3 ? "." : t + ".");
        });
        loadingDotsTimer.start();

        loadingDialog.add(panel);
        loadingDialog.setVisible(true);
    }

    private void hideLoading() {
        if (loadingDialog != null) {
            if (loadingDotsTimer != null) {
                loadingDotsTimer.stop();
                loadingDotsTimer = null;
            }
            loadingDialog.dispose();
            loadingDialog = null;
        }
    }

    private Image createEnhancedGoldSpinner() {
        BufferedImage img = new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        GradientPaint gp = new GradientPaint(0, 0, PREMIUM_GOLD, 50, 50, LIGHT_GOLD);
        g2.setPaint(gp);
        g2.setStroke(new BasicStroke(4, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.drawArc(5, 5, 40, 40, 0, 270);
        g2.dispose();
        return img;
    }

    /* ======================================================= */
    /*                     COIN-RAIN  (60 fps)                 */
    /* ======================================================= */
    private void showEnhancedGoldCoinRain(AuthService.User user) {
        JWindow win = new JWindow(this);
        win.setAlwaysOnTop(true);
        win.setSize(getSize());
        win.setLocation(getLocationOnScreen());
        win.setBackground(new Color(0, 0, 0, 0));
        EnhancedGoldCoinRainPanel p = new EnhancedGoldCoinRainPanel();
        win.add(p);
        win.setVisible(true);
        Timer timer = new Timer(16, e -> {          // ~60 fps
            p.animate();
            if (p.isAnimationComplete()) {
                ((Timer) e.getSource()).stop();
                win.dispose();
                if (user != null) handleImmediateAccess(user);
            }
        });
        timer.start();
    }

    class EnhancedGoldCoinRainPanel extends JPanel {
        private static final int NUM_COINS = 40;
        private final Point[] coins = new Point[NUM_COINS];
        private final int[] vy = new int[NUM_COINS];
        private final int[] vx = new int[NUM_COINS];
        private final int[] rot = new int[NUM_COINS];
        private final int[] sizes = new int[NUM_COINS];
        private int frames = 0;
        EnhancedGoldCoinRainPanel() {
            setOpaque(false);
            int w = getWidth(), h = getHeight();
            for (int i = 0; i < NUM_COINS; i++) {
                coins[i] = new Point((int) (Math.random() * w), (int) (Math.random() * -h));
                vy[i] = 3 + (int) (Math.random() * 7);
                vx[i] = -3 + (int) (Math.random() * 6);
                rot[i] = (int) (Math.random() * 360);
                sizes[i] = 15 + (int) (Math.random() * 10);
            }
        }
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            for (int i = 0; i < NUM_COINS; i++) {
                g2.setColor(new Color(0, 0, 0, 50));
                g2.fill(new Ellipse2D.Double(coins[i].x + 2, coins[i].y + 2, sizes[i], sizes[i]));
                GradientPaint gp = new GradientPaint(
                        coins[i].x, coins[i].y, PREMIUM_GOLD,
                        coins[i].x + sizes[i], coins[i].y + sizes[i], LIGHT_GOLD);
                g2.setPaint(gp);
                g2.rotate(Math.toRadians(rot[i]), coins[i].x + sizes[i] / 2.0, coins[i].y + sizes[i] / 2.0);
                g2.fill(new Ellipse2D.Double(coins[i].x, coins[i].y, sizes[i], sizes[i]));
                g2.setColor(new Color(255, 240, 180));
                g2.fill(new Ellipse2D.Double(coins[i].x + sizes[i] / 4.0, coins[i].y + sizes[i] / 4.0,
                        sizes[i] / 2.0, sizes[i] / 2.0));
                g2.rotate(-Math.toRadians(rot[i]), coins[i].x + sizes[i] / 2.0, coins[i].y + sizes[i] / 2.0);
            }
        }
        void animate() {
            int w = getWidth(), h = getHeight();
            for (int i = 0; i < NUM_COINS; i++) {
                coins[i].y += vy[i];
                coins[i].x += vx[i];
                rot[i] += 10;
                if (coins[i].y > h + 20 || coins[i].x < -20 || coins[i].x > w + 20) {
                    coins[i].setLocation((int) (Math.random() * w), (int) (Math.random() * -50));
                    vy[i] = 3 + (int) (Math.random() * 7);
                    vx[i] = -3 + (int) (Math.random() * 6);
                }
            }
            frames++;
            repaint();
        }
        boolean isAnimationComplete() { return frames > 90; }
    }

    /* ======================================================= */
    /*                     STRENGTH / METER                    */
    /* ======================================================= */
    private void updatePasswordStrength() {
        String pwd = new String(regPasswordField.getPassword());
        boolean len = pwd.length() >= MIN_PASSWORD_LENGTH;
        boolean dig = Pattern.compile("(?=.*[0-9])").matcher(pwd).find();
        boolean low = Pattern.compile("(?=.*[a-z])").matcher(pwd).find();
        boolean upp = Pattern.compile("(?=.*[A-Z])").matcher(pwd).find();
        boolean spe = Pattern.compile("(?=.*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?])").matcher(pwd).find();
        updateEnhancedRequirementLabel(lengthReq, len);
        updateEnhancedRequirementLabel(digitReq, dig);
        updateEnhancedRequirementLabel(lowerReq, low);
        updateEnhancedRequirementLabel(upperReq, upp);
        updateEnhancedRequirementLabel(specialReq, spe);
        int score = 0;
        if (len) score += 20;
        if (dig) score += 20;
        if (low) score += 20;
        if (upp) score += 20;
        if (spe) score += 20;
        if (pwd.length() >= 12) score += 10;
        if (pwd.length() >= 16) score += 10;
        String txt;
        Color col;
        if (score < 40) { txt = "Very Weak"; col = DANGER; }
        else if (score < 60) { txt = "Weak"; col = WARNING; }
        else if (score < 80) { txt = "Good"; col = WARNING; }
        else if (score < 90) { txt = "Strong"; col = SUCCESS; }
        else { txt = "Very Strong"; col = SUCCESS; }
        strengthLabel.setText(txt);
        strengthLabel.setForeground(col);
        strengthProgressBar.setValue(Math.min(score, 100));
        strengthProgressBar.setForeground(col);
    }

    private void updateEnhancedRequirementLabel(JLabel label, boolean met) {
        label.setForeground(met ? SUCCESS : DANGER);
        label.setIcon(met ? createCheckIcon(SUCCESS) : createXIcon(DANGER));
    }

    private void resetRegistrationForm() {
        regUsernameField.setText("");
        regPasswordField.setText("");
        regConfirmField.setText("");
        agreeTerms.setSelected(false);
        strengthLabel.setText("Very Weak");
        strengthLabel.setForeground(DANGER);
        strengthProgressBar.setValue(0);
        strengthProgressBar.setForeground(DANGER);
        updateEnhancedRequirementLabel(lengthReq, false);
        updateEnhancedRequirementLabel(digitReq, false);
        updateEnhancedRequirementLabel(lowerReq, false);
        updateEnhancedRequirementLabel(upperReq, false);
        updateEnhancedRequirementLabel(specialReq, false);
        cardLayout.show(mainPanel, "login");
    }

    private JLabel createEnhancedRequirementLabel(String text, boolean met) {
        JLabel l = new JLabel(text);
        l.setFont(SMALL_FONT);
        l.setForeground(met ? SUCCESS : NEUTRAL);
        l.setIcon(met ? createCheckIcon(SUCCESS) : createXIcon(DANGER));
        return l;
    }

    private Icon createCheckIcon(Color color) {
        BufferedImage img = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(2, 6, 5, 9);
        g2.drawLine(5, 9, 10, 2);
        g2.dispose();
        return new ImageIcon(img);
    }

    private Icon createXIcon(Color color) {
        BufferedImage img = new BufferedImage(12, 12, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(color);
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(2, 2, 10, 10);
        g2.drawLine(10, 2, 2, 10);
        g2.dispose();
        return new ImageIcon(img);
    }

    /* ======================================================= */
    /*                     ANIMATION  THREAD                   */
    /* ======================================================= */
    private void startBackgroundAnimation() {
        scheduler.scheduleAtFixedRate(() -> {
            animationOffset += 0.001f;
            if (animationOffset > 1f) animationOffset = 0f;
            if (glowDirection) {
                glowPulse += 0.05f;
                if (glowPulse > Math.PI * 2) glowDirection = false;
            } else {
                glowPulse -= 0.05f;
                if (glowPulse < 0) glowDirection = true;
            }
            SwingUtilities.invokeLater(() -> animatedBackgroundPanel.repaint());
        }, 0, 25, TimeUnit.MILLISECONDS);
    }

    private void preloadResources() {
        preloadTimer = new Timer(1000, e -> System.gc());
        preloadTimer.setRepeats(false);
        preloadTimer.start();
    }

    /* ======================================================= */
    /*                        CLEAN-UP                         */
    /* ======================================================= */
    @Override
    public void dispose() {
        super.dispose();
        if (scheduler != null) scheduler.shutdownNow();
        if (preloadTimer != null) preloadTimer.stop();
    }
}