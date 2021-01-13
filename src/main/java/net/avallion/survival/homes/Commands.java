package net.avallion.survival.homes;

import me.gladgladius.gladlib.Message;
import me.gladgladius.gladlib.PlayerUtils;
import me.gladgladius.gladlib.TextUtils;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class Commands implements CommandExecutor {

    private final AvallionHomes plugin;
    private final Message msg;

    public Commands(AvallionHomes plugin) {
        this.plugin = plugin;
        this.msg = plugin.msg;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                             @NotNull String[] args) {

        switch (command.getName()) {
            case "homes":
                if (noPerm(sender, "avallionhomes.homes")) return false;
                switch (args.length) {
                    case 0:
                        if (!(sender instanceof Player)) return false;
                        Player player = (Player) sender;
                        return sendHomeList(player, Homeowner.get(player));
                    case 1:
                        if (args[0].equalsIgnoreCase("reload") && sender.hasPermission("avallionhomes.admin")) {
                            plugin.load();
                            msg.send(sender, "Reloaded configs.");
                            return true;
                        } else if (!noPerm(sender, "avallionhomes.homes")) {
                            Homeowner homeowner = getName(sender, args[0]);
                            if (homeowner == null) {
                                return false;
                            }
                            return sendHomeList(sender, homeowner);
                        } else {
                            return false;
                        }
                    default:
                        msg.error(sender, !sender.hasPermission("avallionhomes.homes.other") ?
                                "Usage: /homes" : "Usage: /homes [player]");
                        return false;
                }
            case "delhome":
                if (noPerm(sender, "avallionhomes.delhome")) return false;
                switch (args.length) {
                    case 1:
                        if (!(sender instanceof Player)) return false;
                        Player player = (Player) sender;
                        return delHome(player, Homeowner.get(player), args[0]);
                    case 2:
                        if (!sender.hasPermission("avallionhomes.delhome.other")) {
                            msg.error(sender, "Usage: /delhome <home_name>");
                            return false;
                        }
                        Homeowner homeowner = getName(sender, args[0]);
                        if (homeowner == null) {
                            return false;
                        }
                        return delHome(sender, homeowner, args[1]);
                    default:
                        msg.error(sender, !sender.hasPermission("avallionhomes.delhome.other") ?
                                "Usage: /delhome <home_name>" : "Usage: /delhome [player] <home_name>");
                        return false;
                }
            case "homeinvite":
                return cmdInviteCommon(true, sender, label, args);
            case "homeuninvite":
                return cmdInviteCommon(false, sender, label, args);
            case "renamehome":
                if (noPerm(sender, "avallionhomes.rename")) return false;
                switch (args.length) {
                    case 2:
                        if (!(sender instanceof Player)) return false;
                        Player player = (Player) sender;
                        return renameHome(player, Homeowner.get(player), args[0], args[1]);
                    case 3:
                        if (!sender.hasPermission("avallionhomes.rename.other")) {
                            msg.error(sender, "Usage: /renamehome <from> <to>");
                            return false;
                        }
                        Homeowner homeowner = getName(sender, args[0]);
                        if (homeowner == null) {
                            return false;
                        }
                        return renameHome(sender, homeowner, args[1], args[2]);
                    default:
                        msg.error(sender, !sender.hasPermission("avallionhomes.rename.other") ?
                                "Usage: /renamehome <from> <to>" : "Usage: /renamehome [player] <from> <to>");
                        return false;
                }
            case "homeinvites":
                if (noPerm(sender, "avallionhomes.invites")) return false;
                switch (args.length) {
                    case 0: {
                        if (!(sender instanceof Player)) return false;
                        Player player = (Player) sender;
                        return sendInvitedPlayers(player, Homeowner.get(player));
                    }
                    case 1:
                        if (sender instanceof Player) {
                            Player player = (Player) sender;
                            Homeowner homeowner = Homeowner.get(player);
                            if (homeowner.hasHome(args[0])) {
                                return sendHomeInvitedPlayers(player, homeowner, args[0]);
                            } else {
                                return sendPlayerInvitedHomes(player, homeowner, args[0]);
                            }
                        } else {
                            if (!sender.hasPermission("avallionhomes.invites.other")) {
                                msg.error(sender, "Usage: /homeinvites [player|home]");
                                return false;
                            }
                            Homeowner homeowner = Homeowner.get(args[0]);
                            if (homeowner == null) {
                                return false;
                            }
                            return sendInvitedPlayers(sender, homeowner);
                        }
                    case 2:
                        if (!sender.hasPermission("avallionhomes.invites.other")) {
                            msg.error(sender, "Usage: /homeinvites [player|home]");
                            return false;
                        }
                        Homeowner homeowner = Homeowner.get(args[0]);
                        if (homeowner == null) {
                            return false;
                        }
                        if (homeowner.hasHome(args[1])) {
                            return sendHomeInvitedPlayers(sender, homeowner, args[1]);
                        } else {
                            return sendPlayerInvitedHomes(sender, homeowner, args[1]);
                        }
                    default:
                        msg.error(sender, !sender.hasPermission("avallionhomes.invites.other") ?
                                "Usage: /homeinvites [player|home]" : "Usage: /homeinvites [inviter] [invitee|home]");
                        return false;
                }
        }

        if (sender instanceof Player) {
            Player player = (Player) sender;
            switch (command.getName()) {
                case "sethome":
                    if (noPerm(sender, "avallionhomes.sethome")) return false;
                    switch (args.length) {
                        case 0:
                            return setHome(player, Homeowner.get(player), null, player.getLocation());
                        case 1:
                            return setHome(player, Homeowner.get(player), args[0], player.getLocation());
                        case 2:
                            if (!player.hasPermission("avallionhomes.sethome.other")) {
                                msg.error(player, "Usage: /sethome [home_name]");
                                return false;
                            }
                            Homeowner homeowner = getName(sender, args[0]);
                            if (homeowner == null) {
                                return false;
                            }
                            return setHome(player, homeowner, args[1], player.getLocation());
                        default:
                            msg.error(player, !player.hasPermission("avallionhomes.sethome.other") ?
                                    "Usage: /sethome [home_name]" : "Usage: /sethome [player] [home_name]");
                            return false;
                    }
                case "home":
                    if (args.length > 0 && (args[0].equalsIgnoreCase("invite") ||
                            args[0].equalsIgnoreCase("uninvite"))) {
                        return cmdInviteCommon(args[0].equalsIgnoreCase("invite"), sender,
                                label + " " + args[0], Arrays.copyOfRange(args, 1, args.length));
                    }
                    if (noPerm(sender, "avallionhomes.home")) return false;
                    switch (args.length) {
                        case 0:
                            return teleportHome(player, Homeowner.get(player), null);
                        case 1: {
                            Homeowner homeowner = Homeowner.get(player);
                            if (homeowner.hasHome(args[0])) {
                                return teleportHome(player, homeowner, args[0]);
                            } else {
                                Homeowner target;
                                Player targetPlayer = PlayerUtils.getPlayer(args[0]);
                                if (targetPlayer == null) {
                                     target = Homeowner.get(args[0]);
                                } else {
                                    target = Homeowner.get(targetPlayer);
                                }
                                if (target == null) {
                                    msg.error(sender,
                                            "You do not have a home named &6" + args[0] + "&c, and player &6" + args[0] +
                                                    " &cnot found!");
                                    return false;
                                }
                                return teleportHome(player, target, null);
                            }
                        }
                        case 2: {
                            Homeowner target = getName(sender, args[0]);
                            if (target == null) {
                                return false;
                            }
                            return teleportHome(player, target, args[1]);
                        }
                        default:
                            msg.error(player, "Usage: /home [player] [home_name]");
                            return false;
                    }
                case "respawnhome":
                    if (noPerm(sender, "avallionhomes.respawnhome")) return false;
                    switch (args.length) {
                        case 0:
                            msg.send(player, "Your main home is &a" + Homeowner.get(player).getMainHome() + "&r. Type " +
                                    "&e/mainhome <home_name> &rto change your main home.");
                            return true;
                        case 1:
                            Homeowner homeowner = Homeowner.get(player);
                            if (homeowner.hasHome(args[0])) {
                                homeowner.setMainHome(args[0]);
                                msg.send(player, "Made home &a" + args[0] + " &ryour main home.");
                                return true;
                            } else {
                                msg.error(player, "You do not have a home named &6" + args[0] + "&c!");
                                return false;
                            }
                        default:
                            msg.error(player,"Usage: /mainhome [home_name]");
                            return false;
                    }
            }
        }

        return false;
    }

    private boolean inviteCommon(boolean invite, CommandSender sender, Homeowner homeowner, String name,
                                    @Nullable String home) {
        return (invite) ? invite(sender, homeowner, name, home) :
                uninvite(sender, homeowner, name, home);
    }

    private boolean cmdInviteCommon(boolean invite, CommandSender sender, String label, String[] args) {
        if (noPerm(sender, "avallionhomes.invite")) return false;
        switch (args.length) {
            case 1: {
                if (!(sender instanceof Player)) return false;
                Player player = (Player) sender;
                return inviteCommon(invite, sender, Homeowner.get(player), args[0], null);
            }
            case 2: {
                if (!(sender instanceof Player)) {
                    Homeowner homeowner = getName(sender, args[0]);
                    if (homeowner == null) {
                        return false;
                    }
                    return inviteCommon(invite, sender, homeowner, args[1], null);
                } else {
                    Player player = (Player) sender;
                    Homeowner target = Homeowner.get(args[0]);
                    if (target == null || !sender.hasPermission("avallionhomes.invite.other")) {
                        return inviteCommon(invite, sender, Homeowner.get(player), args[0], args[1]);
                    } else {
                        return inviteCommon(invite, sender, target, args[1], null);
                    }
                }
            }
            case 3:
                if (!sender.hasPermission("avallionhomes.invite.other")) {
                    msg.error(sender, "Usage: /" + label + " <player> [home_name]");
                    return false;
                }
                Homeowner homeowner = getName(sender, args[0]);
                if (homeowner == null) {
                    return false;
                }
                return inviteCommon(invite, sender, homeowner, args[1], args[2]);
            default:
                msg.error(sender, (!sender.hasPermission("avallionhomes.invite.other")) ?
                        "Usage: /" + label + " <player> [home_name]" :
                        "Usage: /" + label + " [inviter] <invitee> [home_name]");
                return false;
        }
    }

    @SuppressWarnings("ConstantConditions")
    private boolean teleportHome(Player sender, Homeowner homeowner, @Nullable String name) {
        boolean self = sender.getName().equals(homeowner.getName());
        String homeName = (name == null) ? homeowner.getMainHome() : name;
        Home home = homeowner.getHome(homeName);

        if (self && name == null && home == null) return sendHomeList(sender, homeowner);
        
        if (!homeowner.canTeleportHome(sender, homeName)) {
            msg.error(sender, ((self) ? "You do" : homeowner.getName() + " does") + " not have a home named &6" +
                    homeName + ((self || sender.hasPermission("avallionhomes.home.other")) ?
                    "&c!" : " &cor does not invite you to it!"));
            if (!self && name == null) {
                msg.error(sender, "If you mean to teleport to a home of yours named &6" +
                        homeowner.getName().toLowerCase() + "&c, it doesn't exist either.");
            }
            return false;
        } else {
            Consumer<Player> tpFunc = (player) -> {
                home.teleport(player);
                msg.send(player, "Teleported " +
                        ((homeName.equals(Homeowner.defaultName) && self) ? "home" :
                        "to " + ((self) ? "" : "&a" + homeowner.getName() + "&r's ") + "home &a" + homeName) +
                        "&r. &7(" + TextUtils.coordsBlock(home.getLocation()) + ")");
            };

            if (sender.hasPermission("avallionhomes.home.delay.bypass")) {
                tpFunc.accept(sender);
            } else {
                msg.send(sender, "Teleporting " +
                        ((homeName.equals(Homeowner.defaultName) && self) ? "home" :
                        "to " + ((self) ? "" : "&a" + homeowner.getName() + "&r's ") + "home &a" + homeName) +
                        " &rin &e" + TextUtils.fraction(Homeowner.teleportDelay) + " second(s)&r. Don't move!");
                new DelayedTeleport(plugin, sender, (int) (Homeowner.teleportDelay * 1000), tpFunc);
            }
            return true;
        }
    }

    private boolean setHome(Player sender, Homeowner homeowner, @Nullable String name, Location location) {
        boolean self = sender.getName().equals(homeowner.getName());
        String homeName = (name == null) ? Homeowner.defaultName : name.toLowerCase();

        if (failedSetHomeChecks(sender, homeowner, homeName)) {
            return false;
        }

        Home old = homeowner.setHome(homeName, location);

        if (old == null) {
            msg.send(sender,
                    "Set home &a" +
                            ((self) ? ((homeName.equals(Homeowner.defaultName)) ? "" : homeName + " ") :
                                    homeName + " &rfor &a" + homeowner.getName() + " ") +
                            "&rat your location. Teleport using &b/home" +
                            ((homeName.equals(Homeowner.defaultName)) ? "" : " " + homeName) + "&r. " +
                            "&7(" + TextUtils.coordsBlock(location) + ")");
        } else {
            msg.send(sender,
                    "Moved " + ((self) ? "" : "&a" + homeowner.getName() + "&r's ") + "home" +
                            ((homeName.equals(Homeowner.defaultName)) ? "" : " &a" + homeName) +
                            " &rfrom " + TextUtils.coords("&6", "&e", true, old.x, old.y, old.z) +
                            " &rto your location.");
        }
        return true;
    }

    private boolean delHome(CommandSender sender, Homeowner homeowner, String name) {
        boolean self = sender.getName().equals(homeowner.getName());
        Home old = homeowner.deleteHome(name);

        if (old == null) {
            msg.error(sender,((self) ? "You do" : homeowner.getName() + " does") + " not have a home named &6" +
                    name + "&c!");
            return false;
        } else {
            msg.send(sender, "Deleted " + ((self) ? "" : "&a" + homeowner.getName() + "&r's ") +
                    "home &a" + name + " &rat " + TextUtils.coords("&6", "&e", true, old.x, old.y, old.z) + "&r.");
            return true;
        }
    }

    private boolean sendHomeList(CommandSender sender, Homeowner homeowner) {
        boolean self = sender instanceof Player && sender.getName().equals(homeowner.getName());
        boolean showAll = self || sender.hasPermission("avallionhomes.homes.other") ||
                (sender instanceof Player && homeowner.isInvitedToAll(sender.getName()));

        List<String> homes;
        if (showAll) {
            homes = homeowner.sortedHomeNames();
        } else {
            homes = homeowner.sortedInvitedHomeNames(sender.getName());
        }

        if (homes.size() == 0) {
            if (showAll) {
                msg.error(sender, ((self) ? "You do" : homeowner.getName() + " does") + " not have any homes." +
                        ((self) ? " Type &6/sethome &cto set one at your position." : ""));
            } else {
                msg.error(sender, "You cannot list " + homeowner.getName() + "'s homes; you aren't invited to any of" +
                        " them.");
            }
            return false;
        } else {
            msg.send(sender,((self) ? "H" : homeowner.getName() + "'s h") + "omes" +
                    ((showAll) ? "" : " &7(invited)") + " &7(&3" + homes.size() + "&7)&r: " +
                    TextUtils.comma("&3", "&b", false, homes.toArray(new String[0])));
            return true;
        }
    }

    private boolean invite(CommandSender sender, Homeowner homeowner, String name, @Nullable String home) {
        boolean self = sender.getName().equals(homeowner.getName());
        if (home != null) {
            if (homeowner.getHome(home) == null) {
                msg.error(sender,((self) ? "You do" : homeowner.getName() + " does") + " not have a home named &6" +
                        home + "&c!");
                return false;
            }
        }
        if (!homeowner.canInvite(name)) {
            int maxInvites = homeowner.getMaxInvites();
            msg.error(sender,((self) ? "You" : homeowner.getName()) + " cannot invite more than &6" +
                    maxInvites + " &c" + TextUtils.plural("player", maxInvites) + "!");
            return false;
        }

        String username = name;
        String homeownerName = homeowner.getName();
        Player target = PlayerUtils.getPlayer(name);
        if (target != null) {
            username = target.getName();
        }
        if (homeownerName.equals(username)) {
            msg.error(sender, "You don't need an invitation to go to your own homes.");
            return false;
        }

        if (homeowner.invite(username, home)) {
            msg.send(sender, "You have invited &a" + username + " &rto " +
                    ((home == null) ?
                            "all of " + ((self) ? "your " : "&a" + homeownerName + "&r's ") + "homes." :
                            ((self) ? "" : "&a" + homeownerName + "&r's ") + "home &a" + home + "&r."));
            if (target != null) {
                msg.send(target, "You have been invited by &a" + homeownerName + " &rto " +
                        ((home == null) ?
                                "all of their homes. Type &b/homes " + homeownerName + " &rto list their homes and " +
                                        "&b/home " + homeownerName + " <home_name> &rto visit them!" :
                                "their home &a" + home + "&r. Type &b/home " + homeownerName + " " + home + " &rto " +
                                        "visit their home!"));
            }
            return true;
        } else {
            msg.error(sender, "&6" + username + " &cis already invited to " +
                    ((home == null) ?
                            "all of " + ((self) ? "your " : "&6" + homeownerName + "&c's ") + "homes." :
                            ((self) ? "" : "&6" + homeownerName + "&c's ") + "home &6" + home + "&c."));
            return false;
        }
    }

    private boolean uninvite(CommandSender sender, Homeowner homeowner, String name, @Nullable String home) {
        if (sender instanceof Player && sender.getName().equals(name)) {
            msg.error(sender, "You cannot uninvite yourself from your own homes. To delete your homes, type " +
                    "&6/delhome <home_name>&c.");
            return false;
        }

        boolean self = sender.getName().equals(homeowner.getName());
        String username = name;
        String homeownerName = homeowner.getName();
        Player target = PlayerUtils.getPlayer(name);
        if (target != null) {
            username = target.getName();
        }

        if (homeowner.uninvite(username, home)) {
            msg.send(sender, "You have uninvited &a" + username + " &rfrom " +
                    ((home == null) ?
                            "all of " + ((self) ? "your " : "&a" + homeownerName + "&r's ") + "homes." :
                            ((self) ? "" : "&a" + homeownerName + "&r's ") + "home &a" + home + "&r."));
            return true;
        } else {
            msg.error(sender, "&6" + username + " &cis not invited to " +
                    ((home == null) ?
                            "any of " + ((self) ? "your " : "&6" + homeownerName + "&c's ") + "homes." :
                            ((self) ? "" : "&6" + homeownerName + "&c's ") + "home &6" + home + "&c."));
            return false;
        }
    }

    private boolean renameHome(CommandSender sender, Homeowner homeowner, String from, String to) {
        boolean self = sender.getName().equals(homeowner.getName());

        Home home = homeowner.getHome(from);
        if (home == null) {
            msg.error(sender,((self) ? "You do" : homeowner.getName() + " does") + " not have a home named &6" +
                    from + "&c!");
            return false;
        }
        if (from.equals(to)) {
            msg.error(sender, "Please type a different new name for &6" + from + "&c.");
            return false;
        }
        if (failedSetHomeChecks(sender, homeowner, to)) {
            return false;
        }

        homeowner.renameHome(from, to);
        msg.send(sender, "Renamed home &a" + from + " &fto &a" + to +
                        ((self) ? "" : " &rfor &a" + homeowner.getName()) +
                        "&r. &7(" + TextUtils.coordsBlock(home.getLocation()) + ")");
        return true;
    }

    private boolean sendInvitedPlayers(CommandSender sender, Homeowner homeowner) {
        boolean self = sender.getName().equals(homeowner.getName());

        Set<String> invited = homeowner.getInvites().keySet();
        if (invited.size() == 0) {
            msg.error(sender,((self) ? "You have" : homeowner.getName() + " has") + " not invited anyone!" +
                    ((self) ? " Type &6/homeinvite <player> [home_name] &cto invite someone to your homes." : ""));
            return false;
        } else {
            msg.send(sender, ((self) ? "You have" : homeowner.getName() + " has") + " invited: " +
                    TextUtils.comma("&r", "&e", true, invited.toArray(new String[0])) +
                    "&r." + ((self) ? " Type &b/homeinvites <player> &rto see which homes each player is invited to." :
                    ""));
            return true;
        }
    }

    private boolean sendPlayerInvitedHomes(CommandSender sender, Homeowner homeowner, String player) {
        boolean self = sender.getName().equals(homeowner.getName());

        Set<String> homes = homeowner.getInvites().get(player);

        if (homeowner.isInvitedToAll(player)) {
            msg.send(sender, "&a" + player + " &ris invited to &eALL &rof " + ((self) ? "your " :
                "&f" + homeowner.getName() + "&r's ") + "homes.");
            return true;
        } else if (homes == null || homes.size() == 0) {
            msg.error(sender,((self) ? "You have" : homeowner.getName() + " has") + " not invited &6" + player + "&c!");
            return false;
        } else {
            msg.send(sender, "&a" + player + " &ris invited to: " +
                    TextUtils.comma("&3", "&b", false, homes.toArray(new String[0])));
            return true;
        }
    }

    private boolean sendHomeInvitedPlayers(CommandSender sender, Homeowner homeowner, String home) {
        boolean self = sender.getName().equals(homeowner.getName());

        Set<String> players = homeowner.getInvites().entrySet().stream()
                .filter(e -> e.getValue() == null || e.getValue().contains(home))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (players.size() == 0) {
            msg.error(sender,
                    ((self) ? "You have" : homeowner.getName() + " has") + " not invited anyone to &6" + home + "&c" +
                            "!");
            return false;
        } else {
            msg.send(sender, "Players invited to &a" + home + "&r: " +
                    TextUtils.comma("&r", "&e", true, players.toArray(new String[0])));
            return true;
        }
    }

    private boolean failedSetHomeChecks(CommandSender sender, Homeowner homeowner, String homeName) {
        boolean self = sender.getName().equals(homeowner.getName());
        if (homeName.length() > Homeowner.nameMaxLength) {
            msg.error(sender, "The home name cannot be longer than &6" + Homeowner.nameMaxLength +
                    " &ccharacters!");
            return true;
        }
        if (homeowner.aboveSetHomeLimit(homeName)) {
            int maxHomes = homeowner.getMaxHomes();
            msg.error(sender,((self) ? "You" : homeowner.getName()) + " cannot set more than &6" +
                    maxHomes + " &c" + TextUtils.plural("home", maxHomes) + "!");
            return true;
        }
        if (!homeName.matches(Homeowner.nameCharWhitelistRegex)) {
            msg.error(sender, "The home name may only consist of letters and numbers!");
            return true;
        }
        if (!Homeowner.allowSetHomeExisting && homeowner.hasHome(homeName)) {
            msg.error(sender, "You already have a home named &6" + homeName + "&c! Type &6/delhome " +
                    homeName + " &cto delete that home first.");
            return true;
        }
        return false;
    }
    
    private Homeowner getName(CommandSender sender, String name) {
        Homeowner homeowner = Homeowner.get(name);
        if (homeowner == null) {
            msg.error(sender, "Player &6" + name + " &cnot found!");
        }
        return homeowner;
    }

    private boolean noPerm(CommandSender sender, String perm) {
        if (!sender.hasPermission(perm)) {
            msg.error(sender, "You do not have permission to do that!");
            return true;
        } else {
            return false;
        }
    }

}
