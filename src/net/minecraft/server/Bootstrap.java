package net.minecraft.server;

import com.mojang.logging.LogUtils;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.SharedConstants;
import net.minecraft.SuppressForbidden;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.selector.options.EntitySelectorOptions;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.dispenser.DispenseItemBehavior;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.block.FireBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.slf4j.Logger;

//@SuppressForbidden(
//    a = "System.out setup"
//)
public class Bootstrap {
    public static final PrintStream STDOUT = System.out;
    private static volatile boolean isBootstrapped;
    private static final Logger LOGGER = LogUtils.getLogger();
    public static final AtomicLong bootstrapDuration = new AtomicLong(-1L);

    public static void bootStrap() {
        if (!isBootstrapped) {
            isBootstrapped = true;
            Instant instant = Instant.now();
            if (BuiltInRegistries.REGISTRY.keySet().isEmpty()) {
                throw new IllegalStateException("Unable to load registries");
            } else {
                FireBlock.bootStrap();
                ComposterBlock.bootStrap();
                if (EntityType.getKey(EntityType.PLAYER) == null) {
                    throw new IllegalStateException("Failed loading EntityTypes");
                } else {
                    EntitySelectorOptions.bootStrap();
                    DispenseItemBehavior.bootStrap();
                    CauldronInteraction.bootStrap();
                    BuiltInRegistries.bootStrap();
                    CreativeModeTabs.validate();
                    wrapStreams();
                    bootstrapDuration.set(Duration.between(instant, Instant.now()).toMillis());
                }
            }
        }
    }

    private static <T> void checkTranslations(Iterable<T> pObjects, Function<T, String> pObjectToKeyFunction, Set<String> pTranslationSet) {
        Language language = Language.getInstance();
        pObjects.forEach(p_135883_ -> {
            String s = pObjectToKeyFunction.apply((T)p_135883_);
            if (!language.has(s)) {
                pTranslationSet.add(s);
            }
        });
    }

    private static void checkGameruleTranslations(final Set<String> pTranslations) {
        final Language language = Language.getInstance();
        GameRules gamerules = new GameRules(FeatureFlags.REGISTRY.allFlags());
        gamerules.visitGameRuleTypes(new GameRules.GameRuleTypeVisitor() {
            @Override
            public <T extends GameRules.Value<T>> void visit(GameRules.Key<T> p_135897_, GameRules.Type<T> p_135898_) {
                if (!language.has(p_135897_.getDescriptionId())) {
                    pTranslations.add(p_135897_.getId());
                }
            }
        });
    }

    public static Set<String> getMissingTranslations() {
        Set<String> set = new TreeSet<>();
        checkTranslations(BuiltInRegistries.ATTRIBUTE, Attribute::getDescriptionId, set);
        checkTranslations(BuiltInRegistries.ENTITY_TYPE, EntityType::getDescriptionId, set);
        checkTranslations(BuiltInRegistries.MOB_EFFECT, MobEffect::getDescriptionId, set);
        checkTranslations(BuiltInRegistries.ITEM, Item::getDescriptionId, set);
        checkTranslations(BuiltInRegistries.BLOCK, BlockBehaviour::getDescriptionId, set);
        checkTranslations(BuiltInRegistries.CUSTOM_STAT, p_135885_ -> "stat." + p_135885_.toString().replace(':', '.'), set);
        checkGameruleTranslations(set);
        return set;
    }

    public static void checkBootstrapCalled(Supplier<String> pCallSite) {
        if (!isBootstrapped) {
            throw createBootstrapException(pCallSite);
        }
    }

    private static RuntimeException createBootstrapException(Supplier<String> pCallSite) {
        try {
            String s = pCallSite.get();
            return new IllegalArgumentException("Not bootstrapped (called from " + s + ")");
        } catch (Exception exception) {
            RuntimeException runtimeexception = new IllegalArgumentException("Not bootstrapped (failed to resolve location)");
            runtimeexception.addSuppressed(exception);
            return runtimeexception;
        }
    }

    public static void validate() {
        checkBootstrapCalled(() -> "validate");
        if (SharedConstants.IS_RUNNING_IN_IDE) {
            getMissingTranslations().forEach(p_179915_ -> LOGGER.error("Missing translations: {}", p_179915_));
            Commands.validate();
        }

        DefaultAttributes.validate();
    }

    private static void wrapStreams() {
        if (LOGGER.isDebugEnabled()) {
            System.setErr(new DebugLoggedPrintStream("STDERR", System.err));
            System.setOut(new DebugLoggedPrintStream("STDOUT", STDOUT));
        } else {
            System.setErr(new LoggedPrintStream("STDERR", System.err));
            System.setOut(new LoggedPrintStream("STDOUT", STDOUT));
        }
    }

    public static void realStdoutPrintln(String pMessage) {
        STDOUT.println(pMessage);
    }
}