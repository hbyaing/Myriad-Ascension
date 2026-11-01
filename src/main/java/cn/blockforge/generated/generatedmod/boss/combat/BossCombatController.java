package cn.blockforge.generated.generatedmod.boss.combat;

import cn.blockforge.generated.generatedmod.GeneratedMod;
import cn.blockforge.generated.generatedmod.Network;
import cn.blockforge.generated.generatedmod.boss.VisualBossEntity;
import cn.blockforge.generated.generatedmod.boss.BossVisualRegistry;
import cn.blockforge.generated.generatedmod.combat.HeartSwordCombatProfile;
import cn.blockforge.generated.generatedmod.mindscape.MindscapeArenas;
import cn.blockforge.generated.generatedmod.network.packet.boss.BossCombatFxPacket;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** One server-owned encounter. No global timers, dimension-wide queries or unbounded projectiles. */
public final class BossCombatController {
    public static final String PARENT="BossMechanicParent",SESSION="MindscapeSession";
    public static final int DANGER=0xF46A48,SAFE=0x48DBC4;
    public final Mob boss;
    public final BossCombatCatalog.Profile profile;
    public final BlockPos center;
    public final UUID ownerId,session;
    private final ServerLevel level;
    private final boolean sessionBound;
    private final boolean playerCast;
    private final Map<UUID,Integer> victimTicks=new HashMap<>();
    private final Map<String,Float> victimBudgets=new HashMap<>();
    private final float playerHealth;
    private final List<Hazard> hazards=new ArrayList<>();
    private final List<Shot> shots=new ArrayList<>();
    private final List<Scheduled> actions=new ArrayList<>();
    private final List<Node> nodes=new ArrayList<>();
    private final List<UUID> echoes=new ArrayList<>();
    private final Map<String,Integer> cooldowns=new HashMap<>();
    private final Map<Integer,Float> damageBudgets=new HashMap<>();
    private final Set<String> introduced=new HashSet<>();
    private final ArrayDeque<Vec3> footsteps=new ArrayDeque<>();
    private final Map<Integer,Integer> anchorCooldowns=new HashMap<>();
    private BossCombatCatalog.Skill skill;
    private int clock,readyAt=100,finishAt,phase=1,castId,effectId,lastIndex=-1,exposedUntil,guardUntil,anchorCount;
    private int lastDamageTick=-100,pressure,basicApproachUntil;
    private boolean closed,interrupted,engaged,basicDue,basicAttack;
    private Vec3 castOrigin,castTarget;
    private record Scheduled(int at,Runnable action){}

    public BossCombatController(VisualBossEntity boss,ServerPlayer owner,BlockPos center,UUID session) {
        this(boss,owner,center,session,true);
    }
    public BossCombatController(VisualBossEntity boss,ServerPlayer owner,BlockPos center,UUID session,boolean sessionBound) {
        this(boss,owner,center,session,sessionBound,Objects.requireNonNull(BossCombatCatalog.get(boss.visualDefinition().id())));
    }
    public BossCombatController(cn.blockforge.generated.generatedmod.HeartDemonEntity boss,ServerPlayer owner,BlockPos center,UUID session,boolean sessionBound) {
        this(boss,owner,center,session,sessionBound,HeartDemonSkillPrograms.PROFILE);
    }
    private BossCombatController(Mob boss,ServerPlayer owner,BlockPos center,UUID session,boolean sessionBound,BossCombatCatalog.Profile profile) {
        this(boss,owner,center,session,sessionBound,profile,false);
    }
    public static BossCombatController playerCast(Mob proxy,ServerPlayer player,BossCombatCatalog.Profile profile) {
        return new BossCombatController(proxy,player,player.blockPosition().below(),UUID.randomUUID(),false,profile,true);
    }
    private BossCombatController(Mob boss,ServerPlayer owner,BlockPos center,UUID session,boolean sessionBound,BossCombatCatalog.Profile profile,boolean playerCast) {
        this.boss=boss;this.profile=profile;
        this.playerCast=playerCast;
        this.level=(ServerLevel)boss.level();this.ownerId=owner.getUUID();this.sessionBound=sessionBound;
        this.center=center.immutable();this.session=session;this.playerHealth=Math.max(20,owner.getMaxHealth());
        if(!playerCast&&boss instanceof VisualBossEntity&&(sessionBound||!boss.getPersistentData().getBoolean("BossCombatScaled"))) {
            float remaining=boss.getHealth()/Math.max(1,boss.getMaxHealth());
            double health=Math.max(160,HeartSwordCombatProfile.projectHeartSwordDamage(owner)*profile.healthMultiplier());
            boss.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.min(5_000_000,health));
            boss.setHealth(boss.getMaxHealth()*(sessionBound?1:remaining));
            boss.getPersistentData().putBoolean("BossCombatScaled",true);
        }
        phase=boss instanceof cn.blockforge.generated.generatedmod.HeartDemonEntity demon
                ?(demon.visualPhase()==cn.blockforge.generated.generatedmod.HeartDemonEntity.VisualPhase.PHASE_2?2:1)
                :sessionBound?1:BossCombatGeometry.phase(boss.getHealth()/boss.getMaxHealth(),profile.phases());
        syncPhase();
        if(playerCast){phase=profile.phases();engaged=true;readyAt=0;syncProxy();}
        boss.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        castOrigin=origin();castTarget=ground(owner.position());
    }
    public ServerPlayer owner() { return level==null?null:level.getServer().getPlayerList().getPlayer(ownerId); }
    private boolean valid(ServerPlayer player) {
        if(player==null||!player.isAlive()||player.level()!=level||GeneratedMod.data(player)==null)return false;
        if(playerCast)return BossPlayerSkills.activeForm(player).equals("wuxingxiantu:"+profile.id());
        var tag=GeneratedMod.data(player).wuxingxiantu$getMindscapeData();
        if(!sessionBound)return !player.isSpectator()&&!tag.hasUUID("session")
                &&player.distanceToSqr(boss)<64*64&&player.position().distanceToSqr(middle())<64*64;
        return tag.hasUUID("session")&&tag.getUUID("session").equals(session)&&tag.hasUUID("boss")&&tag.getUUID("boss").equals(boss.getUUID());
    }
    public void tick() {
        if(closed)return;
        ServerPlayer player=owner();if(!valid(player)){close();return;}
        clock++;
        if(playerCast)syncProxy();
        if(!engaged) {
            if(!sessionBound||cn.blockforge.generated.generatedmod.items.HeartSwordItem.isOwnedBy(player.getMainHandItem(),ownerId)) {
                engaged=true;readyAt=clock+60;
                fx(4,origin(),origin(),1,1,SAFE,60,80,sessionBound?"心境试炼｜观察前摇，寻找破绽":"交战开始｜观察前摇，寻找破绽");
            } else {
                lookAt(player.position());
                if(clock%80==1)player.displayClientMessage(Component.literal("凝出心剑后，试炼开始。场边符台可右键交互。"),true);
                return;
            }
        }
        if(clock%8==0){footsteps.addLast(playerCast?target():ground(player.position()));while(footsteps.size()>12)footsteps.removeFirst();}
        if(clock%20==0) {
            nodes.removeIf(n->{if(!n.entity.isAlive()||clock>=n.expires){n.discard();return true;}return false;});
            echoes.removeIf(id->level.getEntity(id)==null);
            for(var entry:List.copyOf(anchorCooldowns.entrySet()))if(clock>=entry.getValue()) {
                MindscapeArenas.setAnchorActive(level,MindscapeArenas.anchors(center).get(entry.getKey()),false);anchorCooldowns.remove(entry.getKey());
            }
        }
        var due=actions.stream().filter(a->a.at<=clock).toList();actions.removeAll(due);
        for(var action:due){if(closed||!boss.isAlive()||!valid(player))return;if(interrupted)break;action.action.run();}
        tickHazards(player);if(closed||!boss.isAlive()||!valid(player))return;
        tickShots(player);if(closed||!boss.isAlive()||!valid(player))return;
        if(skill!=null&&clock>=finishAt&&actions.isEmpty()) {
            skill=null;readyAt=clock+14;basicApproachUntil=readyAt+30;stopAnimation();
        }
        if(playerCast)return;
        if(skill==null&&clock>=readyAt&&clock>=exposedUntil) {
            int wanted=boss instanceof cn.blockforge.generated.generatedmod.HeartDemonEntity?phase
                    :BossCombatGeometry.phase(boss.getHealth()/boss.getMaxHealth(),profile.phases());
            if(wanted>phase) {phase++;syncPhase();clearTransient();readyAt=clock+60;
                fx(4,origin(),origin(),3,1,profile.color(),45,60,"第"+phase+"阶段｜新招式将单独展示");
                sound(SoundEvents.ELDER_GUARDIAN_CURSE,.55F,.75F);return;}
            choose();
        }
        if(skill==null&&clock>=readyAt-10&&clock>=exposedUntil) {
            Vec3 delta=player.position().subtract(boss.position());double dist=delta.horizontalDistance();
            double preferred=profile.id().equals("muyan")||profile.id().equals("ermei")?7:3.5;
            var basic=BossBasicAttacks.get(profile.id());
            if(basicDue&&basic!=null)preferred=Math.min(preferred,basic.range()*.8);
            if(dist>preferred)move(delta.normalize().scale(.15+phase*.025));
            lookAt(player.position());
        }
    }
    private void choose() {
        List<BossCombatCatalog.Skill> usable=profile.skills().stream().filter(s->s.phase()<=phase&&clock>=cooldowns.getOrDefault(s.id(),0)).toList();
        var basic=BossBasicAttacks.get(profile.id());
        if(basic!=null&&(basicDue||usable.isEmpty())&&clock>=cooldowns.getOrDefault(profile.id()+"_basic",0)
                &&boss.position().distanceToSqr(target())<=basic.range()*basic.range()
                &&clearSight(origin(),target())) {begin(basic.skill(profile.id()));return;}
        // Briefly approach for a melee follow-up, then resume skills if the player stays out of reach.
        if(basicDue&&basic!=null&&!basic.ranged()&&clock<basicApproachUntil) {readyAt=clock+4;return;}
        if(usable.isEmpty()){readyAt=clock+10;return;}
        var next=usable.stream().filter(s->s.phase()==phase&&!introduced.contains(s.id())).findFirst()
                .orElseGet(()->usable.stream().filter(s->!introduced.contains(s.id())).findFirst().orElse(null));
        if(next==null){var pool=new ArrayList<BossCombatCatalog.Skill>();for(var s:usable){int index=profile.skills().indexOf(s);if(index==lastIndex&&usable.size()>1)continue;for(int i=0;i<(index<3?3:1);i++)pool.add(s);}next=pool.get(boss.getRandom().nextInt(pool.size()));}
        begin(next);
    }
    private void begin(BossCombatCatalog.Skill next) {
        skill=next;basicAttack=next.id().equals(profile.id()+"_basic");
        if(!basicAttack){introduced.add(next.id());lastIndex=profile.skills().indexOf(next);}
        basicDue=!basicAttack;castId++;
        if(damageBudgets.size()>16)damageBudgets.keySet().removeIf(k->k<castId-8);
        victimBudgets.keySet().removeIf(k->Integer.parseInt(k.substring(0,k.indexOf(':')))<castId-8);
        victimTicks.entrySet().removeIf(e->clock-e.getValue()>200);
        cooldowns.put(next.id(),clock+next.cooldown());interrupted=false;anchorCount=0;
        castOrigin=origin();castTarget=target();finishAt=clock+next.windup()+next.recovery();
        String motionKey=next.id();
        if(motionKey.equals("yihuo_06")) {
            double distance=castOrigin.distanceTo(castTarget);
            motionKey+=distance<7?"_near":distance<16?"_middle":"_far";
        }else if(motionKey.equals("yihuo_03")||motionKey.equals("heart_demon_04")) {
            motionKey+="_history_"+footsteps.size();
        }
        var motion=profile.id().equals("heart_demon")?legacyHeartMotion(next,phase):Objects.requireNonNull(BossMotionCatalog.get(motionKey,phase),"Missing motion: "+motionKey);
        ((cn.blockforge.generated.generatedmod.boss.BossVisualEntity)boss).previewAnimation(motion.animation());lookAt(castTarget);
        if(playerCast)BossPlayerSkills.motion(owner(),profile.id(),motion.animation(),motion.duration());
        hold(motion.duration());
        // Vanilla mirror form also needs its equipment swing at each authored strike, not at cast start.
        for(var beat:motion.beats()) {
            if(!Set.of("open","pull","guard","clone","split","aim","read","coil").contains(beat.pose()))
                later(beat.tick(),()->{boss.swing(net.minecraft.world.InteractionHand.MAIN_HAND);if(playerCast&&owner()!=null)owner().swing(net.minecraft.world.InteractionHand.MAIN_HAND);});
        }
        fx(4,origin().add(0,boss.getBbHeight()*.65,0),origin(),.8F,1,profile.color(),next.windup(),next.windup(),next.name()+"｜"+next.hint());
        sound(SoundEvents.AMETHYST_BLOCK_CHIME,.6F,.65F+phase*.12F);
        if(basicAttack)BossBasicAttacks.cast(this,BossBasicAttacks.get(profile.id()));
        else BossSkillPrograms.cast(this,lastIndex+1);
    }
    private BossMotionCatalog.Motion legacyHeartMotion(BossCombatCatalog.Skill next,int phase) {
        String animation=next.animation();
        if(next.id().equals("heart_demon_basic"))animation="animation.xinmo_wuxiang.echo_weapon_slash";
        int duration=Math.max(next.windup()+next.recovery(),(int)Math.ceil(BossVisualRegistry.get("heart_demon_phase2").clips().getOrDefault(animation, (next.windup()+next.recovery())/20.0D)*20.0D));
        return new BossMotionCatalog.Motion(next.id(),phase,animation,next.windup(),duration,List.of(new BossMotionCatalog.Beat(next.windup(),"cast")));
    }
    public boolean forceSkill(String id) {
        var basic=BossBasicAttacks.get(profile.id());
        var selected=basic!=null&&id.equals(profile.id()+"_basic")?basic.skill(profile.id()):
                profile.skills().stream().filter(s->s.id().equals(id)).findFirst().orElse(null);
        if(selected==null||!valid(owner()))return false;
        if(boss instanceof cn.blockforge.generated.generatedmod.HeartDemonEntity&&selected.phase()>phase)return false;
        clearTransient();engaged=true;phase=Math.max(phase,selected.phase());syncPhase();exposedUntil=0;begin(selected);return true;
    }
    public boolean castPlayerSkill(String id,boolean noCooldown) {
        if(!playerCast||!valid(owner())||casting()||clock<exposedUntil)return false;
        var basic=BossBasicAttacks.get(profile.id());
        var selected=id.equals(profile.id()+"_basic")?basic.skill(profile.id()):profile.skills().stream().filter(s->s.id().equals(id)).findFirst().orElse(null);
        if(selected==null||!noCooldown&&clock<cooldowns.getOrDefault(id,0))return false;
        begin(selected);return true;
    }
    private void syncPhase(){if(boss instanceof VisualBossEntity visual)visual.combatPhase(phase);}
    public void phaseTransition(int next) {
        clearTransient();skill=null;interrupted=false;phase=Math.max(1,Math.min(profile.phases(),next));
        guardUntil=exposedUntil=0;pressure=0;readyAt=clock+60;syncPhase();
    }
    public boolean closed(){return closed;}
    public int phase(){return phase;}
    public boolean casting(){return skill!=null;}
    public int pendingCount(){return actions.size()+hazards.size()+shots.size();}
    public int windup(){return skill==null?24:skill.windup();}
    public int time(){return clock;}
    public Vec3 origin(){return ground(boss.position());}
    public Vec3 start(){return castOrigin;}
    public Vec3 aim(){return castTarget;}
    public Vec3 target(){
        ServerPlayer p=owner();if(p==null)return castTarget;
        if(playerCast){
            Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(32));
            var block=level.clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            var target=cn.blockforge.generated.generatedmod.DivineArtLogic.lookTarget(p,32);
            if(target!=null&&enemy(target)&&(block.getType()==HitResult.Type.MISS||eye.distanceToSqr(target.getEyePosition())<eye.distanceToSqr(block.getLocation())))return ground(target.position());
            Vec3 aim=block.getType()==HitResult.Type.MISS?p.position().add(p.getLookAngle().scale(16)):block.getLocation();
            return ground(aim);
        }
        return ground(p.position());
    }
    public Vec3 middle(){if(playerCast)return castOrigin==null?boss.position():castOrigin;return new Vec3(center.getX()+.5,center.getY()+1.05,center.getZ()+.5);}
    public Vec3 point(double x,double z){return middle().add(x,0,z);}
    public Vec3 ground(Vec3 p){return new Vec3(p.x,playerCast?p.y:center.getY()+1.05,p.z);}
    public Vec3 direction(){return horizontal(aim().subtract(start()));}
    public static Vec3 horizontal(Vec3 v){double n=v.horizontalDistance();return n<.001?new Vec3(0,0,1):new Vec3(v.x/n,0,v.z/n);}
    public static Vec3 rotate(Vec3 v,double angle){double a=Math.toRadians(angle);return new Vec3(v.x*Math.cos(a)-v.z*Math.sin(a),v.y,v.x*Math.sin(a)+v.z*Math.cos(a));}
    public Vec3 clamp(Vec3 p){Vec3 c=playerCast?castOrigin:middle();return new Vec3(Math.max(c.x-23,Math.min(c.x+23,p.x)),playerCast?p.y:c.y,Math.max(c.z-23,Math.min(c.z+23,p.z)));}
    public List<Vec3> history(){return List.copyOf(footsteps);}
    public int pressure(){return pressure;}
    public void pressure(int value){pressure=Math.max(0,Math.min(5,value));}
    public void later(int delay,Runnable action) {if(actions.size()>=160)return;actions.add(new Scheduled(clock+Math.max(0,delay),action));finishAt=Math.max(finishAt,clock+delay+(skill==null?20:skill.recovery())+20);}
    public void hold(int ticks){finishAt=Math.max(finishAt,clock+ticks);}
    private void lookAt(Vec3 p){Vec3 d=p.subtract(boss.position());float yaw=(float)Math.toDegrees(Math.atan2(-d.x,d.z));boss.setYRot(yaw);boss.setYHeadRot(yaw);boss.setYBodyRot(yaw);}
    public void move(Vec3 delta) {
        if(playerCast){ServerPlayer p=owner();if(!valid(p))return;Vec3 wanted=clamp(p.position().add(delta));Vec3 step=wanted.subtract(p.position());if(level.noCollision(p,p.getBoundingBox().move(step))){p.connection.teleport(wanted.x,wanted.y,wanted.z,p.getYRot(),p.getXRot());syncProxy();}return;}
        Vec3 desired=clamp(boss.position().add(delta));Vec3 actual=new Vec3(desired.x-boss.getX(),0,desired.z-boss.getZ());
        if(level.noCollision(boss,boss.getBoundingBox().move(actual)))boss.move(MoverType.SELF,actual);
    }
    public void teleport(Vec3 p) {if(playerCast){var owner=owner();if(owner!=null)move(clamp(p).subtract(owner.position()));return;}Vec3 pos=clamp(p);if(level.noCollision(boss,boss.getBoundingBox().move(pos.subtract(boss.position()))))boss.teleportTo(pos.x,pos.y,pos.z);}
    public void sound(net.minecraft.sounds.SoundEvent event,float volume,float pitch){level.playSound(null,boss.blockPosition(),event,SoundSource.HOSTILE,volume,pitch);}
    public int fx(int shape,Vec3 from,Vec3 to,float radius,float width,int color,int windup,int life,String label) {
        int id=++effectId;sendFx(new BossCombatFxPacket(effectOwner(),id,shape,from,to,radius,width,color,windup,life,label));return id;
    }
    private void sendFx(BossCombatFxPacket packet) {
        if(playerCast){var p=owner();if(p!=null)Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->p),packet);return;}
        if(!sessionBound)Network.CHANNEL.send(net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY.with(()->boss),packet);
        else {ServerPlayer player=owner();if(player!=null&&player.level()==level)Network.sendToPlayer(player,packet);}
    }
    private int effectOwner(){return playerCast&&owner()!=null?owner().getId():boss.getId();}
    private void syncProxy(){var p=owner();if(p!=null){boss.setPos(p.position());boss.setYRot(p.getYRot());boss.setYHeadRot(p.getYHeadRot());boss.setYBodyRot(p.getYRot());}}
    private void removeFx(int id){sendFx(new BossCombatFxPacket(effectOwner(),id,6,Vec3.ZERO,Vec3.ZERO,0,0,0,0,1,""));}
    public boolean accepts(DamageSource source) {
        if(playerCast)return source.getEntity() instanceof LivingEntity e&&enemy(e);
        return source.is(net.minecraft.tags.DamageTypeTags.BYPASSES_INVULNERABILITY)||valid(owner())&&source.getEntity()==owner();
    }
    public float incoming(DamageSource source,float amount) {
        if(!Float.isFinite(amount)||amount<=0)return 0;
        if(!engaged){engaged=true;readyAt=clock+40;}
        if(skill!=null&&!basicAttack&&profile.id().equals("heart_demon")&&lastIndex==7)pressure(pressure+1);
        if(clock<exposedUntil)amount*=1.25F;
        if(clock<guardUntil&&!HeartSwordCombatProfile.isHeartTrue(source)&&source.getEntity()!=null) {
            Vec3 incoming=horizontal(source.getEntity().position().subtract(boss.position()));
            if(incoming.dot(boss.getLookAngle())>.2)amount*=.4F;
        }
        if(skill!=null&&!basicAttack&&((profile.id().equals("bichen")&&lastIndex==4)||(profile.id().equals("pengzhi")&&lastIndex==6))) {
            pressure=0;interrupt(90);
        }
        return amount;
    }
    public void guard(int ticks){guardUntil=clock+ticks;}
    public void expose(int ticks){exposedUntil=Math.max(exposedUntil,clock+ticks);readyAt=Math.max(readyAt,exposedUntil);fx(4,origin().add(0,1,0),origin(),1.2F,1,SAFE,0,ticks,"破绽｜可集中攻击");}
    public void interrupt(int ticks){if(interrupted)return;interrupted=true;clearTransient();skill=null;expose(ticks);}
    public void recoil(float fraction){ServerPlayer p=owner();if(valid(p)){if(playerCast)p.hurt(p.damageSources().magic(),p.getMaxHealth()*Math.min(.035F,fraction));else boss.hurt(p.damageSources().playerAttack(p),boss.getMaxHealth()*Math.min(.035F,fraction));}}
    public void heal(float fraction){LivingEntity actor=playerCast?owner():boss;if(actor!=null&&actor.isAlive())actor.heal(actor.getMaxHealth()*Math.min(.03F,fraction));}

    private static final class Hazard {
        final int shape,start,end,period,cast;final Vec3 a,b;final float radius,width,damage,height;final boolean cover;final String tag;
        int last=-100,fxId;
        Hazard(int shape,Vec3 a,Vec3 b,float radius,float width,int start,int end,int period,float damage,float height,boolean cover,String tag,int cast) {
            this.shape=shape;this.a=a;this.b=b;this.radius=radius;this.width=width;this.start=start;this.end=end;this.period=period;this.damage=damage;this.height=height;this.cover=cover;this.tag=tag;this.cast=cast;
        }
    }
    private void hazard(int shape,Vec3 a,Vec3 b,double radius,double width,int delay,int duration,int period,float fraction,float height,boolean cover,String tag) {
        if(hazards.size()>=96)return;
        Hazard hazard=new Hazard(shape,a,b,(float)radius,(float)width,clock+delay,clock+delay+duration,period,fraction,height,cover,tag,castId);
        hazards.add(hazard);hazard.fxId=fx(shape,a,b,(float)radius,(float)width,DANGER,delay,delay+duration,"");
        if(duration<=40)hold(delay+duration+(skill==null?20:skill.recovery()));
    }
    public void line(Vec3 a,Vec3 b,double width,int delay,float damage){hazard(1,a,b,0,width,delay,5,20,damage,4,true,"",castId);}
    public void circle(Vec3 p,double radius,int delay,float damage){hazard(0,p,p,radius,0,delay,5,20,damage,5,false,"",castId);}
    public void column(Vec3 p,double radius,int delay,float damage){hazard(0,p,p,radius,0,delay,6,20,damage,20,false,"",castId);}
    public void cone(Vec3 p,Vec3 towards,double radius,double degrees,int delay,float damage){hazard(3,p,towards,radius,degrees,delay,6,20,damage,5,true,"",castId);}
    public void ring(Vec3 p,double radius,double width,int delay,float damage,boolean jumpable){hazard(2,p,p,radius,width,delay,5,20,damage,jumpable?.65F:8,false,"",castId);}
    public void zone(Vec3 p,double radius,int delay,int duration,float damage,String tag){hazard(0,p,p,radius,0,delay,duration,24,damage,2,false,tag,castId);}
    private void hazard(int shape,Vec3 a,Vec3 b,double r,double w,int delay,int duration,int period,float f,float height,boolean cover,String tag,int ignored){hazard(shape,a,b,r,w,delay,duration,period,f,height,cover,tag);}
    private void tickHazards(ServerPlayer player) {
        hazards.removeIf(h->clock>h.end);
        if(playerCast){
            List<LivingEntity> victims=enemies();
            for(Hazard h:List.copyOf(hazards)){
                if(clock<h.start||clock-h.last<h.period)continue;
                for(LivingEntity victim:victims){
                    if(victim.getY()>h.a.y+h.height||victim.getY()+victim.getBbHeight()<h.a.y)continue;
                    double x=victim.getX(),z=victim.getZ(),distance=Math.hypot(x-h.a.x,z-h.a.z);
                    boolean hit=switch(h.shape){case 0->distance<=h.radius+.25;case 1->BossCombatGeometry.segmentDistance(x,z,h.a.x,h.a.z,h.b.x,h.b.z)<=h.width*.5+.25;case 2->distance<=h.radius+.25&&distance>=h.radius-h.width-.25;case 3->BossCombatGeometry.cone(x,z,h.a.x,h.a.z,h.b.x,h.b.z,h.radius+.25,h.width);default->false;};
                    if(hit&&(!h.cover||clearSight(h.a,victim.position())))damage(victim,h.damage,h.cast,h.a);
                }
                h.last=clock;
            }
            return;
        }
        for(Hazard h:List.copyOf(hazards)) {
            if(closed||!valid(player))return;
            if(clock<h.start||clock-h.last<h.period||player.getY()>h.a.y+h.height||player.getY()+player.getBbHeight()<h.a.y)continue;
            double x=player.getX(),z=player.getZ(),distance=Math.hypot(x-h.a.x,z-h.a.z);
            boolean hit=switch(h.shape){
                case 0->distance<=h.radius+.25;
                case 1->BossCombatGeometry.segmentDistance(x,z,h.a.x,h.a.z,h.b.x,h.b.z)<=h.width*.5+.25;
                case 2->distance<=h.radius+.25&&distance>=h.radius-h.width-.25;
                case 3->BossCombatGeometry.cone(x,z,h.a.x,h.a.z,h.b.x,h.b.z,h.radius+.25,h.width);
                default->false;
            };
            if(hit&&(!h.cover||clearSight(h.a,player.position()))) {h.last=clock;damage(player,h.damage,h.cast,h.a);}
        }
    }
    public boolean clearSight(Vec3 from,Vec3 to) {
        Vec3 a=from.add(0,1.1,0),b=new Vec3(to.x,a.y,to.z);
        if(level.clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,boss)).getType()!=HitResult.Type.MISS)return false;
        for(Node node:nodes)if(node.active()&&(node.kind.equals("crystal")||node.kind.equals("cover"))&&BossCombatGeometry.segmentDistance(node.pos.x,node.pos.z,from.x,from.z,to.x,to.z)<1.0&&node.pos.distanceToSqr(from)>2)return false;
        return true;
    }
    private boolean enemy(LivingEntity target){
        ServerPlayer caster=owner();
        if(caster==null||target==caster||target==boss||!target.isAlive()||target.isSpectator()||target.isAlliedTo(caster)||target.getPersistentData().hasUUID(PARENT)||target.getPersistentData().hasUUID("BossEchoParent"))return false;
        if(target instanceof net.minecraft.world.entity.TamableAnimal pet&&caster.getUUID().equals(pet.getOwnerUUID()))return false;
        if(target instanceof net.minecraft.world.entity.player.Player player)return level.getServer().isPvpAllowed()&&!player.isCreative()&&caster.canHarmPlayer(player);
        return true;
    }
    private List<LivingEntity> enemies(){return level.getEntitiesOfClass(LivingEntity.class,boss.getBoundingBox().inflate(40,24,40),this::enemy).stream().limit(64).toList();}
    private void damage(LivingEntity victim,float fraction,int group,Vec3 from) {
        if(playerCast){
            ServerPlayer caster=owner();if(!valid(caster)||!enemy(victim)||clock-victimTicks.getOrDefault(victim.getUUID(),-100)<10)return;
            String key=group+":"+victim.getUUID();float used=victimBudgets.getOrDefault(key,0F);
            float reference=(float)Math.max(4,HeartSwordCombatProfile.projectHeartSwordDamage(caster));
            float amount=BossCombatGeometry.budget(reference*fraction/.055F,used,reference*6);
            if(amount>0&&victim.hurt(caster.damageSources().playerAttack(caster),amount)){victimBudgets.put(key,used+amount);victimTicks.put(victim.getUUID(),clock);}
            return;
        }
        if(!(victim instanceof ServerPlayer player))return;
        if(player.isCreative()||player.isSpectator()||clock-lastDamageTick<10||!valid(player))return;
        float used=damageBudgets.getOrDefault(group,0F),amount=BossCombatGeometry.budget(playerHealth*fraction,used,playerHealth*.30F);
        if(amount<=0)return;
        if(player.hurt(player.damageSources().mobAttack(boss),amount)) {
            damageBudgets.put(group,used+amount);lastDamageTick=clock;
            Vec3 away=horizontal(player.position().subtract(from));player.push(away.x*.18,.04,away.z*.18);player.hurtMarked=true;
        }
    }
    private static final class Shot {Vec3 pos,velocity;final Vec3 aim;final float damage;final boolean homing;final int cast,expires;Shot(Vec3 pos,Vec3 aim,double speed,boolean homing,float damage,int cast,int expires){this.pos=pos;this.aim=aim;this.velocity=horizontal(aim.subtract(pos)).scale(speed);this.homing=homing;this.damage=damage;this.cast=cast;this.expires=expires;}}
    public void projectile(Vec3 start,Vec3 aim,int delay,double speed,boolean homing,float damage) {
        Vec3 end=start.add(horizontal(aim.subtract(start)).scale(Math.min(35,start.distanceTo(aim)+5)));
        fx(1,start,end,0,.8F,DANGER,delay,delay,"");
        later(delay,()->{if(shots.size()<24)shots.add(new Shot(start,aim,speed,homing,damage,castId,clock+100));});
    }
    private void tickShots(ServerPlayer p) {
        for(Shot shot:List.copyOf(shots)) {
            if(closed||!valid(p))return;
            if(clock>shot.expires){shots.remove(shot);continue;}
            if(shot.homing) {Vec3 desired=horizontal(target().subtract(shot.pos)).scale(shot.velocity.length());shot.velocity=horizontal(shot.velocity.scale(.9).add(desired.scale(.1))).scale(shot.velocity.length());}
            Vec3 next=shot.pos.add(shot.velocity);
            if(!clearSight(shot.pos,next)||next.distanceToSqr(middle())>1800){shots.remove(shot);continue;}
            if(clock%3==0)fx(4,shot.pos.add(0,.8,0),next.add(0,.8,0),.3F,1,profile.color(),0,6,"");
            LivingEntity hit=playerCast?enemies().stream().filter(e->BossCombatGeometry.segmentDistance(e.getX(),e.getZ(),shot.pos.x,shot.pos.z,next.x,next.z)<Math.max(.75,e.getBbWidth()/2)&&e.getY()<shot.pos.y+3&&e.getY()+e.getBbHeight()>shot.pos.y).findFirst().orElse(null):p;
            if(hit!=null&&BossCombatGeometry.segmentDistance(hit.getX(),hit.getZ(),shot.pos.x,shot.pos.z,next.x,next.z)<Math.max(.75,hit.getBbWidth()/2)&&hit.getY()<shot.pos.y+3&&hit.getY()+hit.getBbHeight()>shot.pos.y) {damage(hit,shot.damage,shot.cast,shot.pos);shots.remove(shot);continue;}
            shot.pos=next;
        }
    }
    public void dash(Vec3 destination,int delay,int duration,double width,float damage) {
        Vec3 from=origin(),to=clamp(destination);fx(1,from,to,0,(float)width,DANGER,delay,delay+duration,"");
        for(int i=1;i<=duration;i++){final int n=i;later(delay+i,()->{Vec3 wanted=from.lerp(to,n/(double)duration);move(wanted.subtract(boss.position()));hazard(0,origin(),origin(),width*.5,0,0,2,20,damage,4,false,"",castId);});}
    }
    public void pull(Vec3 point,int delay,int duration,double strength,double radius) {
        fx(0,point,point,(float)radius,0,profile.color(),delay,delay+duration,"");
        for(int t=delay;t<delay+duration;t+=2)later(t,()->{ServerPlayer p=owner();if(!valid(p))return;for(LivingEntity victim:playerCast?enemies():List.of(p)){if(victim.distanceToSqr(point)>radius*radius||!clearSight(point,victim.position()))continue;Vec3 d=horizontal(point.subtract(victim.position())).scale(strength);victim.push(d.x,0,d.z);victim.hurtMarked=true;}});
    }

    public final class Node {
        public final String kind;public final Vec3 pos;public final ArmorStand entity;private final int expires;private final Runnable callback;private boolean used;private int fxId;
        private Node(String kind,Vec3 pos,ArmorStand entity,int expires,Runnable callback){this.kind=kind;this.pos=pos;this.entity=entity;this.expires=expires;this.callback=callback;}
        public boolean active(){return !used&&entity.isAlive()&&clock<expires;}
        public void activate(){if(!active())return;discard();if(callback!=null)callback.run();}
        public void discard(){if(!used)removeFx(fxId);used=true;entity.discard();}
    }
    public Node node(String kind,Vec3 position,int lifetime,Runnable action) {
        Vec3 p=clamp(position);ArmorStand entity=new ArmorStand(level,p.x,p.y,p.z);
        var appearance=new net.minecraft.nbt.CompoundTag();appearance.putBoolean("Small",true);appearance.putInt("DisabledSlots",0x3F3F3F);entity.readAdditionalSaveData(appearance);
        entity.setInvisible(true);entity.setNoGravity(true);
        entity.setCustomName(Component.literal(switch(kind){case "bitter"->"苦根 · 引导吞食";case "food"->"甜食 · 击碎";case "knot"->"心结 · 击碎";case "crystal"->"晶簇 · 可遮挡";case "cover"->"金印 · 掩体";default->"破绽 · 攻击";}));entity.setCustomNameVisible(true);
        Item item=switch(kind){case "bitter"->Items.DRIED_KELP_BLOCK;case "food"->Items.MELON;case "crystal"->Items.AMETHYST_BLOCK;case "cover"->Items.GOLD_BLOCK;default->Items.SEA_LANTERN;};
        entity.setItemSlot(EquipmentSlot.HEAD,new ItemStack(item));entity.getPersistentData().putUUID(PARENT,playerCast?ownerId:boss.getUUID());entity.getPersistentData().putUUID(SESSION,session);
        Node node=new Node(kind,p,entity,clock+lifetime,action);if(nodes.size()<20&&level.addFreshEntity(entity)){nodes.add(node);node.fxId=fx(4,p.add(0,.6,0),p,.7F,1,SAFE,0,lifetime,"");}else node.discard();return node;
    }
    public boolean interactNode(Entity entity,ServerPlayer player) {
        if(playerCast?player.level()!=level||!enemy(player)||player.distanceToSqr(entity)>64:!valid(player)||!player.getUUID().equals(ownerId)||player.distanceToSqr(entity)>64)return false;
        for(Node node:List.copyOf(nodes))if(node.entity==entity&&node.active()){node.activate();return true;}return false;
    }
    public VisualBossEntity echo(Vec3 position,int lifetime,boolean sharesHealth) {
        if(!(boss instanceof VisualBossEntity)||echoes.size()>=5)return null;
        VisualBossEntity echo=(VisualBossEntity)boss.getType().create(level);if(echo==null)return null;
        Vec3 p=clamp(position);echo.moveTo(p.x,p.y,p.z,boss.getYRot(),0);echo.setNoAi(true);echo.setNoGravity(true);
        echo.getPersistentData().putUUID("BossEchoParent",playerCast?ownerId:boss.getUUID());echo.getPersistentData().putBoolean("BossEchoShared",sharesHealth);echo.getPersistentData().putUUID(SESSION,session);
        if(level.addFreshEntity(echo)){echoes.add(echo.getUUID());later(lifetime,()->{if(!echo.isRemoved())echo.discard();});return echo;}return null;
    }
    public void hitEcho(VisualBossEntity echo,DamageSource source,float amount) {
        if(!accepts(source)||!Float.isFinite(amount)||amount<=0)return;
        if(echo.getPersistentData().getBoolean("BossEchoShared")){if(playerCast){var caster=owner();if(caster!=null)caster.hurt(source,amount);}else boss.hurt(source,amount);expose(30);}
        removeHazards(h->h.a.distanceToSqr(ground(echo.position()))<1);
        echo.discard();
    }
    public boolean anchor(BlockPos clicked,ServerPlayer player) {
        if(!sessionBound)return false;
        if(!valid(player)||!ownerId.equals(player.getUUID())||player.distanceToSqr(Vec3.atCenterOf(clicked))>49)return false;
        var anchors=MindscapeArenas.anchors(center);
        for(int i=0;i<anchors.size();i++) {
            BlockPos a=anchors.get(i);if(Math.abs(clicked.getX()-a.getX())>1||Math.abs(clicked.getZ()-a.getZ())>1||Math.abs(clicked.getY()-a.getY())>4)continue;
            if(clock<anchorCooldowns.getOrDefault(i,0))return true;
            anchorCooldowns.put(i,clock+160);MindscapeArenas.setAnchorActive(level,a,true);
            fx(0,ground(Vec3.atCenterOf(a)),ground(Vec3.atCenterOf(a)),5,0,SAFE,0,100,"心境机关｜净化与破招");
            removeHazards(h->h.a.distanceToSqr(Vec3.atCenterOf(a))<100||h.tag.equals("poison"));
            anchorCount++;sound(SoundEvents.AMETHYST_BLOCK_RESONATE,.7F,1.2F);
            boolean canBreak=skill!=null&&((profile.id().equals("ermei")&&lastIndex==5)||(profile.id().equals("muyan")&&lastIndex==1)||(profile.id().equals("pengju")&&lastIndex==6)||(profile.id().equals("yihuo")&&lastIndex==3));
            if(canBreak){recoil(.02F);interrupt(80);}else if(profile.id().equals("bichen"))expose(60);
            return true;
        }return false;
    }
    public void gaze(int delay,float damage) {gaze(delay,damage,true);}
    public void gaze(int delay,float damage,boolean exposeAfter) {
        fx(0,origin(),origin(),30,0,profile.color(),delay,delay+8,"");
        later(delay,()->{ServerPlayer p=owner();if(!valid(p))return;for(LivingEntity victim:playerCast?enemies():List.of(p)){Vec3 toward=boss.getEyePosition().subtract(victim.getEyePosition()).normalize();if(victim.getLookAngle().dot(toward)>.45&&clearSight(origin(),victim.position()))damage(victim,damage,castId,origin());}if(exposeAfter)expose(60);});
    }
    private void removeHazards(java.util.function.Predicate<Hazard> predicate){hazards.removeIf(h->{if(!predicate.test(h))return false;removeFx(h.fxId);return true;});}
    public void drainZones(String tag){removeHazards(h->h.tag.equals(tag));}
    private void clearTransient() {
        stopAnimation();
        actions.clear();hazards.clear();shots.clear();nodes.forEach(Node::discard);nodes.clear();
        for(UUID id:echoes){Entity entity=level.getEntity(id);if(entity!=null)entity.discard();}echoes.clear();
        sendFx(new BossCombatFxPacket(effectOwner(),0,5,Vec3.ZERO,Vec3.ZERO,0,0,0,0,1,""));
    }
    public void close(){if(closed)return;closed=true;clearTransient();}
    private void stopAnimation(){((cn.blockforge.generated.generatedmod.boss.BossVisualEntity)boss).previewAnimation("");if(playerCast&&owner()!=null)BossPlayerSkills.motion(owner(),profile.id(),"",0);}
}
