package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

/** Standing limbs and their clothing, with action poses retaining priority. No root transform. */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class JindouCloudPlayerPose {
    private static final Map<HumanoidModel<?>, Saved> SAVED = new IdentityHashMap<>();

    private JindouCloudPlayerPose() {}

    public static void remember(HumanoidModel<?> model, UUID owner) {
        restore(model);
        SAVED.put(model, new Saved(owner, model.leftLeg.storePose(), model.rightLeg.storePose(),
                model instanceof PlayerModel<?> skin ? skin.leftPants.storePose() : null,
                model instanceof PlayerModel<?> skin ? skin.rightPants.storePose() : null,
                model.leftArm.storePose(), model.rightArm.storePose(),
                model instanceof PlayerModel<?> skin ? skin.leftSleeve.storePose() : null,
                model instanceof PlayerModel<?> skin ? skin.rightSleeve.storePose() : null));
    }

    public static void stabilizeIdleArms(PlayerModel<?> model) {
        // Custom/two-handed item poses are deliberate actions, even when isUsingItem is false.
        stabilizeArm(model.leftArm, model.leftArmPose, -1);
        stabilizeArm(model.rightArm, model.rightArmPose, 1);
        model.leftSleeve.copyFrom(model.leftArm);
        model.rightSleeve.copyFrom(model.rightArm);
    }

    private static void stabilizeArm(ModelPart arm, HumanoidModel.ArmPose pose, int side) {
        if (pose != HumanoidModel.ArmPose.EMPTY && pose != HumanoidModel.ArmPose.ITEM) return;
        arm.xRot = pose == HumanoidModel.ArmPose.ITEM ? -0.31415927F : 0;
        arm.yRot = 0;
        arm.zRot = side * 0.06F;
    }

    public static void apply(PlayerModel<?> model, UUID owner) {
        remember(model, owner);
        model.leftLeg.setPos(1.9F, 12, 0);
        model.rightLeg.setPos(-1.9F, 12, 0);
        model.leftLeg.xRot = model.rightLeg.xRot = 0.025F;
        model.leftLeg.yRot = model.rightLeg.yRot = 0;
        model.leftLeg.zRot = model.rightLeg.zRot = 0;
        model.leftPants.copyFrom(model.leftLeg);
        model.rightPants.copyFrom(model.rightLeg);
    }

    public static void restore(HumanoidModel<?> model) {
        Saved saved = SAVED.remove(model);
        if (saved == null) return;
        model.leftLeg.loadPose(saved.left());
        model.rightLeg.loadPose(saved.right());
        model.leftArm.loadPose(saved.leftArm());
        model.rightArm.loadPose(saved.rightArm());
        if (model instanceof PlayerModel<?> skin) {
            skin.leftPants.loadPose(saved.leftPants());
            skin.rightPants.loadPose(saved.rightPants());
            skin.leftSleeve.loadPose(saved.leftSleeve());
            skin.rightSleeve.loadPose(saved.rightSleeve());
        }
    }

    @SubscribeEvent
    public static void after(RenderPlayerEvent.Post event) {
        var models = SAVED.entrySet().stream().filter(e -> e.getValue().owner().equals(event.getEntity().getUUID()))
                .map(Map.Entry::getKey).toList();
        models.forEach(JindouCloudPlayerPose::restore);
    }

    @SubscribeEvent
    public static void beforeFrame(TickEvent.RenderTickEvent event) {
        // Also restores after an interrupted render or world unload where Post was never delivered.
        if (event.phase == TickEvent.Phase.START) {
            new java.util.ArrayList<>(SAVED.keySet()).forEach(JindouCloudPlayerPose::restore);
        }
    }

    private record Saved(UUID owner, PartPose left, PartPose right, PartPose leftPants, PartPose rightPants,
                         PartPose leftArm, PartPose rightArm, PartPose leftSleeve, PartPose rightSleeve) {}
}
