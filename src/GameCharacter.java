import java.util.Random;

public class GameCharacter {

    public final int type;
    public final int maxHp;
    public int currentHp;
    
    // 기본 스탯
    public int atk;
    public int def;
    public int speed;
    
    // 상태 변수
    public boolean defending = false; // 방어 태세 여부
    public int specialCount;          // 특수 공격 남은 횟수 (공통)

    public GameCharacter(int type) {
        this.type = type;
        
        // 캐릭터별 스탯 차이만 남기고 나머지는 통일
        switch (type) {
            case 0: // 전사: 체력 높음
                maxHp = 200; atk = 15; def = 15; speed = 10;
                break;
            case 1: // 마법사: 공격력 높음
                maxHp = 150; atk = 25; def = 5; speed = 20;
                break;
            case 2: // 엘프: 속도 빠름
                maxHp = 170; atk = 18; def = 8; speed = 30;
                break;
            default:
                maxHp = 100; atk = 10; def = 10; speed = 10;
        }
        currentHp = maxHp;
        specialCount = 3; // 모든 캐릭터 특수공격 3회로 통일
    }

    // [변경] 기본 공격: ATK + (1~10 랜덤)
    public int baseAttackDamage() {
        int randomDmg = new Random().nextInt(atk) + 1; // 1~10
        return atk + randomDmg;
    }
    
    // [신규] 특수 공격: (ATK * 1.6) + (1 ~ ATK*1.6 범위의 랜덤)
    public int specialAttackDamage() {
        if (specialCount <= 0) return 0;
        
        specialCount--;
        int base = (int)(atk * 1.6);
        int randomDmg = new Random().nextInt(base) + 1; // 1 ~ base
        return base + randomDmg;
    }

    // [변경] 데미지 적용 로직
    public int applyDamage(int rawDmg) {
        int finalDef = def;

        // 방어 태세일 경우 방어력을 일시적으로 2배 적용 (혹은 데미지 반감 등 룰 설정)
        // 요청하신 "상대 공격 - 내 방어" 논리를 강화하기 위해 방어 태세 시 방어력 증가로 구현
        if (defending) {
            finalDef = def * 2; 
            defending = false; // 방어는 1회 피격 후 해제
        }

        int finalDmg = rawDmg - finalDef;
        if (finalDmg < 1) finalDmg = 1; // 최소 데미지 1 보장

        currentHp -= finalDmg;
        if (currentHp < 0) currentHp = 0;
        
        return finalDmg;
    }
}