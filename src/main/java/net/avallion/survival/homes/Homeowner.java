package net.avallion.survival.homes;

import lombok.Getter;
import lombok.Setter;
import me.gladgladius.gladlib.PlayerUtils;
import me.gladgladius.gladlib.SQLData;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class Homeowner {

    private static SQLAdapter sql;
    static boolean respawnAtHome, allowSetHomeExisting;
    static int nameMaxLength;
    static float teleportDelay;
    static String defaultName = "home", nameCharWhitelistRegex;
    static final List<Homeowner> cachedHomeowners = new ArrayList<>();
    static List<String> respawnWorlds;
    @Getter private final int playerId;
    @Getter private final UUID uuid;
    @Getter private final String name;
    @Nullable private String respawnHome;
    @Getter @Setter private Set<Home> homes = new HashSet<>();
    @Getter @Setter private Map<String, @Nullable Set<String>> invites = new HashMap<>();

    Homeowner(@NotNull UUID uuid, @NotNull String name, int playerId) {
        this.uuid = uuid;
        this.name = name;
        this.playerId = playerId;
    }

    public Homeowner(@NotNull UUID uuid, @NotNull String name) {
        this.uuid = uuid;
        this.name = name;
        playerId = initSQL();
    }

    public Homeowner(@NotNull Player player) {
        this(player.getUniqueId(), player.getName());
    }

    static void load(@NotNull AvallionHomes plugin, @NotNull ConfigurationSection config) {
        defaultName = config.getString("default-name");
        nameMaxLength = config.getInt("name-max-length");
        nameCharWhitelistRegex = "^(?:" + config.getString("name-character-whitelist") + ")+$";
        teleportDelay = (float) config.getDouble("teleport-delay");
        respawnAtHome = config.getBoolean("respawn-at-home");
        allowSetHomeExisting = config.getBoolean("allow-sethome-existing");
        respawnWorlds = config.getStringList("respawn-worlds");

        sql = new SQLAdapter(
                new SQLData(config.getString("mysql.host"),
                        config.getString("mysql.database"),
                        config.getString("mysql.username"),
                        config.getString("mysql.password"),
                        config.getInt("mysql.port")),
                        config.getBoolean("mysql.use-ssl")
        );
        YAMLAdapter yaml = new YAMLAdapter(plugin, plugin.getDataFolder() + "/playerdata");

        long yamlStart = System.currentTimeMillis();
        List<Homeowner> yamlHomeowners = yaml.getHomeowners(
                sql.getHomeowners().stream().map(h -> h.getUuid().toString() + ".yml").collect(Collectors.toList())
        );
        if (yamlHomeowners.size() > 0) {
            plugin.msg.log("Finished reading YAML, starting MySQL migration now...");
            yamlHomeowners.forEach(homeowner -> sql.initHomeowner(homeowner));
            plugin.msg.log("Finished migrating Home YAML files to MySQL. Took " + (System.currentTimeMillis() - yamlStart) + "ms.");
        }
    }

    @Nullable
    private static Homeowner getExisting(@NotNull UUID uuid) {
        Homeowner cached = cachedHomeowners.stream().filter(h -> h.getUuid().equals(uuid)).findFirst().orElse(null);
        if (cached != null) {
            return cached;
        }
        if (sql != null) {
            Homeowner fromSql = sql.getHomeowner(uuid);
            if (Bukkit.getPlayer(uuid) != null) {
                addCache(fromSql);
            }
            return fromSql;
        }
        return null;
    }

    @Nullable
    public static Homeowner get(@NotNull UUID uuid, @NotNull String name) {
        Homeowner existing = getExisting(uuid);
        if (existing != null) return existing;
        if (Bukkit.getPlayer(uuid) != null) {
            Homeowner newHomeowner = new Homeowner(uuid, name);
            addCache(newHomeowner);
            return newHomeowner;
        }
        return null;
    }

    @NotNull
    public static Homeowner get(@NotNull Player player) {
        Homeowner existing = getExisting(player.getUniqueId());
        if (existing != null) return existing;
        Homeowner newHomeowner = new Homeowner(player.getUniqueId(), player.getName());
        addCache(newHomeowner);
        return newHomeowner;
    }

    @Nullable
    public static Homeowner get(@NotNull String name) {
        Homeowner cached = cachedHomeowners.stream().filter(h -> h.getName().equals(name)).findFirst().orElse(null);
        if (cached != null) {
            return cached;
        }
        Player player = PlayerUtils.getPlayer(name);
        if (sql != null) {
            Homeowner fromSql = sql.getHomeowner(name);
            if (player != null) {
                addCache(fromSql);
            }
            if (fromSql != null) {
                return fromSql;
            }
        }
        if (player != null) {
            Homeowner newHomeowner = new Homeowner(player.getUniqueId(), player.getName());
            addCache(newHomeowner);
            return newHomeowner;
        }
        return null;
    }

    public static boolean isHomeowner(@NotNull Player player) {
        return getExisting(player.getUniqueId()) != null;
    }

    private static void addCache(@Nullable Homeowner homeowner) {
        if (homeowner != null) {
            cachedHomeowners.add(homeowner);
        }
    }

    static void removeCache(@NotNull UUID uuid) {
        cachedHomeowners.removeIf(h -> h.uuid.equals(uuid));
    }

    static void clearCache() {
        cachedHomeowners.clear();
    }

    private int initSQL() {
        if (sql != null) {
            try {
                return sql.savePlayer(this);
            } catch (SQLException e) {
                e.printStackTrace();
                return -1;
            }
        } else {
            return -1;
        }
    }

    public boolean isPlayer(Player player) {
        return player != null && uuid.equals(player.getUniqueId()) && name.equals(player.getName());
    }

    public int getMaxHomes() {
        User user = PlayerUtils.getUser(uuid);
        if (user == null) return 1;
        if (PlayerUtils.hasPermission(user, "avallionhomes.sethome.unlimited")) {
            return Integer.MAX_VALUE;
        }
        return PlayerUtils.getIntValue(user, "avallionhomes.sethome.max", 1);
    }

    public int getHomeQty() {
        return homes.size();
    }

    @Nullable
    public Home getHome(@NotNull String name) {
        return homes.stream().filter(h -> h.name.equals(name)).findFirst().orElse(null);
    }

    public boolean hasHome(@NotNull String name) {
        return getHome(name) != null;
    }

    public boolean aboveSetHomeLimit(@NotNull String name) {
        return !hasHome(name) && homes.size() >= getMaxHomes();
    }

    /**
     * Adds a new home with name <tt>name</tt>. If a home with such name already exists, moves it.
     * If home name is above max length or home limit is reached, fails silently.
     * @param name home name, if <tt>null</tt> defaults to <tt>defaultName</tt>
     * @param location home location
     * @return the moved home, <tt>null</tt> if home with name <tt>name</tt> didn't exist
     */
    public Home setHome(@Nullable String name, @NotNull Location location) {
        String homeName = (name == null) ? defaultName : name;
        if (
                homeName.length() > nameMaxLength ||
                aboveSetHomeLimit(homeName) ||
                !homeName.matches(nameCharWhitelistRegex) ||
                !Homeowner.allowSetHomeExisting && hasHome(homeName)
        ) return null;

        Home home = new Home(homeName, location);
        Home oldHome = deleteHome(homeName);
        if (sql != null) sql.addHome(playerId, home);
        if (respawnHome == null || !hasHome(respawnHome)) setRespawnHome(homeName);

        homes.add(home);
        return oldHome;
    }

    public Home setHome(@NotNull Location location) {
        return setHome(null, location);
    }

    /**
     * Optional operation.
     * @param name name of the home to be removed, if <tt>null</tt> defaults to <tt>defaultName</tt>
     * @return the deleted home, <tt>null</tt> if home with name <tt>name</tt> didn't exist
     */
    public Home deleteHome(@Nullable String name) {
        String homeName = (name == null) ? defaultName : name;
        if (sql != null) sql.deleteHome(playerId, homeName);
        Home home = getHome(homeName);
        homes.remove(home);
        return home;
    }

    /**
     * Deletes home with <tt>defaultName</tt>. Optional operation.
     * @return the deleted home, <tt>null</tt> if home with name <tt>name</tt> didn't exist
     */
    public Home deleteHome() {
        return deleteHome(null);
    }

    public boolean deleteAllHomes() {
        if (sql != null) sql.deleteAllHomes(playerId);
        boolean hadHomes = homes.size() > 0;
        homes.clear();
        return hadHomes;
    }

    public void setRespawnHome(@Nullable String home, boolean updateSQL) {
        if (sql != null && updateSQL) sql.setRespawnHome(playerId, home);
        respawnHome = home;
    }

    public void setRespawnHome(@Nullable String home) {
        setRespawnHome(home, true);
    }

    public String getRespawnHome() {
        if (homes.size() == 0) return null;
        if (respawnHome == null || !hasHome(respawnHome)) {
            if (hasHome(Homeowner.defaultName)) {
                setRespawnHome(Homeowner.defaultName);
            } else {
                Home home = getHomes().stream().findFirst().orElse(null);
                if (home == null) return null;
                setRespawnHome(home.name);
            }
        }
        return respawnHome;
    }

    public boolean canTeleportHome(@NotNull Player player, @NotNull String homeName) {
        return hasHome(homeName) && (
                        name.equals(player.getName()) ||
                        player.hasPermission("avallionhomes.home.other") ||
                        isInvited(player.getName(), homeName)
                );
    }

    public int getMaxInvites() {
        User user = PlayerUtils.getUser(uuid);
        if (user == null) return 1;
        if (PlayerUtils.hasPermission(user, "avallionhomes.invite.unlimited")) {
            return Integer.MAX_VALUE;
        }
        return PlayerUtils.getIntValue(user, "avallionhomes.invite.max", 1);
    }

    public boolean canInvite(@NotNull String username) {
        return isInvited(username) || invites.size() < getMaxInvites();
    }

    public boolean isInvited(@NotNull String username) {
        return invites.containsKey(username);
    }

    public boolean isInvited(@NotNull String username, @NotNull String home) {
        if (!isInvited(username)) return false;
        Set<String> homeInvites = invites.get(username);
        return homeInvites == null || homeInvites.contains(home);
    }

    public boolean isInvitedToAll(@NotNull String username) {
        return invites.containsKey(username) && invites.get(username) == null;
    }

    public boolean invite(@NotNull String username, @Nullable String home) {
        if (username.equals(name)) return false;

        if (home == null) {
            if (sql != null) {
                sql.clearInvite(playerId, username);
                sql.addInvite(playerId, username, null);
            }
            boolean didNotContain = !invites.containsKey(username);
            return invites.put(username, null) != null || didNotContain;
        }

        boolean didNotContain;
        Set<String> homeInvites = invites.get(username);
        if (isInvited(username)) {
            didNotContain = homeInvites != null && homeInvites.add(home);
        } else {
            invites.put(username, new HashSet<>(Collections.singletonList(home)));
            didNotContain = true;
        }

        if (sql != null && didNotContain) sql.addInvite(playerId, username, home);

        return didNotContain;
    }

    public boolean invite(@NotNull String username) {
        return invite(username, null);
    }

    public boolean uninvite(@NotNull String username, @Nullable String home) {
        if (!isInvited(username)) return false;

        if (home == null) {
            if (sql != null) sql.clearInvite(playerId, username);
            invites.remove(username);
            return true;
        }

        Set<String> homeInvites = invites.get(username);
        if (homeInvites == null) {
            Set<String> allExcept = homes.stream()
                    .map(h -> h.name)
                    .filter(s -> !s.equals(home))
                    .collect(Collectors.toSet());

            if (sql != null) sql.clearInvite(playerId, username);

            if (allExcept.size() == 0) {
                invites.remove(username);
                return true;
            }

            if (sql != null) {
                allExcept.forEach(s -> sql.addInvite(playerId, username, s));
            }
            invites.put(username, allExcept);
            return true;
        } else {
            if (sql != null) sql.deleteInvite(playerId, username, home);
            boolean contained = homeInvites.remove(home);
            if (homeInvites.size() == 0) {
                invites.remove(username);
            }
            return contained;
        }
    }

    public boolean uninvite(@NotNull String username) {
        return uninvite(username, null);
    }

    public void renameHome(@NotNull String from, @NotNull String to) {
        if (from.equals(to)) {
            return;
        }
        Home home = getHome(from);
        if (home == null) {
            return;
        }
        deleteHome(from);
        setHome(to, home.getLocation());
    }

    public List<String> sortedHomeNames() {
        return homes.stream().map(h -> h.name).sorted(String::compareTo).collect(Collectors.toList());
    }

    public List<String> sortedCanTeleportHomeNames(@NotNull Player player) {
        return homes.stream().filter(h -> canTeleportHome(player, h.name)).map(h -> h.name)
                .sorted(String::compareTo).collect(Collectors.toList());
    }

    public List<String> sortedInvitedHomeNames(@NotNull String username) {
        return homes.stream().filter(h -> isInvited(username, h.name)).map(h -> h.name)
                .sorted(String::compareTo).collect(Collectors.toList());
    }

    public List<String> sortedInvitedNames() {
        return invites.keySet().stream().sorted(String::compareTo).collect(Collectors.toList());
    }
}
