package cn.blockforge.generated.generatedmod.client.tribulation;
import cn.blockforge.generated.generatedmod.tribulation.TribulationVisualPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.level.LevelEvent;
import java.util.*;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.vertex.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
@Mod.EventBusSubscriber(modid="wuxingxiantu",value=Dist.CLIENT)
public final class TribulationEffects {
 private record Fx(TribulationVisualPacket p,LightningRecipe r,List<BoltGeometry> trees,ImpactGeometry impact,List<RainGeometry.Bolt> rain){}
 private static final List<Fx> ACTIVE=new ArrayList<>();
 private static final LinkedHashSet<String> SEEN=new LinkedHashSet<>();
 /** Reuse the actual sky-bolt timeline; no separate cloud timer or new lightning geometry. */
 public record CloudFlash(Vec3 position,float brightness){}
 public static CloudFlash cloudFlash(UUID owner,double now){
  Vec3 position=Vec3.ZERO;double power=0;
  for(var f:ACTIVE){
   if(!f.p.owner().equals(owner)||(f.p.kind()!=0&&f.p.kind()!=1))continue;
   double age=now-f.p.tick();if(age<0||age>=f.r.durationTicks)continue;
   double time=f.p.kind()==1?skyBoltTime(f.p,age):age;
   double brightness=new LightningEnvelope(f.r).brightness(time);
   if(brightness>power){power=brightness;position=f.p.kind()==1?f.p.start().lerp(f.p.end(),.5):f.p.start();}
  }
  return new CloudFlash(position,(float)power);
 }
 private static double skyBoltTime(TribulationVisualPacket packet,double age){
  return age%(30.0/(2+Math.floorMod(packet.seed(),2)));
 }
 public static void accept(TribulationVisualPacket p){
  var mc=Minecraft.getInstance();if(mc.level==null||!mc.level.dimension().location().toString().equals(p.dimension()))return;
  cn.blockforge.generated.generatedmod.client.cloud.TribulationClouds.accept(p);
  if(p.kind()==6)return;
  if(p.kind()==3){ACTIVE.removeIf(f->f.p.owner().equals(p.owner()));return;}
  String id=p.owner()+":"+p.eventId();if(!SEEN.add(id))return;while(SEEN.size()>512)SEEN.remove(SEEN.iterator().next());
  String recipeName=switch(p.kind()){
   case 2 -> "domain_strong";
   case 4 -> "domain";
   case 5 -> "thunder_art";
   default -> "0"+(p.realm()+1);
  };
  try(var stream=mc.getResourceManager().open(new ResourceLocation("wuxingxiantu","tribulation/"+recipeName+".json"))){
   var r=LightningRecipe.parse(JsonParser.parseReader(new java.io.InputStreamReader(stream,java.nio.charset.StandardCharsets.UTF_8)));r.seed=p.seed();
   List<BoltGeometry> trees=new ArrayList<>();
   if(p.kind()==0||p.kind()==1)for(int i=0;i<Math.min(11,p.count());i++){
    var child=LightningRecipe.parse(JsonParser.parseString(new com.google.gson.Gson().toJson(r)));child.seed=(int)(p.seed()+i*104729);child.maxSegments=i==0?Math.min(10000,r.maxSegments):2000;
    Vec3 start=p.start();if(p.kind()==0&&p.count()>1){double a=i*Math.PI*2/p.count()+p.seed()*.00001;start=start.add(Math.cos(a)*28,0,Math.sin(a)*28);}
    if(p.kind()==1){child.durationTicks=30;child.progressionTicks=.5;child.restrikeCount=0;child.flicker=0;child.maxBranchDepth=1;child.branchLimit=12;child.maxSegments=512;}
    var tree=BoltGeometry.generate(start,p.end(),child);
    if(p.kind()==0)tree.segments().replaceAll(s->s.depth()>0?new BoltGeometry.Segment(s.a(),s.b(),s.widthA()/.55f,s.widthB()/.55f,s.intensity(),s.alongA(),s.alongB(),s.depth(),s.mask()):s);
    trees.add(tree);
   }
   if(p.kind()==1){r.durationTicks=30;r.progressionTicks=.5;r.restrikeCount=0;r.flicker=0;}
   List<RainGeometry.Bolt> rain=List.of();
   if(p.kind()==2||p.kind()==4||p.kind()==5){
    r.impactEnabled = p.grounded() && (p.kind()==2 || r.impactEnabled);
    if(p.kind()==2){
     // The tribulation finish still uses its existing realm-scaled rain.
     r.strikeHeight=Math.max(24,p.start().y-p.end().y);
     r.strikeDisplacement=3.5;
     r.strikeRoughness=.625;
     r.strikeWidth=.004+p.realm()*.00015;
     r.coreGain=3.8;
     r.glowGain=2.4;
     r.glowWidth=.155;
     r.durationTicks=30;
     r.progressionTicks=.5;
     r.rainStagger=0;
     r.restrikeCount=6;
     r.flicker=0;
     r.impactDuration=Math.min(29,r.impactDuration);
     r.rainSpread=3+p.realm()*1.5;
     r.rainDepthSpread=r.rainSpread*.8;
     r.rainCount=9+p.realm()*2;
    }
    rain=RainGeometry.generate(r,p.end(),(start,end)->{
     if(!p.grounded())return end;
     var hit=mc.level.clip(new net.minecraft.world.level.ClipContext(start,end.add(0,-10,0),
      net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,mc.player));
     return hit.getType()==net.minecraft.world.phys.HitResult.Type.BLOCK?hit.getLocation():end;
    });
   }
   ACTIVE.add(new Fx(p,r,trees,p.kind()==0&&p.grounded()?new ImpactGeometry(p.end(),r):null,rain));while(ACTIVE.size()>32)ACTIVE.remove(0);
  }catch(Exception ex){org.slf4j.LoggerFactory.getLogger("TribulationEffects").error("Cannot load tribulation recipe",ex);}
 }
 @SubscribeEvent public static void unload(LevelEvent.Unload e){if(e.getLevel().isClientSide()){ACTIVE.clear();SEEN.clear();LightningHalo.release();}}
 @SubscribeEvent public static void render(RenderLevelStageEvent e){
  if(e.getStage()!=RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS)return;var mc=Minecraft.getInstance();if(mc.level==null)return;
  if(cn.blockforge.generated.generatedmod.client.TrailShaderBridge.shadowPass())return;
  double now=mc.level.getGameTime()+e.getPartialTick();ACTIVE.removeIf(f->f.rain.isEmpty()?now-f.p.tick()>Math.max(f.r.durationTicks,f.r.progressionTicks+f.r.impactDuration):RainGeometry.finished(f.rain,now-f.p.tick()));
  if(ACTIVE.isEmpty())return;var pose=new PoseStack();pose.last().pose().set(e.getPoseStack().last().pose());Vec3 camera=e.getCamera().getPosition();
  Runnable draw=()->{for(var f:ACTIVE){double t=now-f.p.tick();if(t<0)continue;for(var tree:f.trees){double boltTime=f.p.kind()==1?skyBoltTime(f.p,t):t;LightningRender.drawBolt(tree,f.r,boltTime,pose,camera);}if(f.impact!=null)LightningRender.drawOrbit(f.impact.sample(t),f.r,pose,camera);for(var bolt:f.rain){double age=t-bolt.delay();if(age<0)continue;LightningRender.drawBolt(bolt.geometry(),bolt.recipe(),age,pose,camera);if(bolt.recipe().impactEnabled)LightningRender.drawOrbit(bolt.impact().sample(age),bolt.recipe(),pose,camera);}}};
  if(!LightningShaderBridge.prepare(draw))draw.run();
 }
}

