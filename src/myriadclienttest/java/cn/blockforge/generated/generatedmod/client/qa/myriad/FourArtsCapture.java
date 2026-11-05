package cn.blockforge.generated.generatedmod.client.qa.myriad;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.Network;
import cn.blockforge.generated.generatedmod.arts.FourArtsVisuals;
import cn.blockforge.generated.generatedmod.client.arts.FourArtsEffects;
import cn.blockforge.generated.generatedmod.items.CultivationLogic;
import cn.blockforge.generated.generatedmod.network.packet.cultivation.ArtReleasePacket;
import com.google.gson.GsonBuilder;
import net.minecraft.client.Camera;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.AccessibilityOnboardingScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Mod.EventBusSubscriber(modid=GeneratedMod.MOD_ID,value=Dist.CLIENT)
public final class FourArtsCapture {
    private static final boolean ENABLED=Boolean.getBoolean("wuxingxiantu.fourArtsCapture");
    private static final Path OUT=Path.of(System.getProperty("wuxingxiantu.myriadCaptureOutput","captures"));
    private static final String[] CASES={"active-renewal","passive-renewal","active-spirit","passive-spirit",
            "active-wind","passive-slow-fall","active-blood-charge","active-blood-impact","passive-blood-impact",
            "active-purification","clean-purification","passive-purification","ice-flight","ice-homing","ice-blocked","flame-wave"};
    private static final String[] ARTS={FourArtsVisuals.RENEWAL,FourArtsVisuals.RENEWAL,FourArtsVisuals.GATHER,
            FourArtsVisuals.GATHER,FourArtsVisuals.LIGHT,FourArtsVisuals.LIGHT,FourArtsVisuals.BLOOD,
            FourArtsVisuals.BLOOD,FourArtsVisuals.BLOOD,
            FourArtsVisuals.PURIFICATION,FourArtsVisuals.PURIFICATION,FourArtsVisuals.PURIFICATION,
            FourArtsVisuals.ICE,FourArtsVisuals.ICE,FourArtsVisuals.ICE,FourArtsVisuals.FLAME};
    private static final int[] LIMIT={14,40,31,25,35,31,31,24,24,21,21,40,29,82,88,76};
    private static final int LOTUS_BASE=1<<9|1<<11|1<<12|1<<15;
    private static final int LOTUS_ACTIVE=1<<13|1<<14|1<<17;
    private static final int LOTUS_STAINS=1<<10|1<<16;
    private static final int ICE_FORMATION=1<<18|1<<19|1<<21|1<<22;
    private static final int ICE_FLIGHT=1<<18|1<<20|1<<22|1<<23|1<<24;
    private static final int FLAME_GATHER=1<<6|1<<28|1<<29;
    private static final int FLAME_TRAVEL=1<<25|1<<26|1<<27|1<<28|1<<30;
    private static final List<Map<String,Object>> RESULTS=new ArrayList<>();
    private static final long START=System.nanoTime();
    private static boolean opened,ready,prepared,finished;
    private static CompletableFuture<?> pending;
    private static int stage,tick,firstDrawTick=-1;
    private static long baseline;
    private static Field draws,materials,states;
    private static int dummyId;
    private static final List<Integer> iceTargets=new ArrayList<>();
    private static final List<Integer> flameTargets=new ArrayList<>();
    private static boolean formationCaptured;
    private static boolean flameGatherCaptured;
    private static boolean flameCrestCaptured;
    private static float iceDamage;
    private static Method cameraPosition;

    @SubscribeEvent
    public static void tick(TickEvent.ClientTickEvent event) {
        if(!ENABLED||finished||event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        try {
            Files.createDirectories(OUT);
            mc.setWindowActive(true);
            mc.options.pauseOnLostFocus=false;
            GLFW.glfwHideWindow(mc.getWindow().getWindow());
            if((System.nanoTime()-START)/1e9>360) throw new AssertionError("Timed out at stage "+stage);
            if(!opened&&mc.player!=null) throw new AssertionError("Expected isolated QA world");
            if(!opened&&mc.getOverlay()==null&&(mc.screen instanceof TitleScreen||mc.screen instanceof AccessibilityOnboardingScreen)) {
                opened=true;
                mc.getWindow().setWindowed(1280,800);
                mc.resizeDisplay();
                mc.options.renderDistance().set(4);
                mc.options.simulationDistance().set(5);
                mc.options.framerateLimit().set(40);
                mc.options.hideGui=true;
                mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                mc.getTutorial().setStep(net.minecraft.client.tutorial.TutorialSteps.NONE);
                var rules=new GameRules();
                rules.getRule(GameRules.RULE_DOMOBSPAWNING).set(false,null);
                rules.getRule(GameRules.RULE_DAYLIGHT).set(false,null);
                var saveRoot=mc.gameDirectory.toPath().resolve("saves");
                if(Files.isDirectory(saveRoot)) try(var worlds=Files.list(saveRoot)) {
                    var reuse=worlds.filter(path->path.getFileName().toString().startsWith("four-arts-")
                            && Files.isRegularFile(path.resolve("level.dat"))).findFirst();
                    if(reuse.isPresent()) {
                        mc.createWorldOpenFlows().loadLevel(new TitleScreen(),reuse.get().getFileName().toString());
                        return;
                    }
                }
                mc.createWorldOpenFlows().createFreshLevel("four-arts-"+System.currentTimeMillis(),
                        new LevelSettings("Four arts isolated QA",GameType.CREATIVE,false,Difficulty.NORMAL,true,rules,WorldDataConfiguration.DEFAULT),
                        new WorldOptions(7219,false,false),WorldPresets::createNormalWorldDimensions);
            }
            if(!opened||mc.player==null||mc.getOverlay()!=null) return;
            if(pending!=null) {
                if(!pending.isDone()) return;
                pending.join();
                pending=null;
            }
            if(!ready) {
                ready=true;
                Files.createDirectories(OUT.resolve("effects"));
                SenseEffectsCapture.shader();
                mc.mouseHandler.grabMouse();
                pending=mc.getSingleplayerServer().submit(()->{
                    var p=serverPlayer(mc);
                    var level=p.serverLevel();
                    level.setDayTime(12700);
                    for(int x=-14;x<=14;x++) for(int z=-14;z<=14;z++)
                        level.setBlockAndUpdate(new BlockPos(x,199,z),Blocks.SMOOTH_STONE.defaultBlockState());
                    for(var entity:level.getEntities(p,p.getBoundingBox().move(.5-p.getX(),200-p.getY(),.5-p.getZ()).inflate(16),
                            candidate->candidate.getTags().contains("four_arts_qa_dummy") || candidate instanceof net.minecraft.world.entity.item.ItemEntity)) {
                        entity.discard();
                    }
                    p.connection.teleport(.5,200,.5,0,0);
                });
                return;
            }
            if(!prepared) {
                prepared=true;tick=0;firstDrawTick=-1;baseline=drawCount();
                mc.setScreen(null);
                mc.options.keyUp.setDown(false);
                if(stage>=9) mc.options.fov().set(58);
                int mode=stage;
                pending=mc.getSingleplayerServer().submit(()->configure(serverPlayer(mc),mode));
                return;
            }
            tick++;
            if((tick==5&&(stage==0||stage==2||stage==4||stage==6||stage==7||stage==9||stage==10||stage==12))
                    ||(tick==50&&stage>=13))
                Network.CHANNEL.sendToServer(new ArtReleasePacket(0));
            if(stage==13 && tick==68) pending=mc.getSingleplayerServer().submit(()->{
                var target=serverPlayer(mc).serverLevel().getEntity(iceTargets.get(1));
                target.teleportTo(target.getX()+1.15,target.getY(),target.getZ()+.42);
            });
            if(stage==15 && tick==57) pending=mc.getSingleplayerServer().submit(()->checkFlameTargets(serverPlayer(mc),false));
            if(stage==4&&tick==9) mc.options.keyUp.setDown(true);
            if((stage==7||stage==8)&&tick==20) pending=mc.getSingleplayerServer().submit(()->hit(serverPlayer(mc)));
            if(tick>170) throw new AssertionError("No acceptable frame for "+CASES[stage]+" draws="+(drawCount()-baseline));
        } catch(Throwable error) { fail(mc,error); }
    }

    private static ServerPlayer serverPlayer(Minecraft mc) {
        return mc.getSingleplayerServer().getPlayerList().getPlayer(mc.player.getUUID());
    }

    private static void configure(ServerPlayer player,int mode) {
        if(player.serverLevel().getEntity(dummyId) instanceof net.minecraft.world.entity.LivingEntity old) old.discard();
        var data=GeneratedMod.data(player);
        data.wuxingxiantu$setRealm(7);
        data.wuxingxiantu$setCreationBlessing(true);
        data.wuxingxiantu$setAura(mode==3?10:CultivationLogic.maxAura(data));
        data.wuxingxiantu$learnArt(ARTS[mode]);
        data.wuxingxiantu$equipArt(0,ARTS[mode]);
        data.wuxingxiantu$clearPassiveArt(0);
        player.removeAllEffects();
        for(String art:ARTS) player.getPersistentData().remove("FourArtsVisualUntil."+art);
        if(mode==1||mode==3||mode==5||mode==8||mode==11) data.wuxingxiantu$equipPassiveArt(0,ARTS[mode]);
        if(mode<=1||mode>=9) player.setHealth(Math.max(1,player.getMaxHealth()-9));
        else player.setHealth(player.getMaxHealth());
        if(mode>=9 && mode<=11) {
            player.serverLevel().setDayTime(3600);
            for(var golem:player.serverLevel().getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class,
                    player.getBoundingBox().inflate(6))) golem.discard();
            if(mode!=10) player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN,1000,0,false,false));
            player.addEffect(new net.minecraft.world.effect.MobEffectInstance(MobEffects.DAMAGE_RESISTANCE,1000,0,false,false));
        }
        player.setItemInHand(InteractionHand.MAIN_HAND,mode>=6&&mode<=8?new ItemStack(Items.IRON_SWORD):ItemStack.EMPTY);
        player.connection.teleport(.5,mode==5?220:200,.5,0,0);
        if(mode>=12) configureIce(player,mode);
        if(mode==15) {
            flameTargets.add(spawnTarget(player,1.75,2.8));
            flameTargets.add(spawnTarget(player,-1.5,5.7));
            flameTargets.add(spawnTarget(player,-3.5,1.7));
            flameTargets.add(spawnTarget(player,.5,7.2));
        }
        GeneratedMod.sync(player);
    }

    private static void configureIce(ServerPlayer player,int mode) {
        var level=player.serverLevel();
        for(int id:iceTargets) if(level.getEntity(id)!=null) level.getEntity(id).discard();
        iceTargets.clear();
        for(int x=-4;x<=4;x++) for(int y=200;y<=204;y++)
            level.setBlockAndUpdate(new BlockPos(x,y,4),mode==14?Blocks.STONE_BRICKS.defaultBlockState():Blocks.AIR.defaultBlockState());
        level.setDayTime(4200);
        var data=GeneratedMod.data(player);
        iceDamage=cn.blockforge.generated.generatedmod.ArtCatalog.damageFor(FourArtsVisuals.ICE,
                player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE),
                data.wuxingxiantu$getArtMastery(FourArtsVisuals.ICE),CultivationLogic.maxAura(data));
        if(mode==13) {
            iceTargets.add(spawnTarget(player,-4.5,-3.5));
            iceTargets.add(spawnTarget(player,2.6,-5.3));
        }
        if(mode==14) iceTargets.add(spawnTarget(player,.5,6.5));
    }

    private static int spawnTarget(ServerPlayer player,double x,double z) {
        var target=EntityType.HUSK.create(player.serverLevel());
        target.setNoAi(true);
        target.addTag("four_arts_qa_dummy");
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(target.getMaxHealth());
        target.setPos(x,200,z);
        player.serverLevel().addFreshEntity(target);
        return target.getId();
    }

    private static void hit(ServerPlayer player) {
        var dummy=EntityType.IRON_GOLEM.create(player.serverLevel());
        dummy.setNoAi(true);
        dummy.addTag("four_arts_qa_dummy");
        dummy.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        dummy.setHealth(dummy.getMaxHealth());
        dummy.setPos(player.position().add(-.55,0,1.65));
        player.serverLevel().addFreshEntity(dummy);
        player.swing(InteractionHand.MAIN_HAND,true);
        player.attack(dummy);
        if(dummy.getHealth()>=dummy.getMaxHealth()) throw new AssertionError("Real melee attack did not land");
        dummyId=dummy.getId();
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void camera(ViewportEvent.ComputeCameraAngles event) {
        if(!ENABLED||finished||!prepared||Minecraft.getInstance().player==null) return;
        try {
            if(cameraPosition==null) {
                cameraPosition=Camera.class.getDeclaredMethod("setPosition",Vec3.class);
                cameraPosition.setAccessible(true);
            }
            var p=Minecraft.getInstance().player;
            Vec3 target=p.position().add(0,stage>=9?1.48:1.03,stage==7||stage==8?.40:0);
            Vec3 origin=stage>=9?target.add(1.85,1.55,4.14):target.add(2.20,1.12,3.70);
            if(stage>=12) {
                target=p.position().add(-.38,1.02,stage==13?-2.6:2.3);
                origin=target.add(stage==13?-7.9:-8.5,3.25,stage==13?-3.4:-2.2);
            }
            if(stage==15) {
                target=p.position().add(-.15,.82,2.7);
                origin=target.add(-8.75,5.75,1.55);
            }
            Vec3 toward=target.subtract(origin);
            cameraPosition.invoke(event.getCamera(),origin);
            event.setYaw((float)Math.toDegrees(Math.atan2(-toward.x,toward.z)));
            event.setPitch((float)-Math.toDegrees(Math.atan2(toward.y,toward.horizontalDistance())));
            event.setRoll(0);
        } catch(Throwable error) { fail(Minecraft.getInstance(),error); }
    }

    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void frame(TickEvent.RenderTickEvent event) {
        if(!ENABLED||finished||!prepared||pending!=null||event.phase!=TickEvent.Phase.END) return;
        var mc=Minecraft.getInstance();
        try {
            long count=drawCount();
            int wanted=switch(stage) {
                case 0,1 -> 1<<1|1<<9;
                case 2,3 -> 1<<6;
                case 4 -> 1<<5;
                case 5 -> 1<<8;
                case 9 -> LOTUS_BASE|LOTUS_ACTIVE|LOTUS_STAINS;
                case 10 -> LOTUS_BASE|LOTUS_ACTIVE;
                case 11 -> LOTUS_BASE;
                case 12 -> ICE_FLIGHT;
                case 13 -> 1<<18|1<<21|1<<22|1<<23|1<<6;
                case 14 -> 1<<18|1<<21|1<<22|1<<23;
                case 15 -> FLAME_TRAVEL;
                default -> 1<<2;
            };
            if(stage==12 && !formationCaptured && tick>=15 && tick<=17 && count>baseline
                    && (drawMaterials()&ICE_FORMATION)==ICE_FORMATION) {
                try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    screenshot.writeToFile(OUT.resolve("ice-formation.png"));
                }
                formationCaptured=true;
            }
            if(stage==15 && !flameGatherCaptured && tick>=55 && tick<=59 && count>baseline
                    && (drawMaterials()&FLAME_GATHER)==FLAME_GATHER) {
                try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    screenshot.writeToFile(OUT.resolve("flame-gather.png"));
                }
                flameGatherCaptured=true;
            }
            if(stage==15 && !flameCrestCaptured && tick>=69 && tick<=72 && count>baseline
                    && (drawMaterials()&FLAME_TRAVEL)==FLAME_TRAVEL) {
                try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                    screenshot.writeToFile(OUT.resolve("flame-crest.png"));
                }
                flameCrestCaptured=true;
            }
            boolean actual=count>baseline && (drawMaterials()&wanted)==wanted && matchesMode(mc);
            if(actual && firstDrawTick<0) firstDrawTick=tick;
            int capture=stage==1?firstDrawTick+6:stage==3?firstDrawTick+9:stage==5?firstDrawTick+5
                    :stage==11?firstDrawTick+3:LIMIT[stage];
            if(!actual) return;
            if(firstDrawTick<0 || tick<capture) return;
            PointerSafety.verify();
            int glError=GL11.glGetError();
            if(glError!=0) throw new AssertionError("GL error "+glError+" during "+CASES[stage]);
            if(stage==2 && (mc.player.getEffect(MobEffects.REGENERATION)==null || mc.player.getEffect(MobEffects.REGENERATION).isVisible()))
                throw new AssertionError("Legacy regeneration particles still enabled");
            if(stage==4 && (mc.player.getEffect(MobEffects.MOVEMENT_SPEED)==null || mc.player.getEffect(MobEffects.MOVEMENT_SPEED).isVisible()))
                throw new AssertionError("Legacy movement particles still enabled");
            if(stage==6 && (mc.player.getEffect(MobEffects.DAMAGE_BOOST)==null || mc.player.getEffect(MobEffects.DAMAGE_BOOST).isVisible()))
                throw new AssertionError("Legacy damage boost particles still enabled");
            if(stage>=9 && stage<=11 && (!mc.player.hasEffect(MobEffects.DAMAGE_RESISTANCE)
                    || mc.player.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)!=(stage==11)
                    || mc.player.getHealth()<=mc.player.getMaxHealth()-9
                    || (stage!=9 && (drawMaterials()&LOTUS_STAINS)!=0)
                    || (stage==11 && (drawMaterials()&LOTUS_ACTIVE)!=0)))
                throw new AssertionError("Purification visual did not match the applied heal and cleanse");
            if(stage==12 && !formationCaptured) throw new AssertionError("Missing ice formation frame");
            if(stage==13 || stage==14) {
                boolean shouldHit=stage==13;
                mc.getSingleplayerServer().submit(()->checkIceTargets(serverPlayer(mc),shouldHit)).join();
            }
            if(stage==15) {
                if(!flameGatherCaptured || !flameCrestCaptured) throw new AssertionError("Missing flame gathering or crest frame");
                mc.getSingleplayerServer().submit(()->checkFlameTargets(serverPlayer(mc),true)).join();
                for(int id:flameTargets.subList(0,2)) {
                    var target=mc.level.getEntity(id);
                    if(target==null || !target.isOnFire()) throw new AssertionError("Vanilla fire flag did not reach client");
                }
            }
            try(var screenshot=Screenshot.takeScreenshot(mc.getMainRenderTarget())) {
                screenshot.writeToFile(OUT.resolve(CASES[stage]+".png"));
            }
            RESULTS.add(Map.of("case",CASES[stage],"drawnFrames",count-baseline,"glError",glError,"firstDraw",firstDrawTick,"screenshotTick",tick));
            if(++stage==CASES.length) {
                Files.writeString(OUT.resolve("result.json"),new GsonBuilder().setPrettyPrinting().create()
                        .toJson(Map.of("passed",true,"cases",RESULTS,"pointer",PointerSafety.snapshot())));
                finished=true;mc.options.keyUp.setDown(false);mc.stop();
            } else prepared=false;
        } catch(Throwable error) { fail(mc,error); }
    }

    private static long drawCount() throws Exception {
        if(draws==null) {
            draws=FourArtsEffects.class.getDeclaredField("drawnFrames");
            draws.setAccessible(true);
        }
        return draws.getLong(null);
    }

    private static int drawMaterials() throws Exception {
        if(materials==null) {
            materials=FourArtsEffects.class.getDeclaredField("drawnMaterials");
            materials.setAccessible(true);
        }
        return materials.getInt(null);
    }

    private static boolean matchesMode(Minecraft mc) throws Exception {
        if(stage>=12) return tick>=(stage==12?10:64);
        if(states==null) {
            states=FourArtsEffects.class.getDeclaredField("STATES");
            states.setAccessible(true);
        }
        Object state=((Map<?,?>)states.get(null)).get(mc.player);
        if(state==null) return false;
        int bits=(int)field(state,"bits");
        return switch(stage) {
            case 0,1 -> (boolean)field(state,"passiveRenewal")==(stage==1);
            case 2 -> (bits&FourArtsVisuals.SPIRIT)!=0;
            case 3 -> (bits&FourArtsVisuals.SPIRIT)==0 && mc.level.getGameTime()-(long)field(state,"gatherPulse")<20;
            case 4 -> (bits&FourArtsVisuals.WIND)!=0;
            case 5 -> (bits&FourArtsVisuals.PASSIVE_WIND)!=0;
            case 6 -> (bits&FourArtsVisuals.BLOOD_AURA)!=0;
            case 7,8 -> {
                var hit=((java.util.ArrayDeque<?>)field(state,"hits")).peekLast();
                yield hit!=null && (boolean)field(hit,"passive")==(stage==8);
            }
            case 9,10,11 -> mc.level.getGameTime()-(long)field(state,"purification")<32
                    && (boolean)field(state,"passivePurification")==(stage==11)
                    && (int)field(state,"removedEffects")==(stage==9?1:0);
            default -> false;
        };
    }

    private static void checkIceTargets(ServerPlayer player,boolean shouldHit) {
        for(int id:iceTargets) {
            if(!(player.serverLevel().getEntity(id) instanceof net.minecraft.world.entity.LivingEntity target))
                throw new AssertionError("Missing ice target "+id);
            float lost=target.getMaxHealth()-target.getHealth();
            var slow=target.getEffect(MobEffects.MOVEMENT_SLOWDOWN);
            if(shouldHit ? Math.abs(lost-iceDamage)>.05 || slow==null || slow.getAmplifier()!=2 : lost!=0 || slow!=null)
                throw new AssertionError("Wrong ice impact for "+target.position()+", damage="+lost+", expected="+iceDamage+", slow="+slow);
        }
    }

    private static void checkFlameTargets(ServerPlayer player,boolean afterWave) {
        int nearTicks=0;
        for(int i=0;i<flameTargets.size();i++) {
            if(!(player.serverLevel().getEntity(flameTargets.get(i)) instanceof net.minecraft.world.entity.LivingEntity target))
                throw new AssertionError("Missing flame target "+i);
            boolean hit=afterWave && i<2;
            int remaining=target.getRemainingFireTicks();
            if(hit ? remaining<55 || remaining>80 || target.getHealth()>=target.getMaxHealth()
                    : remaining>0 || target.getHealth()!=target.getMaxHealth())
                throw new AssertionError("Wrong vanilla flame result for target "+i+" fire="+remaining+" health="+target.getHealth());
            if(i==0) nearTicks=remaining;
            if(i==1 && hit && remaining<nearTicks+4)
                throw new AssertionError("The distant target should ignite later than the near target");
        }
    }

    private static Object field(Object target,String name) throws Exception {
        Field field=target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    private static void fail(Minecraft mc,Throwable error) {
        try { Files.writeString(OUT.resolve("failure.txt"),error.toString()); } catch(Exception ignored) { }
        error.printStackTrace();
        finished=true;
        mc.options.keyUp.setDown(false);
        mc.stop();
    }
}
