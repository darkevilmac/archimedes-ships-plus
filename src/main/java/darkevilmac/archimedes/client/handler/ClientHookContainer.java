package darkevilmac.archimedes.client.handler;

import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.common.entity.EntityShip;
import darkevilmac.archimedes.common.handler.CommonHookContainer;
import darkevilmac.movingworld.common.network.MovingWorldNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class ClientHookContainer extends CommonHookContainer {

    public static ResourceLocation PLUS_LOCATION = new ResourceLocation(ArchimedesShipMod.RESOURCE_DOMAIN, "/textures/gui/plus.png");

    @SubscribeEvent
    public void onEntitySpawn(EntityJoinWorldEvent event) {
        if (event.world.isRemote && event.entity instanceof EntityShip) {
            if (((EntityShip) event.entity).getMobileChunk().chunkTileEntityMap.isEmpty()) {
                return;
            }

            MovingWorldNetworking.NETWORK.send().packet("RequestMovingWorldDataMessage")
                    .with("dimID", event.world.provider.getDimensionId())
                    .with("entityID", event.entity.getEntityId()).toServer();
        }
    }

    @SubscribeEvent
    public void onPlayerRender(RenderPlayerEvent.Post e) {
        if (true || e.isCanceled() || e.entityPlayer == null || !(e.renderer instanceof RenderPlayer)) // Temporarily exit this code it's not working yet.
            return;
    }

    protected boolean canRenderPlate(EntityPlayer entity, Render entityRender) {
        EntityPlayerSP entityplayersp = Minecraft.getMinecraft().thePlayer;

        if (entity instanceof EntityPlayer && entity != entityplayersp) {
            Team team = entity.getTeam();
            Team team1 = entityplayersp.getTeam();

            if (team != null) {
                Team.EnumVisible team$enumvisible = team.getNameTagVisibility();

                switch (team$enumvisible) {
                    case ALWAYS:
                        return true;
                    case NEVER:
                        return false;
                    case HIDE_FOR_OTHER_TEAMS:
                        return team1 == null || team.isSameTeam(team1);
                    case HIDE_FOR_OWN_TEAM:
                        return team1 == null || !team.isSameTeam(team1);
                    default:
                        return true;
                }
            }
        }
        return true;
    }

}
