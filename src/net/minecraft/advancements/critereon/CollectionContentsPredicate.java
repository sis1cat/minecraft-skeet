package net.minecraft.advancements.critereon;

import com.mojang.serialization.Codec;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public interface CollectionContentsPredicate<T, P extends Predicate<T>> extends Predicate<Iterable<T>> {
    List<P> unpack();

    static <T, P extends Predicate<T>> Codec<CollectionContentsPredicate<T, P>> codec(Codec<P> pTestCodec) {
        return pTestCodec.listOf().xmap(CollectionContentsPredicate::of, CollectionContentsPredicate::unpack);
    }

    @SafeVarargs
    static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(P... pTests) {
        return of(List.of(pTests));
    }

    static <T, P extends Predicate<T>> CollectionContentsPredicate<T, P> of(List<P> pTests) {
        return (CollectionContentsPredicate<T, P>)(switch (pTests.size()) {
            case 0 -> new CollectionContentsPredicate.Zero();
            case 1 -> new CollectionContentsPredicate.Single(pTests.getFirst());
            default -> new CollectionContentsPredicate.Multiple(pTests);
        });
    }

    public static record Multiple<T, P extends Predicate<T>>(List<P> tests) implements CollectionContentsPredicate<T, P> {
        public boolean test(Iterable<T> pContents) {
            List<Predicate<T>> list = new ArrayList<>(this.tests);

            for (T t : pContents) {
                list.removeIf(p_331259_ -> p_331259_.test(t));
                if (list.isEmpty()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public List<P> unpack() {
            return this.tests;
        }
    }

    public static record Single<T, P extends Predicate<T>>(P test) implements CollectionContentsPredicate<T, P> {
        public boolean test(Iterable<T> pContents) {
            for (T t : pContents) {
                if (this.test.test(t)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public List<P> unpack() {
            return List.of(this.test);
        }
    }

    public static class Zero<T, P extends Predicate<T>> implements CollectionContentsPredicate<T, P> {
        public boolean test(Iterable<T> pContents) {
            return true;
        }

        @Override
        public List<P> unpack() {
            return List.of();
        }
    }
}