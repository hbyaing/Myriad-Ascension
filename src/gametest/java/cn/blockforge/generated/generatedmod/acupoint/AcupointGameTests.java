package cn.blockforge.generated.generatedmod.acupoint;

import cn.blockforge.generated.generatedmod.CultivationDataImpl;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.event.PlayerLifecycleEvents;
import cn.blockforge.generated.generatedmod.fixtures.GameTestPlayer;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDamageEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.CriticalHitEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.List;
import java.util.UUID;

import static cn.blockforge.generated.generatedmod.acupoint.AcupointCatalog.Stat.*;
import static cn.blockforge.generated.generatedmod.acupoint.AcupointPackets.Action.*;
import static cn.blockforge.generated.generatedmod.acupoint.AcupointProgress.Result.*;

@GameTestHolder(GeneratedMod.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AcupointGameTests {
    @GameTest(template = "empty")
    public static void baiqiaoAllStagesAndRewards(GameTestHelper h) {
        var data = new CultivationDataImpl();
        var state = data.acupoints();
        var catalog = state.catalog();
        h.assertTrue(catalog.points().size() == 361 && catalog.meridians().size() == 14
                && catalog.markers().values().stream().mapToInt(List::size).sum() == 670, "Wrong catalog dimensions");
        var expected = new int[] {36, 108, 216, 361};
        int totalPaid = 0;
        for (int realm = 0; realm < expected.length; realm++) {
            data.wuxingxiantu$setRealm(realm);
            for (var route : catalog.routes().values()) {
                for (var point : route) {
                    if (state.eligibility(point, realm + 1) != IDLE) continue;
                    data.setMaxMana(point.cost() + 5);
                    data.setCurrentMana(point.cost() + 5);
                    h.assertTrue(state.charge(point, data, point.cost() + 10) == OPENED, "Cannot reach " + point.id());
                    near(h, 5, data.getCurrentMana(), "Paid above cost for " + point.id());
                    totalPaid += point.cost();
                }
            }
            h.assertTrue(state.bonuses().total() == expected[realm], "Bad realm quota " + realm);
        }
        h.assertTrue(totalPaid == 123864, "Wrong total mana cost");
        var bonus = state.bonuses();
        h.assertTrue(bonus.minor() && bonus.major() && bonus.complete() == 14, "Missing cycles");
        double[] expectedValues = {10.9, 155, 21, 2.9, 3.3, 13.75, 4.46, 1.25, 10, 8, 20, 10,
                12, 15, 10, 12, 6, 4, .20, 1.5, .50, .35, 8, 8, 6, 4};
        for (var stat : AcupointCatalog.Stat.values()) near(h, expectedValues[stat.ordinal()], bonus.get(stat), stat.key());
        h.assertTrue(bonus == state.bonuses(), "Unchanged stats were rebuilt");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void baiqiaoPartialLegacyAndDeathClone(GameTestHelper h) {
        try (var first = new GameTestPlayer(h.getLevel()); var other = new GameTestPlayer(h.getLevel())) {
            var data = GeneratedMod.data(first);
            var state = data.acupoints();
            var catalog = state.catalog();
            var root = catalog.points().get("CV-6");
            var next = catalog.points().get("CV-5");
            data.setCurrentMana(4.75F);
            h.assertTrue(state.charge(next, data, 10) == PREREQUISITE, "Missing prerequisite accepted");
            h.assertTrue(state.charge(catalog.points().get("BL-1"), data, 10) == REALM_LOCKED, "Higher realm accepted");
            h.assertTrue(state.charge(root, data, 10) == SPENT, "Partial investment failed");
            near(h, .75, data.getCurrentMana(), "Fractional balance lost");
            h.assertTrue(state.charge(root, data, 10) == MANA_EMPTY && state.invested(root) == 4, "Empty mana changed progress");
            state.select(root.id());
            state.configureAuto("CV", true);
            h.assertTrue(GeneratedMod.data(other).acupoints().invested(root) == 0, "Players share progress");
            PlayerLifecycleEvents.clonePlayer(new PlayerEvent.Clone(other, first, true));
            var clone = GeneratedMod.data(other).acupoints();
            h.assertTrue(clone.invested(root) == 4 && clone.auto() && clone.selected().equals(root.id()), "Death clone lost progress");
            var reloaded = new CultivationDataImpl();
            reloaded.deserializeNBT(((CultivationDataImpl) GeneratedMod.data(other)).serializeNBT());
            h.assertTrue(reloaded.acupoints().invested(root) == 4, "Save round-trip lost progress");
            var legacy = reloaded.serializeNBT();
            legacy.remove("Baiqiao");
            reloaded.deserializeNBT(legacy);
            h.assertTrue(reloaded.acupoints().bonuses().total() == 0 && reloaded.acupoints().invested(root) == 0, "Legacy save did not start fresh");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void baiqiaoAttributeOrderAndHalfRewards(GameTestHelper h) {
        try (var player = new GameTestPlayer(h.getLevel())) {
            var data = GeneratedMod.data(player);
            data.wuxingxiantu$setRealm(3);
            var state = data.acupoints();
            var catalog = state.catalog();
            CultivationLogic.applyHealthBonus(player, data);
            var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
            attack.addTransientModifier(new AttributeModifier(UUID.randomUUID(), "qa multiplier", .50, AttributeModifier.Operation.MULTIPLY_TOTAL));
            double before = attack.getValue();
            var meridian = catalog.meridians().get("LI");
            for (var point : catalog.routes().get("LI")) {
                data.setMaxMana(point.cost());
                data.setCurrentMana(point.cost());
                h.assertTrue(state.charge(point, data, point.cost()) == OPENED, "Cannot progress route");
                if (point.order() == meridian.half()) near(h, 1.2, state.bonuses().get(MINING_SPEED_PCT), "Half milestone");
            }
            double increment = state.bonuses().get(ATTACK);
            for (int i = 0; i < 3; i++) CultivationLogic.applyHealthBonus(player, data);
            near(h, before + increment, attack.getValue(), "Flat attack multiplied or awarded repeatedly");
            near(h, 4, state.bonuses().get(MINING_SPEED_PCT), "Full reward stacked with half");
        }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void baiqiaoServerChargeModes(GameTestHelper h) {
        var player = new GameTestPlayer(h.getLevel());
        player.cleanupAfter(h, () -> {});
        var data = GeneratedMod.data(player);
        var state = data.acupoints();
        var catalog = state.catalog();
        var root = catalog.points().get("CV-6");
        var tick = new TickEvent.PlayerTickEvent(TickEvent.Phase.END, player);
        String version = catalog.version();
        data.setCurrentMana(80);
        AcupointService.handle(player, new AcupointPackets.Request(TAP, root.id(), "wrong"));
        h.assertTrue(state.invested(root) == 0, "Mismatched data accepted");
        AcupointService.handle(player, new AcupointPackets.Request(TAP, root.id(), version));
        AcupointService.handle(player, new AcupointPackets.Request(TAP, root.id(), version));
        h.assertTrue(state.invested(root) == 10, "Tap not rate limited");
        h.runAfterDelay(6, () -> AcupointService.handle(player, new AcupointPackets.Request(START, root.id(), version)));
        h.runAfterDelay(17, () -> {
            AcupointService.tick(tick);
            h.assertTrue(state.invested(root) == 12, "Hold did not charge at fixed interval");
            AcupointService.damage(new LivingDamageEvent(player, player.damageSources().generic(), 1));
        });
        h.runAfterDelay(28, () -> {
            AcupointService.tick(tick);
            h.assertTrue(state.invested(root) == 12, "Damage failed to stop channel");
            AcupointService.handle(player, new AcupointPackets.Request(AUTO_ON, "CV", version));
        });
        h.runAfterDelay(39, () -> {
            AcupointService.tick(tick);
            h.assertTrue(state.invested(root) == 12, "Auto spent outside meditation");
            data.wuxingxiantu$setMeditating(true);
        });
        h.runAfterDelay(50, () -> {
            AcupointService.tick(tick);
            h.assertTrue(state.invested(root) == 14, "Auto failed while meditating");
            near(h, 66, data.getCurrentMana(), "Mana and charge progress diverged");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void baiqiaoCoreCombatAndMovement(GameTestHelper h) throws ReflectiveOperationException {
        try (var player = new GameTestPlayer(h.getLevel())) {
            var victim = h.spawn(EntityType.ZOMBIE, 1, 2, 1);
            var data = GeneratedMod.data(player);
            var ticker = LivingEntity.class.getDeclaredField("attackStrengthTicker");
            ticker.setAccessible(true);
            ticker.setInt(player, 100);
            h.assertTrue(ForgeHooks.getCriticalHit(player, victim, false, 1) == null, "Unopened catalog added critical chance");

            var saved = data.acupoints().save();
            var opened = new ListTag();
            data.acupoints().catalog().points().keySet().forEach(id -> opened.add(StringTag.valueOf(id)));
            saved.put("Opened", opened);
            data.acupoints().load(saved);
            double speed = player.getAttributeValue(Attributes.ATTACK_SPEED);
            double swim = player.getAttributeValue(ForgeMod.SWIM_SPEED.get());
            double gravity = player.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get());
            double blockReach = player.getAttributeValue(ForgeMod.BLOCK_REACH.get());
            double meleeReach = player.getAttributeValue(ForgeMod.ENTITY_REACH.get());
            double step = player.getAttributeValue(ForgeMod.STEP_HEIGHT_ADDITION.get());
            double luck = player.getAttributeValue(Attributes.LUCK);
            double knockback = player.getAttributeValue(Attributes.ATTACK_KNOCKBACK);
            double resistance = player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE);
            AcupointAttributes.apply(player, data);
            AcupointAttributes.apply(player, data);
            near(h, speed * 1.10, player.getAttributeValue(Attributes.ATTACK_SPEED), "Attack cooldown attribute");
            near(h, swim * 1.12, player.getAttributeValue(ForgeMod.SWIM_SPEED.get()), "Swim speed");
            near(h, gravity * .96, player.getAttributeValue(ForgeMod.ENTITY_GRAVITY.get()), "Gravity");
            near(h, blockReach + .5, player.getAttributeValue(ForgeMod.BLOCK_REACH.get()), "Block reach");
            near(h, meleeReach + .35, player.getAttributeValue(ForgeMod.ENTITY_REACH.get()), "Melee reach");
            near(h, step + .2, player.getAttributeValue(ForgeMod.STEP_HEIGHT_ADDITION.get()), "Step height");
            near(h, luck + 1.5, player.getAttributeValue(Attributes.LUCK), "Luck");
            near(h, knockback, player.getAttributeValue(Attributes.ATTACK_KNOCKBACK), "Knockback changed");
            near(h, resistance, player.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), "Knockback resistance changed");

            var jump = ForgeHooks.getCriticalHit(player, victim, true, 1.5F);
            h.assertTrue(jump != null && jump.isVanillaCritical(), "Jump critical disappeared");
            near(h, 1.65, jump.getDamageModifier(), "Critical damage modifier");
            var denied = new CriticalHitEvent(player, victim, 1.5F, true);
            denied.setResult(Event.Result.DENY);
            AcupointBenefitEvents.critical(denied);
            h.assertTrue(denied.getResult() == Event.Result.DENY && denied.getDamageModifier() == 1.5F, "Forge denial overwritten");
            player.getRandom().setSeed(361);
            int chargedCriticals = 0;
            for (int i = 0; i < 128; i++) {
                var hit = ForgeHooks.getCriticalHit(player, victim, false, 1);
                if (hit != null) {
                    chargedCriticals++;
                    near(h, 1.65, hit.getDamageModifier(), "Rolled critical stacked or omitted damage");
                }
            }
            h.assertTrue(chargedCriticals > 2 && chargedCriticals < 35, "Charged critical rate out of range");
            player.resetAttackStrengthTicker();
            for (int i = 0; i < 64; i++)
                h.assertTrue(ForgeHooks.getCriticalHit(player, victim, false, 1) == null, "Uncharged swing rolled a critical");

            var arrow = new Arrow(h.getLevel(), player);
            var ranged = new LivingHurtEvent(victim, player.damageSources().arrow(arrow, player), 10);
            AcupointBenefitEvents.ranged(ranged);
            near(h, 10.8, ranged.getAmount(), "Arrow damage");
            var melee = new LivingHurtEvent(victim, player.damageSources().playerAttack(player), 10);
            AcupointBenefitEvents.ranged(melee);
            near(h, 10, melee.getAmount(), "Ranged bonus applied to melee");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void baiqiaoCatalogAndPacketRoundTrip(GameTestHelper h) {
        var data = new CultivationDataImpl();
        var catalog = data.acupoints().catalog();
        var packet = new AcupointPackets.State(data.acupoints().save(), catalog.version(), catalog.documents(), "CV-6", SPENT);
        var buf = new FriendlyByteBuf(Unpooled.buffer());
        try {
            AcupointPackets.State.encode(packet, buf);
            var received = AcupointPackets.State.decode(buf);
            h.assertTrue(received.equals(packet) && buf.readableBytes() == 0, "Catalog/state packet round-trip changed");
            h.assertTrue(AcupointCatalog.parse(received.documents()).version().equals(catalog.version()), "Catalog version mismatch");
            var bad = new java.util.ArrayList<>(received.documents());
            bad.set(0, bad.get(0).replaceFirst("\"LU-7\"", "\"ZZ-7\""));
            try {
                AcupointCatalog.parse(bad);
                h.fail("Missing predecessor was accepted");
            } catch (IllegalArgumentException expected) {
                h.assertTrue(expected.getMessage().contains("LU-6"), "Bad config diagnostic omitted point ID");
            }
        } finally { buf.release(); }
        h.succeed();
    }

    private static void near(GameTestHelper h, double expected, double actual, String label) {
        h.assertTrue(Math.abs(expected - actual) < .001, label + ": " + actual + " vs " + expected);
    }
}
