package net.minecraft.core;

import com.mojang.datafixers.util.Either;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterator;
import java.util.function.Function;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import net.minecraft.Util;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.VisibleForTesting;

public interface HolderSet<T> extends Iterable<Holder<T>> {
    Stream<Holder<T>> stream();

    int size();

    boolean isBound();

    Either<TagKey<T>, List<Holder<T>>> unwrap();

    Optional<Holder<T>> getRandomElement(RandomSource pRandom);

    Holder<T> get(int pIndex);

    boolean contains(Holder<T> pHolder);

    boolean canSerializeIn(HolderOwner<T> pOwner);

    Optional<TagKey<T>> unwrapKey();

    @Deprecated
    @VisibleForTesting
    static <T> HolderSet.Named<T> emptyNamed(HolderOwner<T> pOwner, TagKey<T> pKey) {
        return new HolderSet.Named<T>(pOwner, pKey) {
            @Override
            protected List<Holder<T>> contents() {
                throw new UnsupportedOperationException("Tag " + this.key() + " can't be dereferenced during construction");
            }
        };
    }

    static <T> HolderSet<T> empty() {
        return (HolderSet<T>)HolderSet.Direct.EMPTY;
    }

    @SafeVarargs
    static <T> HolderSet.Direct<T> direct(Holder<T>... pContents) {
        return new HolderSet.Direct<>(List.of(pContents));
    }

    static <T> HolderSet.Direct<T> direct(List<? extends Holder<T>> pContents) {
        return new HolderSet.Direct<>(List.copyOf(pContents));
    }

    @SafeVarargs
    static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> pHolderFactory, E... pValues) {
        return direct(Stream.of(pValues).map(pHolderFactory).toList());
    }

    static <E, T> HolderSet.Direct<T> direct(Function<E, Holder<T>> pHolderFactory, Collection<E> pValues) {
        return direct(pValues.stream().map(pHolderFactory).toList());
    }

    public static final class Direct<T> extends HolderSet.ListBacked<T> {
        static final HolderSet.Direct<?> EMPTY = new HolderSet.Direct(List.of());
        private final List<Holder<T>> contents;
        @Nullable
        private Set<Holder<T>> contentsSet;

        Direct(List<Holder<T>> pContents) {
            this.contents = pContents;
        }

        @Override
        protected List<Holder<T>> contents() {
            return this.contents;
        }

        @Override
        public boolean isBound() {
            return true;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap() {
            return Either.right(this.contents);
        }

        @Override
        public Optional<TagKey<T>> unwrapKey() {
            return Optional.empty();
        }

        @Override
        public boolean contains(Holder<T> p_205816_) {
            if (this.contentsSet == null) {
                this.contentsSet = Set.copyOf(this.contents);
            }

            return this.contentsSet.contains(p_205816_);
        }

        @Override
        public String toString() {
            return "DirectSet[" + this.contents + "]";
        }

        @Override
        public boolean equals(Object pOther) {
            if (this == pOther) {
                return true;
            } else {
                if (pOther instanceof HolderSet.Direct<?> direct && this.contents.equals(direct.contents)) {
                    return true;
                }

                return false;
            }
        }

        @Override
        public int hashCode() {
            return this.contents.hashCode();
        }
    }

    public abstract static class ListBacked<T> implements HolderSet<T> {
        protected abstract List<Holder<T>> contents();

        @Override
        public int size() {
            return this.contents().size();
        }

        @Override
        public Spliterator<Holder<T>> spliterator() {
            return this.contents().spliterator();
        }

        @Override
        public Iterator<Holder<T>> iterator() {
            return this.contents().iterator();
        }

        @Override
        public Stream<Holder<T>> stream() {
            return this.contents().stream();
        }

        @Override
        public Optional<Holder<T>> getRandomElement(RandomSource p_235714_) {
            return Util.getRandomSafe(this.contents(), p_235714_);
        }

        @Override
        public Holder<T> get(int p_205823_) {
            return this.contents().get(p_205823_);
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> p_255876_) {
            return true;
        }
    }

    public static class Named<T> extends HolderSet.ListBacked<T> {
        private final HolderOwner<T> owner;
        private final TagKey<T> key;
        @Nullable
        private List<Holder<T>> contents;

        Named(HolderOwner<T> pOwner, TagKey<T> pKey) {
            this.owner = pOwner;
            this.key = pKey;
        }

        void bind(List<Holder<T>> pContents) {
            this.contents = List.copyOf(pContents);
        }

        public TagKey<T> key() {
            return this.key;
        }

        @Override
        protected List<Holder<T>> contents() {
            if (this.contents == null) {
                throw new IllegalStateException("Trying to access unbound tag '" + this.key + "' from registry " + this.owner);
            } else {
                return this.contents;
            }
        }

        @Override
        public boolean isBound() {
            return this.contents != null;
        }

        @Override
        public Either<TagKey<T>, List<Holder<T>>> unwrap() {
            return Either.left(this.key);
        }

        @Override
        public Optional<TagKey<T>> unwrapKey() {
            return Optional.of(this.key);
        }

        @Override
        public boolean contains(Holder<T> p_205834_) {
            return p_205834_.is(this.key);
        }

        @Override
        public String toString() {
            return "NamedSet(" + this.key + ")[" + this.contents + "]";
        }

        @Override
        public boolean canSerializeIn(HolderOwner<T> p_256542_) {
            return this.owner.canSerializeIn(p_256542_);
        }
    }
}