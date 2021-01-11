package net.avallion.survival.homes;

import me.gladgladius.gladlib.SQLData;
import me.gladgladius.gladlib.SQLLink;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

public class SQLAdapter extends SQLLink {

    private PreparedStatement homesQuery, playersQuery, invitesQuery, playerQuery, savePlayer, lastInsertId, saveHome,
    delHome, delAllHomes, addInvite, delInvite, clearInvite, updateRespawnHome, playerQueryName;

    public SQLAdapter(SQLData data, boolean useSSL) {
        super(data, useSSL);

        createTable("avallionhomes_players",
                "id INT NOT NULL PRIMARY KEY AUTO_INCREMENT, " +
                        "uuid VARCHAR(36) NOT NULL, " +
                        "username VARCHAR(16) NOT NULL, " +
                        "respawn_home VARCHAR(64), " +
                        "UNIQUE (uuid, username)");
        createTable("avallionhomes_homes",
                "player_id INT NOT NULL, " +
                        "name VARCHAR(64) NOT NULL, " +
                        "world VARCHAR(64) NOT NULL, " +
                        "x INT NOT NULL, " +
                        "y INT NOT NULL, " +
                        "z INT NOT NULL, " +
                        "pitch FLOAT NOT NULL, " +
                        "yaw FLOAT NOT NULL, " +
                        "UNIQUE (player_id, name), " +
                        "CONSTRAINT fk_homes_player FOREIGN KEY (player_id) REFERENCES avallionhomes_players(id)");
        createTable("avallionhomes_invites",
                "player_id INT NOT NULL, " +
                        "invited VARCHAR(16) NOT NULL, " +
                        "home_name VARCHAR(64), " +
                        "UNIQUE (player_id, invited, home_name), " +
                        "CONSTRAINT fk_invites_player FOREIGN KEY (player_id) REFERENCES avallionhomes_players(id)");

        try {
            homesQuery = connection.prepareStatement("SELECT name, world, x, y, z, yaw, pitch FROM " +
                    "avallionhomes_homes WHERE player_id=?;");
            playersQuery = connection.prepareStatement("SELECT * FROM avallionhomes_players;");
            invitesQuery = connection.prepareStatement("SELECT invited, home_name FROM avallionhomes_invites " +
                    "WHERE player_id=? ORDER BY invited;");
            playerQuery = connection.prepareStatement("SELECT * FROM avallionhomes_players WHERE uuid=?;");
            savePlayer = connection.prepareStatement("INSERT INTO avallionhomes_players (uuid, username, " +
                    "respawn_home) VALUES (?, ?, ?);");
            lastInsertId = connection.prepareStatement("SELECT LAST_INSERT_ID() AS id;");
            saveHome = connection.prepareStatement("INSERT INTO avallionhomes_homes (player_id, name, world, x, y, z," +
                    " pitch, yaw) VALUES (?, ?, ?, ?, ?, ?, ?, ?);");
            delHome = connection.prepareStatement("DELETE FROM avallionhomes_homes WHERE player_id=? AND name=?;");
            delAllHomes = connection.prepareStatement("DELETE FROM avallionhomes_homes WHERE player_id=?;");
            addInvite = connection.prepareStatement("INSERT INTO avallionhomes_invites (player_id, invited, " +
                    "home_name) VALUES (?, ?, ?);");
            delInvite = connection.prepareStatement("DELETE FROM avallionhomes_invites WHERE player_id=? AND " +
                    "invited=? AND home_name=?;");
            clearInvite = connection.prepareStatement("DELETE FROM avallionhomes_invites WHERE player_id=? AND " +
                    "invited=?;");
            updateRespawnHome = connection.prepareStatement("UPDATE avallionhomes_players SET respawn_home=? WHERE " +
                    "id=?;");
            playerQueryName = connection.prepareStatement("SELECT * FROM avallionhomes_players WHERE username=?;");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public int savePlayer(Homeowner homeowner) throws SQLException {
        int playerId;

        playerQuery.setString(1, homeowner.getUuid().toString());
        ResultSet playerResult = playerQuery.executeQuery();

        if (!playerResult.next()) {
            savePlayer.setString(1, homeowner.getUuid().toString());
            savePlayer.setString(2, homeowner.getName());
            savePlayer.setString(3, homeowner.getRespawnHome());
            savePlayer.executeUpdate();
            ResultSet id = lastInsertId.executeQuery();
            id.next();
            playerId = id.getInt("id");
        } else {
            playerId = playerResult.getInt("id");
        }

        return playerId;
    }

    public void initHomeowner(Homeowner homeowner) {
        try {
            int playerId = savePlayer(homeowner);
            homeowner.getHomes().forEach((home) -> addHome(playerId, home));
            homeowner.getInvites().forEach((invited, homes) -> {
                        if (homes == null) {
                            addInvite(playerId, invited, null);
                        } else {
                            homes.forEach((home -> addInvite(playerId, invited, home)));
                        }
                    }
            );
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addHome(int playerId, Home home) {
        try {
            saveHome.setInt(1, playerId);
            saveHome.setString(2, home.name);
            saveHome.setString(3, home.world);
            saveHome.setInt(4, home.x);
            saveHome.setInt(5, home.y);
            saveHome.setInt(6, home.z);
            saveHome.setFloat(7, home.pitch);
            saveHome.setFloat(8, home.yaw);
            saveHome.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteHome(int playerId, String name) {
        try {
            delHome.setInt(1, playerId);
            delHome.setString(2, name);
            delHome.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteAllHomes(int playerId) {
        try {
            delAllHomes.setInt(1, playerId);
            delAllHomes.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void addInvite(int playerId, String invited, String homeName) {
        try {
            addInvite.setInt(1, playerId);
            addInvite.setString(2, invited);
            addInvite.setString(3, homeName);
            addInvite.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void deleteInvite(int playerId, String invited, String homeName) {
        try {
            delInvite.setInt(1, playerId);
            delInvite.setString(2, invited);
            delInvite.setString(3, homeName);
            delInvite.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void clearInvite(int playerId, String invited) {
        try {
            clearInvite.setInt(1, playerId);
            clearInvite.setString(2, invited);
            clearInvite.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setRespawnHome(int playerId, String respawnHome) {
        try {
            updateRespawnHome.setString(1, respawnHome);
            updateRespawnHome.setInt(2, playerId);
            updateRespawnHome.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public List<Homeowner> getHomeowners() {
        List<Homeowner> homeowners = new ArrayList<>();
        try {
            ResultSet result = playersQuery.executeQuery();
            while (result.next()) {
                homeowners.add(toHomeowner(result.getInt("id"), UUID.fromString(result.getString("uuid")),
                        result.getString("username"), result.getString("respawn_home")));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return homeowners;
    }

    private Homeowner toHomeowner(int playerId, UUID uuid, String username, String respawnHome) throws SQLException {
        Homeowner homeowner = new Homeowner(uuid, username, playerId);
        homeowner.setRespawnHome(respawnHome, false);
        homeowner.setHomes(getHomes(playerId));
        homeowner.setInvites(getInvites(playerId));
        return homeowner;
    }

    @Nullable
    public Homeowner getHomeowner(UUID uuid) {
        try {
            playerQuery.setString(1, uuid.toString());
            ResultSet playerResult = playerQuery.executeQuery();
            if (playerResult.next()) {
                int playerId = playerResult.getInt("id");
                Homeowner homeowner = new Homeowner(uuid, playerResult.getString("username"), playerId);
                homeowner.setRespawnHome(playerResult.getString("respawn_home"), false);
                homeowner.setHomes(getHomes(playerId));
                homeowner.setInvites(getInvites(playerId));
                return homeowner;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Nullable
    public Homeowner getHomeowner(String name) {
        try {
            playerQueryName.setString(1, name);
            ResultSet playerResult = playerQueryName.executeQuery();
            if (playerResult.next()) {
                int playerId = playerResult.getInt("id");
                Homeowner homeowner = new Homeowner(UUID.fromString(playerResult.getString("uuid")), name, playerId);
                homeowner.setRespawnHome(playerResult.getString("respawn_home"), false);
                homeowner.setHomes(getHomes(playerId));
                homeowner.setInvites(getInvites(playerId));
                return homeowner;
            } else {
                return null;
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Set<Home> getHomes(int playerId) throws SQLException {
        Set<Home> homes = new HashSet<>();
        homesQuery.setInt(1, playerId);
        ResultSet result = homesQuery.executeQuery();
        while (result.next()) {
            homes.add(new Home(result.getString("name"),
                            result.getString("world"),
                            result.getInt("x"),
                            result.getInt("y"),
                            result.getInt("z"),
                            result.getFloat("pitch"),
                            result.getFloat("yaw"))
            );
        }
        return homes;
    }

    private Map<String, Set<String>> getInvites(int playerId) throws SQLException {
        Map<String, Set<String>> invites = new HashMap<>();
        invitesQuery.setInt(1, playerId);
        ResultSet result = invitesQuery.executeQuery();
        while (result.next()) {
            String invited = result.getString("invited"),
                    homeName = result.getString("home_name");
            if (!invites.containsKey(invited)) {
                invites.put(invited, new HashSet<>(Collections.singletonList(homeName)));
            } else {
                if (homeName == null) {
                    invites.put(invited, null);
                } else {
                    Set<String> homes = invites.get(invited);
                    homes.add(homeName);
                }
            }
        }
        return invites;
    }
}
