package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.client.realmflight.RealmFlightVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.PlayerModelPart;
import net.minecraft.world.item.Items;

/**
 * Keeps the vanilla cape for ordinary movement and gives a flying-sword rider
 * a cloak driven by the same frame as the sword render root.  Vanilla's
 * CapeLayer samples the player's xCloak/yCloak history; that history does not
 * follow the presentation transform used by YujianRidingPose, so its result
 * can become horizontal or detached while the rider is flying.
 */
final class FlightCapeLayer extends CapeLayer {
    FlightCapeLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffers, int light,
                       AbstractClientPlayer player, float limbSwing, float limbSwingAmount,
                       float partial, float age, float netHeadYaw, float headPitch) {
        ControlledSwordEntity sword = player.getVehicle() instanceof ControlledSwordEntity vehicle ? vehicle : null;
        boolean yujian = sword != null && sword.flightRoot() && YujianRidingPose.flightRootActive(player);
        boolean realm = RealmFlightVisuals.bodyTransformActive(player);
        if (!yujian && !realm) {
            super.render(pose, buffers, light, player, limbSwing, limbSwingAmount, partial, age, netHeadYaw, headPitch);
            return;
        }
        if (!player.isCapeLoaded() || player.isInvisible()
                || !player.isModelPartShown(PlayerModelPart.CAPE)
                || player.getCloakTextureLocation() == null
                || player.getItemBySlot(EquipmentSlot.CHEST).is(Items.ELYTRA)) return;

        float lift;
        float bank;
        if (yujian) {
            var frame = InterpolatedFlyingSwordTransform.get(sword, partial);
            float speed = Mth.clamp(frame.speedFactor(), 0F, 1.5F);
            float climb = (float) Mth.clamp(frame.forward().y * 14.0D, -14.0D, 14.0D);
            float turn = Mth.clamp(frame.angular() * 20F * Mth.RAD_TO_DEG, -24F, 24F);
            bank = Mth.clamp(frame.roll() * .35F + turn * .35F, -24F, 24F);
            lift = Mth.clamp(6F + speed * 28F + climb, -6F, 72F);
        } else {
            lift = RealmFlightVisuals.capeLift(player, partial);
            bank = RealmFlightVisuals.capeBank(player, partial);
        }

        // The cape is attached at the shoulders.  The normal 6 degree hang is
        // retained at a hover; speed lifts it behind the flight direction and
        // bank/turn keeps the sheet from staying on a stale world heading.
        pose.pushPose();
        pose.translate(0, 0, 0.125F);
        pose.mulPose(Axis.XP.rotationDegrees(lift));
        pose.mulPose(Axis.ZP.rotationDegrees(bank));
        pose.mulPose(Axis.YP.rotationDegrees(180F - bank * .5F));
        VertexConsumer consumer = buffers.getBuffer(RenderType.entitySolid(player.getCloakTextureLocation()));
        getParentModel().renderCloak(pose, consumer, light, OverlayTexture.NO_OVERLAY);
        pose.popPose();
    }
}
