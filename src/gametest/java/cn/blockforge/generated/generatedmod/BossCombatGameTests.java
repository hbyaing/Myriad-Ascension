package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.boss.VisualBossEntity;
import cn.blockforge.generated.generatedmod.boss.combat.*;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.items.HeartSwordItem;
import cn.blockforge.generated.generatedmod.mindscape.*;
import cn.blockforge.generated.generatedmod.registry.ModEntities;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("wuxingxiantu_boss")
@PrefixGameTestTemplate(false)
public final class BossCombatGameTests {
    private static final class Encounter implements AutoCloseable {
        final GameTestPlayer player;final VisualBossEntity boss;final BossCombatController fight;final UUID session=UUID.randomUUID();final BlockPos center;
        Encounter(GameTestHelper h,String id) {
            var level=h.getLevel().getServer().getLevel(MindscapeManager.DIMENSION);
            player=new GameTestPlayer(level);var instances=MindscapeInstances.get(level);
            int slot=instances.allocate(player.getUUID(),session);h.assertTrue(slot>=0,"No boss fixture slot");center=MindscapeInstances.center(slot);
            for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++){var at=center.offset(x*16,0,z*16);player.keepTicking(level,at);}
            // This fixture exercises the live encounter without changing a user's persisted save or inventory.
            var data=GeneratedMod.data(player);data.wuxingxiantu$setRealm(8);data.wuxingxiantu$setMaxMana(1000000);data.wuxingxiantu$setAura(1000000);
            player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(4000);player.setHealth(4000);
            player.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(100);
            player.setPos(center.getX()+.5,65.1,center.getZ()-8.5);
            player.setItemInHand(InteractionHand.MAIN_HAND,HeartSwordItem.create(player.getUUID()));
            boss=ModEntities.VISUAL_BOSSES.get(id).get().create(level);
            boss.moveTo(center.getX()+.5,65.1,center.getZ()+5.5,180,0);
            var tag=data.wuxingxiantu$getMindscapeData();tag.putUUID("owner",player.getUUID());tag.putUUID("session",session);tag.putUUID("boss",boss.getUUID());tag.putInt("slot",slot);
            tag.putString("state",MindscapeState.CHALLENGE_ACTIVE.name());tag.putString("challenge","wuxingxiantu:dev_visual_trial");tag.putString("boss_entity_id","wuxingxiantu:"+id);
            boss.getPersistentData().putUUID("MindscapeOwner",player.getUUID());boss.getPersistentData().putUUID("MindscapeSession",session);boss.getPersistentData().putString("MindscapeChallenge",tag.getString("challenge"));
            player.join();boss.beginChallenge(player,center,session);fight=boss.combat();
            h.assertTrue(level.addFreshEntity(boss),"Challenge entity rejected");
        }
        public void close(){boss.discard();MindscapeInstances.get(player.serverLevel()).release(session);GeneratedMod.data(player).wuxingxiantu$getMindscapeData().remove("session");player.close();}
    }
    @GameTest(template="empty",timeoutTicks=200)
    public static void allSixtySevenTimelinesExecuteAndClean(GameTestHelper h) {
        int count=0;
        for(var profile:BossCombatCatalog.all())try(var e=new Encounter(h,profile.id())) {
            for(var skill:profile.skills()) {
                e.player.setHealth(e.player.getMaxHealth());e.player.setPos(e.center.getX()+.5,65.1,e.center.getZ()-8.5);
                h.assertTrue(e.fight.forceSkill(skill.id()),"Cannot select "+skill.id());
                int ticks=0;while(e.fight.casting()&&ticks++<650){e.player.invulnerableTime=0;e.fight.tick();}
                h.assertTrue(ticks<650,"Timeline never completes: "+skill.id());
                h.assertTrue(e.player.isAlive(),"One attack exceeds encounter damage budget: "+skill.id());
                h.assertTrue(e.fight.pendingCount()<=280,"Unbounded attack state: "+skill.id());count++;
            }
            e.fight.close();h.assertTrue(e.fight.pendingCount()==0,"Closing encounter leaves pending damage");
        }
        h.assertTrue(count==67,"Missing executable moves");h.succeed();
    }
    @GameTest(template="empty")
    public static void waitingForHeartSwordKeepsBossFacingOwner(GameTestHelper h) {
        try(var e=new Encounter(h,"muyan")) {
            e.player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            for(Vec3 offset:java.util.List.of(new Vec3(-7,1,-8),new Vec3(7,1,8))) {
                e.player.setPos(e.center.getX()+offset.x,e.center.getY()+offset.y,e.center.getZ()+offset.z);
                e.fight.tick();Vec3 delta=e.player.position().subtract(e.boss.position());
                float expected=(float)Math.toDegrees(Math.atan2(-delta.x,delta.z));
                for(float actual:new float[]{e.boss.getYRot(),e.boss.yBodyRot,e.boss.getYHeadRot()})
                    h.assertTrue(Math.abs(net.minecraft.util.Mth.wrapDegrees(actual-expected))<.001,"Waiting boss did not face owner after movement");
                h.assertTrue(!e.fight.casting(),"Waiting boss attacked without a heart sword");
            }
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void mechanicsRejectOtherPlayersAndCleanEntities(GameTestHelper h) {
        try(var e=new Encounter(h,"shenyin");var outsider=new GameTestPlayer(e.player.serverLevel())) {
            final int[] activations={0};var node=e.fight.node("knot",e.player.position().add(1,0,0),200,()->activations[0]++);
            outsider.setPos(node.pos.x,node.pos.y,node.pos.z);outsider.join();
            h.assertTrue(!e.fight.interactNode(node.entity,outsider),"Foreign player used another trial's node");
            h.assertTrue(e.fight.interactNode(node.entity,e.player),"Owner cannot break node");
            e.fight.interactNode(node.entity,e.player);h.assertTrue(activations[0]==1,"Node callback executed twice");
            var crystal=e.fight.node("crystal",e.player.position().add(1,0,0),200,null);e.fight.close();
            h.assertTrue(crystal.entity.isRemoved(),"Encounter left a mechanic entity");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void damageTelegraphAndJumpWindow(GameTestHelper h) {
        try(var e=new Encounter(h,"muyan")) {
            e.fight.forceSkill("muyan_05");e.fight.interrupt(200);
            float before=e.player.getHealth();Vec3 at=e.fight.ground(e.player.position());
            e.fight.circle(at,3,20,.10F);
            for(int i=0;i<19;i++)e.fight.tick();h.assertTrue(e.player.getHealth()==before,"Damage occurred before warning ended");
            e.fight.tick();h.assertTrue(e.player.getHealth()<before,"Circle never damaged a player inside its volume");
            e.player.setHealth(before);e.player.invulnerableTime=0;e.player.setPos(at.x,at.y+1.2,at.z);
            e.fight.ring(at.add(-4,0,0),5,2,20,.10F,true);
            for(int i=0;i<25;i++)e.fight.tick();h.assertTrue(e.player.getHealth()==before,"Jumpable ring hit an airborne player");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void everyArenaRebuildsSlotAndLeavesSafeSpawns(GameTestHelper h) {
        try(var e=new Encounter(h,"muyan")) {
            var level=e.player.serverLevel();String[] arenas={"six_thieves_1","six_thieves_2","six_thieves_3","six_thieves_4","six_thieves_5","six_thieves_6","three_corps_1","three_corps_2","three_corps_3"};
            for(String arena:arenas) {
                MindscapeArenas.prepare(level,e.center,arena);
                for(Vec3 at:java.util.List.of(MindscapeArenas.playerSpawn(e.center),MindscapeArenas.bossSpawn(e.center))) {
                    h.assertTrue(level.noCollision(new AABB(at.x-.3,at.y,at.z-.3,at.x+.3,at.y+1.8,at.z+.3)),"Blocked spawn in "+arena);
                    h.assertTrue(!level.getBlockState(BlockPos.containing(at).below()).isAir(),"Missing arena floor");
                }
                level.setBlock(e.center.above(18),Blocks.GOLD_BLOCK.defaultBlockState(),2);
            }
            MindscapeArenas.prepare(level,e.center,"six_thieves_1");h.assertTrue(level.getBlockState(e.center.above(18)).isAir(),"Prior arena decoration survived rebuild");
        }h.succeed();
    }
    @GameTest(template="empty")
    public static void thirdPhaseDoesNotResetHealth(GameTestHelper h) {
        try(var e=new Encounter(h,"pengjiao")) {
            e.boss.setHealth(e.boss.getMaxHealth()*.25F);float before=e.boss.getHealth();
            for(int i=0;i<135;i++)e.fight.tick();
            h.assertTrue(e.boss.combatPhase()==3,"Three-stage boss failed to advance");
            h.assertTrue(e.boss.getHealth()==before,"Phase transition healed or refilled boss");
        }h.succeed();
    }
}
