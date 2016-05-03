package darkevilmac.archimedes.common.network;

import darkevilmac.movingworld.MovingWorld;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.network.FMLIndexedMessageToMessageCodec;

public class ArchimedesShipsMessageToMessageCodec extends FMLIndexedMessageToMessageCodec<ArchimedesShipsMessage> {

    private int index;

    public ArchimedesShipsMessageToMessageCodec() {
        index = 1;
        addDiscriminator(AssembleResultMessage.class);
        addDiscriminator(ClientHelmActionMessage.class);
        addDiscriminator(ClientOpenGuiMessage.class);
        addDiscriminator(ClientRenameShipMessage.class);
        addDiscriminator(ControlInputMessage.class);
        addDiscriminator(TranslatedChatMessage.class);
        addDiscriminator(ClientChangeSubmerseMessage.class);
        addDiscriminator(ConfigMessage.class);
    }

    public FMLIndexedMessageToMessageCodec<ArchimedesShipsMessage> addDiscriminator(Class<? extends ArchimedesShipsMessage> type) {
        MovingWorld.logger.info("Registering discriminator " + index + " " + type.getName());
        FMLIndexedMessageToMessageCodec<ArchimedesShipsMessage> ret = super.addDiscriminator(index, type);
        index++;
        return ret;
    }

    @Override
    public void encodeInto(ChannelHandlerContext ctx, ArchimedesShipsMessage msg, ByteBuf target) throws Exception {
        msg.encodeInto(ctx, target, FMLCommonHandler.instance().getSide());
    }

    @Override
    public void decodeInto(ChannelHandlerContext ctx, ByteBuf source, ArchimedesShipsMessage msg) {
        msg.decodeInto(ctx, source, FMLCommonHandler.instance().getSide());
    }
}
