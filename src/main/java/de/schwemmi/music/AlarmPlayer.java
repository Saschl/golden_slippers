package de.schwemmi.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;

import static de.schwemmi.music.SchlusibengPlayer.BASE_PATH;

@RestController
public class AlarmPlayer implements LineListener {

    private Clip audioClip;
    private final SchlusibengPlayer schlusibengPlayer;

    public AlarmPlayer(SchlusibengPlayer schlusibengPlayer) {
        this.schlusibengPlayer = schlusibengPlayer;
    }

    public void playAlarmAndThenSong() {
        if(schlusibengPlayer.isTimeOutElapsed()) {

            //alarm
            File alarmFile = new File(BASE_PATH+"alarm.wav");
            AudioInputStream alarmInputStream = null;
            try {
                alarmInputStream = AudioSystem.getAudioInputStream(alarmFile);
                AudioFormat alarmFormat = alarmInputStream.getFormat();
                DataLine.Info alarmInfo = new DataLine.Info(Clip.class, alarmFormat);
                audioClip = (Clip) AudioSystem.getLine(alarmInfo);
                audioClip.addLineListener(this);

                audioClip.open(alarmInputStream);
                audioClip.start();
            } catch (UnsupportedAudioFileException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (LineUnavailableException e) {
                e.printStackTrace();
            }
        }

    }

    @GetMapping("withAlarm")
    public void startWithAlarm() {

        this.playAlarmAndThenSong();
    }


    private static Logger LOG = LoggerFactory.getLogger(AlarmPlayer.class);
    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();

        if (type == LineEvent.Type.START) {
            LOG.info("Playback started.");

        } else if (type == LineEvent.Type.STOP) {
            audioClip.close();
            this.schlusibengPlayer.play("");

            LOG.info("Playback completed.");
        }

    }
}
