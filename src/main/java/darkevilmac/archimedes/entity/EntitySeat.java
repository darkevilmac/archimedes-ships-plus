package darkevilmac.archimedes.entity;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.registry.IEntityAdditionalSpawnData;

public class EntitySeat extends Entity implements IEntityAdditionalSpawnData {

    private EntityShip ship;
    private BlockPos pos;
    private Entity prevRiddenByEntity;
    private int ticksTillShipCheck;

    public EntitySeat(World world) {
        super(world);
        ticksTillShipCheck = 0;
        ship = null;
        pos = null;
        prevRiddenByEntity = null;
        setSize(0F, 0F);
    }

    /**
     * Called from ShipCapabilities.
     *
     * @param player
     * @return
     * @ShipCapabilities
     */
    @Override
    public boolean interactFirst(EntityPlayer player) {
        checkShipOpinion();

        if (riddenByEntity == null) {
            player.mountEntity(null);
            player.setSneaking(false);
            player.mountEntity(this);
            return true;
        } else {
            return false;
        }
    }


    /**
     * Sets the parent ship as well as chunkposition.
     *
     * @param entityship
     */
    public void setParentShip(EntityShip entityship, BlockPos pos) {
        ship = entityship;
        if (entityship != null) {
            setLocationAndAngles(entityship.posX, entityship.posY, entityship.posZ, 0F, 0F);
            if (worldObj != null && !worldObj.isRemote) {
                if (!this.dataWatcher.getIsBlank() && this.dataWatcher.getWatchableObjectByte(10) == new Byte((byte) 1)) {
                    this.dataWatcher.updateObject(6, entityship.getEntityId());
                    this.dataWatcher.updateObject(7, new Byte((byte) (pos.getX() & 0xFF)));
                    this.dataWatcher.updateObject(8, new Byte((byte) (pos.getY() & 0xFF)));
                    this.dataWatcher.updateObject(9, new Byte((byte) (pos.getZ() & 0xFF)));
                } else {
                    this.dataWatcher.addObject(6, entityship.getEntityId());
                    this.dataWatcher.addObject(7, new Byte((byte) (pos.getX() & 0xFF)));
                    this.dataWatcher.addObject(8, new Byte((byte) (pos.getY() & 0xFF)));
                    this.dataWatcher.addObject(9, new Byte((byte) (pos.getZ() & 0xFF)));
                    this.dataWatcher.addObject(10, 1);
                }
            }
        }
    }

    public EntityShip getParentShip() {
        return ship;
    }

    public BlockPos getPos() {
        return pos;
    }

    public void checkShipOpinion() {
        if (ship != null && ship.getCapabilities() != null && !((ShipCapabilities) ship.getCapabilities()).hasSeat(this)) {
            if (riddenByEntity != null && this.riddenByEntity instanceof EntityPlayer) {
                EntityPlayer player = (EntityPlayer) riddenByEntity;
                EntitySeat seat = ((ShipCapabilities) ship.getCapabilities()).getAvailableSeat();
                if (seat != null) {
                    player.mountEntity(null);
                    player.mountEntity(seat);
                    EntityParachute parachute = new EntityParachute(worldObj, ship, pos);
                    if (worldObj.spawnEntityInWorld(parachute)) {
                        player.mountEntity(parachute);
                        player.setSneaking(false);
                    }
                }
                setDead();
            }
        }
    }

    @Override
    public void onUpdate() {
        super.onUpdate();

        if (worldObj == null)
            return;

        if (worldObj.isRemote) {
            if (!this.dataWatcher.getIsBlank() && this.dataWatcher.getWatchableObjectByte(10) == new Byte((byte) 1)) {
                if (this.dataWatcher.getWatchableObjectInt(6) != 0) {
                    ship = (EntityShip) worldObj.getEntityByID(this.dataWatcher.getWatchableObjectInt(6));
                    pos = new BlockPos(this.dataWatcher.getWatchableObjectByte(7),
                            this.dataWatcher.getWatchableObjectByte(8),
                            this.dataWatcher.getWatchableObjectByte(9));
                }
            }
            if (this.dataWatcher.hasObjectChanged() && this.dataWatcher.getWatchableObjectInt(6) != 0) {
                ship = (EntityShip) worldObj.getEntityByID(this.dataWatcher.getWatchableObjectInt(6));
                pos = new BlockPos(this.dataWatcher.getWatchableObjectByte(7),
                        this.dataWatcher.getWatchableObjectByte(8),
                        this.dataWatcher.getWatchableObjectByte(9));
            }
        }

        if (ship != null) {
            setPosition(ship.posX, ship.posY, ship.posZ);
        }

        if (!worldObj.isRemote) {

            if (riddenByEntity == null) {
                if (prevRiddenByEntity != null) {
                    if (ship != null && ship.isFlying()) {
                        EntityParachute parachute = new EntityParachute(worldObj, ship, pos);
                        if (worldObj.spawnEntityInWorld(parachute)) {
                            prevRiddenByEntity.mountEntity(parachute);
                            prevRiddenByEntity.setSneaking(false);
                        }
                    }
                    prevRiddenByEntity = null;
                }
            } else {
                prevRiddenByEntity = riddenByEntity;
            }
            ticksTillShipCheck++;
            if (ticksTillShipCheck >= 40) {
                if (ship != null)
                    ticksTillShipCheck = 0;
            }
        }

        if (riddenByEntity != null && riddenByEntity.ridingEntity != this) {
            Entity rider = riddenByEntity;
            rider.mountEntity(null);
            rider.mountEntity(this);
        }
    }

    @Override
    protected void entityInit() {
        this.dataWatcher.addObject(6, 0);
        this.dataWatcher.addObject(7, new Byte((byte) (0 & 0xFF)));
        this.dataWatcher.addObject(8, new Byte((byte) (0 & 0xFF)));
        this.dataWatcher.addObject(9, new Byte((byte) (0 & 0xFF)));
        this.dataWatcher.addObject(10, new Byte((byte) 1));
    }

    @Override
    public void updateRiderPosition() {
        if (ship != null) {
            ship.updateRiderPosition(riddenByEntity, pos, 0);
        }
    }

    @Override
    public double getMountedYOffset() {
        return 0.5D;
    }

    @Override
    public AxisAlignedBB getCollisionBox(Entity entity) {
        return null;
    }

    @Override
    public AxisAlignedBB getBoundingBox() {
        return null;
    }

    @Override
    public boolean canBeCollidedWith() {
        return false;
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound compound) {
        checkShipOpinion();
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound p_70014_1_) {

    }

    @Override
    public void writeSpawnData(ByteBuf data) {
        if (ship == null) {
            data.writeInt(0);
            data.writeByte(0);
            data.writeByte(0);
            data.writeByte(0);
            return;
        }
        data.writeInt(ship.getEntityId());
        data.writeByte(pos.getX() & 0xFF);
        data.writeByte(pos.getY() & 0xFF);
        data.writeByte(pos.getZ() & 0xFF);
    }

    @Override
    public void readSpawnData(ByteBuf data) {
        int entityID = data.readInt();
        int posChunkX = data.readUnsignedByte();
        int posChunkY = data.readUnsignedByte();
        int posChunkZ = data.readUnsignedByte();
        if (entityID != 0) {
            Entity entity = worldObj.getEntityByID(entityID);
            if (entity instanceof EntityShip) {
                setParentShip((EntityShip) entity, new BlockPos(posChunkX, posChunkY, posChunkZ));
            }
        }
    }
}