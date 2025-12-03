import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;
import java.util.Arrays;

public class GameClient extends JFrame {
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;
    private static final String SERVER_IP = "127.0.0.1";
    private static final int PORT = 5000;

    private CardLayout cardLayout;
    private JPanel mainPanelContainer;
    private JLabel userCountLabel;
    private JTextArea battleLog;
    private JProgressBar myHpBar, oppHpBar;
    private JLabel myCharImg, oppCharImg;
    private JLabel myCharLabel;
    JButton atkBtn, skillBtn1, skillBtn2;

    private int myCharType = -1;
    private int myMaxHp = 100;
    private int oppCharType = -1;
    private final String[] charNames = {"전사 (Warrior)", "마법사 (Magician)", "엘프 (Elf)"};

    private static final String KOREAN_FONT_NAME = "맑은 고딕";
    private MusicPlayer bgmPlayer = new MusicPlayer();

    public GameClient() {
        setTitle("Battle Arena Online [Team 28]");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        bgmPlayer.playLoop("/sound/sound.wav");
        Font defaultFont = new Font(KOREAN_FONT_NAME, Font.PLAIN, 12);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);
        UIManager.put("OptionPane.font", defaultFont);

        cardLayout = new CardLayout();
        mainPanelContainer = new JPanel(cardLayout);

        mainPanelContainer.add(createMainScreen(), "MAIN");
        mainPanelContainer.add(createWaitScreen(), "WAIT");
        mainPanelContainer.add(createSelectScreen(), "SELECT");
        mainPanelContainer.add(createBattleScreen(), "BATTLE");
        mainPanelContainer.add(createResultScreen("결과 대기중..."), "RESULT");

        add(mainPanelContainer);

        connectToServer();
        setVisible(true);
    }

    class BackgroundPanel extends JPanel {
        private Image backgroundImage;
        public BackgroundPanel() {
            backgroundImage = new ImageIcon(getClass().getResource("/image/background.png")).getImage();
        }
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }

    private JPanel createMainScreen() {
        JPanel panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());

        ImageIcon battleIcon = new ImageIcon(getClass().getResource("/image/battle.png"));
        Image scaled = battleIcon.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
        JLabel titleLabel = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
        titleLabel.setOpaque(false);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);

        ImageIcon startIcon = new ImageIcon(getClass().getResource("/image/start.png"));
        Image original = startIcon.getImage();
        int newWidth = 150;
        int newHeight = original.getHeight(null) * newWidth / original.getWidth(null);
        Image scaledImage = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        JButton startBtn = new JButton(new ImageIcon(scaledImage));

        startBtn.setBorderPainted(false);
        startBtn.setContentAreaFilled(false);
        startBtn.setFocusPainted(false);
        startBtn.setOpaque(false);

        userCountLabel = new JLabel("현재 접속자 수: 0명");
        userCountLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        userCountLabel.setOpaque(false);

        startBtn.addActionListener(e -> {
            sendMessage("REQUEST_MATCH");
            cardLayout.show(mainPanelContainer, "WAIT");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 0, 10, 0);
        centerPanel.add(startBtn, gbc);
        gbc.gridy = 1;
        centerPanel.add(userCountLabel, gbc);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    private JPanel createWaitScreen() {
        JPanel panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());

        JLabel waitLabel = new JLabel("상대방을 기다리는 중입니다...", SwingConstants.CENTER);
        waitLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        waitLabel.setOpaque(false);

        panel.add(waitLabel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createSelectScreen() {
        JPanel panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());

        JLabel guideLabel = new JLabel("캐릭터를 선택하세요!", SwingConstants.CENTER);
        guideLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        guideLabel.setOpaque(false);

        JPanel charPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        charPanel.setOpaque(false);

        String[] chars = {"전사 (체력형)", "마법사 (공격형)", "궁수 (속도형)"};
        String[] imagePaths = {"/image/warrior1.png", "/image/magician1.png", "/image/elp1.png"};

        for (int i = 0; i < 3; i++) {
            int idx = i;
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePaths[i]));
            Image scaledChar = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            JButton btn = new JButton("<html><center>" + chars[i] + "</center></html>", new ImageIcon(scaledChar));

            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(true);

            btn.addActionListener(e -> {
                myCharType = idx;
                sendMessage("SELECT:" + idx);
                guideLabel.setText("선택 완료! 상대방 기다리는 중...");
                for (Component c : charPanel.getComponents()) c.setEnabled(false);
            });
            charPanel.add(btn);
        }

        panel.add(guideLabel, BorderLayout.NORTH);
        panel.add(charPanel, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createBattleScreen() {
        JPanel panel = new JPanel(new BorderLayout()) {
            Image backgroundImage = new ImageIcon(getClass().getResource("/image/battle_background.png")).getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.setOpaque(false);

        JPanel myPanel = new JPanel(new BorderLayout());
        myPanel.setOpaque(false);
        myCharImg = new JLabel("[내 캐릭터]", SwingConstants.CENTER);
        myCharImg.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        myHpBar = new JProgressBar();
        myHpBar.setStringPainted(true);
        myHpBar.setForeground(Color.GREEN);
        myPanel.add(myCharImg, BorderLayout.CENTER);
        myPanel.add(myHpBar, BorderLayout.SOUTH);

        JPanel oppPanel = new JPanel(new BorderLayout());
        oppPanel.setOpaque(false);
        oppCharImg = new JLabel("[상대 캐릭터]", SwingConstants.CENTER);
        oppCharImg.setBorder(BorderFactory.createLineBorder(Color.RED, 2));
        oppHpBar = new JProgressBar();
        oppHpBar.setStringPainted(true);
        oppHpBar.setForeground(Color.RED);
        oppPanel.add(oppCharImg, BorderLayout.CENTER);
        oppPanel.add(oppHpBar, BorderLayout.SOUTH);

        topPanel.add(myPanel);
        topPanel.add(oppPanel);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        battleLog = new JTextArea(5, 40);
        battleLog.setEditable(false);
        JScrollPane scroll = new JScrollPane(battleLog);

        JPanel actionPanel = new JPanel(new FlowLayout());
        actionPanel.setOpaque(false);

        atkBtn = new JButton("공격 (Attack)");
        skillBtn1 = new JButton("스킬 1");
        skillBtn2 = new JButton("스킬 2");


        atkBtn.addActionListener(e -> {
            sendMessage("ACTION:ATTACK");
            myCharImg.setIcon(getAttackImage(myCharType, 2));
            oppCharImg.setIcon(getAttackImage(oppCharType, 3));
            disableActionButtons();
            new javax.swing.Timer(1000, evt -> {
                myCharImg.setIcon(getAttackImage(myCharType, 1));
                oppCharImg.setIcon(getAttackImage(oppCharType, 1));
                ((javax.swing.Timer) evt.getSource()).stop();
            }).start();
        });

        skillBtn1.addActionListener(e -> {
            sendMessage(getSkillCommand(1));
            myCharImg.setIcon(getAttackImage(myCharType, 2));
            oppCharImg.setIcon(getAttackImage(oppCharType, 3));
            disableActionButtons();
            new javax.swing.Timer(1000, evt -> {
                myCharImg.setIcon(getAttackImage(myCharType, 1));
                oppCharImg.setIcon(getAttackImage(oppCharType, 1));
                ((javax.swing.Timer) evt.getSource()).stop();
            }).start();
        });

        skillBtn2.addActionListener(e -> {
            sendMessage(getSkillCommand(2));
            myCharImg.setIcon(getAttackImage(myCharType, 2));
            oppCharImg.setIcon(getAttackImage(oppCharType, 3));
            disableActionButtons();
            new javax.swing.Timer(1000, evt -> {
                myCharImg.setIcon(getAttackImage(myCharType, 1));
                oppCharImg.setIcon(getAttackImage(oppCharType, 1));
                ((javax.swing.Timer) evt.getSource()).stop();
            }).start();
        });

        actionPanel.add(atkBtn);
        actionPanel.add(skillBtn1);
        actionPanel.add(skillBtn2);

        bottomPanel.add(scroll, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    private ImageIcon getAttackImage(int type, int state) {
        String prefix = switch (type) {
            case 0 -> "warrior";
            case 1 -> "magician";
            case 2 -> "elp";
            default -> "default"; // 기본 이미지로 fallback
        };
        String path = "/image/" + prefix + state + ".png";
        URL resource = getClass().getResource(path);
        if (resource == null) {
            System.err.println("이미지 파일 없음: " + path);
            return new ImageIcon();
        }
        ImageIcon icon = new ImageIcon(resource);
        Image img = icon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }

    private void disableActionButtons() {
        atkBtn.setEnabled(false);
        skillBtn1.setEnabled(false);
        skillBtn2.setEnabled(false);
    }

    private void enableActionButtons() {
        atkBtn.setEnabled(true);
        skillBtn1.setEnabled(true);
        skillBtn2.setEnabled(true);
    }

    private JPanel createResultScreen(String resultMsg) {
        JPanel panel = new JPanel(new BorderLayout()) {
            Image backgroundImage = new ImageIcon(getClass().getResource("/image/background.png")).getImage();
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        JLabel resLabel = new JLabel(resultMsg, SwingConstants.CENTER);
        resLabel.setName("RESULT_LABEL");
        JButton homeBtn = new JButton("메인 화면으로");
        homeBtn.addActionListener(e -> cardLayout.show(mainPanelContainer, "MAIN"));
        panel.add(resLabel, BorderLayout.CENTER);
        panel.add(homeBtn, BorderLayout.SOUTH);
        return panel;
    }

    private void connectToServer() {
        new Thread(() -> {
            try {
                socket = new Socket(SERVER_IP, PORT);
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String msg;
                while ((msg = in.readLine()) != null) {
                    String finalMsg = msg;
                    SwingUtilities.invokeLater(() -> processServerMessage(finalMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    private String getSkillCommand(int skillNum) {
        String base = "ACTION:";
        return switch (myCharType) {
            case 0 -> base + (skillNum == 1 ? "WAR_DEFUP" : "WAR_SHIELD");
            case 1 -> base + (skillNum == 1 ? "MAG_METEOR" : "MAG_DEBUFF");
            case 2 -> base + (skillNum == 1 ? "ELF_ARMORBREAK" : "ELF_EVASION");
            default -> base + "INVALID";
        };
    }

    private int getMaxHpForChar(int charType) {
        return switch (charType) {
            case 0 -> 200;
            case 1 -> 150;
            case 2 -> 170;
            default -> 100;
        };
    }

    private void processServerMessage(String msg) {
        if (msg.startsWith("USER_COUNT:")) {
            userCountLabel.setText("현재 접속자 수: " + msg.split(":")[1] + "명");
        } else if (msg.equals("MATCH_SUCCESS")) {
            cardLayout.show(mainPanelContainer, "SELECT");
        } else if (msg.startsWith("GAME_START:")) {
            String[] parts = msg.split(":");
            oppCharType = Integer.parseInt(parts[1]);
            int oppMaxHp = Integer.parseInt(parts[2]);
            myMaxHp = Integer.parseInt(parts[3]);

            myHpBar.setMaximum(myMaxHp);
            myHpBar.setValue(myMaxHp);
            myHpBar.setString(myMaxHp + " / " + myMaxHp);

            oppHpBar.setMaximum(oppMaxHp);
            oppHpBar.setValue(oppMaxHp);
            oppHpBar.setString(oppMaxHp + " / " + oppMaxHp);

            myCharImg.setText(charNames[myCharType] + " (나)");
            oppCharImg.setText(charNames[oppCharType] + " (상대)");

            skillBtn1.setEnabled(true);
            skillBtn2.setEnabled(true);
            myCharImg.setIcon(getAttackImage(myCharType, 1));
            oppCharImg.setIcon(getAttackImage(oppCharType, 1));
            battleLog.setText("전투 시작!\n");
            cardLayout.show(mainPanelContainer, "BATTLE");
        } else if (msg.startsWith("YOUR_TURN")) {
            enableActionButtons();  // ✅ 버튼 모두 활성화
            battleLog.append(">> 당신의 턴입니다. 행동을 선택하세요.\n");


            
        } else if (msg.startsWith("OPP_TURN")) {
            disableActionButtons();  // ✅ 버튼 비활성화
            battleLog.append(">> 상대방의 턴입니다...\n");

        } else if (msg.startsWith("UPDATE:")) {
            String[] parts = msg.split(":", 4);
            int myHp = Integer.parseInt(parts[1]);
            int oppHp = Integer.parseInt(parts[2]);
            String log = parts[3];

            myHpBar.setValue(myHp);
            myHpBar.setString(myHp + " / " + myMaxHp);

            oppHpBar.setValue(oppHp);
            oppHpBar.setString(oppHp + " / " + oppHpBar.getMaximum());

            battleLog.append(log + "\n");
            battleLog.setCaretPosition(battleLog.getDocument().getLength());
        }else if (msg.startsWith("RESULT:")) {
            String result = msg.split(":")[1];

            // ✅ 이미지 파일 경로 결정
            String imagePath = result.equals("WIN") ? "/image/victory.png" : "/image/defeat.png";
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePath));
            Image scaled = icon.getImage().getScaledInstance(400, 200, Image.SCALE_SMOOTH);
            ImageIcon scaledIcon = new ImageIcon(scaled);

            JPanel resPanel = (JPanel) mainPanelContainer.getComponent(4);
            for (Component c : resPanel.getComponents()) {
                if ("RESULT_LABEL".equals(c.getName()) && c instanceof JLabel) {
                    ((JLabel) c).setIcon(scaledIcon);
                    ((JLabel) c).setText("");  // 텍스트 제거
                }
            }

            cardLayout.show(mainPanelContainer, "RESULT");
        }
    }

    public static void main(String[] args) {
        new GameClient();
    }
}