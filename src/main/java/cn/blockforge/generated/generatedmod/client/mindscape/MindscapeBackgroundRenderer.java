package cn.blockforge.generated.generatedmod.client.mindscape;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.client.MindscapeClient;
import cn.blockforge.generated.generatedmod.client.TrailShaderBridge;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.*;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Session-local finite background. It draws BEFORE blocks/entities/translucency,
 * with real shell depth. A dimension-specific factory selects vanilla rendering
 * for this interior while leaving the configured outside shader pack intact. Nothing
 * repaints the final scene over particles, glass, entities, or the player's hand.
 * The further procedural membrane intersections are material coordinates, not
 * depth surfaces in the Minecraft world. This is a surface renderer, not fluid.
 */
@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class MindscapeBackgroundRenderer {
    public static final int RENDER_SCALE = 2;
    public static int generation, draws, backgroundDraws;
    public static float lastPurification, lastTime;
    public static String failure = "";

    private static Program field, shell;
    private static Palette corrupted, purified;
    private static int vao, framebuffer, texture, width, height;
    private static boolean reloadRequested;
    private static UUID session;
    private static int lastDrawFramebuffer, lastStageFramebuffer, lastOutputWidth, lastOutputHeight;
    private static boolean independentDimensionPipeline;
    private static String dimensionPipeline = "not-rendered";
    private static boolean lastRestored = true, packDetected;
    private static Vec3 lastAnchor = Vec3.ZERO, lastEye = Vec3.ZERO;
    private static final Vector3f shellMin = new Vector3f(), shellMax = new Vector3f();
    private static String corruptedHash = "", purifiedHash = "";
    private static final String[] COLOR_KEYS = {
            "goldBase", "goldHighlight", "redBase", "spiritBase", "spiritEmission"
    };

    @Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT,
            bus = Mod.EventBusSubscriber.Bus.MOD)
    public static final class Resources {
        @SubscribeEvent
        public static void reload(RegisterClientReloadListenersEvent event) {
            event.registerReloadListener((ResourceManagerReloadListener) manager ->
                    RenderSystem.recordRenderCall(() -> reloadRequested = true));
        }
    }

    @SubscribeEvent
    public static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        release();
        session = null;
    }

    @SubscribeEvent
    public static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_SKY) return;
        var mc = Minecraft.getInstance();
        Vec3 origin = MindscapeClient.origin();
        UUID activeSession = MindscapeClient.sessionId();
        if (origin == null || activeSession == null || mc.level == null || TrailShaderBridge.shadowPass()) return;
        if (reloadRequested) {
            release();
            reloadRequested = false;
        }
        if (!failure.isEmpty()) return;

        try (State saved = new State()) {
            initialize();
            int outputWidth = saved.viewport[2], outputHeight = saved.viewport[3];
            if (outputWidth <= 0 || outputHeight <= 0) return;
            resize(Math.multiplyExact(outputWidth, RENDER_SCALE), Math.multiplyExact(outputHeight, RENDER_SCALE));

            // Server anchor is the floor block at Y64. The material's local Y0
            // corresponds to its real walkable top at Y65, not the web camera.
            Vec3 anchor = origin.add(0, 1, 0), camera = event.getCamera().getPosition();
            Vec3 relative = anchor.subtract(camera);
            Matrix4f localToClip = new Matrix4f(event.getProjectionMatrix())
                    .mul(new Matrix4f(event.getPoseStack().last().pose()))
                    .translate((float) relative.x, (float) relative.y, (float) relative.z);
            Matrix4f clipToLocal = new Matrix4f(localToClip).invert();
            if (!clipToLocal.isFinite()) throw new IllegalStateException("Non-finite mindscape camera matrix");
            Vector4f eyeH = clipToLocal.transform(new Vector4f(0, 0, 1, 0));
            Vector3f eye = Math.abs(eyeH.w) > 1.0e-7f && Float.isFinite(eyeH.w)
                    ? new Vector3f(eyeH.x, eyeH.y, eyeH.z).div(eyeH.w)
                    : new Vector3f((float) (camera.x - anchor.x), (float) (camera.y - anchor.y), (float) (camera.z - anchor.z));
            if (!eye.isFinite()) throw new IllegalStateException("Non-finite mindscape eye");

            // A third-person camera can cross the transparent barrier. Keep the
            // same virtual field and only extend the finite depth carrier enough
            // to include that camera; the near wall must not hide its own player.
            shellMin.set(Math.min(-27.9f, eye.x - .05f), Math.min(-.1f, eye.y - .05f), Math.min(-27.9f, eye.z - .05f));
            shellMax.set(Math.max(27.9f, eye.x + .05f), Math.max(26.9f, eye.y + .05f), Math.max(27.9f, eye.z + .05f));

            session = activeSession;
            lastAnchor = anchor;
            lastEye = new Vec3(eye.x, eye.y, eye.z);
            lastPurification = MindscapeClient.purification(event.getPartialTick());
            lastTime = (mc.level.getGameTime() + event.getPartialTick()) / 20f;
            packDetected = TrailShaderBridge.shaderPackDetected();
            dimensionPipeline = MindscapeShaderPipelineCompat.pipelineName();
            independentDimensionPipeline = dimensionPipeline.endsWith(".VanillaRenderingPipeline");
            if (packDetected && !independentDimensionPipeline) throw new IllegalStateException("Mindscape dimension did not acquire its independent vanilla pipeline: " + dimensionPipeline);
            int targetFramebuffer = saved.drawFramebuffer;
            int targetAttachment = saved.drawBuffers[0];
            if (targetAttachment == GL11.GL_NONE) throw new IllegalStateException("Mindscape target has no color output");
            lastOutputWidth = outputWidth;
            lastOutputHeight = outputHeight;
            lastStageFramebuffer = saved.drawFramebuffer;
            lastDrawFramebuffer = targetFramebuffer;

            GL30.glBindVertexArray(vao);
            GL11.glDisable(GL11.GL_SCISSOR_TEST);
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_STENCIL_TEST);
            GL11.glDisable(GL30.GL_FRAMEBUFFER_SRGB);
            GL11.glColorMask(true, true, true, true);
            GL11.glDisable(GL11.GL_DEPTH_TEST);
            GL11.glDepthMask(false);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
            GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
            GL11.glViewport(0, 0, width, height);
            field.use();
            field.v2("resolution", width, height);
            field.f("seconds", lastTime);
            field.f("purification", lastPurification);
            field.i("stage", stage(MindscapeClient.challenge()));
            field.v4("layers", 1, 1, 1, 0);
            field.m("ClipToLocal", clipToLocal);
            field.v("EyeLocal", eye);
            uploadPalette(field, lastPurification);
            GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            backgroundDraws++;

            // Mindscape owns a vanilla world pipeline even when an external
            // shader pack is configured outside it. All translucency stays later.
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, targetFramebuffer);
            int[] targetDrawBuffers = new int[saved.drawBuffers.length];
            for (int i = 0; i < targetDrawBuffers.length; i++) targetDrawBuffers[i] = GL11.glGetInteger(GL20.GL_DRAW_BUFFER0 + i);
            try {
                GL20.glDrawBuffers(targetAttachment);
                GL11.glViewport(saved.viewport[0], saved.viewport[1], outputWidth, outputHeight);
                GL11.glEnable(GL11.GL_DEPTH_TEST);
                GL11.glDepthFunc(GL11.GL_LEQUAL);
                GL11.glDepthMask(true);
                GL13.glActiveTexture(GL13.GL_TEXTURE0);
                GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
                GL33.glBindSampler(0, 0);
                shell.use();
                shell.i("BackgroundSampler", 0);
                shell.m("ClipToLocal", clipToLocal);
                shell.m("LocalToClip", localToClip);
                shell.v("EyeLocal", eye);
                shell.v("ShellMin", shellMin);
                shell.v("ShellMax", shellMax);
                GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, 3);
            } finally {
                IntBuffer buffers = BufferUtils.createIntBuffer(targetDrawBuffers.length);
                buffers.put(targetDrawBuffers).flip();
                GL20.glDrawBuffers(buffers);
            }
            int error = GL11.glGetError();
            if (error != GL11.GL_NO_ERROR) throw new IllegalStateException("Mindscape draw GL=" + error);
            draws++;
        } catch (Exception error) {
            failure = error.toString();
            LogUtils.getLogger().error("Mindscape background disabled until resource reload", error);
        }
    }

    private static void initialize() throws Exception {
        if (field != null) return;
        try {
            field = program("field.fsh");
            shell = program("shell.fsh");
            vao = GL30.glGenVertexArrays();
            byte[] corruptData = bytes("palette-corrupted.json"), pureData = bytes("palette-purified.json");
            corrupted = Palette.read(corruptData);
            purified = Palette.read(pureData);
            corruptedHash = sha256(corruptData);
            purifiedHash = sha256(pureData);
            generation++;
        } catch (Exception error) {
            release();
            throw error;
        }
    }

    private static byte[] bytes(String path) throws Exception {
        var id = new ResourceLocation(GeneratedMod.MOD_ID, "mindscape/" + path);
        try (var stream = Minecraft.getInstance().getResourceManager().open(id)) { return stream.readAllBytes(); }
    }

    private static String sha256(byte[] data) throws Exception {
        return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(data));
    }

    private static Program program(String fragment) throws Exception {
        int vertex = 0, pixel = 0, id = GL20.glCreateProgram();
        try {
            vertex = compile(GL20.GL_VERTEX_SHADER, new String(bytes("shaders/fullscreen.vsh"), StandardCharsets.UTF_8));
            pixel = compile(GL20.GL_FRAGMENT_SHADER, new String(bytes("shaders/" + fragment), StandardCharsets.UTF_8));
            GL20.glAttachShader(id, vertex);
            GL20.glAttachShader(id, pixel);
            GL30.glBindFragDataLocation(id, 0, "fragColor");
            GL20.glLinkProgram(id);
            if (GL20.glGetProgrami(id, GL20.GL_LINK_STATUS) == 0) throw new IllegalStateException(fragment + ": " + GL20.glGetProgramInfoLog(id));
            return new Program(id);
        } catch (Exception error) {
            GL20.glDeleteProgram(id);
            throw error;
        } finally {
            if (vertex != 0) GL20.glDeleteShader(vertex);
            if (pixel != 0) GL20.glDeleteShader(pixel);
        }
    }

    private static int compile(int type, String source) {
        int id = GL20.glCreateShader(type);
        GL20.glShaderSource(id, source);
        GL20.glCompileShader(id);
        if (GL20.glGetShaderi(id, GL20.GL_COMPILE_STATUS) == 0) {
            String message = GL20.glGetShaderInfoLog(id);
            GL20.glDeleteShader(id);
            throw new IllegalStateException(message);
        }
        return id;
    }

    private static void resize(int requestedWidth, int requestedHeight) {
        if (width == requestedWidth && height == requestedHeight && texture != 0) return;
        int limit = GL11.glGetInteger(GL11.GL_MAX_TEXTURE_SIZE);
        if (requestedWidth > limit || requestedHeight > limit) throw new IllegalStateException("Mindscape 2x target exceeds GPU texture limit " + limit);
        freeTarget();
        width = requestedWidth;
        height = requestedHeight;
        texture = GL11.glGenTextures();
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture);
        GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, 0);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA16F, width, height, 0, GL11.GL_RGBA, GL11.GL_FLOAT, (ByteBuffer) null);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        framebuffer = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, framebuffer);
        GL30.glFramebufferTexture2D(GL30.GL_DRAW_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0);
        GL20.glDrawBuffers(GL30.GL_COLOR_ATTACHMENT0);
        if (GL30.glCheckFramebufferStatus(GL30.GL_DRAW_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) throw new IllegalStateException("Mindscape 2x framebuffer incomplete");
    }

    private static void uploadPalette(Program program, float amount) {
        for (int k = 0; k < COLOR_KEYS.length; k++) {
            float[] a = corrupted.colors[k], b = purified.colors[k];
            program.v("palette" + Character.toUpperCase(COLOR_KEYS[k].charAt(0)) + COLOR_KEYS[k].substring(1),
                    endpoint(a[0], b[0], amount), endpoint(a[1], b[1], amount), endpoint(a[2], b[2], amount));
        }
        program.f("paletteSpiritStrength", endpoint(corrupted.strength, purified.strength, amount));
        program.f("paletteExposure", endpoint(corrupted.exposure, purified.exposure, amount));
    }

    private static float endpoint(float a, float b, float amount) {
        return amount <= 0 ? a : amount >= 1 ? b : a + (b - a) * amount;
    }

    private static int stage(String id) {
        String path = id.substring(id.indexOf(':') + 1);
        if (path.startsWith("six_thieves_")) { try { return Math.max(0, Math.min(5, Integer.parseInt(path.substring(12)) - 1)); } catch (NumberFormatException ignored) {} }
        if (path.startsWith("three_corps_")) { try { return Math.max(6, Math.min(8, Integer.parseInt(path.substring(12)) + 5)); } catch (NumberFormatException ignored) {} }
        return path.equals("heart_demon_final") || path.equals("heart_demon_1") ? 9 : 0;
    }

    private static void freeTarget() {
        if (framebuffer != 0) GL30.glDeleteFramebuffers(framebuffer);
        if (texture != 0) GL11.glDeleteTextures(texture);
        framebuffer = texture = width = height = 0;
    }

    public static void release() {
        if (!RenderSystem.isOnRenderThread()) { RenderSystem.recordRenderCall(MindscapeBackgroundRenderer::release); return; }
        freeTarget();
        if (field != null) GL20.glDeleteProgram(field.id);
        if (shell != null) GL20.glDeleteProgram(shell.id);
        if (vao != 0) GL30.glDeleteVertexArrays(vao);
        field = shell = null;
        vao = 0;
        failure = "";
    }

    /** Read-only runtime evidence, also useful for diagnosing resource-pack overrides. */
    public static Map<String, Object> diagnostics() {
        var result = new LinkedHashMap<String, Object>();
        result.put("generation", generation); result.put("draws", draws); result.put("backgroundDraws", backgroundDraws);
        result.put("failure", failure); result.put("lastPurification", lastPurification); result.put("lastTime", lastTime);
        result.put("width", lastOutputWidth); result.put("height", lastOutputHeight);
        result.put("renderWidth", width); result.put("renderHeight", height); result.put("renderScale", RENDER_SCALE);
        result.put("packDetected", packDetected); result.put("packName", MindscapeShaderPipelineCompat.packName());
        result.put("outputColorContract", "review-native-display");
        result.put("dimensionPipeline", dimensionPipeline); result.put("independentDimensionPipeline", independentDimensionPipeline);
        result.put("independentPipelinesCreated", MindscapeShaderPipelineCompat.independentPipelinesCreated);
        result.put("session", session == null ? "" : session.toString());
        result.put("anchor", new double[]{lastAnchor.x, lastAnchor.y, lastAnchor.z});
        result.put("eyeLocal", new double[]{lastEye.x, lastEye.y, lastEye.z});
        result.put("shellMin", new float[]{shellMin.x, shellMin.y, shellMin.z}); result.put("shellMax", new float[]{shellMax.x, shellMax.y, shellMax.z});
        result.put("fieldProgram", field == null ? 0 : field.id); result.put("compositeProgram", shell == null ? 0 : shell.id);
        result.put("lastStageFramebuffer", lastStageFramebuffer); result.put("lastDrawFramebuffer", lastDrawFramebuffer); result.put("lastRestored", lastRestored);
        result.put("corruptedPaletteSHA256", corruptedHash); result.put("purifiedPaletteSHA256", purifiedHash);
        result.put("carrier", "early-color-finite-depth-shell"); result.put("mapping", "continuous-ellipsoid-stereographic");
        return result;
    }

    private record Palette(float[][] colors, float strength, float exposure) {
        static Palette read(byte[] bytes) {
            JsonObject data = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (!data.has("schema") || !data.get("schema").getAsString().equals("mindscape-palette/1")) throw new IllegalArgumentException("Unsupported mindscape palette schema");
            JsonObject palette = data.getAsJsonObject("palette");
            if (palette.size() != 7) throw new IllegalArgumentException("Mindscape palette must contain exactly seven keys");
            float[][] values = new float[5][3];
            for (int i = 0; i < COLOR_KEYS.length; i++) {
                var array = palette.getAsJsonArray(COLOR_KEYS[i]);
                if (array == null || array.size() != 3) throw new IllegalArgumentException("Invalid " + COLOR_KEYS[i]);
                for (int k = 0; k < 3; k++) values[i][k] = checked(array.get(k).getAsFloat(), 0, 16);
            }
            return new Palette(values, checked(palette.get("spiritStrength").getAsFloat(), 0, 4), checked(palette.get("exposure").getAsFloat(), .1f, 4));
        }
        static float checked(float value, float low, float high) { if (!Float.isFinite(value) || value < low || value > high) throw new IllegalArgumentException("Invalid mindscape palette value"); return value; }
    }

    private static final class Program {
        final int id; final Map<String, Integer> locations = new HashMap<>(); final float[] matrix = new float[16];
        Program(int id) { this.id = id; }
        void use() { GL20.glUseProgram(id); }
        int location(String name) { return locations.computeIfAbsent(name, n -> GL20.glGetUniformLocation(id, n)); }
        void i(String name, int value) { GL20.glUniform1i(location(name), value); }
        void f(String name, float value) { GL20.glUniform1f(location(name), value); }
        void v2(String name, float x, float y) { GL20.glUniform2f(location(name), x, y); }
        void v4(String name, float x, float y, float z, float w) { GL20.glUniform4f(location(name), x, y, z, w); }
        void v(String name, Vector3f value) { v(name, value.x, value.y, value.z); }
        void v(String name, float x, float y, float z) { GL20.glUniform3f(location(name), x, y, z); }
        void m(String name, Matrix4f value) { GL20.glUniformMatrix4fv(location(name), false, value.get(matrix)); }
    }

    /** Raw GL is restored without modifying Minecraft's cached RenderSystem state. */
    private static final class State implements AutoCloseable {
        final int program = GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM), vertexArray = GL11.glGetInteger(GL30.GL_VERTEX_ARRAY_BINDING);
        final int drawFramebuffer = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING), readFramebuffer = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        final int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE), unpackBuffer = GL11.glGetInteger(GL21.GL_PIXEL_UNPACK_BUFFER_BINDING);
        final boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        final boolean scissor = GL11.glIsEnabled(GL11.GL_SCISSOR_TEST), srgb = GL11.glIsEnabled(GL30.GL_FRAMEBUFFER_SRGB), depthMask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        final boolean stencil = GL11.glIsEnabled(GL11.GL_STENCIL_TEST);
        final int depthFunction = GL11.glGetInteger(GL11.GL_DEPTH_FUNC);
        final int[] viewport = new int[4], drawBuffers = new int[GL11.glGetInteger(GL20.GL_MAX_DRAW_BUFFERS)];
        final ByteBuffer[] colorMasks = new ByteBuffer[drawBuffers.length];
        final boolean[] indexedBlend = new boolean[drawBuffers.length];
        final int texture, sampler;
        State() {
            GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
            for (int i = 0; i < drawBuffers.length; i++) {
                drawBuffers[i] = GL11.glGetInteger(GL20.GL_DRAW_BUFFER0 + i);
                colorMasks[i] = BufferUtils.createByteBuffer(4);
                GL30.glGetBooleani_v(GL11.GL_COLOR_WRITEMASK, i, colorMasks[i]);
                indexedBlend[i] = GL30.glIsEnabledi(GL11.GL_BLEND, i);
            }
            GL13.glActiveTexture(GL13.GL_TEXTURE0); texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D); sampler = GL11.glGetInteger(GL33.GL_SAMPLER_BINDING);
        }
        public void close() {
            GL20.glUseProgram(program); GL30.glBindVertexArray(vertexArray); GL15.glBindBuffer(GL21.GL_PIXEL_UNPACK_BUFFER, unpackBuffer);
            GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, drawFramebuffer); GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, readFramebuffer);
            IntBuffer buffers = BufferUtils.createIntBuffer(drawBuffers.length); buffers.put(drawBuffers).flip(); GL20.glDrawBuffers(buffers);
            GL11.glViewport(viewport[0], viewport[1], viewport[2], viewport[3]);
            GL13.glActiveTexture(GL13.GL_TEXTURE0); GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture); GL33.glBindSampler(0, sampler); GL13.glActiveTexture(activeTexture);
            GL11.glDepthFunc(depthFunction); GL11.glDepthMask(depthMask);
            flag(GL11.GL_CULL_FACE, cull); flag(GL11.GL_DEPTH_TEST, depth); flag(GL11.GL_SCISSOR_TEST, scissor); flag(GL30.GL_FRAMEBUFFER_SRGB, srgb); flag(GL11.GL_STENCIL_TEST, stencil);
            for (int i = 0; i < drawBuffers.length; i++) {
                ByteBuffer mask = colorMasks[i];
                GL30.glColorMaski(i, mask.get(0) != 0, mask.get(1) != 0, mask.get(2) != 0, mask.get(3) != 0);
                if (indexedBlend[i]) GL30.glEnablei(GL11.GL_BLEND, i); else GL30.glDisablei(GL11.GL_BLEND, i);
            }
            lastRestored = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING) == drawFramebuffer
                    && GL11.glGetInteger(GL20.GL_CURRENT_PROGRAM) == program && GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE) == activeTexture
                    && GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK) == depthMask;
        }
        private void flag(int name, boolean value) { if (value) GL11.glEnable(name); else GL11.glDisable(name); }
    }

    private MindscapeBackgroundRenderer() {}
}
