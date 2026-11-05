package cn.blockforge.generated.generatedmod.network.service;

import cn.blockforge.generated.generatedmod.registry.ModRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;
import cn.blockforge.generated.generatedmod.*;
import static cn.blockforge.generated.generatedmod.Network.*;
import cn.blockforge.generated.generatedmod.network.packet.alchemy.AlchemyPacket;
import cn.blockforge.generated.generatedmod.network.packet.alchemy.RefiningPacket;
import cn.blockforge.generated.generatedmod.network.packet.alchemy.CollectRefiningPacket;
import cn.blockforge.generated.generatedmod.alchemy.batch.BatchStartResult;


public final class AlchemyService {
    private record ForgeSession(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension,BlockPos pos,long until) {}
    private static final Map<UUID,ForgeSession> FORGE_SESSIONS=new HashMap<>();
    public static void openRefining(ServerPlayer player,BlockPos pos){
        FORGE_SESSIONS.put(player.getUUID(),new ForgeSession(player.level().dimension(),pos.immutable(),player.server.overworld().getGameTime()+12000));
    }
    private static boolean validRefiningSession(ServerPlayer player,BlockPos pos){
        var session=FORGE_SESSIONS.get(player.getUUID());
        boolean valid=player.isAlive()&&!player.isSpectator()&&session!=null&&session.dimension().equals(player.level().dimension())
                &&session.pos().equals(pos)&&session.until()>=player.server.overworld().getGameTime()
                &&player.distanceToSqr(pos.getX()+.5,pos.getY()+.5,pos.getZ()+.5)<=64;
        if(valid)openRefining(player,pos);
        else player.displayClientMessage(Component.translatable("message.wuxingxiantu.refining_reopen"),true);
        return valid;
    }


        public static void handleAlchemyPacket(AlchemyPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null) return;
                if (!(player.level() instanceof ServerLevel level) || !player.blockPosition().closerThan(packet.pos(), 8.0D)) return;
                if (!(player.containerMenu instanceof AlchemyFurnaceMenu menu)
                        || !menu.blockPos().equals(packet.pos()) || !menu.stillValid(player)) return;
                AlchemyFurnaceStructure.refreshAround(level,packet.pos());
                if (!(level.getBlockState(packet.pos()).getBlock() instanceof AlchemyFurnaceBlock furnace)
                        ||!AlchemyFurnaceBlock.isFormed(level.getBlockState(packet.pos()))) return;
                if (!(level.getBlockEntity(packet.pos()) instanceof AlchemyFurnaceBlockEntity furnaceEntity)) return;
                if (furnaceEntity.hasResult()) {
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.alchemy_take_result"),true);
                    return;
                }
                if (furnaceEntity.busy()) {
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.alchemy_busy"), true);
                    return;
                }
                if (isAlchemyBusy(player)) {
                    player.displayClientMessage(Component.literal("炼丹任务繁忙："+CraftingJobs.get(player.server).describe(player.getUUID(),player.server.overworld().getGameTime())), true);
                    return;
                }
                var positionJob=CraftingJobs.get(player.server).alchemy(new CraftingJobs.JobKey(level.dimension(),packet.pos()));
                if(positionJob!=null) {
                    wakeAt(level,packet.pos());
                    player.displayClientMessage(Component.literal("该丹炉还有旧任务待结算，请稍后重试。"),true);
                    return;
                }
                boolean free = cn.blockforge.generated.generatedmod.alchemy.FreeAlchemy.ID.equals(packet.recipeId());
                if(free!=menu.freeMode())return;
                if(free){reportAlchemyStart(player,furnaceEntity.tryStartV5Batch(player,null));return;}
                AlchemyRecipe recipe=level.getRecipeManager().getAllRecipesFor(ModRecipes.ALCHEMY_TYPE.get()).stream()
                        .filter(candidate->candidate.getId().equals(packet.recipeId())).findFirst().orElse(null);
                if(recipe!=null)reportAlchemyStart(player,furnaceEntity.tryStartV5Batch(player,recipe));

            });
            context.setPacketHandled(true);
        }

        public static void handleRefiningPacket(RefiningPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !(player.level() instanceof ServerLevel level)) return;
                // 所有拒绝路径都必须出声：静默 return 会让玩家以为按钮坏了。
                if (!player.blockPosition().closerThan(packet.pos(), 8.0D)) {
                    player.displayClientMessage(Component.translatable(
                            "message.wuxingxiantu.refining_too_far"), true);
                    return;
                }
                if (!RefiningForgeBlock.isStructureComplete(level, packet.pos())) {
                    player.displayClientMessage(Component.translatable(
                            "message.wuxingxiantu.refining_forge_incomplete"), true);
                    return;
                }
                if(!validRefiningSession(player,packet.pos()))return;
                CraftingJobs jobs=CraftingJobs.get(player.server);
                if(!(level.getBlockEntity(packet.pos()) instanceof RefiningForgeBlockEntity forge)
                        ||forge.hasPendingReward()||forge.isCrafting())return;
                boolean forgeBusy=jobs.activeCount(player.getUUID(),CraftingJobs.Kind.REFINING,
                        player.server.overworld().getGameTime())>=cn.blockforge.generated.generatedmod.balance.GameplayConfig.MAX_ACTIVE_REFINING_JOBS.get()
                        ||jobs.refining(new CraftingJobs.JobKey(level.dimension(),packet.pos()))!=null;
                if (forgeBusy) {
                    player.displayClientMessage(Component.literal("炼器任务繁忙："+jobs.describe(player.getUUID(),player.server.overworld().getGameTime())), true);
                    return;
                }
                RefiningRecipe recipe = level.getRecipeManager().getAllRecipesFor(ModRecipes.REFINING_TYPE.get()).stream()
                        .filter(candidate -> candidate.getId().equals(packet.recipeId())).findFirst().orElse(null);
                if (recipe == null) {
                    player.displayClientMessage(Component.translatable(
                            "message.wuxingxiantu.refining_recipe_gone"), true);
                    return;
                }
                int fireTier = RefiningForgeBlock.fireTier(level, packet.pos());
                if (fireTier < recipe.requiredFireTier()) {
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.refining_fire_low",
                            recipe.requiredFireTier(), fireTier), true);
                    return;
                }
                // 统一进度门槛：炼器同样校验实际操作者本人；火候（由炉心异火决定）越高，
                // 要求操作者境界越高，杜绝借用他人高阶异火跨阶炼器。
                if (!ProgressionGate.canUseRefiningForge(player, recipe.temperature())) {
                    ProgressionGate.deny(player, ProgressionGate.MSG_FORGE,
                            ProgressionGate.requiredRealmForRefiningTemperature(recipe.temperature()));
                    return;
                }
                int forgeFireTier = RefiningForgeBlock.fireTier(level, packet.pos());
                if (forgeFireTier > 0 && !ProgressionGate.canUseFire(player, forgeFireTier)) {
                    ProgressionGate.denyFireUse(player, forgeFireTier);
                    return;
                }
                CultivationData data = GeneratedMod.data(player);
                if (data == null) return;
                int refiningCost=cn.blockforge.generated.generatedmod.progression.PlayerCrafts.refiningCost(recipe.auraCost(),cn.blockforge.generated.generatedmod.progression.PlayerCrafts.bonus(player,"refining"));
                if (data.wuxingxiantu$getAura() < refiningCost) {
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.refining_aura_missing",
                            refiningCost), true);
                    return;
                }
                // 材料只从承华台抽取（不再回落到背包）；不足时逐项点名缺什么、缺多少。
                PedestalContainer pedestals = PedestalContainer.around(level, packet.pos());
                java.util.List<int[]> missing = recipe.missing(pedestals);
                if (!missing.isEmpty()) {
                    // 服务端权威重同步：把检索范围内所有承华台的最新承载推给客户端，
                    // 界面随即按真实状态重算，不会再停留在“一切就绪”。
                    for (BlockPos probe : BlockPos.betweenClosed(
                            packet.pos().offset(-PedestalContainer.SEARCH_RADIUS, -1, -PedestalContainer.SEARCH_RADIUS),
                            packet.pos().offset(PedestalContainer.SEARCH_RADIUS, 2, PedestalContainer.SEARCH_RADIUS))) {
                        if (ChenghuaPedestalBlock.isPedestal(level.getBlockState(probe))) {
                            ChenghuaPedestalBlock.refresh(level, probe);
                        }
                    }
                    player.displayClientMessage(refiningMissingDetail(recipe, missing,
                            pedestals.getContainerSize()), true);
                    return;
                }
                var preview=previewRefiningMaterials(pedestals,recipe);
                var inherited=cn.blockforge.generated.generatedmod.crafting.EquipmentCrafting.inherit(
                        recipe.getResultItem(level.registryAccess()).copy(),preview);
                if(!cn.blockforge.generated.generatedmod.crafting.EquipmentBond.canConsume(player,preview)||inherited.isEmpty()){
                    player.displayClientMessage(Component.translatable("message.wuxingxiantu.refining_foreign_owner"),true);
                    return;
                }
                var materials=consumeFromPedestals(level, pedestals, recipe, packet.pos());
                data.wuxingxiantu$setAura(data.wuxingxiantu$getAura() - refiningCost);
                RefiningForgeBlock.setStructureLit(level, packet.pos(), true);
                long startTick = level.getGameTime();
                long finishTick = startTick + recipe.craftTime();
                // 把炼制进度写进方块实体并同步到客户端，界面据此绘制百分比进度条。
                if (level.getBlockEntity(packet.pos()) instanceof RefiningForgeBlockEntity forgeEntity) {
                    forgeEntity.startCrafting(startTick, finishTick, recipe.getId().toString(), player.getUUID());
                }
                jobs.add(new CraftingJobs.PendingRefining(
                        player.getUUID(),level.dimension(), packet.pos().immutable(), finishTick,
                        inherited,materials,refiningCost));
                jobs.setDirty();
                player.displayClientMessage(Component.translatable("message.wuxingxiantu.refining_offered",
                        recipe.craftTime() / 20), true);
                GeneratedMod.sync(player);
            });
            context.setPacketHandled(true);
        }

        public static void handleCollectRefiningPacket(CollectRefiningPacket packet, Supplier<NetworkEvent.Context> supplier) {
            NetworkEvent.Context context = supplier.get();
            context.enqueueWork(() -> {
                ServerPlayer player = context.getSender();
                if (player == null || !(player.level() instanceof ServerLevel level)) return;
                if (!player.blockPosition().closerThan(packet.pos(), 8.0D)) {
                    player.displayClientMessage(Component.translatable(
                            "message.wuxingxiantu.refining_too_far"), true);
                    return;
                }
                if (!validRefiningSession(player,packet.pos()))return;
                if (!(level.getBlockEntity(packet.pos()) instanceof RefiningForgeBlockEntity forge)
                        || !forge.hasPendingReward()) {
                    player.displayClientMessage(Component.translatable(
                            "message.wuxingxiantu.refining_not_ready"), true);
                    return;
                }
                ItemStack result = forge.takePendingReward();
                // 熄灭炉体亮态并同步客户端，无论成品是否为空，都让界面回到"开始炼器"、悬浮成品消失。
                RefiningForgeBlock.setStructureLit(level, packet.pos(), false);
                if (result.isEmpty()) return;
                // 先取成品名再入包：Inventory.add 对带耐久的物品会走 copyAndClear，把传入栈清成空气，
                // 若之后再 result.getHoverName() 就会读出"空气"。成品名必须在 add 之前捕获。
                net.minecraft.network.chat.Component rewardName = result.getHoverName();
                if (!player.getInventory().add(result)) {
                    player.drop(result, false);
                }
                player.displayClientMessage(Component.translatable(
                        "message.wuxingxiantu.refining_finished", rewardName), true);
            });
            context.setPacketHandled(true);
        }

    /** Notification happens after admission. Failure to show a message cannot undo its result. */
    public static void reportAlchemyStart(ServerPlayer player,BatchStartResult result) {
        Component message;
        if(result.needsRecovery()){
            message=Component.translatable("message.wuxingxiantu.alchemy_start_recovery",result.batchId().toString());
        }else if(result.reason()==BatchStartResult.Reason.PREPARATION_FAILED){
            message=Component.literal("未能开炉："+result.detail());
        }else{
            message=Component.translatable("message.wuxingxiantu.alchemy_start_"+result.reason().name().toLowerCase(java.util.Locale.ROOT));
        }
        try{player.displayClientMessage(message,true);}
        catch(RuntimeException error){com.mojang.logging.LogUtils.getLogger().warn("V5 admission feedback failed: reason={}, batch={}",result.reason(),result.batchId(),error);}
    }

    public static void tickAlchemy(net.minecraft.server.MinecraftServer server) {
        var jobs=CraftingJobs.get(server);long now=server.overworld().getGameTime();
        for(int budget=0;budget<256;budget++) {
            var due=jobs.poll(now);if(due==null)break;
            boolean done;
            if(due.kind()==CraftingJobs.Kind.ALCHEMY) {
                var job=jobs.alchemy(due.key());if(job==null)continue;
                done=settleAlchemy(server,jobs,job);
                if(!done){var jobLevel=server.getLevel(job.dimension());
                    jobs.schedule(due.kind(),due.key(),now+(jobLevel==null||!jobLevel.hasChunkAt(job.furnacePos())?200:20));}
            } else {
                var job=jobs.refining(due.key());if(job==null)continue;
                done=settleRefining(server,jobs,job);
                if(!done)jobs.schedule(due.kind(),due.key(),Math.max(now+200,job.finishTick()));
            }
            if(done)jobs.remove(due.kind(),due.key());
        }
    }
    private static boolean settleAlchemy(net.minecraft.server.MinecraftServer server,CraftingJobs jobs,CraftingJobs.PendingAlchemy job) {
            var level=server.getLevel(job.dimension());
            if(level==null||!level.hasChunkAt(job.furnacePos()))return false;
            if(!(level.getBlockEntity(job.furnacePos()) instanceof AlchemyFurnaceBlockEntity furnace)){
                jobs.refund(job.owner(),job.aura());return true;
            }
            if(furnace.hasResult()&&job.requestId().equals(furnace.brewingRequestId()))return true;
            if(!job.requestId().equals(furnace.brewingRequestId())||!furnace.brewing()){
                jobs.refund(job.owner(),job.aura());return true;
            }
            ItemStack bottle=furnace.getItem(AlchemyFurnaceBlockEntity.IGNITION_SLOT);
            var bottleTag=cn.blockforge.generated.generatedmod.fire.FireBottleData.view(bottle);
            boolean valid=AlchemyFurnaceBlock.isFormed(furnace.getBlockState())&&bottle.getCount()==1
                    &&cn.blockforge.generated.generatedmod.fire.FireBottleData.tier(bottle)==job.fireTier()
                    &&bottleTag.hasUUID("BottleId")&&bottleTag.getUUID("BottleId").equals(job.bottleId());
            if(!valid){furnace.finishBrewing(false);jobs.refund(job.owner(),job.aura());return true;}
            if(furnace.needsSpiritFuel() ? !furnace.poweredComplete() : level.getGameTime()<job.finishTick())return false;
            if(job.result().isEmpty()) {
                if (!furnace.failExperiment()) jobs.refund(job.owner(),job.aura());
                var owner=server.getPlayerList().getPlayer(job.owner());
                if(owner!=null)owner.displayClientMessage(Component.translatable("message.wuxingxiantu.alchemy_failed_residue",furnace.completedResult().getHoverName()),true);
                return true;
            }
            if(!furnace.complete(job.result())){furnace.finishBrewing(false);jobs.refund(job.owner(),job.aura());return true;}
            cn.blockforge.generated.generatedmod.fire.FireBottleData.reward(bottle,job.essence(),job.formula(),
                    job.multiplier(),level.getGameTime(),job.dimension().location().toString());
            furnace.sync();
            cn.blockforge.generated.generatedmod.progression.PlayerCrafts.get(server).receipt(job.owner(),"alchemy",job.result(),cn.blockforge.generated.generatedmod.progression.PlayerCrafts.itemTier(job.result()));
            var owner=server.getPlayerList().getPlayer(job.owner());
            if(owner!=null)owner.displayClientMessage(Component.translatable("message.wuxingxiantu.alchemy_finished",job.result().getHoverName()),true);
            return true;

    }
    private static boolean settleRefining(net.minecraft.server.MinecraftServer server,CraftingJobs jobs,CraftingJobs.PendingRefining job) {
            var level=server.getLevel(job.dimension());
            if(level==null||!level.hasChunkAt(job.corePos()))return false;
            var entity=level.getBlockEntity(job.corePos());
            if(entity instanceof RefiningForgeBlockEntity forge&&forge.hasPendingReward())return true;
            if(!(entity instanceof RefiningForgeBlockEntity)||!RefiningForgeBlock.isStructureComplete(level,job.corePos())){
                for(var stack:job.materials())net.minecraft.world.Containers.dropItemStack(level,
                        job.corePos().getX()+.5,job.corePos().getY()+1,job.corePos().getZ()+.5,stack.copy());
                jobs.refund(job.owner(),job.aura());
                if(entity instanceof RefiningForgeBlockEntity forge)forge.finishCrafting();
                RefiningForgeBlock.setStructureLit(level,job.corePos(),false);return true;
            }
            if(level.getGameTime()<job.finishTick())return false;
            var forge=(RefiningForgeBlockEntity)entity;
            var operated=forge.finishOperatedResult(job.result());
            operated=cn.blockforge.generated.generatedmod.importedcreatures.BeastMaterialEffects.infuseRefining(operated,job.materials());
            operated=cn.blockforge.generated.generatedmod.artifacts.ArtifactData.infuseRefining(operated,job.materials());
            forge.setPendingReward(operated);RefiningForgeBlock.setStructureLit(level,job.corePos(),false);
            cn.blockforge.generated.generatedmod.progression.PlayerCrafts.get(server).receipt(job.owner(),"refining",operated,cn.blockforge.generated.generatedmod.progression.PlayerCrafts.itemTier(operated));
            var owner=server.getPlayerList().getPlayer(job.owner());
            if(owner!=null)owner.displayClientMessage(Component.translatable("message.wuxingxiantu.refining_ready_collect",operated.getHoverName()),true);
            return true;

    }

    /** Retry only online owners, at login and on the low-frequency server tick. Unpaid balance stays saved. */
    public static void deliverRefund(ServerPlayer player) {
        var jobs=CraftingJobs.get(player.server);Float amount=jobs.refunds.get(player.getUUID());
        var data=GeneratedMod.data(player);if(amount==null||data==null)return;
        float before=data.wuxingxiantu$getAura();data.wuxingxiantu$setAura(before+amount);
        float restored=Math.max(0,data.wuxingxiantu$getAura()-before);
        if(restored<=0)return;
        float left=Math.max(0,amount-restored);
        if(left==0)jobs.refunds.remove(player.getUUID());else jobs.refunds.put(player.getUUID(),left);
        jobs.setDirty();GeneratedMod.sync(player);
    }
    public static void wakeAt(ServerLevel level,BlockPos pos) {
        var jobs=CraftingJobs.get(level.getServer());var key=new CraftingJobs.JobKey(level.dimension(),pos);
        long now=level.getServer().overworld().getGameTime();
        jobs.schedule(CraftingJobs.Kind.ALCHEMY,key,now);jobs.schedule(CraftingJobs.Kind.REFINING,key,now);
    }
    public static void cancelAlchemyAt(ServerLevel level,BlockPos pos) {
        var jobs=CraftingJobs.get(level.getServer());var key=new CraftingJobs.JobKey(level.dimension(),pos);
        var job=jobs.alchemy(key);
        if(job!=null){jobs.refund(job.owner(),job.aura());jobs.remove(CraftingJobs.Kind.ALCHEMY,key);}
        if(level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace&&furnace.busy())furnace.finishBrewing(false);
    }
    public static void failAlchemyFuelAt(ServerLevel level,BlockPos pos) {
        if(!(level.getBlockEntity(pos) instanceof AlchemyFurnaceBlockEntity furnace) || !furnace.brewing())return;
        var jobs=CraftingJobs.get(level.getServer());var key=new CraftingJobs.JobKey(level.dimension(),pos);
        var job=jobs.alchemy(key);
        var request=furnace.brewingRequestId();
        furnace.failExperiment();
        if(job!=null && job.requestId().equals(request)) {
            jobs.remove(CraftingJobs.Kind.ALCHEMY,key);
            var owner=level.getServer().getPlayerList().getPlayer(job.owner());
            if(owner!=null)owner.displayClientMessage(Component.literal("炼丹失败：注灵耗尽，异火熄灭；本炉材料与开炉灵力已消耗。"),true);
        }
    }
    public static boolean isAlchemyBusy(net.minecraft.world.entity.player.Player player) {
        return player instanceof ServerPlayer server&&CraftingJobs.get(server.server).activeCount(player.getUUID(),
                CraftingJobs.Kind.ALCHEMY,server.server.overworld().getGameTime())
                +new cn.blockforge.generated.generatedmod.alchemy.batch.BatchJournal(server.server).activeCount(player.getUUID())
                >=cn.blockforge.generated.generatedmod.balance.GameplayConfig.MAX_ACTIVE_ALCHEMY_JOBS.get();
    }
    public static boolean hasAlchemyJob(net.minecraft.world.level.Level level,BlockPos pos) {
        return level.getServer()!=null&&CraftingJobs.get(level.getServer()).alchemy(new CraftingJobs.JobKey(level.dimension(),pos))!=null;
    }
    public static void clearRefining(ServerPlayer player){FORGE_SESSIONS.remove(player.getUUID());}
    public static void clearPending(net.minecraft.server.MinecraftServer server){FORGE_SESSIONS.clear();}

    private AlchemyService() {}
        private static Component refiningMissingDetail(RefiningRecipe recipe, java.util.List<int[]> missing,
                                                       int foundPedestals) {
            net.minecraft.network.chat.MutableComponent list = Component.empty();
            for (int k = 0; k < missing.size(); k++) {
                int[] entry = missing.get(k);
                ItemStack sample = recipe.sample(entry[0]);
                Component part = (sample.isEmpty()
                        ? Component.translatable("screen.wuxingxiantu.refining.unknown_material")
                        : sample.getHoverName().copy()).append(Component.literal("×" + entry[1]));
                if (k > 0) list.append(Component.literal("、"));
                list.append(part);
            }
            return Component.translatable("message.wuxingxiantu.refining_missing_detail", list)
                    .append(Component.translatable("message.wuxingxiantu.refining_missing_count", foundPedestals));
        }
        /** Mirror the recipe allocation on copies, so ownership rejection precedes all consumption. */
        private static java.util.List<ItemStack> previewRefiningMaterials(PedestalContainer pedestals,RefiningRecipe recipe){
            var copy=new net.minecraft.world.SimpleContainer(pedestals.getContainerSize());
            for(int i=0;i<copy.getContainerSize();i++)copy.setItem(i,pedestals.getItem(i).copy());
            recipe.consume(copy);
            var result=new java.util.ArrayList<ItemStack>();
            for(int i=0;i<copy.getContainerSize();i++){
                var before=pedestals.getItem(i);int used=before.getCount()-copy.getItem(i).getCount();
                if(used>0)result.add(before.copyWithCount(used));
            }
            return result;
        }
        private static java.util.List<ItemStack> consumeFromPedestals(ServerLevel level, PedestalContainer pedestals,
                                                 RefiningRecipe recipe, BlockPos corePos) {
            java.util.List<ItemStack> materials=new java.util.ArrayList<>();
            java.util.List<ChenghuaPedestalBlockEntity> stands = pedestals.pedestals();
            java.util.List<net.minecraft.world.item.ItemStack> before = new java.util.ArrayList<>();
            for (ChenghuaPedestalBlockEntity stand : stands) before.add(stand.floatingStack().copy());
            recipe.consume(pedestals);
            BlockPos firePos = ExoticFireState.isFire(level.getBlockState(corePos.above(2)))
                    ? corePos.above(2) : corePos.above(1);
            for (int i = 0; i < stands.size(); i++) {
                ChenghuaPedestalBlockEntity stand = stands.get(i);
                int used = before.get(i).getCount() - stand.floatingStack().getCount();
                if (used <= 0) continue;
                net.minecraft.world.item.ItemStack flying = before.get(i).copy();
                flying.setCount(used);
                materials.add(flying.copy());
                OfferingItemEntity offering = new OfferingItemEntity(level,
                        stand.getBlockPos().getX() + 0.5D, stand.getBlockPos().getY() + 1.05D,
                        stand.getBlockPos().getZ() + 0.5D, flying, firePos);
                level.addFreshEntity(offering);
            }
            level.playSound(null, corePos, net.minecraft.sounds.SoundEvents.BLAZE_AMBIENT,
                    net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 1.6F);
            return materials;
        }
}
