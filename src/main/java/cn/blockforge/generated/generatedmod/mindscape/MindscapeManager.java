package cn.blockforge.generated.generatedmod.mindscape;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.yujian.YujianController;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.*;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class MindscapeManager {
    public static final ResourceKey<Level> DIMENSION=ResourceKey.create(Registries.DIMENSION,new ResourceLocation(GeneratedMod.MOD_ID,"mindscape"));
    public static final int MEDITATION_TICKS=300;
    private static final String OWNER="MindscapeOwner",SESSION="MindscapeSession";
    private static final String VICTORY_UNTIL="victory_display_until";
    private record Meditation(ResourceKey<Level> dimension,UUID seat,Vec3 position,int ticks){}
    private static final Map<UUID,Meditation> MEDITATIONS=new HashMap<>();
    private static final Set<UUID> TRANSFERRING=new HashSet<>();
    private MindscapeManager(){}
    private static CompoundTag data(ServerPlayer p){return GeneratedMod.data(p).wuxingxiantu$getMindscapeData();}
    public static MindscapeState state(ServerPlayer p){return MindscapeState.read(data(p).getString("state"));}
    private static void state(ServerPlayer p,MindscapeState state){data(p).putString("state",state.name());}
    public static boolean canGain(Player p,CultivationData data,boolean notify){
        if(data==null)return false;
        if(data.wuxingxiantu$getDaoXin() != 4)return true;
        if(notify&&!p.level().isClientSide)p.displayClientMessage(Component.translatable("message.wuxingxiantu.mindscape_cultivation_locked"),true);
        return false;
    }
    public static void markUnstable(ServerPlayer p){
        CultivationData cultivation = GeneratedMod.data(p);
        if(data(p).getBoolean("final_completed")&&MindscapeProgression.pending(p)==null) {
            cultivation.wuxingxiantu$resolveHeartDemonInvasion();
            state(p,MindscapeState.NONE);send(p,0);return;
        }
        if (cultivation.wuxingxiantu$getDaoXin() != 4)
            cultivation.wuxingxiantu$setDaoHeartBeforeInvasion(cultivation.wuxingxiantu$getDaoXin());
        if (cultivation.wuxingxiantu$getHeartDemonInvasionCause().isBlank())
            cultivation.wuxingxiantu$setHeartDemonInvasionCause("BREAKTHROUGH");
        cultivation.wuxingxiantu$setDaoXin(4);
        if (MindscapeProgression.pending(p) == null) {
            var next = MindscapeProgression.chooseNext(p);
            // All ten formal trials follow the previous completed challenge.
            if (next != null && next.progressIndex() >= 0)
                data(p).putString("pending_challenge", next.id().toString());
        }
        state(p,MindscapeState.UNSTABLE);send(p,0);
    }
    public static void markUnstableFor(ServerPlayer p,String challengeId){
        MindscapeProgression.markUnstableFor(p,challengeId);state(p,MindscapeState.UNSTABLE);send(p,0);
    }
    private static void send(ServerPlayer p,int ticks){
        var capability=GeneratedMod.data(p);if(capability==null)return;
        var tag=data(p);MindscapeState current=state(p);
        int remaining=current==MindscapeState.MEDITATING?Math.max(0,MEDITATION_TICKS-ticks)
                :current==MindscapeState.COMPLETED?victoryRemaining(p):0;
        UUID session=tag.hasUUID("session")?tag.getUUID("session"):null;
        Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.PLAYER.with(()->p),new cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket(
                capability.wuxingxiantu$isDaoHeartUnstable(),current.ordinal(),remaining,tag.getString("challenge"),session,session==null?-1:tag.getInt("slot")));
    }
    private static int victoryRemaining(ServerPlayer p){
        long remaining=data(p).getLong(VICTORY_UNTIL)-p.serverLevel().getGameTime();
        return (int)Math.max(0,Math.min(MindscapeState.VICTORY_DISPLAY_TICKS,remaining));
    }
    public static void cancelMeditation(ServerPlayer p){
        var capability=GeneratedMod.data(p);if(capability==null)return;
        MEDITATIONS.remove(p.getUUID());
        if(capability.wuxingxiantu$isMeditating())CultivationBlock.stopMeditation(p,capability,false);
        if(state(p)==MindscapeState.MEDITATING){state(p,capability.wuxingxiantu$isDaoHeartUnstable()?MindscapeState.UNSTABLE:MindscapeState.NONE);send(p,0);}
    }
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer p)||GeneratedMod.data(p)==null||!p.isAlive())return;
        var capability=GeneratedMod.data(p);var tag=data(p);
        if(p.level().dimension()==DIMENSION){
            if(state(p)==MindscapeState.COMPLETED){
                if(!tag.getBoolean("boss_completed")||!validLease(p)){abort(p,true);return;}
                keepInside(p);
                if(victoryRemaining(p)==0){release(p);returnHome(p);GeneratedMod.sync(p);}
                else if(p.tickCount%20==0)send(p,0);
                return;
            }
            if(!validSession(p)){abort(p,true);return;}
            var center=MindscapeInstances.center(tag.getInt("slot"));keepInside(p);
            if(p.tickCount%20==0){
                Entity boss=p.serverLevel().getEntity(tag.getUUID("boss"));
                if(!(boss instanceof LivingEntity living)||!living.isAlive()||!boundBoss(p,boss)){abort(p,true);return;}
                if(!MindscapeArenas.inside(center,boss.position())){var spawn=MindscapeArenas.bossSpawn(center);boss.teleportTo(spawn.x,spawn.y,spawn.z);}
            }
            return;
        }
        if(tag.hasUUID("session")){abort(p,true);return;}
        if(!capability.wuxingxiantu$isDaoHeartUnstable()||!capability.wuxingxiantu$isMeditating()){
            if(MEDITATIONS.containsKey(p.getUUID()))cancelMeditation(p);return;
        }
        if(!(p.getVehicle() instanceof MeditationSeatEntity seat)||p.isSleeping()||p.getAbilities().flying
                ||Math.abs(p.xxa)>.01||Math.abs(p.zza)>.01){cancelMeditation(p);return;}
        var previous=MEDITATIONS.get(p.getUUID());
        Vec3 anchor=seat.position().add(0,-.5,0);
        if(previous!=null&&(previous.dimension()!=p.level().dimension()||!previous.seat().equals(seat.getUUID())
                ||p.position().distanceToSqr(previous.position())>.01||Math.abs(p.getDeltaMovement().y)>.08)){
            cancelMeditation(p);return;
        }
        int ticks=previous==null?0:previous.ticks()+1;
        MEDITATIONS.put(p.getUUID(),new Meditation(p.level().dimension(),seat.getUUID(),anchor,ticks));state(p,MindscapeState.MEDITATING);
        if(ticks%10==0)seat.entryProgress(true,ticks);
        if(ticks%20==0)send(p,ticks);
        if(ticks>=MEDITATION_TICKS){
            var challenge=MindscapeProgression.chooseNext(p);
            if (challenge == null && !MindscapeChallenges.hasFormalChallenges())
                challenge=MindscapeChallenges.forMeditation(capability.wuxingxiantu$getRealm());
            if(challenge==null){cancelMeditation(p);p.displayClientMessage(Component.translatable("message.wuxingxiantu.mindscape_no_challenge"),true);}
            else if(!enter(p,challenge,false))cancelMeditation(p);
        }
    }
    private static void keepInside(ServerPlayer p){
        var center=MindscapeInstances.center(data(p).getInt("slot"));
        if(!MindscapeArenas.inside(center,p.position())){
            var spawn=MindscapeArenas.playerSpawn(center);
            YujianController.stop(p);p.stopRiding();p.teleportTo(spawn.x,spawn.y,spawn.z);p.setDeltaMovement(Vec3.ZERO);
        }
    }
    public static boolean enter(ServerPlayer p,MindscapeChallenges.Definition challenge,boolean developer){
        var capability=GeneratedMod.data(p);
        if(capability==null||challenge==null||!p.isAlive()||p.isSpectator()||p.level().dimension()==DIMENSION
                ||data(p).hasUUID("session")||capability.wuxingxiantu$getRealm()<challenge.requiredRealm()
                ||!capability.wuxingxiantu$getActiveDivineArt().isBlank())return false;
        if(!developer&&capability.wuxingxiantu$getDaoXin()!=4)return false;
        ServerLevel level=p.server.getLevel(DIMENSION);if(level==null){cancelMeditation(p);return false;}
        UUID session=UUID.randomUUID();int slot=MindscapeInstances.get(level).allocate(p.getUUID(),session);
        if(slot<0){cancelMeditation(p);p.displayClientMessage(Component.translatable("message.wuxingxiantu.mindscape_full"),true);return false;}
        cancelMeditation(p);YujianController.stop(p);p.stopRiding();p.closeContainer();p.setDeltaMovement(Vec3.ZERO);
        // Clear only transient projection/shape state; learned techniques remain in CultivationData.
        DivineArtLogic.clear(p);TransformAbilityRegistry.clear(p);
        if(!MindscapeExternalSnapshot.capture(p)){
            MindscapeInstances.get(level).release(session);cancelMeditation(p);return false;
        }
        var tag=data(p);tag.putString("original_dimension",p.level().dimension().location().toString());
        tag.putDouble("original_x",p.getX());tag.putDouble("original_y",p.getY());tag.putDouble("original_z",p.getZ());
        tag.putFloat("original_yaw",p.getYRot());tag.putFloat("original_pitch",p.getXRot());
        tag.putUUID("owner",p.getUUID());tag.putUUID("session",session);tag.putInt("slot",slot);
        tag.remove(VICTORY_UNTIL);
        tag.putString("challenge",challenge.id().toString());tag.putString("boss_entity_id",challenge.boss().toString());
        tag.putBoolean("boss_completed",false);tag.putBoolean("is_final",challenge.isFinal());
        capability.wuxingxiantu$setDaoXin(4);state(p,MindscapeState.ENTERING);
        try{
            BlockPos center=MindscapeInstances.center(slot);
            var instances = MindscapeInstances.get(level);
            // A slot is reused across different bosses. Restore its complete arena every entry.
            MindscapeArenas.prepare(level,center,challenge.arena());
            instances.markInitialized(slot);
            EntityType<?> type=ForgeRegistries.ENTITY_TYPES.getValue(challenge.boss());
            Entity entity=type==null?null:type.create(level);
            if(!(entity instanceof Mob boss))throw new IllegalStateException("Missing test boss");
            boss.setPersistenceRequired();boss.setNoAi(true);boss.setCanPickUpLoot(false);
            if(boss instanceof HeartDemonEntity heart){heart.setVisualPhase(1);heart.bindToOwner(p);heart.setNoAi(false);}
            boss.getPersistentData().putUUID(OWNER,p.getUUID());boss.getPersistentData().putUUID(SESSION,session);
            boss.getPersistentData().putString("MindscapeChallenge",challenge.id().toString());
            var bossSpawn=MindscapeArenas.bossSpawn(center);
            boss.moveTo(bossSpawn.x,bossSpawn.y,bossSpawn.z,180,0);tag.putUUID("boss",boss.getUUID());
            if(boss instanceof cn.blockforge.generated.generatedmod.boss.VisualBossEntity visual)visual.beginChallenge(p,center,session);
            if(boss instanceof HeartDemonEntity heart)heart.beginChallenge(p,center,session);
            if(!level.addFreshEntity(boss))throw new IllegalStateException("Boss spawn rejected");
            transfer(p,level,MindscapeArenas.playerSpawn(center),0,0);
            state(p,MindscapeState.IN_MINDSCAPE);state(p,MindscapeState.CHALLENGE_ACTIVE);send(p,0);GeneratedMod.sync(p);return true;
        }catch(RuntimeException exception){com.mojang.logging.LogUtils.getLogger().error("Cannot enter mindscape",exception);abort(p,true);return false;}
    }
    private static void transfer(ServerPlayer p,ServerLevel level,Vec3 point,float yaw,float pitch){
        TRANSFERRING.add(p.getUUID());
        try{
            YujianController.stop(p);p.stopRiding();p.setDeltaMovement(Vec3.ZERO);p.fallDistance=0;
            p.setPose(Pose.STANDING);p.getAbilities().flying=false;p.onUpdateAbilities();p.teleportTo(level,point.x,point.y,point.z,yaw,pitch);
            p.setDeltaMovement(Vec3.ZERO);
        }finally{TRANSFERRING.remove(p.getUUID());}
    }
    private static boolean validLease(ServerPlayer p){
        var tag=data(p);int slot=tag.getInt("slot");
        return p.level().dimension()==DIMENSION&&tag.hasUUID("session")&&tag.hasUUID("owner")&&p.getUUID().equals(tag.getUUID("owner"))
                &&slot>=0&&slot<MindscapeInstances.LIMIT
                &&MindscapeInstances.get(p.serverLevel()).owns(p.getUUID(),tag.getUUID("session"),tag.getInt("slot"));
    }
    private static boolean validSession(ServerPlayer p){
        return validLease(p)&&data(p).hasUUID("boss")&&state(p)==MindscapeState.CHALLENGE_ACTIVE;
    }
    private static boolean boundBoss(ServerPlayer p,Entity boss){
        var tag=data(p);var binding=boss.getPersistentData();
        return tag.hasUUID("session")&&tag.hasUUID("boss")&&tag.getUUID("boss").equals(boss.getUUID())&&binding.hasUUID(OWNER)&&binding.hasUUID(SESSION)
                &&p.getUUID().equals(binding.getUUID(OWNER))&&tag.getUUID("session").equals(binding.getUUID(SESSION))
                &&tag.getString("challenge").equals(binding.getString("MindscapeChallenge"))
                &&tag.getString("boss_entity_id").equals(String.valueOf(ForgeRegistries.ENTITY_TYPES.getKey(boss.getType())));
    }
    public static boolean hasBoundDemon(ServerPlayer player) {
        if(!player.isAlive()||GeneratedMod.data(player)==null)return false;
        if(!validSession(player))return false;
        Entity boss=player.serverLevel().getEntity(data(player).getUUID("boss"));
        return boss instanceof HeartDemonEntity heart&&heart.isAlive()&&boundBoss(player,boss);
    }

    public static boolean complete(ServerPlayer p,boolean developer){
        if(p.level().dimension()!=DIMENSION||!validSession(p)||data(p).getBoolean("boss_completed"))return false;
        var tag=data(p);Entity boss=p.serverLevel().getEntity(tag.getUUID("boss"));
        if(boss==null||!boundBoss(p,boss)||(!developer&&(!(boss instanceof LivingEntity living)||living.getHealth()>0)))return false;
        if(!developer&&boss instanceof HeartDemonEntity heart&&heart.visualPhase()!=HeartDemonEntity.VisualPhase.PHASE_2)return false;
        // Persist the settlement marker before changing the cultivation gate; re-entrant deaths cannot settle twice.
        tag.putBoolean("boss_completed",true);state(p,MindscapeState.COMPLETED);
        var completed=tag.getList("completed_challenges",8);String id=tag.getString("challenge");
        boolean known=false;for(Tag value:completed)if(value.getAsString().equals(id))known=true;
        if(!known&&completed.size()<256)completed.add(StringTag.valueOf(id));tag.put("completed_challenges",completed);
        var definition=MindscapeChallenges.get(id);
        int progress=definition==null?0:definition.progressIndex();
        tag.putInt("completed_stage",Math.max(tag.getInt("completed_stage"),Math.max(progress,GeneratedMod.data(p).wuxingxiantu$getRealm())));
        if(tag.getBoolean("is_final"))tag.putBoolean("final_completed",true);
        CultivationData settled = GeneratedMod.data(p);
        if(!developer){String form=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(boss.getType()).toString();if(cn.blockforge.generated.generatedmod.boss.combat.BossPlayerSkills.FORMS.contains(form)){settled.wuxingxiantu$addPossessedForm(form);p.displayClientMessage(Component.literal("七十二变已解锁：").append(boss.getType().getDescription()),false);}}
        settled.wuxingxiantu$resolveHeartDemonInvasion();
        MindscapeProgression.clearPending(p);
        p.displayClientMessage(Component.translatable("message.wuxingxiantu.mindscape_completed"),false);
        if(developer){release(p);returnHome(p);}
        else{
            // Settle once now, but retain the sealed snapshot and lease until the
            // owner has seen the authoritative purification and short gold hold.
            tag.putLong(VICTORY_UNTIL,p.serverLevel().getGameTime()+MindscapeState.VICTORY_DISPLAY_TICKS);
            clearEncounter(p);send(p,0);
        }
        GeneratedMod.sync(p);return true;
    }
    private static void clearEncounter(ServerPlayer p){
        var tag=data(p);ServerLevel level=p.server.getLevel(DIMENSION);
        if(level!=null){
            if(tag.hasUUID("boss")){var boss=level.getEntity(tag.getUUID("boss"));if(boss!=null&&boundBoss(p,boss))boss.discard();}
            if(tag.hasUUID("session")){
                UUID session=tag.getUUID("session");
                level.getEntitiesOfClass(Entity.class, new AABB(MindscapeInstances.center(tag.getInt("slot"))).inflate(48), entity -> {
                    var persistent=entity.getPersistentData();
                    if(persistent.hasUUID(SESSION)&&session.equals(persistent.getUUID(SESSION))) return true;
                    if(entity instanceof cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity sword)
                        return p.getUUID().equals(sword.ownerId);
                    if(entity instanceof net.minecraft.world.entity.projectile.Projectile projectile
                            && projectile.getOwner() != null) return p.getUUID().equals(projectile.getOwner().getUUID());
                    return false;
                }).forEach(Entity::discard);
            }
        }
    }
    private static void release(ServerPlayer p){
        clearEncounter(p);var level=p.server.getLevel(DIMENSION);
        if(level!=null)MindscapeInstances.get(level).releaseOwner(p.getUUID());
        data(p).remove(VICTORY_UNTIL);
    }
    public static void abort(ServerPlayer p,boolean returnNow){
        if(GeneratedMod.data(p)==null)return;cancelMeditation(p);
        boolean session=data(p).hasUUID("session")||p.level().dimension()==DIMENSION;
        if(!session)return;
        if(data(p).hasUUID("session")&&!data(p).getBoolean("boss_completed"))GeneratedMod.data(p).wuxingxiantu$setDaoXin(4);
        release(p);state(p,MindscapeState.RETURNING);
        if(returnNow&&p.isAlive())returnHome(p);send(p,0);
    }
    /** Explicit creative repair; ordinary cleansing must not bypass an invasion. */
    public static boolean repairInvasion(ServerPlayer player) {
        abort(player,true);
        if(player.level().dimension()==DIMENSION||MindscapeExternalSnapshot.isSealed(player))return false;
        var cultivation=GeneratedMod.data(player);
        MindscapeProgression.clearPending(player);
        cultivation.wuxingxiantu$restoreDaoXin(1);
        cultivation.wuxingxiantu$setDaoHeartBeforeInvasion(1);
        cultivation.wuxingxiantu$setHeartDemonInvasionCause("");
        state(player,MindscapeState.NONE);
        send(player,0);
        GeneratedMod.sync(player);
        return true;
    }
    /** Recovery does not require a live session or a valid instance lease. */
    public static boolean forceRestore(ServerPlayer p) {
        if (GeneratedMod.data(p) == null || !p.isAlive() || !MindscapeExternalSnapshot.isValid(p)) return false;
        if (returnPoint(p) == null) return false;
        cancelMeditation(p);
        release(p);
        state(p, MindscapeState.RETURNING);
        returnHome(p);
        GeneratedMod.sync(p);
        return !MindscapeExternalSnapshot.isValid(p);
    }
    private record ReturnPoint(ServerLevel level,Vec3 position){}
    private static ReturnPoint returnPoint(ServerPlayer p){
        var tag=data(p);var id=ResourceLocation.tryParse(tag.getString("original_dimension"));
        ServerLevel level=id==null?null:p.server.getLevel(ResourceKey.create(Registries.DIMENSION,id));
        double x=tag.getDouble("original_x"),y=tag.getDouble("original_y"),z=tag.getDouble("original_z");
        if(level!=null&&level.dimension()!=DIMENSION&&Double.isFinite(x)&&Double.isFinite(y)&&Double.isFinite(z)
                &&Math.abs(x)<=29999900&&Math.abs(z)<=29999900){
            Vec3 found=findSafe(level,p,new Vec3(x,y,z));if(found!=null)return new ReturnPoint(level,found);
        }
        level=p.server.overworld();var spawn=level.getSharedSpawnPos();
        level.getChunk(spawn.getX()>>4,spawn.getZ()>>4);
        Vec3 found=findSafe(level,p,Vec3.atBottomCenterOf(level.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,spawn)));
        return found==null?null:new ReturnPoint(level,found);
    }
    private static Vec3 findSafe(ServerLevel level,ServerPlayer p,Vec3 origin){
        var start=BlockPos.containing(origin);if(!level.getWorldBorder().isWithinBounds(start))return null;
        level.getChunk(start.getX()>>4,start.getZ()>>4);
        for(int radius=0;radius<=4;radius++)for(int y=4;y>=-4;y--)for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(radius>0&&Math.max(Math.abs(x),Math.abs(z))!=radius)continue;
            BlockPos pos=start.offset(x,y,z);if(!level.hasChunkAt(pos)||pos.getY()<level.getMinBuildHeight()+1||pos.getY()+2>=level.getMaxBuildHeight())continue;
            Vec3 point=Vec3.atBottomCenterOf(pos);AABB box=new AABB(point.x-.31,point.y,point.z-.31,point.x+.31,point.y+1.81,point.z+.31);
            var below=level.getBlockState(pos.below());
            if(level.getWorldBorder().isWithinBounds(box.minX,box.minZ)&&level.getWorldBorder().isWithinBounds(box.maxX,box.maxZ)
                    &&level.noCollision(p,box)&&!level.containsAnyLiquid(box)&&below.isFaceSturdy(level,pos.below(),net.minecraft.core.Direction.UP)
                    &&!below.is(Blocks.MAGMA_BLOCK)&&!below.is(Blocks.CAMPFIRE)&&!below.is(Blocks.SOUL_CAMPFIRE))return point;
        }return null;
    }
    private static void returnHome(ServerPlayer p){
        // Restore temporary projection/shape/camera state before applying the external snapshot.
        if(!GeneratedMod.data(p).wuxingxiantu$getActiveDivineArt().isBlank())DivineArtLogic.clear(p);
        var point=returnPoint(p);if(point==null){state(p,MindscapeState.RETURNING);p.connection.disconnect(Component.translatable("message.wuxingxiantu.mindscape_no_return"));return;}
        var tag=data(p);float yaw=tag.getFloat("original_yaw"),pitch=tag.getFloat("original_pitch");
        transfer(p,point.level(),point.position(),Float.isFinite(yaw)?yaw:0,Float.isFinite(pitch)?pitch:0);
        if(!MindscapeExternalSnapshot.restore(p)){
            state(p,MindscapeState.RETURNING);
            return;
        }
        for(String key:List.of("session","boss","owner","slot","challenge","boss_entity_id","original_dimension","original_x","original_y","original_z","original_yaw","original_pitch","boss_completed","is_final",VICTORY_UNTIL))tag.remove(key);
        state(p,GeneratedMod.data(p).wuxingxiantu$isDaoHeartUnstable()?MindscapeState.UNSTABLE:MindscapeState.NONE);send(p,0);
    }
    @SubscribeEvent public static void damage(LivingDamageEvent event){
        if(event.getAmount()>0&&event.getEntity() instanceof ServerPlayer p)cancelMeditation(p);
    }
    @SubscribeEvent(priority=EventPriority.HIGHEST) public static void isolate(LivingAttackEvent event){
        if(event.getSource().is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY))return;
        Entity victim=event.getEntity();if(victim.level().isClientSide||victim.level().dimension()!=DIMENSION)return;
        if(victim instanceof ServerPlayer p&&GeneratedMod.data(p)!=null&&state(p)==MindscapeState.COMPLETED
                &&data(p).getBoolean("boss_completed")){event.setCanceled(true);return;}
        Entity attacker=event.getSource().getEntity();
        if(victim.getPersistentData().hasUUID(OWNER)){
            if(!(attacker instanceof ServerPlayer p)||!victim.getPersistentData().getUUID(OWNER).equals(p.getUUID())
                    ||!validSession(p)||!boundBoss(p,victim))event.setCanceled(true);
        }else if(victim instanceof ServerPlayer&&attacker instanceof Player)event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void death(LivingDeathEvent event){
        if(event.getEntity() instanceof ServerPlayer p){cancelMeditation(p);if(p.level().dimension()==DIMENSION)abort(p,false);return;}
        var boss=event.getEntity();if(boss.level().dimension()!=DIMENSION||!boss.getPersistentData().hasUUID(OWNER))return;
        var owner=boss.getServer().getPlayerList().getPlayer(boss.getPersistentData().getUUID(OWNER));
        if(owner!=null&&event.getSource().getEntity()==owner&&validSession(owner)&&boundBoss(owner,boss))complete(owner,false);
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event){if(event.getEntity() instanceof ServerPlayer p)abort(p,false);}
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void login(PlayerEvent.PlayerLoggedInEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&GeneratedMod.data(p)!=null){
            cancelMeditation(p);if(data(p).hasUUID("session")||p.level().dimension()==DIMENSION)abort(p,true);
            else{var level=p.server.getLevel(DIMENSION);if(level!=null)MindscapeInstances.get(level).releaseOwner(p.getUUID());
                state(p,GeneratedMod.data(p).wuxingxiantu$isDaoHeartUnstable()?MindscapeState.UNSTABLE:MindscapeState.NONE);}send(p,0);
        }
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void respawn(PlayerEvent.PlayerRespawnEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&GeneratedMod.data(p)!=null){if(data(p).hasUUID("session"))abort(p,true);send(p,0);}
    }
    @SubscribeEvent public static void dimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(event.getEntity() instanceof ServerPlayer p&&!TRANSFERRING.contains(p.getUUID())){cancelMeditation(p);if(data(p).hasUUID("session")||p.level().dimension()==DIMENSION)abort(p,true);}
    }
    @SubscribeEvent public static void travel(net.minecraftforge.event.entity.EntityTravelToDimensionEvent event){
        if(event.getDimension()==DIMENSION&&event.getEntity() instanceof ServerPlayer p&&!TRANSFERRING.contains(p.getUUID()))event.setCanceled(true);
    }
    @SubscribeEvent public static void stopped(net.minecraftforge.event.server.ServerStoppedEvent event){MEDITATIONS.clear();TRANSFERRING.clear();}
    @SubscribeEvent public static void started(net.minecraftforge.event.server.ServerStartedEvent event){
        var level=event.getServer().getLevel(DIMENSION);
        // Reconnect uses a safe abort. Old leases must not exhaust the finite grid after a crash.
        if(level!=null)MindscapeInstances.get(level).resetLeases();
    }
    @SubscribeEvent public static void orphan(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
        var entity=event.getEntity();if(event.getLevel().isClientSide||event.getLevel().dimension()!=DIMENSION||!entity.getPersistentData().hasUUID(OWNER))return;
        var player=event.getLevel().getServer().getPlayerList().getPlayer(entity.getPersistentData().getUUID(OWNER));
        // New bosses join during ENTERING, before the owner transfers into the dimension.
        if (player == null || GeneratedMod.data(player) == null || !data(player).hasUUID("session")
                || !boundBoss(player, entity)
                || (state(player) != MindscapeState.ENTERING && state(player) != MindscapeState.CHALLENGE_ACTIVE))
            event.setCanceled(true);
    }
    @SubscribeEvent(priority=EventPriority.LOWEST) public static void returnDrops(LivingDropsEvent event){
        if(!(event.getEntity() instanceof ServerPlayer p)||p.level().dimension()!=DIMENSION||!data(p).hasUUID("session"))return;
        var target=returnPoint(p);if(target==null)return;
        for(var iterator=event.getDrops().iterator();iterator.hasNext();){
            var original=iterator.next();var point=target.position();
            var drop=new net.minecraft.world.entity.item.ItemEntity(target.level(),point.x,point.y+.3,point.z,original.getItem().copy());
            drop.setDefaultPickUpDelay();
            if(target.level().addFreshEntity(drop))iterator.remove();
        }
    }
    @SubscribeEvent public static void protect(net.minecraftforge.event.level.BlockEvent.BreakEvent event){if(event.getLevel() instanceof Level level&&level.dimension()==DIMENSION)event.setCanceled(true);}
    @SubscribeEvent public static void place(net.minecraftforge.event.level.BlockEvent.EntityPlaceEvent event){if(event.getLevel() instanceof Level level&&level.dimension()==DIMENSION)event.setCanceled(true);}
    @SubscribeEvent public static void explode(net.minecraftforge.event.level.ExplosionEvent.Detonate event){if(event.getLevel().dimension()==DIMENSION){event.getAffectedBlocks().clear();event.getAffectedEntities().clear();}}
}
