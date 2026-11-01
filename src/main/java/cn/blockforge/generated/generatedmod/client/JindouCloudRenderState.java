package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Shared by skin, armor, cloud and trail; never changes the player's authoritative state. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class JindouCloudRenderState {
    private static final Map<UUID, Long> VISIBLE_UNTIL = new HashMap<>();
    private static ClientLevel world;

    private JindouCloudRenderState() {}

    static void checkWorld() {
        ClientLevel current = Minecraft.getInstance().level;
        if (world == current) return;
        VISIBLE_UNTIL.clear();
        world = current;
    }

    public static void update(UUID player, boolean visible) {
        checkWorld();
        if (world == null) return;
        if (visible) VISIBLE_UNTIL.put(player, world.getGameTime() + 12);
        else {
            VISIBLE_UNTIL.remove(player);
            JindouCloudTrailCache.remove(player);
        }
    }

    public static boolean isActive(Player player) {
        checkWorld();
        if (world == null || player.level() != world || !player.isAlive() || player.isSpectator()) return false;
        if (player == Minecraft.getInstance().player) {
            var data = GeneratedMod.data(player);
            return data != null && "jin_dou_yun".equals(data.wuxingxiantu$getActiveDivineArt());
        }
        return VISIBLE_UNTIL.getOrDefault(player.getUUID(), Long.MIN_VALUE) >= world.getGameTime();
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        checkWorld();
        if (world != null) VISIBLE_UNTIL.values().removeIf(until -> until < world.getGameTime());
    }
}
