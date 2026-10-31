package cn.blockforge.generated.generatedmod.yujian;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Inventory-backed implement: its display copy can never be picked up or saved. */
public final class ControlledSwordEntity extends Entity {
    private static final EntityDataAccessor<ItemStack> ITEM=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<Integer> OWNER=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Boolean> SUPPORT=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> FLIGHT_ROOT=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Float> SCALE=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ROLL=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> BANK=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> SPEED=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ANGULAR=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> SPRINT=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> STATE=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> IMPACT=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> PHASE=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> TARGET_SPEED=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> ACCELERATION=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Boolean> AHEAD_LOADED=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> COLLIDING=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> DEBUG=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> DUNXU_STAGE=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HUAHONG_STAGE=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HUAHONG_SKIN=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.INT);
    public HuahongSkin huahongSkin(){return HuahongSkin.fromId(entityData.get(HUAHONG_SKIN));}
    public void huahongSkin(HuahongSkin skin){entityData.set(HUAHONG_SKIN,skin.ordinal());}
    private static final EntityDataAccessor<Float> HUAHONG=SynchedEntityData.defineId(ControlledSwordEntity.class,EntityDataSerializers.FLOAT);
    private float previousHuahong,visualHuahong;
    public int huahongStage(){return entityData.get(HUAHONG_STAGE);}
    public float huahongStrength(){return entityData.get(HUAHONG);}
    public float renderHuahong(float partial){return net.minecraft.util.Mth.lerp(partial,previousHuahong,visualHuahong);}
    public void huahong(int stage,float strength){entityData.set(HUAHONG_STAGE,stage);entityData.set(HUAHONG,strength);}
    public UUID ownerId,binding;
    public int slot,age,mastery;
    public String art="";
    public Vec3 anchor;
    public UUID target;
    public net.minecraft.core.BlockPos block;
    public final java.util.Set<UUID> hits=new java.util.HashSet<>();
    private int lerpSteps;
    private double lx,ly,lz;
    private float lyaw,lpitch;
    private float visualRoll, previousRoll, sprintBlend, previousSprintBlend;
    private float visualBank, previousBank, visualSpeed, previousSpeed, visualAngular, previousAngular;
    private boolean visualInitialized;
    private float jinghongStrength,visualJinghong,previousJinghong;
    public void jinghongStrength(float value){jinghongStrength=value;}
    public float jinghongStrength(){return jinghongStrength;}
    public float renderJinghong(float partial){return net.minecraft.util.Mth.lerp(partial,previousJinghong,visualJinghong);}
    private final FlightInterpolation flightInterpolation=new FlightInterpolation();
    public void acceptSnapshot(FlightSnapshot snapshot){if(level().isClientSide)flightInterpolation.accept(snapshot,tickCount);}

    public ControlledSwordEntity(EntityType<? extends ControlledSwordEntity> type,Level level) { super(type,level);noPhysics=true;setNoGravity(true); }
    @Override protected void defineSynchedData(){entityData.define(ITEM,ItemStack.EMPTY);entityData.define(OWNER,-1);entityData.define(SUPPORT,false);entityData.define(FLIGHT_ROOT,false);entityData.define(SCALE,1.0F);entityData.define(ROLL,0.0F);entityData.define(BANK,0.0F);entityData.define(SPEED,0.0F);entityData.define(ANGULAR,0.0F);entityData.define(SPRINT,false);entityData.define(STATE,0);entityData.define(IMPACT,0);
        entityData.define(PHASE,0);entityData.define(TARGET_SPEED,0F);entityData.define(ACCELERATION,0F);
        entityData.define(AHEAD_LOADED,true);entityData.define(COLLIDING,false);entityData.define(DEBUG,false);entityData.define(DUNXU_STAGE,0);entityData.define(HUAHONG_STAGE,0);entityData.define(HUAHONG,0F);entityData.define(HUAHONG_SKIN,0);}
    public YujianPhase advancedPhase(){return YujianPhase.values()[net.minecraft.util.Mth.clamp(entityData.get(PHASE),0,YujianPhase.values().length-1)];}
    public void advancedPhase(YujianPhase phase){entityData.set(PHASE,phase.ordinal());}
    public float targetSpeed(){return entityData.get(TARGET_SPEED);}
    public float flightAcceleration(){return entityData.get(ACCELERATION);}
    public boolean aheadLoaded(){return entityData.get(AHEAD_LOADED);}
    public boolean colliding(){return entityData.get(COLLIDING);}
    public boolean debugFlight(){return entityData.get(DEBUG);}
    public int dunxuStage(){return entityData.get(DUNXU_STAGE);}
    public void dunxuStage(int stage){entityData.set(DUNXU_STAGE,Math.max(0,Math.min(3,stage)));}
    public void diagnostics(double target,double acceleration,boolean loaded,boolean blocked,boolean debug){
        entityData.set(TARGET_SPEED,(float)target);entityData.set(ACCELERATION,(float)acceleration);
        entityData.set(AHEAD_LOADED,loaded);entityData.set(COLLIDING,blocked);entityData.set(DEBUG,debug);
    }
    public float visualScale(){return entityData.get(SCALE);}
    public void visualScale(float value){entityData.set(SCALE,value);}
    public ItemStack display(){return entityData.get(ITEM);}
    public boolean support(){return entityData.get(SUPPORT);}
    public boolean flightRoot(){return entityData.get(FLIGHT_ROOT);}
    public void flightRoot(boolean value){entityData.set(FLIGHT_ROOT,value);noPhysics=!value;setNoGravity(true);}
    public float roll(){return entityData.get(ROLL);}
    public void roll(float value){entityData.set(ROLL,value);}
    public void bank(float value){entityData.set(BANK,value);}
    public float bank(){return entityData.get(BANK);}
    public float renderBank(float partial){return net.minecraft.util.Mth.lerp(partial,previousBank,visualBank);}
    public float renderSpeed(float partial){return net.minecraft.util.Mth.lerp(partial,previousSpeed,visualSpeed);}
    public float renderAngular(float partial){return net.minecraft.util.Mth.lerp(partial,previousAngular,visualAngular);}
    public float flightSpeed(){return entityData.get(SPEED);}
    public void flightSpeed(float value){entityData.set(SPEED,value);}
    public float angularSpeed(){return entityData.get(ANGULAR);}
    public void angularSpeed(float value){entityData.set(ANGULAR,value);}
    public boolean sprinting(){return entityData.get(SPRINT);}
    public void sprinting(boolean value){entityData.set(SPRINT,value);}
    public boolean rollRecoveryActive(){ return flightRoot() && Math.abs(roll()) > 0.01F && Math.abs(angularSpeed()) < 0.01F; }
    public FlyingSwordState flightState(){return FlyingSwordState.values()[entityData.get(STATE)];}
    public void flightState(FlyingSwordState value){entityData.set(STATE,value.ordinal());}
    public int impact(){return entityData.get(IMPACT);}
    public void impactNow(){entityData.set(IMPACT,impact()+1);}
    public float renderRoll(float partial){return net.minecraft.util.Mth.lerp(partial,previousRoll,visualRoll);}
    public float sprintBlend(float partial){return net.minecraft.util.Mth.lerp(partial,previousSprintBlend,sprintBlend);}
    public double riderOffset(){return FlyingSwordRegistry.visual(display()).riderOffset();}
    @Override public void onSyncedDataUpdated(EntityDataAccessor<?> key){
        super.onSyncedDataUpdated(key);
        if(FLIGHT_ROOT.equals(key)){noPhysics=!flightRoot();refreshDimensions();}
    }
    @Override public EntityDimensions getDimensions(Pose pose){return flightRoot()?EntityDimensions.scalable(0.85F,0.35F):super.getDimensions(pose);}
    // A null controlling passenger keeps vanilla vehicle movement packets non-authoritative.
    @Override public LivingEntity getControllingPassenger(){return null;}
    @Override public boolean shouldRiderSit(){return !flightRoot();}
    public Entity owner(){return level().getEntity(entityData.get(OWNER));}
    public void bind(ServerPlayer player,ItemStack stack,int index,boolean riding){
        ownerId=player.getUUID();binding=YujianController.binding(stack);slot=index;
        entityData.set(ITEM,stack.copy());entityData.set(OWNER,player.getId());entityData.set(SUPPORT,riding);
        setPos(player.position().add(0,1,0));
    }
    public void order(String id,Vec3 point,UUID victim,net.minecraft.core.BlockPos pos,int uses){art=id;anchor=point;target=victim;block=pos;mastery=uses;age=0;hits.clear();}
    public void syncFlightDisplay(ItemStack source){if(!ItemStack.matches(display(),source))entityData.set(ITEM,source.copy());}
    @Override public void tick(){
        previousHuahong=visualHuahong;visualHuahong=huahongStrength();
        super.tick();
        if(level().isClientSide){
            if(!visualInitialized){
                visualInitialized=true;visualRoll=roll();visualBank=entityData.get(BANK);
                visualSpeed=flightSpeed();visualAngular=angularSpeed();
            }
            previousRoll=visualRoll;
            previousJinghong=visualJinghong;
            previousBank=visualBank;previousSpeed=visualSpeed;previousAngular=visualAngular;
            FlightSnapshot sample=flightInterpolation.sample(tickCount);
            if(flightRoot()&&sample!=null){
                setPos(sample.position());setYRot(sample.yaw());setXRot(sample.pitch());
                visualRoll=sample.roll();visualBank=sample.bank();visualSpeed=sample.speed();visualAngular=sample.angular();
                visualJinghong=sample.jinghong();
                previousSprintBlend=sprintBlend;sprintBlend+=((sprinting()?1:0)-sprintBlend)*.22F;
                lerpSteps=0;return;
            }
            float step=1F/Math.max(1,lerpSteps);
            visualRoll+=(roll()-visualRoll)*step;
            visualBank+=(entityData.get(BANK)-visualBank)*step;
            visualSpeed+=(flightSpeed()-visualSpeed)*step;
            visualAngular+=(angularSpeed()-visualAngular)*step;
            previousSprintBlend=sprintBlend;
            sprintBlend+=((sprinting()?1:0)-sprintBlend)*0.22F;
            if(lerpSteps>0){setPos(getX()+(lx-getX())/lerpSteps,getY()+(ly-getY())/lerpSteps,getZ()+(lz-getZ())/lerpSteps);setYRot(getYRot()+net.minecraft.util.Mth.wrapDegrees(lyaw-getYRot())/lerpSteps);setXRot(getXRot()+(lpitch-getXRot())/lerpSteps);lerpSteps--;}
            return;
        }
        if(!(level() instanceof net.minecraft.server.level.ServerLevel server))return;
        ServerPlayer player=ownerId==null?null:(server.getPlayerByUUID(ownerId) instanceof ServerPlayer found?found:null);
        if(player==null || !player.isAlive() || player.level()!=level() || (!flightRoot() && YujianController.find(player,binding).isEmpty())){discard();return;}
        if (flightRoot()) {
            YujianController.tickFlightEntity(this,player);
            if(!isRemoved())cn.blockforge.generated.generatedmod.Network.CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(()->this),FlightSnapshot.of(this));
        }
        else YujianController.tickSword(this,player);
        age++;
    }
    public void toward(Vec3 destination,double maximum){
        Vec3 delta=destination.subtract(position());
        Vec3 step=delta.scale(Math.min(1,maximum/Math.max(0.001,delta.length())));
        setPos(position().add(step));setDeltaMovement(Vec3.ZERO);
        if(step.lengthSqr()>0.0001){setYRot((float)(Math.atan2(-step.x,step.z)*180/Math.PI));setXRot((float)(-Math.atan2(step.y,step.horizontalDistance())*180/Math.PI));}
    }
    @Override protected void positionRider(Entity passenger, MoveFunction moveFunction) {
        if(!hasPassenger(passenger))return;
        Vec3 offset=flightRoot()?new Vec3(FlightFrame.rotation(getYRot(),getXRot(),level().isClientSide?renderRoll(1):roll())
                .transform(new org.joml.Vector3f(0,(float)riderOffset(),0))):new Vec3(0,-0.18,0);
        moveFunction.accept(passenger,getX()+offset.x,getY()+offset.y,getZ()+offset.z);
    }
    @Override protected boolean canAddPassenger(Entity passenger) {
        return flightRoot() && getPassengers().isEmpty() && passenger.getUUID().equals(ownerId);
    }
    @Override public Vec3 getDismountLocationForPassenger(net.minecraft.world.entity.LivingEntity passenger) {
        return position().add(0, 0.35D, 0);
    }
    @Override public boolean isPickable() { return false; }
    @Override public boolean isAttackable() { return false; }
    @Override public boolean shouldBeSaved() { return false; }
    @Override public void lerpTo(double x,double y,double z,float yaw,float pitch,int steps,boolean teleport){
        if(flightRoot()&&flightInterpolation.active())return;
        lx=x;ly=y;lz=z;lyaw=yaw;lpitch=pitch;lerpSteps=Math.max(1,steps);
    }
    @Override protected void readAdditionalSaveData(CompoundTag tag){discard();}
    @Override protected void addAdditionalSaveData(CompoundTag tag){}
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getAddEntityPacket(){return net.minecraftforge.network.NetworkHooks.getEntitySpawningPacket(this);}
}
