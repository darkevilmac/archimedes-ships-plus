package darkevilmac.archimedes.client.gui;

import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.common.entity.ShipAssemblyInteractor;
import darkevilmac.archimedes.common.network.ArchimedesShipsNetworking;
import darkevilmac.archimedes.common.network.HelmClientAction;
import darkevilmac.archimedes.common.tileentity.TileEntityHelm;
import darkevilmac.movingworld.common.chunk.assembly.AssembleResult;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.lwjgl.input.Keyboard;
import org.lwjgl.opengl.GL11;

import java.io.IOException;
import java.util.Locale;

import static darkevilmac.movingworld.common.chunk.assembly.AssembleResult.ResultType;
import static darkevilmac.movingworld.common.chunk.assembly.AssembleResult.ResultType.*;

public class GuiHelm extends GuiContainer {
    public static final ResourceLocation BACKGROUND_TEXTURE = new ResourceLocation("archimedesshipsplus", "textures/gui/shipstatus.png");

    public final TileEntityHelm tileEntity;
    public final EntityPlayer player;

    private GuiButton btnRename, btnAssemble, btnUndo, btnMount;
    private GuiTextField txtShipName;
    private boolean busyCompiling;

    public GuiHelm(TileEntityHelm tileentity, EntityPlayer entityplayer) {
        super(new ContainerHelm(tileentity, entityplayer));
        tileEntity = tileentity;
        player = entityplayer;

        xSize = 256;
        ySize = 256;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void initGui() {
        super.initGui();

        Keyboard.enableRepeatEvents(true);

        int btnx = guiLeft - 100;
        int btny = guiTop + 20;
        buttonList.clear();

        btnRename = new GuiButton(4, btnx, btny, 100, 20, StatCollector.translateToLocal("gui.shipstatus.rename"));
        buttonList.add(btnRename);

        btnAssemble = new GuiButton(1, btnx, btny += 20, 100, 20, StatCollector.translateToLocal("gui.shipstatus.compile"));
        buttonList.add(btnAssemble);

        btnUndo = new GuiButton(2, btnx, btny += 20, 100, 20, StatCollector.translateToLocal("gui.shipstatus.undo"));
        btnUndo.enabled = tileEntity.getPrevAssembleResult() != null && tileEntity.getPrevAssembleResult().getCode() != RESULT_NONE;
        buttonList.add(btnUndo);

        btnMount = new GuiButton(3, btnx, btny += 20, 100, 20, StatCollector.translateToLocal("gui.shipstatus.mount"));
        btnMount.enabled = tileEntity.getAssembleResult() != null && tileEntity.getAssembleResult().getCode() == RESULT_OK;
        buttonList.add(btnMount);

        txtShipName = new GuiTextField(0, fontRendererObj, guiLeft + 8 + xSize / 2, guiTop + 21, 120, 10); // TODO: Might be incorrect not sure about 0 in GuiTextField()
        txtShipName.setMaxStringLength(127);
        txtShipName.setEnableBackgroundDrawing(false);
        txtShipName.setVisible(true);
        txtShipName.setCanLoseFocus(false);
        txtShipName.setTextColor(0xFFFFFF);
        txtShipName.setText(tileEntity.getInfo().getName());
    }

    @Override
    public void onGuiClosed() {
        super.onGuiClosed();
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        super.updateScreen();
        btnUndo.enabled = tileEntity.getPrevAssembleResult() != null && tileEntity.getPrevAssembleResult().getCode() != RESULT_NONE;
        btnMount.enabled = tileEntity.getAssembleResult() != null && tileEntity.getAssembleResult().getCode() == RESULT_OK;

        btnRename.xPosition = btnAssemble.xPosition = btnUndo.xPosition = btnMount.xPosition = guiLeft - 100;

        int y = guiTop + 20;
        btnRename.yPosition = y;
        btnAssemble.yPosition = y += 20;
        btnUndo.yPosition = y += 20;
        btnMount.yPosition = y += 20;
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mousex, int mousey) {
        AssembleResult result = tileEntity.getAssembleResult();

        int color = 0x404040;
        int row = 8;
        int col0 = 8;
        int col1 = col0 + xSize / 2;

        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.title"), col0, row, color);
        row += 5;
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.name"), col0, row += 10, color);

        ResultType resultType;
        int rblocks;
        int rballoons;
        int rtes;
        float rmass;

        if (result == null || (result != null && result.assemblyInteractor == null) || (result != null && result.assemblyInteractor != null && !(result.assemblyInteractor instanceof ShipAssemblyInteractor))) {
            resultType = busyCompiling ? RESULT_BUSY_COMPILING : RESULT_NONE;
            rblocks = rballoons = rtes = 0;
            rmass = 0f;
        } else {
            resultType = result.getCode();
            rblocks = result.getBlockCount();
            rballoons = ((ShipAssemblyInteractor) result.assemblyInteractor).getBalloonCount();
            rtes = result.getTileEntityCount();
            rmass = result.getMass();
            if (resultType != RESULT_NONE) {
                busyCompiling = false;
            }
        }

        String rcodename;
        int color1;
        switch (resultType) {
            case RESULT_NONE:
                color1 = color;
                rcodename = "gui.shipstatus.result.none";
                break;
            case RESULT_OK:
                color1 = 0x40A000;
                rcodename = "gui.shipstatus.result.ok";
                break;
            case RESULT_OK_WITH_WARNINGS:
                color1 = 0xFFAA00;
                rcodename = "gui.shipstatus.result.okwarn";
                break;
            case RESULT_MISSING_MARKER:
                color1 = 0xB00000;
                rcodename = "gui.shipstatus.result.missingmarker";
                break;
            case RESULT_BLOCK_OVERFLOW:
                color1 = 0xB00000;
                rcodename = "gui.shipstatus.result.overflow";
                break;
            case RESULT_ERROR_OCCURED:
                color1 = 0xB00000;
                rcodename = "gui.shipstatus.result.error";
                break;
            case RESULT_BUSY_COMPILING:
                color1 = color;
                rcodename = "gui.shipstatus.result.busy";
                break;
            case RESULT_INCONSISTENT:
                color1 = 0xB00000;
                rcodename = "gui.shipstatus.result.inconsistent";
                break;
            default:
                color1 = color;
                rcodename = "gui.shipstatus.result.none";
                break;
        }

        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.compilerresult"), col0, row += 10, color);
        fontRendererObj.drawString(StatCollector.translateToLocal(rcodename), col1, row, color1);

        float balloonratio = (float) rballoons / rblocks;
        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.shiptype"), col0, row += 10, color);
        if (rblocks == 0) {
            fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.type.unknown"), col1, row, color);
        } else {
            fontRendererObj.drawString(StatCollector.translateToLocal(balloonratio > ArchimedesShipMod.instance.getNetworkConfig().getShared().flyBalloonRatio ? "gui.shipstatus.type.airship" : "gui.shipstatus.type.boat"), col1, row, color);
        }

        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.count.block"), col0, row += 10, color);
        fontRendererObj.drawString(String.valueOf(rblocks), col1, row, color);

        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.count.balloon"), col0, row += 10, color);
        fontRendererObj.drawString(String.valueOf(rballoons) + " (" + (int) (balloonratio * 100f) + "%)", col1, row, color);

        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.count.tileentity"), col0, row += 10, color);
        fontRendererObj.drawString(String.valueOf(rtes), col1, row, color);

        fontRendererObj.drawString(StatCollector.translateToLocal("gui.shipstatus.mass"), col0, row += 10, color);
        fontRendererObj.drawString(String.format(Locale.ROOT, "%.1f %s", rmass, StatCollector.translateToLocal("gui.shipstatus.massunit")), col1, row, color);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(float par1, int par2, int par3) {
        GL11.glColor4f(1F, 1F, 1F, 1F);
        mc.renderEngine.bindTexture(BACKGROUND_TEXTURE);
        int x = (width - xSize) / 2;
        int y = (height - ySize) / 2;
        drawTexturedModalRect(x, y, 0, 0, xSize, ySize);

        txtShipName.drawTextBox();
    }

    @Override
    public void actionPerformed(GuiButton button) {
        if (button == btnRename) {
            if (txtShipName.isFocused()) {
                btnRename.displayString = StatCollector.translateToLocal("gui.shipstatus.rename");
                tileEntity.getInfo().setName(txtShipName.getText());
                txtShipName.setFocused(false);
                //txtShipName.setEnableBackgroundDrawing(false);

                ArchimedesShipsNetworking.NETWORK.send().packet("ClientRenameShipMessage")
                        .with("tileX", tileEntity.getPos().getX())
                        .with("tileY", tileEntity.getPos().getY())
                        .with("tileZ", tileEntity.getPos().getZ())
                        .with("newName", tileEntity.getInfo().getName()).toServer();
            } else {
                btnRename.displayString = StatCollector.translateToLocal("gui.shipstatus.done");
                txtShipName.setFocused(true);
                //txtShipName.setEnableBackgroundDrawing(true);
            }
        } else if (button == btnAssemble) {
            ArchimedesShipsNetworking.NETWORK.send().packet("ClientHelmActionMessage")
                    .with("tileX", tileEntity.getPos().getX())
                    .with("tileY", tileEntity.getPos().getY())
                    .with("tileZ", tileEntity.getPos().getZ())
                    .with("action", HelmClientAction.ASSEMBLE.toInt()).toServer();
            tileEntity.setAssembleResult(null);
            busyCompiling = true;
        } else if (button == btnMount) {
            ArchimedesShipsNetworking.NETWORK.send().packet("ClientHelmActionMessage")
                    .with("tileX", tileEntity.getPos().getX())
                    .with("tileY", tileEntity.getPos().getY())
                    .with("tileZ", tileEntity.getPos().getZ())
                    .with("action", HelmClientAction.MOUNT.toInt()).toServer();
        } else if (button == btnUndo) {
            ArchimedesShipsNetworking.NETWORK.send().packet("ClientHelmActionMessage")
                    .with("tileX", tileEntity.getPos().getX())
                    .with("tileY", tileEntity.getPos().getY())
                    .with("tileZ", tileEntity.getPos().getZ())
                    .with("action", HelmClientAction.UNDOCOMPILE.toInt()).toServer();
        }
    }

    @Override
    protected void keyTyped(char c, int k) {
        if (!checkHotbarKeys(k)) {
            if (k == Keyboard.KEY_RETURN && txtShipName.isFocused()) {
                actionPerformed(btnRename);
            } else if (txtShipName.textboxKeyTyped(c, k)) {
            } else {
                try {
                    super.keyTyped(c, k);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
