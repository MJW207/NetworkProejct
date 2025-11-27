public class GameRoom {

    private final GameServer.ClientHandler p1;
    private final GameServer.ClientHandler p2;

    private GameCharacter c1;
    private GameCharacter c2;

    private boolean started = false;
    private GameServer.ClientHandler currentTurn;

    public GameRoom(GameServer.ClientHandler p1, GameServer.ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
    }

    // 매칭 성공 후 바로 호출
    public void startMatch() {
        System.out.println("[ROOM] 매칭 완료, 캐릭터 선택 대기");
        p1.send("MATCH_SUCCESS");
        p2.send("MATCH_SUCCESS");
    }

    // ClientHandler에서 모든 메시지가 여기로 들어온다.
    public synchronized void handleMessage(GameServer.ClientHandler from, String msg) {
        System.out.println("[ROOM] handleMessage from " + from.getIdStr() + " : " + msg);

        if (msg.startsWith("SELECT:")) {
            handleSelect(from, msg);
        } else if (msg.startsWith("ACTION:")) {
            if (!started) return;
            handleAction(from, msg.substring("ACTION:".length()));
        }
    }

    private void handleSelect(GameServer.ClientHandler from, String msg) {
        int type = Integer.parseInt(msg.split(":")[1]);
        from.selectedCharType = type;
        System.out.println("[ROOM] " + from.getIdStr() + " 캐릭터 선택 = " + type);

        if (p1.selectedCharType != -1 && p2.selectedCharType != -1 && !started) {
            startGame();
        }
    }

    private void startGame() {
        started = true;

        c1 = new GameCharacter(p1.selectedCharType);
        c2 = new GameCharacter(p2.selectedCharType);

        System.out.println("[ROOM] 게임 시작! P1 type=" + c1.type + ", P2 type=" + c2.type);

        // 클라이언트 프로토콜: GAME_START:상대캐릭터타입:상대maxHp
        p1.send("GAME_START:" + c2.type + ":" + c2.maxHp);
        p2.send("GAME_START:" + c1.type + ":" + c1.maxHp);

        // 선턴 결정 (속도 기준)
        currentTurn = (c1.speed >= c2.speed) ? p1 : p2;
        notifyTurn();
    }

    private void notifyTurn() {
        if (currentTurn == p1) {
            p1.send("YOUR_TURN");
            p2.send("OPP_TURN");
            System.out.println("[ROOM] 턴 = P1");
        } else {
            p2.send("YOUR_TURN");
            p1.send("OPP_TURN");
            System.out.println("[ROOM] 턴 = P2");
        }
    }

    private void handleAction(GameServer.ClientHandler from, String action) {
        if (from != currentTurn) {
            System.out.println("[ROOM] 잘못된 턴의 ACTION, 무시");
            return;
        }

        GameCharacter me   = (from == p1) ? c1 : c2;
        GameCharacter enemy= (from == p1) ? c2 : c1;

        String log;

        switch (action) {
            case "ATTACK":
                int dmg = me.attackDamage();
                enemy.applyDamage(dmg);
                log = "공격! " + dmg + "의 피해를 입혔습니다.";
                break;
            case "DEFEND":
                me.defending = true;
                log = "방어 태세! 다음 공격 피해가 감소합니다.";
                break;
            case "HEAL":
                int heal = me.healAmount();
                me.currentHp = Math.min(me.maxHp, me.currentHp + heal);
                log = "회복! 체력이 " + heal + "만큼 회복되었습니다.";
                break;
            default:
                return;
        }

        // p1 기준 / p2 기준 각각 UPDATE 전송
        p1.send("UPDATE:" + c1.currentHp + ":" + c2.currentHp + ":" + log);
        p2.send("UPDATE:" + c2.currentHp + ":" + c1.currentHp + ":" + log);

        if (c1.currentHp <= 0 || c2.currentHp <= 0) {
            endGame();
            return;
        }

        // 턴 교대
        currentTurn = (currentTurn == p1) ? p2 : p1;
        notifyTurn();
    }

    private void endGame() {
        String r1 = (c1.currentHp > 0) ? "WIN" : "LOSE";
        String r2 = (c2.currentHp > 0) ? "WIN" : "LOSE";

        p1.send("RESULT:" + r1);
        p2.send("RESULT:" + r2);

        System.out.println("[ROOM] 게임 종료: P1=" + r1 + ", P2=" + r2);
    }
}
