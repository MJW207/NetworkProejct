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

    private JLabel guideLabel; 
    private JPanel charPanel;

    private int myCharType = -1;
    private int myMaxHp = 100;
    private int oppCharType = -1;
    private final String[] charNames = {"전사 (Warrior)", "마법사 (Magician)", "엘프 (Elf)"};

    private static final String KOREAN_FONT_NAME = "맑은 고딕";
    private MusicPlayer bgmPlayer = new MusicPlayer();

    public GameClient() {
    	// 기본 프레임 설정 : 제목, 크기, 종료 동작, 위치
        setTitle("Battle Arena Online [Team 28]");
        setSize(1000, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        // 배경 음악 재성 설정
        bgmPlayer.playLoop("/sound/sound.wav");
        // UI 폰트 스타일 한글로 통일
        Font defaultFont = new Font(KOREAN_FONT_NAME, Font.PLAIN, 12);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("TextArea.font", defaultFont);
        UIManager.put("OptionPane.font", defaultFont);
        
        // 화면 전환을 위한 카드 레이아웃 및 매인 컨테이너 설정
        cardLayout = new CardLayout();
        mainPanelContainer = new JPanel(cardLayout);
        
        // 각 게임화면 컨테이너 추가
        mainPanelContainer.add(createMainScreen(), "MAIN");
        mainPanelContainer.add(createWaitScreen(), "WAIT");
        mainPanelContainer.add(createSelectScreen(), "SELECT");
        mainPanelContainer.add(createBattleScreen(), "BATTLE");
        mainPanelContainer.add(createResultScreen("결과 대기중..."), "RESULT");

        add(mainPanelContainer);
        
        // 서버로 연결 시도하는 함수
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

    // 게임이 시작되었을 때 시작하는 화면 설정
    private JPanel createMainScreen() {
        JPanel panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());
        
        // "battle.png" 이미지 로드 미치 크기 조정
        ImageIcon battleIcon = new ImageIcon(getClass().getResource("/image/battle.png"));
        Image scaled = battleIcon.getImage().getScaledInstance(400, 300, Image.SCALE_SMOOTH);
        JLabel titleLabel = new JLabel(new ImageIcon(scaled), SwingConstants.CENTER);
        titleLabel.setOpaque(false);

        JPanel centerPanel = new JPanel(new GridBagLayout());
        centerPanel.setOpaque(false);
        
        // 시작 버튼 이미지 로드 미치 크기 조정
        ImageIcon startIcon = new ImageIcon(getClass().getResource("/image/start.png"));
        Image original = startIcon.getImage();
        int newWidth = 150;
        int newHeight = original.getHeight(null) * newWidth / original.getWidth(null);
        Image scaledImage = original.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH);
        JButton startBtn = new JButton(new ImageIcon(scaledImage));
        
        // 시작 버튼 외곽선을 제거하고 배경을 제거(UI를 매끄럽게 설정하기 위해서)
        startBtn.setBorderPainted(false);
        startBtn.setContentAreaFilled(false);
        startBtn.setFocusPainted(false);
        startBtn.setOpaque(false);

        // 현재 접속자 수 라벨 설정
        userCountLabel = new JLabel("현재 접속자 수: 0명");
        userCountLabel.setFont(new Font("SansSerif", Font.BOLD, 16));
        userCountLabel.setOpaque(false);

        // 시작버튼 이벤트리스너 -> 서버에 "REQUEST_MATCH" 메세지 전송, 대기 화면으로 전환
        startBtn.addActionListener(e -> {
            sendMessage("REQUEST_MATCH");
            cardLayout.show(mainPanelContainer, "WAIT");
        });
        
        // 버튼과 라벨을 중앙 패널에 위치
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0; gbc.gridy = 0; gbc.insets = new Insets(10, 0, 10, 0);
        centerPanel.add(startBtn, gbc);
        gbc.gridy = 1;
        centerPanel.add(userCountLabel, gbc);

        // 타이틀은 상단, 버튼/접속자는 중앙에 배치
        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(centerPanel, BorderLayout.CENTER);

        return panel;
    }

    // 게임 대기화면 설정
    private JPanel createWaitScreen() {
        JPanel panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());

        // 중앙에 대기 메세지 라벨 생성
        JLabel waitLabel = new JLabel("상대방을 기다리는 중입니다...", SwingConstants.CENTER);
        waitLabel.setFont(new Font("SansSerif", Font.PLAIN, 20));
        waitLabel.setOpaque(false);

        panel.add(waitLabel, BorderLayout.CENTER);
        return panel;
    }

    // 게임 캐릭터 선택화면 설정
    private JPanel createSelectScreen() {
        JPanel panel = new BackgroundPanel();
        panel.setLayout(new BorderLayout());

        // 상단에 안내 메시지 라벨 설정
        guideLabel = new JLabel("캐릭터를 선택하세요!", SwingConstants.CENTER);
        guideLabel.setFont(new Font("SansSerif", Font.BOLD, 20));
        guideLabel.setOpaque(false);

        // 캐릭터 선택 버튼을 담을 패널 설정
        charPanel = new JPanel(new GridLayout(1, 3, 10, 10));
        charPanel.setOpaque(false);
        
        // 캐릭터 이름과 이미지 경로 배열
        String[] chars = {"전사 (체력형)", "마법사 (공격형)", "궁수 (속도형)"};
        String[] imagePaths = {"/image/warrior1.png", "/image/magician1.png", "/image/elp1.png"};

        // 캐릭터 3종의 버튼 생성
        for (int i = 0; i < 3; i++) {
            int idx = i;
            ImageIcon icon = new ImageIcon(getClass().getResource(imagePaths[i]));
            Image scaledChar = icon.getImage().getScaledInstance(200, 200, Image.SCALE_SMOOTH);
            
            // 버튼 생성(텍스트 + 이미지)
            JButton btn = new JButton("<html><center>" + chars[i] + "</center></html>", new ImageIcon(scaledChar));
            btn.setHorizontalTextPosition(SwingConstants.CENTER);
            btn.setVerticalTextPosition(SwingConstants.BOTTOM);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(true);

            // 캐릭터 버튼 클릭 시 이벤트 리스너 -> 선택된 캐릭터를 서버로 전송
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

    // 전투 화면 설정
    private JPanel createBattleScreen() {
    	// 전투 배경화면으로 전환
        JPanel panel = new JPanel(new BorderLayout()) {
            Image backgroundImage = new ImageIcon(getClass().getResource("/image/battle_background.png")).getImage();

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };

        // 내 캐릭터 정보와 상대 캐릭터 정보를 상단에 배치
        JPanel topPanel = new JPanel(new GridLayout(1, 2));
        topPanel.setOpaque(false);

        // 내캐릭터 영역 설정, 내 캐릭터의 이미지를 불러오고 HPBar를 불러옴
        JPanel myPanel = new JPanel(new BorderLayout());
        myPanel.setOpaque(false);
        myCharImg = new JLabel("[내 캐릭터]", SwingConstants.CENTER);
        myCharImg.setBorder(BorderFactory.createLineBorder(Color.BLUE, 2));
        myHpBar = new JProgressBar();
        myHpBar.setStringPainted(true);
        myHpBar.setForeground(Color.GREEN);
        myPanel.add(myCharImg, BorderLayout.CENTER);
        myPanel.add(myHpBar, BorderLayout.SOUTH);

        // 상대 캐릭터 영역 설정, 상대 캐릭터의 이미지를 불러오고 HPBar를 불러옴
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

        // 하단에는 전투 로그와 스킬 버튼 배치
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setOpaque(false);

        // 전투 로그 텍스트 영역
        battleLog = new JTextArea(5, 40);
        battleLog.setEditable(false);
        JScrollPane scroll = new JScrollPane(battleLog);

        // 스킬 버튼 영역
        JPanel actionPanel = new JPanel(new FlowLayout());
        actionPanel.setOpaque(false);

        // 스킬 버튼 이미지 설정
        ImageIcon atkIcon = loadScaledIcon("/image/기본공격.png", 100, 100);
        ImageIcon specialIcon = loadScaledIcon("/image/특수공격.png", 100, 100);
        ImageIcon defIcon = loadScaledIcon("/image/방어.png", 100, 100);
        
        atkBtn = new JButton(atkIcon);
        skillBtn1 = new JButton(specialIcon);
        skillBtn2 = new JButton(defIcon);
        
        // 버튼 배경, 테두리 제거 (이미지 버튼처럼 보이게 설정)
        for (JButton btn : new JButton[]{atkBtn, skillBtn1, skillBtn2}) {
            btn.setBorderPainted(false);
            btn.setContentAreaFilled(false);
            btn.setFocusPainted(false);
            btn.setOpaque(false);
            styleActionButton(btn);  
        }


        // 기본 공격 버튼 이벤트 리스너 -> 서버로 "ACTION:ATTACK" 메세지를 보냄
        // 내 캐릭터는 때리는 이미지로 변경, 상대 캐릭터는 맞는 이미지로 변경
        atkBtn.addActionListener(e -> {
            sendMessage("ACTION:ATTACK");
            myCharImg.setIcon(getAttackImage(myCharType, 2));
            oppCharImg.setIcon(getAttackImage(oppCharType, 3));
            disableActionButtons();
            // 1초 동안 때리고, 맞는 이미지로 변경되었다가 다시 기본 이미지로 돌아옴
            new javax.swing.Timer(1000, evt -> {
                myCharImg.setIcon(getAttackImage(myCharType, 1));
                oppCharImg.setIcon(getAttackImage(oppCharType, 1));
                ((javax.swing.Timer) evt.getSource()).stop();
            }).start();
        });

        // 특수 공격 버튼 이벤트 리스너 -> 서버로 "ACTION:SPECIAL"을 전송
        skillBtn1.addActionListener(e -> {
            sendMessage("ACTION:SPECIAL"); 
            myCharImg.setIcon(getAttackImage(myCharType, 2)); 
            oppCharImg.setIcon(getAttackImage(oppCharType, 3));
            disableActionButtons();
            new javax.swing.Timer(1000, evt -> {
                myCharImg.setIcon(getAttackImage(myCharType, 1));
                oppCharImg.setIcon(getAttackImage(oppCharType, 1));
                ((javax.swing.Timer) evt.getSource()).stop();
            }).start();
        });

        // 방어 버튼 이벤트 리스너 -> 서버로 "ACTION:DEFEND"를 전송
        // 이미지 애니메이션처리 X
        skillBtn2.addActionListener(e -> {
            sendMessage("ACTION:DEFEND");
            disableActionButtons();
        });

        // 버튼 들을 액션 패널에 추가
        actionPanel.add(atkBtn);
        actionPanel.add(skillBtn1);
        actionPanel.add(skillBtn2);

        // 하단에 전투 로그 + 액션 버튼을 배치
        bottomPanel.add(scroll, BorderLayout.CENTER);
        bottomPanel.add(actionPanel, BorderLayout.SOUTH);

        // 최종으로 상단에 캐릭터 영역, 하단에는 버튼/로그 영역
        panel.add(topPanel, BorderLayout.CENTER);
        panel.add(bottomPanel, BorderLayout.SOUTH);

        return panel;
    }

    // 공격했을때 때리는, 맞는 이미지를 불러오는 함수
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
    // 마지막 최종 승리, 패배 화면 구현
    private JPanel createResultScreen(String resultMsg) {
    	// 배경이미지를 다시 초기 배경이미지로 변경
        JPanel panel = new JPanel(new BorderLayout()) {
            Image backgroundImage = new ImageIcon(getClass().getResource("/image/background.png")).getImage();
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                g.drawImage(backgroundImage, 0, 0, getWidth(), getHeight(), this);
            }
        };
        // 결과 메세지 라벨
        JLabel resLabel = new JLabel(resultMsg, SwingConstants.CENTER);
        resLabel.setName("RESULT_LABEL");
        
        // 메인화면으로 다시 돌아가는 버튼
        JButton homeBtn = new JButton();
        // 1. 이미지 로드 (크기는 버튼에 맞게 200x80 정도로 조절)
        ImageIcon restartIcon = loadScaledIcon("/image/restart.png", 200, 80);
        
        if (restartIcon != null) {
            homeBtn.setIcon(restartIcon);
            // 2. 버튼의 테두리와 배경을 없애서 이미지만 보이게 설정
            homeBtn.setBorderPainted(false);
            homeBtn.setContentAreaFilled(false);
            homeBtn.setFocusPainted(false);
        } else {
            // 이미지가 없을 경우 텍스트 표시
            homeBtn.setText("메인 화면으로");
            homeBtn.setPreferredSize(new Dimension(200, 50));
        }
        // 버튼 클릭시 게임 데이터 리셋 후 메인 화면으로 전환
        homeBtn.addActionListener(e -> {
            resetGameData(); // 게임 리셋
            cardLayout.show(mainPanelContainer, "MAIN");
        });
        panel.add(resLabel, BorderLayout.CENTER);
        panel.add(homeBtn, BorderLayout.SOUTH);
        return panel;
    }

    // 서버에 접속을 하는 함수
    private void connectToServer() {
    	// 서버 연결을 별도의 스레드에서 비동기 처리
        new Thread(() -> {
            try {
            	// 소켓으로 서버에 연결 시도
                socket = new Socket(SERVER_IP, PORT);
                // 입출력 스트림 생성
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                // 서버로부터 오는 메세지를 반복적으로 수신
                String msg;
                while ((msg = in.readLine()) != null) {
                    String finalMsg = msg;
                    // Swing UI 스레드에서 처리 할 수 있도록 설정
                    SwingUtilities.invokeLater(() -> processServerMessage(finalMsg));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    // 서버로 메세지를 보내는 함수
    private void sendMessage(String msg) {
        if (out != null) out.println(msg);
    }


    // 캐릭터 최대(초기)HP를 설정하는 함수
    private int getMaxHpForChar(int charType) {
        return switch (charType) {
            case 0 -> 200;
            case 1 -> 150;
            case 2 -> 170;
            default -> 100;
        };
    }

    // 서버에서 클라이언트로 전송된 메세지를 받아서 그에 따른 UI나 게임 상태를 업데이트하는 함수
    private void processServerMessage(String msg) {
    	// 접속자 수 메시지를 처리
        if (msg.startsWith("USER_COUNT:")) {
            userCountLabel.setText("현재 접속자 수: " + msg.split(":")[1] + "명");
          // 매칭 성공 시 캐릭터 선택 화면으로 전환  
        } else if (msg.equals("MATCH_SUCCESS")) {
            cardLayout.show(mainPanelContainer, "SELECT");
          // 게임 시작 메세지 처리(전투 BGM으로 변경 + 상대 캐릭터 정보, 초기 설정)
        } else if (msg.startsWith("GAME_START:")) {
        	bgmPlayer.stop();
            bgmPlayer.playLoop("/sound/sound2.wav");
            String[] parts = msg.split(":");
            // 상대 캐릭터 타입, 정보를 받음
            oppCharType = Integer.parseInt(parts[1]);
            int oppMaxHp = Integer.parseInt(parts[2]);
            myMaxHp = Integer.parseInt(parts[3]);
            
            // 내 체력바 초기화
            myHpBar.setMaximum(myMaxHp);
            myHpBar.setValue(myMaxHp);
            myHpBar.setString(myMaxHp + " / " + myMaxHp);

            // 상대 체력바 초기화
            oppHpBar.setMaximum(oppMaxHp);
            oppHpBar.setValue(oppMaxHp);
            oppHpBar.setString(oppMaxHp + " / " + oppMaxHp);

            // 캐릭터 이름 표기
            myCharImg.setText(charNames[myCharType] + " (나)");
            oppCharImg.setText(charNames[oppCharType] + " (상대)");

            // 공격, 특수공격, 방어 버튼 활성화
            atkBtn.setEnabled(true);
            skillBtn1.setEnabled(true);
            skillBtn2.setEnabled(true);
            // 초기 이미지로 캐릭터 세팅
            myCharImg.setIcon(getAttackImage(myCharType, 1));
            oppCharImg.setIcon(getAttackImage(oppCharType, 1));
            // 전투 로그 초기화 및 화면 전환
            battleLog.setText("전투 시작!\n");
            cardLayout.show(mainPanelContainer, "BATTLE");
          // 내 턴이 되었을때
        } else if (msg.startsWith("YOUR_TURN")) {
            String[] parts = msg.split(":");
            // 특수 공격 남은 횟수
            int mySpecialCount = Integer.parseInt(parts[1]);
            enableActionButtons(mySpecialCount); 

            battleLog.append(">> 당신의 턴입니다. (남은 특수공격: " + mySpecialCount + "회)\n");
         // 상대 턴이 되었을때  
        } else if (msg.startsWith("OPP_TURN")) {
            disableActionButtons(); 
            battleLog.append(">> 상대방의 턴입니다...\n");
          
          // 체력 정보 및 로그 업데이트(버튼을 누르면 실시간으로 적용될 수 있도록 설정)
        } else if (msg.startsWith("UPDATE:")) {
            String[] parts = msg.split(":", 4);
            int myHp = Integer.parseInt(parts[1]);
            int oppHp = Integer.parseInt(parts[2]);
            String log = parts[3];

            // 체력바 갱신
            myHpBar.setValue(myHp);
            myHpBar.setString(myHp + " / " + myMaxHp);

            oppHpBar.setValue(oppHp);
            oppHpBar.setString(oppHp + " / " + oppHpBar.getMaximum());

            // 전투 로그갱신
            battleLog.append(log + "\n");
            battleLog.setCaretPosition(battleLog.getDocument().getLength());
            
           // 전투 결과 처리
        }else if (msg.startsWith("RESULT:")) {
            String result = msg.split(":")[1];
            bgmPlayer.playLoop("/sound/sound.wav");

            // 승패 결과에 따라서 이미지 선택(승리 했을 시 승리 이미지, 패배시 패배 이미지)
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
    
    // 이미지를 UI에 알맞게 설정하는 함수
    private ImageIcon loadScaledIcon(String path, int width, int height) {
        java.net.URL imgUrl = getClass().getResource(path);
        if (imgUrl == null) {
            System.err.println("이미지 파일을 찾을 수 없음: " + path);
            return null;
        }
        ImageIcon icon = new ImageIcon(imgUrl);
        Image scaled = icon.getImage().getScaledInstance(width, height, Image.SCALE_SMOOTH);
        return new ImageIcon(scaled);
    }

    // 버튼의 텍스트 위치와 주변을 정리해주는 함수
    private void styleActionButton(JButton btn) {
        if (btn == null) return;
        // 버튼 배경과 테두리 정리
        btn.setFocusPainted(false); // 클릭 시 테두리 점선 제거
        // btn.setBackground(Color.WHITE); // 버튼 배경색 (필요 시 변경)
        //btn.setFont(new Font("맑은 고딕", Font.BOLD, 12));
        
        // 여백 조정 (상, 좌, 하, 우)
        btn.setMargin(new Insets(5, 10, 5, 10));
    }
    
    // 맨 마지막 결과창에서 메인 화면으로 돌아갈때 데이터를 초기화 해주는 함수
    private void resetGameData() {
        // 1. 변수 초기화
        myCharType = -1;
        oppCharType = -1;

        // 2. 캐릭터 선택 화면 복구
        if (guideLabel != null) {
            guideLabel.setText("캐릭터를 선택하세요!");
        }
        if (charPanel != null) {
            // 모든 버튼 다시 활성화
            for (Component c : charPanel.getComponents()) {
                c.setEnabled(true);
            }
        }if (battleLog != null) {
            battleLog.setText("");
        }
        enableActionButtons(3);
    }

    // 3개 버튼을 비활성화 시키는 함수
    private void disableActionButtons() {
        atkBtn.setEnabled(false);
        skillBtn1.setEnabled(false);
        skillBtn2.setEnabled(false);
    }

    // 버튼 3개를 다시 활성화 시키는 함수
    private void enableActionButtons(int specialCount) {
        atkBtn.setEnabled(true);
        skillBtn1.setEnabled(specialCount > 0);
        skillBtn2.setEnabled(true);
    }


    public static void main(String[] args) {
        new GameClient();
    }
}