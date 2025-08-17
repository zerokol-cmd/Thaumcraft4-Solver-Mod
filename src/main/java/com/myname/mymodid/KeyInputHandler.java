package com.myname.mymodid;

import java.lang.reflect.Field;
import java.util.*;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import fox.spiteful.forbidden.DarkAspects;
import gregtech.api.enums.TCAspects;
import magicbees.api.MagicBeesAPI;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.Thaumcraft;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketAspectPlaceToServer;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TileResearchTable;

public class KeyInputHandler {

    private static boolean isInHexArray(SolverBackendCommunicator.Cell cell,
        HashMap<String, ResearchManager.HexEntry> hexEntries) {
        String temp = cell.x + ":" + cell.y;
        if (hexEntries.containsKey(temp)) {
            return hexEntries.get(temp).aspect != null;
        }
        return false;
    }

    private static Aspect aspectFromStr(String aspectName) {
        if (aspectName == null || aspectName.isEmpty()) {
            return null;
        }

        // Convert to lowercase and remove any whitespace for case-insensitive matching
        String lowerName = aspectName.toLowerCase()
            .trim();

        switch (lowerName) {
            case "aer":
            case "air":
                return Aspect.AIR;
            case "terra":
            case "earth":
                return Aspect.EARTH;
            case "ignis":
            case "fire":
                return Aspect.FIRE;
            case "aqua":
            case "water":
                return Aspect.WATER;
            case "ordo":
            case "order":
                return Aspect.ORDER;
            case "perditio":
            case "entropy":
                return Aspect.ENTROPY;
            case "vacuos":
            case "void":
                return Aspect.VOID;
            case "lux":
            case "light":
                return Aspect.LIGHT;
            case "tempestas":
            case "weather":
                return Aspect.WEATHER;
            case "motus":
            case "motion":
                return Aspect.MOTION;
            case "gelum":
            case "cold":
                return Aspect.COLD;
            case "vitreus":
            case "crystal":
                return Aspect.CRYSTAL;
            case "victus":
            case "life":
                return Aspect.LIFE;
            case "venenum":
            case "poison":
                return Aspect.POISON;
            case "potentia":
            case "energy":
                return Aspect.ENERGY;
            case "permutatio":
            case "exchange":
                return Aspect.EXCHANGE;
            case "metallum":
            case "metal":
                return Aspect.METAL;
            case "mortuus":
            case "death":
                return Aspect.DEATH;
            case "volatus":
            case "flight":
                return Aspect.FLIGHT;
            case "tenebrae":
            case "darkness":
                return Aspect.DARKNESS;
            case "spiritus":
            case "soul":
                return Aspect.SOUL;
            case "sano":
            case "heal":
                return Aspect.HEAL;
            case "iter":
            case "travel":
                return Aspect.TRAVEL;
            case "alienis":
            case "eldritch":
                return Aspect.ELDRITCH;
            case "praecantatio":
            case "magic":
                return Aspect.MAGIC;
            case "auram":
            case "aura":
                return Aspect.AURA;
            case "vitium":
            case "taint":
                return Aspect.TAINT;
            case "limus":
            case "slime":
                return Aspect.SLIME;
            case "herba":
            case "plant":
                return Aspect.PLANT;
            case "arbor":
            case "tree":
                return Aspect.TREE;
            case "bestia":
            case "beast":
                return Aspect.BEAST;
            case "corpus":
            case "flesh":
                return Aspect.FLESH;
            case "exanimis":
            case "undead":
                return Aspect.UNDEAD;
            case "cognitio":
            case "mind":
                return Aspect.MIND;
            case "sensus":
            case "senses":
                return Aspect.SENSES;
            case "humanus":
            case "man":
                return Aspect.MAN;
            case "messis":
            case "crop":
                return Aspect.CROP;
            case "perfodio":
            case "mine":
                return Aspect.MINE;
            case "instrumentum":
            case "tool":
                return Aspect.TOOL;
            case "meto":
            case "harvest":
                return Aspect.HARVEST;
            case "telum":
            case "weapon":
                return Aspect.WEAPON;
            case "tutamen":
            case "armor":
                return Aspect.ARMOR;
            case "fames":
            case "hunger":
                return Aspect.HUNGER;
            case "lucrum":
            case "greed":
                return Aspect.GREED;
            case "fabrico":
            case "craft":
                return Aspect.CRAFT;
            case "pannus":
            case "cloth":
                return Aspect.CLOTH;
            case "machina":
            case "mechanism":
                return Aspect.MECHANISM;
            case "vinculum":
            case "trap":
                return Aspect.TRAP;
            case "infernus":
            case "nether":
                return DarkAspects.NETHER;
            case "ira":
            case "wrath":
                return DarkAspects.WRATH;
            case "invidia":
            case "envy":
                return DarkAspects.ENVY;
            case "gula":
            case "gluttony":
                return DarkAspects.GLUTTONY;
            case "superbia":
            case "pride":
                return DarkAspects.PRIDE;
            case "luxuria":
            case "lust":
                return DarkAspects.LUST;
            case "desidia":
            case "sloth":
                return DarkAspects.SLOTH;
            case "tempus":
                return (Aspect) MagicBeesAPI.thaumcraftAspectTempus;
            case "electrum":
            case "electricity":
                return (Aspect) TCAspects.ELECTRUM.mAspect;
            case "magneto":
            case "magnetism":
                return (Aspect) TCAspects.MAGNETO.mAspect;
            case "nebrisum":
            case "cheatiness":
                return (Aspect) TCAspects.NEBRISUM.mAspect;
            case "radio":
            case "radioactivity":
                return (Aspect) TCAspects.RADIO.mAspect;
            case "strontio":
            case "stupidity":
                return (Aspect) TCAspects.STRONTIO.mAspect;
            default:
                System.out.println("unknown aspect " + aspectName);
                return null; // Return null if no match found
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (Keyboard.isKeyDown(MyMod.solveKeybind.getKeyCode())) {
            var mc = Minecraft.getMinecraft();
            var screen = mc.currentScreen;
            if (screen != null) {
                if (screen instanceof GuiResearchTable) {

                    System.out.println("Это Ресёрч табле");
                    var tableGui = (GuiResearchTable) screen;
                    TileResearchTable researchTable = null;
                    EntityPlayer player = null;
                    try {
                        Field field = tableGui.getClass()
                            .getDeclaredField("tileEntity");
                        field.setAccessible(true);
                        researchTable = (TileResearchTable) field.get(tableGui);
                        field.setAccessible(false);

                        // Field field1 = tableGui.getClass()
                        // .getDeclaredField("player");
                        // field1.setAccessible(true);
                        // player = (EntityPlayer) field.get(tableGui);
                        // field1.setAccessible(false);
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        // throw new RuntimeException(e);
                        System.out.println("там выше чёто обосралось ");
                    }

                    if (tableGui.note != null) {
                        SolverBackendCommunicator solver = new SolverBackendCommunicator();
                        try {
                            var solution = solver.RequestSolution(
                                tableGui.note.hexEntries,
                                Thaumcraft.proxy.playerKnowledge.aspectsDiscovered);
                            SolverBackendCommunicator.Cell.recenter(solution);
                            for (var cell : solution) {
                                if (!Objects.equals(cell.aspect, "none")
                                    && !isInHexArray(cell, tableGui.note.hexEntries)) {

                                    var msg = new PacketAspectPlaceToServer(
                                        tableGui.mc.thePlayer, // instead of this.player
                                        (byte) ((byte) cell.x),
                                        (byte) ((byte) cell.y),
                                        researchTable.xCoord, // instead of this.tileEntity.xCoord
                                        researchTable.yCoord,
                                        researchTable.zCoord,
                                        aspectFromStr(cell.aspect));
                                    PacketHandler.INSTANCE.sendToServer(msg);

                                }
                            }
                        } catch (Exception e) {
                            tableGui.mc.thePlayer.addChatMessage(new ChatComponentText("чёто сдохло" + e.toString()));
                        }
                        for (Map.Entry<String, ResearchManager.HexEntry> entry : tableGui.note.hexEntries.entrySet()) {
                            String key = entry.getKey();
                            ResearchManager.HexEntry value = entry.getValue();

                            if (value.aspect != null) {
                                System.out.println("Key: " + key + ", Value: " + value.aspect.getName());

                            }

                        }

                    }

                }

            } else {
                System.out.println("Это не гуиха ёпты ");
            }
        }
    }
}
