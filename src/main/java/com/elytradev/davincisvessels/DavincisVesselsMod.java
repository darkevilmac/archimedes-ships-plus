package com.elytradev.davincisvessels;

import com.elytradev.davincisvessels.client.ClientProxy;
import com.elytradev.davincisvessels.common.CommonProxy;
import com.elytradev.davincisvessels.common.DavincisVesselsConfig;
import com.elytradev.davincisvessels.common.command.*;
import com.elytradev.davincisvessels.common.entity.EntityParachute;
import com.elytradev.davincisvessels.common.entity.EntitySeat;
import com.elytradev.davincisvessels.common.entity.EntityShip;
import com.elytradev.davincisvessels.common.handler.ConnectionHandler;
import com.elytradev.davincisvessels.common.network.DavincisVesselsNetworking;
import com.elytradev.davincisvessels.common.object.DavincisVesselsObjects;
import net.minecraft.command.CommandBase;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.registry.EntityRegistry;
import org.apache.logging.log4j.Logger;

import java.util.Collections;

@Mod(modid = DavincisVesselsMod.MOD_ID, name = DavincisVesselsMod.MOD_NAME, version = DavincisVesselsMod.MOD_VERSION, dependencies = "required-after:movingworld", guiFactory = DavincisVesselsMod.MOD_GUIFACTORY)
public class DavincisVesselsMod {

    public static final String MOD_ID = "davincisvessels";
    public static final String MOD_VERSION = "@DVESSELSVER@";
    public static final String MOD_NAME = "Davinci's Vessels";
    public static final String RESOURCE_DOMAIN = "davincisvessels:";
    public static final String MOD_GUIFACTORY = "com.elytradev.davincisvessels.client.gui.DavincisVesselsGUIFactory";
    public static final DavincisVesselsObjects OBJECTS = new DavincisVesselsObjects();
    @Mod.Instance(MOD_ID)
    public static DavincisVesselsMod INSTANCE;
    @SidedProxy(clientSide = "com.elytradev.davincisvessels.client.ClientProxy", serverSide = "com.elytradev.davincisvessels.common.CommonProxy")
    public static CommonProxy PROXY;
    public static Logger LOG;

    public static CreativeTabs CREATIVE_TAB = new CreativeTabs("davincisTab") {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(DavincisVesselsObjects.blockMarkShip);
        }
    };

    private DavincisVesselsConfig localConfig;

    public DavincisVesselsConfig getNetworkConfig() {
        if (FMLCommonHandler.instance().getSide().isClient()) {
            if (((ClientProxy) PROXY).syncedConfig != null)
                return ((ClientProxy) PROXY).syncedConfig;
        }
        return localConfig;
    }

    public DavincisVesselsConfig getLocalConfig() {
        return localConfig;
    }

    @Mod.EventHandler
    public void preInitMod(FMLPreInitializationEvent event) {
        LOG = event.getModLog();

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(PROXY);
        MinecraftForge.EVENT_BUS.register(OBJECTS);

        OBJECTS.preInit(event);

        localConfig = new DavincisVesselsConfig(new Configuration(event.getSuggestedConfigurationFile()));
        localConfig.loadAndSave();
        EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "ship"), EntityShip.class, "shipmod", 1, this, 64, localConfig.getShared().shipEntitySyncRate, true);
        EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "seat"), EntitySeat.class, "attachment.seat", 2, this, 64, 20, false);
        EntityRegistry.registerModEntity(new ResourceLocation(MOD_ID, "parachute"), EntityParachute.class, "parachute", 3, this, 32, localConfig.getShared().shipEntitySyncRate, true);
        PROXY.registerRenderers(event.getModState());
    }

    @Mod.EventHandler
    public void initMod(FMLInitializationEvent event) {
        DavincisVesselsNetworking.setupNetwork();
        OBJECTS.init(event);

        MinecraftForge.EVENT_BUS.register(new ConnectionHandler());
        PROXY.registerKeyHandlers(localConfig);
        PROXY.registerEventHandlers();
        PROXY.registerRenderers(event.getModState());

        localConfig.postLoad();
        localConfig.addBlacklistWhitelistEntries();
    }

    @Mod.EventHandler
    public void postInitMod(FMLPostInitializationEvent event) {
        PROXY.registerRenderers(event.getModState());
    }

    @Mod.EventHandler
    public void serverStarting(FMLServerStartingEvent event) {
        registerASCommand(event, new CommandDVHelp());
        registerASCommand(event, new CommandDisassembleShip());
        registerASCommand(event, new CommandShipInfo());
        registerASCommand(event, new CommandDisassembleNear());
        registerASCommand(event, new CommandDVTP());
        Collections.sort(CommandDVHelp.asCommands);
    }

    private void registerASCommand(FMLServerStartingEvent event, CommandBase commandbase) {
        event.registerServerCommand(commandbase);
        CommandDVHelp.asCommands.add(commandbase);
    }

}
