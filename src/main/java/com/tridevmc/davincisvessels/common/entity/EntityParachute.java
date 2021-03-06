package com.tridevmc.davincisvessels.common.entity;

import com.tridevmc.davincisvessels.DavincisVesselsMod;
import com.tridevmc.movingworld.common.util.Vec3dMod;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntitySize;
import net.minecraft.entity.MoverType;
import net.minecraft.entity.Pose;
import net.minecraft.nbt.CompoundNBT;
import net.minecraft.network.IPacket;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;
import net.minecraftforge.fml.network.NetworkHooks;

import javax.annotation.Nullable;

public class EntityParachute extends Entity implements IEntityAdditionalSpawnData {

    public EntityParachute(World world) {
        super(DavincisVesselsMod.CONTENT.entityTypes.get(EntityParachute.class), world);
    }

    public EntityParachute(World world, EntityVessel vessel, BlockPos pos) {
        this(world);
        Vec3dMod vec = new Vec3dMod(pos.getX() - vessel.getMobileChunk().getCenterX(), pos.getY() - vessel.getMobileChunk().minY(), pos.getZ() - vessel.getMobileChunk().getCenterZ());
        vec = vec.rotateAroundY((float) Math.toRadians(vessel.rotationYaw));

        setLocationAndAngles(vessel.posX + vec.x, vessel.posY + vec.y - 2D, vessel.posZ + vec.z, 0F, 0F);
        this.setMotion(vessel.getMotion());
    }

    public EntityParachute(World world, Entity mounter, Vec3dMod vec, Vec3dMod vesselPos, Vec3dMod motion) {
        this(world);

        setLocationAndAngles(vesselPos.x + vec.x, vesselPos.y + vec.y - 2D, vesselPos.z + vec.z, 0F, 0F);
        this.setMotion(motion);

        mounter.stopRiding();
        mounter.startRiding(this, true);
    }

    @Override
    public EntitySize getSize(Pose poseIn) {
        return new EntitySize(1, 1, true);
    }

    @Override
    protected void registerData() {
        // NO-OP
    }

    @Override
    public void tick() {
        super.tick();

        prevPosX = posX;
        prevPosY = posY;
        prevPosZ = posZ;

        if (!world.isRemote
                &&
                (getControllingPassenger() == null
                        || onGround
                        || isInWater())) {
            remove();
            return;
        }

        if (!world.isRemote && getControllingPassenger() != null) {
            this.setMotion(this.getMotion().add(getControllingPassenger().getMotion().x, 0, getControllingPassenger().getMotion().z));
        }
        if (getMotion().y > -.5)
            this.setMotion(this.getMotion().subtract(0, 0.025D, 0));

        move(MoverType.SELF, this.getMotion());
    }

    @Override
    @Nullable
    public Entity getControllingPassenger() {
        return this.getPassengers().stream().findAny().orElse(null);
    }

    @Override
    public IPacket<?> createSpawnPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean shouldRiderSit() {
        return false;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void readAdditional(CompoundNBT compound) {
        // NO-OP
    }

    @Override
    protected void writeAdditional(CompoundNBT compound) {
        // NO-OP
    }

    @Override
    protected void updateFallState(double y, boolean onGroundIn, BlockState state, BlockPos pos) {
        // NO-OP
    }

    @Override
    public void fall(float fallDistance, float damageMult) {
        // NO-OP
    }

    @Override
    public void writeSpawnData(PacketBuffer buffer) {
        buffer.writeBoolean(getControllingPassenger() != null);
        if (getControllingPassenger() != null) {
            buffer.writeInt(getControllingPassenger().getEntityId());
        }
    }

    @Override
    public void readSpawnData(PacketBuffer additionalData) {
        if (additionalData.readBoolean() && world != null) {
            int entityID = additionalData.readInt();
            if (world.getEntityByID(entityID) != null) {
                world.getEntityByID(entityID).startRiding(this);
            }
        }
    }
}
