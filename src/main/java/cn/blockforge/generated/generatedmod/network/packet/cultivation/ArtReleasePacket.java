package cn.blockforge.generated.generatedmod.network.packet.cultivation;

import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import cn.blockforge.generated.generatedmod.ArtCatalog;
import cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem;
import cn.blockforge.generated.generatedmod.arts.ArtParticleLibrary;
import cn.blockforge.generated.generatedmod.arts.FourArtsVisuals;
import cn.blockforge.generated.generatedmod.arts.IceSpikeVolley;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;
import cn.blockforge.generated.generatedmod.*;
import static cn.blockforge.generated.generatedmod.Network.*;

public record ArtReleasePacket(int slot) {
        public static void encode(ArtReleasePacket packet, FriendlyByteBuf buf) { buf.writeByte(packet.slot); }
        public static ArtReleasePacket decode(FriendlyByteBuf buf) { return new ArtReleasePacket(buf.readByte()); }
        public static void handle(ArtReleasePacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || packet.slot < 0 || packet.slot >= 8) return;
                CultivationData data = GeneratedMod.data(player);
                if (data == null) return;
                String[] arts = data.wuxingxiantu$getEquippedArts();
                String art = arts[packet.slot];
                // 持续术法自行校验并按秒结算；再次按原槽可以随时收诀。
                if (cn.blockforge.generated.generatedmod.arts.SustainedSwordArts.supports(art)) {
                    cn.blockforge.generated.generatedmod.arts.SustainedSwordArts.releaseSlot(player,packet.slot);
                    return;
                }
                if (!data.wuxingxiantu$getActiveDivineArt().isBlank()) return;
                int unlocked = CultivationLogic.unlockedArtSlots(data.wuxingxiantu$getRealm());
                if (SwordTechniqueManager.isSwordTechnique(art)) {
                    SwordTechniqueManager.cast(player, art, data.wuxingxiantu$getArtMastery(art));
                    return;
                }
                if (packet.slot >= unlocked || art == null || !ArtCatalog.isValid(art) || !data.wuxingxiantu$hasLearnedArt(art)) return;
                if (art.equals("art_sword_flight")) {
                    if (cn.blockforge.generated.generatedmod.yujian.YujianController.canCast(player, art)) {
                        cn.blockforge.generated.generatedmod.yujian.YujianController.cast(player, art, data.wuxingxiantu$getArtMastery(art));
                    } else {
                        String reason = cn.blockforge.generated.generatedmod.yujian.YujianController.flightDenialReason(player);
                        if (ProgressionGate.MSG_YUJIAN_REALM.equals(reason)) ProgressionGate.deny(player, reason,
                                ProgressionGate.yujianRequiredRealm(data, cn.blockforge.generated.generatedmod.robe.YujianSlot.candidate(player)));
                        else if (reason != null) player.displayClientMessage(Component.translatable(reason), true);
                    }
                    return;
                }
                // 统一进度门槛：释放时重新验证境界（防止旧存档/命令学会后跨阶释放）；
                // 境界不足不释放、不扣灵力、不进入冷却。
                if (!ProgressionGate.canUseArt(player, art)) {
                    ProgressionGate.deny(player, ProgressionGate.MSG_ART_USE, ProgressionGate.requiredRealmForArt(art));
                    return;
                }
                long now = player.level().getGameTime();
                if (!data.wuxingxiantu$hasCreationBlessing() && data.wuxingxiantu$getArtCooldown(packet.slot) > now) {
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.art_cooldown"), true);
                    return;
                }
                int tier = ArtCatalog.tier(art);
                int mastery = data.wuxingxiantu$getArtMastery(art);
                int effectPercent = ArtCatalog.effectPercent(mastery);
                float cost = ArtCatalog.finalManaCost(art, CultivationLogic.maxAura(data));
                if (!SwordTechniqueManager.canCast(player, art)) {
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.sword_technique_need_sword"), true);
                    return;
                }
                if (data.wuxingxiantu$getAura() < cost) {
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.low_mana"), true);
                    return;
                }
                if (FourArtsVisuals.ICE.equals(art) && !IceSpikeVolley.canCast(player)) return;
                net.minecraft.world.phys.Vec3 flashDestination=null;
                if (art.equals("art_flash_step")) {
                    flashDestination=cn.blockforge.generated.generatedmod.arts.FlashStep.destination(player,effectPercent>=30?10:8);
                    if(flashDestination==null) {
                        player.displayClientMessage(Component.translatable("message.wuxingxiantu.flash_step_blocked"),true);
                        return;
                    }
                }
                data.wuxingxiantu$setAura(data.wuxingxiantu$getAura() - cost);
                int cooldown = ArtCatalog.currentCooldownTicks(art, mastery);
                data.wuxingxiantu$setArtCooldown(packet.slot, now + cooldown);
            executeArt(player, art, tier, mastery, effectPercent, flashDestination);
                 // 心法效果按“实际生效心法”结算：境界不足的高阶心法保留学习记录但不产生加成。
                 int masteryGain = "nine_turns".equals(ProgressionGate.effectiveHeartMethod(player, data)) ? 2 : 1;
                 data.wuxingxiantu$addArtMastery(art, masteryGain);
                GeneratedMod.sync(player);
            });
            context.setPacketHandled(true);
        }

        private static void executeArt(ServerPlayer player, String art, int tier, int mastery, int effectPercent, net.minecraft.world.phys.Vec3 flashDestination) {
            var level = player.serverLevel();
            int masteryLevel = ArtCatalog.masteryLevel(mastery);
            int buffTicks = (6 + masteryLevel) * 20;
            double playerAttack = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            float damage = ArtCatalog.damageFor(art, playerAttack, mastery, CultivationLogic.maxAura(GeneratedMod.data(player)));
            if (art.equals("art_flame_wave")) damage *= cn.blockforge.generated.generatedmod.balance.HeartMethodBalance.fireDamageMultiplier(
                    cn.blockforge.generated.generatedmod.ProgressionGate.effectiveHeartMethod(player,GeneratedMod.data(player)));
            if (art.equals(FourArtsVisuals.RENEWAL) || art.equals(FourArtsVisuals.PURIFICATION)) {
                float healing=ArtCatalog.currentHealing(art, mastery);
                if (art.equals(FourArtsVisuals.RENEWAL)) FourArtsVisuals.renew(player,healing);
                else FourArtsVisuals.purify(player,healing);
            }
            if (art.equals("art_spirit_shield") || art.equals("art_golden_bell") || art.equals("art_immovable_mountain")) {
                int duration = ArtCatalog.shieldDurationTicks(art, mastery);
                if (art.equals("art_spirit_shield")) player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        cn.blockforge.generated.generatedmod.ModEffects.SPIRIT_SHIELD_VISUAL.get(),duration,0,false,false,false));
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.ABSORPTION, duration,
                        ArtCatalog.shieldAmplifier(art,player.getMaxHealth()),false,!art.equals("art_spirit_shield"),true));
            }
            if (art.equals("art_light_body")) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, buffTicks, 1, false, false, true));
                FourArtsVisuals.cast(player,art,buffTicks);
            }
            if (art.equals("art_flash_step")) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, buffTicks, 1));
                player.teleportTo(flashDestination.x,flashDestination.y,flashDestination.z);
                player.fallDistance=0;
            }
            if (art.equals("art_spirit_gathering")) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.REGENERATION, buffTicks, 1, false, false, true));
                FourArtsVisuals.cast(player,art,buffTicks);
            }
            if (art.equals("art_bloodlust") || art.equals("art_human_harmony")) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.DAMAGE_BOOST, buffTicks, tier == 4 ? 2 : 1, false, !art.equals(FourArtsVisuals.BLOOD), true));
                if (art.equals(FourArtsVisuals.BLOOD)) FourArtsVisuals.cast(player,art,buffTicks);
            }
            if (art.equals("art_immobilize")) {
                int controlTicks = (2 + masteryLevel / 2) * 20;
                for (net.minecraft.world.entity.LivingEntity target : level.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class, player.getBoundingBox().inflate(3),
                        e -> e != player && e.isAlive() && !e.isAlliedTo(player)
                                && !cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem.isServant(e))) {
                    target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                            net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, controlTicks, 10));
                    target.setDeltaMovement(0.0D, target.getDeltaMovement().y, 0.0D);
                }
            }
            if (art.equals(FourArtsVisuals.ICE)) IceSpikeVolley.cast(player, damage);
            if (art.equals("art_sword_aura") || art.equals("art_armor_break")
                    || art.equals("art_flame_wave") || art.equals("art_thunder") || art.equals("art_myriad_swords")
                    || art.equals("art_five_elements")) {
                ArtCatalog.AttackProfile profile = ArtCatalog.attackProfile(art);
                double radius = profile.shape() == ArtCatalog.AttackShape.TARGET_AREA ? profile.width() : profile.range();
                net.minecraft.world.phys.Vec3 origin = player.position();
                net.minecraft.world.phys.Vec3 forward = new net.minecraft.world.phys.Vec3(player.getLookAngle().x, 0.0D, player.getLookAngle().z);
                if (forward.lengthSqr() < 1.0E-6D) forward = new net.minecraft.world.phys.Vec3(0, 0, 1);
                forward = forward.normalize();
                net.minecraft.world.phys.Vec3 center = profile.shape() == ArtCatalog.AttackShape.TARGET_AREA
                        ? player.getEyePosition().add(player.getLookAngle().scale(profile.range())) : origin;
                if (profile.shape() == ArtCatalog.AttackShape.CONE || profile.shape() == ArtCatalog.AttackShape.LINE) {
                    double max = profile.range();
                    net.minecraft.world.phys.Vec3 from = origin.add(0, 0.8, 0);
                    net.minecraft.world.phys.Vec3 to = from.add(forward.scale(max));
                    ArtParticleLibrary.emit(level, art, ArtVisualPacket.ARC, from, to, max, 0);
                } else if(art.equals("art_thunder")){
                    var impact=level.clip(new net.minecraft.world.level.ClipContext(center.add(0,32,0),center.add(0,-4,0),net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,player));
                    boolean grounded=impact.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK;
                    cn.blockforge.generated.generatedmod.tribulation.ThunderField.emit(level,player,grounded?impact.getLocation():center,GeneratedMod.data(player).wuxingxiantu$getRealm(),true,grounded);
                } else {
                    ArtParticleLibrary.emit(level, art, ArtVisualPacket.AREA, center, center, radius, 0);
                }
                for (net.minecraft.world.entity.LivingEntity target : level.getEntitiesOfClass(
                        net.minecraft.world.entity.LivingEntity.class,
                        profile.shape() == ArtCatalog.AttackShape.TARGET_AREA
                                ? new net.minecraft.world.phys.AABB(center.x - radius, center.y - 2, center.z - radius,
                                center.x + radius, center.y + 4, center.z + radius)
                                : player.getBoundingBox().inflate(radius),
                        e -> e != player && e.isAlive() && !e.isAlliedTo(player)
                                && !cn.blockforge.generated.generatedmod.items.HumanEmperorBannerItem.isServant(e))) {
                    net.minecraft.world.phys.Vec3 delta = target.position().subtract(profile.shape() == ArtCatalog.AttackShape.TARGET_AREA ? center : origin);
                    boolean inShape = switch (profile.shape()) {
                        case CONE -> delta.horizontalDistance() <= profile.range()
                                && new net.minecraft.world.phys.Vec3(delta.x, 0, delta.z).normalize().dot(forward)
                                >= Math.cos(Math.toRadians(profile.angleDegrees() * .5D));
                        case LINE -> Math.abs(delta.x * forward.z - delta.z * forward.x) <= profile.width() * .5D
                                && delta.x * forward.x + delta.z * forward.z >= 0 && delta.x * forward.x + delta.z * forward.z <= profile.range();
                        case TARGET_AREA, CIRCLE -> delta.horizontalDistance() <= radius;
                        default -> true;
                    };
                    if (!inShape) continue;
                    var source=art.equals("art_thunder")?new net.minecraft.world.damagesource.DamageSource(level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE).getHolderOrThrow(net.minecraft.world.damagesource.DamageTypes.LIGHTNING_BOLT),player):player.damageSources().magic();
                    boolean hit = target.hurt(source, art.equals("art_thunder")?damage*1.35F:damage);
                    if (hit && art.equals("art_flame_wave")) target.setSecondsOnFire(4);
                    if (art.equals("art_armor_break")) {
                        target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                                ModEffects.ARMOR_BREAK.get(), 100, 0, false, true, true));
                    }
                    if (art.equals("art_thunder")) {
                        if(hit)cn.blockforge.generated.generatedmod.ParalysisControl.apply(target,30+tier*4);
                    }
                }
            }
            if(!art.equals("art_thunder") && !FourArtsVisuals.replaces(art)) emitArtParticles(level, player, art);
        }

        /** 每种功法使用不同的粒子组合，保证释放时能立即辨认技能类型。 */
        private static void emitArtParticles(ServerLevel level, ServerPlayer player, String art) {
            if (art.equals("art_spirit_shield")) return;
            net.minecraft.world.phys.Vec3 origin = player.position().add(0.0D, 1.0D, 0.0D);
            net.minecraft.world.phys.Vec3 end = origin.add(player.getLookAngle().scale(
                    Math.max(1.0D, ArtCatalog.attackProfile(art).range())));
            ArtCatalog.AttackProfile profile = ArtCatalog.attackProfile(art);
            if (profile.damaging()) {
                int mode = profile.shape() == ArtCatalog.AttackShape.TARGET_AREA
                        ? ArtVisualPacket.AREA : profile.shape() == ArtCatalog.AttackShape.LINE
                        ? ArtVisualPacket.PROJECTILE : ArtVisualPacket.ARC;
                double size = profile.shape() == ArtCatalog.AttackShape.TARGET_AREA ? profile.width() : profile.range();
                ArtParticleLibrary.emit(level, art, mode, profile.shape() == ArtCatalog.AttackShape.TARGET_AREA
                        ? player.getEyePosition().add(player.getLookAngle().scale(profile.range())) : origin, end, size, 0);
            } else {
                ArtParticleLibrary.emit(level, art, ArtVisualPacket.AURA, origin, origin, 1.8D, 0);
            }
        }
    }
