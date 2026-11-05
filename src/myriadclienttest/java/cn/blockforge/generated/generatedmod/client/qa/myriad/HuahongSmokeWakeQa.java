package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.client.SwordFlightTrailHistory;
import cn.blockforge.generated.generatedmod.yujian.HuahongSkin;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Verifies submitted smoke on a long history that used to produce the extra rear plume. */
final class HuahongSmokeWakeQa {
    static Map<String,Object> verify() throws Exception {
        var draw=Class.forName("cn.blockforge.generated.generatedmod.client.HuahongSkinGeometry")
                .getDeclaredMethod("draw",HuahongSkin.class,VertexConsumer.class,PoseStack.Pose.class,
                        Vec3.class,Vec3.class,Vec3.class,Vec3.class,List.class,List.class,double.class,float.class);
        draw.setAccessible(true);
        var path=new ArrayList<SwordFlightTrailHistory.Node>();
        for(int i=400;i>=0;i--){
            double distance=i*.25;
            path.add(new SwordFlightTrailHistory.Node(new SwordFlightTrailHistory.Point(0,0,-distance),100-distance/15,100-distance));
        }
        var a=new HuahongFanVolumeQa.Collector();
        var b=new HuahongFanVolumeQa.Collector();
        var pose=new PoseStack().last();
        var eye=new Vec3(30,0,0);
        var forward=new Vec3(0,0,1);
        var side=new Vec3(1,0,0);
        draw.invoke(null,HuahongSkin.SMOKE,a,pose,eye,Vec3.ZERO,forward,side,path,path,100d,1F);
        draw.invoke(null,HuahongSkin.SMOKE,b,pose,eye,Vec3.ZERO,forward,side,path,path,106d,1F);
        double reach=Math.max(a.smokeReach,b.smokeReach);
        if(a.smokeVertices==0||b.smokeVertices==0||reach<4||reach>21||a.appearance==b.appearance)
            throw new AssertionError("Expected live near smoke only, submitted reach "+reach);
        return Map.of("passed",true,"historyLength",100,"visibleMeshReach",reach,
                "nearSmokeMoves",true,"visibleVertices",a.smokeVertices);
    }
}
