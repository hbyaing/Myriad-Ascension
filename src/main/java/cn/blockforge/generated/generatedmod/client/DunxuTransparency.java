package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.heartsword.HeartSwordMaterialRenderTypes;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.YujianPhase;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

/** Scoped buffer conversion includes skin, outer layers, armor, held items and the mounted sword. */
public final class DunxuTransparency {
    public static final float OPACITY=.4F;
    private static final Map<RenderType,RenderType> TYPES=new WeakHashMap<>();
    private static final Field TEXTURE=field(RenderType.CompositeState.class,RenderStateShard.EmptyTextureStateShard.class);
    private static final Method TEXTURE_LOCATION=textureMethod();
    private DunxuTransparency() {}

    public static MultiBufferSource wrap(Entity entity,MultiBufferSource source){
        ControlledSwordEntity sword=entity instanceof ControlledSwordEntity s?s:
                entity.getVehicle() instanceof ControlledSwordEntity s?s:null;
        if(sword==null||!sword.flightRoot())return source;
        boolean dunxu=sword.advancedPhase()==YujianPhase.DUNXU&&sword.dunxuStage()!=0;
        float rainbow=cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.TRAILS.get()?sword.renderHuahong(net.minecraft.client.Minecraft.getInstance().getFrameTime()):0;
        if(!dunxu&&rainbow<=0)return source;
        float base=dunxu?(sword.dunxuStage()==1?.72F:sword.dunxuStage()==3?.7F:OPACITY):1;
        float opacity=base*(1-rainbow);
        return type->new Alpha(source.getBuffer(translucent(type)),opacity);
    }

    private static RenderType translucent(RenderType type){
        // Keep the low-alpha emissive shader and the crystal's non-occluding material pass.
        if(type==HeartSwordMaterialRenderTypes.CRYSTAL||type==HeartSwordMaterialRenderTypes.EMISSIVE)return type;
        if(type.format()!=DefaultVertexFormat.NEW_ENTITY)return type;
        return TYPES.computeIfAbsent(type,key->{
            // Find by type/signature: field names differ between Mojmap and the shipped SRG runtime.
            Field state=field(key.getClass(),RenderType.CompositeState.class);
            if(state==null)return key;
            try{
                @SuppressWarnings("unchecked")
                Optional<ResourceLocation> texture=(Optional<ResourceLocation>)TEXTURE_LOCATION.invoke(TEXTURE.get(state.get(key)));
                return texture.map(RenderType::entityTranslucent).orElse(key);
            }catch(ReflectiveOperationException e){throw new IllegalStateException("Cannot read void-flight render texture",e);}
        });
    }
    private static Field field(Class<?> owner,Class<?> type){
        for(Class<?> c=owner;c!=null;c=c.getSuperclass())for(Field f:c.getDeclaredFields())
            if(f.getType()==type){f.setAccessible(true);return f;}
        return null;
    }
    private static Method textureMethod(){
        for(Method m:RenderStateShard.EmptyTextureStateShard.class.getDeclaredMethods())
            if(m.getParameterCount()==0&&m.getReturnType()==Optional.class){m.setAccessible(true);return m;}
        throw new IllegalStateException("Missing render texture accessor");
    }
    private record Alpha(VertexConsumer target,float opacity) implements VertexConsumer {
        public VertexConsumer vertex(double x,double y,double z){target.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){target.color(r,g,b,Math.round(a*opacity));return this;}
        public VertexConsumer uv(float u,float v){target.uv(u,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){target.overlayCoords(u,v);return this;}
        public VertexConsumer uv2(int u,int v){target.uv2(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){target.normal(x,y,z);return this;}
        public void endVertex(){target.endVertex();}
        public void defaultColor(int r,int g,int b,int a){target.defaultColor(r,g,b,Math.round(a*opacity));}
        public void unsetDefaultColor(){target.unsetDefaultColor();}
    }
}
