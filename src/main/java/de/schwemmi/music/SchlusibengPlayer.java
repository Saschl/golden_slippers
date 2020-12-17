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

    private Clip audioClip;
    private AtomicBoolean isRestart = new AtomicBoolean(false);
    @Value("${client.slippers:3}")
    private Integer slipperVariants;


    public SchlusibengPlayer() {
        this.slipperRandom = new Random();
    }


    /**
     * Play a given audio file.
     *
     * @param audioFilePath Path of the audio file.
     */
    @GetMapping("/play")
    public void play(@RequestParam("fileName") String audioFilePath) {
        if (audioFilePath == null || audioFilePath.isEmpty()) {
            int nextSlipper = slipperRandom.nextInt(slipperVariants);
            audioFilePath = "/home/pi/slippers_" + nextSlipper + ".wav";
        }
        File audioFile = new File(audioFilePath);

        try {

            this.playCompleted.set(false);
            new ProcessBuilder("/bin/bash", "-c", "sudo systemctl stop bluealsa-aplay").start();
            new ProcessBuilder("/bin/bash", "-c", "sudo systemctl stop shairport-sync").start();

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);
            AudioFormat format = audioStream.getFormat();
            DataLine.Info info = new DataLine.Info(Clip.class, format);

            audioClip = (Clip) AudioSystem.getLine(info);
            audioClip.addLineListener(this);
            audioClip.open(audioStream);
            audioClip.start();

        } catch (UnsupportedAudioFileException ex) {
            LOG.error("The specified audio file is not supported.",ex);
            ex.printStackTrace();
            audioClip.stop();
            this.playCompleted.compareAndSet(false, true);
        } catch (LineUnavailableException ex) {
            LOG.error("Audio line for playing back is unavailable.",ex);
            ex.printStackTrace();
            audioClip.stop();
            this.playCompleted.compareAndSet(false, true);
        } catch (IOException ex) {
            LOG.error("Error playing the audio file.", ex);
            audioClip.stop();
            this.playCompleted.compareAndSet(false, true);
        }

    }

    @GetMapping("/restart")
    public void stop() {
        audioClip.stop();
        this.isRestart.compareAndSet(false, true);
    }

    @PreDestroy
    public void turnOnAudioAgain() {
        try {
            new ProcessBuilder("/bin/bash", "-c", "sudo systemctl start bluealsa-aplay").start();
            new ProcessBuilder("/bin/bash", "-c", "sudo systemctl start shairport-sync").start();
        } catch (IOException ex) {
            LOG.error("Could no restart audio services", ex);
        }

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
            try {
                new ProcessBuilder("/bin/bash", "-c", "sudo systemctl start bluealsa-aplay").start();
                new ProcessBuilder("/bin/bash", "-c", "sudo systemctl start shairport-sync").start();

                this.playCompleted.compareAndSet(false, true);
                if (this.isRestart.compareAndSet(true, false)) {
                    play("");
                }

            } catch (IOException ex) {
                LOG.error("Could no restart audio services", ex);
            }

            LOG.info("Playback completed.");
        }

    }

    public boolean isPlayCompleted() {
        return this.playCompleted.get();
    }
}
