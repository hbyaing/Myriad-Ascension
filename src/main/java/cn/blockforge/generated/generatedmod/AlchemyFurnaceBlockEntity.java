package cn.blockforge.generated.generatedmod;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * 炼丹炉的动态状态载体：同步炼制状态，成型外观由独立炉体模型呈现。
 * 六种品阶共用一个方块实体类型，普通右键打开八卦炼丹面板。
 *
 * <p>容器布位：0-7 为八个八卦素材槽，8 为「异火瓶」点火槽。素材与点火均由
 * {@link AlchemyFurnaceMenu} 展示，开炉锁定实际素材，完成或取消时结算；异火瓶保留在槽内。
 */
public final class AlchemyFurnaceBlockEntity extends BlockEntity implements Container {
    // V5 authority lives in the write-ahead journal. Only this UUID/public progress enters chunk NBT.
    private java.util.UUID v5Ref;
    private net.minecraft.nbt.CompoundTag v5=new net.minecraft.nbt.CompoundTag();
    private boolean v5Fault, v5Client;
    private long nextV5Tick;
    private int v5DeviceWear;
    public int v5DeviceWear(){return v5DeviceWear;}
    public int v5Warning(){return v5.isEmpty()?0:v5.getInt("Warning");}
    public int v5HeatMode(){return v5.isEmpty()?1:v5.getInt("HeatMode");}
    public int v5StructureDamage(){return v5.isEmpty()?0:v5.getInt("StructureDamage");}
    public boolean v5Warming(){return !v5.isEmpty()&&v5.getBoolean("Warming");}
    public boolean v5Control(net.minecraft.server.level.ServerPlayer player,int button){
        if(v5Fault||!stillValid(player)||!player.isAlive()||player.isSpectator()||v5.isEmpty()||!v5.getUUID("Owner").equals(player.getUUID()))return false;
        var next=cn.blockforge.generated.generatedmod.alchemy.batch.BatchIncidents.control(v5,button,level.getGameTime());if(next==v5)return false;
        new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(player.server).write(next);applyV5(next);sync();return true;
    }
    public int v5DismantleFuel(){return spiritFuel+(v5.isEmpty()?0:v5.getInt("FuelReserved"));}
    public boolean v5Batch(){return v5Ref!=null||v5Client;}
    public boolean inventoryLocked(){return busy()||v5Batch();}
    public net.minecraft.nbt.CompoundTag v5ServerRecord(){if(!(level instanceof ServerLevel))throw new IllegalStateException("Server only");return v5.copy();}
    private void applyV5(net.minecraft.nbt.CompoundTag doc){
        cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.validate(doc);
        v5=doc.copy();v5Ref=doc.getUUID("Id");v5Client=false;v5DeviceWear=doc.getInt("DeviceWear");
        var inventory=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.inventory(doc);
        for(int i=0;i<items.size();i++)items.set(i,inventory.get(i));
        spiritFuel=doc.getInt("FuelBalance");needsSpiritFuel=false;
        var core=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.core(doc);
        brewing=doc.getString("State").equals("ACTIVE");poweredTicks=(doc.contains("IncidentVersion")?doc.getInt("ProcessStep"):(int)core.tick())*20;
        var profile=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.profile(doc.getString("Profile"),doc.getInt("Burn"));
        startTick=0;finishTick=profile.steps()*20L;outcome=brewing?0:1;
        if(!brewing){completedResult=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.result(doc);completionTick=level.getGameTime();}
        else completedResult=ItemStack.EMPTY;
        activeFireId=cn.blockforge.generated.generatedmod.alchemy.inputs.AlchemyInputs.fire(items.get(IGNITION_SLOT)).id();
    }
    private void clearV5(){v5Ref=null;v5=new CompoundTag();v5Client=false;brewing=false;completedResult=ItemStack.EMPTY;outcome=0;startTick=finishTick=0;completionTick=-1;}
    public void restoreV5(){
        if(!(level instanceof ServerLevel server))return;
        try{
            var doc=new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.getServer()).at(server,worldPosition);
            if(doc.isEmpty()){if(v5Ref!=null)throw new IllegalStateException("Missing V5 journal; retain escrow");return;}
            boolean same=v5Ref!=null&&v5Ref.equals(doc.getUUID("Id"));
            if(doc.getBoolean("Detached")){if(same){for(int i=0;i<items.size();i++)items.set(i,ItemStack.EMPTY);spiritFuel=0;clearV5();}return;}
            if(doc.getString("State").equals("CLAIMED")){if(same){applyV5(doc);clearV5();}return;}
            if(!doc.getString("Dimension").equals(server.dimension().location().toString())||doc.getLong("Position")!=worldPosition.asLong())throw new IllegalStateException("Journal location mismatch");
            applyV5(doc);nextV5Tick=level.getGameTime()+20;v5Fault=false;sync();
        }catch(RuntimeException e){v5Fault=true;com.mojang.logging.LogUtils.getLogger().error("V5 furnace quarantined; journal retained",e);}
    }
    public boolean startV5Batch(net.minecraft.server.level.ServerPlayer player){return startV5Batch(player,null);}
    public boolean startV5Batch(net.minecraft.server.level.ServerPlayer player,AlchemyRecipe recipe){
        if(!(level instanceof ServerLevel server)||inventoryLocked()||hasResult()||!stillValid(player)||!player.isAlive()||player.isSpectator()
                ||v5DeviceWear>=1000||!AlchemyFurnaceBlock.isFormed(getBlockState())||!ProgressionGate.canUseAlchemyFurnace(player,tier())||!ignited()
                ||!ProgressionGate.canUseFire(player,ignitionTier())||!cn.blockforge.generated.generatedmod.alchemy.FormulaKnowledge.unlocked(player)
                ||Network.hasAlchemyJob(level,worldPosition)||Network.isAlchemyBusy(player))return false;
        try{
            net.minecraft.nbt.CompoundTag doc;
            if(recipe==null)doc=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.prepare(this,player);
            else{
                if(recipe.furnaceTier()>tier()||!ProgressionGate.isAtLeastRealm(player,recipe.minimumRealm())||!recipe.acceptsFire(ignitionTier())||!cn.blockforge.generated.generatedmod.alchemy.FormulaKnowledge.knows(player,recipe.formulaId()))return false;
                var materialSlots=new net.minecraft.world.SimpleContainer(8);for(int i=0;i<8;i++)materialSlots.setItem(i,items.get(i).copy());
                int[] used=cn.blockforge.generated.generatedmod.alchemy.HeavenlyFurnace.allocationV5(recipe,materialSlots);if(used==null)return false;
                used=cn.blockforge.generated.generatedmod.alchemy.auxiliary.AuxiliaryAlchemy.allocate(materialSlots,used).counts();
                doc=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.prepare(this,player,used,"ordinary_formula",recipe.getId().toString());
            }
            var journal=new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.getServer());
            journal.write(doc);journal.link(server,worldPosition,doc.getUUID("Id"));applyV5(doc);nextV5Tick=level.getGameTime()+20;sync();
            // Make the removal of original ingredients durable before the batch can progress or be claimed.
            server.getChunkSource().save(true);
            player.displayClientMessage(net.minecraft.network.chat.Component.literal("V5批次已托管：材料与炉内灵力已锁定，按真实材料反应结算，完成后领取成丹或副产物。"),true);return true;
        }catch(RuntimeException e){if(v5Ref!=null)v5Fault=true;player.displayClientMessage(net.minecraft.network.chat.Component.literal(v5Ref!=null?"批次已托管但保存确认失败，已暂停，材料保留。":"未能开炉："+e.getMessage()),true);com.mojang.logging.LogUtils.getLogger().warn("V5 start rejected or retained for recovery",e);return false;}
    }
    private void tickV5(){
        if(!(level instanceof ServerLevel server)||v5Fault)return;
        if(v5.isEmpty()){restoreV5();return;}
        if(!v5.getString("State").equals("ACTIVE")){
            if(level.getGameTime()>=nextV5Tick){nextV5Tick=level.getGameTime()+20;
                try{var current=new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.getServer()).read(v5Ref);if(current.getString("State").equals("CLAIMED")){clearV5();sync();}}
                catch(RuntimeException e){v5Fault=true;com.mojang.logging.LogUtils.getLogger().error("V5 receipt reconciliation retained",e);}}
            return;
        }
        if(structureDirty||(level.getGameTime()+worldPosition.asLong())%200L==0){structureDirty=false;AlchemyFurnaceStructure.refreshAround(level,worldPosition);}
        if(!v5.contains("IncidentVersion")&&!AlchemyFurnaceBlock.isFormed(getBlockState())){nextV5Tick=level.getGameTime()+20;return;}
        if(level.getGameTime()<nextV5Tick)return;nextV5Tick=level.getGameTime()+20;
        try{var next=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.advance(v5,server,worldPosition);
            if(next==v5)return;new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.getServer()).write(next);boolean blast=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.core(next).status()==cn.blockforge.generated.generatedmod.alchemy.simulation.AlchemySimulation.Status.EXPLODED;applyV5(next);sync();
            if(blast)cn.blockforge.generated.generatedmod.alchemy.FurnaceExplosion.contained(this);
        }catch(RuntimeException e){v5Fault=true;com.mojang.logging.LogUtils.getLogger().error("V5 step stopped before publication; journal retained",e);}
    }
    public boolean sealV5Batch(){
        if(!(level instanceof ServerLevel server)||v5.isEmpty()||!v5.getString("State").equals("ACTIVE"))return false;
        var next=cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.seal(v5);new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.getServer()).write(next);applyV5(next);sync();return true;
    }
    public void detachV5(){
        if(!(level instanceof ServerLevel server)||v5.isEmpty())return;
        boolean active=v5.getString("State").equals("ACTIVE");var next=active?cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.seal(v5):v5.copy();next.putBoolean("Detached",true);
        new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.getServer()).write(next);applyV5(next);
        if(active)net.minecraft.world.Containers.dropItemStack(server,worldPosition.getX()+.5,worldPosition.getY()+.5,worldPosition.getZ()+.5,completedResult.copy());
    }
    private boolean sealedV5Result(){return completedResult.hasTag()&&completedResult.getTag().hasUUID(cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.KEY);}
    private int collectV5(Player player){
        if(!(player instanceof net.minecraft.server.level.ServerPlayer server)||!sealedV5Result()||!stillValid(player)||!player.isAlive()||player.isSpectator())return 0;
        try{var receipt=completedResult.getTag().getUUID(cn.blockforge.generated.generatedmod.alchemy.batch.V5Batch.KEY);
            boolean already=new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.server).read(receipt).getString("State").equals("CLAIMED");
            boolean claimed=cn.blockforge.generated.generatedmod.alchemy.batch.BatchDelivery.claim(server,receipt);
            if(!claimed)return 0;clearV5();sync();return already?0:1;
        }catch(RuntimeException e){com.mojang.logging.LogUtils.getLogger().error("V5 claim retained for recovery",e);return 0;}
    }
    public static final int MATERIAL_SLOT_START = 0;
    public static final int MATERIAL_SLOT_COUNT = 8;
    public static final int IGNITION_SLOT = 8;
    public static final int CORE_SLOT = 9;
    public static final int CONTAINER_SIZE = MATERIAL_SLOT_COUNT + 2;
    public int spiritCapacity() { return cn.blockforge.generated.generatedmod.alchemy.AlchemyBalance.fuelCapacity(tier()); }
    private int burnRate() { return cn.blockforge.generated.generatedmod.alchemy.AlchemyBalance.burnPerSecond(tier()); }
    private int spiritFuel, poweredTicks, fuelTicks;
    private boolean needsSpiritFuel;
    private boolean rankedFailure;
    public void rankedFailure(boolean enabled){rankedFailure=enabled;setChanged();}
    private long lastInjection = Long.MIN_VALUE;
    public boolean upgradingCore;
    public int spiritFuel() { return spiritFuel; }
    public boolean needsSpiritFuel() { return needsSpiritFuel; }
    public boolean poweredComplete() { return poweredTicks >= finishTick-startTick; }
    public void enableSpiritFuel() { needsSpiritFuel=true;poweredTicks=0;fuelTicks=0;sync(); }
    public int injectSpirit(Player player) {
        if (!(level instanceof ServerLevel) || !stillValid(player) || !player.isAlive() || player.isSpectator() || !ignited() || v5Batch() || explosionPending()
                || !ProgressionGate.canUseFire(player,ignitionTier())) return 0;
        long now=level.getGameTime();
        if(lastInjection!=Long.MIN_VALUE && now-lastInjection<4) return 0;
        lastInjection=now;
        var data=GeneratedMod.data(player);
        if(data==null)return 0;
        int amount=Math.min(cn.blockforge.generated.generatedmod.alchemy.AlchemyBalance.injectionPerTransfer(tier()),Math.min(spiritCapacity()-spiritFuel,(int)Math.floor(data.wuxingxiantu$getAura())));
        if(amount<=0)return 0;
        data.wuxingxiantu$setAura(data.wuxingxiantu$getAura()-amount);
        spiritFuel+=amount;sync();if(player instanceof net.minecraft.server.level.ServerPlayer owner)GeneratedMod.sync(owner);return amount;
    }
    public void advancePoweredTick() {
        if(!brewing || !needsSpiritFuel || poweredComplete() || !AlchemyFurnaceBlock.isFormed(getBlockState()))return;
        if(spiritFuel<burnRate()) { failFromEmptyFuel();return; }
        poweredTicks++;
        if(++fuelTicks>=20) { spiritFuel-=burnRate();fuelTicks=0; }
        setChanged();
        if(level!=null && (poweredTicks%10==0 || poweredComplete() || spiritFuel==0))sync();
        if(poweredComplete() && level instanceof ServerLevel server)
            cn.blockforge.generated.generatedmod.network.service.AlchemyService.wakeAt(server,worldPosition);
        else if(spiritFuel<burnRate())failFromEmptyFuel();
    }
    private void failFromEmptyFuel() {
        if(level instanceof ServerLevel server)cn.blockforge.generated.generatedmod.network.service.AlchemyService.failAlchemyFuelAt(server,worldPosition);
    }
    public boolean upgradeCore(Player player) {
        if(!(level instanceof ServerLevel server) || busy() || hasResult() || !stillValid(player)||!player.isAlive()||player.isSpectator()||!ProgressionGate.canUseAlchemyFurnace(player,tier()))return false;
        ItemStack core=items.get(CORE_SLOT);
        if(core.isEmpty()&&v5DeviceWear>0&&player instanceof net.minecraft.server.level.ServerPlayer owner){
            int count=0;for(int i=0;i<36;i++)if(player.getInventory().getItem(i).is(net.minecraft.world.item.Items.IRON_INGOT))count+=player.getInventory().getItem(i).getCount();
            if(count<4)return false;int left=4;for(int i=0;i<36&&left>0;i++){var item=player.getInventory().getItem(i);if(item.is(net.minecraft.world.item.Items.IRON_INGOT)){int paid=Math.min(left,item.getCount());item.shrink(paid);left-=paid;}}
            owner.server.getPlayerList().save(owner);var journal=new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.getServer());var saved=journal.at(server,worldPosition);if(!saved.isEmpty()){saved.putInt("DeviceWear",0);journal.write(saved);}
            v5DeviceWear=0;sync();server.getChunkSource().save(true);return true;
        }
        if(!(core.getItem() instanceof net.minecraft.world.item.BlockItem item)
                || !(item.getBlock() instanceof AlchemyFurnaceBlock target) || target.tier()<=tier()
                || !ProgressionGate.canUseAlchemyFurnace(player,target.tier()))return false;
        var old=getBlockState();
        var next=target.defaultBlockState().setValue(AlchemyFurnaceBlock.FACING,old.getValue(AlchemyFurnaceBlock.FACING))
                .setValue(AlchemyFurnaceBlock.FORMED,old.getValue(AlchemyFurnaceBlock.FORMED));
        var snapshot=saveWithoutMetadata();
        upgradingCore=true;
        boolean changed;
        try { changed=level.setBlock(worldPosition,next,3); } finally { upgradingCore=false; }
        if(!changed)return false;
        if(!(level.getBlockEntity(worldPosition) instanceof AlchemyFurnaceBlockEntity replacement))throw new IllegalStateException("Missing upgraded furnace entity");
        replacement.load(snapshot);replacement.items.get(CORE_SLOT).shrink(1);replacement.sync();
        AlchemyFurnaceStructure.refreshAround(server,worldPosition);
        if(player instanceof net.minecraft.server.level.ServerPlayer owner)
            net.minecraftforge.network.NetworkHooks.openScreen(owner,replacement.menuProvider(),worldPosition);
        return true;
    }

    private static final String BREWING_TAG = "Brewing";
    private static final String START_TICK_TAG = "StartTick";
    private static final String FINISH_TICK_TAG = "FinishTick";
    private static final String VISUAL_SEED_TAG = "VisualSeed";

    private final NonNullList<ItemStack> items = NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY);

    private java.util.UUID brewingRequestId;
    public java.util.UUID brewingRequestId() { return brewingRequestId; }
    private boolean brewing;
    private int explosionWarningTicks;
    private java.util.UUID explosionOperator;
    public boolean explosionPending(){return explosionWarningTicks>0;}
    public boolean busy(){return brewing||explosionPending()||v5Fault;}
    public int explosionWarningTicks(){return explosionWarningTicks;}
    public boolean beginExplosionWarning(Player player){
        if(!(level instanceof ServerLevel)||busy()||hasResult()||!ignited()||!hasAnyMaterial())return false;
        for(int i=0;i<MATERIAL_SLOT_COUNT;i++)items.set(i,ItemStack.EMPTY);
        java.util.Arrays.fill(reservedCounts,0);
        explosionWarningTicks=60;explosionOperator=player==null?null:player.getUUID();sync();return true;
    }
    /** Invalid experiments consume the submitted batch and leave ash, without damaging the furnace. */
    public boolean failSubmittedBatch() {
        if (!(level instanceof ServerLevel) || busy() || hasResult() || !ignited() || !hasAnyMaterial()) return false;
        for (int i=0;i<MATERIAL_SLOT_COUNT;i++) reservedCounts[i]=items.get(i).getCount();
        startBrewing(level.getGameTime(),level.getGameTime()+1);
        return failExperiment();
    }
    public void clearExplosionWarning(){explosionWarningTicks=0;explosionOperator=null;}
    private long startTick;
    private long finishTick;
    private long visualSeed;
    private int outcome;
    private ItemStack completedResult = ItemStack.EMPTY;
    private String activeFireId = "";
    private long completionTick = -1;
    private boolean structureDirty=true;
    public void markStructureDirty(){structureDirty=true;}

    @Override public void onLoad() {
        super.onLoad();structureDirty=true;
        if(level!=null&&level.isClientSide)ClientPacketSink.furnaceLoaded.accept(this);
        if(level instanceof ServerLevel server){restoreV5();if(!v5Batch())cn.blockforge.generated.generatedmod.network.service.AlchemyService.wakeAt(server,worldPosition);}
    }
    private int[] reservedCounts = new int[MATERIAL_SLOT_COUNT];
    private cn.blockforge.generated.generatedmod.alchemy.auxiliary.AuxiliaryAlchemy.Plan auxiliaryPlan = new cn.blockforge.generated.generatedmod.alchemy.auxiliary.AuxiliaryAlchemy.Plan(java.util.List.of());
    public cn.blockforge.generated.generatedmod.alchemy.auxiliary.AuxiliaryAlchemy.Plan auxiliaryPlan(){return auxiliaryPlan;}

    public AlchemyFurnaceBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        visualSeed = pos.asLong() ^ 0x5A17C0DE;
    }

    public boolean brewing() { return brewing; }
    public long startTick() { return startTick; }
    public long finishTick() { return finishTick; }
    public long visualSeed() { return visualSeed; }
    public int outcome() { return outcome; }
    public ItemStack completedResult() { return v5Batch()||sealedV5Result()?completedResult.copy():completedResult; }
    public boolean hasResult() { return !completedResult.isEmpty(); }
    public boolean hasAshResult() { return completedResult.is(WuXingItems.PILL_ASH.get()); }
    public String activeFireId() { return activeFireId; }
    public float completionAge(float partialTick) {
        return completionTick < 0 || level == null ? AlchemyFurnaceVisualLayout.COMPLETE_TICKS
                : Math.max(0, level.getGameTime()-completionTick+partialTick);
    }
    public double[] reserve(AlchemyRecipe recipe) {
        if (busy() || hasResult()) throw new IllegalStateException("Alchemy furnace is occupied");
        var allocation=cn.blockforge.generated.generatedmod.alchemy.auxiliary.AuxiliaryAlchemy.allocate(this,recipe.reservation(this));
        reservedCounts=allocation.counts();auxiliaryPlan=allocation.plan();
        java.util.List<ItemStack> consumed=new java.util.ArrayList<>();
        for (int slot=0;slot<MATERIAL_SLOT_COUNT;slot++) if (reservedCounts[slot]>0)
            consumed.add(items.get(slot).copyWithCount(reservedCounts[slot]));
        return cn.blockforge.generated.generatedmod.fire.FireBottleData.medicinalEssence(consumed);
    }
    /** Copies only the batch reserved by the authoritative recipe, excluding idle input stacks. */
    public java.util.List<ItemStack> reservedMaterials() {
        var consumed=new java.util.ArrayList<ItemStack>();
        for(int slot=0;slot<MATERIAL_SLOT_COUNT;slot++)if(reservedCounts[slot]>0)
            consumed.add(items.get(slot).copyWithCount(reservedCounts[slot]));
        return consumed;
    }
    private boolean settleMaterials() {
        for (int slot=0;slot<MATERIAL_SLOT_COUNT;slot++)
            if(items.get(slot).getCount()<reservedCounts[slot])return false;
        for (int slot=0;slot<MATERIAL_SLOT_COUNT;slot++) {
            items.get(slot).shrink(reservedCounts[slot]);
            reservedCounts[slot]=0;
        }
        return true;
    }
    public boolean complete(ItemStack result) {
        if(v5Batch())return false;
        if (!brewing || hasResult() || result.isEmpty()) return false;
        if (!settleMaterials()) { finishBrewing(false); return false; }
        completedResult=result.copy();
        completionTick=level.getGameTime();
        brewing=false; outcome=1; startTick=0; finishTick=0;
        sync(); return true;
    }
    /** Actual failure consumes the reserved batch and leaves one collectible ash; cancellation releases it. */
    public boolean failExperiment() {
        if(v5Batch())return false;
        if (!brewing || hasResult()) return false;
        if(!settleMaterials()){finishBrewing(false);return false;}
        completedResult=new ItemStack(rankedFailure?WuXingItems.WASTE_PILL.get():WuXingItems.PILL_ASH.get());
        completionTick=level.getGameTime();brewing=false;outcome=2;startTick=finishTick=0;
        sync();return true;
    }

    public int tier() {
        return getBlockState().getBlock() instanceof AlchemyFurnaceBlock furnace ? furnace.tier() : 0;
    }

    /** 点火槽中的异火品阶；0 表示未点火（空瓶或空槽）。 */
    public int ignitionTier() {
        ItemStack stack = items.get(IGNITION_SLOT);
        return stack.getCount() == 1 ? cn.blockforge.generated.generatedmod.fire.FireBottleData.tier(stack) : 0;
    }

    public boolean ignited() { return ignitionTier() >= 1; }

    /** 八个素材槽是否为空的快捷判断。 */
    public boolean hasAnyMaterial() {
        for (int i = MATERIAL_SLOT_START; i < MATERIAL_SLOT_START + MATERIAL_SLOT_COUNT; i++) {
            if (!items.get(i).isEmpty()) return true;
        }
        return false;
    }

    /** 炼制开始：记录起止 tick，炉顶开始冒丹气。 */
    public void startBrewing(long start, long finish) {
        if (hasResult() || busy()) throw new IllegalStateException("Alchemy furnace is occupied");
        activeFireId=cn.blockforge.generated.generatedmod.fire.FireBottleData.view(items.get(IGNITION_SLOT)).getString("fire_id");
        rankedFailure=false;
        completionTick=-1;
        brewingRequestId = java.util.UUID.randomUUID();
        brewing = true;
        needsSpiritFuel=false;poweredTicks=0;fuelTicks=0;
        outcome = 0;
        startTick = start;
        finishTick = Math.max(start + 1L, finish);
        sync();
    }

    /** 炼制完成或被取消：停止丹气。 */
    public void finishBrewing() {
        finishBrewing(false);
    }

    public void finishBrewing(boolean completed) {
        if(v5Batch()){sealV5Batch();return;}
        clearExplosionWarning();
        // Cancellation releases reservations; only successful completion consumes ingredients.
        java.util.Arrays.fill(reservedCounts,0);
        brewing = false;
        outcome = hasResult() ? 1 : 2;
        if (!hasResult()) { activeFireId=""; completionTick=-1; }
        startTick = 0L;
        finishTick = 0L;
        sync();
    }

    /** 炼制进度（0-1），供界面进度条使用；未炼制时返回 0。 */
    public float brewingProgress() {
        if (!brewing || level == null || finishTick <= startTick) return 0.0F;
        float t = (needsSpiritFuel||v5Batch() ? poweredTicks : level.getGameTime() - startTick) / (float) (finishTick - startTick);
        return Math.max(0.0F, Math.min(1.0F, t));
    }

    /** 打开八卦炼丹面板的菜单提供者。 */
    public MenuProvider menuProvider() {
        return new SimpleMenuProvider(
                (id, inventory, player) -> new AlchemyFurnaceMenu(id, inventory, this),
                AlchemyFurnaceBlock.tierName(tier()));
    }

    /** 客户端持续特效：炼制时炉顶冒出彩色灵光与丹气。 */
    public static void tick(Level level, BlockPos pos, BlockState state, AlchemyFurnaceBlockEntity furnace) {
        if (!level.isClientSide) {
            if(furnace.v5Batch()||furnace.v5Fault){furnace.tickV5();return;}
            if(furnace.explosionPending()) {
                if(furnace.explosionWarningTicks==1) {
                    var operator=furnace.explosionOperator==null?null:((ServerLevel)level).getServer().getPlayerList().getPlayer(furnace.explosionOperator);
                    cn.blockforge.generated.generatedmod.alchemy.FurnaceExplosion.detonate(furnace,operator);
                } else {furnace.explosionWarningTicks--;furnace.setChanged();if(furnace.explosionWarningTicks%4==0)furnace.sync();}
                return;
            }
            if (furnace.brewing && !Network.hasAlchemyJob(level, pos)) furnace.finishBrewing(false);
            furnace.advancePoweredTick();
            if (furnace.hasResult() && level.getGameTime()%8==0
                    && furnace.completionAge(0)>=AlchemyFurnaceVisualLayout.COMPLETE_TICKS) furnace.collectNearby();
            // Block events coalesce into one refresh; retain a staggered fallback for bulk world edits.
            if (furnace.structureDirty || (level.getGameTime() + pos.asLong()) % 200L == 0L) {
                furnace.structureDirty=false;
                AlchemyFurnaceStructure.refreshAround(level, pos);
            }
            return;
        }
        if(furnace.explosionPending())furnace.explosionWarningTicks--;
        if (AlchemyFurnaceBlock.isFormed(state) && furnace.hasAshResult() && level.getGameTime()%4==0) {
            var mouth=AlchemyFurnaceVisualLayout.world(pos,AlchemyFurnaceStructure.inwardOf(state),AlchemyFurnaceVisualLayout.MOUTH);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.SMOKE,
                    mouth.x+(level.random.nextDouble()-.5)*.24,mouth.y+.06,
                    mouth.z+(level.random.nextDouble()-.5)*.24,0,.035,0);
        }
    }

    private void collectNearby() {
        if(sealedV5Result())return;
        if (!(level instanceof ServerLevel server)) return;
        var mouth=AlchemyFurnaceVisualLayout.world(worldPosition,AlchemyFurnaceStructure.inwardOf(getBlockState()),AlchemyFurnaceVisualLayout.MOUTH);
        var area=new AABB(mouth,mouth).inflate(AlchemyFurnaceVisualLayout.PICKUP_RADIUS);
        for (var player:server.getEntitiesOfClass(net.minecraft.server.level.ServerPlayer.class,area,p->p.isAlive()&&!p.isSpectator())) {
            if(player.containerMenu instanceof AlchemyFurnaceMenu menu&&menu.furnace()==this)continue;
            if (player.getEyePosition().distanceToSqr(mouth)>AlchemyFurnaceVisualLayout.PICKUP_RADIUS*AlchemyFurnaceVisualLayout.PICKUP_RADIUS) continue;
            int before=completedResult.getCount();
            var inventory=player.getInventory();
            insertResult(inventory.items,inventory.getMaxStackSize());
            if (completedResult.getCount()==before) continue;
            inventory.setChanged(); player.containerMenu.broadcastChanges();
            server.playSound(null,worldPosition,net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.BLOCKS,.3F,1.2F);
            if (!hasResult()) { completedResult=ItemStack.EMPTY; completionTick=-1; activeFireId=""; outcome=0; }
            sync();
            if (!hasResult()) break;
        }
    }

    /** Server-only transaction; no creative-mode deletion of uninserted remainder. */
    public int collectResult(Player player){
        if(sealedV5Result())return collectV5(player);
        if(!(level instanceof ServerLevel)||!stillValid(player)||brewing||!hasResult()||!player.isAlive()||player.isSpectator())return 0;
        int moved=insertResult(player.getInventory().items,player.getInventory().getMaxStackSize());
        if(moved>0){player.getInventory().setChanged();if(!hasResult()){completedResult=ItemStack.EMPTY;completionTick=-1;activeFireId="";outcome=0;}sync();player.containerMenu.broadcastChanges();}
        return moved;
    }
    /** Explicit UI pickup; preserve the ordinary cursor item and all untransferred remainder. */
    public int collectResultToCursor(Player player,net.minecraft.world.inventory.AbstractContainerMenu menu){
        if(sealedV5Result())return player.containerMenu==menu?collectV5(player):0;
        if(!(level instanceof ServerLevel)||player.containerMenu!=menu||!stillValid(player)||brewing||!hasResult()||!player.isAlive()||player.isSpectator())return 0;
        ItemStack carried=menu.getCarried();if(!carried.isEmpty()&&!ItemStack.isSameItemSameTags(carried,completedResult))return 0;
        int moved=Math.min(completedResult.getCount(),Math.max(0,completedResult.getMaxStackSize()-carried.getCount()));
        if(moved==0)return 0;
        var taken=completedResult.split(moved);if(carried.isEmpty())menu.setCarried(taken);else{carried.grow(moved);menu.setCarried(carried);}
        if(!hasResult()){completedResult=ItemStack.EMPTY;completionTick=-1;activeFireId="";outcome=0;}
        sync();menu.broadcastChanges();return moved;
    }
    int insertResult(java.util.List<ItemStack> inventory,int stackLimit) {
        int before=completedResult.getCount();
        for (int pass=0;pass<2 && hasResult();pass++) for (int slot=0;slot<inventory.size() && hasResult();slot++) {
            ItemStack target=inventory.get(slot);
            if (pass==0 && (target.isEmpty() || !ItemStack.isSameItemSameTags(target,completedResult))) continue;
            if (pass==1 && !target.isEmpty()) continue;
            int capacity=Math.min(stackLimit,completedResult.getMaxStackSize())-target.getCount();
            int moved=Math.min(completedResult.getCount(),Math.max(0,capacity));
            if (moved==0) continue;
            if (target.isEmpty()) inventory.set(slot,completedResult.split(moved));
            else { target.grow(moved); completedResult.shrink(moved); }
        }
        return before-completedResult.getCount();
    }

    public void sync() {
        setChanged();
        if (level instanceof ServerLevel server) {
            server.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    @Override
    public AABB getRenderBoundingBox() {
        return new AABB(worldPosition).inflate(4.0D, 4.5D, 4.0D);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        tag.remove("V5Ref");
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        load(tag);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        if(v5Ref!=null)tag.putUUID("V5Ref",v5Ref);tag.putBoolean("V5Mode",v5Batch());
        tag.putBoolean(BREWING_TAG, brewing);
        tag.putInt("V5DeviceWear",v5DeviceWear);
        tag.putInt("ExplosionWarningTicks",explosionWarningTicks);
        if(explosionOperator!=null)tag.putUUID("ExplosionOperator",explosionOperator);
        tag.putInt("SpiritFuel",spiritFuel);tag.putInt("PoweredTicks",poweredTicks);tag.putInt("FuelTicks",fuelTicks);
        tag.putBoolean("NeedsSpiritFuel",needsSpiritFuel);
        tag.putBoolean("RankedFailure",rankedFailure);
        if (brewingRequestId != null) tag.putUUID("BrewingRequestId", brewingRequestId);
        tag.putLong(START_TICK_TAG, startTick);
        tag.putLong(FINISH_TICK_TAG, finishTick);
        tag.putLong(VISUAL_SEED_TAG, visualSeed);
        tag.putInt("AlchemyOutcome", outcome);
        tag.putLong("CompletionTick",completionTick);
        tag.put("CompletedResult",completedResult.save(new CompoundTag()));
        tag.putString("ActiveFireId",activeFireId);
        tag.putIntArray("ReservedCounts",reservedCounts);
        tag.put("AuxiliaryPlan",auxiliaryPlan.save());
        ContainerHelper.saveAllItems(tag, items);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        v5Ref=tag.hasUUID("V5Ref")?tag.getUUID("V5Ref"):null;v5Client=tag.getBoolean("V5Mode");v5=new CompoundTag();v5Fault=false;
        brewing = tag.getBoolean(BREWING_TAG);
        v5DeviceWear=Math.max(0,Math.min(1000,tag.getInt("V5DeviceWear")));
        explosionWarningTicks=Math.max(0,Math.min(60,tag.getInt("ExplosionWarningTicks")));
        explosionOperator=tag.hasUUID("ExplosionOperator")?tag.getUUID("ExplosionOperator"):null;
        spiritFuel=Math.max(0,Math.min(spiritCapacity(),tag.getInt("SpiritFuel")));
        poweredTicks=Math.max(0,tag.getInt("PoweredTicks"));fuelTicks=Math.max(0,Math.min(19,tag.getInt("FuelTicks")));
        needsSpiritFuel=tag.getBoolean("NeedsSpiritFuel");
        rankedFailure=tag.getBoolean("RankedFailure");
        brewingRequestId = tag.hasUUID("BrewingRequestId") ? tag.getUUID("BrewingRequestId") : null;
        startTick = tag.getLong(START_TICK_TAG);
        finishTick = tag.getLong(FINISH_TICK_TAG);
        outcome = Math.max(0, Math.min(2, tag.getInt("AlchemyOutcome")));
        completedResult=ItemStack.of(tag.getCompound("CompletedResult"));
        activeFireId=tag.getString("ActiveFireId");
        completionTick=tag.contains("CompletionTick") ? tag.getLong("CompletionTick") : -1;
        auxiliaryPlan=cn.blockforge.generated.generatedmod.alchemy.auxiliary.AuxiliaryAlchemy.Plan.load(tag.getCompound("AuxiliaryPlan"));
        int[] saved=tag.getIntArray("ReservedCounts");
        reservedCounts=new int[MATERIAL_SLOT_COUNT];
        for (int i=0;i<Math.min(saved.length,MATERIAL_SLOT_COUNT);i++) reservedCounts[i]=Math.max(0,Math.min(64,saved[i]));
        visualSeed = tag.contains(VISUAL_SEED_TAG) ? tag.getLong(VISUAL_SEED_TAG) : worldPosition.asLong() ^ 0x5A17C0DE;
        // Update packets omit empty slots; clear old client contents before applying the snapshot.
        for (int slot=0;slot<CONTAINER_SIZE;slot++) items.set(slot,ItemStack.EMPTY);
        ContainerHelper.loadAllItems(tag, items);
    }

    // ---- Container (八卦素材槽 + 异火瓶点火槽) ----

    @Override public int getContainerSize() { return CONTAINER_SIZE; }

    @Override public boolean canPlaceItem(int index, ItemStack stack) {
        if (inventoryLocked() || index < 0 || index >= CONTAINER_SIZE) return false;
        if (index == IGNITION_SLOT) return !brewing && stack.getCount() == 1
                && cn.blockforge.generated.generatedmod.fire.FireBottleData.tier(stack) > 0;
        if(index==CORE_SLOT)return stack.getItem() instanceof net.minecraft.world.item.BlockItem item
                && item.getBlock() instanceof AlchemyFurnaceBlock core && core.tier()>tier();
        return !(stack.getItem() instanceof cn.blockforge.generated.generatedmod.items.FireSeedItem);
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) if (!stack.isEmpty()) return false;
        return true;
    }

    @Override
    public ItemStack getItem(int index) {
        return index >= 0 && index < CONTAINER_SIZE ? (v5Batch()?items.get(index).copy():items.get(index)) : ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeItem(int index, int count) {
        if (inventoryLocked()) return ItemStack.EMPTY;
        if (index < 0 || index >= CONTAINER_SIZE) return ItemStack.EMPTY;
        ItemStack stack = items.get(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        ItemStack removed = stack.split(count);
        sync();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int index) {
        if (inventoryLocked()) return ItemStack.EMPTY;
        if (index < 0 || index >= CONTAINER_SIZE) return ItemStack.EMPTY;
        ItemStack stack = items.get(index);
        if (stack.isEmpty()) return ItemStack.EMPTY;
        items.set(index, ItemStack.EMPTY);
        sync();
        return stack;
    }

    @Override
    public void setItem(int index, ItemStack stack) {
        if (inventoryLocked()) return;
        if (index < 0 || index >= CONTAINER_SIZE) return;
        items.set(index, stack.isEmpty() ? ItemStack.EMPTY : stack);
        sync();
    }

    @Override
    public boolean stillValid(Player player) {
        if (level == null || player.level() != level || level.getBlockEntity(worldPosition) != this) return false;
        return player.distanceToSqr(worldPosition.getX() + 0.5D, worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D) <= 64.0D;
    }

    @Override
    public void clearContent() {
        if (inventoryLocked()) return;
        for (int slot = 0; slot < CONTAINER_SIZE; slot++) items.set(slot, ItemStack.EMPTY);
        sync();
    }
}
