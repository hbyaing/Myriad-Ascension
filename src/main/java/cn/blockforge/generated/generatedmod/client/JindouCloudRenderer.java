package cn.blockforge.generated.generatedmod.client;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.JindouCloudConfig;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = GeneratedMod.MOD_ID, value = Dist.CLIENT)
public final class JindouCloudRenderer {
    private static final MultiBufferSource.BufferSource BUFFERS = MultiBufferSource.immediate(new BufferBuilder(256));
    private static ClientLevel world;
    private static Matrix4f capturedWorldToClip;
    private static Vec3 capturedCamera;
    private static int depthTexture = -1, depthWidth, depthHeight;
    private static int trailTexture = -1;
    private static final int MAX_SEGMENTS = 32;
    private static final float[] TRAIL_UPLOAD = new float[MAX_SEGMENTS * 20];
    private static final BufferBuilder POST_VERTICES = new BufferBuilder(256);
    private static com.mojang.blaze3d.vertex.VertexBuffer postBuffer;
    private static boolean warned;
    private static boolean postPending;
    private static float postPartial;
    public static int renderedVolumes;
    public static String renderTrace="";
    private static final boolean DIAGNOSTIC=Boolean.getBoolean("wuxingxiantu.cloudOnlyCapture");
    /** Isolated-client diagnostics; no maps or history walks are added to normal frames. */
    public static java.util.Map<String,Object> motionSnapshot=java.util.Map.of();

    private JindouCloudRenderer() {}

    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void startFrame(TickEvent.RenderTickEvent event) {
        if(event.phase==TickEvent.Phase.END){post();return;}
        if (event.phase == TickEvent.Phase.START){capturedWorldToClip = null;renderedVolumes=0;renderTrace="";postPending=false;if(DIAGNOSTIC)motionSnapshot=java.util.Map.of();}
    }
    @SubscribeEvent public static void gui(net.minecraftforge.client.event.RenderGuiEvent.Pre e){if(postPending){e.getGuiGraphics().flush();post();}}
    private static void post(){
        if(!postPending||capturedWorldToClip==null)return;postPending=false;
        var matrix=capturedWorldToClip;capturedWorldToClip=null;
        try(var state=new cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer.State()){
            Minecraft.getInstance().getMainRenderTarget().bindWrite(true);
            renderVolumes(matrix,capturedCamera,postPartial,true);
        }
    }

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) resetWorld(Minecraft.getInstance().level);
    }

    private static void resetWorld(ClientLevel current) {
        if (world == current) return;
        world = current;
        capturedWorldToClip = null;
        if (depthTexture != -1) RenderSystem.deleteTexture(depthTexture);
        if (trailTexture != -1) RenderSystem.deleteTexture(trailTexture);
        if(postBuffer!=null){postBuffer.close();postBuffer=null;}
        depthTexture = -1;
        trailTexture = -1;
        depthWidth = depthHeight = 0;
        warned = false;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void render(RenderLevelStageEvent event) {
        if(DIAGNOSTIC)renderTrace+=event.getStage()+";";
        Minecraft client = Minecraft.getInstance();
        resetWorld(client.level);
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            // Forge 47.4 AFTER_LEVEL supplies a projection PoseStack, not the world view.
            capturedWorldToClip = new Matrix4f(event.getProjectionMatrix())
                    .mul(event.getPoseStack().last().pose());
            capturedCamera = event.getCamera().getPosition();
            if(TrailShaderBridge.shaderPackDetected())TrailShaderBridge.captureDepth();
            return;
        }
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL || capturedWorldToClip == null) return;
        if(TrailShaderBridge.shaderPackDetected()){postPending=true;postPartial=event.getPartialTick();return;}
        Matrix4f worldToClip = capturedWorldToClip;
        Vec3 camera = capturedCamera;
        capturedWorldToClip = null;
        renderVolumes(worldToClip,camera,event.getPartialTick(),false);
    }
    private static void renderVolumes(Matrix4f worldToClip,Vec3 camera,float partial,boolean post){
        var client=Minecraft.getInstance();
        if (world == null || client.player == null || JindouCloudShader.shader == null) return;
        List<Volume> volumes = new ArrayList<>();
        if(DIAGNOSTIC)renderTrace+="players="+world.players().size()+",fp="+client.options.getCameraType().isFirstPerson()+";";
        for (Player player : world.players()) {
            if (!JindouCloudRenderState.isActive(player)) continue;
            if (JindouCloudConfig.HIDE_FIRST_PERSON.get() && client.options.getCameraType().isFirstPerson()
                    && player == client.getCameraEntity()) continue;
            // Use the camera's Entity.getPosition interpolation, not renderer-only
            // xOld/yOld/zOld snapshots which can diverge during rapid local flight.
            Vec3 feet = player.getPosition(partial);
            // Vanilla resets interpolation on actual teleports. Do not jump the
            // body to the next tick's endpoint while its camera is still interpolating.
            double cloudY = JindouCloudConfig.Y_OFFSET.get() + JindouCloudConfig.FOOT_ENVELOPMENT.get();
            double baseHeight = JindouCloudConfig.RADIUS_Y.get() * JindouCloudConfig.VERTICAL_FULLNESS.get();
            cloudY -= baseHeight * 0.30;
            Vec3 center = feet.add(0, cloudY, 0);
            if(center.distanceToSqr(camera)>=96*96)continue;
            float motion = JindouCloudTrailCache.motion(player, partial);
            Vec3 heading = JindouCloudTrailCache.heading(player, partial);
            Matrix3f cloudBasis = basis(heading);
            Vec3 radii = new Vec3(JindouCloudConfig.RADIUS_X.get(),
                    baseHeight * 1.50,
                    JindouCloudConfig.RADIUS_Z.get());
            List<Segment> segments = new ArrayList<>();
            List<JindouCloudTrailCache.Point> history = JindouCloudTrailCache.points(player, feet, partial);
            double now = world.getGameTime() - 1 + (double) partial;
            int first = Math.max(0, history.size() - MAX_SEGMENTS);
            // Midpoint joins give adjacent quadratic curves identical tangents. GLSL evaluates them.
            // Only the live end changes; older triples retain their original world-space controls.
            for (int i = history.size() - 1; i >= first; i--) {
                var c = history.get(i);
                var a = history.get(Math.max(0, i - 1));
                var b = history.get(Math.min(history.size() - 1, i + 1));
                if (a.position().distanceToSqr(b.position()) < 0.00000001
                        && a.position().distanceToSqr(c.position()) < 0.00000001) continue;
                float lifeA = (a.lifetime() + c.lifetime()) * 0.5F;
                float lifeB = (b.lifetime() + c.lifetime()) * 0.5F;
                if (now - (a.time() + c.time()) * 0.5 >= lifeA
                        && now - (b.time() + c.time()) * 0.5 >= lifeB) continue;
                segments.add(new Segment(a.position().lerp(c.position(), 0.5).add(0, cloudY, 0),
                        b.position().lerp(c.position(), 0.5).add(0, cloudY, 0), c.position().add(0, cloudY, 0),
                        (float) Math.max(0, now - (a.time() + c.time()) * 0.5),
                        (float) Math.max(0, now - (b.time() + c.time()) * 0.5),
                        (float) ((a.velocity().length() + c.velocity().length()) * 0.5),
                        (float) ((b.velocity().length() + c.velocity().length()) * 0.5),
                        (a.width() + c.width()) * 0.5F, (b.width() + c.width()) * 0.5F,
                        (a.density() + c.density()) * 0.5F, (b.density() + c.density()) * 0.5F, lifeA, lifeB));
            }
            Volume volume = new Volume(player, center, radii, cloudBasis, segments, motion);
            if(DIAGNOSTIC&&player==client.player){
                var snapshot=new java.util.LinkedHashMap<String,Object>(JindouCloudTrailCache.diagnostics(player,feet,partial));
                var expectedFeet=new Vec3(Mth.lerp((double)partial,player.xo,player.getX()),
                        Mth.lerp((double)partial,player.yo,player.getY()),Mth.lerp((double)partial,player.zo,player.getZ()));
                snapshot.put("feet",java.util.List.of(feet.x,feet.y,feet.z));
                snapshot.put("center",java.util.List.of(center.x,center.y,center.z));
                snapshot.put("partial",partial);snapshot.put("anchorError",feet.distanceTo(expectedFeet));
                snapshot.put("renderOriginX",Math.floor(camera.x/256)*256);
                snapshot.put("renderOriginZ",Math.floor(camera.z/256)*256);
                snapshot.put("segments",segments.size());motionSnapshot=snapshot;
            }
            // AFTER_LEVEL may carry a projection-stage frustum; the retained world
            // matrix below performs conservative screen bounds instead.
            if (center.distanceToSqr(camera) < 96 * 96) volumes.add(volume);
        }
        if (volumes.isEmpty()){if(DIAGNOSTIC)renderTrace+="no-visible-volume;";return;}
        volumes.sort(Comparator.comparingDouble((Volume v) -> v.center().distanceToSqr(camera)).reversed());
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        int sceneDepth=post?TrailShaderBridge.capturedDepthTexture():-1;
        if (viewport[2] <= 0 || viewport[3] <= 0 || (post?sceneDepth<0:!captureDepth(viewport))){if(DIAGNOSTIC)renderTrace+="no-depth;";return;}
        Vec3 renderOrigin = new Vec3(Math.floor(camera.x / 256) * 256,
                Math.floor(camera.y / 256) * 256, Math.floor(camera.z / 256) * 256);
        Vec3 cameraPosition = camera.subtract(renderOrigin);

        boolean blend = GL11.glIsEnabled(GL11.GL_BLEND), depth = GL11.glIsEnabled(GL11.GL_DEPTH_TEST);
        boolean cull = GL11.glIsEnabled(GL11.GL_CULL_FACE), mask = GL11.glGetBoolean(GL11.GL_DEPTH_WRITEMASK);
        int depthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC), texture = RenderSystem.getShaderTexture(0);
        int srcRgb = GL11.glGetInteger(GL14.GL_BLEND_SRC_RGB), dstRgb = GL11.glGetInteger(GL14.GL_BLEND_DST_RGB);
        int srcAlpha = GL11.glGetInteger(GL14.GL_BLEND_SRC_ALPHA), dstAlpha = GL11.glGetInteger(GL14.GL_BLEND_DST_ALPHA);
        int activeTexture = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        var previousShader = RenderSystem.getShader();
        try (var saved=new cn.blockforge.generated.generatedmod.client.realmflight.MeteorBloomRenderer.State()) {
            var shader = JindouCloudShader.shader;
            shader.setSampler("SceneDepth", post?sceneDepth:depthTexture);
            shader.safeGetUniform("ClipToRelativeWorld").set(new Matrix4f(worldToClip).invert());
            shader.safeGetUniform("RelativeWorldToClip").set(worldToClip);
            shader.safeGetUniform("CameraPos").set((float) cameraPosition.x, (float) cameraPosition.y, (float) cameraPosition.z);
            shader.safeGetUniform("Viewport").set((float) viewport[0], (float) viewport[1], (float) viewport[2], (float) viewport[3]);
            shader.safeGetUniform("DebugVolume").set(JindouCloudConfig.DEBUG_VOLUME.get() ? 1 : 0);
            shader.safeGetUniform("Extinction").set(JindouCloudConfig.EXTINCTION.get().floatValue());
            shader.safeGetUniform("AmbientStrength").set(JindouCloudConfig.AMBIENT.get().floatValue());
            shader.safeGetUniform("SoftIntersection").set(JindouCloudConfig.SOFT_INTERSECTION.get().floatValue());
            float sunAngle = world.getSunAngle(partial), daylight = Math.max(0, Mth.cos(sunAngle));
            shader.safeGetUniform("LightDirection").set(-Mth.sin(sunAngle), Math.max(0.25F, daylight), 0.15F);
            shader.safeGetUniform("DayLight").set(daylight * (1 - world.getRainLevel(partial) * 0.55F));
            double time = (world.getGameTime() - 1 + (double) partial) / 20.0;
            for (Volume volume : volumes) {
                float[] rect = projectBounds(worldToClip, volume, camera);
                if (rect == null) continue;
                double distance = volume.center().distanceTo(camera);
                Vec3 p = volume.center().subtract(renderOrigin), r = volume.radii();
                shader.safeGetUniform("CloudCenter").set((float) p.x, (float) p.y, (float) p.z);
                shader.safeGetUniform("CloudRadii").set((float) r.x, (float) r.y, (float) r.z);
                shader.safeGetUniform("WorldToCloud").set(new Matrix3f(volume.toWorld()).transpose());
                shader.safeGetUniform("Steps").set(distance > 48 ? 20 : distance > 24 ? 24 : 32);
                shader.safeGetUniform("TrailCount").set(volume.segments().size());
                shader.safeGetUniform("MotionAmount").set(volume.motion());
                shader.safeGetUniform("TrailLifetime").set(JindouCloudConfig.TRAIL_LIFETIME.get().floatValue());
                uploadTrail(volume);
                shader.setSampler("TrailData", trailTexture);
                shader.safeGetUniform("DensityScale").set(JindouCloudConfig.DENSITY.get().floatValue());
                shader.safeGetUniform("Opacity").set((float) Mth.clamp((96 - distance) / 16, 0, 1));
                double seed = (volume.player().getUUID().getLeastSignificantBits() & 65535) / 65536.0;
                double flow = JindouCloudTrailCache.flow(volume.player(), partial);
                shader.safeGetUniform("NoiseOffset").set((float) seed, fract(seed * 0.37), fract(seed * 0.73));
                shader.safeGetUniform("FlowPhase").set((float) (fract(time / 11.0 + seed) * Math.PI * 2),
                        (float) (fract(time / 17.0 + seed * 0.37) * Math.PI * 2),
                        (float) (fract(time / 23.0 + seed * 0.73) * Math.PI * 2));
                shader.safeGetUniform("DetailOffset").set(fract(time * 0.025 + seed),
                        fract(time * 0.016 + seed * 0.37), fract(-time * 0.020 - flow + seed * 0.73));
                // All noise frequencies have an integer number of periods over this 10-unit domain.
                shader.safeGetUniform("HistoryNoiseOrigin").set(period10(volume.center().x * 0.38),
                        period10(volume.center().y * 0.38), period10(volume.center().z * 0.38));
                shader.safeGetUniform("HistoryDetailOffset").set(fract(time * 0.013 + seed),
                        fract(time * 0.007 + seed * 0.37), fract(-time * 0.010 + seed * 0.73));
                shader.safeGetUniform("SkyLight").set(world.getBrightness(LightLayer.SKY, volume.player().blockPosition()) / 15.0F);
                var raw=post?POST_VERTICES:null;
                if(post)raw.begin(com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS,com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION);
                VertexConsumer vertices = post?raw:BUFFERS.getBuffer(JindouCloudShader.VOLUME);
                vertices.vertex(rect[0], rect[1], 0).endVertex();
                vertices.vertex(rect[2], rect[1], 0).endVertex();
                vertices.vertex(rect[2], rect[3], 0).endVertex();
                vertices.vertex(rect[0], rect[3], 0).endVertex();
                if(post){
                    shader.setSampler("Sampler0",client.getTextureManager().getTexture(new net.minecraft.resources.ResourceLocation(GeneratedMod.MOD_ID,"textures/effect/jindou_worley.png")).getId());
                    RenderSystem.enableBlend();RenderSystem.defaultBlendFunc();RenderSystem.disableDepthTest();RenderSystem.depthMask(false);RenderSystem.disableCull();
                    if(postBuffer==null)postBuffer=new com.mojang.blaze3d.vertex.VertexBuffer(com.mojang.blaze3d.vertex.VertexBuffer.Usage.DYNAMIC);
                    try{postBuffer.bind();postBuffer.upload(raw.end());shader.apply();postBuffer.draw();shader.clear();}
                    finally{com.mojang.blaze3d.vertex.VertexBuffer.unbind();}
                }else BUFFERS.endBatch(JindouCloudShader.VOLUME);
                renderedVolumes++;
            }
        } finally {
            RenderSystem.setShaderTexture(0, texture);
            RenderSystem.setShader(() -> previousShader);
            RenderSystem.activeTexture(activeTexture);
            RenderSystem.depthMask(mask);
            RenderSystem.depthFunc(depthFunc);
            if (depth) RenderSystem.enableDepthTest(); else RenderSystem.disableDepthTest();
            if (cull) RenderSystem.enableCull(); else RenderSystem.disableCull();
            RenderSystem.blendFuncSeparate(srcRgb, dstRgb, srcAlpha, dstAlpha);
            if (blend) RenderSystem.enableBlend(); else RenderSystem.disableBlend();
        }
    }

    private static float fract(double value) { return (float) (value - Math.floor(value)); }
    private static float period10(double value) { return (float) (value - Math.floor(value / 10.0) * 10.0); }

    private static void uploadTrail(Volume volume) {
        // TrailCount ignores inactive rows; retain one upload array instead of
        // allocating 640 floats for every player on every rendered frame.
        float[] data = TRAIL_UPLOAD;
        Matrix3f toCloud = new Matrix3f(volume.toWorld()).transpose();
        for (int i = 0; i < volume.segments().size(); i++) {
            Segment s = volume.segments().get(i);
            Vec3 a = s.a().subtract(volume.center()), b = s.b().subtract(volume.center());
            Vector3f p = toCloud.transform(new Vector3f((float) a.x, (float) a.y, (float) a.z));
            Vector3f q = toCloud.transform(new Vector3f((float) b.x, (float) b.y, (float) b.z));
            Vec3 control = s.control().subtract(volume.center());
            Vector3f c = toCloud.transform(new Vector3f((float) control.x, (float) control.y, (float) control.z));
            int j = i * 20;
            data[j] = p.x; data[j + 1] = p.y; data[j + 2] = p.z; data[j + 3] = s.ageA();
            data[j + 4] = q.x; data[j + 5] = q.y; data[j + 6] = q.z; data[j + 7] = s.ageB();
            data[j + 8] = s.speedA(); data[j + 9] = s.speedB();
            data[j + 10] = s.lifeA(); data[j + 11] = s.lifeB();
            data[j + 12] = s.widthA(); data[j + 13] = s.widthB();
            data[j + 14] = s.densityA(); data[j + 15] = s.densityB();
            data[j + 16] = c.x; data[j + 17] = c.y; data[j + 18] = c.z;
        }
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int previous = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        try {
            if (trailTexture == -1) {
                trailTexture = GL11.glGenTextures();
                RenderSystem.bindTexture(trailTexture);
                GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_RGBA32F, 5, MAX_SEGMENTS, 0,
                        GL11.GL_RGBA, GL11.GL_FLOAT, data);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            } else {
                RenderSystem.bindTexture(trailTexture);
                GL11.glTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, 5, MAX_SEGMENTS, GL11.GL_RGBA, GL11.GL_FLOAT, data);
            }
        } finally {
            RenderSystem.bindTexture(previous);
            RenderSystem.activeTexture(active);
        }
    }

    private static Matrix3f basis(Vec3 direction) {
        Vector3f forward = new Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
        Vector3f reference = Math.abs(forward.y) > 0.95F ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f right = reference.cross(forward, new Vector3f()).normalize();
        Vector3f up = forward.cross(right, new Vector3f()).normalize();
        return new Matrix3f().setColumn(0, right).setColumn(1, up).setColumn(2, forward);
    }

    private static AABB bounds(Volume volume) {
        Vec3 half = halfBounds(volume), p = volume.center();
        AABB box = new AABB(p.subtract(half), p.add(half));
        Vector3f backward = volume.toWorld().transform(new Vector3f(0, 0,
                (float) (-volume.radii().z * 1.25 * 0.85 * volume.motion())));
        box = box.minmax(box.move(backward.x, backward.y, backward.z));
        for (Segment s : volume.segments()) {
            double width = Math.max(s.widthA(), s.widthB()) * 1.12;
            box = box.minmax(new AABB(s.a(), s.b()).minmax(new AABB(s.control(), s.control()))
                    .inflate(width, width * volume.radii().y / volume.radii().x, width));
        }
        return box;
    }

    private static Vec3 halfBounds(Volume volume) {
        Matrix3f m = volume.toWorld(); Vec3 r = volume.radii().multiply(1.15, 1.15, 1.25);
        return new Vec3(Math.abs(m.m00()) * r.x + Math.abs(m.m10()) * r.y + Math.abs(m.m20()) * r.z,
                Math.abs(m.m01()) * r.x + Math.abs(m.m11()) * r.y + Math.abs(m.m21()) * r.z,
                Math.abs(m.m02()) * r.x + Math.abs(m.m12()) * r.y + Math.abs(m.m22()) * r.z);
    }

    private static float[] projectBounds(Matrix4f matrix, Volume volume, Vec3 camera) {
        float left = 1, bottom = 1, right = -1, top = -1;
        AABB box = bounds(volume);
        Vec3 center = box.getCenter().subtract(camera);
        Vec3 half = new Vec3(box.getXsize() * 0.5, box.getYsize() * 0.5, box.getZsize() * 0.5);
        for (int i = 0; i < 8; i++) {
            Vector4f p = matrix.transform(new Vector4f((float) (center.x + ((i & 1) == 0 ? -half.x : half.x)),
                    (float) (center.y + ((i & 2) == 0 ? -half.y : half.y)),
                    (float) (center.z + ((i & 4) == 0 ? -half.z : half.z)), 1));
            if (p.w <= 0.01F) return new float[]{-1, -1, 1, 1};
            left = Math.min(left, p.x / p.w); right = Math.max(right, p.x / p.w);
            bottom = Math.min(bottom, p.y / p.w); top = Math.max(top, p.y / p.w);
        }
        left = Math.max(-1, left); bottom = Math.max(-1, bottom);
        right = Math.min(1, right); top = Math.min(1, top);
        return left < right && bottom < top ? new float[]{left, bottom, right, top} : null;
    }

    private static boolean captureDepth(int[] viewport) {
        int active = GL11.glGetInteger(GL13.GL_ACTIVE_TEXTURE);
        RenderSystem.activeTexture(GL13.GL_TEXTURE0);
        int texture = GL11.glGetInteger(GL11.GL_TEXTURE_BINDING_2D);
        int read = GL11.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING);
        int draw = GL11.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING);
        try {
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, draw);
            if (draw != 0 && GL30.glGetFramebufferAttachmentParameteri(GL30.GL_READ_FRAMEBUFFER,
                    GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE) == GL11.GL_NONE) return false;
            if (depthTexture == -1) depthTexture = GL11.glGenTextures();
            RenderSystem.bindTexture(depthTexture);
            if (depthWidth != viewport[2] || depthHeight != viewport[3]) {
                GL11.glCopyTexImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH_COMPONENT32F,
                        viewport[0], viewport[1], viewport[2], viewport[3], 0);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
                GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
                depthWidth = viewport[2]; depthHeight = viewport[3];
            } else GL11.glCopyTexSubImage2D(GL11.GL_TEXTURE_2D, 0, 0, 0, viewport[0], viewport[1], viewport[2], viewport[3]);
            int error = GL11.glGetError();
            if (error == GL11.GL_NO_ERROR) return true;
            depthWidth = depthHeight = 0;
            if (!warned) com.mojang.logging.LogUtils.getLogger().warn("Jindou cloud depth capture failed: GL {}", error);
            warned = true;
            return false;
        } finally {
            RenderSystem.bindTexture(texture);
            RenderSystem.activeTexture(active);
            GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, read);
        }
    }

    private record Segment(Vec3 a, Vec3 b, Vec3 control, float ageA, float ageB, float speedA, float speedB,
                           float widthA, float widthB, float densityA, float densityB, float lifeA, float lifeB) {}

    private record Volume(Player player, Vec3 center, Vec3 radii, Matrix3f toWorld, List<Segment> segments, float motion) {}
}
