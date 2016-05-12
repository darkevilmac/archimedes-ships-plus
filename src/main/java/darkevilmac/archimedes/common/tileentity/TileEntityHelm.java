package darkevilmac.archimedes.common.tileentity;

import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.common.entity.EntityShip;
import darkevilmac.archimedes.common.entity.ShipAssemblyInteractor;
import darkevilmac.archimedes.common.network.ArchimedesShipsNetworking;
import darkevilmac.movingworld.common.chunk.MovingWorldAssemblyInteractor;
import darkevilmac.movingworld.common.chunk.assembly.AssembleResult;
import darkevilmac.movingworld.common.chunk.mobilechunk.MobileChunk;
import darkevilmac.movingworld.common.entity.EntityMovingWorld;
import darkevilmac.movingworld.common.entity.MovingWorldInfo;
import darkevilmac.movingworld.common.tile.TileMovingWorldMarkingBlock;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;

public class TileEntityHelm extends TileMovingWorldMarkingBlock {
    public boolean submerge;
    private ShipAssemblyInteractor interactor;
    private EntityShip activeShip;
    private MovingWorldInfo info;

    public TileEntityHelm() {
        super();
        activeShip = null;
    }

    @Override
    public void assembledMovingWorld(EntityPlayer player, boolean returnVal) {
        sendAssembleResult(player, false);
        sendAssembleResult(player, true);
    }


    @Override
    public void setParentMovingWorld(BlockPos pos, EntityMovingWorld entityMovingWorld) {
        activeShip = (EntityShip) entityMovingWorld;
    }

    @Override
    public EntityShip getParentMovingWorld() {
        return activeShip;
    }

    @Override
    public void setParentMovingWorld(EntityMovingWorld entityMovingWorld) {
        setParentMovingWorld(new BlockPos(BlockPos.ORIGIN), entityMovingWorld);
    }

    @Override
    public void tick(MobileChunk mobileChunk) {
        // No implementation
    }

    @Override
    public MovingWorldAssemblyInteractor getInteractor() {
        if (interactor == null) {
            interactor = new ShipAssemblyInteractor();
        }
        return interactor;
    }

    @Override
    public void setInteractor(MovingWorldAssemblyInteractor interactor) {
        this.interactor = (ShipAssemblyInteractor) interactor;
    }

    @Override
    public MovingWorldInfo getInfo() {
        if (this.info == null)
            this.info = new MovingWorldInfo();
        return info;
    }

    @Override
    public void setInfo(MovingWorldInfo info) {
        this.info = info;
    }

    @Override
    public int getMaxBlocks() {
        return ArchimedesShipMod.instance.
                getNetworkConfig().
                getShared()
                .maxShipChunkBlocks;
    }

    @Override
    public EntityMovingWorld getMovingWorld(World worldObj) {
        return new EntityShip(worldObj);
    }

    @Override
    public void mountedMovingWorld(EntityPlayer player, EntityMovingWorld movingWorld, int stage) {
        switch (stage) {
            case 1: {
                sendAssembleResult(player, false);
            }
        }
    }

    @Override
    public void undoCompilation(EntityPlayer player) {
        super.undoCompilation(player);
        sendAssembleResult(player, false);
        sendAssembleResult(player, true);
    }

    @Override
    public MovingWorldAssemblyInteractor getNewAssemblyInteractor() {
        return new ShipAssemblyInteractor();
    }

    public void sendAssembleResult(EntityPlayer player, boolean prev) {
        if (!worldObj.isRemote) {
            AssembleResult res;
            if (prev) {
                res = getPrevAssembleResult();
            } else {
                res = getAssembleResult();
            }

            ByteBuf buf = Unpooled.buffer(12);

            buf.writeBoolean(prev);
            if (res == null) {
                buf.writeByte(AssembleResult.ResultType.RESULT_NONE.toByte());
            } else {
                buf = res.toByteBuf(buf);
            }

            ArchimedesShipsNetworking.NETWORK.send().packet("AssembleResultMessage")
                    .with("result", buf.array()).toAllIn(player.worldObj);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tagCompound) {
        super.writeToNBT(tagCompound);
        tagCompound.setBoolean("submergeShipOnAssemble", submerge);
    }

    @Override
    public void readFromNBT(NBTTagCompound tagCompound) {
        super.readFromNBT(tagCompound);
        submerge = tagCompound.getBoolean("submergeShipOnAssemble");
    }
}
