package net.avallion.survival.homes;

import me.gladgladius.gladlib.Message;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Arrays;
import java.util.List;

public class AvallionHomes extends JavaPlugin {

    Message msg;
    private final List<String> commandList = Arrays.asList("home", "sethome", "delhome", "homes", "respawnhome",
            "homeinvite", "homeuninvite", "homeinvites", "renamehome");

    @SuppressWarnings("ConstantConditions")
    @Override
    public void onEnable() {
        load();
        Commands commands = new Commands(this);
        TabComplete tab = new TabComplete();
        commandList.forEach(s -> {
            getCommand(s).setExecutor(commands);
            getCommand(s).setTabCompleter(tab);
        });

        if (Homeowner.respawnAtHome) {
            Bukkit.getPluginManager().registerEvents(new Listeners(), this);
        }
    }

    @Override
    public void onDisable() {
        Homeowner.clearCache();
        Homeowner.closeSQL();
    }

    public void load() {
        saveDefaultConfig();
        reloadConfig();
        FileConfiguration config = getConfig();

        msg = new Message("[AvallionHomes] ", config.getString("prefix"));
        Homeowner.load(this, config);
    }
}
