package darkevilmac.archimedes.client.handler;

import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.common.entity.EntityShip;
import darkevilmac.archimedes.common.handler.CommonHookContainer;
import darkevilmac.movingworld.common.network.MovingWorldNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.entity.RenderPlayer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;

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

        //TODO: Check if user is patron somehow.

        if (canRenderPlate(e.entityPlayer, e.renderer)) {
            RenderManager renderManager = Minecraft.getMinecraft().getRenderManager();

            double x = e.x;
            double y = e.y;
            double z = e.z;

            double d0 = e.entity.getDistanceSqToEntity(renderManager.livingPlayer);

            if (d0 <= (double) (64 * 64)) {
                float f = 1.6F;
                float f1 = 0.016666668F * f;
                GlStateManager.pushMatrix();
                GlStateManager.translate((float) x + 0.0F, (float) y + e.entity.height + 0.5F, (float) z);
                GL11.glNormal3f(0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(-renderManager.playerViewY, 0.0F, 1.0F, 0.0F);
                GlStateManager.rotate(renderManager.playerViewX, 1.0F, 0.0F, 0.0F);
                GlStateManager.scale(-f1, -f1, f1);
                GlStateManager.disableLighting();
                GlStateManager.enableBlend();

                Tessellator tessellator = Tessellator.getInstance();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();

                int heightAdjust = -32;
                int size = 32;
                Minecraft.getMinecraft().renderEngine.bindTexture(PLUS_LOCATION);
                worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);

                double posXA = (double) (-size - 1);
                double posXB = (double) (size + 1);
                double posYA = (double) (-1 + heightAdjust);
                double posYB = (double) (8 + heightAdjust);

                worldrenderer.pos(posXA, posYA, 0.0D).endVertex();
                worldrenderer.pos(posXA, posYB, 0.0D).endVertex();
                worldrenderer.pos(posXB, posYB, 0.0D).endVertex();
                worldrenderer.pos(posXB, posYA, 0.0D).endVertex();
                tessellator.draw();

                GlStateManager.enableLighting();
                GlStateManager.disableBlend();
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.popMatrix();
            }
        }
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
