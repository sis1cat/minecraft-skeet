package net.minecraft.network.chat;

import com.mojang.logging.LogUtils;
import java.time.Instant;
import java.util.UUID;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import net.minecraft.util.SignatureUpdater;
import net.minecraft.util.SignatureValidator;
import net.minecraft.util.Signer;
import net.minecraft.world.entity.player.ProfilePublicKey;
import org.slf4j.Logger;

public class SignedMessageChain {
    static final Logger LOGGER = LogUtils.getLogger();
    @Nullable
    SignedMessageLink nextLink;
    Instant lastTimeStamp = Instant.EPOCH;

    public SignedMessageChain(UUID pSender, UUID pSessionId) {
        this.nextLink = SignedMessageLink.root(pSender, pSessionId);
    }

    public SignedMessageChain.Encoder encoder(Signer pSigner) {
        return p_326076_ -> {
            SignedMessageLink signedmessagelink = this.nextLink;
            if (signedmessagelink == null) {
                return null;
            } else {
                this.nextLink = signedmessagelink.advance();
                return new MessageSignature(pSigner.sign(p_248065_ -> PlayerChatMessage.updateSignature(p_248065_, signedmessagelink, p_326076_)));
            }
        };
    }

    public SignedMessageChain.Decoder decoder(final ProfilePublicKey pPublicKey) {
        final SignatureValidator signaturevalidator = pPublicKey.createSignatureValidator();
        return new SignedMessageChain.Decoder() {
            @Override
            public PlayerChatMessage unpack(@Nullable MessageSignature p_328199_, SignedMessageBody p_328915_) throws SignedMessageChain.DecodeException {
                if (p_328199_ == null) {
                    throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.MISSING_PROFILE_KEY);
                } else if (pPublicKey.data().hasExpired()) {
                    throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.EXPIRED_PROFILE_KEY);
                } else {
                    SignedMessageLink signedmessagelink = SignedMessageChain.this.nextLink;
                    if (signedmessagelink == null) {
                        throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.CHAIN_BROKEN);
                    } else if (p_328915_.timeStamp().isBefore(SignedMessageChain.this.lastTimeStamp)) {
                        this.setChainBroken();
                        throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.OUT_OF_ORDER_CHAT);
                    } else {
                        SignedMessageChain.this.lastTimeStamp = p_328915_.timeStamp();
                        PlayerChatMessage playerchatmessage = new PlayerChatMessage(signedmessagelink, p_328199_, p_328915_, null, FilterMask.PASS_THROUGH);
                        if (!playerchatmessage.verify(signaturevalidator)) {
                            this.setChainBroken();
                            throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.INVALID_SIGNATURE);
                        } else {
                            if (playerchatmessage.hasExpiredServer(Instant.now())) {
                                SignedMessageChain.LOGGER
                                    .warn("Received expired chat: '{}'. Is the client/server system time unsynchronized?", p_328915_.content());
                            }

                            SignedMessageChain.this.nextLink = signedmessagelink.advance();
                            return playerchatmessage;
                        }
                    }
                }
            }

            @Override
            public void setChainBroken() {
                SignedMessageChain.this.nextLink = null;
            }
        };
    }

    public static class DecodeException extends ThrowingComponent {
        static final Component MISSING_PROFILE_KEY = Component.translatable("chat.disabled.missingProfileKey");
        static final Component CHAIN_BROKEN = Component.translatable("chat.disabled.chain_broken");
        static final Component EXPIRED_PROFILE_KEY = Component.translatable("chat.disabled.expiredProfileKey");
        static final Component INVALID_SIGNATURE = Component.translatable("chat.disabled.invalid_signature");
        static final Component OUT_OF_ORDER_CHAT = Component.translatable("chat.disabled.out_of_order_chat");

        public DecodeException(Component p_249149_) {
            super(p_249149_);
        }
    }

    @FunctionalInterface
    public interface Decoder {
        static SignedMessageChain.Decoder unsigned(UUID pId, BooleanSupplier pShouldEnforceSecureProfile) {
            return (p_326079_, p_326080_) -> {
                if (pShouldEnforceSecureProfile.getAsBoolean()) {
                    throw new SignedMessageChain.DecodeException(SignedMessageChain.DecodeException.MISSING_PROFILE_KEY);
                } else {
                    return PlayerChatMessage.unsigned(pId, p_326080_.content());
                }
            };
        }

        PlayerChatMessage unpack(@Nullable MessageSignature pSignature, SignedMessageBody pBody) throws SignedMessageChain.DecodeException;

        default void setChainBroken() {
        }
    }

    @FunctionalInterface
    public interface Encoder {
        SignedMessageChain.Encoder UNSIGNED = p_250548_ -> null;

        @Nullable
        MessageSignature pack(SignedMessageBody pBody);
    }
}