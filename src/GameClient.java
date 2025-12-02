import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.*;

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

    JButton atkBtn, defBtn, healBtn;

    private int myCharType = -1;
    private int myMaxHp = 100;
    private int oppCharType = -1;


    public GameClient() {
        setTitle("Battle Arena Online [Team 28]");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

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

    // 메인 화면
 // 먼저 클래스 위쪽에 BackgroundPanel 정의 추가
    class BackgroundPanel extends JPanel {
        private Image backgroundImage;

        public BackgroundPanel() {
            backgroundImage = new ImageIcon(getClass().getResource("/image/background.png")).getImage();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
        }
    }



    private JPanel createMainScreen() {
        // 기존 JPanel → BackgroundPanel 로 변경
        JPanel panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());

        // Title 이미지
        ImageIcon battleIcon = new ImageIcon(getClass().getResource("/image/battle.png"));
        Image scaled = battleIcon.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
        JLabel titleLabel = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
        titleLabel.setOpaque(false);  // 배경 투명

        // Center 영역
        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);   // 배경 투명

        // Start 버튼 이미지
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


    // 매칭 대기 화면
    private JPanel createWaitScreen() {
        JPanel panel = new BackgroundPanel();  // ✅ 배경 이미지 사용
        panel.setLayout(new BorderLayout());

        JLabel waitLabel = new JLabel("상대방을 기다리는 중입니다...", SwingConstants.CENTER);
        waitLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        waitLabel.setOpaque(false);  // ✅ 배경 투명

        panel.add(waitLabel, BorderLayout.CENTER);
        return panel;
    }

    // 캐릭터 선택 화면
    private JPanel createSelectScreen() {
        JPanel panel = new BackgroundPanel();  // ✅ 배경 이미지 사용
        panel.setLayout(new BorderLayout());

        ImageIcon selectIcon = new ImageIcon(getClass().getResource("/image/selectcharacter.png"));
        Image scaled = selectIcon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
        JLabel guideLabel = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
        guideLabel.setOpaque(false);
        JPanel charPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        charPanel.setOpaque(false);  // ✅ 배경 투명

        String[] chars = {"전사 (체력형)", "마법사 (공격형)", "궁수 (속도형)"};

        for (int i = 0; i < 3; i++) {
            final int charIdx = i;

            JButton btn;

            if (i == 0) {
                // 전사 버튼에 이미지 추가
                ImageIcon icon = new ImageIcon(getClass().getResource("/image/warrior1.png"));
                Image scaledChar = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledChar);

                btn = new JButton("<html><center>전사 (체력형)<br></center></html>", scaledIcon);
                btn.setHorizontalTextPosition(SwingConstants.CENTER);
                btn.setVerticalTextPosition(SwingConstants.BOTTOM);

            } else if (i == 1) {
                // 마법사 버튼에 이미지 추가
                ImageIcon icon = new ImageIcon(getClass().getResource("/image/magician1.png"));
                Image scaledChar = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledChar);

                btn = new JButton("<html><center>마법사 (공격형)<br></center></html>", scaledIcon);
                btn.setHorizontalTextPosition(SwingConstants.CENTER);
                btn.setVerticalTextPosition(SwingConstants.BOTTOM);

            } else {
                // 궁수(엘프) 버튼에 이미지 추가
                ImageIcon icon = new ImageIcon(getClass().getResource("/image/elp1.png"));
                Image scaledChar = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
                ImageIcon scaledIcon = new ImageIcon(scaledChar);

                btn = new JButton("<html><center>궁수 (속도형)<br></center></html>", scaledIcon);
                btn.setHorizontalTextPosition(SwingConstants.CENTER);
                btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            }


            btn.setFocusPainted(false);
            btn.setOpaque(false);              
            btn.setContentAreaFilled(false);   
            btn.setBorderPainted(true);        

            btn.addActionListener(e -> {
                myCharType = charIdx;
                sendMessage("SELECT:" + charIdx);
                guideLabel.setText("선택 완료! 상대방 기다리는 중...");
                for (Component c : charPanel.getComponents()) c.setEnabled(false);
            });

            charPanel.add(btn);
        }


        panel.add(guideLabel, BorderLayout.NORTH);
        panel.add(charPanel, BorderLayout.CENTER);
        return panel;
    }

    private ImageIcon getCharacterIcon(int charType) {
        String path = switch (charType) {
            case 0 -> "/image/warrior1.png";
            case 1 -> "/image/magician1.png";
            case 2 -> "/image/elp1.png";
            default -> "/image/unknown.png"; // 예외 대비
        };

        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image img = icon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }
    
    private ImageIcon getAttackImage(int type, int state) {
        // state: 1 = 기본, 2 = 공격, 3 = 피격
        String prefix = switch (type) {
            case 0 -> "warrior";
            case 1 -> "magician";
            case 2 -> "elp";
            default -> "unknown";
        };
        String path = "/image/" + prefix + state + ".png";
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image img = icon.getImage().getScaledInstance(300, 300, Image.SCALE_SMOOTH);
        return new ImageIcon(img);
    }


    // 전투 화면
    private JPanel createBattleScreen() {
    	JPanel panel = new JPanel(new BorderLayout()) {
    	    Image backgroundImage = new ImageIcon(getClass().getResource("/image/battle_background.png")).getImage();

    	    @Override
    	    protected void paintComponent(Graphics g) {
    	        super.paintComponent(g);
    	        g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
    	    }
    	};

        panel.setLayout(new BorderLayout());

        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.setOpaque(false);  // ✅ 배경이 보이도록

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
        defBtn = new JButton("방어 (Defend)");
        healBtn = new JButton("회복 (Heal)");

        atkBtn.addActionListener(e -> {
            sendMessage("ACTION:ATTACK");

            // 👉 애니메이션: 공격 상태로 변경
            myCharImg.setIcon(getAttackImage(myCharType, 2));   // 때리는 이미지
            oppCharImg.setIcon(getAttackImage(oppCharType, 3)); // 맞는 이미지

            // 👉 1초 후 다시 원래 이미지로 복구
            new javax.swing.Timer(1000, evt -> {
                myCharImg.setIcon(getAttackImage(myCharType, 1));   // 기본 이미지
                oppCharImg.setIcon(getAttackImage(oppCharType, 1)); // 상대방 기본 이미지 복구
                ((javax.swing.Timer) evt.getSource()).stop();
            }).start();
        });


        defBtn.addActionListener(e -> sendMessage("ACTION:DEFEND"));
        healBtn.addActionListener(e -> sendMessage("ACTION:HEAL"));

        actionPanel.add(atkBtn);
        actionPanel.add(defBtn);
        actionPanel.add(healBtn);

        bottomPanel.add(scroll, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }


    // 결과 화면
    private JPanel createResultScreen(String resultMsg) {
        // ✅ 익명 클래스 방식으로 배경 이미지 적용
        JPanel panel = new JPanel(new BorderLayout()) {
            Image backgroundImage = new ImageIcon(getClass().getResource("/image/background.png")).getImage();
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        // ✅ 결과 이미지 라벨 (초기엔 빈 이미지)
        JLabel resLabel = new JLabel();
        resLabel.setHorizontalAlignment(SwingConstants.CENTER);
        resLabel.setName("RESULT_LABEL");  // 나중에 참조할 이름

        JButton homeBtn = new JButton("메인 화면으로");
        homeBtn.addActionListener(e -> cardLayout.show(mainPanelContainer, "MAIN"));

        panel.add(resLabel, BorderLayout.CENTER);
        panel.add(homeBtn, BorderLayout.SOUTH);

        return panel;
    }


    private int getMaxHpForChar(int charType) {
        switch (charType) {
            case 0: return 200;
            case 1: return 150;
            case 2: return 170;
            default: return 100;
        }
    }

    // 서버 연결
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
    
    private void showCustomPopup(String message) {
        JDialog dialog = new JDialog(this, "알림", true);
        dialog.setUndecorated(true);  // 타이틀 바 제거
        dialog.setSize(400, 150);
        dialog.setLocationRelativeTo(this);

        // 배경 Panel
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 3));
        panel.setBackground(new Color(0, 0, 0, 220)); // 반투명 느낌

        // 메시지 라벨
        JLabel msgLabel = new JLabel(message, SwingConstants.CENTER);
        msgLabel.setForeground(Color.WHITE);
        msgLabel.setFont(new Font("SansSerif", Font.BOLD, 16));

        // OK 버튼
        JButton okBtn = new JButton("확인");
        okBtn.setFocusPainted(false);
        okBtn.addActionListener(e -> dialog.dispose());

        // 구성
        panel.add(msgLabel, BorderLayout.CENTER);
        panel.add(okBtn, BorderLayout.SOUTH);

        dialog.setContentPane(panel);
        dialog.setVisible(true);
    }


    private void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    private void processServerMessage(String msg) {
        if (msg.startsWith("USER_COUNT:")) {
            userCountLabel.setText("현재 접속자 수: " + msg.split(":")[1] + "명");

        } else if (msg.equals("MATCH_SUCCESS")) {
            showCustomPopup("매칭이 되었습니다! 곧 게임이 시작됩니다.");
            cardLayout.show(mainPanelContainer, "SELECT");

        } else if (msg.startsWith("GAME_START:")) {
            String[] parts = msg.split(":");

            oppCharType = Integer.parseInt(parts[1]);  // 전역 변수에 저장
            int oppMaxHp = Integer.parseInt(parts[2]);

            // 선택된 캐릭터 이미지 적용
            myCharImg.setIcon(getCharacterIcon(myCharType));
            myCharImg.setText("");

            oppCharImg.setIcon(getCharacterIcon(oppCharType));
            oppCharImg.setText("");

            // HP 바 설정
            oppHpBar.setMaximum(oppMaxHp);
            oppHpBar.setValue(oppMaxHp);
            oppHpBar.setString(oppMaxHp + " / " + oppMaxHp);

            myMaxHp = getMaxHpForChar(myCharType);
            myHpBar.setMaximum(myMaxHp);
            myHpBar.setValue(myMaxHp);
            myHpBar.setString(myMaxHp + " / " + myMaxHp);

            battleLog.setText("전투 시작!\n");
            cardLayout.show(mainPanelContainer, "BATTLE");
        }else if (msg.equals("YOUR_TURN")) {
            battleLog.append(">> 당신의 턴입니다. 행동을 선택하세요.\n");

            atkBtn.setEnabled(true);
            defBtn.setEnabled(true);
            healBtn.setEnabled(true);

        } else if (msg.equals("OPP_TURN")) {
            battleLog.append(">> 상대방의 턴입니다...\n");

            atkBtn.setEnabled(false);
            defBtn.setEnabled(false);
            healBtn.setEnabled(false);


        } else if (msg.startsWith("UPDATE:")) {
            String[] parts = msg.split(":", 4);
            int myHp  = Integer.parseInt(parts[1]);
            int oppHp = Integer.parseInt(parts[2]);
            String log = parts[3];

            myHpBar.setValue(myHp);
            myHpBar.setString(myHp + " / " + myMaxHp);

            oppHpBar.setValue(oppHp);
            oppHpBar.setString(oppHp + " / " + oppHpBar.getMaximum());

            battleLog.append(log + "\n");
            battleLog.setCaretPosition(battleLog.getDocument().getLength());

        } else if (msg.startsWith("RESULT:")) {
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
