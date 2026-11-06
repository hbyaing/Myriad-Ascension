package cn.blockforge.generated.generatedmod.acupoint;

import cn.blockforge.generated.generatedmod.ClientPacketSink;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public final class AcupointPackets {
    public enum Action { SNAPSHOT, SELECT, TAP, START, RENEW, STOP, AUTO_ON, AUTO_OFF }

    public record Request(Action action, String id, String catalogVersion) {
        public static void encode(Request request, FriendlyByteBuf buffer) {
            buffer.writeEnum(request.action);
            buffer.writeUtf(request.id, 12);
            buffer.writeUtf(request.catalogVersion, 36);
        }
        public static Request decode(FriendlyByteBuf buffer) {
            return new Request(buffer.readEnum(Action.class), buffer.readUtf(12), buffer.readUtf(36));
        }
        public static void handle(Request request, Supplier<NetworkEvent.Context> context) {
            // NetworkChannel routes all C2S handlers onto the logical server thread.
            var sender = context.get().getSender();
            if (sender != null) AcupointService.handle(sender, request);
        }
    }

    public record State(CompoundTag progress, String catalogVersion, List<String> documents,
                        String activePoint, AcupointProgress.Result result) {
        public static void encode(State state, FriendlyByteBuf buffer) {
            buffer.writeNbt(state.progress);
            buffer.writeUtf(state.catalogVersion, 36);
            buffer.writeBoolean(!state.documents.isEmpty());
            for (String document : state.documents) buffer.writeByteArray(document.getBytes(StandardCharsets.UTF_8));
            buffer.writeUtf(state.activePoint, 12);
            buffer.writeEnum(state.result);
        }
        public static State decode(FriendlyByteBuf buffer) {
            CompoundTag progress = buffer.readNbt();
            String version = buffer.readUtf(36);
            List<String> documents = new ArrayList<>(4);
            if (buffer.readBoolean()) {
                for (int i = 0; i < 4; i++)
                    documents.add(new String(buffer.readByteArray(AcupointCatalog.FILE_LIMIT), StandardCharsets.UTF_8));
            }
            return new State(progress == null ? new CompoundTag() : progress, version, List.copyOf(documents),
                    buffer.readUtf(12), buffer.readEnum(AcupointProgress.Result.class));
        }
        public static void handle(State packet, Supplier<NetworkEvent.Context> context) {
            var net = context.get();
            net.enqueueWork(() -> ClientPacketSink.safe(ClientPacketSink.acupointState, packet));
            net.setPacketHandled(true);
        }
    }

    private AcupointPackets() {}
}
