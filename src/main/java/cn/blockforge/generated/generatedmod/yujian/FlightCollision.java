package cn.blockforge.generated.generatedmod.yujian;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Bounded swept-volume checks. Only already-available chunks are read; never waits for generation. */
public final class FlightCollision {
    public record Sweep(Vec3 movement, boolean blocked, boolean loaded) {}
    public enum Prediction { CLEAR, PASSABLE, BLOCKED, UNLOADED }
    public static final TagKey<net.minecraft.world.level.block.Block> DUNXU_BLOCKED = TagKey.create(
            Registries.BLOCK, new ResourceLocation("wuxingxiantu", "dunxu_blocked"));
    private FlightCollision() {}

    public static boolean loaded(ServerPlayer player, AABB box) {
        var level=player.serverLevel();
        // Build height limits block placement, not entity flight. Outside it, loaded chunks contain air.
        if(!Double.isFinite(box.minX)||!Double.isFinite(box.minY)||!Double.isFinite(box.minZ)
                ||!Double.isFinite(box.maxX)||!Double.isFinite(box.maxY)||!Double.isFinite(box.maxZ)
                ||!level.getWorldBorder().isWithinBounds(box.minX,box.minZ)
                ||!level.getWorldBorder().isWithinBounds(box.maxX,box.maxZ))return false;
        int minX=net.minecraft.util.Mth.floor(box.minX)>>4,maxX=net.minecraft.util.Mth.floor(box.maxX)>>4;
        int minZ=net.minecraft.util.Mth.floor(box.minZ)>>4,maxZ=net.minecraft.util.Mth.floor(box.maxZ)>>4;
        if(maxX-minX>2||maxZ-minZ>2)return false;
        for(int x=minX;x<=maxX;x++)for(int z=minZ;z<=maxZ;z++)
            // hasChunk only checks the holder's requested FULL ticket level. Its FULL
            // future may still be pending, and getBlockState would then block the server.
            if(level.getChunkSource().getChunkNow(x,z)==null)return false;
        return true;
    }

    public static boolean clear(ServerPlayer player, AABB box, boolean phasing) {
        if(!loaded(player,box))return false;
        if(!loaded(player,box.inflate(1)))return false;
        var level=player.serverLevel();
        var min=BlockPos.containing(box.minX-1,Math.max(level.getMinBuildHeight(),box.minY-1),box.minZ-1);
        var max=BlockPos.containing(box.maxX+1,Math.min(level.getMaxBuildHeight()-1,box.maxY),box.maxZ+1);
        if(min.getY()>max.getY())return level.getEntityCollisions(player,box).isEmpty();
        long volume=(long)(max.getX()-min.getX()+1)*(max.getY()-min.getY()+1)*(max.getZ()-min.getZ()+1);
        if(volume>YujianTuning.MAX_COLLISION_BLOCKS)return false;
        for(var pos:BlockPos.betweenClosed(min,max)) {
            var state=level.getBlockState(pos);
            // Ordinary walls are passable; block entities and blocked-tag entries remain solid.
            if(phasing && (state.is(DUNXU_BLOCKED) || state.hasBlockEntity())
                    && new AABB(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1).intersects(box))return false;
            if(!phasing&&state.getFluidState().is(FluidTags.LAVA))return false;
            boolean intersects=false;
            for(AABB shape:state.getCollisionShape(level,pos).toAabbs())
                if(shape.move(pos.getX(),pos.getY(),pos.getZ()).intersects(box)){intersects=true;break;}
            if(!intersects)continue;
            if(phasing && !state.is(DUNXU_BLOCKED)
                    && level.getBlockEntity(pos)==null)continue;
            return false;
        }
        return level.getEntityCollisions(player,box).isEmpty();
    }

    private static boolean collisionRegionLoaded(ServerPlayer player,AABB box) {
        return loaded(player,box)&&loaded(player,box.inflate(1));
    }

    /** A decorative/damage ray must obey the same readiness guard as movement. */
    public static java.util.Optional<net.minecraft.world.phys.BlockHitResult> clipLoaded(
            ServerPlayer player,net.minecraft.world.entity.Entity sword,Vec3 from,Vec3 to) {
        if(!loaded(player,new AABB(from,to).inflate(1)))return java.util.Optional.empty();
        return java.util.Optional.of(player.serverLevel().clip(new net.minecraft.world.level.ClipContext(
                from,to,net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,sword)));
    }

    public static Sweep sweep(ServerPlayer player, AABB volume, Vec3 requested, boolean phasing) {
        Vec3 velocity=YujianTuning.finiteVelocity(requested);
        // Empty sky and open corridors need one bounded broad-phase check,
        // rather than rescanning overlapping blocks every 0.2 blocks. Any
        // obstacle, unloaded region or oversized box retains the exact sweep.
        AABB whole=volume.minmax(volume.move(velocity));
        if(clear(player,whole,phasing))return new Sweep(velocity,false,true);
        int steps=Math.max(1,(int)Math.ceil(velocity.length()/2.0));
        Vec3 accepted=Vec3.ZERO;
        for(int i=1;i<=steps;i++) {
            Vec3 end=velocity.scale(i/(double)steps);
            AABB section=volume.move(accepted).minmax(volume.move(end));
            if(clear(player,section,phasing)){accepted=end;continue;}
            // Only uncertain sections need the 0.2-block exact sweep. Empty
            // sections do not repeatedly re-read the same overlapping blocks.
            Vec3 start=accepted,delta=end.subtract(start);
            int fine=Math.max(1,(int)Math.ceil(delta.length()/YujianTuning.SWEEP_STEP));
            for(int j=1;j<=fine;j++){
                Vec3 next=start.add(delta.scale(j/(double)fine));
                AABB swept=volume.move(accepted).minmax(volume.move(next));
                if(!collisionRegionLoaded(player,swept))return new Sweep(accepted,true,false);
                if(!clear(player,swept,phasing))return new Sweep(accepted,true,true);
                accepted=next;
            }
        }
        return new Sweep(accepted,false,true);
    }

    /** Predict the first collision along the next few ticks without loading chunks. */
    public static Prediction predict(ServerPlayer player, AABB volume, Vec3 requested) {
        Vec3 velocity=YujianTuning.finiteVelocity(requested);
        int steps=Math.min(YujianTuning.MAX_SWEEP_STEPS,
                Math.max(1,(int)Math.ceil(velocity.length()/YujianTuning.SWEEP_STEP)));
        Vec3 accepted=Vec3.ZERO;
        for(int i=1;i<=steps;i++) {
            Vec3 next=velocity.scale(i/(double)steps);
            AABB swept=volume.move(accepted).minmax(volume.move(next));
            if(!collisionRegionLoaded(player,swept))return Prediction.UNLOADED;
            Prediction hit=firstBlockCollision(player,swept);
            if(hit!=Prediction.CLEAR)return hit;
            if(!player.serverLevel().getEntityCollisions(player,swept).isEmpty())return Prediction.BLOCKED;
            accepted=next;
        }
        return Prediction.CLEAR;
    }

    private static Prediction firstBlockCollision(ServerPlayer player,AABB box) {
        var level=player.serverLevel();
        var min=BlockPos.containing(box.minX-1,Math.max(level.getMinBuildHeight(),box.minY-1),box.minZ-1);
        var max=BlockPos.containing(box.maxX+1,Math.min(level.getMaxBuildHeight()-1,box.maxY),box.maxZ+1);
        if(min.getY()>max.getY())return Prediction.CLEAR;
        long volume=(long)(max.getX()-min.getX()+1)*(max.getY()-min.getY()+1)*(max.getZ()-min.getZ()+1);
        if(volume>YujianTuning.MAX_COLLISION_BLOCKS)return Prediction.BLOCKED;
        for(BlockPos pos:BlockPos.betweenClosed(min,max)) {
            var state=level.getBlockState(pos);
            if(level.getBlockEntity(pos)!=null
                    && new AABB(pos.getX(),pos.getY(),pos.getZ(),pos.getX()+1,pos.getY()+1,pos.getZ()+1).intersects(box))
                return Prediction.BLOCKED;
            boolean intersects=false;
            for(AABB shape:state.getCollisionShape(level,pos).toAabbs())
                if(shape.move(pos.getX(),pos.getY(),pos.getZ()).intersects(box)){intersects=true;break;}
            if(!intersects)continue;
            if(!state.is(DUNXU_BLOCKED) && level.getBlockEntity(pos)==null)
                return Prediction.PASSABLE;
            return Prediction.BLOCKED;
        }
        return Prediction.CLEAR;
    }
}
