package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.ClientPacketSink;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.Network;
import cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog;
import cn.blockforge.generated.generatedmod.acupoint.AcupointPackets;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class AcupointClient {
    private static void accept(AcupointPackets.State packet) {
        var client = Minecraft.getInstance();
        var data = client.player == null ? null : GeneratedMod.data(client.player);
        if (data == null) return;
        var progress = data.acupoints();
        if (!packet.documents().isEmpty()) {
            var catalog = AcupointCatalog.parse(packet.documents());
            if (!catalog.version().equals(packet.catalogVersion())) return;
            progress.useClientCatalog(catalog);
        }
        if (!progress.catalog().version().equals(packet.catalogVersion())) {
            send(AcupointPackets.Action.SNAPSHOT, "", "");
            return;
        }
        progress.load(packet.progress());
        if (client.screen instanceof AcupointScreen screen) screen.accept(packet);
    }

    static void send(AcupointPackets.Action action, String id, String version) {
        if (Minecraft.getInstance().getConnection() != null)
            Network.CHANNEL.sendToServer(new AcupointPackets.Request(action, id, version));
    }

    @SubscribeEvent
    public static void setup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> ClientPacketSink.acupointState = AcupointClient::accept);
    }

    private AcupointClient() {}
}
