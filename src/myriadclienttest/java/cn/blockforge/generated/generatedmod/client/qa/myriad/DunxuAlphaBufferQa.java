package cn.blockforge.generated.generatedmod.client.qa.myriad;

import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.inventory.InventoryMenu;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.util.*;

/** Actual BufferBuilder comparison against the pre-optimization consumer. QA only. */
public final class DunxuAlphaBufferQa {
    private static final String OWNER="cn.blockforge.generated.generatedmod.client.DunxuTransparency";
    private static final float[] BRIGHTNESS={.173F,.507F,.811F,1F};
    private static final int[] LIGHTS={0,0x00f00000,0x000000f0,0x00d00090};
    private DunxuAlphaBufferQa(){}
    private static final class Legacy implements VertexConsumer {
        final VertexConsumer target;final float opacity;
        Legacy(VertexConsumer target,float opacity){this.target=target;this.opacity=opacity;}
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
    private static Constructor<?> constructor(ClassLoader loader)throws Exception{
        Constructor<?> c=Class.forName(OWNER+"$Alpha",true,loader).getDeclaredConstructor(VertexConsumer.class,float.class,boolean.class);c.setAccessible(true);return c;
    }
    private static VertexConsumer candidate(Constructor<?> c,VertexConsumer target,float opacity,boolean entity)throws Exception{
        return (VertexConsumer)c.newInstance(target,opacity,entity);
    }
    private static Object writer(VertexConsumer consumer)throws Exception{
        Field f=consumer.getClass().getDeclaredField("writer");f.setAccessible(true);return f.get(consumer);
    }
    private static boolean irisExtending(VertexConsumer target)throws Exception{
        try{Class<?> type=Class.forName("net.irisshaders.iris.vertices.IrisExtendedBufferBuilder");return type.isInstance(target)&&(boolean)type.getMethod("iris$extending").invoke(target);}
        catch(ClassNotFoundException absent){return false;}
    }
    private static BakedQuad quad(Minecraft mc,int variant){
        int[] data=new int[32];int[] alpha={0,1,127,255};
        for(int i=0;i<4;i++){
            int b=i*8;
            data[b]=Float.floatToRawIntBits((i==0||i==3?-.37F:.81F)+variant*.013F);
            data[b+1]=Float.floatToRawIntBits(i<2?-.29F:1.17F);
            data[b+2]=Float.floatToRawIntBits(.137F+(i&1)*.29F);
            data[b+3]=(31+i*61)|((244-i*53)<<8)|((17+i*71)<<16)|(alpha[(i+variant)%4]<<24);
            data[b+4]=Float.floatToRawIntBits(.03125F+i*.21875F);
            data[b+5]=Float.floatToRawIntBits(.9375F-i*.203125F);
            data[b+6]=i%2==0?0x00f00020:0x000000f0;
            // Mixed zeros intentionally preserve Forge's previous-normal behavior.
            data[b+7]=switch((i+variant)%4){case 0->0;case 1->0x00567f21;case 2->0x007f0080;default->0x002d93fa;};
        }
        return new BakedQuad(data,-1,Direction.values()[variant%6],mc.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(MissingTextureAtlasSprite.getLocation()),true);
    }
    private static PoseStack.Pose pose(int variant){
        PoseStack p=new PoseStack();
        if(variant>0){p.translate(17.173,-3.712,9.139);p.mulPose(Axis.YP.rotationDegrees(37.3F));p.mulPose(Axis.XP.rotationDegrees(-21.7F));}
        if(variant>1)p.scale(.37F,1.73F,-.81F);
        if(variant>2)p.scale(1.53F,.61F,.23F);
        return p.last();
    }
    private static void submit(VertexConsumer c,PoseStack.Pose p,BakedQuad q,int overload,float alpha,boolean existing){
        switch(overload){
            case 0->c.putBulkData(p,q,.713F,.371F,.913F,0x009000b0,0x12345678);
            case 1->c.putBulkData(p,q,BRIGHTNESS,.713F,.371F,.913F,LIGHTS,0x12345678,existing);
            case 2->c.putBulkData(p,q,.713F,.371F,.913F,alpha,0x009000b0,0x12345678,existing);
            default->c.putBulkData(p,q,BRIGHTNESS,.713F,.371F,.913F,alpha,LIGHTS,0x12345678,existing);
        }
    }
    private static long compare(BufferBuilder.RenderedBuffer a,BufferBuilder.RenderedBuffer b,String description){
        VertexFormat format=a.drawState().format();
        if(!format.equals(b.drawState().format())||a.drawState().vertexCount()!=b.drawState().vertexCount())throw new AssertionError("Dunxu format/count "+description);
        ByteBuffer old=a.vertexBuffer(),now=b.vertexBuffer();int stride=format.getVertexSize(),offset=0;long checked=0;
        for(VertexFormatElement element:format.getElements()){
            if(element.getUsage()!=VertexFormatElement.Usage.PADDING)
                for(int v=0;v<a.drawState().vertexCount();v++)for(int k=0;k<element.getByteSize();k++){
                    int index=v*stride+offset+k;
                    if(old.get(index)!=now.get(index))throw new AssertionError("Dunxu byte mismatch "+description+" vertex="+v+" element="+element+" byte="+k+" old="+(old.get(index)&255)+" new="+(now.get(index)&255));
                    checked++;
                }
            offset+=element.getByteSize();
        }
        return checked;
    }
    private static final class NoSodiumLoader extends ClassLoader {
        int denied;
        NoSodiumLoader(ClassLoader parent){super(parent);}
        @Override protected synchronized Class<?> loadClass(String name,boolean resolve)throws ClassNotFoundException{
            if(name.startsWith("net.caffeinemc.mods.sodium.")){denied++;throw new ClassNotFoundException("QA intentionally excludes optional API: "+name);}
            if(!name.equals(OWNER)&&!name.startsWith(OWNER+"$"))return super.loadClass(name,resolve);
            Class<?> value=findLoadedClass(name);
            if(value==null)try(var input=getParent().getResourceAsStream(name.replace('.','/')+".class")){
                if(input==null)throw new ClassNotFoundException(name);byte[] bytes=input.readAllBytes();value=defineClass(name,bytes,0,bytes.length);
            }catch(java.io.IOException e){throw new ClassNotFoundException(name,e);}
            if(resolve)resolveClass(value);return value;
        }
    }
    private static Map<String,Object> benchmark(Constructor<?> constructor,Minecraft mc,boolean old)throws Exception{
        BufferBuilder target=new BufferBuilder(65536);VertexConsumer c=null;
        BakedQuad q=quad(mc,3);PoseStack.Pose p=pose(3);
        com.sun.management.ThreadMXBean bean=ManagementFactory.getPlatformMXBean(com.sun.management.ThreadMXBean.class);
        if(bean.isThreadAllocatedMemorySupported()&&!bean.isThreadAllocatedMemoryEnabled())bean.setThreadAllocatedMemoryEnabled(true);
        long allocated=0,elapsed=0;int count=0;
        for(int round=0;round<60;round++){
            target.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);
            if(c==null){c=old?new Legacy(target,.4F):candidate(constructor,target,.4F,true);if(!old&&net.minecraftforge.fml.ModList.get().isLoaded("embeddium")&&!irisExtending(target)&&writer(c)==null)throw new AssertionError("Benchmark did not bind packed writer after begin");}
            long beforeBytes=bean.isThreadAllocatedMemorySupported()?bean.getThreadAllocatedBytes(Thread.currentThread().getId()):0,start=System.nanoTime();
            for(int i=0;i<128;i++)submit(c,p,q,3,.613F,true);
            long end=System.nanoTime(),afterBytes=bean.isThreadAllocatedMemorySupported()?bean.getThreadAllocatedBytes(Thread.currentThread().getId()):0;
            if(round>=20){allocated+=afterBytes-beforeBytes;elapsed+=end-start;count+=128;}
            target.end().release();
        }
        return Map.of("quads",count,"javaAllocatedBytes",allocated,"bytesPerQuad",allocated/(double)count,"nanosecondsPerQuad",elapsed/(double)count,"backend",old?"legacy":writer(c)!=null?"packed":"cached-legacy-hooks","scope","quad encode/submission only; not full-model or frame benchmark");
    }
    public static Map<String,Object> verify(Minecraft mc)throws Exception{
        ClassLoader loader=DunxuAlphaBufferQa.class.getClassLoader();Constructor<?> ctor=constructor(loader);
        BufferBuilder a=new BufferBuilder(4096),b=new BufferBuilder(4096);long bytes=0;int cases=0,packedCases=0,irisFallbackCases=0;
        Set<String> layouts=new TreeSet<>();
        for(int pv=0;pv<4;pv++)for(int qv=0;qv<6;qv++)for(float opacity:new float[]{0,.259F,.4F,.72F,1})for(boolean existing:new boolean[]{false,true})for(int overload=0;overload<4;overload++){
            BakedQuad q=quad(mc,qv);int[] source=q.getVertices().clone();PoseStack.Pose p=pose(pv);
            a.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);
            VertexConsumer current=candidate(ctor,b,opacity,true);if(writer(current)!=null)packedCases++;
            if(irisExtending(b)){if(writer(current)!=null)throw new AssertionError("Iris extended target selected unsafe packed path");irisFallbackCases++;}
            submit(new Legacy(a,opacity),p,q,overload,.613F,existing);submit(current,p,q,overload,.613F,existing);
            var old=a.end();var now=b.end();try{layouts.add(old.drawState().format().toString());bytes+=compare(old,now,"pose="+pv+",quad="+qv+",opacity="+opacity+",existing="+existing+",overload="+overload);}finally{old.release();now.release();}
            if(!Arrays.equals(source,q.getVertices()))throw new AssertionError("Dunxu mutated baked source");cases++;
        }
        boolean embeddium=net.minecraftforge.fml.ModList.get().isLoaded("embeddium");
        if(embeddium&&packedCases!=cases-irisFallbackCases)throw new AssertionError("Embeddium packed quad path was not exercised: "+packedCases+"/"+(cases-irisFallbackCases)+"; Iris legacy cases="+irisFallbackCases);
        // Real child-loader absence: production class resolves no Sodium class,
        // while Minecraft's types and actual target builder remain shared.
        NoSodiumLoader plain=new NoSodiumLoader(loader);Constructor<?> plainCtor=constructor(plain);
        a.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);b.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.NEW_ENTITY);
        VertexConsumer fallback=candidate(plainCtor,b,.4F,true);if(writer(fallback)!=null||plain.denied==0)throw new AssertionError("Missing optional API did not select legacy fallback");
        submit(new Legacy(a,.4F),pose(3),quad(mc,2),3,.613F,true);submit(fallback,pose(3),quad(mc,2),3,.613F,true);
        var old=a.end();var now=b.end();try{bytes+=compare(old,now,"isolated-no-Sodium");}finally{old.release();now.release();}
        VertexConsumer nonEntity=candidate(ctor,b,.4F,false);if(writer(nonEntity)!=null)throw new AssertionError("Non NEW_ENTITY selected packed path");
        Map<String,Object> result=new LinkedHashMap<>();result.put("cases",cases);result.put("comparedAttributeBytes",bytes);result.put("packedCases",packedCases);result.put("irisFallbackCases",irisFallbackCases);result.put("embeddiumPresent",embeddium);result.put("layouts",layouts);result.put("missingApiClassloaderDenials",plain.denied);result.put("legacyBenchmark",benchmark(ctor,mc,true));result.put("candidateBenchmark",benchmark(ctor,mc,false));result.put("normalSemantics","Preserves Forge47.4.10 legacy previous-normal behavior for mixed zero/nonzero baked normals; actual Iris extended targets retain original per-vertex/endVertex generation.");result.put("scope","Actual BufferBuilder bytes, 4 overloads, source immutability, optional API absent in isolated production loader; no visual or frame-performance claim.");return result;
    }
}
