package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.MeditationSeatEntity;
import net.minecraft.client.model.*;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.layers.CapeLayer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import java.util.*;

/** Separate six-pixel upper/lower limbs are used only for meditation.
 * Normal animation and skin geometry remain vanilla; armor bends with the same bones. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT,bus=Mod.EventBusSubscriber.Bus.MOD)
public final class MeditationModels {
    private static final String[] LIMBS={"right_arm","left_arm","right_leg","left_leg"};

    @SubscribeEvent
    @SuppressWarnings({"rawtypes","unchecked"})
    public static void install(EntityRenderersEvent.AddLayers event) {
        try {
            var modelField=ObfuscationReflectionHelper.findField(LivingEntityRenderer.class,"f_115290_");
            var layersField=ObfuscationReflectionHelper.findField(LivingEntityRenderer.class,"f_115291_");
            for(String skin:event.getSkins()) {
                if(!((Object)event.getSkin(skin) instanceof net.minecraft.client.renderer.entity.player.PlayerRenderer renderer))continue;
                modelField.set(renderer,new SeatedPlayer("slim".equals(skin)));
                List layers=(List)layersField.get(renderer);
                layers.removeIf(layer->layer instanceof HumanoidArmorLayer);
                layers.removeIf(layer->layer instanceof net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer);
                // The vanilla cape uses the player's world cloak history. Flight
                // renders the rider through a sword-relative root transform, so
                // install a layer that uses that same interpolated flight frame.
                layers.removeIf(layer->layer instanceof CapeLayer);
                renderer.addLayer(new FlightCapeLayer((net.minecraft.client.renderer.entity.RenderLayerParent)renderer));
                ((LivingEntityRenderer)renderer).addLayer(new FlightHands((net.minecraft.client.renderer.entity.RenderLayerParent)renderer,event.getContext().getItemInHandRenderer()));
                // Keep the same split armor bones when installing visibility and enchantment visuals.
                renderer.addLayer(new RobeArmorLayer(renderer,new SeatedArmor(0.5F),
                        new SeatedArmor(1),event.getContext().getModelManager()));
            }
        } catch(IllegalAccessException ex) { throw new IllegalStateException("Cannot install meditation pose",ex); }
    }

    private static ModelPart mesh(boolean slim,boolean armor,float dilation) {
        var mesh=armor?HumanoidModel.createMesh(new CubeDeformation(dilation),0)
                :PlayerModel.createMesh(CubeDeformation.NONE,slim);
        var root=mesh.getRoot();
        for(int i=0;i<4;i++) {
            boolean arm=i<2,left=i%2==1;
            int u=arm?(left&&!armor?32:40):(left&&!armor?16:0);
            int v=left&&!armor?48:16;
            for(int half=0;half<2;half++) {
                float width=arm&&slim&&!armor?3:4;
                var box=CubeListBuilder.create().texOffs(u,v+half*6).mirror(left&&armor)
                        .addBox(-width/2,0,-2,width,6,4,new CubeDeformation(dilation));
                root.addOrReplaceChild(name(i,half,false),box,PartPose.ZERO);
                if(!armor) {
                    int ou=arm?(left?48:40):(left?0:0);
                    int ov=left?48:32;
                    root.addOrReplaceChild(name(i,half,true),CubeListBuilder.create().texOffs(ou,ov+half*6)
                            .addBox(-width/2,0,-2,width,6,4,new CubeDeformation(0.25F)),PartPose.ZERO);
                }
            }
        }
        return LayerDefinition.create(mesh,64,armor?32:64).bakeRoot();
    }
    private static String name(int limb,int half,boolean overlay) {
        return "seated_"+LIMBS[limb]+half+(overlay?"_outer":"");
    }

    // Directions are expressed in player model pixels: negative Z is forward.
    // Independent bones avoid the compound rotations which twist skin and armor at knees.
    private static void place(ModelPart part,float x,float y,float z,float dx,float dy,float dz) {
        part.setPos(x,y,z);
        float length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        part.xRot=(float)Math.asin(dz/length);
        part.yRot=0;
        part.zRot=(float)Math.atan2(-dx,dy);
    }
    private static final class FoldedLimbs {
        final ModelPart[][] bones=new ModelPart[4][2],outer=new ModelPart[4][2];
        final boolean armor;
        FoldedLimbs(ModelPart root,boolean armor) {
            this.armor=armor;
            for(int i=0;i<4;i++)for(int j=0;j<2;j++) {
                bones[i][j]=root.getChild(name(i,j,false));
                if(!armor)outer[i][j]=root.getChild(name(i,j,true));
            }
            meditation();
        }
        void meditation() {
            for(int i=0;i<4;i++) {
                float sign=i%2==0?-1:1;
                if(i<2) {
                    place(bones[i][0],sign*5,2,0,0,5.94F,-0.85F);
                    place(bones[i][1],sign*5,7.94F,-0.85F,-sign*3.8F,1.5F,-4.39F);
                } else {
                    place(bones[i][0],sign*1.9F,12,0,sign*4,1,-4.36F);
                    place(bones[i][1],sign*5.9F,13,-4.36F,-sign*5.75F,0.5F,i==2?-1.64F:1.64F);
                }
                if(!armor)for(int j=0;j<2;j++)outer[i][j].copyFrom(bones[i][j]);
            }
        }
        void flight(float sprint,float brace) {
            for(int i=0;i<4;i++) {
                float sign=i%2==0?-1:1;
                if(i<2) {
                    float dz=2.8F*sprint,dy=(float)Math.sqrt(36-dz*dz);
                    place(bones[i][0],sign*5,2+2*sprint,0,sign*.45F,dy,dz);
                    place(bones[i][1],sign*5+sign*.45F,2+2*sprint+dy,dz,sign*.3F,dy,dz);
                } else {
                    float hipY=12.5F+2*sprint+1.5F*brace,hipZ=-sprint;
                    float footZ=i==2?-3-3*sprint:3+sprint;
                    float dy=24-hipY,dz=footZ-hipZ;
                    float length=(float)Math.sqrt(dy*dy+dz*dz);
                    float bend=(float)Math.sqrt(Math.max(0,36-length*length/4));
                    float kneeY=(hipY+24)/2+dz/length*bend;
                    float kneeZ=(hipZ+footZ)/2-dy/length*bend;
                    place(bones[i][0],sign*1.4F,hipY,hipZ,0,kneeY-hipY,kneeZ-hipZ);
                    place(bones[i][1],sign*1.4F,kneeY,kneeZ,0,24-kneeY,footZ-kneeZ);
                }
                if(!armor)for(int j=0;j<2;j++)outer[i][j].copyFrom(bones[i][j]);
            }
        }
        void forge(ModelPart right,ModelPart left,boolean slim,float along,boolean leftHanded) {
            ModelPart[] arms={right,left};
            for(int i=0;i<2;i++) {
                float center=(slim?.5F:1F)*(i==0?-1:1);
                var shoulder=new HeartForgeLayout.Point((arms[i].x+center)/16,arms[i].y/16,arms[i].z/16);
                boolean holding=i==(leftHanded?1:0);
                var wrist=HeartForgeLayout.thirdPerson(holding?0:along,holding?0:HeartForgeLayout.PALM_CLEARANCE).handed(leftHanded);
                var elbow=HeartForgeLayout.elbow(shoulder.handed(leftHanded),wrist.handed(leftHanded),holding).handed(leftHanded);
                var upper=elbow.subtract(shoulder);var lower=wrist.subtract(elbow);
                place(bones[i][0],shoulder.x()*16,shoulder.y()*16,shoulder.z()*16,upper.x()*16,upper.y()*16,upper.z()*16);
                place(bones[i][1],elbow.x()*16,elbow.y()*16,elbow.z()*16,lower.x()*16,lower.y()*16,lower.z()*16);
                if(!armor)for(int j=0;j<2;j++)outer[i][j].copyFrom(bones[i][j]);
            }
        }
        Iterable<ModelPart> parts(HumanoidModel<?> model,PlayerModel<?> skin,boolean flight,boolean forge) {
            // PlayerModel.bodyParts() normally supplies the head and its hat layer.
            // The folded pose replaces that iterator, so keep both parts here as
            // well; otherwise meditation/flight renders the face without the
            // player's outer head skin (and armor loses its head part too).
            var result=new ArrayList<ModelPart>();result.add(model.body);
            if(skin!=null)result.add(skin.jacket);
            ModelPart[] vanilla={model.rightArm,model.leftArm,model.rightLeg,model.leftLeg};
            ModelPart[] overlays=skin==null?null:new ModelPart[]{skin.rightSleeve,skin.leftSleeve,skin.rightPants,skin.leftPants};
            for(int i=0;i<4;i++) {
                boolean split=i<2?forge||!flight:!forge||flight;
                if(!split) {
                    result.add(vanilla[i]);if(skin!=null)result.add(overlays[i]);continue;
                }
                for(int j=0;j<2;j++) {
                    bones[i][j].visible=vanilla[i].visible;result.add(bones[i][j]);
                    if(!armor) {outer[i][j].visible=overlays[i].visible;result.add(outer[i][j]);}
                }
            }
            // Keep the vanilla body-part order: the custom folded limbs above
            // still render before the head and its outer layer.
            result.add(model.head);
            if(skin!=null)result.add(skin.hat);
            return result;
        }
    }

    public static final class SeatedPlayer extends PlayerModel<AbstractClientPlayer> {
        private final FoldedLimbs folded;
        private final boolean slim;
        private boolean forging;
        public boolean meditating,flying;
        private float flightSprint,flightBrace;
        private java.util.UUID cloudOwner;
        private boolean cloudIdleArms;
        public SeatedPlayer(boolean slim) {this(mesh(slim,false,0),slim);}
        private SeatedPlayer(ModelPart root,boolean slim) {super(root,slim);this.slim=slim;folded=new FoldedLimbs(root,false);}
        @Override public void setupAnim(AbstractClientPlayer player,float walk,float speed,float age,float yaw,float pitch) {
            JindouCloudPlayerPose.restore(this);
            cloudOwner=null;
            cloudIdleArms=false;
            forging=false;
            body.setPos(0,0,0);head.setPos(0,0,0);
            boolean cloudActive=JindouCloudRenderState.isActive(player);
            // Suppress locomotion and idle bob at their source; vanilla attack/use poses still run.
            boolean wasCrouching=crouching,wasRiding=riding;
            float wasSwimAmount=swimAmount;
            if(cloudActive){crouching=false;riding=false;swimAmount=0;}
            super.setupAnim(player,cloudActive?0:walk,cloudActive?0:speed,cloudActive?0:age,yaw,pitch);
            crouching=wasCrouching;riding=wasRiding;swimAmount=wasSwimAmount;
            if(cn.blockforge.generated.generatedmod.client.tribulation.ParalysisPose.apply(this,player,age)) {
                meditating=false;flying=false;forging=false;return;
            }
            HeartSwordPose.arms(this, player, age-player.tickCount);
            rightSleeve.copyFrom(rightArm); leftSleeve.copyFrom(leftArm);
            flying=false;
            if(cloudActive) {
                meditating=false;
                cloudOwner=player.getUUID();
                JindouCloudPlayerPose.apply(this,cloudOwner);
                cloudIdleArms=!player.isUsingItem()&&!player.swinging&&attackTime<=0;
                if(cloudIdleArms)JindouCloudPlayerPose.stabilizeIdleArms(this);
                forge(player,age-player.tickCount);
                return;
            }
            if(player.getVehicle() instanceof cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity sword && sword.flightRoot()) {
                flying=true;
                var frame=InterpolatedFlyingSwordTransform.get(sword,age-player.tickCount);
                flightSprint=frame.sprintFactor();flightBrace=frame.brace();
                YujianRidingPose.animate(this,sword,age-player.tickCount);
                folded.flight(flightSprint,flightBrace);
                float crouch=2*flightSprint+1.5F*flightBrace;
                body.y=crouch;jacket.copyFrom(body);head.y=crouch;hat.copyFrom(head);
                rightArm.y+=crouch;leftArm.y+=crouch;
                rightSleeve.copyFrom(rightArm);leftSleeve.copyFrom(leftArm);
            }
            meditating=player.getVehicle() instanceof MeditationSeatEntity;
            if(meditating) {
                folded.meditation();
                body.setPos(0,0,0);body.xRot=body.yRot=body.zRot=0;
                jacket.copyFrom(body);
                head.y=0;hat.copyFrom(head);
                rightArm.copyFrom(folded.bones[0][0]);leftArm.copyFrom(folded.bones[1][0]);
                rightLeg.copyFrom(folded.bones[2][0]);leftLeg.copyFrom(folded.bones[3][0]);
            }
            if(!meditating)forge(player,age-player.tickCount);
            if(!meditating&&!flying&&!forging)
                cn.blockforge.generated.generatedmod.client.realmflight.RealmFlightVisuals.pose(this,player,age-player.tickCount);
            if(HeavenlyFurnacePose.apply(this,player,age-player.tickCount))meditating=flying=forging=false;
        }
        private void forge(AbstractClientPlayer player,float partial) {
            int action=HeartSwordClientEffects.action(player);
            if(action<2)return;
            forging=true;
            folded.forge(rightArm,leftArm,slim,HeartSwordPose.forgeAlong(HeartSwordClientEffects.sword(player),action,
                    HeartSwordClientEffects.progress(player,partial),true),HeartSwordClientEffects.leftHanded(player));
        }
        @Override protected Iterable<ModelPart> bodyParts() {
            if(cloudOwner!=null&&cloudIdleArms)JindouCloudPlayerPose.stabilizeIdleArms(this);
            return meditating||flying||forging?folded.parts(this,this,flying,forging):super.bodyParts();
        }
        @Override public void copyPropertiesTo(HumanoidModel<AbstractClientPlayer> model) {
            JindouCloudPlayerPose.restore(model);
            if(cloudOwner!=null)JindouCloudPlayerPose.remember(model,cloudOwner);
            super.copyPropertiesTo(model);
            if(model instanceof SeatedArmor armor){
                armor.meditating=meditating;armor.flying=flying;armor.forging=forging;
                if(flying)armor.folded.flight(flightSprint,flightBrace);else if(meditating)armor.folded.meditation();
                if(forging)for(int i=0;i<2;i++)for(int j=0;j<2;j++)armor.folded.bones[i][j].copyFrom(folded.bones[i][j]);
            }
        }
        @Override public void translateToHand(net.minecraft.world.entity.HumanoidArm arm,
                com.mojang.blaze3d.vertex.PoseStack pose) {
            if(!meditating) {super.translateToHand(arm,pose);return;}
            folded.bones[arm==net.minecraft.world.entity.HumanoidArm.RIGHT?0:1][1].translateAndRotate(pose);
            pose.translate(0,-4.0/16,0);
        }
    }
    private static final class SeatedArmor extends HumanoidModel<AbstractClientPlayer> {
        final FoldedLimbs folded;boolean meditating,flying,forging;
        SeatedArmor(float dilation) {this(mesh(false,true,dilation));}
        SeatedArmor(ModelPart root) {super(root);folded=new FoldedLimbs(root,true);}
        @Override protected Iterable<ModelPart> bodyParts() {return meditating||flying||forging?folded.parts(this,null,flying,forging):super.bodyParts();}
    }

    private static final class FlightHands extends net.minecraft.client.renderer.entity.layers.PlayerItemInHandLayer<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> {
        FlightHands(net.minecraft.client.renderer.entity.RenderLayerParent<AbstractClientPlayer,PlayerModel<AbstractClientPlayer>> parent,
                    net.minecraft.client.renderer.ItemInHandRenderer items){super(parent,items);}
        @Override public void render(com.mojang.blaze3d.vertex.PoseStack pose,
                net.minecraft.client.renderer.MultiBufferSource buffers, int light, AbstractClientPlayer player,
                float walk, float speed, float partial, float age, float yaw, float pitch) {
            int action = HeartSwordClientEffects.action(player);
            if (player.hasEffect(cn.blockforge.generated.generatedmod.ModEffects.PARALYSIS.get()) || action < 0) { super.render(pose,buffers,light,player,walk,speed,partial,age,yaw,pitch); return; }
            if (action < 2) {
                var offArm=HeartSwordClientEffects.leftHanded(player)?net.minecraft.world.entity.HumanoidArm.RIGHT:net.minecraft.world.entity.HumanoidArm.LEFT;
                renderArmWithItem(player,player.getOffhandItem(),offArm==net.minecraft.world.entity.HumanoidArm.LEFT
                        ?net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_LEFT_HAND:net.minecraft.world.item.ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                        offArm,pose,buffers,light);
            }
            pose.pushPose();
            boolean left=HeartSwordClientEffects.leftHanded(player);
            float handSign=left?-1:1;
            var grip=(action>=2?HeartForgeLayout.thirdPerson(0,0):new HeartForgeLayout.Point(-.32F,.72F,-.45F)).handed(left);
            pose.translate(grip.x(),grip.y(),grip.z());
            if(action<2)pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(12*handSign));
            pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees((action>=2?-90:-160)*handSign));
            HeartSwordPose.renderSword(player,action,HeartSwordClientEffects.progress(player,partial),pose,buffers,light);
            pose.popPose();
        }
        @Override protected void renderArmWithItem(net.minecraft.world.entity.LivingEntity entity,net.minecraft.world.item.ItemStack stack,
                net.minecraft.world.item.ItemDisplayContext context,net.minecraft.world.entity.HumanoidArm arm,
                com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.MultiBufferSource buffers,int light){
            if(HeavenlyFurnaceVisuals.active(entity.getUUID())!=null)return;
            if(entity.getVehicle() instanceof cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity sword && sword.flightRoot()
                    &&YujianClientController.isBoundSword(stack,sword))return;
            super.renderArmWithItem(entity,stack,context,arm,pose,buffers,light);
        }
    }
}
