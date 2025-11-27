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
    private JPanel createMainScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel titleLabel = new JLabel("Battle Arena Online", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Serif", Font.BOLD, 40));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        JButton startBtn = new JButton("게임 시작 (매칭 찾기)");
        startBtn.setFont(new Font("SansSerif", Font.BOLD, 20));
        startBtn.setPreferredSize(new Dimension(250, 60));

        userCountLabel = new JLabel("현재 접속자 수: 0명");
        userCountLabel.setFont(new Font("SansSerif", Font.PLAIN, 14));

        startBtn.addActionListener(e -> {
            sendMessage("REQUEST_MATCH");
            cardLayout.show(mainPanelContainer, "WAIT");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10,0,10,0);
        centerPanel.add(startBtn, gbc);
        gbc.gridy = 1;
        centerPanel.add(userCountLabel, gbc);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);
        return panel;
    }

    // 매칭 대기 화면
    private JPanel createWaitScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel waitLabel = new JLabel("상대방을 기다리는 중입니다...", SwingConstants.CENTER);
        waitLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        panel.add(waitLabel, BorderLayout.CENTER);
        return panel;
    }

    // 캐릭터 선택 화면
    private JPanel createSelectScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel guideLabel = new JLabel("캐릭터를 선택하세요!", SwingConstants.CENTER);
        guideLabel.setFont(new Font("SansSerif", Font.BOLD, 25));

        JPanel charPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        String[] chars = {"전사 (체력형)", "마법사 (공격형)", "궁수 (속도형)"};

        for (int i = 0; i < 3; i++) {
            final int charIdx = i;
            JButton btn = new JButton("<html><center>" + chars[i] + "<br>[이미지 공간]</center></html>");

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

    // 전투 화면
    private JPanel createBattleScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        JPanel topPanel = new JPanel(new GridLayout(1, 2));

        JPanel myPanel = new JPanel(new BorderLayout());
        myCharImg = new JLabel("[내 캐릭터]", SwingConstants.CENTER);
        myCharImg.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        myHpBar = new JProgressBar();
        myHpBar.setStringPainted(true);
        myHpBar.setForeground(Color.GREEN);
        myPanel.add(myCharImg, BorderLayout.CENTER);
        myPanel.add(myHpBar, BorderLayout.SOUTH);

        JPanel oppPanel = new JPanel(new BorderLayout());
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
        battleLog = new JTextArea(5, 40);
        battleLog.setEditable(false);
        JScrollPane scroll = new JScrollPane(battleLog);

        JPanel actionPanel = new JPanel(new FlowLayout());
        atkBtn = new JButton("공격 (Attack)");
        defBtn = new JButton("방어 (Defend)");
        healBtn = new JButton("회복 (Heal)");

        atkBtn.addActionListener(e -> sendMessage("ACTION:ATTACK"));
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
        JPanel panel = new JPanel(new BorderLayout());
        JLabel resLabel = new JLabel(resultMsg, SwingConstants.CENTER);
        resLabel.setFont(new Font("SansSerif", Font.BOLD, 40));
        resLabel.setName("RESULT_LABEL");

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

    private void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }

    private void processServerMessage(String msg) {
        if (msg.startsWith("USER_COUNT:")) {
            userCountLabel.setText("현재 접속자 수: " + msg.split(":")[1] + "명");

        } else if (msg.equals("MATCH_SUCCESS")) {
            JOptionPane.showMessageDialog(this, "매칭이 되었습니다! 곧 게임이 시작됩니다.");
            cardLayout.show(mainPanelContainer, "SELECT");

        } else if (msg.startsWith("GAME_START:")) {
            String[] parts = msg.split(":");
            int oppMaxHp = Integer.parseInt(parts[2]);

            oppHpBar.setMaximum(oppMaxHp);
            oppHpBar.setValue(oppMaxHp);
            oppHpBar.setString(oppMaxHp + " / " + oppMaxHp);

            myMaxHp = getMaxHpForChar(myCharType);
            myHpBar.setMaximum(myMaxHp);
            myHpBar.setValue(myMaxHp);
            myHpBar.setString(myMaxHp + " / " + myMaxHp);

            battleLog.setText("전투 시작!\n");
            cardLayout.show(mainPanelContainer, "BATTLE");

        } else if (msg.equals("YOUR_TURN")) {
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
            String endMsg = result.equals("WIN") ? "승리했습니다!" : "패배했습니다...";

            JPanel resPanel = (JPanel) mainPanelContainer.getComponent(4);
            for (Component c : resPanel.getComponents()) {
                if ("RESULT_LABEL".equals(c.getName())) {
                    ((JLabel) c).setText(endMsg);
                }
            }
            cardLayout.show(mainPanelContainer, "RESULT");
        }
    }

    public static void main(String[] args) {
        new GameClient();
    }
}
