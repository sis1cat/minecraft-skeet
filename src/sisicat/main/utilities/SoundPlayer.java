package sisicat.main.utilities;

import sisicat.main.functions.FunctionsManager;

import javax.sound.sampled.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;

public class SoundPlayer {

    public static void initialize() {

        //try {

            File[] files = new File("hitmarkersounds").listFiles();

            if (files != null)
                for (File file : files)
                    if (file.isFile() && file.getName().endsWith(".wav")) {

                        try {

                            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
                            AudioFormat baseFormat = ais.getFormat();
                            AudioFormat decodedFormat = new AudioFormat(
                                    AudioFormat.Encoding.PCM_SIGNED,
                                    baseFormat.getSampleRate(),
                                    16,
                                    baseFormat.getChannels(),
                                    baseFormat.getChannels() * 2,
                                    baseFormat.getSampleRate(),
                                    false // little-endian
                            );
                            AudioInputStream decodedAis = AudioSystem.getAudioInputStream(decodedFormat, ais);

                            Clip clip = AudioSystem.getClip();
                            clip.open(decodedAis);
                            soundBuffers.put(file.getName().replace(".wav", ""), clip);

                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }


                    }

        //} catch (Exception ignored) {
        //}

        FunctionsManager.getFunctionByName("Player ESP").
                getSettingByName("Hit marker sound").
                optionsList = new ArrayList<>(soundBuffers.keySet());

    }

    public static void play(String name) {

        try {

            Clip clip = soundBuffers.get(name);

            if(clip.isRunning())
                clip.stop();

            FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
            gainControl.setValue(-10.0f);

            clip.setFramePosition(0);
            clip.start();

        } catch (Exception ignored) {
        }

    }

    public static final HashMap<String, Clip> soundBuffers = new HashMap<>();

}