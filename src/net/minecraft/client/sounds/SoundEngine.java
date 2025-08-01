package net.minecraft.client.sounds;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.mojang.blaze3d.audio.Channel;
import com.mojang.blaze3d.audio.Library;
import com.mojang.blaze3d.audio.Listener;
import com.mojang.blaze3d.audio.ListenerTransform;
import com.mojang.blaze3d.audio.SoundBuffer;
import com.mojang.logging.LogUtils;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.SharedConstants;
import net.minecraft.Util;
import net.minecraft.client.Camera;
import net.minecraft.client.Options;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceProvider;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

@OnlyIn(Dist.CLIENT)
public class SoundEngine {
    private static final Marker MARKER = MarkerFactory.getMarker("SOUNDS");
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final float PITCH_MIN = 0.5F;
    private static final float PITCH_MAX = 2.0F;
    private static final float VOLUME_MIN = 0.0F;
    private static final float VOLUME_MAX = 1.0F;
    private static final int MIN_SOURCE_LIFETIME = 20;
    private static final Set<ResourceLocation> ONLY_WARN_ONCE = Sets.newHashSet();
    private static final long DEFAULT_DEVICE_CHECK_INTERVAL_MS = 1000L;
    public static final String MISSING_SOUND = "FOR THE DEBUG!";
    public static final String OPEN_AL_SOFT_PREFIX = "OpenAL Soft on ";
    public static final int OPEN_AL_SOFT_PREFIX_LENGTH = "OpenAL Soft on ".length();
    private final SoundManager soundManager;
    private final Options options;
    private boolean loaded;
    private final Library library = new Library();
    private final Listener listener = this.library.getListener();
    private final SoundBufferLibrary soundBuffers;
    private final SoundEngineExecutor executor = new SoundEngineExecutor();
    private final ChannelAccess channelAccess = new ChannelAccess(this.library, this.executor);
    private int tickCount;
    private long lastDeviceCheckTime;
    private final AtomicReference<SoundEngine.DeviceCheckState> devicePoolState = new AtomicReference<>(SoundEngine.DeviceCheckState.NO_CHANGE);
    private final Map<SoundInstance, ChannelAccess.ChannelHandle> instanceToChannel = Maps.newHashMap();
    private final Multimap<SoundSource, SoundInstance> instanceBySource = HashMultimap.create();
    private final List<TickableSoundInstance> tickingSounds = Lists.newArrayList();
    private final Map<SoundInstance, Integer> queuedSounds = Maps.newHashMap();
    private final Map<SoundInstance, Integer> soundDeleteTime = Maps.newHashMap();
    private final List<SoundEventListener> listeners = Lists.newArrayList();
    private final List<TickableSoundInstance> queuedTickableSounds = Lists.newArrayList();
    private final List<Sound> preloadQueue = Lists.newArrayList();

    public SoundEngine(SoundManager pSoundManager, Options pOptions, ResourceProvider pResourceManager) {
        this.soundManager = pSoundManager;
        this.options = pOptions;
        this.soundBuffers = new SoundBufferLibrary(pResourceManager);
    }

    public void reload() {
        ONLY_WARN_ONCE.clear();

        for (SoundEvent soundevent : BuiltInRegistries.SOUND_EVENT) {
            if (soundevent != SoundEvents.EMPTY) {
                ResourceLocation resourcelocation = soundevent.location();
                if (this.soundManager.getSoundEvent(resourcelocation) == null) {
                    LOGGER.warn("Missing sound for event: {}", BuiltInRegistries.SOUND_EVENT.getKey(soundevent));
                    ONLY_WARN_ONCE.add(resourcelocation);
                }
            }
        }

        this.destroy();
        this.loadLibrary();
    }

    private synchronized void loadLibrary() {
        if (!this.loaded) {
            try {
                String s = this.options.soundDevice().get();
                this.library.init("".equals(s) ? null : s, this.options.directionalAudio().get());
                this.listener.reset();
                this.listener.setGain(this.options.getSoundSourceVolume(SoundSource.MASTER));
                this.soundBuffers.preload(this.preloadQueue).thenRun(this.preloadQueue::clear);
                this.loaded = true;
                LOGGER.info(MARKER, "Sound engine started");
            } catch (RuntimeException runtimeexception) {
                LOGGER.error(MARKER, "Error starting SoundSystem. Turning off sounds & music", (Throwable)runtimeexception);
            }
        }
    }

    private float getVolume(@Nullable SoundSource pCategory) {
        return pCategory != null && pCategory != SoundSource.MASTER ? this.options.getSoundSourceVolume(pCategory) : 1.0F;
    }

    public void updateCategoryVolume(SoundSource pCategory, float pVolume) {
        if (this.loaded) {
            if (pCategory == SoundSource.MASTER) {
                this.listener.setGain(pVolume);
            } else {
                this.instanceToChannel.forEach((p_120280_, p_120281_) -> {
                    float f = this.calculateVolume(p_120280_);
                    p_120281_.execute(p_174990_ -> {
                        if (f <= 0.0F) {
                            p_174990_.stop();
                        } else {
                            p_174990_.setVolume(f);
                        }
                    });
                });
            }
        }
    }

    public void destroy() {
        if (this.loaded) {
            this.stopAll();
            this.soundBuffers.clear();
            this.library.cleanup();
            this.loaded = false;
        }
    }

    public void emergencyShutdown() {
        if (this.loaded) {
            this.library.cleanup();
        }
    }

    public void stop(SoundInstance pSound) {
        if (this.loaded) {
            ChannelAccess.ChannelHandle channelaccess$channelhandle = this.instanceToChannel.get(pSound);
            if (channelaccess$channelhandle != null) {
                channelaccess$channelhandle.execute(Channel::stop);
            }
        }
    }

    public void setVolume(SoundInstance pSoundInstance, float pVolume) {
        if (this.loaded) {
            ChannelAccess.ChannelHandle channelaccess$channelhandle = this.instanceToChannel.get(pSoundInstance);
            if (channelaccess$channelhandle != null) {
                channelaccess$channelhandle.execute(p_374745_ -> p_374745_.setVolume(pVolume * this.calculateVolume(pSoundInstance)));
            }
        }
    }

    public void stopAll() {
        if (this.loaded) {
            this.executor.flush();
            this.instanceToChannel.values().forEach(p_120288_ -> p_120288_.execute(Channel::stop));
            this.instanceToChannel.clear();
            this.channelAccess.clear();
            this.queuedSounds.clear();
            this.tickingSounds.clear();
            this.instanceBySource.clear();
            this.soundDeleteTime.clear();
            this.queuedTickableSounds.clear();
        }
    }

    public void addEventListener(SoundEventListener pListener) {
        this.listeners.add(pListener);
    }

    public void removeEventListener(SoundEventListener pListener) {
        this.listeners.remove(pListener);
    }

    private boolean shouldChangeDevice() {
        if (this.library.isCurrentDeviceDisconnected()) {
            LOGGER.info("Audio device was lost!");
            return true;
        } else {
            long i = Util.getMillis();
            boolean flag = i - this.lastDeviceCheckTime >= 1000L;
            if (flag) {
                this.lastDeviceCheckTime = i;
                if (this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.NO_CHANGE, SoundEngine.DeviceCheckState.ONGOING)) {
                    String s = this.options.soundDevice().get();
                    Util.ioPool().execute(() -> {
                        if ("".equals(s)) {
                            if (this.library.hasDefaultDeviceChanged()) {
                                LOGGER.info("System default audio device has changed!");
                                this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.CHANGE_DETECTED);
                            }
                        } else if (!this.library.getCurrentDeviceName().equals(s) && this.library.getAvailableSoundDevices().contains(s)) {
                            LOGGER.info("Preferred audio device has become available!");
                            this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.CHANGE_DETECTED);
                        }

                        this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.ONGOING, SoundEngine.DeviceCheckState.NO_CHANGE);
                    });
                }
            }

            return this.devicePoolState.compareAndSet(SoundEngine.DeviceCheckState.CHANGE_DETECTED, SoundEngine.DeviceCheckState.NO_CHANGE);
        }
    }

    public void tick(boolean pIsGamePaused) {
        if (this.shouldChangeDevice()) {
            this.reload();
        }

        if (!pIsGamePaused) {
            this.tickNonPaused();
        }

        this.channelAccess.scheduleTick();
    }

    private void tickNonPaused() {
        this.tickCount++;
        this.queuedTickableSounds.stream().filter(SoundInstance::canPlaySound).forEach(this::play);
        this.queuedTickableSounds.clear();

        for (TickableSoundInstance tickablesoundinstance : this.tickingSounds) {
            if (!tickablesoundinstance.canPlaySound()) {
                this.stop(tickablesoundinstance);
            }

            tickablesoundinstance.tick();
            if (tickablesoundinstance.isStopped()) {
                this.stop(tickablesoundinstance);
            } else {
                float f = this.calculateVolume(tickablesoundinstance);
                float f1 = this.calculatePitch(tickablesoundinstance);
                Vec3 vec3 = new Vec3(tickablesoundinstance.getX(), tickablesoundinstance.getY(), tickablesoundinstance.getZ());
                ChannelAccess.ChannelHandle channelaccess$channelhandle = this.instanceToChannel.get(tickablesoundinstance);
                if (channelaccess$channelhandle != null) {
                    channelaccess$channelhandle.execute(p_194478_ -> {
                        p_194478_.setVolume(f);
                        p_194478_.setPitch(f1);
                        p_194478_.setSelfPosition(vec3);
                    });
                }
            }
        }

        Iterator<Entry<SoundInstance, ChannelAccess.ChannelHandle>> iterator = this.instanceToChannel.entrySet().iterator();

        while (iterator.hasNext()) {
            Entry<SoundInstance, ChannelAccess.ChannelHandle> entry = iterator.next();
            ChannelAccess.ChannelHandle channelaccess$channelhandle1 = entry.getValue();
            SoundInstance soundinstance = entry.getKey();
            float f2 = this.options.getSoundSourceVolume(soundinstance.getSource());
            if (f2 <= 0.0F) {
                channelaccess$channelhandle1.execute(Channel::stop);
                iterator.remove();
            } else if (channelaccess$channelhandle1.isStopped()) {
                int i = this.soundDeleteTime.get(soundinstance);
                if (i <= this.tickCount) {
                    if (shouldLoopManually(soundinstance)) {
                        this.queuedSounds.put(soundinstance, this.tickCount + soundinstance.getDelay());
                    }

                    iterator.remove();
                    LOGGER.debug(MARKER, "Removed channel {} because it's not playing anymore", channelaccess$channelhandle1);
                    this.soundDeleteTime.remove(soundinstance);

                    try {
                        this.instanceBySource.remove(soundinstance.getSource(), soundinstance);
                    } catch (RuntimeException runtimeexception) {
                    }

                    if (soundinstance instanceof TickableSoundInstance) {
                        this.tickingSounds.remove(soundinstance);
                    }
                }
            }
        }

        Iterator<Entry<SoundInstance, Integer>> iterator1 = this.queuedSounds.entrySet().iterator();

        while (iterator1.hasNext()) {
            Entry<SoundInstance, Integer> entry1 = iterator1.next();
            if (this.tickCount >= entry1.getValue()) {
                SoundInstance soundinstance1 = entry1.getKey();
                if (soundinstance1 instanceof TickableSoundInstance) {
                    ((TickableSoundInstance)soundinstance1).tick();
                }

                this.play(soundinstance1);
                iterator1.remove();
            }
        }
    }

    private static boolean requiresManualLooping(SoundInstance pSound) {
        return pSound.getDelay() > 0;
    }

    private static boolean shouldLoopManually(SoundInstance pSound) {
        return pSound.isLooping() && requiresManualLooping(pSound);
    }

    private static boolean shouldLoopAutomatically(SoundInstance pSound) {
        return pSound.isLooping() && !requiresManualLooping(pSound);
    }

    public boolean isActive(SoundInstance pSound) {
        if (!this.loaded) {
            return false;
        } else {
            return this.soundDeleteTime.containsKey(pSound) && this.soundDeleteTime.get(pSound) <= this.tickCount ? true : this.instanceToChannel.containsKey(pSound);
        }
    }

    public void play(SoundInstance pSound) {
        if (this.loaded) {
            if (pSound.canPlaySound()) {
                WeighedSoundEvents weighedsoundevents = pSound.resolve(this.soundManager);
                ResourceLocation resourcelocation = pSound.getLocation();
                if (weighedsoundevents == null) {
                    if (ONLY_WARN_ONCE.add(resourcelocation)) {
                        LOGGER.warn(MARKER, "Unable to play unknown soundEvent: {}", resourcelocation);
                    }
                } else {
                    Sound sound = pSound.getSound();
                    if (sound != SoundManager.INTENTIONALLY_EMPTY_SOUND) {
                        if (sound == SoundManager.EMPTY_SOUND) {
                            if (ONLY_WARN_ONCE.add(resourcelocation)) {
                                LOGGER.warn(MARKER, "Unable to play empty soundEvent: {}", resourcelocation);
                            }
                        } else {
                            float f = pSound.getVolume();
                            float f1 = Math.max(f, 1.0F) * (float)sound.getAttenuationDistance();
                            SoundSource soundsource = pSound.getSource();
                            float f2 = this.calculateVolume(f, soundsource);
                            float f3 = this.calculatePitch(pSound);
                            SoundInstance.Attenuation soundinstance$attenuation = pSound.getAttenuation();
                            boolean flag = pSound.isRelative();
                            if (f2 == 0.0F && !pSound.canStartSilent()) {
                                LOGGER.debug(MARKER, "Skipped playing sound {}, volume was zero.", sound.getLocation());
                            } else {
                                Vec3 vec3 = new Vec3(pSound.getX(), pSound.getY(), pSound.getZ());
                                if (!this.listeners.isEmpty()) {
                                    float f4 = !flag && soundinstance$attenuation != SoundInstance.Attenuation.NONE ? f1 : Float.POSITIVE_INFINITY;

                                    for (SoundEventListener soundeventlistener : this.listeners) {
                                        soundeventlistener.onPlaySound(pSound, weighedsoundevents, f4);
                                    }
                                }

                                if (this.listener.getGain() <= 0.0F) {
                                    LOGGER.debug(MARKER, "Skipped playing soundEvent: {}, master volume was zero", resourcelocation);
                                } else {
                                    boolean flag1 = shouldLoopAutomatically(pSound);
                                    boolean flag2 = sound.shouldStream();
                                    CompletableFuture<ChannelAccess.ChannelHandle> completablefuture = this.channelAccess
                                        .createHandle(sound.shouldStream() ? Library.Pool.STREAMING : Library.Pool.STATIC);
                                    ChannelAccess.ChannelHandle channelaccess$channelhandle = completablefuture.join();
                                    if (channelaccess$channelhandle == null) {
                                        if (SharedConstants.IS_RUNNING_IN_IDE) {
                                            LOGGER.warn("Failed to create new sound handle");
                                        }
                                    } else {
                                        LOGGER.debug(MARKER, "Playing sound {} for event {}", sound.getLocation(), resourcelocation);
                                        this.soundDeleteTime.put(pSound, this.tickCount + 20);
                                        this.instanceToChannel.put(pSound, channelaccess$channelhandle);
                                        this.instanceBySource.put(soundsource, pSound);
                                        channelaccess$channelhandle.execute(p_194488_ -> {
                                            p_194488_.setPitch(f3);
                                            p_194488_.setVolume(f2);
                                            if (soundinstance$attenuation == SoundInstance.Attenuation.LINEAR) {
                                                p_194488_.linearAttenuation(f1);
                                            } else {
                                                p_194488_.disableAttenuation();
                                            }

                                            p_194488_.setLooping(flag1 && !flag2);
                                            p_194488_.setSelfPosition(vec3);
                                            p_194488_.setRelative(flag);
                                        });
                                        if (!flag2) {
                                            this.soundBuffers
                                                .getCompleteBuffer(sound.getPath())
                                                .thenAccept(p_377326_ -> channelaccess$channelhandle.execute(p_194495_ -> {
                                                        p_194495_.attachStaticBuffer(p_377326_);
                                                        p_194495_.play();
                                                    }));
                                        } else {
                                            this.soundBuffers
                                                .getStream(sound.getPath(), flag1)
                                                .thenAccept(p_376862_ -> channelaccess$channelhandle.execute(p_194498_ -> {
                                                        p_194498_.attachBufferStream(p_376862_);
                                                        p_194498_.play();
                                                    }));
                                        }

                                        if (pSound instanceof TickableSoundInstance) {
                                            this.tickingSounds.add((TickableSoundInstance)pSound);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    public void queueTickingSound(TickableSoundInstance pTickableSound) {
        this.queuedTickableSounds.add(pTickableSound);
    }

    public void requestPreload(Sound pSound) {
        this.preloadQueue.add(pSound);
    }

    private float calculatePitch(SoundInstance pSound) {
        return Mth.clamp(pSound.getPitch(), 0.5F, 2.0F);
    }

    private float calculateVolume(SoundInstance pSound) {
        return this.calculateVolume(pSound.getVolume(), pSound.getSource());
    }

    private float calculateVolume(float pVolumeMultiplier, SoundSource pSource) {
        return Mth.clamp(pVolumeMultiplier * this.getVolume(pSource), 0.0F, 1.0F);
    }

    public void pause() {
        if (this.loaded) {
            this.channelAccess.executeOnChannels(p_194510_ -> p_194510_.forEach(Channel::pause));
        }
    }

    public void resume() {
        if (this.loaded) {
            this.channelAccess.executeOnChannels(p_194508_ -> p_194508_.forEach(Channel::unpause));
        }
    }

    public void playDelayed(SoundInstance pSound, int pDelay) {
        this.queuedSounds.put(pSound, this.tickCount + pDelay);
    }

    public void updateSource(Camera pRenderInfo) {
        if (this.loaded && pRenderInfo.isInitialized()) {
            ListenerTransform listenertransform = new ListenerTransform(pRenderInfo.getPosition(), new Vec3(pRenderInfo.getLookVector()), new Vec3(pRenderInfo.getUpVector()));
            this.executor.execute(() -> this.listener.setTransform(listenertransform));
        }
    }

    public void stop(@Nullable ResourceLocation pSoundName, @Nullable SoundSource pCategory) {
        if (pCategory != null) {
            for (SoundInstance soundinstance : this.instanceBySource.get(pCategory)) {
                if (pSoundName == null || soundinstance.getLocation().equals(pSoundName)) {
                    this.stop(soundinstance);
                }
            }
        } else if (pSoundName == null) {
            this.stopAll();
        } else {
            for (SoundInstance soundinstance1 : this.instanceToChannel.keySet()) {
                if (soundinstance1.getLocation().equals(pSoundName)) {
                    this.stop(soundinstance1);
                }
            }
        }
    }

    public String getDebugString() {
        return this.library.getDebugString();
    }

    public List<String> getAvailableSoundDevices() {
        return this.library.getAvailableSoundDevices();
    }

    public ListenerTransform getListenerTransform() {
        return this.listener.getTransform();
    }

    @OnlyIn(Dist.CLIENT)
    static enum DeviceCheckState {
        ONGOING,
        CHANGE_DETECTED,
        NO_CHANGE;
    }
}