package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelOutboundHandler;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.util.ReferenceCountUtil;
import net.minecraft.network.protocol.Packet;

public class UnconfiguredPipelineHandler {
    public static <T extends PacketListener> UnconfiguredPipelineHandler.InboundConfigurationTask setupInboundProtocol(ProtocolInfo<T> pProtocolInfo) {
        return setupInboundHandler(new PacketDecoder<>(pProtocolInfo));
    }

    private static UnconfiguredPipelineHandler.InboundConfigurationTask setupInboundHandler(ChannelInboundHandler pHandler) {
        return p_331657_ -> {
            p_331657_.pipeline().replace(p_331657_.name(), "decoder", pHandler);
            p_331657_.channel().config().setAutoRead(true);
        };
    }

    public static <T extends PacketListener> UnconfiguredPipelineHandler.OutboundConfigurationTask setupOutboundProtocol(ProtocolInfo<T> pProtocolInfo) {
        return setupOutboundHandler(new PacketEncoder<>(pProtocolInfo));
    }

    private static UnconfiguredPipelineHandler.OutboundConfigurationTask setupOutboundHandler(ChannelOutboundHandler pHandler) {
        return p_329768_ -> p_329768_.pipeline().replace(p_329768_.name(), "encoder", pHandler);
    }

    public static class Inbound extends ChannelDuplexHandler {
        @Override
        public void channelRead(ChannelHandlerContext pContext, Object pMessage) {
            if (!(pMessage instanceof ByteBuf) && !(pMessage instanceof Packet)) {
                pContext.fireChannelRead(pMessage);
            } else {
                ReferenceCountUtil.release(pMessage);
                throw new DecoderException("Pipeline has no inbound protocol configured, can't process packet " + pMessage);
            }
        }

        @Override
        public void write(ChannelHandlerContext pContext, Object pMessage, ChannelPromise pPromise) throws Exception {
            if (pMessage instanceof UnconfiguredPipelineHandler.InboundConfigurationTask unconfiguredpipelinehandler$inboundconfigurationtask) {
                try {
                    unconfiguredpipelinehandler$inboundconfigurationtask.run(pContext);
                } finally {
                    ReferenceCountUtil.release(pMessage);
                }

                pPromise.setSuccess();
            } else {
                pContext.write(pMessage, pPromise);
            }
        }
    }

    @FunctionalInterface
    public interface InboundConfigurationTask {
        void run(ChannelHandlerContext pContext);

        default UnconfiguredPipelineHandler.InboundConfigurationTask andThen(UnconfiguredPipelineHandler.InboundConfigurationTask pTask) {
            return p_334974_ -> {
                this.run(p_334974_);
                pTask.run(p_334974_);
            };
        }
    }

    public static class Outbound extends ChannelOutboundHandlerAdapter {
        @Override
        public void write(ChannelHandlerContext pContext, Object pMessage, ChannelPromise pPromise) throws Exception {
            if (pMessage instanceof Packet) {
                ReferenceCountUtil.release(pMessage);
                throw new EncoderException("Pipeline has no outbound protocol configured, can't process packet " + pMessage);
            } else {
                if (pMessage instanceof UnconfiguredPipelineHandler.OutboundConfigurationTask unconfiguredpipelinehandler$outboundconfigurationtask) {
                    try {
                        unconfiguredpipelinehandler$outboundconfigurationtask.run(pContext);
                    } finally {
                        ReferenceCountUtil.release(pMessage);
                    }

                    pPromise.setSuccess();
                } else {
                    pContext.write(pMessage, pPromise);
                }
            }
        }
    }

    @FunctionalInterface
    public interface OutboundConfigurationTask {
        void run(ChannelHandlerContext pContext);

        default UnconfiguredPipelineHandler.OutboundConfigurationTask andThen(UnconfiguredPipelineHandler.OutboundConfigurationTask pTask) {
            return p_334875_ -> {
                this.run(p_334875_);
                pTask.run(p_334875_);
            };
        }
    }
}