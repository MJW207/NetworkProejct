import java.util.Random;

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
            if (!started) {
                 from.send("LOG:게임 시작 전입니다.");
                 return;
            }
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

        // 클라이언트 프로토콜: GAME_START:상대캐릭터타입:상대maxHp:나의maxHp
        p1.send("GAME_START:" + c2.type + ":" + c2.maxHp + ":" + c1.maxHp);
        p2.send("GAME_START:" + c1.type + ":" + c1.maxHp + ":" + c2.maxHp);

        // 선턴 결정 (속도 기준: 엘프(30) > 마법사(20) > 전사(10))
        if (c1.speed != c2.speed) {
            currentTurn = (c1.speed > c2.speed) ? p1 : p2;
        } else {
            currentTurn = (new Random().nextBoolean()) ? p1 : p2;
        }

        notifyTurn();
    }

 // [수정] 턴 정보 전송 (특수 공격 횟수만 교환)
    private void notifyTurn() {
        // getStatusString이 이제 단순 숫자("3")만 반환함
        String p1Status = getStatusString(c1);
        String p2Status = getStatusString(c2);

        if (currentTurn == p1) {
            // 프로토콜 변경: YOUR_TURN:나의특공횟수:상대특공횟수
            // 예: "YOUR_TURN:3:2"
            p1.send("YOUR_TURN:" + p1Status + ":" + p2Status);
            p2.send("OPP_TURN:" + p2Status + ":" + p1Status);
            System.out.println("[ROOM] 턴 = P1");
        } else {
            // 예: "YOUR_TURN:2:3"
            p2.send("YOUR_TURN:" + p2Status + ":" + p1Status);
            p1.send("OPP_TURN:" + p1Status + ":" + p2Status);
            System.out.println("[ROOM] 턴 = P2");
        }
    }

    // 캐릭터의 스킬 쿨다운 상태 문자열 반환 (쿨다운:쿨다운)
    private String getStatusString(GameCharacter c) {
        // 예: "3" (단순 정수 문자열)
        return String.valueOf(c.specialCount);
    }

    private void handleAction(GameServer.ClientHandler from, String action) {
        if (from != currentTurn) {
            from.send("LOG:당신의 턴이 아닙니다!");
            return;
        }

        GameCharacter me    = (from == p1) ? c1 : c2;
        GameCharacter enemy = (from == p1) ? c2 : c1;

        String log = null;
        
        // 턴 시작 시 내 방어 태세는 유지하되(이번 턴에 맞을 때까지), 
        // 행동을 하면 "새로운 행동"을 하는 것이므로 기존 방어 상태 처리 로직이 필요할 수 있음.
        // 여기서는 단순화하여 공격 시 방어 태세가 아니게 됨을 가정하지 않고,
        // GameCharacter.applyDamage에서 맞으면 풀리도록 설계했습니다.

        switch (action) {
            case "ATTACK": // 기본 공격
                int dmg = enemy.applyDamage(me.baseAttackDamage());
                log = "기본 공격! " + dmg + "의 피해를 입혔습니다.";
                break;

            case "SPECIAL": // 특수 공격 (통일됨)
                if (me.specialCount > 0) {
                    int sDmg = enemy.applyDamage(me.specialAttackDamage());
                    log = "특수 공격 발동! " + sDmg + "의 강력한 피해!";
                } else {
                    log = "특수 공격 횟수가 소진되었습니다. (기본 공격 실패 처리)";
                }
                break;

            case "DEFEND": // 방어 (통일됨)
            	me.defending = true;
                log = "방어 태세! 방어력이 일시적으로 상승합니다.";
                break;

            default:
                from.send("LOG:알 수 없는 행동입니다.");
                return;
        }

        // 상태 업데이트 전송 (스킬 쿨다운은 이제 specialCount 하나만 보냄)
        // 기존: getStatusString 메서드도 수정 필요 (아래 참조)
        p1.send("UPDATE:" + c1.currentHp + ":" + c2.currentHp + 
               ":" + c1.specialCount + ":" + c2.specialCount + ":" + log);
        p2.send("UPDATE:" + c2.currentHp + ":" + c1.currentHp + 
               ":" + c2.specialCount + ":" + c1.specialCount + ":" + log);

        //게임 종료 확인
        if (c1.currentHp <= 0 || c2.currentHp <= 0) {
            endGame();
            return;
        }

        currentTurn = (currentTurn == p1) ? p2 : p1;
        notifyTurn();
    }

    private void endGame() {
        String r1 = (c1.currentHp > 0) ? "WIN" : "LOSE";
        String r2 = (c2.currentHp > 0) ? "WIN" : "LOSE";

        p1.send("RESULT:" + r1);
        p2.send("RESULT:" + r2);

        System.out.println("[ROOM] 게임 종료: P1=" + r1 + ", P2=" + r2);
        
        // ────────────────────────────
        // [수정] 게임 재시작을 위한 상태 초기화
        // ────────────────────────────
        p1.setRoom(null);
        p2.setRoom(null);
        p1.selectedCharType = -1; 
        p2.selectedCharType = -1;
    }
}	