package cn.blockforge.generated.generatedmod.event;

import cn.blockforge.generated.generatedmod.CultivationData;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.ArtCatalog;
import cn.blockforge.generated.generatedmod.arts.FourArtsVisuals;
import java.util.Arrays;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Applies the three passive slots on a short refresh window; effects survive ordinary packet updates. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID)
public final class ArtPassiveTicker {
    private ArtPassiveTicker() {}

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.getServer().overworld().getGameTime() % 20L != 0L) return;
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            if (!player.isAlive() || player.isSpectator()) continue;
            CultivationData data = GeneratedMod.data(player);
            if (data == null || !data.wuxingxiantu$getActiveDivineArt().isBlank()) continue;
            String[] passiveArts=data.wuxingxiantu$getPassiveArts();
            boolean renewal=Arrays.asList(passiveArts).contains(FourArtsVisuals.RENEWAL);
            int level = 1;
            for (String art : passiveArts) {
                if (!ArtCatalog.isPassiveCompatible(art)) continue;
                int mastery = data.wuxingxiantu$getArtMastery(art);
                level = Math.max(level, ArtCatalog.masteryLevel(mastery));
                switch (art) {
                    case "art_purification" -> {
                        if(!renewal) effect(player,MobEffects.REGENERATION,45,Math.min(1,level/3));
                    }
                    case "art_spirit_shield" -> {
                        // Refreshing absorption replenishes its hearts. Let a finite
                        // shield finish instead of refilling it once per second.
                        if(player.hasEffect(cn.blockforge.generated.generatedmod.ModEffects.SPIRIT_SHIELD_VISUAL.get()))break;
                        effect(player, MobEffects.ABSORPTION, 45, 0);
                        player.addEffect(new MobEffectInstance(cn.blockforge.generated.generatedmod.ModEffects.SPIRIT_SHIELD_VISUAL.get(),45,0,true,false,false));
                    }
                    case "art_golden_bell" -> effect(player, MobEffects.DAMAGE_RESISTANCE, 45, 0);
                    case "art_immovable_mountain" -> {
                        effect(player, MobEffects.DAMAGE_RESISTANCE, 45, 1);
                    }
                    case "art_light_body" -> {
                        effect(player, MobEffects.MOVEMENT_SPEED, 45, 0);
                        effect(player, MobEffects.SLOW_FALLING, 45, 0);
                    }
                    case "art_spirit_gathering" -> {
                        float max = CultivationLogic.maxAura(data);
                        float amount = Math.max(0.25F, max * (0.004F + level * 0.001F));
                        float before=data.wuxingxiantu$getAura();
                        float after=Math.min(max,before+amount);
                        data.wuxingxiantu$setAura(after);
                        if(after>before) FourArtsVisuals.gathered(player);
                    }
                    case "art_bloodlust" -> effect(player, MobEffects.DAMAGE_BOOST, 45, 0);
                    case "art_human_harmony" -> {
                        effect(player, MobEffects.DAMAGE_BOOST, 45, 1);
                        effect(player, MobEffects.DAMAGE_RESISTANCE, 45, 0);
                    }
                    default -> { }
                }
            }
            if (data.wuxingxiantu$getPassiveArts().length > 0) GeneratedMod.sync(player);
        }
    }
    @SubscribeEvent
    public static void renew(TickEvent.PlayerTickEvent event) {
        if(event.phase!=TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || !player.isAlive() || player.isSpectator()) return;
        CultivationData data=GeneratedMod.data(player);
        if(data==null || !data.wuxingxiantu$getActiveDivineArt().isBlank()) return;
        String[] slots=data.wuxingxiantu$getPassiveArts();
        if(!Arrays.asList(slots).contains(FourArtsVisuals.RENEWAL)) return;
        int level=1, amplifier=0;
        for(String art:slots) {
            if(!ArtCatalog.isPassiveCompatible(art)) continue;
            level=Math.max(level,ArtCatalog.masteryLevel(data.wuxingxiantu$getArtMastery(art)));
            if(FourArtsVisuals.RENEWAL.equals(art) || "art_purification".equals(art))
                amplifier=Math.max(amplifier,Math.min(1,level/3));
        }
        // Vanilla regeneration heals at a duration multiple of its interval.
        // Refreshing a 45-tick instance every 20 ticks starves that boundary.
        var current=player.getEffect(MobEffects.REGENERATION);
        if(current==null || current.getAmplifier()<amplifier)
            effect(player,MobEffects.REGENERATION,50>>amplifier,amplifier);
    }

    private static void effect(ServerPlayer player, MobEffect effect, int duration, int amplifier) {
        player.addEffect(new MobEffectInstance(effect, duration, amplifier, true, false, true));
    }
}
