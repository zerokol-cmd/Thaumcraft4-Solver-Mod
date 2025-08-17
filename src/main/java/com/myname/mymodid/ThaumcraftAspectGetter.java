package com.myname.mymodid;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;

import thaumcraft.api.aspects.Aspect;
import thaumcraft.common.Thaumcraft;

public class ThaumcraftAspectGetter extends CommandBase {

    @Override
    public String getCommandName() {
        return "getAspects";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/getAspects";
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        sender.addChatMessage(new ChatComponentText("hello thaumcraft!"));
        // Check if sender is a player
        if (sender instanceof EntityPlayer) {
            EntityPlayer player = (EntityPlayer) sender;

            var aspectsDiscovered = Thaumcraft.proxy.playerKnowledge
                .getAspectsDiscovered(sender.getCommandSenderName());
            for (Aspect aspect : aspectsDiscovered.getAspects()) {
                var count = Thaumcraft.proxy.playerKnowledge.getAspectPoolFor(sender.getCommandSenderName(), aspect);

                sender.addChatMessage(new ChatComponentText("Found aspect:  " + aspect.getName() + "(" + count + ")"));
            }
        } else {
            sender.addChatMessage(new ChatComponentText("This command can only be run by a player!"));
        }

    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0; // 0 = all players can use
    }
}
