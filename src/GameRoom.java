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

    // 턴 정보 전송
    private void notifyTurn() {
        // 상대방의 고유 스킬 쿨다운 정보를 함께 전송하여 UI 갱신 가능하도록 추가
        String p1Status = getStatusString(c1);
        String p2Status = getStatusString(c2);

        if (currentTurn == p1) {
            // YOUR_TURN:나의스킬1:나의스킬2:상대의스킬1:상대의스킬2
            p1.send("YOUR_TURN:" + p1Status + ":" + p2Status);
            p2.send("OPP_TURN:" + p2Status + ":" + p1Status);
            System.out.println("[ROOM] 턴 = P1");
        } else {
            // OPP_TURN:상대의스킬1:상대의스킬2:나의스킬1:나의스킬2
            p2.send("YOUR_TURN:" + p2Status + ":" + p1Status);
            p1.send("OPP_TURN:" + p1Status + ":" + p2Status);
            System.out.println("[ROOM] 턴 = P2");
        }
    }

    // 캐릭터의 스킬 쿨다운 상태 문자열 반환 (쿨다운:쿨다운)
    private String getStatusString(GameCharacter c) {
        if (c.type == 0) return c.defUpCount + ":" + c.shieldCount; 
        if (c.type == 1) return c.meteorCount + ":" + c.debuffCount; 
        if (c.type == 2) return c.armorBreakCount + ":" + c.evasionCount;
        return "0:0";
    }


    private void handleAction(GameServer.ClientHandler from, String action) {
        if (from != currentTurn) {
            System.out.println("[ROOM] 잘못된 턴의 ACTION, 무시");
            from.send("LOG:당신의 턴이 아닙니다!");
            return;
        }

        GameCharacter me    = (from == p1) ? c1 : c2;
        GameCharacter enemy = (from == p1) ? c2 : c1;

        String log = null;

        // 방어/회피 상태 초기화 (전 턴에 사용했던 일반 방어)
        me.defending = false; 
        // 상대가 방패/회피 상태였다면 무효화 상태도 초기화
        enemy.shieldActive = false;
        enemy.evasionActive = false;

        switch (action) {
            case "ATTACK": // 기본 공격 (슬래쉬/파이어볼/활쏘기)
                int dmg = enemy.applyDamage(me.baseAttackDamage());
                log = "기본 공격! " + dmg + "의 피해를 입혔습니다.";
                break;
            case "DEFEND": // 일반 방어 (회복 기능 제거로 DEFEND만 남음)
                me.defending = true;
                log = "일반 방어 태세! 다음 공격 피해가 크게 감소합니다.";
                break;
            case "HEAL": // 회복 기능 제거 (클라이언트에서 버튼은 숨겨짐)
                log = "ERR:회복 기능은 제거되었습니다.";
                break;
            case "WAR_DEFUP": // 전사: 방어력 높이기 (방어력 +10)
                if (me.type != 0 || me.defUpCount <= 0) return;
                me.def += 10;
                me.defUpCount--;
                log = "방어력 높이기! 방어력이 영구적으로 10 상승했습니다. (남은 횟수: " + me.defUpCount + ")";
                break;
            case "WAR_SHIELD": // 전사: 방패 (1턴 공격 무효화)
                if (me.type != 0 || me.shieldCount <= 0) return;
                me.shieldActive = true;
                me.shieldCount--;
                log = "방패 발동! 다음 1턴 동안 상대 공격을 무효화합니다. (남은 횟수: " + me.shieldCount + ")";
                break;
            case "MAG_METEOR": // 마법사: 메테오 (강력 공격)
                if (me.type != 1 || me.meteorCount <= 0) return;
                int mDmg = enemy.applyDamage(me.meteorDamage());
                log = "메테오 발사! " + mDmg + "의 강력한 피해를 입혔습니다. (남은 횟수: " + me.meteorCount + ")";
                break;
            case "MAG_DEBUFF": // 마법사: 디버프 (상대 공/방 하락)
                if (me.type != 1 || me.debuffCount <= 0) return;
                enemy.atk = Math.max(5, enemy.atk - 5); // 공격력 5 하락 (최소 5 보장)
                enemy.def = Math.max(1, enemy.def - 3); // 방어력 3 하락 (최소 1 보장)
                me.debuffCount--;
                log = "디버프 성공! 상대의 공격력(-5)과 방어력(-3)이 하락했습니다. (남은 횟수: " + me.debuffCount + ")";
                break;
            case "ELF_ARMORBREAK": // 엘프: 갑옷뚫기 (공격 + 상대 방어 하락)
                if (me.type != 2 || me.armorBreakCount <= 0) return;
                int aDmg = enemy.applyDamage(me.armorBreakDamage());
                enemy.def = Math.max(1, enemy.def - 3); // 상대 방어력 3 하락 (최소 1 보장)
                me.armorBreakCount--;
                log = "갑옷 뚫기! " + aDmg + "의 피해와 함께 상대 방어력(-3)이 하락했습니다. (남은 횟수: " + me.armorBreakCount + ")";
                break;
            case "ELF_EVASION": // 엘프: 회피 (1턴 공격 회피)
                if (me.type != 2 || me.evasionCount <= 0) return;
                me.evasionActive = true;
                me.evasionCount--;
                log = "회피 발동! 다음 1턴 동안 상대 공격을 회피합니다. (남은 횟수: " + me.evasionCount + ")";
                break;
            default:
                from.send("LOG:유효하지 않은 행동입니다.");
                return;
        }

        // 피해 무효화 상태 로그 추가 (이전에 무효화된 공격이었다면)
        if (log.contains("공격!") || log.contains("메테오") || log.contains("갑옷 뚫기")) {
            if (enemy.shieldActive || enemy.evasionActive) {
                // applyDamage에서 무효화 처리가 되었지만, 로그는 직전에 작성된 것이므로 업데이트
                log = log.split("!")[0] + "! 하지만 상대의 방패/회피로 피해가 무효화되었습니다.";
            }
        }


        // p1 기준 / p2 기준 각각 UPDATE 전송
        // UPDATE:나의hp:상대hp:나의남은스킬1:나의남은스킬2:상대남은스킬1:상대남은스킬2:로그
        p1.send("UPDATE:" + c1.currentHp + ":" + c2.currentHp + 
               ":" + getStatusString(c1) + ":" + getStatusString(c2) + ":" + log);
        p2.send("UPDATE:" + c2.currentHp + ":" + c1.currentHp + 
               ":" + getStatusString(c2) + ":" + getStatusString(c1) + ":" + log);

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
        
        // ────────────────────────────
        // [수정] 게임 재시작을 위한 상태 초기화
        // ────────────────────────────
        p1.setRoom(null);
        p2.setRoom(null);
        p1.selectedCharType = -1; 
        p2.selectedCharType = -1;
    }
}	