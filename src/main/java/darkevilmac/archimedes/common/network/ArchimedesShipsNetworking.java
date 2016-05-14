package darkevilmac.archimedes.common.network;

import com.unascribed.lambdanetwork.*;
import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.client.ClientProxy;
import darkevilmac.archimedes.client.gui.ContainerHelm;
import darkevilmac.archimedes.common.ArchimedesConfig;
import darkevilmac.archimedes.common.entity.EntityShip;
import darkevilmac.archimedes.common.entity.ShipAssemblyInteractor;
import darkevilmac.archimedes.common.tileentity.TileEntityHelm;
import darkevilmac.movingworld.common.chunk.assembly.AssembleResult;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.BlockPos;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.fml.relauncher.Side;


public class ArchimedesShipsNetworking {

    public static LambdaNetwork NETWORK;

    public static void setupNetwork() {
        ArchimedesShipMod.modLog.info("Setting up network...");
        ArchimedesShipsNetworking.NETWORK = registerPackets(LambdaNetwork.builder().channel("ArchimedesShipsPlus")).build();
        ArchimedesShipMod.modLog.info("Setup network! " + ArchimedesShipsNetworking.NETWORK.toString());
    }

    private static LambdaNetworkBuilder registerPackets(LambdaNetworkBuilder builder) {
        builder = builder.packet("AssembleResultMessage").boundTo(Side.CLIENT)
                .with(DataType.ARBITRARY, "result").handledBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        ByteBuf buf = Unpooled.wrappedBuffer(token.getData("result"));
                        boolean prevFlag = buf.readBoolean();
                        byte resultCode = buf.readByte();
                        AssembleResult result = new AssembleResult(AssembleResult.ResultType.fromByte(resultCode), buf);
                        result.assemblyInteractor = new ShipAssemblyInteractor().fromByteBuf(resultCode, buf);

                        if (entityPlayer != null && entityPlayer.openContainer instanceof ContainerHelm) {
                            if (prevFlag) {
                                ((ContainerHelm) entityPlayer.openContainer).tileEntity.setPrevAssembleResult(result);
                                ((ContainerHelm) entityPlayer.openContainer).tileEntity.getPrevAssembleResult().assemblyInteractor = result.assemblyInteractor;
                            } else {
                                ((ContainerHelm) entityPlayer.openContainer).tileEntity.setAssembleResult(result);
                                ((ContainerHelm) entityPlayer.openContainer).tileEntity.getAssembleResult().assemblyInteractor = result.assemblyInteractor;
                            }
                        }

                    }
                });

        builder = builder.packet("ClientRequestSubmerseMessage").boundTo(Side.SERVER)
                .with(DataType.INT, "dimID")
                .with(DataType.INT, "entityID")
                .with(DataType.BOOLEAN, "submerse").handledOnMainThreadBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        boolean submerse = token.getBoolean("submerse");
                        World world = DimensionManager.getWorld(token.getInt("dimID"));
                        if (world != null) {
                            Entity unCast = world.getEntityByID(token.getInt("entityID"));
                            if (unCast != null && unCast instanceof EntityShip) {
                                EntityShip ship = (EntityShip) unCast;

                                if (submerse && !ship.canSubmerge()) {
                                    if (entityPlayer != null && entityPlayer instanceof EntityPlayerMP)
                                        ((EntityPlayerMP) entityPlayer).playerNetServerHandler.kickPlayerFromServer("Oi, stop hacking ya moron.  \n XOXO ~Archimedes");
                                    return;
                                }

                                ship.setSubmerge(submerse);
                            }
                        }
                    }
                });

        builder = builder.packet("ClientHelmActionMessage").boundTo(Side.SERVER)
                .with(DataType.INT, "action")
                .with(DataType.INT, "tileX")
                .with(DataType.INT, "tileY")
                .with(DataType.INT, "tileZ")
                .handledOnMainThreadBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        BlockPos pos = new BlockPos(token.getInt("tileX"),
                                token.getInt("tileY"), token.getInt("tileZ"));
                        HelmClientAction action = HelmClientAction.fromInt((byte) token.getInt("action"));

                        if (entityPlayer.worldObj.getTileEntity(pos) != null && entityPlayer.worldObj.getTileEntity(pos) instanceof TileEntityHelm) {
                            TileEntityHelm tileEntity = (TileEntityHelm) entityPlayer.worldObj.getTileEntity(pos);
                            switch (action) {
                                case ASSEMBLE:
                                    tileEntity.assembleMovingWorld(entityPlayer);
                                    break;
                                case MOUNT:
                                    tileEntity.mountMovingWorld(entityPlayer, tileEntity.getMovingWorld(tileEntity.getWorld()));
                                    break;
                                case UNDOCOMPILE:
                                    tileEntity.undoCompilation(entityPlayer);
                                    break;
                                default:
                                    break;
                            }
                        }
                    }
                });

        builder = builder.packet("ClientRenameShipMessage").boundTo(Side.SERVER)
                .with(DataType.STRING, "newName")
                .with(DataType.INT, "tileX")
                .with(DataType.INT, "tileY")
                .with(DataType.INT, "tileZ")
                .handledOnMainThreadBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        BlockPos pos = new BlockPos(token.getInt("tileX"),
                                token.getInt("tileY"), token.getInt("tileZ"));

                        if (entityPlayer.worldObj.getTileEntity(pos) != null && entityPlayer.worldObj.getTileEntity(pos) instanceof TileEntityHelm) {
                            TileEntityHelm helm = (TileEntityHelm) entityPlayer.worldObj.getTileEntity(pos);
                            helm.getInfo().setName(token.getString("newName"));
                        }
                    }
                });

        builder = builder.packet("ClientOpenGUIMessage").boundTo(Side.SERVER)
                .with(DataType.INT, "guiID").handledOnMainThreadBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        entityPlayer.openGui(ArchimedesShipMod.instance, token.getInt("guiID"), entityPlayer.worldObj, 0, 0, 0);
                    }
                });

        builder = builder.packet("ControlInputMessage").boundTo(Side.SERVER)
                .with(DataType.INT, "dimID")
                .with(DataType.INT, "entityID")
                .with(DataType.BYTE, "control").handledOnMainThreadBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        World world = DimensionManager.getWorld(token.getInt("dimID"));
                        if (world != null) {
                            Entity unCast = world.getEntityByID(token.getInt("entityID"));
                            if (unCast != null && unCast instanceof EntityShip) {
                                EntityShip ship = (EntityShip) unCast;

                                ship.getController().updateControl(ship, entityPlayer, token.getInt("control"));
                            }
                        }
                    }
                });

        builder = builder.packet("TranslatedChatMessage").boundTo(Side.CLIENT)
                .with(DataType.STRING, "message").handledOnMainThreadBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        if (entityPlayer == null)
                            return;

                        String message = token.getString("message");

                        String[] split = message.split("~");
                        ChatComponentText text = new ChatComponentText("");

                        if (split.length > 0) {
                            for (String string : split) {
                                if (string.startsWith("TR:")) {
                                    text.appendSibling(new ChatComponentTranslation(string.substring(3)));
                                } else {
                                    text.appendSibling(new ChatComponentText(string));
                                }
                            }
                        }

                        entityPlayer.addChatComponentMessage(text);
                    }
                });

        builder = builder.packet("ConfigMessage").boundTo(Side.CLIENT)
                .with(DataType.NBT_COMPOUND, "data")
                .handledOnMainThreadBy(new BiConsumer<EntityPlayer, Token>() {
                    @Override
                    public void accept(EntityPlayer entityPlayer, Token token) {
                        NBTTagCompound tag = token.getNBT("data");
                        ArchimedesConfig.SharedConfig config = null;

                        if (!tag.getBoolean("restore")) {
                            config = ArchimedesShipMod.instance.getLocalConfig().getShared()
                                    .deserialize(tag);
                        }

                        if (ArchimedesShipMod.proxy != null && ArchimedesShipMod.proxy instanceof ClientProxy) {
                            if (config != null) {
                                ((ClientProxy) ArchimedesShipMod.proxy).syncedConfig = ArchimedesShipMod.instance.getLocalConfig();
                                ((ClientProxy) ArchimedesShipMod.proxy).syncedConfig.setShared(config);
                            } else {
                                ((ClientProxy) ArchimedesShipMod.proxy).syncedConfig = null;
                            }
                        }

                    }
                });

        return builder;
    }
}
