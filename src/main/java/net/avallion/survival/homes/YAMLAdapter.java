package net.avallion.survival.homes;

import me.gladgladius.gladlib.YAMLLink;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

public class YAMLAdapter extends YAMLLink {

    private final File yamlDirectory;
    private final AvallionHomes plugin;

    public YAMLAdapter(AvallionHomes plugin, String yamlDirectory) {
        this.plugin = plugin;
        this.yamlDirectory = new File(yamlDirectory);
    }

    @SuppressWarnings("unchecked")
    public List<Homeowner> getHomeowners(List<String> ignoredFiles) {
        File[] files = yamlDirectory.listFiles(f -> !ignoredFiles.contains(f.getName()));

        if (files == null || files.length == 0) {
            return new ArrayList<>();
        }
        plugin.msg.log("Found " + files.length + " new YAML files in /AvallionHomes/playerdata/. Migrating to " +
                "MySQL, this will take a while...");
        return Arrays.stream(files).map(file -> {
            Map<String, Object> root = null;
            try {
                root = getRoot(file);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }

            UUID id = UUID.fromString(file.getName().replaceAll("\\.ya?ml$", ""));

            String name = get(root, "player.info.lastSeenAs");

            Homeowner homeowner = new Homeowner(id, Objects.requireNonNull(name));

            Map<String, Object> homes = getKeys(root, "homes");
            if (homes != null && homes.size() > 0) {
                homeowner.setHomes(homes.entrySet().stream().map(e -> {
                    Map<String, Object> homeData = (Map<String, Object>) e.getValue();
                    int x = (int) homeData.get("x"), y = (int) homeData.get("y"), z = (int) homeData.get("z");
                    float pitch = ((Double) homeData.get("pitch")).floatValue(), yaw = ((Double) homeData.get("yaw")).floatValue();
                    String world = (String) homeData.get("world");

                    return new Home(e.getKey(), world, x, y, z, pitch, yaw);
                }).collect(Collectors.toSet()));
            }

            return homeowner;
        }).collect(Collectors.toList());
    }
}
