package com.zerokol.tcSolverFrontend;

import static elan.tweaks.thaumcraft.research.frontend.integration.table.container.ResearchTableContainerFactory.RESEARCH_NOTES_SLOT_INDEX;

import java.util.*;

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
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketAspectCombinationToServer;
import thaumcraft.common.lib.network.playerdata.PacketAspectPlaceToServer;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.lib.research.ResearchNoteData;
import thaumcraft.common.tiles.TileResearchTable;

@Handler
public class KeyInputHandler {

    // Map for fast aspect lookup (lowercase keys)

    private static final Map<String, List<String>> ASPECT_TABLE = new HashMap<>();

    static {
        // Primal aspects (no components)
        ASPECT_TABLE.put("aer", Arrays.asList());
        ASPECT_TABLE.put("aqua", Arrays.asList());
        ASPECT_TABLE.put("ignis", Arrays.asList());
        ASPECT_TABLE.put("terra", Arrays.asList());
        ASPECT_TABLE.put("ordo", Arrays.asList());
        ASPECT_TABLE.put("perditio", Arrays.asList());

        // Compound aspects
        ASPECT_TABLE.put("alienis", Arrays.asList("vacuos", "tenebrae"));
        ASPECT_TABLE.put("arbor", Arrays.asList("aer", "herba"));
        ASPECT_TABLE.put("auram", Arrays.asList("aer", "praecantatio"));
        ASPECT_TABLE.put("bestia", Arrays.asList("motus", "victus"));
        ASPECT_TABLE.put("cognitio", Arrays.asList("ignis", "spiritus"));
        ASPECT_TABLE.put("corpus", Arrays.asList("bestia", "mortuus"));
        ASPECT_TABLE.put("exanimis", Arrays.asList("motus", "mortuus"));
        ASPECT_TABLE.put("fabrico", Arrays.asList("humanus", "instrumentum"));
        ASPECT_TABLE.put("fames", Arrays.asList("vacuos", "victus"));
        ASPECT_TABLE.put("gelum", Arrays.asList("ignis", "perditio"));
        ASPECT_TABLE.put("herba", Arrays.asList("terra", "victus"));
        ASPECT_TABLE.put("humanus", Arrays.asList("bestia", "cognitio"));
        ASPECT_TABLE.put("instrumentum", Arrays.asList("ordo", "humanus"));
        ASPECT_TABLE.put("iter", Arrays.asList("terra", "motus"));
        ASPECT_TABLE.put("limus", Arrays.asList("aqua", "victus"));
        ASPECT_TABLE.put("lucrum", Arrays.asList("fames", "humanus"));
        ASPECT_TABLE.put("lux", Arrays.asList("aer", "ignis"));
        ASPECT_TABLE.put("machina", Arrays.asList("motus", "instrumentum"));
        ASPECT_TABLE.put("messis", Arrays.asList("herba", "humanus"));
        ASPECT_TABLE.put("metallum", Arrays.asList("terra", "vitreus"));
        ASPECT_TABLE.put("meto", Arrays.asList("instrumentum", "messis"));
        ASPECT_TABLE.put("mortuus", Arrays.asList("perditio", "victus"));
        ASPECT_TABLE.put("motus", Arrays.asList("aer", "ordo"));
        ASPECT_TABLE.put("pannus", Arrays.asList("bestia", "instrumentum"));
        ASPECT_TABLE.put("perfodio", Arrays.asList("terra", "humanus"));
        ASPECT_TABLE.put("permutatio", Arrays.asList("ordo", "perditio"));
        ASPECT_TABLE.put("potentia", Arrays.asList("ignis", "ordo"));
        ASPECT_TABLE.put("praecantatio", Arrays.asList("potentia", "vacuos"));
        ASPECT_TABLE.put("sano", Arrays.asList("ordo", "victus"));
        ASPECT_TABLE.put("sensus", Arrays.asList("aer", "spiritus"));
        ASPECT_TABLE.put("spiritus", Arrays.asList("victus", "mortuus"));
        ASPECT_TABLE.put("telum", Arrays.asList("ignis", "instrumentum"));
        ASPECT_TABLE.put("tempestas", Arrays.asList("aer", "aqua"));
        ASPECT_TABLE.put("tenebrae", Arrays.asList("lux", "vacuos"));
        ASPECT_TABLE.put("tutamen", Arrays.asList("terra", "instrumentum"));
        ASPECT_TABLE.put("vacuos", Arrays.asList("aer", "perditio"));
        ASPECT_TABLE.put("venenum", Arrays.asList("aqua", "perditio"));
        ASPECT_TABLE.put("victus", Arrays.asList("aqua", "terra"));
        ASPECT_TABLE.put("vinculum", Arrays.asList("perditio", "motus"));
        ASPECT_TABLE.put("vitium", Arrays.asList("perditio", "praecantatio"));
        ASPECT_TABLE.put("vitreus", Arrays.asList("ordo", "terra"));
        ASPECT_TABLE.put("volatus", Arrays.asList("aer", "motus"));

        // Custom / Extra aspects
        ASPECT_TABLE.put("desidia", Arrays.asList("vinculum", "spiritus"));
        ASPECT_TABLE.put("gula", Arrays.asList("fames", "vacuos"));
        ASPECT_TABLE.put("infernus", Arrays.asList("ignis", "praecantatio"));
        ASPECT_TABLE.put("invidia", Arrays.asList("sensus", "fames"));
        ASPECT_TABLE.put("ira", Arrays.asList("telum", "ignis"));
        ASPECT_TABLE.put("luxuria", Arrays.asList("corpus", "fames"));
        ASPECT_TABLE.put("superbia", Arrays.asList("volatus", "vacuos"));
        ASPECT_TABLE.put("tempus", Arrays.asList("vacuos", "ordo"));
        ASPECT_TABLE.put("electrum", Arrays.asList("potentia", "machina"));
        ASPECT_TABLE.put("magneto", Arrays.asList("metallum", "iter"));
        ASPECT_TABLE.put("nebrisum", Arrays.asList("perfodio", "lucrum"));
        ASPECT_TABLE.put("radio", Arrays.asList("lux", "potentia"));
        ASPECT_TABLE.put("strontio", Arrays.asList("perditio", "cognitio"));
    }

    private static boolean hasAspectInHexEntries(SolverBackendCommunicator.Cell cell,
        Map<String, ResearchManager.HexEntry> hexEntries) {
        if (cell == null || hexEntries == null) return false;
        ResearchManager.HexEntry entry = hexEntries.get(cell.x + ":" + cell.y);
        return entry != null && entry.aspect != null;
    }

    private static Aspect aspectFromStr(String aspectName) {
        if (aspectName == null) return null;

        Aspect aspect = Aspect.getAspect(aspectName);
        if (aspect == null) {
            System.out.println("unknown aspect " + aspectName);
        }
        return aspect;
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

    private static void combineAspect(TileResearchTable table, EntityPlayer player, Aspect first, Aspect second) {
        System.out.println("Combining aspect1 " + first.getName() + " and aspect2" + second.getName());
        PacketHandler.INSTANCE.sendToServer(
            new PacketAspectCombinationToServer(
                player,
                table.xCoord,
                table.yCoord,
                table.zCoord,
                first,
                second,
                table.bonusAspects.getAmount(first) > 0,
                table.bonusAspects.getAmount(second) > 0,
                true));
    }

    private static boolean tryToCreateAspectFromExisting(TileResearchTable table, EntityPlayer player,
        String aspectName) {
        Aspect aspect = Aspect.getAspect(aspectName);
        if (aspect == null) {
            player.addChatMessage(new ChatComponentText("§cUnknown aspect: " + aspectName));
            return false;
        }

        if (Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(player.getCommandSenderName(), aspect) >= 1) {
            return true;
        }

        List<String> components = ASPECT_TABLE.get(aspectName);

        if (components == null || components.isEmpty()) {
            // It's a primal aspect → cannot be crafted
            player.addChatMessage(new ChatComponentText("§ePrimal aspect " + aspectName + " cannot be created."));
            return false;
        }

        String firstName = components.get(0);
        String secondName = components.get(1);

        // Recursively try to create the first component
        boolean hasFirst = tryToCreateAspectFromExisting(table, player, firstName);
        // Recursively try to create the second component
        boolean hasSecond = tryToCreateAspectFromExisting(table, player, secondName);

        if (hasFirst && hasSecond) {
            Aspect first = Aspect.getAspect(firstName);
            Aspect second = Aspect.getAspect(secondName);
            combineAspect(table, player, first, second);
            return true;
        } else {
            player.addChatMessage(new ChatComponentText("§cMissing components for aspect: " + aspectName));
            return false;
        }
    }

    private static void sendAspectPlacement(EntityPlayer player, SolverBackendCommunicator.Cell cell,
        TileResearchTable table) {
        if (player == null || cell == null || table == null) return;
        Aspect aspect = aspectFromStr(cell.aspect);

        if (Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(player.getCommandSenderName(), aspect) < 1) {

            player.addChatMessage(new ChatComponentText("Out of " + cell.aspect));
            tryToCreateAspectFromExisting(table, player, cell.aspect);
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

            System.out.println("Key pressed with throttle!");

            keyCooldown = 15;
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
            int aspectCount = 0;
            for (var cell : solution) {
                if (cell == null) continue;
                if (!"none".equalsIgnoreCase(cell.aspect) && !hasAspectInHexEntries(cell, note.hexEntries)) {
                    aspectCount++;
                    sendAspectPlacement(player, cell, table);
                }
            }
            System.out.println("aspectCount: " + aspectCount);
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
