package cn.blockforge.generated.generatedmod.client.arts;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
final class FourArtsPose extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    record Sample(Vec3 hand,Vec3 chest,List<Vec3> surface) {
        static Vec3 inWorld(Vec3 point,Matrix4f inverse,Vec3 camera) {
            return camera.add(vec(inverse.transformPosition(new Vector3f((float)point.x,(float)point.y,(float)point.z))));
        }
    }

    private FourArtsPose(PlayerRenderer renderer) { super(renderer); }

    @SubscribeEvent
    public static void register(EntityRenderersEvent.AddLayers event) {
        for(String skin:event.getSkins()) {
            if(event.getSkin(skin) instanceof PlayerRenderer renderer) renderer.addLayer(new FourArtsPose(renderer));
        }
    }

    @Override
    public void render(PoseStack pose,MultiBufferSource buffers,int light,AbstractClientPlayer player,
                       float limbSwing,float limbAmount,float partial,float age,float yaw,float pitch) {
        if(!FourArtsEffects.needsPose(player)) return;
        var model=getParentModel();
        ModelPart arm=player.getMainArm()==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
        double reach=player.getMainHandItem().isEmpty()?.16:.39;
        pose.pushPose();
        arm.translateAndRotate(pose);
        Vec3 hand=vec(pose.last().pose().transformPosition(new Vector3f(0,.53f,(float)-reach)));
        pose.popPose();
        Vec3 chest=null;
        List<Vec3> surface=List.of();
        if(FourArtsEffects.needsSurface(player) && !player.isInvisible()) {
            pose.pushPose();
            model.body.translateAndRotate(pose);
            chest=vec(pose.last().pose().transformPosition(new Vector3f(.08f,.33f,-.31f)));
            pose.popPose();
            var positions=new SurfaceVertices();
            for(ModelPart part:List.of(model.head,model.body,model.leftArm,model.rightArm,model.leftLeg,model.rightLeg))
                part.render(pose,positions,light,0);
            surface=positions.points;
        }
        FourArtsEffects.pose(player,new Sample(hand,chest,surface));
    }

    static Vec3 fallbackHand(AbstractClientPlayer player,Vec3 base,float partial) {
        double h=player.getBbHeight()/1.8;
        float angle=net.minecraft.util.Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot);
        Vec3 front=Vec3.directionFromRotation(0,angle), right=new Vec3(front.z,0,-front.x);
        double sign=player.getMainArm()==HumanoidArm.RIGHT?1:-1;
        return base.add(right.scale(sign*.36)).add(0,.93*h,0).add(front.scale(.36));
    }

    private static final class SurfaceVertices implements VertexConsumer {
        private final List<Vec3> points=new ArrayList<>(144);
        public VertexConsumer vertex(double x,double y,double z) { points.add(new Vec3(x,y,z)); return this; }
        public VertexConsumer color(int r,int g,int b,int a) { return this; }
        public VertexConsumer uv(float u,float v) { return this; }
        public VertexConsumer overlayCoords(int u,int v) { return this; }
        public VertexConsumer uv2(int u,int v) { return this; }
        public VertexConsumer normal(float x,float y,float z) { return this; }
        public void endVertex() {}
        public void defaultColor(int r,int g,int b,int a) {}
        public void unsetDefaultColor() {}
    }

    static Vec3 vec(Vector3f v) { return new Vec3(v.x,v.y,v.z); }
}
