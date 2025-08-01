package net.minecraft.tags;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ExtraCodecs;

public class TagEntry {
    private static final Codec<TagEntry> FULL_CODEC = RecordCodecBuilder.create(
        p_215937_ -> p_215937_.group(
                    ExtraCodecs.TAG_OR_ELEMENT_ID.fieldOf("id").forGetter(TagEntry::elementOrTag),
                    Codec.BOOL.optionalFieldOf("required", Boolean.valueOf(true)).forGetter(p_215952_ -> p_215952_.required)
                )
                .apply(p_215937_, TagEntry::new)
    );
    public static final Codec<TagEntry> CODEC = Codec.either(ExtraCodecs.TAG_OR_ELEMENT_ID, FULL_CODEC)
        .xmap(
            p_215935_ -> p_215935_.map(p_215933_ -> new TagEntry(p_215933_, true), p_215946_ -> (TagEntry)p_215946_),
            p_215931_ -> p_215931_.required ? Either.left(p_215931_.elementOrTag()) : Either.right(p_215931_)
        );
    private final ResourceLocation id;
    private final boolean tag;
    private final boolean required;

    private TagEntry(ResourceLocation pId, boolean pTag, boolean pRequired) {
        this.id = pId;
        this.tag = pTag;
        this.required = pRequired;
    }

    private TagEntry(ExtraCodecs.TagOrElementLocation pTagOrElementLocation, boolean pRequired) {
        this.id = pTagOrElementLocation.id();
        this.tag = pTagOrElementLocation.tag();
        this.required = pRequired;
    }

    private ExtraCodecs.TagOrElementLocation elementOrTag() {
        return new ExtraCodecs.TagOrElementLocation(this.id, this.tag);
    }

    public static TagEntry element(ResourceLocation pElementLocation) {
        return new TagEntry(pElementLocation, false, true);
    }

    public static TagEntry optionalElement(ResourceLocation pElementLocation) {
        return new TagEntry(pElementLocation, false, false);
    }

    public static TagEntry tag(ResourceLocation pTagLocation) {
        return new TagEntry(pTagLocation, true, true);
    }

    public static TagEntry optionalTag(ResourceLocation pTagLocation) {
        return new TagEntry(pTagLocation, true, false);
    }

    public <T> boolean build(TagEntry.Lookup<T> pLookup, Consumer<T> pConsumer) {
        if (this.tag) {
            Collection<T> collection = pLookup.tag(this.id);
            if (collection == null) {
                return !this.required;
            }

            collection.forEach(pConsumer);
        } else {
            T t = pLookup.element(this.id, this.required);
            if (t == null) {
                return !this.required;
            }

            pConsumer.accept(t);
        }

        return true;
    }

    public void visitRequiredDependencies(Consumer<ResourceLocation> pVisitor) {
        if (this.tag && this.required) {
            pVisitor.accept(this.id);
        }
    }

    public void visitOptionalDependencies(Consumer<ResourceLocation> pVisitor) {
        if (this.tag && !this.required) {
            pVisitor.accept(this.id);
        }
    }

    public boolean verifyIfPresent(Predicate<ResourceLocation> pElementPredicate, Predicate<ResourceLocation> pTagPredicate) {
        return !this.required || (this.tag ? pTagPredicate : pElementPredicate).test(this.id);
    }

    @Override
    public String toString() {
        StringBuilder stringbuilder = new StringBuilder();
        if (this.tag) {
            stringbuilder.append('#');
        }

        stringbuilder.append(this.id);
        if (!this.required) {
            stringbuilder.append('?');
        }

        return stringbuilder.toString();
    }

    public interface Lookup<T> {
        @Nullable
        T element(ResourceLocation pId, boolean pRequired);

        @Nullable
        Collection<T> tag(ResourceLocation pId);
    }
}