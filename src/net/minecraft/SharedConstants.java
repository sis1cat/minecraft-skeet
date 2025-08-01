package net.minecraft;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.netty.util.ResourceLeakDetector;
import io.netty.util.ResourceLeakDetector.Level;
import java.time.Duration;
import javax.annotation.Nullable;
import net.minecraft.commands.BrigadierExceptions;
import net.minecraft.world.level.ChunkPos;

public class SharedConstants {
    @Deprecated
    public static final boolean SNAPSHOT = false;
    @Deprecated
    public static final int WORLD_VERSION = 4189;
    @Deprecated
    public static final String SERIES = "main";
    @Deprecated
    public static final String VERSION_STRING = "1.21.4";
    @Deprecated
    public static final int RELEASE_NETWORK_PROTOCOL_VERSION = 769;
    @Deprecated
    public static final int SNAPSHOT_NETWORK_PROTOCOL_VERSION = 228;
    public static final int SNBT_NAG_VERSION = 4173;
    private static final int SNAPSHOT_PROTOCOL_BIT = 30;
    public static final boolean CRASH_EAGERLY = false;
    @Deprecated
    public static final int RESOURCE_PACK_FORMAT = 46;
    @Deprecated
    public static final int DATA_PACK_FORMAT = 61;
    @Deprecated
    public static final int LANGUAGE_FORMAT = 1;
    public static final int REPORT_FORMAT_VERSION = 1;
    public static final String DATA_VERSION_TAG = "DataVersion";
    public static final boolean FIX_TNT_DUPE = false;
    public static final boolean FIX_SAND_DUPE = false;
    public static final boolean USE_DEBUG_FEATURES = false;
    public static final boolean DEBUG_OPEN_INCOMPATIBLE_WORLDS = false;
    public static final boolean DEBUG_ALLOW_LOW_SIM_DISTANCE = false;
    public static final boolean DEBUG_HOTKEYS = false;
    public static final boolean DEBUG_UI_NARRATION = false;
    public static final boolean DEBUG_RENDER = false;
    public static final boolean DEBUG_PATHFINDING = false;
    public static final boolean DEBUG_WATER = false;
    public static final boolean DEBUG_HEIGHTMAP = false;
    public static final boolean DEBUG_COLLISION = false;
    public static final boolean DEBUG_SHOW_LOCAL_SERVER_ENTITY_HIT_BOXES = false;
    public static final boolean DEBUG_SUPPORT_BLOCKS = false;
    public static final boolean DEBUG_SHAPES = false;
    public static final boolean DEBUG_NEIGHBORSUPDATE = false;
    public static final boolean DEBUG_EXPERIMENTAL_REDSTONEWIRE_UPDATE_ORDER = false;
    public static final boolean DEBUG_STRUCTURES = false;
    public static final boolean DEBUG_LIGHT = false;
    public static final boolean DEBUG_SKY_LIGHT_SECTIONS = false;
    public static final boolean DEBUG_WORLDGENATTEMPT = false;
    public static final boolean DEBUG_SOLID_FACE = false;
    public static final boolean DEBUG_CHUNKS = false;
    public static final boolean DEBUG_GAME_EVENT_LISTENERS = false;
    public static final boolean DEBUG_DUMP_TEXTURE_ATLAS = false;
    public static final boolean DEBUG_DUMP_INTERPOLATED_TEXTURE_FRAMES = false;
    public static final boolean DEBUG_STRUCTURE_EDIT_MODE = false;
    public static final boolean DEBUG_SAVE_STRUCTURES_AS_SNBT = false;
    public static final boolean DEBUG_SYNCHRONOUS_GL_LOGS = false;
    public static final boolean DEBUG_VERBOSE_SERVER_EVENTS = false;
    public static final boolean DEBUG_NAMED_RUNNABLES = false;
    public static final boolean DEBUG_GOAL_SELECTOR = false;
    public static final boolean DEBUG_VILLAGE_SECTIONS = false;
    public static final boolean DEBUG_BRAIN = false;
    public static final boolean DEBUG_BEES = false;
    public static final boolean DEBUG_RAIDS = false;
    public static final boolean DEBUG_BLOCK_BREAK = false;
    public static final boolean DEBUG_MONITOR_TICK_TIMES = false;
    public static final boolean DEBUG_KEEP_JIGSAW_BLOCKS_DURING_STRUCTURE_GEN = false;
    public static final boolean DEBUG_DONT_SAVE_WORLD = false;
    public static final boolean DEBUG_LARGE_DRIPSTONE = false;
    public static final boolean DEBUG_CARVERS = false;
    public static final boolean DEBUG_ORE_VEINS = false;
    public static final boolean DEBUG_SCULK_CATALYST = false;
    public static final boolean DEBUG_BYPASS_REALMS_VERSION_CHECK = false;
    public static final boolean DEBUG_SOCIAL_INTERACTIONS = false;
    public static final boolean DEBUG_VALIDATE_RESOURCE_PATH_CASE = false;
    public static final boolean DEBUG_UNLOCK_ALL_TRADES = false;
    public static final boolean DEBUG_BREEZE_MOB = false;
    public static final boolean DEBUG_TRIAL_SPAWNER_DETECTS_SHEEP_AS_PLAYERS = false;
    public static final boolean DEBUG_VAULT_DETECTS_SHEEP_AS_PLAYERS = false;
    public static final boolean DEBUG_FORCE_ONBOARDING_SCREEN = false;
    public static final boolean DEBUG_IGNORE_LOCAL_MOB_CAP = false;
    public static final boolean DEBUG_DISABLE_LIQUID_SPREADING = false;
    public static final boolean DEBUG_AQUIFERS = false;
    public static final boolean DEBUG_JFR_PROFILING_ENABLE_LEVEL_LOADING = false;
    public static boolean debugGenerateSquareTerrainWithoutNoise = false;
    public static boolean debugGenerateStripedTerrainWithoutNoise = false;
    public static final boolean DEBUG_ONLY_GENERATE_HALF_THE_WORLD = false;
    public static final boolean DEBUG_DISABLE_FLUID_GENERATION = false;
    public static final boolean DEBUG_DISABLE_AQUIFERS = false;
    public static final boolean DEBUG_DISABLE_SURFACE = false;
    public static final boolean DEBUG_DISABLE_CARVERS = false;
    public static final boolean DEBUG_DISABLE_STRUCTURES = false;
    public static final boolean DEBUG_DISABLE_FEATURES = false;
    public static final boolean DEBUG_DISABLE_ORE_VEINS = false;
    public static final boolean DEBUG_DISABLE_BLENDING = false;
    public static final boolean DEBUG_DISABLE_BELOW_ZERO_RETROGENERATION = false;
    public static final int DEFAULT_MINECRAFT_PORT = 25565;
    public static final boolean DEBUG_SUBTITLES = false;
    public static final int FAKE_MS_LATENCY = 0;
    public static final int FAKE_MS_JITTER = 0;
    public static final Level NETTY_LEAK_DETECTION = Level.DISABLED;
    public static final boolean COMMAND_STACK_TRACES = false;
    public static final boolean DEBUG_WORLD_RECREATE = false;
    public static final boolean DEBUG_SHOW_SERVER_DEBUG_VALUES = false;
    public static final boolean DEBUG_FEATURE_COUNT = false;
    public static final boolean DEBUG_RESOURCE_GENERATION_OVERRIDE = false;
    public static final boolean DEBUG_FORCE_TELEMETRY = false;
    public static final boolean DEBUG_DONT_SEND_TELEMETRY_TO_BACKEND = false;
    public static final long MAXIMUM_TICK_TIME_NANOS = Duration.ofMillis(300L).toNanos();
    public static final float MAXIMUM_BLOCK_EXPLOSION_RESISTANCE = 3600000.0F;
    public static final boolean USE_WORKFLOWS_HOOKS = false;
    public static final boolean USE_DEVONLY = false;
    public static boolean CHECK_DATA_FIXER_SCHEMA = true;
    public static boolean IS_RUNNING_IN_IDE;
    public static final int WORLD_RESOLUTION = 16;
    public static final int MAX_CHAT_LENGTH = 256;
    public static final int MAX_USER_INPUT_COMMAND_LENGTH = 32500;
    public static final int MAX_FUNCTION_COMMAND_LENGTH = 2000000;
    public static final int MAX_PLAYER_NAME_LENGTH = 16;
    public static final int MAX_CHAINED_NEIGHBOR_UPDATES = 1000000;
    public static final int MAX_RENDER_DISTANCE = 32;
    public static final char[] ILLEGAL_FILE_CHARACTERS = new char[]{'/', '\n', '\r', '\t', '\u0000', '\f', '`', '?', '*', '\\', '<', '>', '|', '"', ':'};
    public static final int TICKS_PER_SECOND = 20;
    public static final int MILLIS_PER_TICK = 50;
    public static final int TICKS_PER_MINUTE = 1200;
    public static final int TICKS_PER_GAME_DAY = 24000;
    public static final float AVERAGE_GAME_TICKS_PER_RANDOM_TICK_PER_BLOCK = 1365.3334F;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_MINUTE = 0.87890625F;
    public static final float AVERAGE_RANDOM_TICKS_PER_BLOCK_PER_GAME_DAY = 17.578125F;
    public static final int WORLD_ICON_SIZE = 64;
    @Nullable
    private static WorldVersion CURRENT_VERSION;

    public static void setVersion(WorldVersion pVersion) {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = pVersion;
        } else if (pVersion != CURRENT_VERSION) {
            throw new IllegalStateException("Cannot override the current game version!");
        }
    }

    public static void tryDetectVersion() {
        if (CURRENT_VERSION == null) {
            CURRENT_VERSION = DetectedVersion.tryDetectVersion();
        }
    }

    public static WorldVersion getCurrentVersion() {
        if (CURRENT_VERSION == null) {
            throw new IllegalStateException("Game version not set");
        } else {
            return CURRENT_VERSION;
        }
    }

    public static int getProtocolVersion() {
        return 769;
    }

    public static boolean debugVoidTerrain(ChunkPos pChunkPos) {
        int i = pChunkPos.getMinBlockX();
        int j = pChunkPos.getMinBlockZ();
        return !debugGenerateSquareTerrainWithoutNoise ? false : i > 8192 || i < 0 || j > 1024 || j < 0;
    }

    static {
        ResourceLeakDetector.setLevel(NETTY_LEAK_DETECTION);
        CommandSyntaxException.ENABLE_COMMAND_STACK_TRACES = false;
        CommandSyntaxException.BUILT_IN_EXCEPTIONS = new BrigadierExceptions();
    }
}