package net.avallion.survival.homes;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class TabComplete implements TabCompleter {

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label,
                                      @NotNull String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;
            Homeowner homeowner = Homeowner.get(player);
            switch (command.getName()) {
                case "home": {
                    List<String> sub = homeowner.sortedHomeNames();
                    if (player.hasPermission("avallionhomes.invite")) {
                        sub.add(0, "invite");
                        sub.add(1, "uninvite");
                    }
                    switch (args.length) {
                        case 0:
                            return (sub.size() == 0) ? null : sub;
                        case 1: {
                            List<String> startsWith = startsWith(sub, args[0]);
                            return (startsWith.size() == 0) ? null : startsWith;
                        }
                        case 2: {
                            Homeowner target = Homeowner.get(args[0]);
                            if (target == null) {
                                if (args[0].equalsIgnoreCase("invite")) {
                                    return null;
                                } else if (args[0].equalsIgnoreCase("uninvite")) {
                                    List<String> startsWith = startsWith(homeowner.sortedInvitedNames(), args[1]);
                                    return (startsWith.size() == 0) ? ((player
                                            .hasPermission("avallionhomes.invite.other")) ? null : Collections.emptyList()) : startsWith;
                                } else {
                                    return Collections.emptyList();
                                }
                            } else {
                                return startsWith(target.sortedCanTeleportHomeNames(player), args[1]);
                            }
                        }
                        case 3:
                            if (args[0].equalsIgnoreCase("invite")) {
                                List<String> startsWith = startsWith(homeowner.sortedHomeNames(), args[2]);
                                return (startsWith.size() == 0) ?
                                        ((player.hasPermission("avallionhomes.invite.other")) ? null :
                                                Collections.emptyList()) : startsWith;
                            } else if (args[0].equalsIgnoreCase("uninvite")) {
                                List<String> startsWith = startsWith(homeowner.sortedInvitedHomeNames(args[1]),
                                        args[2]);
                                if (startsWith.size() == 0) {
                                    if (player.hasPermission("avallionhomes.invite.other")) {
                                        Homeowner target = Homeowner.get(args[1]);
                                        if (target != null) {
                                            return startsWith(target.sortedInvitedNames(), args[2]);
                                        }
                                    }
                                    return Collections.emptyList();
                                }
                                return startsWith;
                            } else {
                                return Collections.emptyList();
                            }
                        case 4:
                            Homeowner target = Homeowner.get(args[1]);
                            if (player.hasPermission("avallionhomes.invite.other") && target != null) {
                                if (args[0].equalsIgnoreCase("invite")) {
                                    return startsWith(target.sortedHomeNames(), args[3]);
                                }
                                if (args[0].equalsIgnoreCase("uninvite")) {
                                    return startsWith(target.sortedInvitedHomeNames(args[2]), args[3]);
                                }
                            }
                            return Collections.emptyList();
                        default:
                            return Collections.emptyList();
                    }
                }
                case "sethome":
                    if (player.hasPermission("avallionhomes.sethome.other") && args.length < 2) {
                        return null;
                    }
                    return Collections.emptyList();
                case "delhome": {
                    List<String> sub = homeowner.sortedHomeNames();
                    switch (args.length) {
                        case 0:
                            return (sub.size() == 0) ?
                                    ((player.hasPermission("avallionhomes.delhome.other")) ? null :
                                            Collections.emptyList()) : sub;
                        case 1:
                            List<String> startsWith = startsWith(sub, args[0]);
                            return (startsWith.size() == 0) ?
                                    ((player.hasPermission("avallionhomes.delhome.other")) ? null :
                                            Collections.emptyList()) : startsWith;
                        case 2:
                            Homeowner target = Homeowner.get(args[0]);
                            if (!player.hasPermission("avallionhomes.delhome.other") || target == null) {
                                return Collections.emptyList();
                            } else {
                                return startsWith(target.sortedHomeNames(), args[1]);
                            }
                        default:
                            return Collections.emptyList();
                    }
                }
                case "homes":
                    if (player.hasPermission("avallionhomes.homes.other") && args.length < 2) {
                        return null;
                    }
                    return Collections.emptyList();
                case "respawnhome":
                    if (player.hasPermission("avallionhomes.respawnhome")) {
                        if (args.length == 0) {
                            return homeowner.sortedHomeNames();
                        } else if (args.length == 1) {
                            return startsWith(homeowner.sortedHomeNames(), args[0]);
                        }
                    }
                    return Collections.emptyList();
                case "homeinvite":
                    if (player.hasPermission("avallionhomes.invite")) {
                        switch (args.length) {
                            case 0:
                            case 1:
                                return null;
                            case 2:
                                List<String> startsWith = startsWith(homeowner.sortedHomeNames(), args[1]);
                                return (startsWith.size() == 0) ?
                                        ((player.hasPermission("avallionhomes.invite.other")) ? null :
                                        Collections.emptyList()) : startsWith;
                            case 3:
                                Homeowner target = Homeowner.get(args[0]);
                                if (!player.hasPermission("avallionhomes.invite.other") || target == null) {
                                    return Collections.emptyList();
                                } else {
                                    return startsWith(target.sortedHomeNames(), args[2]);
                                }
                            default:
                                return Collections.emptyList();
                        }
                    }
                    return Collections.emptyList();
                case "homeuninvite":
                    if (player.hasPermission("avallionhomes.invite")) {
                        List<String> sub = homeowner.sortedInvitedNames();
                        switch (args.length) {
                            case 0:
                                return (sub.size() == 0) ?
                                        ((player.hasPermission("avallionhomes.invite.other")) ? null :
                                                Collections.emptyList()) : sub;
                            case 1: {
                                List<String> startsWith = startsWith(sub, args[0]);
                                return (startsWith.size() == 0) ?
                                        ((player.hasPermission("avallionhomes.invite.other")) ? null :
                                        Collections.emptyList()) : startsWith;
                            }
                            case 2: {
                                List<String> startsWith = startsWith(homeowner.sortedInvitedHomeNames(args[0]),
                                        args[1]);
                                if (startsWith.size() == 0) {
                                    if (player.hasPermission("avallionhomes.invite.other")) {
                                        Homeowner target = Homeowner.get(args[0]);
                                        if (target != null) {
                                            return startsWith(target.sortedInvitedNames(), args[1]);
                                        }
                                    }
                                    return Collections.emptyList();
                                }
                                return startsWith;
                            }
                            case 3:
                                Homeowner target = Homeowner.get(args[0]);
                                if (!player.hasPermission("avallionhomes.invite.other") || target == null) {
                                    return Collections.emptyList();
                                } else {
                                    return startsWith(target.sortedInvitedHomeNames(args[1]), args[2]);
                                }
                            default:
                                return Collections.emptyList();
                        }
                    }
                    return Collections.emptyList();
                case "homeinvites":
                    if (player.hasPermission("avallionhomes.invites")) {
                        switch (args.length) {
                            case 0:
                                return homeowner.sortedInvitedNames();
                            case 1:
                                List<String> sub = homeowner.sortedInvitedNames();
                                sub.addAll(homeowner.sortedHomeNames());
                                return startsWith(sub, args[0]);
                            default:
                                return Collections.emptyList();
                        }
                    }
                    return Collections.emptyList();
                case "renamehome":
                    if (player.hasPermission("avallionhomes.rename")) {
                        switch (args.length) {
                            case 0:
                                return homeowner.sortedHomeNames();
                            case 1:
                                return startsWith(homeowner.sortedHomeNames(), args[0]);
                            default:
                                return Collections.emptyList();
                        }
                    }
                    return Collections.emptyList();
            }
        }
        return null;
    }

    private List<String> startsWith(List<String> list, String str) {
        return list.stream()
                .filter(s -> s.startsWith(str.toLowerCase()))
                .collect(Collectors.toList());
    }
}
