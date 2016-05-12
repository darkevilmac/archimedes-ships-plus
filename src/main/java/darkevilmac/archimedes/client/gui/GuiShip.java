package darkevilmac.archimedes.client.gui;

import darkevilmac.archimedes.common.entity.EntityShip;
import darkevilmac.archimedes.common.network.ArchimedesShipsNetworking;
import darkevilmac.movingworld.common.network.MovingWorldClientAction;
import darkevilmac.movingworld.common.network.MovingWorldNetworking;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.opengl.GL11;

public class GuiShip extends GuiContainer {
    public static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("archimedesshipsplus", "textures/gui/shipinv.png");

    public final EntityShip ship;
    public final EntityPlayer player;

    private GuiButton btnDisassemble, btnAlign, btnSubmersible;

    public GuiShip(EntityShip entityship, EntityPlayer entityplayer) {
        super(new ContainerShip(entityship, entityplayer));
        ship = entityship;
        player = entityplayer;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();
        buttonList.clear();

        btnDisassemble = new GuiButton(1, guiLeft + 4, guiTop + 20, 100, 20, StatCollector.translateToLocal("gui.shipinv.decompile"));
        btnDisassemble.enabled = ship.getDisassembler().canDisassemble(ship.getAssemblyInteractor());
        buttonList.add(btnDisassemble);

        btnAlign = new GuiButton(2, guiLeft + 4, guiTop + 40, 100, 20, StatCollector.translateToLocal("gui.shipinv.align"));
        buttonList.add(btnAlign);


        btnSubmersible = new GuiButtonSubmersible(3, guiLeft + xSize + 2, guiTop);
        ((GuiButtonSubmersible) btnSubmersible).canDo = ship.canSubmerge();
        if (ship.canSubmerge())
            ((GuiButtonSubmersible) btnSubmersible).submerse = ship.getSubmerge();
        else
            ((GuiButtonSubmersible) btnSubmersible).submerse = false;
        buttonList.add(btnSubmersible);

    }

    @Override
    public void updateScreen() {
        if (this.mc != null && this.mc.thePlayer != null) super.updateScreen();

        if (btnDisassemble == null || btnAlign == null) {
            return;
        }

        btnDisassemble.xPosition = btnAlign.xPosition = guiLeft + 4;
        int y = guiTop + 20;
        btnDisassemble.yPosition = y;
        btnAlign.yPosition = y += 20;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mousex, int mousey) {
        int color = 0x404040;
        int row = 8;
        int col0 = 8;

        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipinv.title") + " - " + ship.getInfo().getName(), col0, row, color);
        row += 5;

        fontRendererObj.drawString(StatCollector.translateToLocal("container.inventory"), 8, ySize - 96 + 2, color);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float var1, int var2, int var3) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.renderEngine.bindTexture(BACKGROUND_TEXTURE);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        if (button == btnDisassemble) {
            MovingWorldNetworking.NETWORK.send().packet("MovingWorldClientActionMessage")
                    .with("dimID", ship.worldObj.provider.getDimensionId())
                    .with("entityID", ship.getEntityId())
                    .with("action", MovingWorldClientAction.DISASSEMBLE.toByte())
                    .toServer();
            mc.displayGuiScreen(null);
        } else if (button == btnAlign) {
            MovingWorldNetworking.NETWORK.send().packet("MovingWorldClientActionMessage")
                    .with("dimID", ship.worldObj.provider.getDimensionId())
                    .with("entityID", ship.getEntityId())
                    .with("action", MovingWorldClientAction.ALIGN.toByte())
                    .toServer();

            ship.alignToGrid();
        } else if (button == btnSubmersible && ((GuiButtonSubmersible) btnSubmersible).canDo) {
            GuiButtonSubmersible subButton = (GuiButtonSubmersible) button;

            ArchimedesShipsNetworking.NETWORK.send().packet("ClientRequestSubmerseMessage")
                    .with("dimID", ship.worldObj.provider.getDimensionId())
                    .with("entityID", ship.getEntityId())
                    .with("submerse", !subButton.submerse).toServer();

            subButton.submerse = !subButton.submerse;
        }
    }
}
