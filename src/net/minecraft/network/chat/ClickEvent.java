package net.minecraft.network.chat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Lifecycle;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.mojang.serialization.codecs.RecordCodecBuilder.Instance;
import net.minecraft.util.StringRepresentable;

public class ClickEvent {
    public static final Codec<ClickEvent> CODEC = RecordCodecBuilder.create(
        p_311166_ -> p_311166_.group(
                    ClickEvent.Action.CODEC.forGetter(p_313238_ -> p_313238_.action),
                    Codec.STRING.fieldOf("value").forGetter(p_312346_ -> p_312346_.value)
                )
                .apply(p_311166_, ClickEvent::new)
    );
    private final ClickEvent.Action action;
    private final String value;

    public ClickEvent(ClickEvent.Action pAction, String pValue) {
        this.action = pAction;
        this.value = pValue;
    }

    public ClickEvent.Action getAction() {
        return this.action;
    }

    public String getValue() {
        return this.value;
    }

    @Override
    public boolean equals(Object pOther) {
        if (this == pOther) {
            return true;
        } else if (pOther != null && this.getClass() == pOther.getClass()) {
            ClickEvent clickevent = (ClickEvent)pOther;
            return this.action == clickevent.action && this.value.equals(clickevent.value);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "ClickEvent{action=" + this.action + ", value='" + this.value + "'}";
    }

    @Override
    public int hashCode() {
        int i = this.action.hashCode();
        return 31 * i + this.value.hashCode();
    }

    public static enum Action implements StringRepresentable {
        OPEN_URL("open_url", true),
        OPEN_FILE("open_file", false),
        RUN_COMMAND("run_command", true),
        SUGGEST_COMMAND("suggest_command", true),
        CHANGE_PAGE("change_page", true),
        COPY_TO_CLIPBOARD("copy_to_clipboard", true);

        public static final MapCodec<ClickEvent.Action> UNSAFE_CODEC = StringRepresentable.fromEnum(ClickEvent.Action::values).fieldOf("action");
        public static final MapCodec<ClickEvent.Action> CODEC = UNSAFE_CODEC.validate(ClickEvent.Action::filterForSerialization);
        private final boolean allowFromServer;
        private final String name;

        private Action(final String pName, final boolean pAllowFromServer) {
            this.name = pName;
            this.allowFromServer = pAllowFromServer;
        }

        public boolean isAllowedFromServer() {
            return this.allowFromServer;
        }

        @Override
        public String getSerializedName() {
            return this.name;
        }

        public static DataResult<ClickEvent.Action> filterForSerialization(ClickEvent.Action pAction) {
            return !pAction.isAllowedFromServer() ? DataResult.error(() -> "Action not allowed: " + pAction) : DataResult.success(pAction, Lifecycle.stable());
        }
    }
}