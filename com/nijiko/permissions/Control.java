package com.nijiko.permissions;

import com.nijiko.Messaging;
import com.nijikokun.bukkit.Permissions.FileManager;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

/**
 * Permissions 2.x
 * Copyright (C) 2010  Nijikokun <nijikokun@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * iControl.java
 * Permission handler
 *
 * @author Nijiko
 */
public class Control extends PermissionHandler {

    public static final Logger log = Logger.getLogger("Minecraft");

    private List<String> Worlds = new LinkedList<String>();
    private Map<String, Configuration> WorldConfiguration = new HashMap<String, Configuration>();
    private Map<String, String> WorldBase = new HashMap<String, String>();
    private Map<String, String> WorldInheritance = new HashMap<String, String>();
    private Map<String, Map<String, Set<String>>> WorldUserPermissions = new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, String>> WorldUserGroups = new HashMap<String, Map<String, String>>();
    private Map<String, Map<String, Set<String>>> WorldGroups = new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, Object[]>> WorldGroupsData = new HashMap<String, Map<String, Object[]>>();
    private Map<String, Map<String, Set<String>>> WorldGroupsInheritance = new HashMap<String, Map<String, Set<String>>>();
    private Map<String, Map<String, Boolean>> WorldCache = new HashMap<String, Map<String, Boolean>>();

    private File directory;
    private String defaultWorld = "";
    private Configuration config;

    public Control(Configuration config) {
        this.config = config;
    }

    public void reload() {
        this.clearAllCache();

        final List<String> Syncronized = new LinkedList<String>(Worlds);
        Worlds = new LinkedList<String>();

        synchronized (Syncronized) {
            for (Iterator it = Syncronized.iterator(); it.hasNext(); ) {
                String world = (String)it.next();
                this.forceLoadWorld(world);
            }
        }
    }
    
    public void setDefaultWorld(String world) {
        // log.info("Default world: " + world);
        this.defaultWorld = world;
    }

    public void setDirectory(File directory) {
        this.directory = directory;
    }

    public boolean loadWorld(String world) {
        // log.info("Checking for the world: " + world);
        if(!this.Worlds.contains(world)) {
            this.load(world, new Configuration(new File(this.directory + File.separator + world + ".yml")));
            log.info("Loaded world: " + world);
           return true;
        }

        // log.info("World already exists: " + world);
        return false;
    }
    
    public void forceLoadWorld(String world) {
        this.load(world, new Configuration(new File(this.directory + File.separator + world + ".yml")));
    }

    public boolean checkWorld(String world) {
        if(this.Worlds.contains(world)) {
            return true;
        } else {
            return false;
        }
    }

    public void load() {
        if(this.defaultWorld == null ? "" != null : !this.defaultWorld.equals("")) {
            return;
        }

        this.load(this.defaultWorld, this.config);
    }

    public void load(String world, Configuration config) {
        if (!(new File(this.directory + File.pathSeparator + world + ".yml").exists())) {
            FileManager file = new FileManager(this.directory.getPath() + File.separator, world + ".yml", true);
        }

        config.load();

        this.Worlds.add(world);
        this.WorldConfiguration.put(world, config);

        if(!world.equals(this.defaultWorld)) {
            // log.info("Checking for copying in world: " + directory.getPath() + world + ".yml");
            if(config.getString("plugin.permissions.copies", "") == null ? "" != null : !config.getString("plugin.permissions.copies", "").equals("")) {
                // log.info("Copying exists for world: " + directory.getPath() + world + ".yml");
                this.WorldInheritance.put(world, config.getString("plugin.permissions.copies", ""));
                return;
            }
        }

        this.WorldBase.put(world, "");
        this.WorldCache.put(world, new HashMap<String, Boolean>());
        this.WorldUserPermissions.put(world, new HashMap<String, Set<String>>());
        this.WorldUserGroups.put(world, new HashMap<String, String>());
        this.WorldGroups.put(world, new HashMap<String, Set<String>>());
        this.WorldGroupsData.put(world, new HashMap<String, Object[]>());
        this.WorldGroupsInheritance.put(world, new HashMap<String, Set<String>>());

        // Grab the keys we are going to need
        List<String> userKeys = config.getKeys("users");
        List<String> groupKeys = config.getKeys("groups");

        // Permission set.
        Set Permissions = new HashSet();
        Set Inheritance = new HashSet();

        // Permission list
        List permissions;
        List inheritance;

        // Group
        String group;

        // log.info("Grabbing group keys for world: " + directory.getPath() + File.separator + world + ".yml");
        // log.info("User keys for world: " + world + " - " + new ArrayList<String>(groupKeys).toString());
        if (groupKeys != null) {
            for (String key : groupKeys) {
                Inheritance = new HashSet();
                Permissions = new HashSet();

                // Configuration
                inheritance = config.getStringList("groups." + key + ".inheritance", null);
                permissions = config.getStringList("groups." + key + ".permissions", null);
                boolean Default = config.getBoolean("groups." + key + ".default", false);
                String prefix = config.getString("groups." + key + ".info.prefix", null);
                String suffix = config.getString("groups." + key + ".info.suffix", null);
                boolean build = config.getBoolean("groups." + key + ".info.build", false);

                if (Default && (this.WorldBase.get(world) == null ? "" == null : this.WorldBase.get(world).equals(""))) {
                    this.WorldBase.put(world, key.toLowerCase());
                }

                if (inheritance.size() > 0) {
                    Inheritance.addAll(inheritance);
                }

                if (permissions.size() > 0) {
                    Permissions.addAll(permissions);
                }

                // log.info("Updating group data for world: " + directory.getPath() + File.separator + world + ".yml");
                // log.info("Permissions For Group: " + key + " - " + new ArrayList<String>(Permissions).toString());
                this.WorldGroups.get(world).put(key.toLowerCase(), Permissions);
                this.WorldGroupsData.get(world).put(key.toLowerCase(), new Object[]{key, prefix, suffix, build});
                // log.info("Updated group data for world: " + directory.getPath() + world + ".yml");

                if (Inheritance.size() > 0) {
                   this.WorldGroupsInheritance.get(world).put(key.toLowerCase(), Inheritance);
                }
            }
        }

        // log.info("Grabbing userkeys for world: " + directory.getPath() + File.separator + world + ".yml");
        // log.info("User keys for world: " + world + " - " + new ArrayList<String>(userKeys).toString());
        if (userKeys != null) {
            for (String key : userKeys) {
                Permissions = new HashSet();

                // Configuration
                permissions = config.getStringList("users." + key + ".permissions", null);
                group = config.getString("users." + key + ".group");

                if (group != null) {
                    if (!group.isEmpty()) {
                        this.WorldUserGroups.get(world).put(key.toLowerCase(), group);
                    }
                } else {
                    this.WorldUserGroups.get(world).put(key.toLowerCase(), this.WorldBase.get(world));
                }

                if (permissions.size() > 0) {
                    Permissions.addAll(permissions);
                }

                this.WorldUserPermissions.get(world).put(key.toLowerCase(), Permissions);
            }
        }
    }

    private String toArrayListString(Collection variable) {
        return new ArrayList<String>(variable).toString();
    }

    /**
     * Simple alias for permission method.
     * Easier to understand / recognize what it does and is checking for.
     *
     * @param player
     * @param permission
     * @return boolean
     */
    public boolean has(Player player, String permission) {
        return this.permission(player, permission);
    }

    /**
     * Checks to see if a player has permission to a specific tree node.
     * <br /><br />
     * Example usage:
     * <blockquote><pre>
     * boolean canReload = Plugin.Permissions.Security.permission(player, "permission.reload");
     * if(canReload) {
     *	System.out.println("The user can reload!");
     * } else {
     *	System.out.println("The user has no such permission!");
     * }
     * </pre></blockquote>
     *
     * @param player
     * @param permission
     * @return boolean
     */
    public boolean permission(Player player, String permission) {
        Set<String> Permissions = new HashSet();
        Set<String> GroupPermissions = new HashSet();
        Set<String> GroupInheritedPermissions = new HashSet();
        String group = "";
        String name = player.getName().toLowerCase();
        String world = player.getWorld().getName();

        // log.info("Checking inside world: " + world);

        // Load if it isn't already
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);

            // log.info("Checking inherited world: " + world);
        }

        if (this.WorldCache.get(world).containsKey(name + "," + permission)) {
            // log.info("World contained cached node " + permission + ": " + world);
            return this.WorldCache.get(world).get(name + "," + permission);
        }

        Map<String, Set<String>> UserPermissions = this.WorldUserPermissions.get(world);
        Map<String, String> UserGroups = this.WorldUserGroups.get(world);
        Map<String, Set<String>> Groups = this.WorldGroups.get(world);
        Map<String, Set<String>> GroupsInheritance = this.WorldGroupsInheritance.get(world);
        Map<String, Boolean> Cached = this.WorldCache.get(world);
        String base = this.WorldBase.get(world);

        // log.info("Checking for the node " + permission + " in the world: " + world);

        if (this.WorldUserPermissions.get(world).containsKey(name)) {
            Permissions = (Set<String>) UserPermissions.get(name);
            group = ((String) UserGroups.get(name)).toLowerCase();

            // log.info("User group:" + group);
            // log.info("User Permissions: " + (new ArrayList<String>(Permissions)).toString());

            if (!Groups.isEmpty() || Groups != null) {
                if (Groups.containsKey(group)) {
                    GroupPermissions = (Set<String>) Groups.get(group);
                }

                if (GroupsInheritance.containsKey(group)) {
                    GroupInheritedPermissions = getInheritancePermissions(world, group);
                }
            } else {
                Cached.put(name + "," + permission, false);
                return false;
            }
        } else {
            if ((base == null ? "" == null : base.equals(""))) {
                Cached.put(name + "," + permission, false);
                return false;
            }

            group = ((String) base).toLowerCase();

            if (!Groups.isEmpty() || Groups != null) {
                if (Groups.containsKey(group)) {
                    GroupPermissions = (Set<String>) Groups.get(group);
                }

                if (GroupsInheritance.containsKey(group)) {
                    GroupInheritedPermissions = getInheritancePermissions(world, group);
                }
            } else {
                Cached.put(name + "," + permission, false);
                return false;
            }
        }

        StringTokenizer globalized = new StringTokenizer(permission, ".");

        if (GroupInheritedPermissions.size() > 0) {
            GroupPermissions.addAll(GroupInheritedPermissions);
        }

        // log.info("Group Permissions: " + (new ArrayList<String>(GroupPermissions)).toString());

        if (Permissions == null || GroupPermissions == null) {
            Cached.put(name + "," + permission, false);
            return false;
        }

        if(GroupPermissions.contains("-" + permission) || Permissions.contains("-" + permission)) {
            Cached.put(name + "," + permission, false);
            return false;
        }

        if (GroupPermissions.contains("*") || Permissions.contains("*")) {
            Cached.put(name + "," + permission, true);
            return true;
        }

        if (GroupPermissions.contains(permission) || Permissions.contains(permission)) {
            Cached.put(name + "," + permission, true);
            return true;
        }

        if (permission.contains(".")) {
            String setting = "";
            String node = "";

            while (globalized.hasMoreElements()) {
                setting += (String) globalized.nextToken() + ".";
                node = setting + "*";

                if (GroupPermissions.contains(node) || Permissions.contains(node)) {
                    Cached.put(name + "," + permission, true);
                    return true;
                } else {
                    continue;
                }
            }
        }

        Cached.put(name + "," + permission, false);
        return false;
    }

    private Set<String> getInheritance(String world, String group) {
        if (this.WorldGroupsInheritance.containsKey(world)) {
            Map<String, Set<String>> WorldGroupInheritance = this.WorldGroupsInheritance.get(world);

            if(WorldGroupInheritance.containsKey(group)) {
                if (WorldGroupInheritance.size() > 0) {
                    return WorldGroupInheritance.get(group);
                } else {
                    return new HashSet<String>();
                }
            }

            return new HashSet<String>();
        }

        return new HashSet<String>();
    }

    private Object[] getInheritancePermissions(String world, Set<String> Permissions, Set<String> Inheritance, Set<String> Checked, String group) {
        Map<String, Set<String>> Groups = this.WorldGroups.get(world);

        group = group.toLowerCase();

        if (Inheritance.size() > 0) {
            for (String inherited : Inheritance) {
                inherited = inherited.toLowerCase();
                Set<String> GroupPermissions = (Set<String>) Groups.get(inherited.toLowerCase());
                Set<String> GottenInheritance = getInheritance(world, inherited);

                if (GroupPermissions == null) {
                    continue;
                }

                if (GroupPermissions.size() > 0) {
                    Permissions.addAll(GroupPermissions);
                }

                if (!Checked.contains(inherited)) {
                    Checked.add(inherited);
                    Object[] InheritedPermissions = getInheritancePermissions(world, Permissions, GottenInheritance, Checked, inherited);

                    if (((Set<String>) InheritedPermissions[0]).size() > 0) {
                        Permissions.addAll((Set<String>) InheritedPermissions[0]);
                    }
                }
            }
        } else {
            Set<String> GroupPermissions = (Set<String>) Groups.get(group);

            if (GroupPermissions.size() > 0) {
                Permissions.addAll(GroupPermissions);
            }
        }

        return new Object[]{Permissions, Checked};
    }

    private Set<String> getInheritancePermissions(String world, String group) {
        group = group.toLowerCase();
        Map<String, Set<String>> Groups = this.WorldGroups.get(world);
        Set<String> Permissions = new HashSet();
        Set<String> Inheritance = getInheritance(world, group);
        Set<String> Checked = new HashSet();

        if (Inheritance.size() > 0 && !Inheritance.isEmpty()) {
            for (String inherited : Inheritance) {
                inherited = inherited.toLowerCase();
                Set<String> GroupPermissions = (Set<String>) Groups.get(inherited);

                if (GroupPermissions == null) {
                    continue;
                }

                if (GroupPermissions.size() > 0) {
                    Permissions.addAll(GroupPermissions);
                }

                if (getInheritance(world, inherited).size() > 0 && !Checked.contains(inherited)) {
                    Checked.add(inherited);
                    Object[] InheritedPermissions = getInheritancePermissions(world, Permissions, getInheritance(world, inherited), Checked, inherited);

                    if (((Set<String>) InheritedPermissions[0]).size() > 0) {
                        Permissions.addAll((Set<String>) InheritedPermissions[0]);
                    }
                }
            }
        }

        return Permissions;
    }

    public boolean inGroup(String world, String name, String group) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        name = name.toLowerCase();
        group = group.toLowerCase();

        if (this.WorldUserPermissions.get(world).containsKey(name)) {
            String Group = (String) this.WorldUserGroups.get(world).get(name);
            Set<String> Inherited = getInheritance(world, Group);

            if (Inherited.contains(group) || Group.equalsIgnoreCase(group)) {
                return true;
            }
        }

        return false;
    }

    public String getGroup(String world, String name) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        name = name.toLowerCase();

        if (this.WorldUserPermissions.get(world).containsKey(name) && this.WorldUserGroups.get(world).containsKey(name)) {
            String group = (String) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldUserGroups.get(world).get(name).toLowerCase()))[0];
            return (group == null) ? null : group;
        } else {
            if (this.WorldBase.get(world).equals("")) {
                return null;
            } else {
                String group = (String) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[0];
                return (group == null) ? null : group;
            }
        }
    }

    public String getGroupPrefix(String world, String group) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            String prefix = (String) ((Object[]) this.WorldGroupsData.get(world).get(group))[1];
            return (prefix == null) ? null : Messaging.parse(prefix);
        } else {
            return null;
        }
    }

    public String getGroupSuffix(String world, String group) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            String suffix = (String) ((Object[]) this.WorldGroupsData.get(world).get(group))[2];
            return (suffix == null) ? null : Messaging.parse(suffix);
        } else {
            return null;
        }
    }

    public boolean canGroupBuild(String world, String group) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        group = group.toLowerCase();

        if (this.WorldGroups.get(world).containsKey(group)) {
            return (Boolean) ((Object[]) this.WorldGroupsData.get(world).get(group))[3];
        } else {
            if (this.WorldBase.get(world).equals("")) {
                return false;
            } else {
                return (Boolean) ((Object[]) this.WorldGroupsData.get(world).get(this.WorldBase.get(world)))[3];
            }
        }
    }

    public String[] getGroups(String world, String name) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        String Group = (String) this.WorldUserGroups.get(world).get(name.toLowerCase());
        Set<String> Inherited = getInheritance(world, Group.toLowerCase());
        Inherited.add(Group.toLowerCase());

        return Inherited.toArray(new String[0]);
    }

    @Deprecated
    public void setCache(Map<String, Boolean> Cache) {
        this.WorldCache.put(this.defaultWorld, Cache);
    }
    
    public void setCache(String world, Map<String, Boolean> Cache) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            this.WorldCache.put(world, Cache);
        }
    }
    
    @Deprecated
    public void setCacheItem(String player, String permission, boolean data) {
        this.WorldCache.get(this.defaultWorld).put(player + "," + permission, data);
    }
    
    public void setCacheItem(String world, String player, String permission, boolean data) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            this.WorldCache.get(world).put(player + "," + permission, data);
        }
    }

    @Deprecated
    public Map<String, Boolean> getCache() {
        return this.WorldCache.get(this.defaultWorld);
    }

    public Map<String, Boolean> getCache(String world) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            return this.WorldCache.get(world);
        }

        return new HashMap<String, Boolean>();
    }

    @Deprecated
    public boolean getCacheItem(String player, String permission) {
        if (this.WorldCache.get(this.defaultWorld).containsKey(player + "," + permission)) {
            return this.WorldCache.get(this.defaultWorld).get(player + "," + permission);
        }

        return false;
    }

    public boolean getCacheItem(String world, String player, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.WorldInheritance.containsKey(world)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            if (this.WorldCache.get(world).containsKey(player + "," + permission)) {
                return this.WorldCache.get(world).get(player + "," + permission);
            }
        }

        return false;
    }

    @Deprecated
    public void removeCachedItem(String player, String permission) {
        this.removeCachedItem(this.defaultWorld, player, permission);
    }

    public void removeCachedItem(String world, String player, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.WorldInheritance.containsKey(world)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        if(this.Worlds.contains(world)) {
            if (this.WorldCache.get(world).containsKey(player + "," + permission)) {
                this.WorldCache.get(world).remove(player + "," + permission);
            }
        }
    }
    
    public void clearCache() {
        this.WorldCache.put(this.defaultWorld, new HashMap<String, Boolean>());
    }

    public void clearAllCache() {
        for(String world : this.WorldCache.keySet()) {
            this.WorldCache.put(world, new HashMap<String, Boolean>());
        }
    }

    public void clearCache(String world) {
        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);
        }

        if(this.Worlds.contains(world)) {
            this.WorldCache.put(world, new HashMap<String, Boolean>());
        }
    }
    
    public void addGroupPermission(String world, String group, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("groups." + group + ".permissions", new LinkedList<String>());
        list.add(node);
        this.WorldConfiguration.get(world).setProperty("groups." + group + ".permissions", list);
    }
    
    public void removeGroupPermission(String world, String group, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("groups." + group + ".permissions", new LinkedList<String>());
        
        if(list.contains(node)) {
            list.remove(node);
        }
        
        this.WorldConfiguration.get(world).setProperty("groups." + group + ".permissions", list);
    }
    
    public void addGroupInfo(String world, String group, String node, Object data) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).setProperty("groups." + group + ".info." + node, data);
    }
    
    public void removeGroupInfo(String world, String group, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).removeProperty("groups." + group + ".info." + node);
    }
    
    public void addUserPermission(String world, String user, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("users." + user + ".permissions", new LinkedList<String>());
        list.add(node);
        this.WorldConfiguration.get(world).setProperty("users." + user + ".permissions", list);
    }
    
    public void removeUserPermission(String world, String user, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        List<String> list = this.WorldConfiguration.get(world).getStringList("users." + user + ".permissions", new LinkedList<String>());
        
        if(list.contains(node)) {
            list.remove(node);
        }
        
        this.WorldConfiguration.get(world).setProperty("users." + user + ".permissions", list);
    }
    
    public void addUserInfo(String world, String user, String node, Object data) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).setProperty("users." + user + ".info." + node, data);
    }
    
    public void removeUserInfo(String world, String user, String node) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        this.WorldConfiguration.get(world).removeProperty("users." + user + ".info." + node);
    }
    
    public String getGroupPermissionString(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return (this.WorldConfiguration.get(world).getString("groups." + group + ".info." + permission) == null)
                ? "" : this.WorldConfiguration.get(world).getString("groups." + group + ".info." + permission);
    }
    
    public int getGroupPermissionInteger(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getInt("groups." + group + ".info." + permission, -1);
    }
    
    public boolean getGroupPermissionBoolean(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getBoolean("groups." + group + ".info." + permission, false);
    }
    
    public double getGroupPermissionDouble(String world, String group, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getDouble("groups." + group + ".info." + permission, -1.0);
    }
    
    public String getUserPermissionString(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return (this.WorldConfiguration.get(world).getString("users." + name + ".info." + permission) == null)
                ? "" : this.WorldConfiguration.get(world).getString("users." + name + ".info." + permission);
    }
    
    public int getUserPermissionInteger(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getInt("users." + name + ".info." + permission, -1);
    }
    
    public boolean getUserPermissionBoolean(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getBoolean("users." + name + ".info." + permission, false);
    }
    
    public double getUserPermissionDouble(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        return this.WorldConfiguration.get(world).getDouble("users." + name + ".info." + permission, -1.0);
    }
    
    public String getPermissionString(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        String group = this.getGroup(world, name);
        String userPermission = this.getUserPermissionString(world, name, permission);
        String userGroupPermission = "";

        if (group != null || !group.isEmpty()) {
            userGroupPermission = this.getGroupPermissionString(world, group, permission);
        }

        if (!userPermission.equalsIgnoreCase("")) {
            return userPermission;
        }

        return userGroupPermission;
    }
    
    public boolean getPermissionBoolean(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        String group = this.getGroup(world, name);
        boolean userPermission = this.getUserPermissionBoolean(world, name, permission);
        boolean userGroupPermission = false;

        if (group != null || !group.isEmpty()) {
            userGroupPermission = this.getGroupPermissionBoolean(world, group, permission);
        }

        if (userPermission) {
            return userPermission;
        }

        return userGroupPermission;
    }
    
    public int getPermissionInteger(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        String group = this.getGroup(world, name);
        int userPermission = this.getUserPermissionInteger(world, name, permission);
        int userGroupPermission = -1;

        if (group != null || !group.isEmpty()) {
            userGroupPermission = this.getGroupPermissionInteger(world, group, permission);
        }

        if (userPermission != -1) {
            return userPermission;
        }

        return userGroupPermission;
    }
    
    public double getPermissionDouble(String world, String name, String permission) {
        this.loadWorld(world);

        if(this.WorldInheritance.containsKey(world) && !world.equals(this.defaultWorld)) {
            world = this.WorldInheritance.get(world);

            this.loadWorld(world);
        }

        String group = this.getGroup(world, name);
        double userPermission = this.getUserPermissionDouble(world, name, permission);
        double userGroupPermission = -1.0;

        if (group != null || !group.isEmpty()) {
            userGroupPermission = this.getGroupPermissionDouble(world, group, permission);
        }

        if (userPermission != -1.0) {
            return userPermission;
        }

        return userGroupPermission;
    }

    @Deprecated
    public boolean inGroup(String name, String group) {
        return this.inGroup(this.defaultWorld, name, group);
    }

    @Deprecated
    public String getGroup(String name) {
        return this.getGroup(this.defaultWorld, name);
    }

    @Deprecated
    public String getGroupPrefix(String group) {
        return this.getGroupPrefix(this.defaultWorld, group);
    }

    @Deprecated
    public String getGroupSuffix(String group) {
        return this.getGroupSuffix(this.defaultWorld, group);
    }

    @Deprecated
    public boolean canGroupBuild(String group) {
        return this.canGroupBuild(this.defaultWorld, group);
    }

    @Deprecated
    public String[] getGroups(String name) {
        return this.getGroups(this.defaultWorld, name);
    }

    @Deprecated
    public String getGroupPermissionString(String group, String permission) {
        return this.getGroupPermissionString(this.defaultWorld, group, permission);
    }

    @Deprecated
    public int getGroupPermissionInteger(String group, String permission) {
        return this.getGroupPermissionInteger(this.defaultWorld, group, permission);
    }

    @Deprecated
    public boolean getGroupPermissionBoolean(String group, String permission) {
        return this.getGroupPermissionBoolean(this.defaultWorld, group, permission);
    }

    @Deprecated
    public double getGroupPermissionDouble(String group, String permission) {
        return this.getGroupPermissionDouble(this.defaultWorld, group, permission);
    }

    @Deprecated
    public String getUserPermissionString(String name, String permission) {
        return this.getUserPermissionString(this.defaultWorld, name, permission);
    }

    @Deprecated
    public int getUserPermissionInteger(String name, String permission) {
        return this.getUserPermissionInteger(this.defaultWorld, name, permission);
    }

    @Deprecated
    public boolean getUserPermissionBoolean(String name, String permission) {
        return this.getUserPermissionBoolean(this.defaultWorld, name, permission);
    }

    @Deprecated
    public double getUserPermissionDouble(String name, String permission) {
        return this.getUserPermissionDouble(this.defaultWorld, name, permission);
    }

    @Deprecated
    public String getPermissionString(String name, String permission) {
        return this.getPermissionString(this.defaultWorld, name, permission);
    }

    @Deprecated
    public boolean getPermissionBoolean(String name, String permission) {
        return this.getPermissionBoolean(this.defaultWorld, name, permission);
    }

    @Deprecated
    public int getPermissionInteger(String name, String permission) {
        return this.getPermissionInteger(this.defaultWorld, name, permission);
    }

    @Deprecated
    public double getPermissionDouble(String name, String permission) {
        return this.getPermissionDouble(this.defaultWorld, name, permission);
    }
    
    @Deprecated
    public void removeUserInfo(String user, String node) {
        this.removeUserInfo(this.defaultWorld, user, node);
    }
    
    @Deprecated
    public void addGroupPermission(String group, String node) {
        this.addGroupPermission(this.defaultWorld, group, node);
    }
    
    @Deprecated
    public void removeGroupPermission(String group, String node) {
        this.removeGroupPermission(this.defaultWorld, group, node);
    }
    
    @Deprecated
    public void addGroupInfo(String group, String node, Object data) {
        this.addGroupInfo(this.defaultWorld, group, node, data);
    }
    
    @Deprecated
    public void removeGroupInfo(String group, String node) {
        this.removeGroupInfo(this.defaultWorld, group, node);
    }
    
    @Deprecated
    public void addUserPermission(String user, String node) {
        this.addUserPermission(this.defaultWorld, user, node);
    }
    
    @Deprecated
    public void removeUserPermission(String user, String node) {
        this.removeUserPermission(this.defaultWorld, user, node);
    }
    
    @Deprecated
    public void addUserInfo(String user, String node, Object data) {
        this.addUserInfo(this.defaultWorld, user, node, data);
    }
}
