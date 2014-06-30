package com.nijikokun.bukkit.Permissions;

import com.nijiko.CLI;
import com.nijiko.Messaging;
import com.nijiko.Misc;
import java.io.File;
import java.util.logging.Logger;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;
import com.nijiko.configuration.ConfigurationHandler;
import com.nijiko.configuration.DefaultConfiguration;
import com.nijiko.permissions.Control;
import com.nijiko.permissions.PermissionHandler;
import java.util.ArrayList;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.util.config.Configuration;

/**
 * Permissions 1.x & Code from iConomy 2.x
 * Copyright (C) 2011  Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Permissions Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Permissions Public License for more details.
 *
 * You should have received a copy of the GNU Permissions Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
public class Permissions extends JavaPlugin {
    /*
     * Loggery Foggery
     */

    public static final Logger log = Logger.getLogger("Minecraft");

    /*
     * Central Data pertaining directly to the plugin name & versioning.
     */
    public static String name = "Permissions";
    public static String codename = "Handler";
    public static String version = "2.1";
    /**
     * Controller for permissions and security.
     */
    public static PermissionHandler Security;
    /**
     * Controller for permissions and security.
     */
    private Listener Listener = new Listener(this);
    /**
     * Miscellaneous object for various functions that don't belong anywhere else
     */
    public static Misc Misc = new Misc();

    /*
     * Internal Properties controllers
     */
    private DefaultConfiguration config;

    /*
     * Server
     */
    public static Server Server = null;

    private String DefaultWorld = "";

    public void onDisable() {
        log.info("[" + name + "] version [" + version + "] (" + codename + ") Disabled.");
    }

    public Permissions() {
        new File("plugins" + File.separator + "Permissions" + File.separator).mkdirs();

        PropertyHandler server = new PropertyHandler("server.properties");
        DefaultWorld = server.getString("level-name");

        // Attempt
        if (!(new File(getDataFolder(), DefaultWorld + ".yml").exists())) {
            Misc.touch(DefaultWorld + ".yml");
        }

        Configuration configure = new Configuration(new File(getDataFolder(), DefaultWorld + ".yml"));
        configure.load();

        // Gogo
        this.config = new ConfigurationHandler(configure);

        // Setup Permission
        setupPermissions();

        // Enabled
        log.info("[" + name + "] version [" + version + "] (" + codename + ") was Initialized.");
    }

    public void onEnable() {
        // Server
        Server = getServer();

        // Register
        registerEvents();

        // Enabled
        log.info("[" + name + "] version [" + version + "] (" + codename + ") loaded");
    }

    private void registerEvents() {
        this.getServer().getPluginManager().registerEvent(Type.PLAYER_CHAT, Listener, Priority.Monitor, this);
    }

    /**
     * Alternative method of grabbing Permissions.Security
     * <br /><br />
     * <blockquote><pre>
     * Permissions.getHandler()
     * </pre></blockquote>
     *
     * @return PermissionHandler
     */
    public PermissionHandler getHandler() {
        return Security;
    }

    public void setupPermissions() {
        Security = new Control(new Configuration(new File("plugins" + File.separator + "Permissions", DefaultWorld + ".yml")));
        Security.setDefaultWorld(DefaultWorld);
        Security.setDirectory(new File("plugins" + File.separator + "Permissions"));
        Security.load();
    }

    private class Listener extends PlayerListener {

        private Permissions plugin;
        private CLI Commands;

        public Listener(Permissions plugin) {
            this.plugin = plugin;

            this.Commands = new CLI();
            Commands.add("/pr|perms", "Reload Permissions.");
            Commands.add("/pr|perms -reload|-r +world:all", "Reload World.");
        }

        @Override
        public void onPlayerChat(PlayerChatEvent event) {
            final Player player = event.getPlayer();
            String message = event.getMessage();

            // Save player.
            Messaging.save(player);

            // Commands
            Commands.save(message);

            // Parsing / Checks
            String base = Commands.base();
            String command = Commands.command();
            ArrayList<Object> variables = Commands.parse();

            if (base != null) {
                if (Misc.isEither(base, "pr", "perms")) {
                    if (command == null) {
                        Messaging.send("&7-------[ &fPermissions&7 ]-------");
                        Messaging.send("&7Currently running version: &f" + version);

                        if (Security.permission(player, "permissions.reload")) {
                            Messaging.send("&7Reload with: &f/pr reload");
                        }

                        Messaging.send("&7-------[ &fPermissions&7 ]-------");

                        return;
                    }

                    if(Misc.isEither(command, "reload", "-r")) {
                        if (Security.permission(player, "permissions.reload")) {
                            String world = Commands.getString("world");

                            if (world.equalsIgnoreCase("all")) {
                                Security.reload();
                                Messaging.send(ChatColor.GRAY + "[Permissions] Reload completed.");
                            } else {
                                if(Security.reload(world)) {
                                   Messaging.send(ChatColor.GRAY + "[Permissions] Reload of "+ world +" completed.");
                                } else {
                                   Messaging.send("&7[Permissions] "+ world +" does not exist!");
                                }
                            }
                        }

                        return;
                    }
                }
            }
        }
    }
}
