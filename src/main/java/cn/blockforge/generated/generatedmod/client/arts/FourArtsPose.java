package cn.blockforge.generated.generatedmod.client.arts;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import com.mojang.blaze3d.vertex.PoseStack;
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

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
final class FourArtsPose extends RenderLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
    record Site(Vec3 point,Vec3 across,Vec3 along,Vec3 normal) {
        Site inWorld(Matrix4f inverse,Vec3 camera) {
            return new Site(camera.add(vec(inverse.transformPosition(vector(point)))),
                    vec(inverse.transformDirection(vector(across))).normalize(),
                    vec(inverse.transformDirection(vector(along))).normalize(),
                    vec(inverse.transformDirection(vector(normal))).normalize());
        }
    }

    record Sample(Site[] sites,Vec3 hand) {
        Sample inWorld(Matrix4f inverse,Vec3 camera) {
            var world=new Site[sites.length];
            for(int i=0;i<sites.length;i++) world[i]=sites[i].inWorld(inverse,camera);
            return new Sample(world,camera.add(vec(inverse.transformPosition(vector(hand)))));
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
        Site[] sites={
                part(pose,model.leftLeg,0,.28,-.129,false,false),
                part(pose,model.rightArm,0,.32,-.127,false,false),
                part(pose,model.head,0,-.535,0,true,false),
                part(pose,model.body,.09,.38,-.139,false,false),
                part(pose,model.rightLeg,0,.29,-.129,false,false),
                part(pose,model.leftArm,0,.32,-.127,false,false),
                part(pose,model.body,-.08,.32,.139,false,true),
                part(pose,model.rightLeg,0,.69,-.129,false,false)
        };
        ModelPart arm=player.getMainArm()==HumanoidArm.RIGHT?model.rightArm:model.leftArm;
        double reach=player.getMainHandItem().isEmpty()?.16:.39;
        Vec3 hand=part(pose,arm,0,.53,-reach,false,false).point;
        FourArtsEffects.pose(player,new Sample(sites,hand));
    }

    private static Site part(PoseStack pose,ModelPart part,double x,double y,double z,boolean top,boolean back) {
        pose.pushPose();
        part.translateAndRotate(pose);
        Matrix4f matrix=pose.last().pose();
        var point=vec(matrix.transformPosition(new Vector3f((float)x,(float)y,(float)z)));
        var across=vec(matrix.transformDirection(new Vector3f(1,0,0))).normalize();
        var along=vec(matrix.transformDirection(top?new Vector3f(0,0,1):new Vector3f(0,-1,0))).normalize();
        var normal=vec(matrix.transformDirection(top?new Vector3f(0,-1,0):new Vector3f(0,0,back?1:-1))).normalize();
        pose.popPose();
        return new Site(point,across,along,normal);
    }

    static Sample fallback(AbstractClientPlayer player,Vec3 base,float partial) {
        double h=player.getBbHeight()/1.8;
        float angle=net.minecraft.util.Mth.rotLerp(partial,player.yBodyRotO,player.yBodyRot);
        Vec3 front=Vec3.directionFromRotation(0,angle), right=new Vec3(front.z,0,-front.x);
        Vec3 up=new Vec3(0,1,0);
        double[][] points={{-.16,.43,.14},{.32,1.12,.15},{0,1.81,0},{.10,1.24,.15},
                {.16,.43,.14},{-.32,1.12,.15},{-.09,1.28,-.15},{.16,.10,.16}};
        Site[] sites=new Site[points.length];
        for(int i=0;i<points.length;i++) {
            var p=points[i];
            Vec3 location=base.add(right.scale(p[0])).add(0,p[1]*h,0).add(front.scale(p[2]));
            sites[i]=new Site(location,right,i==2?front:up,i==2?up:i==6?front.scale(-1):front);
        }
        double sign=player.getMainArm()==HumanoidArm.RIGHT?1:-1;
        return new Sample(sites,base.add(right.scale(sign*.36)).add(0,.93*h,0).add(front.scale(.36)));
    }

    private static Vector3f vector(Vec3 v) { return new Vector3f((float)v.x,(float)v.y,(float)v.z); }
    static Vec3 vec(Vector3f v) { return new Vec3(v.x,v.y,v.z); }
}
