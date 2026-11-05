package cn.blockforge.generated.generatedmod.artifacts;

import cn.blockforge.generated.generatedmod.*;
import cn.blockforge.generated.generatedmod.crafting.*;
import cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem;
import cn.blockforge.generated.generatedmod.yujian.YujianController;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.*;
import net.minecraftforge.fml.common.Mod;
import java.util.*;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class ArtifactEffects {
    /** Artifact procs add exclusions to sword eligibility; do not feed these back into sword damage. */
    public static boolean target(ServerPlayer player,LivingEntity target){return YujianController.canTarget(player,target)
            &&!target.isSpectator()&&!HumanEmperorBannerItem.isServant(target)
            &&!(target instanceof OwnableEntity ownable&&ownable.getOwnerUUID()!=null)
            &&!(target instanceof net.minecraft.world.entity.decoration.ArmorStand);}
    @SubscribeEvent public static void hurt(LivingHurtEvent event){
        if(event.getAmount()<=0)return;
        if(event.getSource().getEntity() instanceof ServerPlayer player&&target(player,event.getEntity())){
            var stack=player.getMainHandItem();if(ArtifactData.artifact(stack)&&EquipmentBond.canUse(player,stack)){
                if(ArtifactData.material(stack)==2)event.getEntity().addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,40,0));
                if(ArtifactData.has(stack,ArtifactAux.Trait.THUNDER)&&player.getRandom().nextFloat()<.25F){
                    event.setAmount(event.getAmount()+4+ArtifactData.quality(stack)+conduct(stack)*.3F);
                    player.serverLevel().sendParticles(ParticleTypes.ELECTRIC_SPARK,event.getEntity().getX(),event.getEntity().getY()+1,event.getEntity().getZ(),16,.3,.4,.3,.2);
                }
                if(ArtifactData.has(stack,ArtifactAux.Trait.BLOODTHIRST))player.heal(event.getAmount()*.18F);
            }
        }
        if(event.getEntity() instanceof ServerPlayer player)shield(player);
    }
    /** Same vanilla absorption authority as the target shields; never install an extra health pool. */
    public static void shield(ServerPlayer player){
        long now=player.level().getGameTime();var data=player.getPersistentData();
        if(data.contains("MyriadArtifactShield")&&now-data.getLong("MyriadArtifactShield")<200)return;
        var worn=new ArrayList<ItemStack>();worn.add(player.getMainHandItem());worn.add(player.getOffhandItem());player.getArmorSlots().forEach(worn::add);
        for(var stack:worn)if(ArtifactData.artifact(stack)&&EquipmentBond.canUse(player,stack)&&ArtifactData.has(stack,ArtifactAux.Trait.SHIELD)){
            int amplifier=Math.max(0,Math.min(255,(2+ArtifactData.quality(stack))/4));
            player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,200,amplifier,false,true,true));
            data.putLong("MyriadArtifactShield",now);return;
        }
    }
    public static int conduct(ItemStack stack){return ArtifactData.conduct(stack);}
    @SubscribeEvent public static void tick(TickEvent.PlayerTickEvent event){
        if(event.phase!=TickEvent.Phase.END||!(event.player instanceof ServerPlayer player)||player.tickCount%20!=0)return;
        var stack=player.getMainHandItem();if(!ArtifactData.artifact(stack)||!EquipmentBond.canUse(player,stack))return;
        var data=GeneratedMod.data(player);if(data==null)return;float amount=conduct(stack)*.15F;
        if(amount>0){data.wuxingxiantu$setAura(Math.min(data.wuxingxiantu$getMaxMana(),data.wuxingxiantu$getAura()+amount));GeneratedMod.sync(player);}
    }
    private ArtifactEffects(){}
}
