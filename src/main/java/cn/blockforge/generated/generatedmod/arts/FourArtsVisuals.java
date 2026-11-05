package cn.blockforge.generated.generatedmod.arts;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.network.NetworkChannel;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FourArtsPacket;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.FourArtsPacket.Action;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHealEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import java.util.List;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID)
public final class FourArtsVisuals {
    public static final String LIGHT="art_light_body", RENEWAL="art_renewal", PURIFICATION="art_purification";
    public static final String GATHER="art_spirit_gathering", BLOOD="art_bloodlust", ICE="art_ice_spike";
    public static final String FLAME="art_flame_wave";
    public static final int WIND=1, PASSIVE_WIND=2, SPIRIT=4, BLOOD_AURA=8;
    private static final String LAST_STATE="FourArtsVisualState";
    private static final String ACTIVE_HEAL="FourArtsActiveHeal";

    private FourArtsVisuals() {}

    public static boolean replaces(String art) {
        return LIGHT.equals(art)||RENEWAL.equals(art)||GATHER.equals(art)||BLOOD.equals(art)
                ||PURIFICATION.equals(art)||ICE.equals(art)||FLAME.equals(art);
    }

    static void flame(ServerPlayer player,Vec3 origin,float yaw) {
        NetworkChannel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->player),
                new FourArtsPacket(player.getId(),Action.FLAME_WAVE,0,origin,yaw));
    }

    public static void renew(ServerPlayer player,float amount) {
        activeHeal(player,amount);
        send(player,Action.RENEWAL,0,Vec3.ZERO);
    }

    public static void purify(ServerPlayer player,float amount) {
        activeHeal(player,amount);
        int removed=0;
        for(var effect:List.copyOf(player.getActiveEffects())) {
            if(effect.getEffect().getCategory()==MobEffectCategory.HARMFUL && player.removeEffect(effect.getEffect()))
                removed++;
        }
        send(player,Action.PURIFICATION,Math.min(removed,4),Vec3.ZERO);
    }

    private static void activeHeal(ServerPlayer player,float amount) {
        var tag=player.getPersistentData();
        tag.putBoolean(ACTIVE_HEAL,true);
        try {
            player.heal(amount);
        } finally {
            tag.remove(ACTIVE_HEAL);
        }
    }

    public static void cast(ServerPlayer player,String art,int ticks) {
        player.getPersistentData().putLong(key(art),now(player)+ticks);
        sync(player);
    }

    private static long now(ServerPlayer player) {
        return player.server.overworld().getGameTime();
    }

    private static String key(String art) { return "FourArtsVisualUntil."+art; }
    private static boolean active(ServerPlayer player,String art) {
        return player.getPersistentData().getLong(key(art))>now(player);
    }

    private static boolean passive(ServerPlayer player,String art) {
        var data=GeneratedMod.data(player);
        if(data==null || !data.wuxingxiantu$getActiveDivineArt().isBlank()) return false;
        for(String slot:data.wuxingxiantu$getPassiveArts()) if(art.equals(slot)) return true;
        return false;
    }

    @SubscribeEvent
    public static void tick(TickEvent.PlayerTickEvent event) {
        if(event.phase==TickEvent.Phase.END && event.player instanceof ServerPlayer player && player.tickCount%10==0) sync(player);
    }

    private static void sync(ServerPlayer player) {
        int state=0;
        if(player.isAlive() && !player.isSpectator()) {
            if(active(player,LIGHT)&&player.hasEffect(MobEffects.MOVEMENT_SPEED)) state|=WIND;
            if(passive(player,LIGHT)&&player.hasEffect(MobEffects.SLOW_FALLING)) state|=PASSIVE_WIND;
            if(active(player,GATHER)&&player.hasEffect(MobEffects.REGENERATION)) state|=SPIRIT;
            if(active(player,BLOOD)&&player.hasEffect(MobEffects.DAMAGE_BOOST)) state|=BLOOD_AURA;
        }
        var tag=player.getPersistentData();
        if(state!=0 || tag.getInt(LAST_STATE)!=0) {
            tag.putInt(LAST_STATE,state);
            send(player,Action.STATE,state,Vec3.ZERO);
        }
    }

    public static void gathered(ServerPlayer player) {
        send(player,Action.GATHER_PULSE,0,Vec3.ZERO);
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void healed(LivingHealEvent event) {
        if(event.getEntity() instanceof ServerPlayer player && event.getAmount()>0
                && player.getHealth()<player.getMaxHealth() && player.hasEffect(MobEffects.REGENERATION)
                && !player.getPersistentData().getBoolean(ACTIVE_HEAL)) {
            if(passive(player,RENEWAL)) send(player,Action.PASSIVE_RENEWAL,0,Vec3.ZERO);
            else if(passive(player,PURIFICATION)) send(player,Action.PASSIVE_PURIFICATION,0,Vec3.ZERO);
        }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void hit(LivingDamageEvent event) {
        var source=event.getSource();
        if(event.getAmount()<=0 || !(source.getDirectEntity() instanceof ServerPlayer player)
                || source.getEntity()!=player || !source.is(DamageTypes.PLAYER_ATTACK)
                || source.is(DamageTypeTags.IS_PROJECTILE) || !player.hasEffect(MobEffects.DAMAGE_BOOST)) return;
        boolean active=active(player,BLOOD);
        if(!active&&!passive(player,BLOOD)) return;
        var bounds=event.getEntity().getBoundingBox();
        var eye=player.getEyePosition();
        var edge=new Vec3(Mth.clamp(eye.x,bounds.minX,bounds.maxX),
                Mth.clamp(eye.y,bounds.minY+bounds.getYsize()*.28,bounds.minY+bounds.getYsize()*.76),
                Mth.clamp(eye.z,bounds.minZ,bounds.maxZ));
        send(player,active?Action.BLOOD_HIT:Action.PASSIVE_BLOOD_HIT,0,edge.add(eye.subtract(edge).normalize().scale(.075)));
    }

    private static void send(ServerPlayer player,Action action,int state,Vec3 point) {
        NetworkChannel.CHANNEL.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->player),
                new FourArtsPacket(player.getId(),action,state,point));
    }
}
