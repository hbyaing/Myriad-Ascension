package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.arts.FlashStep;
import cn.blockforge.generated.generatedmod.mindscape.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.level.block.Blocks;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Required integration regressions; run explicitly with runGameTestServer. */
@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RecheckGameTests {
    private static GameTestPlayer player(GameTestHelper h) {
        var p=new GameTestPlayer(h.getLevel());
        p.cleanupAfter(h,()->{
            MindscapeManager.abort(p,true);
            MindscapeInstances.get(p.server.getLevel(MindscapeManager.DIMENSION)).releaseOwner(p.getUUID());
        });
        h.setBlock(new BlockPos(2,0,2),Blocks.STONE);
        var pos=h.absolutePos(new BlockPos(2,1,2));p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);
        var d=GeneratedMod.data(p);d.wuxingxiantu$setRealm(8);
        d.wuxingxiantu$setMaxMana(cn.blockforge.generated.generatedmod.items.CultivationLogic.maxAura(d));
        d.wuxingxiantu$setAura(d.wuxingxiantu$getMaxMana());
        p.join();return p;
    }
    private static void whenBossLoaded(GameTestHelper h,GameTestPlayer p,Runnable action) {
        var tag=GeneratedMod.data(p).wuxingxiantu$getMindscapeData();
        var level=p.serverLevel();
        var bossId=tag.getUUID("boss");
        p.keepTicking(level,MindscapeInstances.center(tag.getInt("slot")));
        boolean[] loaded={false};
        h.runAfterDelay(100,()->h.assertTrue(loaded[0],
                "Destination boss did not load within 100 ticks: slot="+tag.getInt("slot")+" boss="+bossId));
        h.startSequence().thenWaitUntil(()->h.assertTrue(level.getEntity(bossId)!=null,
                "Waiting for destination boss to become visible")).thenExecute(()->{
                    loaded[0]=true;
                    h.assertTrue(p.isAlive()&&p.level()==level&&MindscapeExternalSnapshot.isValid(p),
                            "Session changed while loading: level="+p.level().dimension()+" health="+p.getHealth()+" tick="+p.tickCount+" data="+tag);
                    action.run();
                });
    }
    private static void prepareMindscape(GameTestHelper h,GameTestPlayer p,Runnable action) {
        var level=p.server.getLevel(MindscapeManager.DIMENSION);
        var instances=MindscapeInstances.get(level);
        int reserved=instances.allocate(p.getUUID(),java.util.UUID.randomUUID());
        h.assertTrue(reserved>=0,"No free test arena");
        // enter() replaces this lease with the first free slot; earlier slots may be freed meanwhile.
        for(int slot=0;slot<=reserved;slot++)p.keepTicking(level,MindscapeInstances.center(slot));
        // Fresh CI worlds load these distant entity chunks asynchronously; allow that setup
        // separately from the twenty ticking-entity frames required by the phase transition.
        boolean[] ready={false};
        h.runAfterDelay(200,()->h.assertTrue(ready[0],
                "Mindscape arena chunks did not become ready within 200 ticks: last slot="+reserved));
        h.startSequence().thenWaitUntil(()->{
            for(int slot=0;slot<=reserved;slot++){
                var center=MindscapeInstances.center(slot);
                h.assertTrue(level.isPositionEntityTicking(center)
                        &&level.areEntitiesLoaded(new net.minecraft.world.level.ChunkPos(center).toLong()),
                        "Waiting for test arena entity chunks");
            }
        }).thenExecute(()->{ready[0]=true;action.run();});
    }
    private static void awaitVictoryReturn(GameTestHelper h,GameTestPlayer p,long cultivation) {
        var data=GeneratedMod.data(p);var tag=data.wuxingxiantu$getMindscapeData();
        var session=tag.getUUID("session");int slot=tag.getInt("slot");
        var level=p.serverLevel();int settledCount=tag.getList("completed_challenges",8).size();
        h.assertTrue(MindscapeManager.state(p)==MindscapeState.COMPLETED&&p.level().dimension()==MindscapeManager.DIMENSION,
                "Natural victory did not enter its purification window");
        h.assertTrue(MindscapeExternalSnapshot.isSealed(p)&&MindscapeInstances.get(level).owns(p.getUUID(),session,slot),
                "Victory released its snapshot or arena before the visual window");
        h.assertTrue(!MindscapeManager.complete(p,false)&&!MindscapeManager.complete(p,true),"Victory settled twice");
        h.runAfterDelay(MindscapeState.PURIFICATION_TICKS,()->{
            h.assertTrue(p.level().dimension()==MindscapeManager.DIMENSION&&MindscapeManager.state(p)==MindscapeState.COMPLETED,
                    "Victory returned before the purified hold");
            h.assertTrue(MindscapeExternalSnapshot.isSealed(p)&&MindscapeInstances.get(level).owns(p.getUUID(),session,slot),
                    "Purified hold lost the player's recovery data or lease");
        });
        h.runAfterDelay(MindscapeState.VICTORY_DISPLAY_TICKS+2,()->{
            try {
                h.assertTrue(p.level().dimension()!=MindscapeManager.DIMENSION&&!MindscapeExternalSnapshot.isSealed(p),
                        "Victory window did not return and restore the owner");
                h.assertTrue(!MindscapeInstances.get(level).owns(p.getUUID(),session,slot),"Victory leaked its arena lease");
                h.assertTrue(!tag.contains("victory_display_until")&&!tag.hasUUID("session"),"Victory transient state survived return");
                h.assertTrue(tag.getList("completed_challenges",8).size()==settledCount,"Display ticks repeated victory settlement");
                h.assertTrue(data.wuxingxiantu$getCultivation()==cultivation,"Challenge changed external cultivation");
                h.succeed();
            } finally {MindscapeManager.abort(p,true);p.close();}
        });
    }
    @GameTest(template="empty") public static void playerFixtureTicksAndUnregisters(GameTestHelper h) {
        h.setBlock(new BlockPos(2,0,2),Blocks.STONE);
        var p=player(h);
        int start=p.tickCount;
        h.runAfterDelay(10,()->{
            try {
                h.assertTrue(p.tickCount>start,"Real player gameplay tick did not run");
                h.assertTrue(p.server.getPlayerList().getPlayer(p.getUUID())==p
                        &&p.server.getPlayerList().getPlayers().contains(p)&&h.getLevel().players().contains(p),
                        "Player lookup paths disagree");
                p.close();p.close();
                h.assertTrue(p.server.getPlayerList().getPlayer(p.getUUID())==null
                        &&!p.server.getPlayerList().getPlayers().contains(p)&&!h.getLevel().players().contains(p),
                        "Closing fixture left a registered player");
                h.succeed();
            } finally {p.close();}
        });
    }
    @GameTest(template="empty") public static void ordinaryLethalDamageWithoutShieldDies(GameTestHelper h) {
        var p=player(h);
        try {
            // D29 explicitly adds a Nascent Soul passive; this regression isolates mana alone below that gate.
            GeneratedMod.data(p).wuxingxiantu$setRealm(2);
            h.assertTrue(!cn.blockforge.generated.generatedmod.soulpreservation.SoulPreservation.eligible(p,p.damageSources().generic()),
                    "Mana-only fixture unexpectedly qualifies for the confirmed Nascent Soul passive");
            p.hurt(p.damageSources().generic(),1000000);
            h.assertTrue(p.getHealth()<=0.0F||p.isDeadOrDying(),"Mana alone must not grant a death-prevention passive");
        } finally {p.close();}
        h.succeed();
    }
    @GameTest(template="empty") public static void forcedKillBypassesManaAndAbsorption(GameTestHelper h) {
        var p=player(h);
        try {
            p.setAbsorptionAmount(10000);
            p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4),"kill @s");
            h.assertTrue(p.getHealth()<=0.0F||p.isDeadOrDying(),"Forced kill must not leave one health");
        } finally {p.close();}
        h.succeed();
    }
    @GameTest(template="empty") public static void activeAbsorptionStillProtectsOrdinaryDamage(GameTestHelper h) {
        var p=player(h);
        try {
            p.setHealth(10);p.setAbsorptionAmount(20);
            p.hurt(p.damageSources().generic(),12);
            h.assertTrue(p.isAlive()&&p.getHealth()==10.0F&&p.getAbsorptionAmount()<20,"Explicit absorption shield stopped working");
        } finally {p.close();}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=100) public static void forcedKillHighRealmStaysDead(GameTestHelper h) {
        var p=player(h);var d=GeneratedMod.data(p);
        d.wuxingxiantu$setCreationBlessing(true);d.wuxingxiantu$setRootMask(31);
        cn.blockforge.generated.generatedmod.items.CultivationLogic.applyHealthBonus(p,d);
        p.setHealth(p.getMaxHealth());p.setAbsorptionAmount(10000);
        p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4),"kill");
        h.assertTrue(p.isDeadOrDying(),"Bare /kill failed at full realm health: "+p.getHealth());
        h.runAfterDelay(40,()->{
            try {h.assertTrue(p.isDeadOrDying(),"Player revived after /kill: "+p.getHealth());h.succeed();}
            finally {p.close();}
        });
    }
    @GameTest(template="empty") public static void forcedKillBypassesExternalCombatVeto(GameTestHelper h) {
        var p=player(h);
        class CombatVeto {
            @net.minecraftforge.eventbus.api.SubscribeEvent public void attack(net.minecraftforge.event.entity.living.LivingAttackEvent e) {
                if(e.getEntity()==p)e.setCanceled(true);
            }
            @net.minecraftforge.eventbus.api.SubscribeEvent public void hurt(net.minecraftforge.event.entity.living.LivingHurtEvent e) {
                if(e.getEntity()==p){e.setAmount(0);e.setCanceled(true);}
            }
            @net.minecraftforge.eventbus.api.SubscribeEvent public void damage(net.minecraftforge.event.entity.living.LivingDamageEvent e) {
                if(e.getEntity()==p){e.setAmount(0);e.setCanceled(true);}
            }
            @net.minecraftforge.eventbus.api.SubscribeEvent public void death(net.minecraftforge.event.entity.living.LivingDeathEvent e) {
                if(e.getEntity()==p){p.setHealth(20);e.setCanceled(true);}
            }
        }
        var veto=new CombatVeto();net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(veto);
        try {
            p.hurt(p.damageSources().generic(),1000000);
            h.assertTrue(p.isAlive(),"Ordinary protection was bypassed");
            p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(4),"kill");
            h.assertTrue(p.isDeadOrDying()&&p.getHealth()==0,"External combat veto blocked /kill");
        } finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(veto);p.close();}
        h.succeed();
    }
    @GameTest(template="empty") public static void repeatedSneakUseKeepsMeditationSurface(GameTestHelper h) {
        var p=player(h);var d=GeneratedMod.data(p);
        var pos=h.absolutePos(new BlockPos(2,1,2));
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        try {
            for(var registered:java.util.List.of(WuXingBlocks.MEDITATION_CUSHION,WuXingBlocks.SPIRIT_CUSHION,WuXingBlocks.MEDITATION_PLATFORM)) {
            var block=(CultivationBlock)registered.get();
            h.getLevel().setBlockAndUpdate(pos,block.defaultBlockState());
            double surface=pos.getY()+block.meditationSurfaceHeight();
            for(int i=0;i<20;i++) {
                p.setShiftKeyDown(false);p.setDeltaMovement(net.minecraft.world.phys.Vec3.ZERO);
                block.use(block.defaultBlockState(),h.getLevel(),pos,p,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                h.assertTrue(p.getVehicle() instanceof MeditationSeatEntity,"Could not remount cushion");
                var seat=(MeditationSeatEntity)p.getVehicle();seat.positionRider(p);
                h.assertTrue(Math.abs(p.getY()-(surface-.5))<.0001,"Seated height changed");
                p.setShiftKeyDown(true);p.stopRiding();
                block.use(block.defaultBlockState(),h.getLevel(),pos,p,net.minecraft.world.InteractionHand.MAIN_HAND,hit);
                h.assertTrue(!p.isPassenger()&&!d.wuxingxiantu$isMeditating(),"Sneak-use remounted or retained stale meditation");
                h.assertTrue(p.getY()>=surface&&h.getLevel().noCollision(p),"Dismount left player inside cushion");
                seat.discard();
            }
            }
        } finally {CultivationBlock.stopMeditation(p,d,false);p.close();}
        h.succeed();
    }
    @GameTest(template="empty") public static void heartDemonPhaseOneLethalTransitions(GameTestHelper h) {
        var p=player(h);var boss=HeartDemonEntity.create(h.getLevel(),p);
        h.getLevel().addFreshEntity(boss);
        try {
            boss.hurt(p.damageSources().playerAttack(p),1000000);
            h.assertTrue(boss.isAlive()&&boss.visualPhase()==HeartDemonEntity.VisualPhase.PHASE_2,"Ordinary first-phase lethal damage must transition");
            boss.kill();
            h.assertTrue(boss.isDeadOrDying(),"Forced kill during phase transition must not be blocked");
        } finally {boss.discard();p.close();}
        h.succeed();
    }
    @GameTest(template="empty") public static void forcedKillSkipsFirstPhase(GameTestHelper h) {
        var p=player(h);var boss=HeartDemonEntity.create(h.getLevel(),p);
        h.getLevel().addFreshEntity(boss);
        try {boss.kill();h.assertTrue(boss.isDeadOrDying(),"Forced kill must skip phase-one rescue");}
        finally {boss.discard();p.close();}
        h.succeed();
    }
    @GameTest(template="empty",timeoutTicks=500) public static void phaseTwoPlayerKillCompletesChallenge(GameTestHelper h) {
        phaseTwoKillCompletesChallenge(h,false);
    }
    @GameTest(template="empty",timeoutTicks=500) public static void heartTruePhaseTwoKillCompletesChallenge(GameTestHelper h) {
        phaseTwoKillCompletesChallenge(h,true);
    }
    private static void phaseTwoKillCompletesChallenge(GameTestHelper h,boolean heartTrue) {
        var p=player(h);var d=GeneratedMod.data(p);
        d.wuxingxiantu$setCultivation(12345);
        var challenge=MindscapeChallenges.ids().stream().map(id->MindscapeChallenges.get(id.toString()))
                .filter(def->def.boss().getPath().equals("heart_demon")).findFirst().orElseThrow();
        prepareMindscape(h,p,()->{
        try {
            h.assertTrue(MindscapeManager.enter(p,challenge,true),"Cannot enter heart demon challenge");
            whenBossLoaded(h,p,()->{
            var boss=(HeartDemonEntity)p.serverLevel().getEntity(d.wuxingxiantu$getMindscapeData().getUUID("boss"));
            h.assertTrue(boss!=null,"Heart demon missing after destination chunks loaded");
            boss.setNoAi(true);
            if(heartTrue)cn.blockforge.generated.generatedmod.combat.HeartSwordCombatProfile.applyHeartTrueDamage(p,boss,1000000);
            else boss.hurt(p.damageSources().playerAttack(p),1000000);
            h.assertTrue(boss.isAlive()&&boss.visualPhase()==HeartDemonEntity.VisualPhase.PHASE_2,"First phase did not transition");
            h.assertTrue(MindscapeManager.state(p)==MindscapeState.CHALLENGE_ACTIVE&&!d.wuxingxiantu$getMindscapeData().getBoolean("boss_completed"),
                    "First-phase defeat incorrectly purified the mindscape");
            int phaseStartTick=boss.tickCount;
            h.startSequence().thenWaitUntil(()->h.assertTrue(boss.tickCount-phaseStartTick>=20,
                    "Waiting for the boss entity to finish its phase transition")).thenExecute(()->{
                try {
                    h.assertTrue(boss.tickCount-phaseStartTick>=20,"Boss chunk did not tick through phase transition: "+(boss.tickCount-phaseStartTick));
                    if(heartTrue)cn.blockforge.generated.generatedmod.combat.HeartSwordCombatProfile.applyHeartTrueDamage(p,boss,1000000);
                    else boss.hurt(p.damageSources().playerAttack(p),1000000);
                    h.assertTrue(!boss.isAlive(),"Second phase survived ordinary lethal damage");
                    awaitVictoryReturn(h,p,12345);
                } catch(RuntimeException|AssertionError failure) {MindscapeManager.abort(p,true);p.close();throw failure;}
            });
            });
        } catch(RuntimeException|AssertionError failure) {MindscapeManager.abort(p,true);p.close();throw failure;}
        });
    }
    @GameTest(template="empty",timeoutTicks=400) public static void removedBossDoesNotLeaveBrokenSession(GameTestHelper h) {
        var p=player(h);var d=GeneratedMod.data(p);
        prepareMindscape(h,p,()->{
        try {
            h.assertTrue(MindscapeManager.enter(p,MindscapeChallenges.formalByIndex(0,8),true),"Cannot enter challenge");
            whenBossLoaded(h,p,()->{
            p.serverLevel().getEntity(d.wuxingxiantu$getMindscapeData().getUUID("boss")).discard();
            h.runAfterDelay(25,()->{
                try {
                    h.assertTrue(p.level().dimension()!=MindscapeManager.DIMENSION&&!d.wuxingxiantu$getMindscapeData().hasUUID("session"),"Removed boss left a stale session");
                    h.assertTrue(d.wuxingxiantu$getMindscapeData().getList("completed_challenges",8).isEmpty(),"Removing boss granted trial victory");
                    h.assertTrue(d.wuxingxiantu$isDaoHeartUnstable()&&MindscapeManager.state(p)==MindscapeState.UNSTABLE,
                            "Removing a boss purified a failed challenge");
                    h.succeed();
                } finally {MindscapeManager.abort(p,true);p.close();}
            });
            });
        } catch(RuntimeException|AssertionError failure) {MindscapeManager.abort(p,true);p.close();throw failure;}
        });
    }
    private static void bossTracking(net.minecraft.server.level.ServerLevel level,net.minecraft.world.entity.Entity boss,boolean visible){
        bossChunkVisibility(level,boss.chunkPosition(),visible);
    }
    private static void bossChunkVisibility(net.minecraft.server.level.ServerLevel level,net.minecraft.world.level.ChunkPos chunk,boolean visible){
        try {
            var field=net.minecraft.server.level.ServerLevel.class.getDeclaredField("entityManager");field.setAccessible(true);
            var manager=(net.minecraft.world.level.entity.PersistentEntitySectionManager<?>)field.get(level);
            manager.updateChunkStatus(chunk,visible?net.minecraft.world.level.entity.Visibility.TICKING:net.minecraft.world.level.entity.Visibility.HIDDEN);
        } catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
    }
    public static final class BossJoinCapture {
        final java.util.UUID owner;net.minecraft.world.entity.Mob boss;
        BossJoinCapture(java.util.UUID owner){this.owner=owner;}
        @net.minecraftforge.eventbus.api.SubscribeEvent public void joined(net.minecraftforge.event.entity.EntityJoinLevelEvent event){
            var tag=event.getEntity().getPersistentData();
            if(event.getEntity() instanceof net.minecraft.world.entity.Mob mob&&tag.hasUUID("MindscapeOwner")&&owner.equals(tag.getUUID("MindscapeOwner")))boss=mob;
        }
    }
    @GameTest(template="empty",timeoutTicks=600) public static void allFormalBossesFaceOwnerAtFirstSpawn(GameTestHelper h){
        var p=player(h);
        prepareMindscape(h,p,()->{
            try {
                int count=0;
                for(var id:MindscapeChallenges.ids()){
                    var challenge=MindscapeChallenges.get(id.toString());
                    if(!challenge.triggerStage().equals("formal"))continue;
                    var capture=new BossJoinCapture(p.getUUID());
                    net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(capture);
                    try {
                        h.assertTrue(MindscapeManager.enter(p,challenge,true),"Entry rejected: "+id);
                        var boss=capture.boss;h.assertTrue(boss!=null,"Spawn event missing: "+id);
                        var delta=p.position().subtract(boss.position());
                        float expected=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
                        for(float actual:new float[]{boss.getYRot(),boss.yBodyRot,boss.getYHeadRot(),boss.yRotO,boss.yBodyRotO,boss.yHeadRotO})
                            h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(actual-expected))<.001,"Initial body/head/interpolation faces away: "+id+" actual="+actual+" expected="+expected);
                        if(boss instanceof HeartDemonEntity heart){
                            heart.setVisualPhase(2);
                            h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(heart.yBodyRot-expected))<.001,"Phase two reversed body orientation");
                        }
                        count++;
                    }finally{net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(capture);MindscapeManager.abort(p,true);}
                }
                h.assertTrue(count==10,"Expected all ten formal challenges, got "+count);h.succeed();
            }finally{MindscapeManager.abort(p,true);p.close();}
        });
    }
    public static final class KeepFreshBossHidden {
        final GameTestPlayer player;final net.minecraft.server.level.ServerLevel level;final net.minecraft.world.level.ChunkPos chunk;
        KeepFreshBossHidden(GameTestPlayer player,net.minecraft.world.entity.Mob boss){this.player=player;level=player.serverLevel();chunk=boss.chunkPosition();}
        @net.minecraftforge.eventbus.api.SubscribeEvent(priority=net.minecraftforge.eventbus.api.EventPriority.HIGHEST)
        public void tick(net.minecraftforge.event.TickEvent.PlayerTickEvent event){
            if(event.player==player&&event.phase==net.minecraftforge.event.TickEvent.Phase.END&&player.level()==level)bossChunkVisibility(level,chunk,false);
        }
    }
    private static void activeMindscapeTick(GameTestPlayer p){
        // Exercise the production check at the exact periodic boundary that used
        // to mistake a just-created, not-yet-tracked entity for a missing boss.
        p.tickCount=20;
        MindscapeManager.tick(new net.minecraftforge.event.TickEvent.PlayerTickEvent(net.minecraftforge.event.TickEvent.Phase.END,p));
    }
    private static net.minecraft.world.entity.Mob freshMindscapeBoss(GameTestHelper h,GameTestPlayer p){
        var level=p.server.getLevel(MindscapeManager.DIMENSION);
        int slot=MindscapeInstances.get(level).allocate(p.getUUID(),java.util.UUID.randomUUID());
        var spawn=MindscapeArenas.bossSpawn(MindscapeInstances.center(slot));
        var chunk=new net.minecraft.world.level.ChunkPos(BlockPos.containing(spawn));
        // Make the real entity manager's destination section HIDDEN before the
        // production spawn, not after an already-successful first lookup.
        bossChunkVisibility(level,chunk,false);
        var capture=new BossJoinCapture(p.getUUID());net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(capture);
        try {
            h.assertTrue(MindscapeManager.enter(p,MindscapeChallenges.formalByIndex(0,8),true),"Fresh entry rejected");
            h.assertTrue(capture.boss!=null&&level.getEntity(capture.boss.getUUID())==null,"New boss did not spawn into a real tracking gap");
            return capture.boss;
        } catch(RuntimeException|AssertionError error){bossChunkVisibility(level,chunk,true);throw error;}
        finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(capture);}
    }
    @GameTest(template="empty",timeoutTicks=400) public static void freshBossWaitsForFirstTrackingOnly(GameTestHelper h){
        var p=player(h);
        prepareMindscape(h,p,()->{
            net.minecraft.world.entity.Mob boss=null;
            try {
                boss=freshMindscapeBoss(h,p);var level=p.serverLevel();
                var tag=GeneratedMod.data(p).wuxingxiantu$getMindscapeData();var session=tag.getUUID("session");
                bossTracking(level,boss,false);
                h.assertTrue(level.getEntity(boss.getUUID())==null&&!boss.isRemoved(),"Fixture did not hide a live entity");
                activeMindscapeTick(p);
                h.assertTrue(MindscapeManager.state(p)==MindscapeState.CHALLENGE_ACTIVE&&tag.getUUID("session").equals(session)
                        &&MindscapeExternalSnapshot.isSealed(p),"Initial tracking gap aborted the real session");
                bossTracking(level,boss,true);activeMindscapeTick(p);
                boss.discard();activeMindscapeTick(p);
                h.assertTrue(p.level().dimension()!=MindscapeManager.DIMENSION&&!tag.hasUUID("session")
                        &&MindscapeManager.state(p)==MindscapeState.UNSTABLE,"Already-tracked removal received fresh-boss grace");
                h.succeed();
            } finally {if(boss!=null){bossTracking((net.minecraft.server.level.ServerLevel)boss.level(),boss,true);if(!boss.isRemoved())boss.discard();}MindscapeManager.abort(p,true);p.close();}
        });
    }
    @GameTest(template="empty",timeoutTicks=400) public static void freshBossChunkReloadCanReplaceItsInstance(GameTestHelper h){
        var p=player(h);
        prepareMindscape(h,p,()->{
            net.minecraft.world.entity.Entity replacement=null;
            try {
                var boss=freshMindscapeBoss(h,p);var level=p.serverLevel();
                var saved=new net.minecraft.nbt.CompoundTag();boss.saveWithoutId(saved);
                var session=GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getUUID("session");
                boss.remove(net.minecraft.world.entity.Entity.RemovalReason.UNLOADED_TO_CHUNK);
                activeMindscapeTick(p);
                h.assertTrue(MindscapeManager.state(p)==MindscapeState.CHALLENGE_ACTIVE,"Saveable chunk unload was treated as discard");
                bossTracking(level,boss,true);
                replacement=boss.getType().create(level);h.assertTrue(replacement!=null,"Replacement could not be created");
                replacement.load(saved);
                h.assertTrue(replacement!=boss&&replacement.getUUID().equals(boss.getUUID()),"Fixture did not reload the same UUID into a new instance");
                h.assertTrue(level.addFreshEntity(replacement),"Reloaded bound boss was rejected");
                activeMindscapeTick(p);
                h.assertTrue(level.getEntity(boss.getUUID())==replacement&&MindscapeManager.state(p)==MindscapeState.CHALLENGE_ACTIVE
                        &&GeneratedMod.data(p).wuxingxiantu$getMindscapeData().getUUID("session").equals(session),"Reloaded instance could not take over initial tracking");
                h.succeed();
            } finally {MindscapeManager.abort(p,true);if(replacement!=null&&!replacement.isRemoved())replacement.discard();p.close();}
        });
    }
    @GameTest(template="empty",timeoutTicks=400) public static void freshHiddenDiscardStillAbortsImmediately(GameTestHelper h){
        var p=player(h);
        prepareMindscape(h,p,()->{
            try {
                var boss=freshMindscapeBoss(h,p);var level=p.serverLevel();
                bossTracking(level,boss,false);boss.discard();activeMindscapeTick(p);bossTracking(level,boss,true);
                h.assertTrue(p.level().dimension()!=MindscapeManager.DIMENSION&&MindscapeManager.state(p)==MindscapeState.UNSTABLE
                        &&!MindscapeExternalSnapshot.isSealed(p),"Explicit discard was hidden by initial tracking grace");
                h.succeed();
            } finally {MindscapeManager.abort(p,true);p.close();}
        });
    }
    @GameTest(template="empty",timeoutTicks=400) public static void freshHiddenBossTimesOutAndIsCleaned(GameTestHelper h){
        var p=player(h);
        prepareMindscape(h,p,()->{
            var boss=freshMindscapeBoss(h,p);var level=p.serverLevel();
            var tag=GeneratedMod.data(p).wuxingxiantu$getMindscapeData();var session=tag.getUUID("session");int slot=tag.getInt("slot");
            var hidden=new KeepFreshBossHidden(p,boss);net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(hidden);
            bossTracking(level,boss,false);activeMindscapeTick(p);
            h.assertTrue(MindscapeManager.state(p)==MindscapeState.CHALLENGE_ACTIVE,"Fresh hidden boss did not receive initial wait");
            h.runAfterDelay(6,()->{
                try {
                    activeMindscapeTick(p);
                    h.assertTrue(boss.isRemoved()&&!tag.hasUUID("session")&&!MindscapeExternalSnapshot.isSealed(p)
                            &&p.level().dimension()!=MindscapeManager.DIMENSION&&!MindscapeInstances.get(level).owns(p.getUUID(),session,slot),
                            "Tracking timeout leaked a hidden boss, lease or sealed snapshot");
                    h.assertTrue(MindscapeManager.state(p)==MindscapeState.UNSTABLE&&tag.getList("completed_challenges",8).isEmpty(),
                            "Tracking timeout granted victory or purification");
                    h.succeed();
                } finally {net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(hidden);MindscapeManager.abort(p,true);bossTracking(level,boss,true);if(!boss.isRemoved())boss.discard();p.close();}
            });
        });
    }
    @GameTest(template="empty",timeoutTicks=400) public static void victoryLogoutPreservesSettlementAndRecovery(GameTestHelper h) {
        var p=player(h);var d=GeneratedMod.data(p);d.wuxingxiantu$setCultivation(67890);
        prepareMindscape(h,p,()->{
            try {
                h.assertTrue(MindscapeManager.enter(p,MindscapeChallenges.formalByIndex(0,8),true),"Cannot enter victory recovery fixture");
                whenBossLoaded(h,p,()->{
                    try {
                        var tag=d.wuxingxiantu$getMindscapeData();var level=p.serverLevel();
                        var session=tag.getUUID("session");int slot=tag.getInt("slot");
                        var boss=(net.minecraft.world.entity.LivingEntity)level.getEntity(tag.getUUID("boss"));
                        h.assertTrue(!MindscapeManager.complete(p,false),"A living boss granted purification");
                        boss.setHealth(0);
                        h.assertTrue(MindscapeManager.complete(p,false),"Validated defeated boss could not settle");
                        int settled=tag.getList("completed_challenges",8).size();
                        MindscapeManager.logout(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent(p));
                        h.assertTrue(MindscapeManager.state(p)==MindscapeState.RETURNING&&MindscapeExternalSnapshot.isSealed(p),
                                "Logout lost the durable victory recovery record");
                        h.assertTrue(!MindscapeInstances.get(level).owns(p.getUUID(),session,slot),"Logout leaked the completed lease");
                        MindscapeManager.login(new net.minecraftforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent(p));
                        h.assertTrue(p.level().dimension()!=MindscapeManager.DIMENSION&&!MindscapeExternalSnapshot.isSealed(p),
                                "Login did not restore the completed owner");
                        h.assertTrue(!d.wuxingxiantu$isDaoHeartUnstable()&&MindscapeManager.state(p)==MindscapeState.NONE,
                                "Victory recovery reapplied corruption");
                        h.assertTrue(d.wuxingxiantu$getCultivation()==67890&&tag.getList("completed_challenges",8).size()==settled,
                                "Recovery changed cultivation or repeated settlement");
                        h.succeed();
                    } finally {MindscapeManager.abort(p,true);p.close();}
                });
            } catch(RuntimeException|AssertionError failure) {MindscapeManager.abort(p,true);p.close();throw failure;}
        });
    }
    @GameTest(template="empty") public static void mindscapeStatePacketCarriesOptionalSession(GameTestHelper h) {
        var session=java.util.UUID.randomUUID();
        var message=new cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket(false,
                MindscapeState.COMPLETED.ordinal(),MindscapeState.VICTORY_DISPLAY_TICKS,"wuxingxiantu:heart_demon_final",session,63);
        var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket.encode(message,buffer);
            h.assertTrue(message.equals(cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket.decode(buffer)),
                    "Session-bound visual state failed its packet round trip");
            var empty=new cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket(true,MindscapeState.UNSTABLE.ordinal(),0,"");
            cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket.encode(empty,buffer);
            h.assertTrue(empty.equals(cn.blockforge.generated.generatedmod.network.packet.mindscape.MindscapeStatePacket.decode(buffer)),
                    "Empty visual session failed its packet round trip");
            h.succeed();
        } finally {buffer.release();}
    }
    @GameTest(template="empty",timeoutTicks=400) public static void directMindscapeEnterAndAdminCompletionPreserveCultivation(GameTestHelper h) {
        var p=player(h);var d=GeneratedMod.data(p);d.wuxingxiantu$setCultivation(123456);
        prepareMindscape(h,p,()->{
        try {
            var challenge=MindscapeChallenges.formalByIndex(0,8);
            h.assertTrue(MindscapeManager.enter(p,challenge,true),"Could not enter test challenge");
            h.assertTrue(d.wuxingxiantu$getCultivation()==123456,"Entering mindscape reset cultivation");
            whenBossLoaded(h,p,()->{
            try {
            int result=p.server.getCommands().performPrefixedCommand(p.createCommandSourceStack().withPermission(2),"wuxing mindscape force_complete @s");
            h.assertTrue(result==1,"Admin completion command failed");
            h.assertTrue(d.wuxingxiantu$getCultivation()==123456&&!MindscapeExternalSnapshot.isSealed(p),"Completion failed to restore without changing cultivation");
            h.assertTrue(!MindscapeManager.complete(p,true),"Challenge settled twice");
            h.succeed();
            } finally {MindscapeManager.abort(p,true);p.close();}
            });
        } catch(RuntimeException|AssertionError failure) {MindscapeManager.abort(p,true);p.close();throw failure;}
        });
    }
    @GameTest(template="empty",timeoutTicks=400) public static void directMindscapeForceRestorePreservesCultivation(GameTestHelper h) {
        var p=player(h);var d=GeneratedMod.data(p);d.wuxingxiantu$setCultivation(123456);
        prepareMindscape(h,p,()->{
        try {
            h.assertTrue(MindscapeManager.enter(p,MindscapeChallenges.formalByIndex(0,8),true),"Could not enter test challenge");
            whenBossLoaded(h,p,()->{
            try {
            h.assertTrue(MindscapeManager.forceRestore(p),"Force restore failed");
            h.assertTrue(d.wuxingxiantu$getCultivation()==123456,"Force restore reset cultivation");
            h.succeed();
            } finally {MindscapeManager.abort(p,true);p.close();}
            });
        } catch(RuntimeException|AssertionError failure) {MindscapeManager.abort(p,true);p.close();throw failure;}
        });
    }
    @GameTest(template="empty") public static void ninthAndTenthChallengesContinueNormally(GameTestHelper h) {
        var p=player(h);var d=GeneratedMod.data(p);
        try {
            var completed=new ListTag();
            for(int i=0;i<8;i++)completed.add(StringTag.valueOf(MindscapeChallenges.formalByIndex(i,8).id().toString()));
            d.wuxingxiantu$getMindscapeData().put("completed_challenges",completed);
            MindscapeManager.markUnstable(p);
            h.assertTrue(MindscapeProgression.pending(p).progressIndex()==8,"Ninth trial needs an unintended special trigger");
            completed.add(StringTag.valueOf(MindscapeChallenges.formalByIndex(8,8).id().toString()));
            MindscapeProgression.clearPending(p);MindscapeManager.markUnstable(p);
            h.assertTrue(MindscapeProgression.pending(p).progressIndex()==9,"Tenth trial needs an unintended special trigger");
        } finally {p.close();}
        h.succeed();
    }
    @GameTest(template="furnace_empty") public static void flashStepStopsBeforeSolidWall(GameTestHelper h) {
        for(int x=0;x<7;x++)for(int z=0;z<7;z++)h.setBlock(new BlockPos(x,0,z),Blocks.STONE);
        for(int x=0;x<7;x++)for(int y=1;y<4;y++)h.setBlock(new BlockPos(x,y,5),Blocks.STONE);
        var p=player(h);p.setYRot(0);p.setXRot(0);
        try {
            var target=FlashStep.destination(p,8);
            h.assertTrue(target!=null&&target.z<h.absolutePos(new BlockPos(2,1,5)).getZ(),"Flash step crossed the wall");
            var box=p.getBoundingBox().move(target.subtract(p.position()));
            h.assertTrue(h.getLevel().noCollision(p,box),"Flash step ends inside a solid block");
        } finally {p.close();}
        h.succeed();
    }
}
