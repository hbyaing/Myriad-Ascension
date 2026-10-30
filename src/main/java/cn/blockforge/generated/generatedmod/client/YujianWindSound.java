package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** One local wind loop following authoritative speed and roll, with a short fade on exit. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class YujianWindSound extends AbstractTickableSoundInstance {
    private static YujianWindSound current;
    private final ControlledSwordEntity sword;

    private YujianWindSound(ControlledSwordEntity sword) {
        super(SoundEvents.ELYTRA_FLYING, SoundSource.PLAYERS, RandomSource.create());
        this.sword = sword;
        looping = true;
        delay = 0;
        volume = .001F;
    }

    @Override public boolean canStartSilent() { return true; }
    @Override public void tick() {
        var client = Minecraft.getInstance();
        boolean riding = client.player != null && client.level == sword.level() && !sword.isRemoved()
                && client.player.getVehicle() == sword;
        float roll = Mth.clamp(Math.abs(sword.angularSpeed()) * 4, 0, 1);
        float wanted = riding ? (float) (FlyingSwordConfig.WIND_VOLUME.get()
                * Mth.clamp(sword.flightSpeed(), 0, 1) * (.55 + .45 * roll)) : 0;
        volume = Mth.lerp(.2F, volume, wanted);
        pitch = Mth.lerp(.15F, pitch, .85F + .3F * roll + .35F*sword.huahongStrength());
        x = sword.getX(); y = sword.getY(); z = sword.getZ();
        if (!riding && volume < .005F) stop();
    }

    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            if (current != null) current.stop();
            current = null;
            return;
        }
        if (client.player.getVehicle() instanceof ControlledSwordEntity sword && sword.flightRoot()
                && (current == null || current.sword != sword || current.isStopped())) {
            if (current != null) current.stop();
            current = new YujianWindSound(sword);
            client.getSoundManager().play(current);
        }
    }
}
