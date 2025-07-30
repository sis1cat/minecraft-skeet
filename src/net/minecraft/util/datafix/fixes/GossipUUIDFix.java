package net.minecraft.util.datafix.fixes;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixUtils;
import com.mojang.datafixers.Typed;
import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.stream.Stream;

public class GossipUUIDFix extends NamedEntityFix {
    public GossipUUIDFix(Schema pOutputSchema, String pEntityName) {
        super(pOutputSchema, false, "Gossip for for " + pEntityName, References.ENTITY, pEntityName);
    }

    @Override
    protected Typed<?> fix(Typed<?> p_15881_) {
        return p_15881_.update(
            DSL.remainderFinder(),
            p_15883_ -> p_15883_.update(
                    "Gossips",
                    p_326594_ -> DataFixUtils.orElse(
                            p_326594_.asStreamOpt()
                                .result()
                                .map(
                                    p_145374_ -> p_145374_.map(
                                            p_145378_ -> AbstractUUIDFix.replaceUUIDLeastMost((Dynamic<?>)p_145378_, "Target", "Target").orElse((Dynamic<?>)p_145378_)
                                        )
                                )
                                .map(p_326594_::createList),
                            p_326594_
                        )
                )
        );
    }
}