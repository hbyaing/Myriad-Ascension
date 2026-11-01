package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.client.heartsword.HeartSwordMaterialRenderTypes;
import cn.blockforge.generated.generatedmod.yujian.ControlledSwordEntity;
import cn.blockforge.generated.generatedmod.yujian.YujianPhase;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import cn.blockforge.generated.generatedmod.GeneratedMod;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;

/** Scoped buffer conversion includes skin, outer layers, armor, held items and the mounted sword. */
@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class DunxuTransparency {
    public static final float OPACITY=.4F;
    private static final Map<RenderType,RenderType> TYPES=new WeakHashMap<>();
    private static final Field TEXTURE=field(RenderType.CompositeState.class,RenderStateShard.EmptyTextureStateShard.class);
    private static final Method TEXTURE_LOCATION=textureMethod();
    private static boolean prepared;
    private DunxuTransparency() {}

    public static MultiBufferSource wrap(Entity entity,MultiBufferSource source){
        ControlledSwordEntity sword=entity instanceof ControlledSwordEntity s?s:
                entity.getVehicle() instanceof ControlledSwordEntity s?s:null;
        if(sword==null||!sword.flightRoot())return source;
        boolean dunxu=sword.advancedPhase()==YujianPhase.DUNXU&&sword.dunxuStage()!=0;
        float rainbow=cn.blockforge.generated.generatedmod.yujian.FlyingSwordConfig.TRAILS.get()?InterpolatedFlyingSwordTransform.get(sword,net.minecraft.client.Minecraft.getInstance().getFrameTime()).huahong():0;
        if(!dunxu&&rainbow<=0)return source;
        float base=dunxu?(sword.dunxuStage()==1?.72F:sword.dunxuStage()==3?.7F:OPACITY):1;
        float opacity=base*(1-rainbow);
        return type->new Alpha(source.getBuffer(translucent(type)),opacity,type.format()==DefaultVertexFormat.NEW_ENTITY);
    }

    @SubscribeEvent public static void unload(LevelEvent.Unload event){
        if(event.getLevel().isClientSide()){PackedBulk.releaseScratch();TYPES.clear();}
    }
    @SubscribeEvent public static void clientTick(TickEvent.ClientTickEvent event){
        if(event.phase==TickEvent.Phase.END&&!prepared){
            // Resolve optional APIs after all mods have loaded, before the first
            // flight frame needs a transparent sword or player buffer.
            PackedBulk.prepare();prepared=true;
        }
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
    private static final class Alpha implements VertexConsumer {
        private final VertexConsumer target;
        private final float opacity;
        private final PackedBulk.Writer writer;
        private final boolean entityFormat;
        private Alpha(VertexConsumer target,float opacity,boolean entityFormat){
            this.target=target;this.opacity=opacity;this.entityFormat=entityFormat;writer=entityFormat?PackedBulk.bind(target):null;
        }
        public VertexConsumer vertex(double x,double y,double z){target.vertex(x,y,z);return this;}
        public VertexConsumer color(int r,int g,int b,int a){target.color(r,g,b,Math.round(a*opacity));return this;}
        public VertexConsumer uv(float u,float v){target.uv(u,v);return this;}
        public VertexConsumer overlayCoords(int u,int v){target.overlayCoords(u,v);return this;}
        public VertexConsumer uv2(int u,int v){target.uv2(u,v);return this;}
        public VertexConsumer normal(float x,float y,float z){target.normal(x,y,z);return this;}
        public void endVertex(){target.endVertex();}
        public void defaultColor(int r,int g,int b,int a){target.defaultColor(r,g,b,Math.round(a*opacity));}
        public void unsetDefaultColor(){target.unsetDefaultColor();}

        @Override public void putBulkData(PoseStack.Pose pose,BakedQuad quad,float red,float green,float blue,int light,int overlay){
            if(entityFormat&&PackedBulk.write(writer,target,opacity,pose,quad,null,red,green,blue,1,null,light,overlay,false))return;
            VertexConsumer.super.putBulkData(pose,quad,red,green,blue,light,overlay);
        }
        @Override public void putBulkData(PoseStack.Pose pose,BakedQuad quad,float[] brightness,float red,float green,float blue,int[] lights,int overlay,boolean existingColor){
            putBulkData(pose,quad,brightness,red,green,blue,1,lights,overlay,existingColor);
        }
        @Override public void putBulkData(PoseStack.Pose pose,BakedQuad quad,float red,float green,float blue,float alpha,int light,int overlay,boolean existingColor){
            if(entityFormat&&PackedBulk.write(writer,target,opacity,pose,quad,null,red,green,blue,alpha,null,light,overlay,existingColor))return;
            VertexConsumer.super.putBulkData(pose,quad,red,green,blue,alpha,light,overlay,existingColor);
        }
        @Override public void putBulkData(PoseStack.Pose pose,BakedQuad quad,float[] brightness,float red,float green,float blue,float alpha,int[] lights,int overlay,boolean existingColor){
            if(entityFormat&&PackedBulk.write(writer,target,opacity,pose,quad,brightness,red,green,blue,alpha,lights,0,overlay,existingColor))return;
            VertexConsumer.super.putBulkData(pose,quad,brightness,red,green,blue,alpha,lights,overlay,existingColor);
        }
    }

    /** Optional public Sodium/Embeddium API. All foreign types are resolved
     * during client startup; absent/extended backends retain per-vertex hooks.
     * This restores packed quad submission, not ItemRenderer's whole-model
     * intrinsic (Alpha deliberately does not advertise an arbitrary-format writer).
     */
    private static final class PackedBulk {
        private static final Api API=api();
        private static final int MAX_CACHED_BYTES=256*1024;
        private static final ThreadLocal<Scratch> SCRATCH=ThreadLocal.withInitial(Scratch::new);
        private record Api(Class<?> writer,MethodHandle canUse,MethodHandle push,Object format,int stride,int position,int color,int texture,int overlay,int light,int normal,Class<?> irisBuilder,MethodHandle irisExtending){}
        private record Writer(Api api,Object target){}
        private static final class Scratch {
            final Vector4f position=new Vector4f();
            final Vector3f normal=new Vector3f();
            long address;int capacity,depth;
            void release(){if(address!=0){MemoryUtil.nmemFree(address);address=0;capacity=0;}}
        }
        private static Api api(){
            try{
                ClassLoader loader=DunxuTransparency.class.getClassLoader();
                Class<?> writer=Class.forName("net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter",false,loader);
                Class<?> description=Class.forName("net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription",false,loader);
                Class<?> attribute=Class.forName("net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute",false,loader);
                Object format=Class.forName("net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex",true,loader).getField("FORMAT").get(null);
                int stride=((Number)description.getMethod("stride").invoke(format)).intValue();
                if(stride<32||stride>4096)return null;
                Method contains=description.getMethod("containsElement",attribute),offset=description.getMethod("getElementOffset",attribute);
                String[] names={"POSITION","COLOR","TEXTURE","OVERLAY","LIGHT","NORMAL"};int[] widths={12,4,8,4,4,4},offsets=new int[6];
                for(int i=0;i<names.length;i++){
                    Object element=attribute.getField(names[i]).get(null);
                    if(!Boolean.TRUE.equals(contains.invoke(format,element)))return null;
                    offsets[i]=((Number)offset.invoke(format,element)).intValue();
                    if(offsets[i]<0||offsets[i]+widths[i]>stride)return null;
                    for(int j=0;j<i;j++)if(offsets[i]<offsets[j]+widths[j]&&offsets[j]<offsets[i]+widths[i])return null;
                }
                var lookup=MethodHandles.publicLookup();
                MethodHandle canUse=lookup.unreflect(writer.getMethod("canUseIntrinsics")).asType(MethodType.methodType(boolean.class,Object.class));
                MethodHandle push=lookup.unreflect(writer.getMethod("push",MemoryStack.class,long.class,int.class,description))
                        .asType(MethodType.methodType(void.class,Object.class,MemoryStack.class,long.class,int.class,Object.class));
                Class<?> irisBuilder=null;MethodHandle irisExtending=null;
                try{
                    irisBuilder=Class.forName("net.irisshaders.iris.vertices.IrisExtendedBufferBuilder",false,loader);
                    irisExtending=lookup.unreflect(irisBuilder.getMethod("iris$extending")).asType(MethodType.methodType(boolean.class,Object.class));
                }catch(ClassNotFoundException absent){/* Iris/Oculus is optional. */}
                return new Api(writer,canUse,push,format,stride,offsets[0],offsets[1],offsets[2],offsets[3],offsets[4],offsets[5],irisBuilder,irisExtending);
            }catch(ReflectiveOperationException|LinkageError|RuntimeException unavailable){return null;}
        }
        private static Writer bind(VertexConsumer target){
            Api api=API;if(api==null||!api.writer().isInstance(target))return null;
            try{
                // Iris generates quad normals/tangents and extended attributes
                // during per-vertex completion. A raw packed push skips that
                // work even when Sodium advertises an intrinsic-capable format.
                if(api.irisBuilder()!=null&&api.irisBuilder().isInstance(target)&&(boolean)api.irisExtending().invokeExact((Object)target))return null;
                return (boolean)api.canUse().invokeExact((Object)target)?new Writer(api,target):null;
            }
            catch(Throwable failure){throw propagate(failure);}
        }
        private static void prepare(){
            Scratch scratch=SCRATCH.get();
            if(API!=null){
                // Resolve the per-consumer carrier before rendering too. No
                // backend is advertised and no vertices are submitted here.
                new Writer(API,null).api();
                if(scratch.address==0){scratch.capacity=1024;scratch.address=MemoryUtil.nmemAllocChecked(scratch.capacity);}
            }
            new Alpha(null,1,false);
        }
        private static boolean write(Writer writer,VertexConsumer target,float opacity,PoseStack.Pose pose,BakedQuad quad,float[] brightness,
                float red,float green,float blue,float alpha,int[] lights,int uniformLight,int overlay,boolean existingColor){
            int[] data=quad.getVertices();if(data.length!=32)return false;
            Api api=writer==null?null:writer.api();int bytes=api==null?0:api.stride()*4;Scratch scratch=SCRATCH.get();
            boolean cached=scratch.depth==0&&bytes<=MAX_CACHED_BYTES;
            long pointer=0;
            if(api!=null&&cached){
                if(scratch.capacity<bytes){scratch.release();scratch.capacity=Math.max(1024,Integer.highestOneBit(bytes-1)<<1);scratch.address=MemoryUtil.nmemAllocChecked(scratch.capacity);}
                pointer=scratch.address;
            }else if(api!=null)pointer=MemoryUtil.nmemAllocChecked(bytes);
            scratch.depth++;
            // Nested target writers may call another alpha consumer. Give such
            // a call its own vectors as well as an independent native block.
            Vector4f p=cached?scratch.position:new Vector4f();Vector3f n=cached?scratch.normal:new Vector3f();
            try(MemoryStack stack=api==null?null:MemoryStack.stackPush()){
                if(api!=null)MemoryUtil.memSet(pointer,0,bytes);
                var direction=quad.getDirection().getNormal();n.set(direction.getX(),direction.getY(),direction.getZ());pose.normal().transform(n);
                for(int i=0;i<4;i++){
                    int base=i*8;int color=data[base+3];float shade=brightness==null?1:brightness[i];
                    float r=shade*red,g=shade*green,b=shade*blue,a=alpha;
                    if(existingColor){r=((color&255)/255F)*shade*red;g=(((color>>>8)&255)/255F)*shade*green;b=(((color>>>16)&255)/255F)*shade*blue;a=alpha*((color>>>24)&255)/255F;}
                    // The old wrapper receives already-quantized integer alpha,
                    // then rounds its opacity product. Preserve both operations.
                    int ir=(int)(r*255),ig=(int)(g*255),ib=(int)(b*255),ia=Math.round((int)(a*255)*opacity);
                    p.set(Float.intBitsToFloat(data[base]),Float.intBitsToFloat(data[base+1]),Float.intBitsToFloat(data[base+2]),1);pose.pose().transform(p);
                    float u=Float.intBitsToFloat(data[base+4]),v=Float.intBitsToFloat(data[base+5]);
                    int light=lights==null?uniformLight:lights[i],baked=data[base+6];
                    int packedLight=Math.max(light&65535,baked&65535)|(Math.max((light>>>16)&65535,(baked>>>16)&65535)<<16);
                    int normal=data[base+7];byte nx=(byte)normal,ny=(byte)(normal>>>8),nz=(byte)(normal>>>16);
                    // Forge's legacy bulk method keeps this vector between
                    // vertices: a zero baked normal retains its previous value.
                    if(nx!=0||ny!=0||nz!=0){n.set(nx/127F,ny/127F,nz/127F);n.mul(pose.normal());}
                    if(api==null){
                        // Keep each call on the original target, ignoring its
                        // returned consumer just like Alpha's legacy methods.
                        // Iris must complete every vertex to generate its quad
                        // normal, tangent, entity ID and mid-texture attributes.
                        target.vertex(p.x,p.y,p.z);
                        target.color(ir,ig,ib,ia);
                        target.uv(u,v);
                        target.overlayCoords(overlay&65535,(overlay>>>16)&65535);
                        target.uv2(packedLight&65535,(packedLight>>>16)&65535);
                        target.normal(n.x,n.y,n.z);
                        target.endVertex();
                    }else{
                        long vertex=pointer+(long)i*api.stride();
                        MemoryUtil.memPutFloat(vertex+api.position(),p.x);MemoryUtil.memPutFloat(vertex+api.position()+4,p.y);MemoryUtil.memPutFloat(vertex+api.position()+8,p.z);
                        MemoryUtil.memPutInt(vertex+api.color(),(ir&255)|((ig&255)<<8)|((ib&255)<<16)|((ia&255)<<24));
                        MemoryUtil.memPutFloat(vertex+api.texture(),u);MemoryUtil.memPutFloat(vertex+api.texture()+4,v);
                        MemoryUtil.memPutInt(vertex+api.overlay(),overlay);MemoryUtil.memPutInt(vertex+api.light(),packedLight);
                        MemoryUtil.memPutInt(vertex+api.normal(),(BufferVertexConsumer.normalIntValue(n.x)&255)|((BufferVertexConsumer.normalIntValue(n.y)&255)<<8)|((BufferVertexConsumer.normalIntValue(n.z)&255)<<16));
                    }
                }
                if(api!=null)api.push().invokeExact(writer.target(),stack,pointer,4,api.format());return true;
            }catch(Throwable failure){throw propagate(failure);}
            finally{scratch.depth--;if(api!=null&&!cached)MemoryUtil.nmemFree(pointer);}
        }
        private static RuntimeException propagate(Throwable failure){
            if(failure instanceof java.lang.reflect.InvocationTargetException wrapped&&wrapped.getCause()!=null)return propagate(wrapped.getCause());
            if(failure instanceof RuntimeException runtime)return runtime;if(failure instanceof Error error)throw error;
            return new IllegalStateException("Packed void-flight vertex writer failed",failure);
        }
        private static void releaseScratch(){Scratch scratch=SCRATCH.get();scratch.release();SCRATCH.remove();}
    }
}
