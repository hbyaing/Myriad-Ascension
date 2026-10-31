package cn.blockforge.generated.generatedmod.client.qa.myriad;

import com.mojang.blaze3d.platform.NativeImage;
import java.lang.reflect.Field;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.*;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

/** QA-only whole-background evidence, sampled before legitimate terrain/GUI occlusion. */
final class MindscapeCoverageProbe {
    private static final int W=192,H=108,COLS=8,ROWS=6;
    private static final double MAX_WHITE_COMPONENT=.01,MIN_TILE_VARIANCE=.00001,MIN_GLOBAL_VARIANCE=.00005;
    private static int framebuffer,texture;
    static final class Sample {
        final Map<String,Object> evidence;
        final byte[] field,target;
        Sample(Map<String,Object> evidence,byte[] field,byte[] target){this.evidence=evidence;this.field=field;this.target=target;}
        void writeImages(Path directory,String name)throws Exception{
            write(directory.resolve(name+"-field.png"),field);write(directory.resolve(name+"-after-sky.png"),target);
        }
    }
    private MindscapeCoverageProbe(){}
    private static int privateInt(Class<?> owner,String name)throws Exception{Field field=owner.getDeclaredField(name);field.setAccessible(true);return field.getInt(null);}
    static Sample capture()throws Exception{
        Class<?> renderer=Class.forName("cn.blockforge.generated.generatedmod.client.mindscape.MindscapeBackgroundRenderer");
        int fieldFbo=privateInt(renderer,"framebuffer"),fieldWidth=privateInt(renderer,"width"),fieldHeight=privateInt(renderer,"height");
        if(fieldFbo==0||fieldWidth<=0||fieldHeight<=0)throw new AssertionError("Background field FBO is unavailable");
        State saved=new State();Readback fieldRead,targetRead;
        try{
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,0);initialize();GL11.glDisable(GL11.GL_SCISSOR_TEST);GL11.glColorMask(true,true,true,true);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,0);GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,1);
            GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES,0);
            GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,0);GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,0);GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,0);
            fieldRead=read(fieldFbo,GL30.GL_COLOR_ATTACHMENT0,0,0,fieldWidth,fieldHeight);
            targetRead=read(saved.drawFbo,saved.drawBuffer,saved.viewport[0],saved.viewport[1],saved.viewport[2],saved.viewport[3]);
        }finally{saved.close();}
        if(!saved.restored())throw new AssertionError("Whole-background probe did not restore GL state");
        int error=GL11.glGetError();if(error!=GL11.GL_NO_ERROR)throw new AssertionError("Whole-background probe GL error "+error);
        var fieldStats=statistics(fieldRead,true);var targetStats=statistics(targetRead,false);
        var comparisons=new ArrayList<Map<String,Object>>();int divergent=0;
        @SuppressWarnings("unchecked") var fieldTiles=(List<Map<String,Object>>)fieldStats.get("tiles");
        @SuppressWarnings("unchecked") var targetTiles=(List<Map<String,Object>>)targetStats.get("tiles");
        for(int i=0;i<fieldTiles.size();i++){
            @SuppressWarnings("unchecked") var a=(List<Double>)fieldTiles.get(i).get("meanRGB");
            @SuppressWarnings("unchecked") var b=(List<Double>)targetTiles.get(i).get("meanRGB");
            double difference=0;for(int c=0;c<3;c++)difference=Math.max(difference,Math.abs(a.get(c)-b.get(c)));
            if(difference>.15)divergent++;
            comparisons.add(Map.of("column",i%COLS,"row",i/COLS,"maxMeanChannelDifference",difference));
        }
        var failures=new ArrayList<String>();
        if(((Number)fieldStats.get("uncoveredAlphaRatio")).doubleValue()>0)failures.add("Field accumulated fold coverage is below .999 for sampled rays");
        for(var measured:List.of(fieldStats,targetStats)){
            if(((Number)measured.get("luminanceVariance")).doubleValue()<MIN_GLOBAL_VARIANCE
                    ||((Number)measured.get("texturedTiles")).intValue()<COLS*ROWS/2)
                failures.add((measured==fieldStats?"Field":"After-sky target")+" lacks texture variation globally or in at least half the tiles");
        }
        if(divergent>2)failures.add("Actual target differs from field in "+divergent+" background tiles");
        var evidence=new LinkedHashMap<String,Object>();evidence.put("sampleSize",List.of(W,H));evidence.put("tileGrid",List.of(COLS,ROWS));
        evidence.put("sourceFieldFramebuffer",fieldFbo);evidence.put("sourceTargetFramebuffer",saved.drawFbo);
        evidence.put("field",fieldStats);evidence.put("afterSky",targetStats);evidence.put("targetComparisons",comparisons);
        evidence.put("divergentTiles",divergent);evidence.put("glStateRestored",true);evidence.put("passed",failures.isEmpty());evidence.put("failures",failures);
        evidence.put("scope","Full-screen field and real early composite before terrain/entity/particle/GUI drawing. Float alpha checks true accumulated fold coverage; texture variation and target agreement reject a constant-alpha flat fallback. Near-white regions are diagnostic only: silver-white material is allowed.");
        return new Sample(evidence,fieldRead.rgba(),targetRead.rgba());
    }
    private static void initialize(){
        if(framebuffer!=0)return;
        framebuffer=GL30.glGenFramebuffers();texture=GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MIN_FILTER,GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D,GL11.GL_TEXTURE_MAG_FILTER,GL11.GL_LINEAR);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D,0,GL30.GL_RGBA16F,W,H,0,GL11.GL_RGBA,GL11.GL_FLOAT,(ByteBuffer)null);
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER,GL30.GL_COLOR_ATTACHMENT0,GL11.GL_TEXTURE_2D,texture,0);
        GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
        if(GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER)!=GL30.GL_FRAMEBUFFER_COMPLETE)throw new AssertionError("QA background readback FBO incomplete");
    }
    private record Readback(byte[] rgba,float[] alpha){}
    private static Readback read(int source,int sourceBuffer,int x,int y,int width,int height){
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,source);int previousBuffer=GL11.glGetInteger(GL11.GL_READ_BUFFER);
        try{
            GL11.glReadBuffer(sourceBuffer);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,framebuffer);GL11.glDrawBuffer(GL30.GL_COLOR_ATTACHMENT0);
            GL30.glBlitFramebuffer(x,y,x+width,y+height,0,0,W,H,GL11.GL_COLOR_BUFFER_BIT,GL11.GL_LINEAR);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,framebuffer);GL11.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0);
            FloatBuffer pixels=BufferUtils.createFloatBuffer(W*H*4);GL11.glReadPixels(0,0,W,H,GL11.GL_RGBA,GL11.GL_FLOAT,pixels);
            byte[] topDown=new byte[W*H*4];float[] alpha=new float[W*H];
            for(int row=0;row<H;row++)for(int column=0;column<W;column++){
                int sourcePixel=((H-1-row)*W+column)*4,destination=(row*W+column)*4;
                for(int channel=0;channel<4;channel++){float value=pixels.get(sourcePixel+channel);if(!Float.isFinite(value))throw new AssertionError("Non-finite background readback");topDown[destination+channel]=(byte)Math.round(Math.max(0,Math.min(1,value))*255);}
                alpha[row*W+column]=pixels.get(sourcePixel+3);
            }
            return new Readback(topDown,alpha);
        }finally{GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,source);GL11.glReadBuffer(previousBuffer);}
    }
    private static Map<String,Object> statistics(Readback readback,boolean checkAlpha){
        byte[] pixels=readback.rgba();
        boolean[] white=new boolean[W*H];int whites=0,uncovered=0;double sum=0,squared=0;
        var tiles=new ArrayList<Map<String,Object>>();int texturedTiles=0;float minimumAlpha=Float.POSITIVE_INFINITY;
        for(int row=0;row<ROWS;row++)for(int col=0;col<COLS;col++){
            int n=0,w=0,red=0,gold=0,blue=0;double rSum=0,gSum=0,bSum=0,lSum=0,l2=0;
            for(int y=row*H/ROWS;y<(row+1)*H/ROWS;y++)for(int x=col*W/COLS;x<(col+1)*W/COLS;x++){
                int p=(y*W+x)*4,r=pixels[p]&255,g=pixels[p+1]&255,b=pixels[p+2]&255;float alpha=readback.alpha()[y*W+x];minimumAlpha=Math.min(minimumAlpha,alpha);
                double lum=(.2126*r+.7152*g+.0722*b)/255.;rSum+=r/255.;gSum+=g/255.;bSum+=b/255.;lSum+=lum;l2+=lum*lum;sum+=lum;squared+=lum*lum;n++;
                boolean neutral=Math.min(r,Math.min(g,b))>.86*255&&Math.max(r,Math.max(g,b))-Math.min(r,Math.min(g,b))<.08*255;
                if(neutral){white[y*W+x]=true;w++;whites++;}if(checkAlpha&&alpha<.999f)uncovered++;
                if(r>=45&&r-g>=18&&r-b>=12&&2*r>=3*g)red++;
                if(r>=70&&g>=50&&r-b>=20&&g-b>=8&&10*r>=9*g)gold++;
                if(b>=40&&b-r>=12&&b>=g)blue++;
            }
            var tile=new LinkedHashMap<String,Object>();tile.put("column",col);tile.put("row",row);tile.put("meanRGB",List.of(rSum/n,gSum/n,bSum/n));
            double variance=Math.max(0,l2/n-Math.pow(lSum/n,2));if(variance>=MIN_TILE_VARIANCE)texturedTiles++;
            tile.put("luminanceVariance",variance);tile.put("nearWhiteRatio",w/(double)n);
            tile.put("redRatio",red/(double)n);tile.put("goldRatio",gold/(double)n);tile.put("blueRatio",blue/(double)n);tiles.add(tile);
        }
        int[] integral=new int[(W+1)*(H+1)];for(int y=0;y<H;y++)for(int x=0;x<W;x++){int i=(y+1)*(W+1)+x+1;integral[i]=(white[y*W+x]?1:0)+integral[i-1]+integral[i-W-1]-integral[i-W-2];}
        boolean[] seen=new boolean[W*H];int[] queue=new int[W*H];int largest=0;Map<String,Object> largestRegion=Map.of();boolean hole=false;
        int thickW=(int)Math.ceil(W*.05),thickH=(int)Math.ceil(H*.05);
        for(int start=0;start<white.length;start++){
            if(!white[start]||seen[start])continue;int head=0,tail=1,minX=W,minY=H,maxX=0,maxY=0;double componentSum=0,componentSquared=0;boolean thick=false;queue[0]=start;seen[start]=true;
            while(head<tail){int at=queue[head++],x=at%W,y=at/W;minX=Math.min(minX,x);minY=Math.min(minY,y);maxX=Math.max(maxX,x);maxY=Math.max(maxY,y);
                int p=at*4;double lum=(.2126*(pixels[p]&255)+.7152*(pixels[p+1]&255)+.0722*(pixels[p+2]&255))/255.;componentSum+=lum;componentSquared+=lum*lum;
                if(x+thickW<=W&&y+thickH<=H){int a=y*(W+1)+x,b=a+thickW,c=(y+thickH)*(W+1)+x,d=c+thickW;if(integral[d]-integral[b]-integral[c]+integral[a]==thickW*thickH)thick=true;}
                for(int neighbor:new int[]{x>0?at-1:-1,x+1<W?at+1:-1,y>0?at-W:-1,y+1<H?at+W:-1})if(neighbor>=0&&white[neighbor]&&!seen[neighbor]){seen[neighbor]=true;queue[tail++]=neighbor;}
            }
            double ratio=tail/(double)(W*H);if(ratio>MAX_WHITE_COMPONENT&&thick)hole=true;
            if(tail>largest){largest=tail;largestRegion=Map.of("pixels",tail,"ratio",ratio,"boxPixels",List.of(minX,minY,maxX+1,maxY+1),"containsThickRectangle",thick,"luminanceVariance",Math.max(0,componentSquared/tail-Math.pow(componentSum/tail,2)));}
        }
        var result=new LinkedHashMap<String,Object>();result.put("tiles",tiles);result.put("nearWhiteRatio",whites/(double)(W*H));
        result.put("uncoveredAlphaRatio",uncovered/(double)(W*H));result.put("luminanceVariance",Math.max(0,squared/(W*H)-Math.pow(sum/(W*H),2)));
        result.put("largestNearWhiteRegion",largestRegion);result.put("largeNearWhiteRegionDiagnostic",hole);result.put("nearWhiteIsFailure",false);
        result.put("whiteThreshold","minRGB>0.86, maxRGB-minRGB<0.08");result.put("maxWhiteComponentRatio",MAX_WHITE_COMPONENT);
        result.put("minimumThickRectangle",List.of(thickW,thickH));result.put("minimumAlpha",minimumAlpha);result.put("minimumRequiredCoverage",.999);
        result.put("texturedTiles",texturedTiles);result.put("minimumTexturedTiles",COLS*ROWS/2);result.put("minimumTileVariance",MIN_TILE_VARIANCE);result.put("minimumGlobalVariance",MIN_GLOBAL_VARIANCE);return result;
    }
    private static void write(Path path,byte[] pixels)throws Exception{
        try(var image=new NativeImage(W,H,true)){for(int y=0;y<H;y++)for(int x=0;x<W;x++){int p=(y*W+x)*4;image.setPixelRGBA(x,y,(pixels[p]&255)|((pixels[p+1]&255)<<8)|((pixels[p+2]&255)<<16)|((pixels[p+3]&255)<<24));}image.writeToFile(path);}
    }
    static void release(){if(framebuffer!=0)GL30.glDeleteFramebuffers(framebuffer);if(texture!=0)GL11.glDeleteTextures(texture);framebuffer=texture=0;}
    private static final class State implements AutoCloseable {
        final int readFbo=GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING),drawFbo=GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        final int readBuffer=GL11.glGetInteger(GL11.GL_READ_BUFFER),drawBuffer=GL11.glGetInteger(GL20.GL_DRAW_BUFFER0);
        final int pbo=GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING),alignment=GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT);
        final int unpackPbo=GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING),swapBytes=GL11.glGetInteger(GL11.GL_PACK_SWAP_BYTES);
        final int rowLength=GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH),skipRows=GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS),skipPixels=GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS);
        final int active=GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE),texture0;final int[] viewport=new int[4];
        final boolean scissor=GL11.glIsEnabled(GL11.GL_SCISSOR_TEST);final ByteBuffer colorMask=BufferUtils.createByteBuffer(4);
        State(){GL11.glGetIntegerv(GL11.GL_VIEWPORT,viewport);GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,colorMask);GL13.glActiveTexture(GL13.GL_TEXTURE0);texture0=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);GL13.glActiveTexture(active);}
        public void close(){
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER,readFbo);GL11.glReadBuffer(readBuffer);GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER,drawFbo);
            GL15.glBindBuffer(GL21.GL_PIXEL_PACK_BUFFER,pbo);GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT,alignment);GL11.glPixelStorei(GL11.GL_PACK_ROW_LENGTH,rowLength);GL11.glPixelStorei(GL11.GL_PACK_SKIP_ROWS,skipRows);GL11.glPixelStorei(GL11.GL_PACK_SKIP_PIXELS,skipPixels);
            GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER,unpackPbo);GL11.glPixelStorei(GL11.GL_PACK_SWAP_BYTES,swapBytes);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);GL11.glBindTexture(GL11.GL_TEXTURE_2D,texture0);GL13.glActiveTexture(active);
            if(scissor)GL11.glEnable(GL11.GL_SCISSOR_TEST);else GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glColorMask(colorMask.get(0)!=0,colorMask.get(1)!=0,colorMask.get(2)!=0,colorMask.get(3)!=0);
        }
        boolean restored(){
            int[] currentViewport=new int[4];GL11.glGetIntegerv(GL11.GL_VIEWPORT,currentViewport);ByteBuffer mask=BufferUtils.createByteBuffer(4);GL11.glGetBooleanv(GL11.GL_COLOR_WRITEMASK,mask);
            boolean maskRestored=true;for(int i=0;i<4;i++)maskRestored&=mask.get(i)==colorMask.get(i);
            GL13.glActiveTexture(GL13.GL_TEXTURE0);boolean textureRestored=GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D)==texture0;GL13.glActiveTexture(active);
            return Arrays.equals(viewport,currentViewport)&&maskRestored&&textureRestored&&GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)==readFbo&&GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)==drawFbo
                &&GL11.glGetInteger(GL11.GL_READ_BUFFER)==readBuffer&&GL11.glGetInteger(GL20.GL_DRAW_BUFFER0)==drawBuffer
                &&GL11.glGetInteger(GL21.GL_PIXEL_PACK_BUFFER_BINDING)==pbo&&GL11.glGetInteger(GL11.GL_PACK_ALIGNMENT)==alignment
                &&GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING)==unpackPbo&&GL11.glGetInteger(GL11.GL_PACK_SWAP_BYTES)==swapBytes
                &&GL11.glGetInteger(GL11.GL_PACK_ROW_LENGTH)==rowLength&&GL11.glGetInteger(GL11.GL_PACK_SKIP_ROWS)==skipRows&&GL11.glGetInteger(GL11.GL_PACK_SKIP_PIXELS)==skipPixels
                &&GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE)==active&&GL11.glIsEnabled(GL11.GL_SCISSOR_TEST)==scissor;
        }
    }
}
