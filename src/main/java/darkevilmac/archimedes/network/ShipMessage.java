package darkevilmac.archimedes.network;

import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.entity.EntityShip;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Created by DarkEvilMac on 4/19/2015.
 */

public abstract class ShipMessage extends ArchimedesShipsMessage {
    public EntityShip ship;

    public ShipMessage() {
        ship = null;
    }

    public ShipMessage(EntityShip ship) {
        this.ship = ship;
    }


    @Override
    public void encodeInto(ChannelHandlerContext ctx, ByteBuf buf) {
        buf.writeInt(ship.getEntityId());
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf buf, EntityPlayer player) {
        int entityID = buf.readInt();
        Entity entity = player.worldObj.getEntityByID(entityID);
        if (entity instanceof EntityShip) {
            ship = (EntityShip) entity;
        } else {
            ArchimedesShipMod.modLog.warn("Unable to find Ship entity with ID " + entityID);
        }
    }
}
