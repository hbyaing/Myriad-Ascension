package cn.blockforge.generated.generatedmod.client.arts;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

import java.util.List;

/** Integrates the rolled fire front through world-space depth. */
final class FlameWaveVolume {
    record Sample(Vec3 origin,Vec3 forward,float age,float front,float strength,float[] floor) {}

    private static final BufferBuilder SCREEN=new BufferBuilder(256);
    static ShaderInstance shader;

    private FlameWaveVolume() {}

    static boolean draw(List<Sample> waves,Matrix4f inverseClip,int width,int height) {
        if(shader==null || waves.isEmpty()) return false;
        RenderSystem.setShader(()->shader);
        shader.safeGetUniform("InverseClip").set(inverseClip);
        shader.safeGetUniform("OutputSize").set((float)width,(float)height);
        for(var wave:waves) {
            var origin=wave.origin;
            var floor=wave.floor;
            shader.safeGetUniform("Origin").set((float)origin.x,(float)origin.y,(float)origin.z);
            shader.safeGetUniform("Forward").set((float)wave.forward.x,(float)wave.forward.z);
            shader.safeGetUniform("Wave").set(wave.age,wave.front,wave.strength);
            shader.safeGetUniform("FloorLeft").set(floor[0],floor[1],floor[2],floor[3]);
            shader.safeGetUniform("FloorRight").set(floor[4],floor[5],floor[6],floor[7]);
            SCREEN.begin(VertexFormat.Mode.QUADS,DefaultVertexFormat.POSITION);
            SCREEN.vertex(-1,-1,0).endVertex();
            SCREEN.vertex(1,-1,0).endVertex();
            SCREEN.vertex(1,1,0).endVertex();
            SCREEN.vertex(-1,1,0).endVertex();
            BufferUploader.drawWithShader(SCREEN.end());
        }
        return true;
    }
}
