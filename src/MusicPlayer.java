import javax.sound.sampled.*;
import java.io.IOException;
import java.net.URL;

// 배경음악을 재생하고 멈추는 역할을 하는 클래스
public class MusicPlayer {
    private Clip clip;

    // 지정된 사운드 파일을 불러와서 무한 반복 재생
    public void playLoop(String path) {
        try {
            stop(); 

            URL soundURL = getClass().getResource(path);
            if (soundURL == null) {
                System.err.println("사운드 파일을 찾을 수 없습니다: " + path);
                return;
            }

            AudioInputStream audioIn = AudioSystem.getAudioInputStream(soundURL);
            clip = AudioSystem.getClip();
            clip.open(audioIn);
            clip.loop(Clip.LOOP_CONTINUOUSLY);

        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }

    // 현재 음악을 멈추고 초기화
    public void stop() {
        if (clip != null && clip.isRunning()) {
            clip.stop();
            clip.flush();
            clip.setFramePosition(0);
        }
    }
}
