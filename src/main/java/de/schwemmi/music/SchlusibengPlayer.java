package de.schwemmi.music;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.LineListener;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

@RestController
public class SchlusibengPlayer implements LineListener {

    /**
     * this flag indicates whether the playback completes or not.
     */
    AtomicBoolean playCompleted = new AtomicBoolean(true);

    private Clip audioClip;

    /**
     * Play a given audio file.
     * @param audioFilePath Path of the audio file.
     */
    @GetMapping("/play")
    public void play(@RequestParam("fileName") String audioFilePath) {
        File audioFile = new File(audioFilePath);

        try {

            this.playCompleted.set(false);
            new ProcessBuilder( "/bin/bash","-c","sudo systemctl stop bluealsa-aplay" ).start();
            new ProcessBuilder( "/bin/bash","-c","sudo systemctl stop shairport-sync" ).start();

            AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile);

            AudioFormat format = audioStream.getFormat();

            DataLine.Info info = new DataLine.Info(Clip.class, format);

            audioClip = (Clip) AudioSystem.getLine(info);



            audioClip.addLineListener(this);

            audioClip.open(audioStream);

            audioClip.start();

        } catch (UnsupportedAudioFileException ex) {
            System.out.println("The specified audio file is not supported.");
            ex.printStackTrace();
            audioClip.stop();
            this.playCompleted.set(true);
        } catch (LineUnavailableException ex) {
            System.out.println("Audio line for playing back is unavailable.");
            ex.printStackTrace();
            audioClip.stop();
            this.playCompleted.set(true);
        } catch (IOException ex) {
            System.out.println("Error playing the audio file.");
            ex.printStackTrace();
            audioClip.stop();
            this.playCompleted.set(true);
        }

    }

    /**
     * Listens to the START and STOP events of the audio line.
     */
    @Override
    public void update(LineEvent event) {
        LineEvent.Type type = event.getType();

        if (type == LineEvent.Type.START) {
            System.out.println("Playback started.");

        } else if (type == LineEvent.Type.STOP) {
            audioClip.close();
            try {
                new ProcessBuilder( "/bin/bash","-c","sudo systemctl start bluealsa-aplay" ).start();
                new ProcessBuilder( "/bin/bash","-c","sudo systemctl start shairport-sync" ).start();

                this.playCompleted.set(true);

            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Playback completed.");
        }

    }

    public boolean isPlayCompleted() {
        return this.playCompleted.get();
    }
}
