package cn.blockforge.generated.generatedmod;

import cn.blockforge.generated.generatedmod.artifacts.*;
import cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.registry.ModEntities;
import cn.blockforge.generated.generatedmod.yujian.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.gametest.*;
import java.util.*;

/** Existing gameplay contract, including intentional differences; no copied target algorithm. */
@GameTestHolder("wuxingxiantu_entry") @PrefixGameTestTemplate(false)
public final class TargetEligibilityGameTests {
    private static GameTestPlayer player(GameTestHelper h){
        var p=new GameTestPlayer(h.getLevel());var pos=h.absolutePos(new BlockPos(0,4,1));
        p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;
    }
    private static <T extends Mob>T mob(GameTestHelper h,GameTestPlayer p,EntityType<T> type){
        var e=Objects.requireNonNull(type.create(h.getLevel()));e.setNoAi(true);e.setNoGravity(true);
        e.setPos(p.getX()+3,p.getY(),p.getZ());return e;
    }
    private static void expect(GameTestHelper h,GameTestPlayer p,LivingEntity e,boolean sword,boolean artifact,String label){
        h.assertTrue(YujianController.canTarget(p,e)==sword,label+": sword eligibility changed");
        h.assertTrue(ArtifactEffects.target(p,e)==artifact,label+": artifact eligibility changed");
    }
    private static void servant(LivingEntity e){
        var tag=e.getPersistentData();tag.putUUID("HumanEmperorBannerOwner",UUID.randomUUID());
        tag.putUUID("HumanEmperorBannerId",UUID.randomUUID());tag.putString("HumanEmperorBannerType","minecraft:cow");
    }
    @GameTest(template="empty",batch="a03_targets") public static void selfDeadOrdinaryAndPetMatrix(GameTestHelper h){
        try(var p=player(h)){
            expect(h,p,p,false,false,"self");
            var cow=mob(h,p,EntityType.COW);expect(h,p,cow,true,true,"ordinary living target");
            cow.setHealth(0);expect(h,p,cow,false,false,"dead");
            var wolf=mob(h,p,EntityType.WOLF);wolf.setTame(true);wolf.setOwnerUUID(p.getUUID());
            expect(h,p,wolf,false,false,"own pet");wolf.setOwnerUUID(UUID.randomUUID());
            expect(h,p,wolf,true,false,"foreign pet");wolf.setOwnerUUID(null);wolf.setTame(false);
            expect(h,p,wolf,true,true,"wild wolf");
            var spirit=mob(h,p,EntityType.COW);servant(spirit);expect(h,p,spirit,true,false,"banner servant");
            var stand=new ArmorStand(h.getLevel(),p.getX()+3,p.getY(),p.getZ());
            expect(h,p,stand,true,false,"armor stand");
        }h.succeed();
    }
    @GameTest(template="empty",batch="a03_targets") public static void teamsFriendlyFireAndSpectatorMatrix(GameTestHelper h){
        try(var p=player(h);var other=player(h)){
            other.setPos(p.getX()+3,p.getY(),p.getZ());
            expect(h,p,other,true,true,"ordinary player");
            var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("a03_"+UUID.randomUUID().toString().substring(0,8));
            try{
                board.addPlayerToTeam(p.getScoreboardName(),team);board.addPlayerToTeam(other.getScoreboardName(),team);
                team.setAllowFriendlyFire(false);expect(h,p,other,false,false,"ally friendly fire disabled");
                team.setAllowFriendlyFire(true);expect(h,p,other,false,false,"ally even with friendly fire enabled");
            }finally{board.removePlayerTeam(team);}
            other.setGameMode(GameType.SPECTATOR);expect(h,p,other,true,false,"spectator");
        }h.succeed();
    }
    @GameTest(template="empty",batch="a03_targets") public static void realWallBlocksBothPoliciesAndRemovalRestoresThem(GameTestHelper h){
        try(var p=player(h)){
            var cow=mob(h,p,EntityType.COW);expect(h,p,cow,true,true,"clear sight");
            var previous=new LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            try{
                for(int y=4;y<=6;y++)for(int z=0;z<=2;z++){
                    var pos=h.absolutePos(new BlockPos(2,y,z));previous.put(pos,h.getLevel().getBlockState(pos));
                    h.getLevel().setBlockAndUpdate(pos,Blocks.STONE.defaultBlockState());
                }
                expect(h,p,cow,false,false,"solid wall");
            }finally{previous.forEach(h.getLevel()::setBlockAndUpdate);}
            expect(h,p,cow,true,true,"restored sight");
        }h.succeed();
    }
    @GameTest(template="empty",batch="a03_targets") public static void artifactHurtConsumerHonorsExtraExclusions(GameTestHelper h){
        try(var p=player(h)){
            var stack=new ItemStack(ArtifactBootstrap.ARTIFACTS.get(ArtifactKind.SWORD).get());
            EquipmentCrafting.write(stack).putInt("ArtifactMaterial",2);ArtifactData.infuse(stack,ArtifactAux.LONGXUE);
            p.setItemInHand(InteractionHand.MAIN_HAND,stack);
            var wild=mob(h,p,EntityType.WOLF);var own=mob(h,p,EntityType.WOLF);own.setTame(true);own.setOwnerUUID(p.getUUID());
            var foreign=mob(h,p,EntityType.WOLF);foreign.setTame(true);foreign.setOwnerUUID(UUID.randomUUID());
            var spirit=mob(h,p,EntityType.COW);servant(spirit);
            for(var e:List.of(wild,own,foreign,spirit)){
                p.setHealth(5);var event=new LivingHurtEvent(e,p.damageSources().playerAttack(p),10);ArtifactEffects.hurt(event);
                boolean enabled=e==wild;
                h.assertTrue((p.getHealth()>5)==enabled&&e.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)==enabled,
                        "Artifact hurt consumer disagreed with ownership/servant policy for "+e.getType());
            }
        }h.succeed();
    }
    @GameTest(template="empty",batch="a03_targets") public static void swordArrayConsumerRetainsItsBroaderTargets(GameTestHelper h){
        try(var p=player(h)){
            var source=new ItemStack(WuXingItems.TIANGANG_SWORD.get());p.getInventory().setItem(0,source);
            var sword=Objects.requireNonNull(ModEntities.CONTROLLED_SWORD.get().create(h.getLevel()));sword.bind(p,source,0,false);
            var own=mob(h,p,EntityType.WOLF);own.setTame(true);own.setOwnerUUID(p.getUUID());
            var foreign=mob(h,p,EntityType.WOLF);foreign.setTame(true);foreign.setOwnerUUID(UUID.randomUUID());
            var spirit=mob(h,p,EntityType.COW);servant(spirit);
            var targets=List.of(own,foreign,spirit);var health=targets.stream().map(LivingEntity::getHealth).toList();
            try{
                targets.forEach(h.getLevel()::addFreshEntity);
                sword.order("art_sword_array",foreign.position(),null,foreign.blockPosition(),0);sword.age=96;
                YujianController.tickSword(sword,p);
                h.assertTrue(!sword.isRemoved(),"Source-backed sword was rejected before the consumer ran");
                h.assertTrue(own.getHealth()==health.get(0),"Sword array damaged its owner's pet");
                h.assertTrue(foreign.getHealth()<health.get(1)&&spirit.getHealth()<health.get(2),
                        "Sword array silently inherited artifact-only target exclusions");
            }finally{targets.forEach(Entity::discard);sword.discard();}
        }h.succeed();
    }
}
