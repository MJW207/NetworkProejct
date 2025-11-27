public class GameCharacter {

    public final int type;   // 0: 전사, 1: 마법사, 2: 궁수
    public final int maxHp;
    public int currentHp;
    public int atk;
    public int def;
    public int speed;
    public boolean defending = false;

    public GameCharacter(int type) {
        this.type = type;

        switch (type) {
            case 0: // 전사: 체력형
                maxHp = 200; atk = 30; def = 10; speed = 10;
                break;
            case 1: // 마법사: 공격형
                maxHp = 150; atk = 50; def = 5; speed = 20;
                break;
            case 2: // 궁수: 속도형
                maxHp = 170; atk = 40; def = 8; speed = 30;
                break;
            default:
                maxHp = 150; atk = 30; def = 10; speed = 10;
        }
        currentHp = maxHp;
    }

    public int attackDamage() {
        int base = atk;
        int rand = (int) (Math.random() * 10);  // 0~9
        int dmg = base + rand;
        return Math.max(5, dmg);
    }

    public int healAmount() {
        return 20 + (int) (Math.random() * 10); // 20~29
    }

    public void applyDamage(int dmg) {
        if (defending) {
            dmg /= 2;
            defending = false;
        }
        currentHp -= dmg;
        if (currentHp < 0) currentHp = 0;
    }
}
