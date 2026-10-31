package cn.blockforge.generated.generatedmod.yujian;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Retains a mounted owner's control ticks across a temporarily non-ticking
 * entity section. This neither loads terrain nor grants movement through it. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class FlightRootTicking {
    @SubscribeEvent public static void endLevelTick(TickEvent.LevelTickEvent event){
        if(event.phase==TickEvent.Phase.END&&event.level instanceof ServerLevel level)tickMissing(level);
    }

    /** Normal ServerLevel ticks win; only a carrier missed this gameTime is supplemented.
     * tickNonPassenger also runs vanilla tickPassenger, so the player advances
     * with its vehicle rather than remaining frozen on a stale riding pose. */
    public static int tickMissing(ServerLevel level){
        int count=0;long now=level.getGameTime();
        for(ServerPlayer player:level.players()){
            if(!player.isAlive()||player.level()!=level
                    ||!(player.getVehicle() instanceof ControlledSwordEntity root)
                    ||root.isRemoved()||!root.isAddedToWorld()||!root.canUpdate()||root.level()!=level
                    ||!root.flightRoot()||root.getFirstPassenger()!=player||!player.getUUID().equals(root.ownerId)
                    ||root.lastServerTick()==now)continue;
            // Uses the unchanged production movement, permission and collision
            // checks. A missing getChunkNow/sweep result still brakes the flight.
            level.guardEntityTick(level::tickNonPassenger,root);count++;
        }
        return count;
    }
    private FlightRootTicking(){}
}
