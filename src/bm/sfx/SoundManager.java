package bm.sfx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

public class SoundManager implements Runnable {

    public static final String[] soundURLs = { "/music.wav", "/expl.wav",
            "/powup.wav", "/die.wav", "/quaddamage.wav", "/damage3.wav" };

    private Clip[][] sounds;

    private Sound[] testSounds;
    private SoundTuple music;

    private List<SoundTuple> playingSounds;

    private SourceDataLine outputLine;

    private float sampleRate;
    private int sampleSize;

    byte[] buffer;
    byte[] emptyBuff;

    private float volume = 0.5f;

    public SoundManager() {
        playingSounds = new ArrayList<SoundTuple>();
        AudioInputStream audioInputStream;
        AudioFormat aufo = null;
        try {
            audioInputStream = AudioSystem.getAudioInputStream(getClass()
                    .getResource(soundURLs[1]));
            aufo = audioInputStream.getFormat();
            sampleRate = aufo.getSampleRate();
            sampleSize = (int) (sampleRate / TIMESPERSEC);
            buffer = new byte[sampleSize * 4];
            emptyBuff = new byte[sampleSize * 4];
            for (int i = 0; i < emptyBuff.length; i++)
                emptyBuff[i] = 0;
            System.out.println(aufo.getSampleSizeInBits());
            System.out.println(aufo.getChannels());
            System.out.println(aufo.isBigEndian());
        } catch (UnsupportedAudioFileException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        } catch (IOException e2) {
            // TODO Auto-generated catch block
            e2.printStackTrace();
        }

        // AudioFormat aufo = new AudioFormat(44100, 16, 1, true, false);
        try {
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, aufo);
            // outputLine = AudioSystem.getSourceDataLine(aufo);
            outputLine = (SourceDataLine) AudioSystem.getLine(info);
            // outputLine.open(aufo, (2 << 13));
            outputLine.open(aufo, sampleSize * 2);
            // outputLine.open();
            outputLine.start();
        } catch (LineUnavailableException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        // clipCount = new short[soundURLs.length];
        // sounds = new Clip[soundURLs.length][COPIES];
        testSounds = new Sound[soundURLs.length];
        for (int i = 0; i < soundURLs.length; i++) {
            // for (int j = 0; j < COPIES; j++) {
            try {
                // AudioInputStream audioInputStream = AudioSystem
                // .getAudioInputStream(getClass().getResource(
                // soundURLs[i]));
                testSounds[i] = new Sound(soundURLs[i]);
                // BufferedInputStream bufferedInputStream = new
                // BufferedInputStream(
                // audioInputStream);
                // AudioFormat af = audioInputStream.getFormat();
                // int size = (int) (af.getFrameSize() * audioInputStream
                // .getFrameLength());
                // byte[] audio = new byte[size];
                // DataLine.Info info = new DataLine.Info(Clip.class, af, size);
                // bufferedInputStream.read(audio, 0, size);
                // Clip clip = (Clip) AudioSystem.getLine(info);
                // clip.open(af, audio, 0, size);
                // sounds[i][j] = clip;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // }
        music = new SoundTuple(0, 0);
        new Thread(this).start();
    }

    /**
     * Plays a sound referenced by a path stored in the static soundURLs array.
     * One may search through this array to find a desired clip, but should
     * usually know the position.
     * 
     * @param i: Position of path in soundURLs and thus implicitly the sound to
     * play.
     */
    public void playSound(final int i) {
        playingSounds.add(new SoundTuple(i, 0));
    }

    public void decreaseVolume() {
        volume -= 0.01f;
        if (volume < 0)
            volume = 0;
    }

    public void increaseVolume() {
        volume += 0.01f;
        if (volume > 1)
            volume = 1;
    }

    // }.start();
    // }

    private class SoundTuple {
        int sound;
        int read;

        public SoundTuple(int sound, int read) {
            this.sound = sound;
            this.read = read;
        }
    }

    // public void flushSound(int samples) {
    public void flushSound(int samples) {
        sampleSize = (int) Math.min((sampleRate / samples), buffer.length / 2);
        int soundsToMix = 0;
        int sound, read, rd = 0;
        int maxRead = 0;
        byte[] rdBuff = new byte[2];
        int currSound;
        short amplitude;
        int pos = -1;
        for (int j = 0; j < sampleSize; j++) {
            soundsToMix = 0;
            currSound = 0;
            if (music != null) {
                rd = testSounds[music.sound].read(rdBuff, music.read, 2);
                if (rd <= 0) {
                    music.read = 0;
                    rd = testSounds[music.sound].read(rdBuff, music.read, 2);
                } else music.read += rd;
                soundsToMix++;
                amplitude = (short) (((rdBuff[1] & 0xFF) << 8) | rdBuff[0] & 0xFF);
                currSound += amplitude * volume;
            }
            for (int i = 0; i < playingSounds.size(); i++) {
                if (playingSounds.get(i) != null) {
                    sound = playingSounds.get(i).sound;
                    read = playingSounds.get(i).read;
                    rd = testSounds[sound].read(rdBuff, read, 2);
                    if (rd <= 0)
                        playingSounds.remove(i);
                    else {
                        playingSounds.get(i).read += 2;
                        soundsToMix++;

                        // left = rdBuff[1] & 0xFF;
                        // right = rdBuff[0] & 0xFF;

                        amplitude = (short) (((rdBuff[1] & 0xFF) << 8) | rdBuff[0] & 0xFF);

                        currSound += amplitude;
                    }
                }
            }
            if (soundsToMix <= 0)
                break;
            currSound = (int) (currSound * volume);
            if (currSound > Short.MAX_VALUE)
                currSound = Short.MAX_VALUE;
            else if (currSound < Short.MIN_VALUE)
                currSound = Short.MIN_VALUE;
            buffer[++pos] = (byte) (currSound & 0xFF);
            buffer[++pos] = (byte) ((currSound >> 8) & 0xFF);
            maxRead += rd;
        }
        outputLine.write(buffer, 0, maxRead);
        outputLine.write(emptyBuff, maxRead, sampleSize * 2 - maxRead);
    }

    public void repeatSound(int i) {
        sounds[i][0].loop(-1);
    }

    public void stopSound(int i) {
        sounds[i][0].stop();
    }

    public static final SoundManager staticInstance = new SoundManager();

    @Override
    public void run() {
        long beforeTime = System.nanoTime();
        long passedTime = FLUSHTIME;
        while (true) {
            if (passedTime >= FLUSHTIME) {
                beforeTime = System.nanoTime();
                flushSound((int) (SECOND / passedTime));
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            passedTime = System.nanoTime() - beforeTime;
        }

    }

    private static final long SECOND = 1000000000;
    private static final int TIMESPERSEC = 20;
    private static final long FLUSHTIME = SECOND / TIMESPERSEC;

}// end of class SoundManager

