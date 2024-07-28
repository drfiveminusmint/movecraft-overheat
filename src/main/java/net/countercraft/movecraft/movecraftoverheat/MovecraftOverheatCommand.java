package net.countercraft.movecraft.movecraftoverheat;

import net.countercraft.movecraft.craft.Craft;
import net.countercraft.movecraft.craft.CraftManager;
import net.countercraft.movecraft.craft.type.CraftType;
import net.countercraft.movecraft.movecraftoverheat.tracking.CraftHeat;
import net.countercraft.movecraft.util.ChatUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class MovecraftOverheatCommand implements CommandExecutor {

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if (!command.getName().equalsIgnoreCase("movecraftoverheat")) {
            return false;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatUtils.ERROR_PREFIX + " Please supply an argument. Valid arguments include: 'check' 'add'");
            return true;
        }

        if (args[0].equalsIgnoreCase("check")) {
            if (args.length == 1) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(ChatUtils.ERROR_PREFIX + " You must be a player to use the command this way. Try /movecraftoverheat check <player>");
                    return true;
                }
                return checkCommand(sender, (Player) sender);
            } else {
                Player player = Bukkit.getPlayer(args[1]);
                if (player == null) {
                    sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + " Player '" + args[1] + "' not found.");
                    return true;
                }
                return checkCommand(sender, player);
            }
        }
        if (args[0].equalsIgnoreCase("add")) {
            if (args.length < 3) {
                sender.sendMessage(ChatUtils.ERROR_PREFIX + " Insufficient arguments. Usage: /movecraftoverheat add <player> <amount>");
                return true;
            }
            Double amount;
            try {
                amount = Double.valueOf(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(ChatUtils.ERROR_PREFIX + " Could not parse argument '" + args[2] + "' as a double. Make sure you enter a number.");
                return true;
            }

            Player player = Bukkit.getPlayer(args[1]);
            if (player == null) {
                sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + " Player '" + args[1] + "' not found.");
                return true;
            }
            return addCommand(sender,player,amount);
        }
        sender.sendMessage(ChatUtils.ERROR_PREFIX + " Invalid argument: '" + args[0] + "'. Valid arguments include: 'check' 'add'");
        return true;
    }

    private boolean checkCommand (CommandSender sender, Player player) {
        Craft c = CraftManager.getInstance().getCraftByPlayer(player);
        if (c == null) {
            sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + " Player '" + player.getDisplayName() + "' is not piloting a craft.");
            return true;
        }
        CraftHeat craftHeat = MovecraftOverheat.getInstance().getHeatManager().getHeat(c);
        if (craftHeat == null) {
            sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + " This craft does not have a CraftHeat associated with it. Perhaps it does not use the Heat system?");
            return true;
        }
        sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + " Checking craft of type " + c.getType().getStringProperty(CraftType.NAME) + " commanded by " + player.getDisplayName() + ":");
        sender.sendMessage("Current Heat: " + craftHeat.getCraft().getDataTag(CraftHeat.HEAT));
        sender.sendMessage("Capacity: " + craftHeat.getCraft().getDataTag(CraftHeat.HEAT_CAPACITY));
        sender.sendMessage("Dissipation / second: " + craftHeat.getCraft().getDataTag(CraftHeat.DISSIPATION));
        return true;
    }

    private boolean addCommand (CommandSender sender, Player player, double add) {
        Craft c = CraftManager.getInstance().getCraftByPlayer(player);
        if (c == null) {
            sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Player '" + player.getDisplayName() + "' is not piloting a craft.");
            return true;
        }
        CraftHeat craftHeat = MovecraftOverheat.getInstance().getHeatManager().getHeat(c);
        if (craftHeat == null) {
            sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "This craft does not have a CraftHeat associated with it. Perhaps it does not use the Heat system?");
            return true;
        }
        craftHeat.addHeat(add);
        sender.sendMessage(ChatUtils.MOVECRAFT_COMMAND_PREFIX + "Added " + add + " heat to craft commanded by " + player.getDisplayName() + ".");
        return true;
    }
}
