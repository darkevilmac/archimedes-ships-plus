package io.github.elytra.davincisvessels.common.network.message;

import io.github.elytra.concrete.Message;
import io.github.elytra.concrete.NetworkContext;
import io.github.elytra.concrete.annotation.field.MarshalledAs;
import io.github.elytra.concrete.annotation.type.ReceivedOn;
import io.github.elytra.davincisvessels.DavincisVesselsMod;
import io.github.elytra.davincisvessels.common.network.DavincisVesselsNetworking;
import io.github.elytra.davincisvessels.common.network.marshallers.TileEntityMarshaller;
import io.github.elytra.davincisvessels.common.tileentity.AnchorInstance;
import io.github.elytra.davincisvessels.common.tileentity.BlockLocation;
import io.github.elytra.davincisvessels.common.tileentity.TileAnchorPoint;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.relauncher.Side;

import java.util.HashMap;
import java.util.UUID;

/**
 * Created by darkevilmac on 2/2/2017.
 */
@ReceivedOn(Side.SERVER)
public class AnchorPointMessage extends Message {

    @MarshalledAs(TileEntityMarshaller.MARSHALLER_NAME)
    public TileAnchorPoint anchorPoint;

    public AnchorPointMessage( TileAnchorPoint anchorPoint, TileAnchorPoint.AnchorPointAction action) {
        super(DavincisVesselsNetworking.NETWORK);
        this.anchorPoint = anchorPoint;
        this.action = action;
    }

    public TileAnchorPoint.AnchorPointAction action;

    public AnchorPointMessage(NetworkContext ctx) {
        super(ctx);
    }

    @Override
    protected void handle(EntityPlayer sender) {
        if (anchorPoint == null)
            return;

        World world = sender.world;


        if (action == TileAnchorPoint.AnchorPointAction.SWITCH) {
            // Switch
            /**
             * Clear the entries as well as notify the entries to clear us from them.
             * Then switch mode.
             */
            for (HashMap.Entry<UUID, BlockLocation> e : anchorPoint.getInstance().getRelatedAnchors().entrySet()) {
                if (world.getTileEntity(e.getValue().pos) != null && world.getTileEntity(e.getValue().pos) instanceof TileAnchorPoint) {
                    TileAnchorPoint entryAnchorPoint = (TileAnchorPoint) world.getTileEntity(e.getValue().pos);
                    ((EntityPlayerMP) sender).connection.sendPacket(entryAnchorPoint.getUpdatePacket());
                } else {
                    DavincisVesselsMod.LOG.error("Invalid entries in anchor tile: " + anchorPoint.toString() + ", cleaning.");
                }
            }

            anchorPoint.getInstance().clearRelations();
            anchorPoint.getInstance().setType(anchorPoint.getInstance().getType().opposite());
            anchorPoint.getInstance().setIdentifier(UUID.randomUUID());
            anchorPoint.markDirty();
        } else if (anchorPoint.content != null) {
            // Link
            /**
             * As a note, we don't set the relation of our own anchor because the anchor we
             * would relate it to has yet to be placed, we set this info when the anchor is placed.
             */
            if (anchorPoint.getInstance().getType() == AnchorInstance.InstanceType.FORLAND) {
                if (anchorPoint.content.getTagCompound() == null) {
                    anchorPoint.content.setTagCompound(new NBTTagCompound());
                }
                if (anchorPoint.content.getTagCompound().hasKey("INSTANCE"))
                    anchorPoint.content.getTagCompound().removeTag("INSTANCE");
                AnchorInstance itemAnchorInstanceTag = new AnchorInstance();

                itemAnchorInstanceTag.setType(AnchorInstance.InstanceType.FORSHIP);
                itemAnchorInstanceTag.setIdentifier(UUID.randomUUID());
                itemAnchorInstanceTag.addRelation(anchorPoint.getInstance().getIdentifier(),
                        new BlockLocation(anchorPoint.getPos(), sender.world.provider.getDimension()));
                anchorPoint.content.getTagCompound().setTag("INSTANCE", itemAnchorInstanceTag.serializeNBT());
            } else {
                if (anchorPoint.content.getTagCompound() == null) {
                    anchorPoint.content.setTagCompound(new NBTTagCompound());
                }
                if (anchorPoint.content.getTagCompound().hasKey("INSTANCE"))
                    anchorPoint.content.getTagCompound().removeTag("INSTANCE");

                AnchorInstance itemAnchorInstanceTag = new AnchorInstance();

                itemAnchorInstanceTag.setType(AnchorInstance.InstanceType.FORLAND);
                itemAnchorInstanceTag.setIdentifier(UUID.randomUUID());
                itemAnchorInstanceTag.addRelation(anchorPoint.getInstance().getIdentifier(),
                        new BlockLocation(anchorPoint.getPos(), sender.world.provider.getDimension()));
                anchorPoint.content.getTagCompound().setTag("INSTANCE", itemAnchorInstanceTag.serializeNBT());
            }
        }
        anchorPoint.markDirty();
        if (world instanceof WorldServer)
            ((WorldServer) world).getPlayerChunkMap().markBlockForUpdate(anchorPoint.getPos());
    }
}
