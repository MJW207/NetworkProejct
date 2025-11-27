import java.io.*;
import java.net.*;
import java.util.*;

public class GameServer {

    private static final int PORT = 5000;

    private static final List<ClientHandler> allClients =
            Collections.synchronizedList(new ArrayList<>());

    private static final List<ClientHandler> waiting =
            Collections.synchronizedList(new ArrayList<>());

    public static void main(String[] args) {
        System.out.println(">>> Battle Arena Online Server Started on Port " + PORT);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("[SERVER] New client: " + socket.getInetAddress());

                ClientHandler handler = new ClientHandler(socket);
                allClients.add(handler);
                handler.start();

                broadcastUserCount();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 유저 수 브로드캐스트
    public static synchronized void broadcastUserCount() {
        int size = allClients.size();
        for (ClientHandler c : allClients) {
            c.send("USER_COUNT:" + size);
        }
    }

    // 매칭 큐에 추가
    public static synchronized void addToWaiting(ClientHandler c) {
        if (!waiting.contains(c)) {
            waiting.add(c);
        }
        System.out.println("[SERVER] Waiting size = " + waiting.size());

        if (waiting.size() >= 2) {
            ClientHandler p1 = waiting.remove(0);
            ClientHandler p2 = waiting.remove(0);

            GameRoom room = new GameRoom(p1, p2);
            p1.setRoom(room);
            p2.setRoom(room);

            room.startMatch();
        }
    }

    // ───────────────────────
    // ClientHandler
    // ───────────────────────
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

        public String getIdStr() {
            return "P" + id;
        }

        public void setRoom(GameRoom room) {
            this.room = room;
        }

        public void send(String msg) {
            out.println(msg);
        }

        @Override
        public void run() {
            try {
                String msg;
                while ((msg = in.readLine()) != null) {
                    System.out.println("[SERVER] " + getIdStr() + " -> " + msg);

                    if ("REQUEST_MATCH".equals(msg)) {
                        GameServer.addToWaiting(this);
                    } else if (room != null) {
                        room.handleMessage(this, msg);
                    } else {
                        System.out.println("[SERVER] room == null 상태에서 받은 메시지: " + msg);
                    }
                }
            } catch (IOException e) {
                System.out.println("[SERVER] Client disconnected: " + getIdStr());
            } finally {
                allClients.remove(this);
                broadcastUserCount();
                try { socket.close(); } catch (IOException ignored) {}
            }
        }
    }
}
