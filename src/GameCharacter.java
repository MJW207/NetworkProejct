import java.util.Random;

public class GameCharacter {

    public final int type;   // 0: 전사(Warrior), 1: 마법사(Magician), 2: 엘프(Elf)
    public final int maxHp;
    public int currentHp;
    
    // 기본 스탯
    public int atk;
    public int def;
    public int speed;
    
    // 전투 중 일시적 상태
    public boolean defending = false; // 방어 (일반)
    public boolean shieldActive = false; // 방패 (전사 스킬)
    public boolean evasionActive = false; // 회피 (엘프 스킬)
    
    // 스킬 쿨다운 (남은 사용 횟수)
    public int defUpCount;      // 전사: 방어력 높이기 (3회)
    public int shieldCount;     // 전사: 방패 (2회)
    public int meteorCount;     // 마법사: 메테오 (2회)
    public int debuffCount;     // 마법사: 디버프 (3회)
    public int armorBreakCount; // 엘프: 갑옷뚫기 (3회)
    public int evasionCount;    // 엘프: 회피 (2회)
    
    // 디버프 상태 저장을 위한 기본 스탯
    public final int baseAtk; 
    public final int baseDef; 

    public GameCharacter(int type) {
        this.type = type;

        switch (type) {
            case 0: // 전사 (Warrior): 체력 300, 방어 15, 공격 35, 속공 10
                maxHp = 300; atk = 35; def = 15; speed = 10;
                defUpCount = 3; shieldCount = 2;
                break;
            case 1: // 마법사 (Magician): 체력 180, 방어 5, 공격 70, 속공 20
                maxHp = 180; atk = 70; def = 5; speed = 20;
                meteorCount = 2; debuffCount = 3;
                break;
            case 2: // 엘프 (Elf): 체력 220, 방어 8, 공격 50, 속공 30
                maxHp = 220; atk = 50; def = 8; speed = 30;
                armorBreakCount = 3; evasionCount = 2;
                break;
            default:
                maxHp = 150; atk = 30; def = 10; speed = 10;
                defUpCount = 0; shieldCount = 0; meteorCount = 0; debuffCount = 0; armorBreakCount = 0; evasionCount = 0;
        }
        currentHp = maxHp;
        this.baseAtk = atk;
        this.baseDef = def;
    }

    // 기본 공격 (ATK + 0~10 랜덤 데미지, 마법사는 0~5)
    public int baseAttackDamage() {
        int randRange = (type == 1) ? 5 : 10; // 마법사는 랜덤 범위 작게
        int base = atk;
        int rand = new Random().nextInt(randRange + 1); // 0부터 randRange까지
        int dmg = base + rand;
        return Math.max(1, dmg);
    }
    
    // 메테오 공격 (마법사 스킬, ATK * 1.5)
    public int meteorDamage() {
        if (meteorCount > 0) {
            meteorCount--;
            int base = (int)(atk * 1.5);
            int rand = new Random().nextInt(21); // 0~20 랜덤 추가 피해
            return base + rand;
        }
        return 0;
    }
    
    // 갑옷뚫기 공격 (엘프 스킬, ATK * 0.8)
    public int armorBreakDamage() {
        if (armorBreakCount > 0) {
            armorBreakCount--;
            int base = (int)(atk * 0.8);
            int rand = new Random().nextInt(11); // 0~10 랜덤 추가 피해
            return base + rand;
        }
        return 0;
    }

    // 회복 기능은 완전히 제거됨 (baseHealAmount 삭제)

    // 데미지 적용
    public int applyDamage(int rawDmg) {
        if (shieldActive || evasionActive) {
            // 방패(전사) 또는 회피(엘프)로 무효화
            shieldActive = false;
            evasionActive = false;
            return 0; // 무효화된 데미지
        }
        
        int finalDmg = rawDmg;
        
        // 일반 방어: 방어력의 1.5배만큼 데미지 감소
        if (defending) {
            finalDmg -= (int)(def * 1.5);
            defending = false;
        } else {
            // 일반 피격: 방어력만큼 데미지 감소
            finalDmg -= def;
        }
        
        finalDmg = Math.max(1, finalDmg); // 최소 1 데미지 보장

        currentHp -= finalDmg;
        if (currentHp < 0) currentHp = 0;
        
        return finalDmg; // 실제 입은 데미지
    }
}