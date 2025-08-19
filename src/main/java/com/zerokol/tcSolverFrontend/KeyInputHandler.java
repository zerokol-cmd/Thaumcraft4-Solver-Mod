package com.zerokol.tcSolverFrontend;

import static elan.tweaks.thaumcraft.research.frontend.integration.table.container.ResearchTableContainerFactory.RESEARCH_NOTES_SLOT_INDEX;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.World;

import org.lwjgl.input.Keyboard;

import com.enderio.core.common.Handlers.Handler;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import elan.tweaks.common.gui.ComposableContainerGui;
import fox.spiteful.forbidden.DarkAspects;
import gregtech.api.enums.TCAspects;
import magicbees.api.MagicBeesAPI;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketAspectPlaceToServer;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.tiles.TileResearchTable;

@Handler
public class KeyInputHandler {

    // Map for fast aspect lookup (lowercase keys)
    private static final Map<String, Aspect> ASPECT_MAP = new HashMap<>();

    static {
        ASPECT_MAP.put("aer", Aspect.AIR);
        ASPECT_MAP.put("air", Aspect.AIR);
        ASPECT_MAP.put("terra", Aspect.EARTH);
        ASPECT_MAP.put("earth", Aspect.EARTH);
        ASPECT_MAP.put("ignis", Aspect.FIRE);
        ASPECT_MAP.put("fire", Aspect.FIRE);
        ASPECT_MAP.put("aqua", Aspect.WATER);
        ASPECT_MAP.put("water", Aspect.WATER);
        ASPECT_MAP.put("ordo", Aspect.ORDER);
        ASPECT_MAP.put("order", Aspect.ORDER);
        ASPECT_MAP.put("perditio", Aspect.ENTROPY);
        ASPECT_MAP.put("entropy", Aspect.ENTROPY);
        ASPECT_MAP.put("vacuos", Aspect.VOID);
        ASPECT_MAP.put("void", Aspect.VOID);
        ASPECT_MAP.put("lux", Aspect.LIGHT);
        ASPECT_MAP.put("light", Aspect.LIGHT);
        ASPECT_MAP.put("tempestas", Aspect.WEATHER);
        ASPECT_MAP.put("weather", Aspect.WEATHER);
        ASPECT_MAP.put("motus", Aspect.MOTION);
        ASPECT_MAP.put("motion", Aspect.MOTION);
        ASPECT_MAP.put("gelum", Aspect.COLD);
        ASPECT_MAP.put("cold", Aspect.COLD);
        ASPECT_MAP.put("vitreus", Aspect.CRYSTAL);
        ASPECT_MAP.put("crystal", Aspect.CRYSTAL);
        ASPECT_MAP.put("victus", Aspect.LIFE);
        ASPECT_MAP.put("life", Aspect.LIFE);
        ASPECT_MAP.put("venenum", Aspect.POISON);
        ASPECT_MAP.put("poison", Aspect.POISON);
        ASPECT_MAP.put("potentia", Aspect.ENERGY);
        ASPECT_MAP.put("energy", Aspect.ENERGY);
        ASPECT_MAP.put("permutatio", Aspect.EXCHANGE);
        ASPECT_MAP.put("exchange", Aspect.EXCHANGE);
        ASPECT_MAP.put("metallum", Aspect.METAL);
        ASPECT_MAP.put("metal", Aspect.METAL);
        ASPECT_MAP.put("mortuus", Aspect.DEATH);
        ASPECT_MAP.put("death", Aspect.DEATH);
        ASPECT_MAP.put("volatus", Aspect.FLIGHT);
        ASPECT_MAP.put("flight", Aspect.FLIGHT);
        ASPECT_MAP.put("tenebrae", Aspect.DARKNESS);
        ASPECT_MAP.put("darkness", Aspect.DARKNESS);
        ASPECT_MAP.put("spiritus", Aspect.SOUL);
        ASPECT_MAP.put("soul", Aspect.SOUL);
        ASPECT_MAP.put("sano", Aspect.HEAL);
        ASPECT_MAP.put("heal", Aspect.HEAL);
        ASPECT_MAP.put("iter", Aspect.TRAVEL);
        ASPECT_MAP.put("travel", Aspect.TRAVEL);
        ASPECT_MAP.put("alienis", Aspect.ELDRITCH);
        ASPECT_MAP.put("eldritch", Aspect.ELDRITCH);
        ASPECT_MAP.put("praecantatio", Aspect.MAGIC);
        ASPECT_MAP.put("magic", Aspect.MAGIC);
        ASPECT_MAP.put("auram", Aspect.AURA);
        ASPECT_MAP.put("aura", Aspect.AURA);
        ASPECT_MAP.put("vitium", Aspect.TAINT);
        ASPECT_MAP.put("taint", Aspect.TAINT);
        ASPECT_MAP.put("limus", Aspect.SLIME);
        ASPECT_MAP.put("slime", Aspect.SLIME);
        ASPECT_MAP.put("herba", Aspect.PLANT);
        ASPECT_MAP.put("plant", Aspect.PLANT);
        ASPECT_MAP.put("arbor", Aspect.TREE);
        ASPECT_MAP.put("tree", Aspect.TREE);
        ASPECT_MAP.put("bestia", Aspect.BEAST);
        ASPECT_MAP.put("beast", Aspect.BEAST);
        ASPECT_MAP.put("corpus", Aspect.FLESH);
        ASPECT_MAP.put("flesh", Aspect.FLESH);
        ASPECT_MAP.put("exanimis", Aspect.UNDEAD);
        ASPECT_MAP.put("undead", Aspect.UNDEAD);
        ASPECT_MAP.put("cognitio", Aspect.MIND);
        ASPECT_MAP.put("mind", Aspect.MIND);
        ASPECT_MAP.put("sensus", Aspect.SENSES);
        ASPECT_MAP.put("senses", Aspect.SENSES);
        ASPECT_MAP.put("humanus", Aspect.MAN);
        ASPECT_MAP.put("man", Aspect.MAN);
        ASPECT_MAP.put("messis", Aspect.CROP);
        ASPECT_MAP.put("crop", Aspect.CROP);
        ASPECT_MAP.put("perfodio", Aspect.MINE);
        ASPECT_MAP.put("mine", Aspect.MINE);
        ASPECT_MAP.put("instrumentum", Aspect.TOOL);
        ASPECT_MAP.put("tool", Aspect.TOOL);
        ASPECT_MAP.put("meto", Aspect.HARVEST);
        ASPECT_MAP.put("harvest", Aspect.HARVEST);
        ASPECT_MAP.put("telum", Aspect.WEAPON);
        ASPECT_MAP.put("weapon", Aspect.WEAPON);
        ASPECT_MAP.put("tutamen", Aspect.ARMOR);
        ASPECT_MAP.put("armor", Aspect.ARMOR);
        ASPECT_MAP.put("fames", Aspect.HUNGER);
        ASPECT_MAP.put("hunger", Aspect.HUNGER);
        ASPECT_MAP.put("lucrum", Aspect.GREED);
        ASPECT_MAP.put("greed", Aspect.GREED);
        ASPECT_MAP.put("fabrico", Aspect.CRAFT);
        ASPECT_MAP.put("craft", Aspect.CRAFT);
        ASPECT_MAP.put("pannus", Aspect.CLOTH);
        ASPECT_MAP.put("cloth", Aspect.CLOTH);
        ASPECT_MAP.put("machina", Aspect.MECHANISM);
        ASPECT_MAP.put("mechanism", Aspect.MECHANISM);
        ASPECT_MAP.put("vinculum", Aspect.TRAP);
        ASPECT_MAP.put("trap", Aspect.TRAP);

        // TODO: Create checks, whether mod, which provides new aspect, is loaded.
        // Right now it is hardcoded to GTNH-like environment.

        // DarkAspects
        ASPECT_MAP.put("infernus", DarkAspects.NETHER);
        ASPECT_MAP.put("nether", DarkAspects.NETHER);
        ASPECT_MAP.put("ira", DarkAspects.WRATH);
        ASPECT_MAP.put("wrath", DarkAspects.WRATH);
        ASPECT_MAP.put("invidia", DarkAspects.ENVY);
        ASPECT_MAP.put("envy", DarkAspects.ENVY);
        ASPECT_MAP.put("gula", DarkAspects.GLUTTONY);
        ASPECT_MAP.put("gluttony", DarkAspects.GLUTTONY);
        ASPECT_MAP.put("superbia", DarkAspects.PRIDE);
        ASPECT_MAP.put("pride", DarkAspects.PRIDE);
        ASPECT_MAP.put("luxuria", DarkAspects.LUST);
        ASPECT_MAP.put("lust", DarkAspects.LUST);
        ASPECT_MAP.put("desidia", DarkAspects.SLOTH);
        ASPECT_MAP.put("sloth", DarkAspects.SLOTH);

        // MagicBees tempus aspect (if present)
        ASPECT_MAP.put("tempus", (Aspect) MagicBeesAPI.thaumcraftAspectTempus);

        // GT aspects
        ASPECT_MAP.put("electrum", (Aspect) TCAspects.ELECTRUM.mAspect); // electricity
        ASPECT_MAP.put("magneto", (Aspect) TCAspects.MAGNETO.mAspect); // magnetism
        ASPECT_MAP.put("nebrisum", (Aspect) TCAspects.NEBRISUM.mAspect); // cheatiness
        ASPECT_MAP.put("radio", (Aspect) TCAspects.RADIO.mAspect); // radioactivity
        ASPECT_MAP.put("strontio", (Aspect) TCAspects.STRONTIO.mAspect);

    }

    private static boolean hasAspectInHexEntries(SolverBackendCommunicator.Cell cell,
        Map<String, ResearchManager.HexEntry> hexEntries) {
        if (cell == null || hexEntries == null) return false;
        ResearchManager.HexEntry entry = hexEntries.get(cell.x + ":" + cell.y);
        return entry != null && entry.aspect != null;
    }

    private static Aspect aspectFromStr(String aspectName) {
        if (aspectName == null) return null;
        String key = aspectName.trim()
            .toLowerCase(Locale.ROOT);
        Aspect a = ASPECT_MAP.get(key);
        if (a == null) {
            System.out.println("unknown aspect " + aspectName);
        }
        return a;
    }

    /**
     * Returns the TileResearchTable the player is currently looking at, or null.
     */
    private static TileResearchTable getLookedAtResearchTable() {
        Minecraft mc = Minecraft.getMinecraft();
        MovingObjectPosition mop = mc.objectMouseOver;
        if (mop == null || mop.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) return null;
        World world = mc.theWorld;
        TileEntity tile = world.getTileEntity(mop.blockX, mop.blockY, mop.blockZ);

        return (tile instanceof TileResearchTable) ? (TileResearchTable) tile : null;
    }

    private static ResearchNoteData getResearchNoteFromTable(TileResearchTable table) {
        if (table == null) return null;
        return ResearchManager.getData(table.getStackInSlot(RESEARCH_NOTES_SLOT_INDEX));
    }

    private static void sendAspectPlacement(EntityPlayer player, SolverBackendCommunicator.Cell cell,
        TileResearchTable table) {
        if (player == null || cell == null || table == null) return;
        Aspect aspect = aspectFromStr(cell.aspect);

        if (Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(player.getCommandSenderName(), aspect) < 1) {
            player.addChatMessage(new ChatComponentText("Out of " + cell.aspect));
            return;
        }

        // TODO: Sometimes sending this packet crashes server. 🥰
        PacketAspectPlaceToServer msg = new PacketAspectPlaceToServer(
            player,
            (byte) cell.x,
            (byte) cell.y,
            table.xCoord,
            table.yCoord,
            table.zCoord,
            aspect);
        PacketHandler.INSTANCE.sendToServer(msg);
    }

    int keyCooldown = 0;

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        // early returns reduce nesting
        if (event.phase != TickEvent.Phase.START) return;

        if (keyCooldown > 0) {
            keyCooldown--; // reduce cooldown
            return;
        }

        if (Keyboard.isKeyDown(TCSolver.solveKeybind.getKeyCode())) {
            // do your action here
            System.out.println("Key pressed with throttle!");

            // set cooldown to 5 ticks
            keyCooldown = 5;
        }
        if (!Keyboard.isKeyDown(TCSolver.solveKeybind.getKeyCode())) return;

        final Minecraft mc = Minecraft.getMinecraft();
        final Object screen = mc.currentScreen;
        final EntityPlayer player = mc.thePlayer;

        if (screen == null) {
            System.out.println("No GUI open");
            return;
        }

        if (!(screen instanceof GuiResearchTable || screen instanceof ComposableContainerGui)) {
            System.out.println("Not a research GUI: " + screen.getClass());
            return;
        }

        final TileResearchTable table = getLookedAtResearchTable();
        if (table == null) {
            System.out.println("Not looking at a Research Table");
            return;
        }

        final ResearchNoteData note = getResearchNoteFromTable(table);
        if (note == null) {
            System.out.println("No research note in table");
            return;
        }

        // Solve and place aspects
        try {
            SolverBackendCommunicator solver = new SolverBackendCommunicator();
            var solution = solver.RequestSolution(note.hexEntries, Thaumcraft.proxy.playerKnowledge.aspectsDiscovered);
            SolverBackendCommunicator.Cell.recenter(solution);

            for (var cell : solution) {
                if (cell == null) continue;
                if (!"none".equalsIgnoreCase(cell.aspect) && !hasAspectInHexEntries(cell, note.hexEntries)) {
                    sendAspectPlacement(player, cell, table);
                }
            }
        } catch (Exception e) {
            if (player != null) {
                player.addChatMessage(new ChatComponentText("Solver error: " + e.toString()));
            } else {
                System.out.println("Solver error (no player): " + e.toString());
            }
        }

        for (Map.Entry<String, ResearchManager.HexEntry> entry : note.hexEntries.entrySet()) {
            ResearchManager.HexEntry value = entry.getValue();
            if (value != null && value.aspect != null) {
                System.out.println("Key: " + entry.getKey() + ", Value: " + value.aspect.getName());
            }
        }
    }
}
