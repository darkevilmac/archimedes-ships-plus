package darkevilmac.archimedes;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ListMultimap;
import darkevilmac.archimedes.client.ClientProxy;
import darkevilmac.archimedes.common.ArchimedesConfig;
import darkevilmac.archimedes.common.CommonProxy;
import darkevilmac.archimedes.common.command.CommandASHelp;
import darkevilmac.archimedes.common.command.CommandDisassembleNear;
import darkevilmac.archimedes.common.command.CommandDisassembleShip;
import darkevilmac.archimedes.common.command.CommandShipInfo;
import darkevilmac.archimedes.common.entity.EntityParachute;
import darkevilmac.archimedes.common.entity.EntitySeat;
import darkevilmac.archimedes.common.entity.EntityShip;
import darkevilmac.archimedes.common.handler.ConnectionHandler;
import darkevilmac.archimedes.common.network.ArchimedesShipsNetworking;
import darkevilmac.archimedes.common.object.ArchimedesObjects;
import net.minecraft.command.CommandBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collections;
import java.util.List;

@Mod(modid = ArchimedesShipMod.MOD_ID, name = ArchimedesShipMod.MOD_NAME, version = ArchimedesShipMod.MOD_VERSION, dependencies = "required-after:MovingWorld", guiFactory = ArchimedesShipMod.MOD_GUIFACTORY)
public class ArchimedesShipMod {

    public static final String MOD_ID = "ArchimedesShipsPlus";
    public static final String MOD_VERSION = "@AS+VER@";
    public static final String MOD_NAME = "Archimedes' Ships Plus";
    public static final String RESOURCE_DOMAIN = "archimedesshipsplus:";
    public static final String MOD_GUIFACTORY = "darkevilmac.archimedes.client.gui.ArchimedesGUIFactory";

    public static CreativeTabs creativeTab = new CreativeTabs("archimedesTab") {
        @Override
        public Item getTabIconItem() {
            return Item.getItemFromBlock(ArchimedesObjects.blockMarkShip);
        }
    };

    @Mod.Instance(MOD_ID)
    public static ArchimedesShipMod instance;

    @SidedProxy(clientSide = "darkevilmac.archimedes.client.ClientProxy", serverSide = "darkevilmac.archimedes.common.CommonProxy")
    public static CommonProxy proxy;

    public static ArchimedesObjects objects;

    public static Logger modLog;
    private ArchimedesConfig localConfig;

    public ArchimedesConfig getNetworkConfig() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            if (((ClientProxy) proxy).syncedConfig != null)
                return ((ClientProxy) proxy).syncedConfig;
        }
        return localConfig;
    }

    public ArchimedesConfig getLocalConfig() {
        return localConfig;
    }

    @Mod.EventHandler
    public void preInitMod(FMLPreInitializationEvent event) {
        modLog = event.getModLog();
        objects = new ArchimedesObjects();

        MinecraftForge.EVENT_BUS.register(this);

        objects.preInit(event);
        localConfig = new ArchimedesConfig(new Configuration(event.getSuggestedConfigurationFile()));
        localConfig.loadAndSave();

        localConfig.postLoad();
        proxy.registerRenderers(event.getModState());
    }

    @Mod.EventHandler
    public void initMod(FMLInitializationEvent event) {
        ArchimedesShipsNetworking.setupNetwork();
        objects.init(event);

        MinecraftForge.EVENT_BUS.register(new ConnectionHandler());
        MinecraftForge.EVENT_BUS.register(new ConnectionHandler());

        EntityRegistry.registerModEntity(EntityShip.class, "shipmod", 1, this, 64, localConfig.getShared().shipEntitySyncRate, true);
        EntityRegistry.registerModEntity(EntitySeat.class, "attachment.seat", 2, this, 64, 100, false);
        EntityRegistry.registerModEntity(EntityParachute.class, "parachute", 3, this, 32, localConfig.getShared().shipEntitySyncRate, true);

        proxy.registerKeyHandlers(localConfig);
        proxy.registerEventHandlers();
        proxy.registerRenderers(event.getModState());

        localConfig.addBlacklistWhitelistEntries();
    }

    @Mod.EventHandler
    public void postInitMod(FMLPostInitializationEvent event) {
        proxy.registerRenderers(event.getModState());
    }

    @Mod.EventHandler
    public void missingMappingsFound(FMLMissingMappingsEvent event) {
        if (event != null && event.getAll() != null && !event.getAll().isEmpty()) {
            ListMultimap<String, FMLMissingMappingsEvent.MissingMapping> missing = ReflectionHelper.getPrivateValue(FMLMissingMappingsEvent.class, event, "missing");
            if (missing != null) {
                List<FMLMissingMappingsEvent.MissingMapping> missingMappingList = ImmutableList.copyOf(missing.get("ArchimedesShips"));

                if (missingMappingList != null && !missingMappingList.isEmpty()) {
                    Logger log = LogManager.getLogger(MOD_ID);

                    log.info("ARCHIMEDES LEGACY MAPPINGS FOUND");

                    for (FMLMissingMappingsEvent.MissingMapping mapping : missingMappingList) {
                        if (mapping != null && mapping.type != null && mapping.name != null && !mapping.name.isEmpty()) {

                            String name = mapping.name.substring("ArchimedesShips:".length());

                            if (mapping.type == GameRegistry.Type.BLOCK) {
                                mapping.remap(GameRegistry.findBlock(ArchimedesShipMod.MOD_ID, name));
                            } else {
                                mapping.remap(Item.getItemFromBlock(GameRegistry.findBlock(ArchimedesShipMod.MOD_ID, name)));
                            }

                            log.debug("ArchimedesShips:" + name + " ~~~> " + "ArchimedesShipsPlus:" + name);
                        }
                    }

                    log.info("REMAPPED TO ARCHIMEDES SHIPS PLUS, ENJOY! ~Darkevilmac");
                }
            }
        }
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        registerASCommand(event, new CommandASHelp());
        registerASCommand(event, new CommandDisassembleShip());
        registerASCommand(event, new CommandShipInfo());
        registerASCommand(event, new CommandDisassembleNear());
        Collections.sort(CommandASHelp.asCommands);
    }

    private void registerASCommand(FMLServerStartingEvent event, CommandBase commandbase) {
        event.registerServerCommand(commandbase);
        CommandASHelp.asCommands.add(commandbase);
    }

}
