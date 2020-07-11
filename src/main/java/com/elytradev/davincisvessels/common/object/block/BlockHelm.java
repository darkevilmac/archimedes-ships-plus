package com.elytradev.davincisvessels.common.object.block;

import com.elytradev.davincisvessels.DavincisVesselsMod;
import com.elytradev.davincisvessels.common.tileentity.TileHelm;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumBlockRenderType;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class BlockHelm extends BlockDirectional implements ITileEntityProvider {

    public static final PropertyBool IS_WHEEL = PropertyBool.create("wheel");

    public BlockHelm() {
        super(Material.WOOD);
        this.setDefaultState(this.blockState.getBaseState().withProperty(FACING, EnumFacing.NORTH).withProperty(IS_WHEEL, false));
        this.setSoundType(SoundType.WOOD);
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT_MIPPED;
    }

    @Override
    public boolean isFullCube(IBlockState state) {
        return false;
    }

    @Override
    public boolean isOpaqueCube(IBlockState state) {
        return false;
    }

    @Override
    public EnumBlockRenderType getRenderType(IBlockState state) {
        return EnumBlockRenderType.MODEL;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public AxisAlignedBB getSelectedBoundingBox(IBlockState state, World worldIn, BlockPos pos) {
        AxisAlignedBB selectedBoundingBox = super.getSelectedBoundingBox(state, worldIn, pos);
        if (state == null || state.getValue(FACING) == null)
            return super.getSelectedBoundingBox(state, worldIn, pos);

        double pixelSize = 1D / 16D;
        EnumFacing facing = state.getValue(FACING);
        switch (facing) {
            case NORTH: {
                selectedBoundingBox = new AxisAlignedBB(1 - (pixelSize * 1), 1, 1 - (pixelSize * 3), pixelSize * 1, 0, pixelSize * 2);
                selectedBoundingBox = selectedBoundingBox.offset(pos.getX(), pos.getY(), pos.getZ());

                return selectedBoundingBox;
            }
            case SOUTH: {
                selectedBoundingBox = new AxisAlignedBB(1 - (pixelSize * 1), 1, 1 - (pixelSize * 2), pixelSize * 1, 0, pixelSize * 3);
                selectedBoundingBox = selectedBoundingBox.offset(pos.getX(), pos.getY(), pos.getZ());

                return selectedBoundingBox;
            }
            case WEST: {
                selectedBoundingBox = new AxisAlignedBB(pixelSize * 2, 0, pixelSize * 1, 1 - (pixelSize * 3), 1, 1 - (pixelSize * 1));
                selectedBoundingBox = selectedBoundingBox.offset(pos.getX(), pos.getY(), pos.getZ());

                return selectedBoundingBox;
            }
            case EAST: {
                selectedBoundingBox = new AxisAlignedBB(1 - (pixelSize * 2), 1, 1 - (pixelSize * 1), pixelSize * 3, 0, pixelSize * 1);
                selectedBoundingBox = selectedBoundingBox.offset(pos.getX(), pos.getY(), pos.getZ());

                return selectedBoundingBox;
            }
            default: {
                return selectedBoundingBox;
            }
        }
    }

    @Override
    public boolean onBlockActivated(World worldIn, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumHand hand, EnumFacing side, float hitX, float hitY, float hitZ) {
        if (!playerIn.isSneaking()) {
            TileEntity tileentity = worldIn.getTileEntity(pos);
            if (tileentity != null) {
                playerIn.openGui(DavincisVesselsMod.INSTANCE, 1, worldIn, pos.getX(), pos.getY(), pos.getZ());
                return true;
            }
        }
        return false;
    }

    @Override
    public TileEntity createNewTileEntity(World var1, int var2) {
        return new TileHelm();
    }

    @Override
    public void breakBlock(World world, BlockPos pos, IBlockState state) {
        super.breakBlock(world, pos, state);
        world.removeTileEntity(pos);
    }

    @Override
    public boolean eventReceived(IBlockState state, World world, BlockPos pos, int eventID, int eventParam) {
        super.eventReceived(state, world, pos, eventID, eventParam);
        TileEntity tileentity = world.getTileEntity(pos);
        return tileentity != null && tileentity.receiveClientEvent(eventID, eventParam);
    }

    @Override
    public void onBlockAdded(World worldIn, BlockPos pos, IBlockState state) {
        this.setDefaultFacing(worldIn, pos, state);
    }

    private void setDefaultFacing(World worldIn, BlockPos pos, IBlockState state) {
        if (!worldIn.isRemote) {
            IBlockState block = worldIn.getBlockState(pos.north());
            IBlockState block1 = worldIn.getBlockState(pos.south());
            IBlockState block2 = worldIn.getBlockState(pos.west());
            IBlockState block3 = worldIn.getBlockState(pos.east());
            EnumFacing enumfacing = state.getValue(FACING);

            if (enumfacing == EnumFacing.NORTH && block.isFullBlock() && !block1.isFullBlock()) {
                enumfacing = EnumFacing.SOUTH;
            } else if (enumfacing == EnumFacing.SOUTH && block1.isFullBlock() && !block.isFullBlock()) {
                enumfacing = EnumFacing.NORTH;
            } else if (enumfacing == EnumFacing.WEST && block2.isFullBlock() && !block3.isFullBlock()) {
                enumfacing = EnumFacing.EAST;
            } else if (enumfacing == EnumFacing.EAST && block3.isFullBlock() && !block2.isFullBlock()) {
                enumfacing = EnumFacing.WEST;
            }

            worldIn.setBlockState(pos, state.withProperty(FACING, enumfacing), 2);
        }
    }

    @Override
    public IBlockState getStateForPlacement(World world, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer, EnumHand hand) {
        if (placer instanceof EntityPlayer) {
            // TODO: Achievements are gone.
            //((EntityPlayer) placer).addStat(DavincisVesselsObjects.achievementCreateHelm);
        }

        return this.getDefaultState().withProperty(FACING, placer.getHorizontalFacing().getOpposite());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(FACING, placer.getHorizontalFacing().getOpposite()), 2);
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        EnumFacing enumfacing = EnumFacing.byIndex(meta);

        if (enumfacing.getAxis() == EnumFacing.Axis.Y) {
            enumfacing = EnumFacing.NORTH;
        }

        return this.getDefaultState().withProperty(FACING, enumfacing);
    }


    @Override
    public int getMetaFromState(IBlockState state) {
        return state.getValue(FACING).getIndex();
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, IS_WHEEL);
    }

}
