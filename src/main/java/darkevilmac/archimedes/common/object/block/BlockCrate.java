package darkevilmac.archimedes.common.object.block;

import darkevilmac.archimedes.common.tileentity.TileEntityCrate;
import darkevilmac.movingworld.common.entity.EntityMovingWorld;
import net.minecraft.block.Block;
import net.minecraft.block.BlockContainer;
import net.minecraft.block.BlockFence;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumWorldBlockLayer;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;

public class BlockCrate extends BlockContainer {
    public static final PropertyEnum AXIS = PropertyEnum.create("axis", EnumFacing.Axis.class, EnumFacing.Axis.X, EnumFacing.Axis.Z);
    public static final PropertyBool POWERED = PropertyBool.create("powered");


    public BlockCrate(Material material) {
        super(material);
        this.setBlockBounds(0F, 0F, 0F, 1F, 0.1F, 1F);
        this.setDefaultState(this.getBlockState().getBaseState().withProperty(AXIS, EnumFacing.Axis.X).withProperty(POWERED, false));
    }

    public AxisAlignedBB getCollisionBoundingBox(World worldIn, BlockPos pos, IBlockState state) {
        return null;
    }

    public boolean isOpaqueCube() {
        return false;
    }

    public boolean isFullCube() {
        return false;
    }

    public boolean isPassable(IBlockAccess worldIn, BlockPos pos) {
        return true;
    }


    public static int getMetaForAxis(EnumFacing.Axis axis) {
        return axis == EnumFacing.Axis.X ? 1 : (axis == EnumFacing.Axis.Z ? 2 : 0);
    }

    @Override
    public int getRenderType() {
        return 3;
    }

    @Override
    public EnumWorldBlockLayer getBlockLayer() {
        return EnumWorldBlockLayer.SOLID;
    }

    @Override
    public IBlockState getStateFromMeta(int meta) {
        boolean powered = false;
        if (meta > 2) {
            powered = true;
            meta /= 2;
        }

        return this.getDefaultState().withProperty(AXIS, (meta & 3) == 2 ? EnumFacing.Axis.Z : EnumFacing.Axis.X).withProperty(POWERED, powered);
    }

    @Override
    public int getMetaFromState(IBlockState state) {
        return getMetaForAxis((EnumFacing.Axis) state.getValue(AXIS)) * (state.getValue(POWERED) ? 2 : 1);
    }

    @Override
    protected BlockState createBlockState() {
        return new BlockState(this, AXIS, POWERED);
    }

    @Override
    public IBlockState onBlockPlaced(World worldIn, BlockPos pos, EnumFacing facing, float hitX, float hitY, float hitZ, int meta, EntityLivingBase placer) {
        return this.getDefaultState().withProperty(AXIS, placer.getHorizontalFacing().getAxis());
    }

    @Override
    public void onBlockPlacedBy(World worldIn, BlockPos pos, IBlockState state, EntityLivingBase placer, ItemStack stack) {
        worldIn.setBlockState(pos, state.withProperty(AXIS, placer.getHorizontalFacing().getAxis()), 2);
    }

    @Override
    public void onEntityCollidedWithBlock(World world, BlockPos pos, IBlockState state, Entity entity) {
        if (world == null || world.isRemote || state.getValue(POWERED))
            return;

        if (entity != null && !(entity instanceof EntityPlayer || entity instanceof EntityMovingWorld)) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileEntityCrate) {
                if (((TileEntityCrate) te).canCatchEntity() && ((TileEntityCrate) te).getContainedEntity() == null) {
                    ((TileEntityCrate) te).setContainedEntity(entity);
                }
            }
        }
    }

    @Override
    public TileEntity createNewTileEntity(World world, int meta) {
        return new TileEntityCrate();
    }

    @Override
    public boolean onBlockActivated(World world, BlockPos pos, IBlockState state, EntityPlayer playerIn, EnumFacing side, float hitX, float hitY, float hitZ) {
        TileEntity te = world.getTileEntity(pos);
        if (te != null && te instanceof TileEntityCrate) {
            ((TileEntityCrate) te).releaseEntity();
            return true;
        }
        return false;
    }

    @Override
    public boolean canPlaceBlockAt(World world, BlockPos pos) {
        return canBePlacedOn(world, pos.add(0, -1, 0));
    }

    private boolean canBePlacedOn(World worldIn, BlockPos pos) {
        return worldIn.getBlockState(pos).getBlock().isSideSolid(worldIn, pos, EnumFacing.UP) || worldIn.getBlockState(pos).getBlock() instanceof BlockFence;
    }

    @Override
    public void onNeighborBlockChange(World world, BlockPos pos, IBlockState state, Block neighborBlock) {
        if (world.isRemote)
            return;

        if (!canBePlacedOn(world, pos.down())) {
            dropBlockAsItem(world, pos, state, 0);
            world.setBlockToAir(pos);
        }

        boolean powered = world.isBlockPowered(pos) || world.isBlockPowered(pos.up());

        if (!(world.getBlockState(pos).getBlock() instanceof BlockCrate))
            return;

        if (powered) {
            TileEntity te = world.getTileEntity(pos);
            if (te != null && te instanceof TileEntityCrate) {
                ((TileEntityCrate) te).releaseEntity();
                world.setBlockState(pos, world.getBlockState(pos).withProperty(POWERED, Boolean.TRUE));
            }
        } else {
            world.setBlockState(pos, world.getBlockState(pos).withProperty(POWERED, Boolean.FALSE));
        }
    }

}
