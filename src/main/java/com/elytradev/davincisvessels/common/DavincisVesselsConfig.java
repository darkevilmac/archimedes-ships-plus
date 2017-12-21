package com.elytradev.davincisvessels.common;

import com.elytradev.davincisvessels.DavincisVesselsMod;
import com.elytradev.davincisvessels.common.object.DavincisVesselsObjects;
import com.elytradev.movingworld.MovingWorldMod;
import com.google.gson.Gson;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.init.Blocks;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Configuration;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.ArrayUtils;
import org.lwjgl.input.Keyboard;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DavincisVesselsConfig {
    public static final int CONTROL_TYPE_VANILLA = 0, CONTROL_TYPE_DAVINCI = 1;

    @SideOnly(Side.CLIENT)
    public KeyBinding kbUp, kbDown, kbBrake, kbAlign, kbDisassemble, kbShipInv;

    public int anchorRadius;

    private Configuration config;
    private SharedConfig shared;

    public DavincisVesselsConfig(Configuration configuration) {
        shared = new SharedConfig();
        config = configuration;
        shared.balloonAlternatives = new HashSet<String>();
        shared.seats = new HashSet<String>();
        shared.stickyObjects = new HashSet<String>();

        MinecraftForge.EVENT_BUS.register(this); // For in game config reloads.
    }

    public void loadAndSave() {
        config.load();

        shared.shipEntitySyncRate = config.get("settings", "sync_rate", 5, "The amount of ticks between a server-client synchronization. Higher numbers reduce network traffic. Lower numbers increase multiplayer experience. 20 ticks = 1 second").getInt();
        shared.enableAirShips = config.get("settings", "enable_air_ships", true, "Enable or disable air ships.").getBoolean(true);
        shared.enableSubmersibles = config.get("settings", "enable_submersibles", true, "Enable or disable the ability to submerse ships.").getBoolean(true);
        shared.bankingMultiplier = (float) config.get("settings", "banking_multiplier", 3d, "A multiplier for how much ships bank while making turns. Set a positive value for passive banking or a negative value for active banking. 0 disables banking.").getDouble(3d);
        shared.enginesMandatory = config.get("settings", "mandatory_engines", false, "Are engines required for a ship to move?").getBoolean();
        shared.engineBurnSpeed = config.get("settings", "engine_burn_speed", 10, "How many fuel units an engine consumes per tick.").getInt();
        shared.enableShipDownfall = config.get("settings", "ship_fall", true, "Do ships slowly fall?").getBoolean();
        anchorRadius = config.get("settings", "anchor_radius", 12, "The radius around the ship that an anchor can snap to.").getInt();

        shared.shipControlType = config.get("control", "control_type", CONTROL_TYPE_DAVINCI, "Set to 0 to use vanilla boat controls, set to 1 to use Davinci controls.").getInt();
        shared.turnSpeed = (float) config.get("control", "turn_speed", 1D, "A multiplier of the ship's turn speed.").getDouble(1D);
        shared.speedLimit = (float) config.get("control", "speed_limit", 30D, "The maximum velocity a ship can have, in OBJECTS per second. This does not affect acceleration.").getDouble(30D);
        shared.speedLimit /= 20F;
        shared.disassembleOnDismount = config.get("control", "decompile_on_dismount", false).getBoolean(false);

        shared.maxShipChunkBlocks = config.get("mobile_chunk", "max_chunk_blocks", 2048, "The maximum amount of OBJECTS that a mobile ship chunk may contain.").getInt();
        shared.flyBalloonRatio = (float) config.get("mobile_chunk", "airship_balloon_ratio", 0.4D, "The part of the total amount of OBJECTS that should be balloon OBJECTS in order to make an airship.").getDouble(0.4D);
        shared.submersibleFillRatio = (float) config.get("mobile_chunk", "submersible_fill_ratio", 0.3D, "The part of the ship that needs to not be water fillable for it to be considered submersible.").getDouble(0.9D);

        if (FMLCommonHandler.instance().getSide().isClient()) {
            loadKeybindings();
        }

        config.save();
    }

    public void addBlacklistWhitelistEntries() {
        MovingWorldMod.INSTANCE.getNetworkConfig().addBlacklistedBlock(DavincisVesselsObjects.blockBuffer);

        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockMarkShip);
        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockFloater);
        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockBalloon);
        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockGauge);
        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockSeat);
        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockEngine);
        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockStickyBuffer);
        MovingWorldMod.INSTANCE.getNetworkConfig().addWhitelistedBlock(DavincisVesselsObjects.blockAnchorPoint);
    }

    public void postLoad() {
        Block[] defaultStickyBlocks = {DavincisVesselsObjects.blockStickyBuffer, Blocks.STONE_BUTTON, Blocks.WOODEN_BUTTON, Blocks.LEVER};
        String[] stickyBlockNames = new String[defaultStickyBlocks.length];
        for (int i = 0; i < defaultStickyBlocks.length; i++) {
            stickyBlockNames[i] = Block.REGISTRY.getNameForObject(defaultStickyBlocks[i]).toString();
        }

        Block[] defaultSeatBlocks = {DavincisVesselsObjects.blockSeat, Blocks.END_PORTAL_FRAME};
        String[] seatBlockNames = new String[defaultSeatBlocks.length];
        for (int i = 0; i < defaultSeatBlocks.length; i++) {
            seatBlockNames[i] = Block.REGISTRY.getNameForObject(defaultSeatBlocks[i]).toString();
        }


        Block[] defaultBalloonBlocks = {DavincisVesselsObjects.blockBalloon};
        String[] balloonBlockNames = new String[defaultBalloonBlocks.length];
        for (int i = 0; i < defaultBalloonBlocks.length; i++) {
            balloonBlockNames[i] = Block.REGISTRY.getNameForObject(defaultBalloonBlocks[i]).toString();
        }

        config.load();

        String[] balloonBlocks = config.get("mobile_chunk", "balloon_blocks", balloonBlockNames, "A list of blocks that are taken into account for ship flight capability").getStringList();
        Collections.addAll(shared.balloonAlternatives, balloonBlocks);

        String[] seatBlocks = (config.get("settings", "seats", seatBlockNames, "Blocks that are considered seats, BlockSeat is hard coded, you can't disable it.").getStringList());
        Collections.addAll(shared.seats, seatBlocks);

        String[] stickyBlocks = config.get("settings", "stickyblocks", stickyBlockNames, "Blocks that behave like a Sticky buffer, they stop assembly when they're reached").getStringList();
        Collections.addAll(shared.stickyObjects, stickyBlocks);

        config.save();
    }

    @SideOnly(Side.CLIENT)
    private void loadKeybindings() {
        kbUp = new KeyBinding("key.davincis.up", getKeyIndex(config, "key_ascent", Keyboard.KEY_X), "Davincis Vessels");
        kbDown = new KeyBinding("key.davincis.down", getKeyIndex(config, "key_descent", Keyboard.KEY_Z), "Davincis Vessels");
        kbBrake = new KeyBinding("key.davincis.brake", getKeyIndex(config, "key_brake", Keyboard.KEY_C), "Davincis Vessels");
        kbAlign = new KeyBinding("key.davincis.align", getKeyIndex(config, "key_align", Keyboard.KEY_EQUALS), "Davincis Vessels");
        kbDisassemble = new KeyBinding("key.davincis.decompile", getKeyIndex(config, "key_decompile", Keyboard.KEY_BACKSLASH), "Davincis Vessels");
        kbShipInv = new KeyBinding("key.davincis.shipinv", getKeyIndex(config, "key_shipinv", Keyboard.KEY_K), "Davincis Vessels");
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.keyBindings = ArrayUtils.addAll(mc.gameSettings.keyBindings, kbUp, kbDown, kbBrake, kbAlign, kbDisassemble, kbShipInv);
    }

    @SideOnly(Side.CLIENT)
    private int getKeyIndex(Configuration config, String name, int defaultkey) {
        return Keyboard.getKeyIndex(config.get("control", name, Keyboard.getKeyName(defaultkey)).getString());
    }

    public boolean isBalloon(Block block) {
        return shared.balloonAlternatives.contains(Block.REGISTRY.getNameForObject(block).toString());
    }

    public boolean isSeat(Block block) {
        return shared.seats.contains(Block.REGISTRY.getNameForObject(block).toString());
    }

    public boolean isSticky(Block block) {
        return shared.stickyObjects.contains(Block.REGISTRY.getNameForObject(block).toString());
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.getModID().equals(DavincisVesselsMod.MOD_ID)) {
            if (config.hasChanged())
                config.save();
            loadAndSave();
        }
    }

    public Configuration getConfig() {
        return config;
    }

    public SharedConfig getShared() {
        return shared;
    }

    public void setShared(SharedConfig shared) {
        this.shared = shared;
    }

    public class SharedConfig {
        //Settings
        public boolean enableAirShips;
        public boolean enableSubmersibles;
        public int shipEntitySyncRate;
        //Mobile Chunk
        public int maxShipChunkBlocks;
        public float flyBalloonRatio;
        public float submersibleFillRatio;
        //Control
        public int shipControlType;
        public float turnSpeed;
        public float speedLimit;
        public float bankingMultiplier;
        public boolean disassembleOnDismount;
        public boolean enginesMandatory;
        public int engineBurnSpeed;
        public Set<String> balloonAlternatives;

        public Set<String> seats;
        public Set<String> stickyObjects;

        public boolean enableShipDownfall;

        public NBTTagCompound serialize() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setBoolean("enableAirShips", enableAirShips);
            tag.setBoolean("enableSubmersibles", enableSubmersibles);
            tag.setInteger("shipEntitySyncRate", shipEntitySyncRate);
            tag.setInteger("maxShipChunkBlocks", maxShipChunkBlocks);
            tag.setFloat("flyBalloonRatio", flyBalloonRatio);
            tag.setFloat("submersibleFillRatio", submersibleFillRatio);
            tag.setInteger("shipControlType", shipControlType);
            tag.setFloat("turnSpeed", turnSpeed);
            tag.setFloat("speedLimit", speedLimit);
            tag.setFloat("bankingMultiplier", bankingMultiplier);
            tag.setBoolean("disassembleOnDismount", disassembleOnDismount);
            tag.setBoolean("enginesMandatory", enginesMandatory);
            tag.setInteger("engineBurnSpeed", engineBurnSpeed);
            tag.setBoolean("enableShipDownfall", enableShipDownfall);

            tag.setString("balloonAlternatives", new Gson().toJson(balloonAlternatives));
            tag.setString("seats", new Gson().toJson(seats));
            tag.setString("stickyObjects", new Gson().toJson(stickyObjects));

            return tag;
        }

        public SharedConfig deserialize(NBTTagCompound tag) {
            SharedConfig sharedConfig = new SharedConfig();
            sharedConfig.enableAirShips = tag.getBoolean("enableAirShips");
            sharedConfig.enableSubmersibles = tag.getBoolean("enableSubmersibles");
            sharedConfig.shipEntitySyncRate = tag.getInteger("shipEntitySyncRate");
            sharedConfig.maxShipChunkBlocks = tag.getInteger("maxShipChunkBlocks");
            sharedConfig.flyBalloonRatio = tag.getFloat("flyBalloonRatio");
            sharedConfig.submersibleFillRatio = tag.getFloat("submersibleFillRatio");
            sharedConfig.shipControlType = tag.getInteger("shipControlType");
            sharedConfig.turnSpeed = tag.getFloat("turnSpeed");
            sharedConfig.speedLimit = tag.getFloat("speedLimit");
            sharedConfig.bankingMultiplier = tag.getFloat("bankingMultiplier");
            sharedConfig.disassembleOnDismount = tag.getBoolean("disassembleOnDismount");
            sharedConfig.enginesMandatory = tag.getBoolean("enginesMandatory");
            sharedConfig.engineBurnSpeed = tag.getInteger("engineBurnSpeed");
            sharedConfig.enableShipDownfall = tag.getBoolean("enableShipDownfall");

            sharedConfig.balloonAlternatives = new Gson().fromJson(tag.getString("balloonAlternatives"), balloonAlternatives.getClass());
            sharedConfig.seats = new Gson().fromJson(tag.getString("seats"), seats.getClass());
            sharedConfig.stickyObjects = new Gson().fromJson(tag.getString("stickyObjects"), stickyObjects.getClass());

            return sharedConfig;
        }
    }
}
