import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {
	
	// 서버 포트 설정
    private static final int PORT = 5000;
    // 접속한 모든 클라이언트 목록
    private static final List<ClientHandler> allClients =
            Collections.synchronizedList(new ArrayList<>());
    // 매칭 대기 중인 클라이언트 목록
    private static final List<ClientHandler> waiting =
            Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println(">>> Battle Arena Online Server Started on Port " + PORT);
        // 서버 소켓 생성 및 클라이언트 접속 대기
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
            	// 클라이언트 접속시 소켓 수락
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] New client: " + socket.getInetAddress());
                
                // 클라이언트 핸들러 스레드 생성 및 실행
                ClientHandler handler = new ClientHandler(socket);
                allClients.add(handler);
                handler.start();

                broadcastUserCount();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 유저 수를 전체 클라이언트에 브로드캐스트
    public static synchronized void broadcastUserCount() {
        int size = allClients.size();
        for (ClientHandler c : allClients) {
            c.send("USER_COUNT:" + size);
        }
    }

    // 매칭 큐에 클라이너트를 추가하고 2명 이상이면 매칭 시작
    public static synchronized void addToWaiting(ClientHandler c) {
        if (!waiting.contains(c)) {
            waiting.add(c);
        }
        System.out.println("[SERVER] Waiting size = " + waiting.size());
        // 두명 이상이라면 게임방 생성(매칭 시작)
        if (waiting.size() >= 2) {
            ClientHandler p1 = waiting.remove(0);
            ClientHandler p2 = waiting.remove(0);

            GameRoom room = new GameRoom(p1, p2);
            p1.setRoom(room);
            p2.setRoom(room);

            room.startMatch();
        }
    }

    // 클라이언트 연결 및 메세지 처리 클래스
    public static class ClientHandler extends Thread {
        private final Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        private GameRoom room;
        public int selectedCharType = -1;

        private static int ID_SEQ = 1;
        private final int id;

        public ClientHandler(Socket socket) {
            this.socket = socket;
            this.id = ID_SEQ++;

            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // 클라이언트 식별 문자열
        public String getIdStr() {
            return "P" + id;
        }
        // 클라이언트를 게임룸에 할당
        public void setRoom(GameRoom room) {
            this.room = room;
        }
        // 클라이언트에게 메세지 전송
        public void send(String msg) {
            out.println(msg);
        }

        // 클라이언트의 메세지 수신 및 처리 루프
        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("[SERVER] " + getIdStr() + " -> " + msg);
                    // 매칭 요청 메세지 처리
                    if ("REQUEST_MATCH".equals(msg)) {
                        GameServer.addToWaiting(this);
                     // 게임 룸에 있는 경우 해당 메세지를 룸에 전달
                    } else if (room != null) {
                        room.handleMessage(this, msg);
                    } else {
                        System.out.println("[SERVER] room == null 상태에서 받은 메시지: " + msg);
                    }
                }
            } catch (IOException e) {
                System.out.println("[SERVER] Client disconnected: " + getIdStr());
            } finally {
            	// 연결 종료 시 클라이언트 목록에서 제거, 유저 수 갱신
                allClients.remove(this);
                broadcastUserCount();
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
