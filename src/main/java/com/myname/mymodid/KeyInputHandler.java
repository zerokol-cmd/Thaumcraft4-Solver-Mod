package com.myname.mymodid;

import java.lang.reflect.Field;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import thaumcraft.api.aspects.Aspect;
import thaumcraft.client.gui.GuiResearchTable;
import thaumcraft.common.lib.network.PacketHandler;
import thaumcraft.common.lib.network.playerdata.PacketAspectPlaceToServer;
import thaumcraft.common.lib.research.ResearchManager;
import thaumcraft.common.tiles.TileResearchTable;

public class KeyInputHandler {

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
                        ResearchSolver solver = new ResearchSolver(
                            tableGui.note,
                            tableGui.mc.thePlayer.getCommandSenderName());
                        try {
                            solver.Solve();
                        } catch (RuntimeException e) {
                            tableGui.mc.thePlayer.addChatMessage((new ChatComponentText(e.toString())));
                        }
                        for (Map.Entry<String, ResearchManager.HexEntry> entry : tableGui.note.hexEntries.entrySet()) {
                            String key = entry.getKey();
                            ResearchManager.HexEntry value = entry.getValue();

                            if (value.aspect != null) {
                                System.out.println("Key: " + key + ", Value: " + value.aspect.getName());
                                var msg = new PacketAspectPlaceToServer(
                                    tableGui.mc.thePlayer, // instead of this.player
                                    (byte) 0,
                                    (byte) 0,
                                    researchTable.xCoord, // instead of this.tileEntity.xCoord
                                    researchTable.yCoord,
                                    researchTable.zCoord,
                                    Aspect.AIR);
                                PacketHandler.INSTANCE.sendToServer(msg);

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
