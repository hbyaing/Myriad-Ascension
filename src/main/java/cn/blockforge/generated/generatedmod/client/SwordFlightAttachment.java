package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.yujian.*;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import java.util.*;

/** Model-space rear attachment, using the same center/display rotations as the entity renderer. */
public final class SwordFlightAttachment {
    private record Key(BakedModel model,float yaw,float pitch,float roll,boolean custom) {}
    private static final Map<Key,Vec3> TAILS=new HashMap<>();
    public static void rotateModel(PoseStack pose,float yaw,float pitch,float roll){
        pose.mulPose(Axis.YP.rotationDegrees(yaw));pose.mulPose(Axis.XP.rotationDegrees(pitch));pose.mulPose(Axis.ZP.rotationDegrees(roll));
    }
    public static Vec3 tail(ControlledSwordEntity sword,Vec3 position,float yaw,float pitch,float roll){
        var mc=Minecraft.getInstance();var definition=FlyingSwordRegistry.visual(sword.display());
        var model=definition.baked3d()?mc.getModelManager().getModel(FlyingSwordItemModels.worldModel(definition))
                :mc.getItemRenderer().getModel(sword.display(),sword.level(),null,sword.getId());
        var key=new Key(model,definition.modelYaw(),definition.modelPitch(),definition.modelRoll(),definition.baked3d());
        if(TAILS.size()>32)TAILS.clear();
        Vec3 local=TAILS.computeIfAbsent(key,SwordFlightAttachment::measure);
        float scale=2.35F*sword.visualScale();
        return position.add(new Vec3(FlightFrame.rotation(yaw,pitch,roll).transform(
                new Vector3f((float)local.x*scale,(float)local.y*scale,(float)local.z*scale))));
    }
    private static Vec3 measure(Key key){
        var pose=new PoseStack();rotateModel(pose,key.yaw,key.pitch,key.roll);
        var model=key.model.applyTransform(key.custom?ItemDisplayContext.NONE:ItemDisplayContext.FIXED,pose,false);
        pose.translate(-.5,-.5,-.5);
        var points=new ArrayList<Vector3f>();var random=net.minecraft.util.RandomSource.create(42);
        for(int face=-1;face<6;face++){
            random.setSeed(42);
            for(var quad:model.getQuads(null,face<0?null:net.minecraft.core.Direction.values()[face],random)){
                int[] data=quad.getVertices();int stride=data.length/4;
                for(int i=0;i<4;i++)points.add(pose.last().pose().transformPosition(new Vector3f(
                        Float.intBitsToFloat(data[i*stride]),Float.intBitsToFloat(data[i*stride+1]),Float.intBitsToFloat(data[i*stride+2]))));
            }
        }
        if(points.isEmpty())return new Vec3(0,0,-.85/2.35);
        double rear=points.stream().mapToDouble(p->p.z).min().orElse(-.5);
        double front=points.stream().mapToDouble(p->p.z).max().orElse(.5);
        double band=Math.max(.00001,(front-rear)*.0001);
        double minX=Double.POSITIVE_INFINITY,minY=minX,maxX=Double.NEGATIVE_INFINITY,maxY=maxX;
        for(var p:points)if(p.z<=rear+band){minX=Math.min(minX,p.x);maxX=Math.max(maxX,p.x);minY=Math.min(minY,p.y);maxY=Math.max(maxY,p.y);}
        // A quad repeats its corners. Averaging those vertices biases the rear
        // towards whichever side happens to contain more decorative faces.
        return new Vec3((minX+maxX)*.5,(minY+maxY)*.5,rear);
    }
    private SwordFlightAttachment(){}
}
