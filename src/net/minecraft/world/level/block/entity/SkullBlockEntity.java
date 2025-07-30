package net.minecraft.world.level.block.entity;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.yggdrasil.ProfileResult;
import com.mojang.logging.LogUtils;
import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Services;
import net.minecraft.util.StringUtil;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

public class SkullBlockEntity extends BlockEntity {
    private static final String TAG_PROFILE = "profile";
    private static final String TAG_NOTE_BLOCK_SOUND = "note_block_sound";
    private static final String TAG_CUSTOM_NAME = "custom_name";
    private static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    private static Executor mainThreadExecutor;
    @Nullable
    private static LoadingCache<String, CompletableFuture<Optional<GameProfile>>> profileCacheByName;
    @Nullable
    private static LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> profileCacheById;
    public static final Executor CHECKED_MAIN_THREAD_EXECUTOR = p_296964_ -> {
        Executor executor = mainThreadExecutor;
        if (executor != null) {
            executor.execute(p_296964_);
        }
    };
    @Nullable
    private ResolvableProfile owner;
    @Nullable
    private ResourceLocation noteBlockSound;
    private int animationTickCount;
    private boolean isAnimating;
    @Nullable
    private Component customName;

    public SkullBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(BlockEntityType.SKULL, pPos, pBlockState);
    }

    public static void setup(final Services pServices, Executor pMainThreadExecutor) {
        mainThreadExecutor = pMainThreadExecutor;
        final BooleanSupplier booleansupplier = () -> profileCacheById == null;
        profileCacheByName = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<String, CompletableFuture<Optional<GameProfile>>>() {
                public CompletableFuture<Optional<GameProfile>> load(String p_312380_) {
                    return SkullBlockEntity.fetchProfileByName(p_312380_, pServices);
                }
            });
        profileCacheById = CacheBuilder.newBuilder()
            .expireAfterAccess(Duration.ofMinutes(10L))
            .maximumSize(256L)
            .build(new CacheLoader<UUID, CompletableFuture<Optional<GameProfile>>>() {
                public CompletableFuture<Optional<GameProfile>> load(UUID p_330530_) {
                    return SkullBlockEntity.fetchProfileById(p_330530_, pServices, booleansupplier);
                }
            });
    }

    static CompletableFuture<Optional<GameProfile>> fetchProfileByName(String pName, Services pServices) {
        return pServices.profileCache()
            .getAsync(pName)
            .thenCompose(
                p_327322_ -> {
                    LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheById;
                    return loadingcache != null && !p_327322_.isEmpty()
                        ? loadingcache.getUnchecked(p_327322_.get().getId()).thenApply(p_327317_ -> p_327317_.or(() -> p_327322_))
                        : CompletableFuture.completedFuture(Optional.empty());
                }
            );
    }

    static CompletableFuture<Optional<GameProfile>> fetchProfileById(UUID pId, Services pServices, BooleanSupplier pCacheUninitialized) {
        return CompletableFuture.supplyAsync(() -> {
            if (pCacheUninitialized.getAsBoolean()) {
                return Optional.empty();
            } else {
                ProfileResult profileresult = pServices.sessionService().fetchProfile(pId, true);
                return Optional.ofNullable(profileresult).map(ProfileResult::profile);
            }
        }, Util.backgroundExecutor().forName("fetchProfile"));
    }

    public static void clear() {
        mainThreadExecutor = null;
        profileCacheByName = null;
        profileCacheById = null;
    }

    @Override
    protected void saveAdditional(CompoundTag p_187518_, HolderLookup.Provider p_329143_) {
        super.saveAdditional(p_187518_, p_329143_);
        if (this.owner != null) {
            p_187518_.put("profile", ResolvableProfile.CODEC.encodeStart(NbtOps.INSTANCE, this.owner).getOrThrow());
        }

        if (this.noteBlockSound != null) {
            p_187518_.putString("note_block_sound", this.noteBlockSound.toString());
        }

        if (this.customName != null) {
            p_187518_.putString("custom_name", Component.Serializer.toJson(this.customName, p_329143_));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag p_335831_, HolderLookup.Provider p_329643_) {
        super.loadAdditional(p_335831_, p_329643_);
        if (p_335831_.contains("profile")) {
            ResolvableProfile.CODEC
                .parse(NbtOps.INSTANCE, p_335831_.get("profile"))
                .resultOrPartial(p_327318_ -> LOGGER.error("Failed to load profile from player head: {}", p_327318_))
                .ifPresent(this::setOwner);
        }

        if (p_335831_.contains("note_block_sound", 8)) {
            this.noteBlockSound = ResourceLocation.tryParse(p_335831_.getString("note_block_sound"));
        }

        if (p_335831_.contains("custom_name", 8)) {
            this.customName = parseCustomNameSafe(p_335831_.getString("custom_name"), p_329643_);
        } else {
            this.customName = null;
        }
    }

    public static void animation(Level pLevel, BlockPos pPos, BlockState pState, SkullBlockEntity pBlockEntity) {
        if (pState.hasProperty(SkullBlock.POWERED) && pState.getValue(SkullBlock.POWERED)) {
            pBlockEntity.isAnimating = true;
            pBlockEntity.animationTickCount++;
        } else {
            pBlockEntity.isAnimating = false;
        }
    }

    public float getAnimation(float pPartialTick) {
        return this.isAnimating ? (float)this.animationTickCount + pPartialTick : (float)this.animationTickCount;
    }

    @Nullable
    public ResolvableProfile getOwnerProfile() {
        return this.owner;
    }

    @Nullable
    public ResourceLocation getNoteBlockSound() {
        return this.noteBlockSound;
    }

    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider p_335540_) {
        return this.saveCustomOnly(p_335540_);
    }

    public void setOwner(@Nullable ResolvableProfile pOwner) {
        synchronized (this) {
            this.owner = pOwner;
        }

        this.updateOwnerProfile();
    }

    private void updateOwnerProfile() {
        if (this.owner != null && !this.owner.isResolved()) {
            this.owner.resolve().thenAcceptAsync(p_327314_ -> {
                this.owner = p_327314_;
                this.setChanged();
            }, CHECKED_MAIN_THREAD_EXECUTOR);
        } else {
            this.setChanged();
        }
    }

    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(String pProfileName) {
        LoadingCache<String, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheByName;
        return loadingcache != null && StringUtil.isValidPlayerName(pProfileName)
            ? loadingcache.getUnchecked(pProfileName)
            : CompletableFuture.completedFuture(Optional.empty());
    }

    public static CompletableFuture<Optional<GameProfile>> fetchGameProfile(UUID pProfileUuid) {
        LoadingCache<UUID, CompletableFuture<Optional<GameProfile>>> loadingcache = profileCacheById;
        return loadingcache != null ? loadingcache.getUnchecked(pProfileUuid) : CompletableFuture.completedFuture(Optional.empty());
    }

    @Override
    protected void applyImplicitComponents(BlockEntity.DataComponentInput p_334905_) {
        super.applyImplicitComponents(p_334905_);
        this.setOwner(p_334905_.get(DataComponents.PROFILE));
        this.noteBlockSound = p_334905_.get(DataComponents.NOTE_BLOCK_SOUND);
        this.customName = p_334905_.get(DataComponents.CUSTOM_NAME);
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder p_335245_) {
        super.collectImplicitComponents(p_335245_);
        p_335245_.set(DataComponents.PROFILE, this.owner);
        p_335245_.set(DataComponents.NOTE_BLOCK_SOUND, this.noteBlockSound);
        p_335245_.set(DataComponents.CUSTOM_NAME, this.customName);
    }

    @Override
    public void removeComponentsFromTag(CompoundTag p_332333_) {
        super.removeComponentsFromTag(p_332333_);
        p_332333_.remove("profile");
        p_332333_.remove("note_block_sound");
        p_332333_.remove("custom_name");
    }
}