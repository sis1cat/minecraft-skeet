package sisicat.main.utilities;

import org.lwjgl.openal.AL10;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import java.io.BufferedInputStream;
import java.nio.ByteBuffer;

public class SoundPlayer {

    public static void play(String path, float volume){

        new Thread(() -> {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new BufferedInputStream(SoundPlayer.class.getResourceAsStream(path)));
                
                int format = -1;
                switch (audioInputStream.getFormat().getChannels()) {
                    case 1:
                        format = audioInputStream.getFormat().getSampleSizeInBits() == 8 ? AL10.AL_FORMAT_MONO8 : AL10.AL_FORMAT_MONO16;
                        break;
                    case 2:
                        format = audioInputStream.getFormat().getSampleSizeInBits() == 8 ? AL10.AL_FORMAT_STEREO8 : AL10.AL_FORMAT_STEREO16;
                        break;
                    default:
                        throw new IllegalStateException("");
                }


                byte[] data = new byte[audioInputStream.available()];

                int bytesRead = audioInputStream.read(data);
                int buffer = AL10.alGenBuffers();
                ByteBuffer byteBuffer = ByteBuffer.allocateDirect(data.length);
                byteBuffer.put(data);
                byteBuffer.flip();
                AL10.alBufferData(buffer, format, byteBuffer, (int) audioInputStream.getFormat().getSampleRate());

                int source = AL10.alGenSources();
                AL10.alSourcei(source, AL10.AL_BUFFER, buffer);
                AL10.alSourcef(source, AL10.AL_GAIN, volume);

                AL10.alSourcePlay(source);

                if(AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE) != AL10.AL_PLAYING) {
                    AL10.alDeleteSources(source);
                    AL10.alDeleteBuffers(buffer);
                }


            } catch (Exception e) {
                e.printStackTrace();
            }

        }).start();

    }

}
