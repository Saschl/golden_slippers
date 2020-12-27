package de.schwemmi.music;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PreDestroy;
import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class SchlusibengPlayer implements LineListener {

    private static final Logger LOG = LoggerFactory.getLogger(SchlusibengPlayer.class);

    /**
     * this flag indicates whether the playback completes or not.
     */
    AtomicBoolean playCompleted = new AtomicBoolean(true);

    private final Random slipperRandom;
    private LocalDateTime lastPlay = null;

    private Clip audioClip;
    private AtomicBoolean isRestart = new AtomicBoolean(false);
    @Value("${client.slippers:3}")
    private Integer slipperVariants=3;

    private final String[] slippersFiles = new String[40];

    public static final String BASE_PATH = "C:/Users/sasch/";


    public SchlusibengPlayer() {
        this.slipperRandom = new Random();
        slippersFiles[0] = "slippers_2.wav";
        slippersFiles[1] = "slippers_2.wav";
        slippersFiles[2] = "slippers_2.wav";
        for(int i = 3; i < 20; i++) {
            slippersFiles[i] = "slippers_0.wav";
        }
        for(int i = 20; i < 37; i++) {
            slippersFiles[i] = "slippers_1.wav";
        }
        for(int i = 37; i < 40; i++) {
            slippersFiles[i] = "slippers_3.wav";
        }
    }

    public synchronized void markSongStart() {
        this.playCompleted.set(false);
    }

    public synchronized void markSongEnd() {
        this.playCompleted.compareAndSet(false, true);
        this.lastPlay = LocalDateTime.now();
    }
    public boolean isTimeOutElapsed() {
        return this.lastPlay == null|| this.lastPlay.plus(10, ChronoUnit.MINUTES).isBefore(LocalDateTime.now());
    }

    private void stopAudioServices() {
        // new ProcessBuilder("/bin/bash", "-c", "sudo systemctl stop bluealsa-aplay").start();
        // new ProcessBuilder("/bin/bash", "-c", "sudo systemctl stop shairport-sync").start();
    }

    private void startAudioServices() {
      //  new ProcessBuilder("/bin/bash", "-c", "sudo systemctl start bluealsa-aplay").start();
       // new ProcessBuilder("/bin/bash", "-c", "sudo systemctl start shairport-sync").start();

    }

    /**
     * Play a given audio file.
     *
     * @param audioFilePath Path of the audio file.
     */
    @GetMapping("/play")
    public void play(@RequestParam("fileName") String audioFilePath)  {
        if(isTimeOutElapsed()) {
            if (audioFilePath == null || audioFilePath.isEmpty()) {
                int nextSlipper = slipperRandom.nextInt(slipperVariants*10);
                String nextSlipperFile = slippersFiles[nextSlipper];

                audioFilePath = BASE_PATH+nextSlipperFile;
            }
            File audioFile = new File(audioFilePath);

            try {

                markSongStart();

                stopAudioServices();



                // actual song
                AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
                AudioFormat format = audioStream.getFormat();
                DataLine.Info info = new DataLine.Info(Clip.class, format);

                audioClip = (Clip) AudioSystem.getLine(info);
                audioClip.addLineListener(this);

                audioClip.open(audioStream);
                audioClip.start();

            } catch (UnsupportedAudioFileException ex) {
                LOG.error("The specified audio file is not supported.", ex);
                ex.printStackTrace();
                audioClip.stop();
                this.playCompleted.compareAndSet(false, true);
            } catch (LineUnavailableException ex) {
                LOG.error("Audio line for playing back is unavailable.", ex);
                ex.printStackTrace();
                audioClip.stop();
                this.playCompleted.compareAndSet(false, true);
            } catch (IOException ex) {
                LOG.error("Error playing the audio file.", ex);
                audioClip.stop();
                this.playCompleted.compareAndSet(false, true);
            }
        } else {
            startAudioServices();
        }

    }

    @GetMapping("/restart")
    public void stop() {
        audioClip.stop();
        //this.isRestart.compareAndSet(false, true);
    }

    @PreDestroy
    public void turnOnAudioAgain() {
       startAudioServices();
    }


    /**
     * Listens to the START and STOP events of the audio line.
     */
    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();

        if (type == LineEvent.Type.START) {
            LOG.info("Playback started.");

        } else if (type == LineEvent.Type.STOP) {
            audioClip.close();

               startAudioServices();


                markSongEnd();


            LOG.info("Playback completed.");
        }

    }

    public boolean isPlayCompleted() {
        return this.playCompleted.get();
    }
}
