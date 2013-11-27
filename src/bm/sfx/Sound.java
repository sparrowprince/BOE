package bm.sfx;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;

public class Sound {

    private AudioInputStream ais;
    private AudioFormat af;
    private int size;
    private byte[] soundBytes;

    public Sound(String fn) throws UnsupportedAudioFileException, IOException {
        ais = AudioSystem.getAudioInputStream(getClass().getResource(fn));
        af = ais.getFormat();
        size = (int) (af.getFrameSize() * ais.getFrameLength());
        soundBytes = new byte[size];
        int read = 0;
        while ((read += ais.read(soundBytes, read, size)) < size)
            ;

    }

    public int read(byte[] b, int off, int len) {
        int read = 0;
        for (int i = off; i < size && i < (off + len); i++) {
            b[i - off] = soundBytes[i];
            read++;
        }
        return read;
    }

}
