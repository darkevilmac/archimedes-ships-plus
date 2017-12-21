package com.elytradev.davincisvessels.common.tileentity;

import com.elytradev.davincisvessels.DavincisVesselsMod;
import com.elytradev.davincisvessels.common.LanguageEntries;
import com.elytradev.davincisvessels.common.api.tileentity.ITileEngineModifier;
import com.elytradev.davincisvessels.common.entity.ShipCapabilities;
import com.elytradev.movingworld.common.chunk.mobilechunk.MobileChunk;
import com.elytradev.movingworld.common.entity.EntityMovingWorld;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityFurnace;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;


public class TileEngine extends TileEntity implements IInventory, ITileEngineModifier {
    public float enginePower;
    ItemStack[] itemStacks;
    private int burnTime;
    private boolean running;
    private BlockPos chunkPos;

    public TileEngine() {
        itemStacks = new ItemStack[getSizeInventory()];
        for (int i = 0; i < itemStacks.length; i++) {
            itemStacks[i] = ItemStack.EMPTY;
        }
        burnTime = 0;
        running = false;
    }

    public TileEngine(float power) {
        this();

        enginePower = power;
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        super.readFromNBT(tag);
        burnTime = tag.getInteger("burn");
        enginePower = tag.getFloat("power");
        NBTTagList list = tag.getTagList("inv", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            NBTTagCompound comp = list.getCompoundTagAt(i);
            int j = comp.getByte("i");
            itemStacks[j] = new ItemStack(comp);
        }
    }

    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound tag) {
        tag = super.writeToNBT(tag);
        tag.setInteger("burn", burnTime);
        tag.setFloat("power", enginePower);
        NBTTagList list = new NBTTagList();
        for (int i = 0; i < getSizeInventory(); i++) {
            if (itemStacks[i] != ItemStack.EMPTY) {
                NBTTagCompound comp = new NBTTagCompound();
                comp.setByte("i", (byte) i);
                itemStacks[i].writeToNBT(comp);
                list.appendTag(comp);
            }
        }
        tag.setTag("inv", list);
        return tag;
    }

    @Override
    public void markDirty() {
    }

    @Override
    public SPacketUpdateTileEntity getUpdatePacket() {
        NBTTagCompound compound = new NBTTagCompound();
        writeToNBT(compound);
        return new SPacketUpdateTileEntity(pos, 1, compound);
    }

    @Override
    public ITextComponent getDisplayName() {
        return new TextComponentString("Engine Inventory");
    }

    @Override
    public void onDataPacket(NetworkManager net, SPacketUpdateTileEntity packet) {
        readFromNBT(packet.getNbtCompound());
    }

    public boolean isRunning() {
        return running;
    }

    public int getBurnTime() {
        return burnTime;
    }

    public boolean consumeFuel(int f) {
        if (burnTime >= f) {
            burnTime -= f;
            return true;
        }

        for (int i = 0; i < getSizeInventory(); i++) {
            ItemStack is = decrStackSize(i, 1);
            if (is != ItemStack.EMPTY && is.getCount() > 0) {
                burnTime += TileEntityFurnace.getItemBurnTime(is);
                return consumeFuel(f);
            }
        }
        return false;
    }

    @Override
    public int getSizeInventory() {
        return 4;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack itemstack : this.itemStacks) {
            if (!itemstack.isEmpty()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public ItemStack getStackInSlot(int i) {
        return i >= 0 && i < 4 ? itemStacks[i] : ItemStack.EMPTY;
    }

    @Override
    public ItemStack decrStackSize(int i, int n) {
        if (itemStacks[i] != ItemStack.EMPTY) {
            ItemStack itemstack;

            if (itemStacks[i].getCount() <= n) {
                itemstack = itemStacks[i];
                itemStacks[i] = ItemStack.EMPTY;
                markDirty();
                return itemstack;
            }

            itemstack = itemStacks[i].splitStack(n);
            if (itemStacks[i].getCount() <= 0) {
                itemStacks[i] = ItemStack.EMPTY;
            }

            markDirty();
            return itemstack;
        }
        return ItemStack.EMPTY;
    }

    @Override
    public ItemStack removeStackFromSlot(int i) {
        ItemStack content = itemStacks[i].copy();
        itemStacks[i] = ItemStack.EMPTY;
        return content;
    }

    @Override
    public void setInventorySlotContents(int i, ItemStack is) {
        if (i >= 0 && i < 4) {
            itemStacks[i] = is;
        }
    }

    @Override
    public int getInventoryStackLimit() {
        return 64;
    }

    @Override
    public boolean isUsableByPlayer(EntityPlayer player) {
        return world.getTileEntity(pos) == this && player.getDistanceSq(pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d) <= 64d;
    }

    @Override
    public void openInventory(EntityPlayer player) {
    }

    @Override
    public void closeInventory(EntityPlayer player) {
    }

    @Override
    public boolean isItemValidForSlot(int i, ItemStack is) {
        return i >= 0 && i < 4 && TileEntityFurnace.isItemFuel(is);
    }

    @Override
    public int getField(int id) {
        return 0;
        // We have none.
    }

    @Override
    public void setField(int id, int value) {
        // We have none.
    }

    @Override
    public int getFieldCount() {
        return 0;
        // We have none.
    }

    @Override
    public void clear() {
        itemStacks = new ItemStack[getSizeInventory()];
    }

    @Override
    public String getName() {
        return LanguageEntries.CONTAINER_ENGINE;
    }

    @Override
    public boolean hasCustomName() {
        return false; //No custom names for this.
    }

    @Override
    public float getPowerIncrement(ShipCapabilities shipCapabilities) {
        return isRunning() ? enginePower : 0;
    }

    @Override
    public void setParentMovingWorld(EntityMovingWorld movingWorld, BlockPos chunkPos) {
        // We don't bother with our parent.

        this.chunkPos = pos;
    }

    @Override
    public EntityMovingWorld getParentMovingWorld() {
        return null;
    }

    @Override
    public void setParentMovingWorld(EntityMovingWorld entityMovingWorld) {
        // We don't bother with our parent.
    }

    @Override
    public BlockPos getChunkPos() {
        return chunkPos;
    }

    @Override
    public void setChunkPos(BlockPos chunkPos) {

    }

    @Override
    public void tick(MobileChunk mobileChunk) {
        running = consumeFuel(DavincisVesselsMod.INSTANCE.getNetworkConfig().getShared().engineBurnSpeed);
    }
}
