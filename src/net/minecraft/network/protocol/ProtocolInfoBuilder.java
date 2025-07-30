package net.minecraft.network.protocol;

import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.annotation.Nullable;
import net.minecraft.network.ClientboundPacketListener;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.PacketListener;
import net.minecraft.network.ProtocolInfo;
import net.minecraft.network.ServerboundPacketListener;
import net.minecraft.network.codec.StreamCodec;

public class ProtocolInfoBuilder<T extends PacketListener, B extends ByteBuf> {
    final ConnectionProtocol protocol;
    final PacketFlow flow;
    private final List<ProtocolInfoBuilder.CodecEntry<T, ?, B>> codecs = new ArrayList<>();
    @Nullable
    private BundlerInfo bundlerInfo;

    public ProtocolInfoBuilder(ConnectionProtocol pProtocol, PacketFlow pFlow) {
        this.protocol = pProtocol;
        this.flow = pFlow;
    }

    public <P extends Packet<? super T>> ProtocolInfoBuilder<T, B> addPacket(PacketType<P> pType, StreamCodec<? super B, P> pSerializer) {
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(pType, pSerializer));
        return this;
    }

    public <P extends BundlePacket<? super T>, D extends BundleDelimiterPacket<? super T>> ProtocolInfoBuilder<T, B> withBundlePacket(
        PacketType<P> pType, Function<Iterable<Packet<? super T>>, P> pBundler, D pPacket
    ) {
        StreamCodec<ByteBuf, D> streamcodec = StreamCodec.unit(pPacket);
        PacketType<D> packettype = (PacketType)pPacket.type();
        this.codecs.add(new ProtocolInfoBuilder.CodecEntry<>(packettype, streamcodec));
        this.bundlerInfo = BundlerInfo.createForPacket(pType, pBundler, pPacket);
        return this;
    }

    StreamCodec<ByteBuf, Packet<? super T>> buildPacketCodec(Function<ByteBuf, B> pBufferFactory, List<ProtocolInfoBuilder.CodecEntry<T, ?, B>> pCodecs) {
        ProtocolCodecBuilder<ByteBuf, T> protocolcodecbuilder = new ProtocolCodecBuilder<>(this.flow);

        for (ProtocolInfoBuilder.CodecEntry<T, ?, B> codecentry : pCodecs) {
            codecentry.addToBuilder(protocolcodecbuilder, pBufferFactory);
        }

        return protocolcodecbuilder.build();
    }

    public ProtocolInfo<T> build(Function<ByteBuf, B> pBufferFactory) {
        return new ProtocolInfoBuilder.Implementation<>(this.protocol, this.flow, this.buildPacketCodec(pBufferFactory, this.codecs), this.bundlerInfo);
    }

    public ProtocolInfo.Unbound<T, B> buildUnbound() {
        final List<ProtocolInfoBuilder.CodecEntry<T, ?, B>> list = List.copyOf(this.codecs);
        final BundlerInfo bundlerinfo = this.bundlerInfo;
        return new ProtocolInfo.Unbound<T, B>() {
            @Override
            public ProtocolInfo<T> bind(Function<ByteBuf, B> p_343642_) {
                return new ProtocolInfoBuilder.Implementation<>(
                    ProtocolInfoBuilder.this.protocol, ProtocolInfoBuilder.this.flow, ProtocolInfoBuilder.this.buildPacketCodec(p_343642_, list), bundlerinfo
                );
            }

            @Override
            public ConnectionProtocol id() {
                return ProtocolInfoBuilder.this.protocol;
            }

            @Override
            public PacketFlow flow() {
                return ProtocolInfoBuilder.this.flow;
            }

            @Override
            public void listPackets(ProtocolInfo.Unbound.PacketVisitor p_343184_) {
                for (int i = 0; i < list.size(); i++) {
                    ProtocolInfoBuilder.CodecEntry<T, ?, B> codecentry = list.get(i);
                    p_343184_.accept(codecentry.type, i);
                }
            }
        };
    }

    private static <L extends PacketListener, B extends ByteBuf> ProtocolInfo.Unbound<L, B> protocol(
        ConnectionProtocol pProtocol, PacketFlow pFlow, Consumer<ProtocolInfoBuilder<L, B>> pSetup
    ) {
        ProtocolInfoBuilder<L, B> protocolinfobuilder = new ProtocolInfoBuilder<>(pProtocol, pFlow);
        pSetup.accept(protocolinfobuilder);
        return protocolinfobuilder.buildUnbound();
    }

    public static <T extends ServerboundPacketListener, B extends ByteBuf> ProtocolInfo.Unbound<T, B> serverboundProtocol(
        ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B>> pSetup
    ) {
        return protocol(pProtocol, PacketFlow.SERVERBOUND, pSetup);
    }

    public static <T extends ClientboundPacketListener, B extends ByteBuf> ProtocolInfo.Unbound<T, B> clientboundProtocol(
        ConnectionProtocol pProtocol, Consumer<ProtocolInfoBuilder<T, B>> pSetup
    ) {
        return protocol(pProtocol, PacketFlow.CLIENTBOUND, pSetup);
    }

    static record CodecEntry<T extends PacketListener, P extends Packet<? super T>, B extends ByteBuf>(
        PacketType<P> type, StreamCodec<? super B, P> serializer
    ) {
        public void addToBuilder(ProtocolCodecBuilder<ByteBuf, T> pCodecBuilder, Function<ByteBuf, B> pBufferFactory) {
            StreamCodec<ByteBuf, P> streamcodec = this.serializer.mapStream(pBufferFactory);
            pCodecBuilder.add(this.type, streamcodec);
        }
    }

    static record Implementation<L extends PacketListener>(
        ConnectionProtocol id, PacketFlow flow, StreamCodec<ByteBuf, Packet<? super L>> codec, @Nullable BundlerInfo bundlerInfo
    ) implements ProtocolInfo<L> {
        @Nullable
        @Override
        public BundlerInfo bundlerInfo() {
            return this.bundlerInfo;
        }

        @Override
        public ConnectionProtocol id() {
            return this.id;
        }

        @Override
        public PacketFlow flow() {
            return this.flow;
        }

        @Override
        public StreamCodec<ByteBuf, Packet<? super L>> codec() {
            return this.codec;
        }
    }
}