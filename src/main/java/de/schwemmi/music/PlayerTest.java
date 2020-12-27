package de.schwemmi.music;


import org.junit.jupiter.api.Test;

public class PlayerTest {
    private AlarmPlayer player = new AlarmPlayer(new SchlusibengPlayer());

    @Test
    public void testPlay() {

        player.playAlarmAndThenSong();
       while(true) {
           try {
               Thread.sleep(100);
           } catch (InterruptedException e) {
               e.printStackTrace();
           }
       }
    }
}
