package cn.blockforge.generated.generatedmod.flight;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Readiness checks never wait for a future; three expiring look-ahead tickets per flyer. */
public final class FlightTerrainStreaming {
    private static final TicketType<UUID> TICKET=TicketType.create("xiantu_flight_ahead",Comparator.<UUID>naturalOrder(),40);
    private record Prefetch(ServerLevel level,Set<ChunkPos> chunks,long tick){}
    private static final Map<ServerPlayer,Prefetch> PREFETCH=new WeakHashMap<>();
    public static boolean available(Player player,AABB box){
        if(!Double.isFinite(box.minX)||!Double.isFinite(box.minZ)||!Double.isFinite(box.maxX)||!Double.isFinite(box.maxZ)
                ||!player.level().getWorldBorder().isWithinBounds(box))return false;
        int x0=net.minecraft.util.Mth.floor(box.minX)>>4,x1=net.minecraft.util.Mth.floor(box.maxX)>>4;
        int z0=net.minecraft.util.Mth.floor(box.minZ)>>4,z1=net.minecraft.util.Mth.floor(box.maxZ)>>4;
        if(x1-x0>8||z1-z0>8)return false;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++){
            if(player instanceof ServerPlayer server){if(server.serverLevel().getChunkSource().getChunkNow(x,z)==null)return false;}
            else if(player.level().getChunkSource().getChunk(x,z,ChunkStatus.FULL,false)==null)return false;
        }
        return true;
    }
    /** Vanilla still handles solid collision. This only clips movement to ready terrain. */
    public static Vec3 limit(Player player,AABB body,Vec3 requested){
        if(!Double.isFinite(requested.lengthSqr()))return Vec3.ZERO;
        if(available(player,body.expandTowards(requested).inflate(1)))return requested;
        if(!available(player,body.inflate(1)))return Vec3.ZERO;
        double low=0,high=1;
        for(int i=0;i<10;i++){
            double mid=(low+high)*.5;
            if(available(player,body.expandTowards(requested.scale(mid)).inflate(1)))low=mid;else high=mid;
        }
        return requested.scale(low);
    }
    public static void prefetch(ServerPlayer player,Vec3 from,Vec3 velocity){
        if(velocity.horizontalDistanceSqr()<.01||!Double.isFinite(velocity.lengthSqr()))return;
        var level=player.serverLevel();long now=level.getGameTime();var old=PREFETCH.get(player);
        if(old!=null&&old.level()==level&&now>=old.tick()&&now-old.tick()<10)return;
        Vec3 direction=new Vec3(velocity.x,0,velocity.z).normalize();
        Set<ChunkPos> next=new HashSet<>();
        double horizon=Math.max(32,Math.min(96,velocity.horizontalDistance()*20));
        for(int i=1;i<=3;i++){
            Vec3 p=from.add(direction.scale(horizon*i/3));var chunk=new ChunkPos(net.minecraft.core.BlockPos.containing(p));
            if(level.getWorldBorder().isWithinBounds(chunk.getWorldPosition()))next.add(chunk);
        }
        // Removing old tickets bounds outstanding work instead of accumulating a long flight corridor.
        if(old!=null)for(var chunk:old.chunks())if(old.level()!=level||!next.contains(chunk))old.level().getChunkSource().removeRegionTicket(TICKET,chunk,0,player.getUUID());
        for(var chunk:next)level.getChunkSource().addRegionTicket(TICKET,chunk,0,player.getUUID());
        PREFETCH.put(player,new Prefetch(level,next,now));
    }
    private FlightTerrainStreaming(){}
}
