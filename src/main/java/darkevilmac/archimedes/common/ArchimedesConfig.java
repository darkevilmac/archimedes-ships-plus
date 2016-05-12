package darkevilmac.archimedes.common;

import com.google.gson.Gson;
import darkevilmac.archimedes.ArchimedesShipMod;
import darkevilmac.archimedes.common.object.ArchimedesObjects;
import darkevilmac.movingworld.MovingWorld;
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

public class ArchimedesConfig {
    public static final int CONTROL_TYPE_VANILLA = 0, CONTROL_TYPE_ARCHIMEDES = 1;

    @SideOnly(Side.CLIENT)
    public KeyBinding kbUp, kbDown, kbBrake, kbAlign, kbDisassemble, kbShipInv;

    private Configuration config;
    private SharedConfig shared;

    public ArchimedesConfig(Configuration configuration) {
        shared = new SharedConfig();
        config = configuration;
        shared.balloonAlternatives = new HashSet<String>();
        shared.seats = new HashSet<String>();
        shared.stickyObjects = new HashSet<String>();

        MinecraftForge.EVENT_BUS.register(this); // For in game config reloads.
    }

    public void loadAndSave() {
        config.load();

        shared.shipEntitySyncRate = config.get("settings", "sync_rate", 20, "The amount of ticks between a server-client synchronization. Higher numbers reduce network traffic. Lower numbers increase multiplayer experience. 20 ticks = 1 second").getInt();
        shared.enableAirShips = config.get("settings", "enable_air_ships", true, "Enable or disable air ships.").getBoolean(true);
        shared.enableSubmersibles = config.get("settings", "enable_submersibles", true, "Enable or disable the ability to submerse ships.").getBoolean(true);
        shared.bankingMultiplier = (float) config.get("settings", "banking_multiplier", 3d, "A multiplier for how much ships bank while making turns. Set a positive value for passive banking or a negative value for active banking. 0 disables banking.").getDouble(3d);
        shared.enginesMandatory = config.get("settings", "mandatory_engines", false, "Are engines required for a ship to move?").getBoolean();
        shared.enableShipDownfall = config.get("settings", "ship_fall", true, "Do ships slowly fall?").getBoolean();

        shared.shipControlType = config.get("control", "control_type", CONTROL_TYPE_ARCHIMEDES, "Set to 0 to use vanilla boat controls, set to 1 to use the new Archimedes controls.").getInt();
        shared.turnSpeed = (float) config.get("control", "turn_speed", 1D, "A multiplier of the ship's turn speed.").getDouble(1D);
        shared.speedLimit = (float) config.get("control", "speed_limit", 30D, "The maximum velocity a ship can have, in objects per second. This does not affect acceleration.").getDouble(30D);
        shared.speedLimit /= 20F;
        shared.disassembleOnDismount = config.get("control", "decompile_on_dismount", false).getBoolean(false);

        shared.maxShipChunkBlocks = config.get("mobile_chunk", "max_chunk_blocks", 2048, "The maximum amount of objects that a mobile ship chunk may contain.").getInt();
        //maxShipChunkBlocks = Math.min(maxShipChunkBlocks, 3400);
        shared.flyBalloonRatio = (float) config.get("mobile_chunk", "airship_balloon_ratio", 0.4D, "The part of the total amount of objects that should be balloon objects in order to make an airship.").getDouble(0.4D);
        shared.submersibleFillRatio = (float) config.get("mobile_chunk", "submersible_fill_ratio", 0.3D, "The part of the ship that needs to not be water fillable for it to be considered submersible.").getDouble(0.9D);

        if (FMLCommonHandler.instance().getSide().isClient()) {
            loadKeybindings();
        }

        config.save();
    }

    public void addBlacklistWhitelistEntries() {
        MovingWorld.instance.getNetworkConfig().addBlacklistedBlock(ArchimedesObjects.blockBuffer);

        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockMarkShip);
        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockFloater);
        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockBalloon);
        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockGauge);
        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockSeat);
        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockEngine);
        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockStickyBuffer);
        MovingWorld.instance.getNetworkConfig().addWhitelistedBlock(ArchimedesObjects.blockAnchorPoint);
    }

    public void postLoad() {
        Block[] defaultStickyBlocks = {ArchimedesObjects.blockStickyBuffer, Blocks.stone_button, Blocks.wooden_button, Blocks.lever};
        String[] stickyBlockNames = new String[defaultStickyBlocks.length];
        for (int i = 0; i < defaultStickyBlocks.length; i++) {
            stickyBlockNames[i] = Block.blockRegistry.getNameForObject(defaultStickyBlocks[i]).toString();
        }

        Block[] defaultSeatBlocks = {ArchimedesObjects.blockSeat, Blocks.end_portal_frame};
        String[] seatBlockNames = new String[defaultSeatBlocks.length];
        for (int i = 0; i < defaultSeatBlocks.length; i++) {
            seatBlockNames[i] = Block.blockRegistry.getNameForObject(defaultSeatBlocks[i]).toString();
        }


        Block[] defaultBalloonBlocks = {ArchimedesObjects.blockBalloon};
        String[] balloonBlockNames = new String[defaultBalloonBlocks.length];
        for (int i = 0; i < defaultBalloonBlocks.length; i++) {
            balloonBlockNames[i] = Block.blockRegistry.getNameForObject(defaultBalloonBlocks[i]).toString();
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
        kbUp = new KeyBinding("key.archimedes.up", getKeyIndex(config, "key_ascent", Keyboard.KEY_X), "Archimedes");
        kbDown = new KeyBinding("key.archimedes.down", getKeyIndex(config, "key_descent", Keyboard.KEY_Z), "Archimedes");
        kbBrake = new KeyBinding("key.archimedes.brake", getKeyIndex(config, "key_brake", Keyboard.KEY_C), "Archimedes");
        kbAlign = new KeyBinding("key.archimedes.align", getKeyIndex(config, "key_align", Keyboard.KEY_EQUALS), "Archimedes");
        kbDisassemble = new KeyBinding("key.archimedes.decompile", getKeyIndex(config, "key_decompile", Keyboard.KEY_BACKSLASH), "Archimedes");
        kbShipInv = new KeyBinding("key.archimedes.shipinv", getKeyIndex(config, "key_shipinv", Keyboard.KEY_K), "Archimedes");
        Minecraft mc = Minecraft.getMinecraft();
        mc.gameSettings.keyBindings = ArrayUtils.addAll(mc.gameSettings.keyBindings, kbUp, kbDown, kbBrake, kbAlign, kbDisassemble, kbShipInv);
    }

    @SideOnly(Side.CLIENT)
    private int getKeyIndex(Configuration config, String name, int defaultkey) {
        return Keyboard.getKeyIndex(config.get("control", name, Keyboard.getKeyName(defaultkey)).getString());
    }

    public boolean isBalloon(Block block) {
        return shared.balloonAlternatives.contains(Block.blockRegistry.getNameForObject(block).toString());
    }

    public boolean isSeat(Block block) {
        return shared.seats.contains(Block.blockRegistry.getNameForObject(block).toString());
    }

    public boolean isSticky(Block block) {
        return shared.stickyObjects.contains(Block.blockRegistry.getNameForObject(block).toString());
    }

    @SubscribeEvent
    public void onConfigChange(ConfigChangedEvent.OnConfigChangedEvent event) {
        if (event.modID.equals(ArchimedesShipMod.MOD_ID)) {
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
            sharedConfig.enableShipDownfall = tag.getBoolean("enableShipDownfall");

            sharedConfig.balloonAlternatives = new Gson().fromJson(tag.getString("balloonAlternatives"), balloonAlternatives.getClass());
            sharedConfig.seats = new Gson().fromJson(tag.getString("seats"), seats.getClass());
            sharedConfig.stickyObjects = new Gson().fromJson(tag.getString("stickyObjects"), stickyObjects.getClass());

            return sharedConfig;
        }
    }
}
