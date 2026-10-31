package cn.blockforge.generated.generatedmod.yujian;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.datafixers.util.Either;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.TicketType;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Bounded server-side route streaming. Collision readiness remains authoritative.
 * No synchronous chunk access, waiting, global view-distance changes or forced chunks. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class FlightChunkStreaming {
    public static final int MAX_RIDER_TICKETS=96,MAX_LEVEL_TICKETS=256,MAX_TOTAL_TICKETS=512;
    /** Direct cold destination chunks, not vanilla's neighbouring generation/light dependencies. */
    public static final int MAX_RIDER_REQUESTS=8,MAX_TOTAL_REQUESTS=24;
    public static final int MAX_NEW_RIDER_TICKETS_PER_TICK=8,MAX_NEW_TICKETS_PER_TICK=16;
    public static final int TICKET_TIMEOUT=100,REFRESH_TICKS=20,STALE_RIDER_TICKS=4;
    public static final double MAX_ROUTE_DISTANCE=768;
    public static final double MAX_READINESS_DISTANCE=2048;
    public record Readiness(double readyDistance,double lookAheadDistance,double brakingDistance,double suggestedSpeed,boolean complete){}
    private static final TicketType<UUID> TICKET=TicketType.create("wuxingxiantu_flight_route",Comparator.<UUID>naturalOrder(),TICKET_TIMEOUT);
    private static final Map<ServerLevel,LevelState> LEVELS=new IdentityHashMap<>();
    private static final Map<UUID,Rider> RIDERS=new HashMap<>();
    private static final Map<MinecraftServer,Budget> BUDGETS=new IdentityHashMap<>();
    private static final Map<MinecraftServer,ExecutorService> DISPATCHERS=new IdentityHashMap<>();

    private static final class Lease {
        final ChunkPos pos;
        long refreshed,retryAt;
        Lease(ChunkPos pos,long now){this.pos=pos;refreshed=now;}
    }
    private record Job(UUID owner,CompletableFuture<Either<ChunkAccess,ChunkHolder.ChunkLoadingFailure>> future){}
    private static final class LevelState {
        final ServerLevel level;
        final Map<Long,Job> jobs=new HashMap<>();
        long requests,completed,failed,budgetDeferrals;
        LevelState(ServerLevel level){this.level=level;}
    }
    private static final class Rider {
        final UUID id;
        final LevelState state;
        final LinkedHashMap<Long,Lease> leases=new LinkedHashMap<>();
        List<ChunkPos> plan=List.of();
        long updated,lastPlan=Long.MIN_VALUE,lastIssueTick=Long.MIN_VALUE;
        int issued;
        float desiredYaw,desiredPitch;
        long chunk=Long.MIN_VALUE;
        double desiredSpeed;
        boolean braking;
        Readiness readiness=new Readiness(0,0,0,0,false);
        Rider(UUID id,LevelState state){this.id=id;this.state=state;}
    }
    private static final class Budget {
        int tick=Integer.MIN_VALUE,issued,requests;
    }

    /** Call on the server thread after this tick's velocity/steering calculation,
     * before the unchanged collision sweep. targetPitch is already response-scaled. */
    public static void update(ServerPlayer player,ControlledSwordEntity sword,Vec3 plannedVelocity,
                              float targetYaw,float targetPitch,double turnFraction,double acceleration,
                              double targetSpeed,boolean braking){
        var level=player.serverLevel();requireServerThread(level);
        if(!valid(player,sword)||!finite(plannedVelocity)||!Float.isFinite(targetYaw)||!Float.isFinite(targetPitch)
                ||!Double.isFinite(turnFraction)||!Double.isFinite(acceleration)||!Double.isFinite(targetSpeed)){
            release(player);return;
        }
        double speed=plannedVelocity.length();
        if(braking&&targetSpeed<=.025&&speed<.025){release(player);return;}
        Rider rider=RIDERS.get(player.getUUID());
        if(rider!=null&&rider.state.level!=level){release(player);rider=null;}
        if(rider==null){var state=LEVELS.computeIfAbsent(level,LevelState::new);rider=new Rider(player.getUUID(),state);RIDERS.put(rider.id,rider);}
        long now=level.getGameTime();rider.updated=now;
        if(rider.lastPlan==Long.MIN_VALUE||now-rider.lastPlan>=2||now<rider.lastPlan
                ||rider.chunk!=sword.chunkPosition().toLong()||Math.abs(Mth.wrapDegrees(targetYaw-rider.desiredYaw))>5
                ||Math.abs(targetPitch-rider.desiredPitch)>5||targetSpeed>rider.desiredSpeed+1||braking!=rider.braking){
            rider.lastPlan=now;rider.chunk=sword.chunkPosition().toLong();rider.desiredYaw=targetYaw;
            rider.desiredPitch=targetPitch;rider.desiredSpeed=targetSpeed;rider.braking=braking;
            rider.plan=preview(sword.position(),plannedVelocity,sword.getYRot(),sword.getXRot(),targetYaw,targetPitch,turnFraction,acceleration,targetSpeed,braking)
                    .stream().filter(level.getWorldBorder()::isWithinBounds).toList();
        }
        reap(rider.state);
        retainAndRequest(rider,now);
    }

    /** Pure route planner for regression checks and diagnostics. Every sampled
     * segment is <=8 blocks and includes its 3x3 chunk neighbourhood, covering
     * the swept rider/blade and collision's one-block margin at chunk edges. */
    public static List<ChunkPos> preview(Vec3 origin,Vec3 velocity,float yaw,float pitch,float targetYaw,
                                         float targetPitch,double turnFraction,double acceleration,
                                         double targetSpeed,boolean braking){
        if(!finite(origin)||!finite(velocity)||!Float.isFinite(yaw)||!Float.isFinite(pitch)
                ||!Float.isFinite(targetYaw)||!Float.isFinite(targetPitch)||!Double.isFinite(targetSpeed)
                ||!Double.isFinite(turnFraction)||!Double.isFinite(acceleration))return List.of();
        double speed=Math.max(velocity.length(),Math.max(0,targetSpeed));
        if(!Double.isFinite(speed))return List.of();
        double horizon=Mth.clamp(16+speed*.8,24,48),dt=Math.min(1,8/Math.max(1,speed));
        boolean zeroTarget=braking&&targetSpeed<=.0001;
        double steer=Mth.clamp(turnFraction,.03,.35),accel=Mth.clamp(zeroTarget?Math.max(.18,acceleration):acceleration,.01,1);
        Map<Long,Double> priorities=new HashMap<>();addNeighbourhood(priorities,origin,0);
        float momentumYaw=velocity.horizontalDistanceSqr()>.0001?(float)Math.toDegrees(Math.atan2(-velocity.x,velocity.z)):yaw;
        float momentumPitch=velocity.lengthSqr()>.0001?(float)-Math.toDegrees(Math.atan2(velocity.y,velocity.horizontalDistance())):pitch;
        // Main steered path, existing momentum/braking envelope, and reachable
        // left/right alternatives. Higher cost keeps the real route first.
        for(int branch=0;branch<(zeroTarget?1:4);branch++){
            Vec3 p=origin,v=velocity;float heading=yaw,tilt=pitch;double travelled=0;
            float desired=branch==0?targetYaw:branch==1?momentumYaw:yaw+(branch==2?-90:90);
            float vertical=branch==1?momentumPitch:targetPitch;
            double weight=branch==0?1:branch==1?1.6:2.5;
            for(int step=0;step<128&&step*dt<horizon&&travelled<MAX_ROUTE_DISTANCE;step++){
                // The first displacement is the actual velocity supplied by the
                // controller, not an instantaneous heading replacement.
                if(step>0){
                    heading+=Mth.wrapDegrees(desired-heading)*(float)(1-Math.pow(1-steer,dt));
                    tilt+=(Mth.clamp(vertical,-70,70)-tilt)*(float)(1-Math.pow(.86,dt));
                    Vec3 target=direction(heading,tilt).scale(zeroTarget?0:Math.max(0,targetSpeed));
                    v=v.lerp(target,1-Math.pow(1-accel,dt));
                }
                double distance=v.length()*dt;
                if(!Double.isFinite(distance)||distance<.005)break;
                double scale=Math.min(dt,(MAX_ROUTE_DISTANCE-travelled)/Math.max(.0001,v.length()));
                p=p.add(v.scale(scale));travelled+=v.length()*scale;
                if(!finite(p))break;
                addNeighbourhood(priorities,p,(step+1)*dt*weight);
            }
        }
        return priorities.entrySet().stream().sorted(Map.Entry.<Long,Double>comparingByValue().thenComparingLong(Map.Entry::getKey))
                .limit(MAX_RIDER_TICKETS).map(e->new ChunkPos(e.getKey())).toList();
    }
    private static void addNeighbourhood(Map<Long,Double> candidates,Vec3 p,double priority){
        int cx=Mth.floor(p.x)>>4,cz=Mth.floor(p.z)>>4;
        for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
            candidates.merge(ChunkPos.asLong(cx+dx,cz+dz),priority+.05*(Math.abs(dx)+Math.abs(dz)),Math::min);
    }
    private static Vec3 direction(float yaw,float pitch){double y=Math.toRadians(yaw),p=Math.toRadians(pitch);return new Vec3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));}
    private static boolean finite(Vec3 v){return v!=null&&Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z)&&Double.isFinite(v.lengthSqr());}

    private static void retainAndRequest(Rider rider,long now){
        LevelState state=rider.state;var level=state.level;var cache=level.getChunkSource();
        int levelRiders=(int)RIDERS.values().stream().filter(r->r.state==state).count();
        int quota=Math.min(MAX_RIDER_TICKETS,Math.min(MAX_LEVEL_TICKETS/Math.max(1,levelRiders),MAX_TOTAL_TICKETS/Math.max(1,RIDERS.size())));
        Set<Long> wanted=new HashSet<>();for(var p:rider.plan){if(wanted.size()>=quota)break;wanted.add(p.toLong());}
        for(var it=rider.leases.entrySet().iterator();it.hasNext();){var entry=it.next();if(!wanted.contains(entry.getKey())){cache.removeRegionTicket(TICKET,entry.getValue().pos,0,rider.id);it.remove();}}
        Budget budget=BUDGETS.computeIfAbsent(level.getServer(),s->new Budget());int serverTick=level.getServer().getTickCount();
        if(budget.tick!=serverTick){budget.tick=serverTick;budget.issued=0;budget.requests=0;}
        if(rider.lastIssueTick!=serverTick){rider.lastIssueTick=serverTick;rider.issued=0;}
        for(var pos:rider.plan){
            long key=pos.toLong();if(!wanted.contains(key))continue;
            Lease lease=rider.leases.get(key);
            if(lease==null){
                if(rider.issued>=MAX_NEW_RIDER_TICKETS_PER_TICK||budget.issued>=MAX_NEW_TICKETS_PER_TICK
                        ||levelTicketCount(state)>=MAX_LEVEL_TICKETS||totalTicketCount()>=MAX_TOTAL_TICKETS){state.budgetDeferrals++;continue;}
                // A FULL ticket itself starts generation during vanilla distance
                // updates. Admit cold leases against the work budget BEFORE
                // adding a ticket, not merely when tracking its explicit future.
                if(cache.getChunkNow(pos.x,pos.z)==null&&(coldDestinations(rider.id)>=MAX_RIDER_REQUESTS
                        ||!state.jobs.containsKey(key)&&coldDestinations(null)>=MAX_TOTAL_REQUESTS
                        ||!state.jobs.containsKey(key)&&budget.requests>=MAX_NEW_TICKETS_PER_TICK)){state.budgetDeferrals++;continue;}
                cache.addRegionTicket(TICKET,pos,0,rider.id);lease=new Lease(pos,now);rider.leases.put(key,lease);rider.issued++;budget.issued++;
            }else if(now-lease.refreshed>=REFRESH_TICKS||now<lease.refreshed){cache.addRegionTicket(TICKET,pos,0,rider.id);lease.refreshed=now;}
            if(cache.getChunkNow(pos.x,pos.z)!=null||state.jobs.containsKey(key)||now<lease.retryAt)continue;
            if(totalRequestCount()>=MAX_TOTAL_REQUESTS||riderRequestCount(rider.id)>=MAX_RIDER_REQUESTS||budget.requests>=MAX_NEW_TICKETS_PER_TICK){state.budgetDeferrals++;continue;}
            // Important 1.20.1 detail: getChunkFuture also managedBlocks when
            // called ON the server thread. Its public off-thread path marshals
            // getChunkFutureMainThread onto the server executor and composes the
            // result without waiting. The adapter calls only that thread-safe
            // vanilla entry; all world reads/tickets/budgets stay on this thread.
            ExecutorService dispatcher=DISPATCHERS.computeIfAbsent(level.getServer(),server->Executors.newSingleThreadExecutor(task->{var thread=new Thread(task,"Flight chunk request dispatch");thread.setDaemon(true);return thread;}));
            var future=CompletableFuture.supplyAsync(()->cache.getChunkFuture(pos.x,pos.z,ChunkStatus.FULL,true),dispatcher).thenCompose(result->result);
            state.jobs.put(key,new Job(rider.id,future));state.requests++;budget.requests++;
        }
    }
    private static void reap(LevelState state){
        for(var it=state.jobs.entrySet().iterator();it.hasNext();){var entry=it.next();var future=entry.getValue().future();if(!future.isDone())continue;
            boolean failed=future.isCancelled()||future.isCompletedExceptionally();
            if(!failed){var result=future.getNow(null);failed=result==null||result.left().isEmpty();}
            if(failed){state.failed++;long retry=state.level.getGameTime()+20;for(var rider:RIDERS.values())if(rider.state==state){var lease=rider.leases.get(entry.getKey());if(lease!=null)lease.retryAt=retry;}}
            else state.completed++;
            it.remove();
        }
    }
    private static int levelTicketCount(LevelState state){return RIDERS.values().stream().filter(r->r.state==state).mapToInt(r->r.leases.size()).sum();}
    private static int totalTicketCount(){return RIDERS.values().stream().mapToInt(r->r.leases.size()).sum();}
    private static int totalRequestCount(){return LEVELS.values().stream().mapToInt(s->s.jobs.size()).sum();}
    private static long riderRequestCount(UUID id){return LEVELS.values().stream().flatMap(s->s.jobs.values().stream()).filter(j->j.owner().equals(id)).count();}
    private static int coldDestinations(UUID owner){
        int count=0;
        for(var state:LEVELS.values()){
            Set<Long> chunks=new HashSet<>();
            for(var entry:state.jobs.entrySet())if(owner==null||entry.getValue().owner().equals(owner))chunks.add(entry.getKey());
            for(var rider:RIDERS.values())if(rider.state==state&&(owner==null||rider.id.equals(owner)))
                for(var entry:rider.leases.entrySet())if(state.level.getChunkSource().getChunkNow(entry.getValue().pos.x,entry.getValue().pos.z)==null)chunks.add(entry.getKey());
            count+=chunks.size();
        }
        return count;
    }

    /** Stop/dismount callers can release immediately; lifecycle events also reap
     * forgotten rides. Shared vanilla futures are never cancelled. Pending work
     * remains in the outstanding budget until it actually completes. */
    public static void release(ServerPlayer player){requireServerThread(player.serverLevel());release(player.getUUID());}
    private static void release(UUID id){var rider=RIDERS.remove(id);if(rider!=null)for(var lease:rider.leases.values())rider.state.level.getChunkSource().removeRegionTicket(TICKET,lease.pos,0,id);}
    private static boolean valid(ServerPlayer player,ControlledSwordEntity sword){return player.isAlive()&&!player.isRemoved()&&!sword.isRemoved()&&sword.flightRoot()&&sword.level()==player.level()&&player.getVehicle()==sword&&sword.getFirstPassenger()==player&&player.getUUID().equals(sword.ownerId);}
    private static void requireServerThread(ServerLevel level){if(!level.getServer().isSameThread())throw new IllegalStateException("Flight chunk streaming requires the server thread");}

    /** Nonblocking readiness of the actual momentum corridor, with the same
     * one-block terrain margin as collision. It predicts terrain only, never
     * authorizes passage through an obstacle or changes the flight target. */
    public static Readiness readiness(ServerPlayer player,AABB volume,Vec3 velocity,double brakingAcceleration){
        var level=player.serverLevel();requireServerThread(level);
        double speed=finite(velocity)?velocity.length():0;
        double stopping=brakingDistance(speed,brakingAcceleration);
        double look=Math.min(MAX_READINESS_DISTANCE,Math.max(32,stopping+speed*4+16));
        double ready=0;
        if(finite(velocity)&&loaded(level,volume.inflate(1))){
            Vec3 direction=speed>.00001?velocity.scale(1/speed):Vec3.ZERO;
            if(speed<.00001)ready=look;
            else for(double end=Math.min(8,look);;end=Math.min(look,end+8)){
                AABB swept=volume.move(direction.scale(ready)).minmax(volume.move(direction.scale(end))).inflate(1);
                if(!loaded(level,swept)){
                    double low=ready,high=end;
                    for(int step=0;step<8;step++){double mid=(low+high)*.5;
                        AABB prefix=volume.move(direction.scale(ready)).minmax(volume.move(direction.scale(mid))).inflate(1);
                        if(loaded(level,prefix))low=mid;else high=mid;
                    }
                    ready=low;break;
                }
                ready=end;if(end>=look)break;
            }
        }
        var result=new Readiness(ready,look,stopping,speedForReadyDistance(ready,brakingAcceleration),ready>=look-1e-6);
        var rider=RIDERS.get(player.getUUID());if(rider!=null)rider.readiness=result;
        return result;
    }
    /** Geometric decay under target-zero braking, plus two ticks of reaction
     * and a two-block margin. These helpers impose no global speed limit. */
    public static double brakingDistance(double speed,double acceleration){
        double a=Double.isFinite(acceleration)?Mth.clamp(acceleration,.01,1):.18;
        return (Double.isFinite(speed)?Math.max(0,speed):0)*(2+(1-a)/a)+2;
    }
    public static double speedForReadyDistance(double distance,double acceleration){
        double a=Double.isFinite(acceleration)?Mth.clamp(acceleration,.01,1):.18;
        return (Double.isFinite(distance)?Math.max(0,distance-2):0)/(2+(1-a)/a);
    }
    private static boolean loaded(ServerLevel level,AABB box){
        if(!Double.isFinite(box.minX)||!Double.isFinite(box.minY)||!Double.isFinite(box.minZ)||!Double.isFinite(box.maxX)||!Double.isFinite(box.maxY)||!Double.isFinite(box.maxZ)
                ||!level.getWorldBorder().isWithinBounds(box))return false;
        int x0=Mth.floor(box.minX)>>4,x1=Mth.floor(box.maxX)>>4,z0=Mth.floor(box.minZ)>>4,z1=Mth.floor(box.maxZ)>>4;
        if(x1-x0>4||z1-z0>4)return false;
        for(int x=x0;x<=x1;x++)for(int z=z0;z<=z1;z++)if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }

    public static void releaseAll(MinecraftServer server){for(var level:new ArrayList<>(LEVELS.keySet()))if(level.getServer()==server){requireServerThread(level);releaseLevel(level);}BUDGETS.remove(server);var dispatcher=DISPATCHERS.remove(server);if(dispatcher!=null)dispatcher.shutdown();}
    public static void releaseAll(){for(var level:new ArrayList<>(LEVELS.keySet())){requireServerThread(level);releaseLevel(level);}BUDGETS.clear();for(var dispatcher:DISPATCHERS.values())dispatcher.shutdown();DISPATCHERS.clear();}

    @SubscribeEvent public static void endLevelTick(TickEvent.LevelTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.level instanceof ServerLevel level))return;
        var state=LEVELS.get(level);if(state==null)return;reap(state);long now=level.getGameTime();
        for(var id:new ArrayList<>(RIDERS.keySet())){var rider=RIDERS.get(id);if(rider.state!=state)continue;var player=level.getServer().getPlayerList().getPlayer(id);
            if(player==null||player.serverLevel()!=level||!(player.getVehicle() instanceof ControlledSwordEntity root)||!valid(player,root)||now-rider.updated>STALE_RIDER_TICKS||now<rider.updated)release(id);
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer player)release(player);}
    @SubscribeEvent public static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event){if(event.getEntity() instanceof ServerPlayer player)release(player);}
    @SubscribeEvent public static void unload(LevelEvent.Unload event){if(event.getLevel() instanceof ServerLevel level)releaseLevel(level);}
    @SubscribeEvent public static void stop(ServerStoppingEvent event){releaseAll(event.getServer());}
    private static void releaseLevel(ServerLevel level){for(var id:new ArrayList<>(RIDERS.keySet()))if(RIDERS.get(id).state.level==level)release(id);LEVELS.remove(level);}

    /** Read-only server-thread diagnostics; getChunkNow never starts loading. */
    public static Map<String,Object> diagnostics(ServerPlayer player){
        requireServerThread(player.serverLevel());var rider=RIDERS.get(player.getUUID());if(rider==null)return Map.of("active",false,"tickets",0,"outstanding",riderRequestCount(player.getUUID()),"directColdDestinations",coldDestinations(player.getUUID()));
        var rows=new ArrayList<Map<String,Object>>();for(var lease:rider.leases.values())rows.add(Map.of("x",lease.pos.x,"z",lease.pos.z,"ready",rider.state.level.getChunkSource().getChunkNow(lease.pos.x,lease.pos.z)!=null,"refreshed",lease.refreshed));
        long ready=rows.stream().filter(row->Boolean.TRUE.equals(row.get("ready"))).count();
        return Map.of("active",true,"tickets",rows.size(),"ticketsReady",ready,"ticketsMissing",rows.size()-ready,"directColdDestinations",coldDestinations(rider.id),"outstanding",riderRequestCount(rider.id),"planned",rider.plan.size(),"updated",rider.updated,"leases",List.copyOf(rows),"readiness",rider.readiness);
    }
    public static Map<String,Object> diagnostics(ServerLevel level){
        requireServerThread(level);var state=LEVELS.get(level);
        var result=new LinkedHashMap<String,Object>();result.put("tickets",state==null?0:levelTicketCount(state));result.put("outstanding",state==null?0:state.jobs.size());result.put("totalTickets",totalTicketCount());result.put("totalOutstanding",totalRequestCount());
        result.put("directColdDestinations",coldDestinations(null));result.put("coldBudgetScope","Direct missing FULL destinations plus retained unresolved requests, not vanilla generation/light dependencies");
        result.put("requests",state==null?0:state.requests);result.put("completed",state==null?0:state.completed);result.put("failed",state==null?0:state.failed);result.put("budgetDeferrals",state==null?0:state.budgetDeferrals);
        var jobs=new ArrayList<Map<String,Object>>();if(state!=null)for(var entry:state.jobs.entrySet()){var pos=new ChunkPos(entry.getKey());jobs.add(Map.of("x",pos.x,"z",pos.z,"owner",entry.getValue().owner().toString(),"done",entry.getValue().future().isDone()));}
        result.put("pendingJobs",List.copyOf(jobs));return Collections.unmodifiableMap(result);
    }
    private FlightChunkStreaming(){}
}
