package net.avallion.survival.homes;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;

public class Listeners implements Listener {

    @SuppressWarnings("ConstantConditions")
    @EventHandler
    public void onPlayerRespawn(PlayerRespawnEvent e) {
        if (Homeowner.respawnWorlds.contains(e.getPlayer().getWorld().getName()) &&
                Homeowner.isHomeowner(e.getPlayer())) {
            Homeowner homeowner = Homeowner.get(e.getPlayer());
            if (homeowner.getHomeQty() == 0) return;

            Home respawnHome = homeowner.getHome(homeowner.getRespawnHome());

            e.setRespawnLocation(respawnHome.getLocation());
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent e) {
        Homeowner.removeCache(e.getPlayer().getUniqueId());
    }
}
