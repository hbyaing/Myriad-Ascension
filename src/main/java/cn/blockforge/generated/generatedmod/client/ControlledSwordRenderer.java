package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;

public final class ControlledSwordRenderer extends EntityRenderer<ControlledSwordEntity>{
    private final net.minecraft.client.renderer.entity.ItemRenderer items;
    public ControlledSwordRenderer(EntityRendererProvider.Context context){super(context);items=context.getItemRenderer();}
    @Override public void render(ControlledSwordEntity sword,float yaw,float partial,PoseStack pose,MultiBufferSource buffers,int light){
        if(cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.TRAILS.get()&&sword.flightRoot()&&sword.huahongStrength()>=.995F)return;
        buffers=DunxuTransparency.wrap(sword,buffers);
        pose.pushPose();
        var definition=cn.blockforge.generated.generatedmod.yujian.FlyingSwordRegistry.visual(sword.display());
        boolean custom=definition.baked3d();
        if (sword.flightRoot() || custom) {
            var frame=InterpolatedFlyingSwordTransform.get(sword,partial);
            pose.mulPose(frame.rotation());
            // BB tip is -X, top is +Y. Convert to the shared +Z-forward flight frame.
            SwordFlightAttachment.rotateModel(pose,definition.modelYaw(),definition.modelPitch(),definition.modelRoll());
        } else {
            pose.mulPose(Axis.YP.rotationDegrees(180-yaw));
            pose.mulPose(Axis.XP.rotationDegrees(sword.support()?90:90+net.minecraft.util.Mth.lerp(partial,sword.xRotO,sword.getXRot())));
            pose.mulPose(Axis.ZP.rotationDegrees(-45.0F));
        }
        float scale=(sword.flightRoot()?2.35F:(sword.support()?2.2F:1.1F))*sword.visualScale();pose.scale(scale,scale,scale);
        if(custom)items.render(sword.display(),ItemDisplayContext.NONE,false,pose,buffers,light,OverlayTexture.NO_OVERLAY,
                net.minecraft.client.Minecraft.getInstance().getModelManager().getModel(FlyingSwordItemModels.worldModel(definition)));
        else items.renderStatic(sword.display(),ItemDisplayContext.FIXED,light,OverlayTexture.NO_OVERLAY,pose,buffers,sword.level(),sword.getId());
        pose.popPose();super.render(sword,yaw,partial,pose,buffers,light);
    }
    @Override public ResourceLocation getTextureLocation(ControlledSwordEntity entity){return net.minecraft.client.renderer.texture.TextureAtlas.LOCATION_BLOCKS;}
}
