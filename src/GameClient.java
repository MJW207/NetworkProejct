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
    private JLabel myCharLabel; // 내 캐릭터 정보 표시

    // 공통 액션 버튼
    JButton atkBtn;
    // 고유 스킬 버튼 (캐릭터별로 동적으로 설정)
    JButton skillBtn1, skillBtn2; 
    
    // 캐릭터 상태 추적
    private int myCharType = -1;
    private int myMaxHp = 100;
    private String[] charNames = {"전사 (Warrior)", "마법사 (Magician)", "엘프 (Elf)"};
    
    // 한국어 폰트 설정
    private static final String KOREAN_FONT_NAME = "맑은 고딕";


    public GameClient() {
        setTitle("Battle Arena Online [Team 28]");
        setSize(1000, 700); // 크기 확장
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        
        // 폰트 설정
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

    // 메인 화면
    private JPanel createMainScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(50, 50, 70));
        
        JLabel titleLabel = new JLabel("BATTLE ARENA ONLINE", SwingConstants.CENTER);
        titleLabel.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 48));
        titleLabel.setForeground(new Color(255, 180, 0)); // Accent color
        titleLabel.setBorder(BorderFactory.createEmptyBorder(30, 0, 30, 0));

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setBackground(panel.getBackground());
        
        JButton startBtn = new JButton("게임 시작 (매칭 찾기)");
        startBtn.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 20));
        startBtn.setPreferredSize(new Dimension(300, 70));
        startBtn.setBackground(new Color(26, 188, 156));
        startBtn.setForeground(Color.WHITE);

        userCountLabel = new JLabel("현재 접속자 수: 0명");
        userCountLabel.setFont(new Font(KOREAN_FONT_NAME, Font.PLAIN, 18));
        userCountLabel.setForeground(Color.WHITE);

        startBtn.addActionListener(e -> {
            sendMessage("REQUEST_MATCH");
            cardLayout.show(mainPanelContainer, "WAIT");
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(20,0,20,0);
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
        panel.setBackground(new Color(50, 50, 70));
        
        JLabel waitLabel = new JLabel("상대방을 기다리는 중입니다...", SwingConstants.CENTER);
        waitLabel.setFont(new Font(KOREAN_FONT_NAME, Font.PLAIN, 24));
        waitLabel.setForeground(new Color(255, 180, 0));
        panel.add(waitLabel, BorderLayout.CENTER);
        return panel;
    }

    // 캐릭터 선택 화면
    private JPanel createSelectScreen() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(50, 50, 70));
        
        JLabel guideLabel = new JLabel("캐릭터를 선택하세요!", SwingConstants.CENTER);
        guideLabel.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 30));
        guideLabel.setForeground(new Color(26, 188, 156));
        guideLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0));

        JPanel charPanel = new JPanel(new GridLayout(1, 3, 20, 20));
        charPanel.setBackground(panel.getBackground());
        charPanel.setBorder(BorderFactory.createEmptyBorder(0, 50, 50, 50));

        // 캐릭터 설명 업데이트 (새로운 스탯 및 기능 반영)
        String[] charDescriptions = {
            "전사: HP 300, DEF 15. 높은 체력/방어력, 낮은 공격/속공.\n 스킬: 방어력 +10 (3회), 방패(1턴 무효, 2회).",
            "마법사: HP 180, ATK 70. 최고 공격력, 낮은 체력/방어력.\n 스킬: 메테오(강력 공격, 2회), 디버프(상대 ATK/DEF 하락, 3회).",
            "엘프: HP 220, SPEED 30. 최고 속공, 균형 잡힌 능력치.\n 스킬: 갑옷뚫기(공격+상대 DEF 하락, 3회), 회피(1턴 회피, 2회)."
        };

        for (int i = 0; i < 3; i++) {
            final int charIdx = i;
            JButton btn = new JButton("<html><center>" + charNames[i] + "<br><br><span style='font-size:10px;'>" + charDescriptions[i].replaceAll("\n", "<br>") + "</span></center></html>");
            btn.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 16));
            btn.setBackground(new Color(70, 90, 110));
            btn.setForeground(Color.WHITE);

            btn.addActionListener(e -> {
                myCharType = charIdx;
                sendMessage("SELECT:" + charIdx);
                guideLabel.setText(charNames[charIdx] + " 선택 완료! 상대방 기다리는 중...");
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
        panel.setBackground(new Color(50, 50, 70));
        
        // 상단 캐릭터 정보 패널
        JPanel topPanel = new JPanel(new GridLayout(1, 2, 20, 0));
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        topPanel.setBackground(panel.getBackground());

        // 내 캐릭터 정보
        JPanel myPanel = new JPanel(new BorderLayout());
        myPanel.setBackground(new Color(70, 90, 110));
        myPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(new Color(26, 188, 156), 2), "나의 캐릭터", 
            0, 2, new Font(KOREAN_FONT_NAME, Font.BOLD, 18), Color.WHITE));
        
        myCharImg = new JLabel("[내 캐릭터 이미지/이름]", SwingConstants.CENTER);
        myCharImg.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 24));
        myCharImg.setForeground(Color.WHITE);
        myHpBar = new JProgressBar();
        myHpBar.setStringPainted(true);
        myHpBar.setForeground(new Color(46, 204, 113)); // Emerald Green
        myHpBar.setBackground(new Color(50, 50, 70));
        myPanel.add(myCharImg, BorderLayout.CENTER);
        myPanel.add(myHpBar, BorderLayout.SOUTH);
        
        myCharLabel = new JLabel("상태: -", SwingConstants.CENTER);
        myCharLabel.setForeground(new Color(255, 180, 0));
        myPanel.add(myCharLabel, BorderLayout.NORTH);

        // 상대 캐릭터 정보
        JPanel oppPanel = new JPanel(new BorderLayout());
        oppPanel.setBackground(new Color(70, 90, 110));
        oppPanel.setBorder(BorderFactory.createTitledBorder(
            BorderFactory.createLineBorder(Color.RED, 2), "상대 캐릭터",
            0, 2, new Font(KOREAN_FONT_NAME, Font.BOLD, 18), Color.WHITE));
        
        oppCharImg = new JLabel("[상대 캐릭터 이미지/이름]", SwingConstants.CENTER);
        oppCharImg.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 24));
        oppCharImg.setForeground(Color.WHITE);
        oppHpBar = new JProgressBar();
        oppHpBar.setStringPainted(true);
        oppHpBar.setForeground(new Color(231, 76, 60)); // Alizarin Red
        oppHpBar.setBackground(new Color(50, 50, 70));
        oppPanel.add(oppCharImg, BorderLayout.CENTER);
        oppPanel.add(oppHpBar, BorderLayout.SOUTH);

        topPanel.add(myPanel);
        topPanel.add(oppPanel);

        // 하단 로그 및 액션 패널
        JPanel bottomPanel = new JPanel(new BorderLayout(0, 10));
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 10, 20));
        
        battleLog = new JTextArea(10, 40);
        battleLog.setEditable(false);
        battleLog.setBackground(new Color(44, 62, 80)); // Wet Asphalt
        battleLog.setForeground(new Color(236, 240, 241)); // Light Text
        battleLog.setFont(new Font(KOREAN_FONT_NAME, Font.PLAIN, 14));
        JScrollPane scroll = new JScrollPane(battleLog);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(127, 140, 141), 1));


        // 액션 버튼 패널
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 15, 5));
        actionPanel.setBackground(panel.getBackground());
        
        // 공통 버튼
        atkBtn = createActionButton("공격 (일반)", "ATTACK");
        
        // 고유 스킬 버튼 (2개만 표시)
        skillBtn1 = createActionButton("스킬 1", "SKILL1"); // 초기 더미
        skillBtn2 = createActionButton("스킬 2", "SKILL2"); // 초기 더미
        
        actionPanel.add(atkBtn);
        actionPanel.add(skillBtn1);
        actionPanel.add(skillBtn2);
        

        bottomPanel.add(scroll, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        panel.add(topPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);
        return panel;
    }
    
    // 액션 버튼 생성 헬퍼
    private JButton createActionButton(String text, String command) {
        JButton btn = new JButton(text);
        btn.setPreferredSize(new Dimension(200, 40));
        btn.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 14));
        btn.setBackground(new Color(52, 152, 219)); // Peter River
        btn.setForeground(Color.WHITE);
        btn.setFocusPainted(false);
        
        // 액션 리스너는 processServerMessage에서 처리되는 ACTION 프로토콜을 따름
        btn.addActionListener(e -> {
            if (btn.isEnabled()) {
                // 고유 스킬 버튼은 커맨드에 따라 실제 프로토콜을 다르게 보냄
                if (command.equals("SKILL1")) {
                    sendMessage(getSkillCommand(1));
                } else if (command.equals("SKILL2")) {
                    sendMessage(getSkillCommand(2));
                } else {
                    sendMessage("ACTION:" + command); // ATTACK, DEFEND
                }
            }
        });
        return btn;
    }
    
    // 캐릭터 타입에 따른 스킬 커맨드 생성
    private String getSkillCommand(int skillNum) {
        String base = "ACTION:";
        if (myCharType == 0) { // 전사
            return base + (skillNum == 1 ? "WAR_DEFUP" : "WAR_SHIELD");
        } else if (myCharType == 1) { // 마법사
            return base + (skillNum == 1 ? "MAG_METEOR" : "MAG_DEBUFF");
        } else if (myCharType == 2) { // 엘프
            return base + (skillNum == 1 ? "ELF_ARMORBREAK" : "ELF_EVASION");
        }
        return base + "INVALID";
    }

    // 결과 화면
    private JPanel createResultScreen(String resultMsg) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(50, 50, 70));
        
        JLabel resLabel = new JLabel(resultMsg, SwingConstants.CENTER);
        resLabel.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 50));
        resLabel.setForeground(new Color(255, 180, 0));
        resLabel.setName("RESULT_LABEL");

        JButton homeBtn = new JButton("메인 화면으로");
        homeBtn.setFont(new Font(KOREAN_FONT_NAME, Font.BOLD, 20));
        homeBtn.setPreferredSize(new Dimension(250, 60));
        homeBtn.setBackground(new Color(26, 188, 156));
        homeBtn.setForeground(Color.WHITE);

        homeBtn.addActionListener(e -> {
            // 서버에 게임 종료를 알리고 메인으로 복귀 (서버에서 상태 초기화는 이미 됨)
            cardLayout.show(mainPanelContainer, "MAIN");
        });

        panel.add(resLabel, BorderLayout.CENTER);
        panel.add(homeBtn, BorderLayout.SOUTH);
        return panel;
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
                // 서버 연결 실패 시 메인 화면에 오류 메시지 표시
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "서버 연결에 실패했습니다.", "연결 오류", JOptionPane.ERROR_MESSAGE));
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
            // 게임 재시작 시 클라이언트 상태 초기화: 
            myCharType = -1; // 클라이언트 캐릭터 선택 초기화
            
            // 캐릭터 선택 버튼 활성화
            JPanel selectPanel = (JPanel) mainPanelContainer.getComponent(2); // SELECT 화면 인덱스 2
            JPanel charPanel = (JPanel) selectPanel.getComponent(1); // 두 번째 컴포넌트(캐릭터 버튼 패널)
            for (Component c : charPanel.getComponents()) c.setEnabled(true);
            
            // 안내 메시지 초기화
            JLabel guideLabel = (JLabel) selectPanel.getComponent(0);
            guideLabel.setText("캐릭터를 선택하세요!");
            
            JOptionPane.showMessageDialog(this, "매칭이 되었습니다! 캐릭터를 선택하세요.");
            cardLayout.show(mainPanelContainer, "SELECT");

        } else if (msg.startsWith("GAME_START:")) {
            // GAME_START:상대캐릭터타입:상대maxHp:나의maxHp
            String[] parts = msg.split(":");
            int oppCharType = Integer.parseInt(parts[1]);
            int oppMaxHp = Integer.parseInt(parts[2]);
            myMaxHp = Integer.parseInt(parts[3]);

            // UI 설정 및 초기화
            myHpBar.setMaximum(myMaxHp);
            myHpBar.setValue(myMaxHp);
            myHpBar.setString(myMaxHp + " / " + myMaxHp);

            oppHpBar.setMaximum(oppMaxHp);
            oppHpBar.setValue(oppMaxHp);
            oppHpBar.setString(oppMaxHp + " / " + oppMaxHp);

            myCharImg.setText(charNames[myCharType] + " (나)");
            oppCharImg.setText(charNames[oppCharType] + " (상대)");
            
            // 전투 시작 시 스킬 버튼 텍스트 설정 (오류 해결 핵심)
            setSkillButtonTextAndInitialState(myCharType); // 쿨다운 횟수를 반영한 텍스트 설정

            battleLog.setText("전투 시작!\n");
            cardLayout.show(mainPanelContainer, "BATTLE");

        } else if (msg.startsWith("YOUR_TURN:")) {
            // YOUR_TURN:나의스킬1:나의스킬2:상대의스킬1:상대의스킬2
            String[] parts = msg.split(":");
            updateSkillButtonState(true, parts[1], parts[2], parts[3], parts[4]);
            
            battleLog.append(">> 당신의 턴입니다. 행동을 선택하세요.\n");
            myCharLabel.setText("상태: 나의 턴!");

        } else if (msg.startsWith("OPP_TURN:")) {
            // OPP_TURN:상대의스킬1:상대의스킬2:나의스킬1:나의스킬2
            String[] parts = msg.split(":");
            updateSkillButtonState(false, parts[3], parts[4], parts[1], parts[2]);

            battleLog.append(">> 상대방의 턴입니다...\n");
            myCharLabel.setText("상태: 상대 턴...");


        } else if (msg.startsWith("UPDATE:")) {
            // UPDATE:나의hp:상대hp:나의남은스킬1:나의남은스킬2:상대남은스킬1:상대남은스킬2:로그
            String[] parts = msg.split(":", 8);
            int myHp  = Integer.parseInt(parts[1]);
            int oppHp = Integer.parseInt(parts[2]);
            String log = parts[7];

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
    
    // 모든 액션 버튼 활성화/비활성화
    private void setAllActionsEnabled(boolean enabled) {
        atkBtn.setEnabled(enabled);
        
        // 스킬 버튼은 쿨다운 상태에 따라 추가적으로 조정됨
        // isEnabled 플래그는 쿨다운과 관계없이 턴 자체를 막는 역할만 수행
        skillBtn1.setEnabled(enabled); 
        skillBtn2.setEnabled(enabled); 
    }
    
    // 캐릭터 타입에 따른 스킬 버튼 텍스트 및 초기 상태 설정
    private void setSkillButtonTextAndInitialState(int charType) {
        String skill1Name;
        int skill1InitialCount;
        String skill2Name;
        int skill2InitialCount;
        
        if (charType == 0) { // 전사
            skill1Name = "방어력 높이기"; skill1InitialCount = 3;
            skill2Name = "방패"; skill2InitialCount = 2;
        } else if (charType == 1) { // 마법사
            skill1Name = "메테오"; skill1InitialCount = 2;
            skill2Name = "디버프"; skill2InitialCount = 3;
        } else { // 엘프
            skill1Name = "갑옷뚫기"; skill1InitialCount = 3;
            skill2Name = "회피"; skill2InitialCount = 2;
        }
        
        skillBtn1.setText(skill1Name + " (" + skill1InitialCount + "회)");
        skillBtn2.setText(skill2Name + " (" + skill2InitialCount + "회)");
        
        // 초기 쿨다운 횟수에 따라 버튼 활성화/비활성화 상태 설정
        skillBtn1.setEnabled(skill1InitialCount > 0);
        skillBtn2.setEnabled(skill2InitialCount > 0);
    }
    
    // 스킬 버튼 상태 및 쿨다운 업데이트 (쿨다운이 0이면 버튼 비활성화)
    private void updateSkillButtonState(boolean isMyTurn, String mySkill1CountStr, String mySkill2CountStr, 
                                        String oppSkill1CountStr, String oppSkill2CountStr) {
        try {
            int mySkill1Count = Integer.parseInt(mySkill1CountStr);
            int mySkill2Count = Integer.parseInt(mySkill2CountStr);
            
            // 1. 공통 버튼 턴 활성화/비활성화 설정
            setAllActionsEnabled(isMyTurn);
            
            // 2. 나의 스킬 버튼 쿨다운 업데이트 및 턴 활성화/비활성화
            
            // myCharType에 맞는 텍스트와 쿨다운을 조합하여 설정
            setSkillButtonTextAndCount(skillBtn1, mySkill1Count, 1);
            setSkillButtonTextAndCount(skillBtn2, mySkill2Count, 2);
            
            // 턴이 돌아왔을 때 (isMyTurn == true) 쿨다운이 0이면 비활성화
            if (isMyTurn) {
                if (mySkill1Count == 0) skillBtn1.setEnabled(false);
                if (mySkill2Count == 0) skillBtn2.setEnabled(false);
            }
            
        } catch (NumberFormatException e) {
            System.err.println("스킬 카운트 파싱 오류: " + e.getMessage());
        }
    }
    
    // SkillBtn의 텍스트와 상태를 갱신하는 헬퍼
    private void setSkillButtonTextAndCount(JButton button, int count, int skillNum) {
        String skillName;
        if (myCharType == 0) { // 전사
            skillName = (skillNum == 1) ? "방어력 높이기" : "방패";
        } else if (myCharType == 1) { // 마법사
            skillName = (skillNum == 1) ? "메테오" : "디버프";
        } else { // 엘프
            skillName = (skillNum == 1) ? "갑옷뚫기" : "회피";
        }
        button.setText(skillName + " (" + count + "회)");
    }


    public static void main(String[] args) {
        new GameClient();
    }
}