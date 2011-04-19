package com.nijiko.configuration;

import org.bukkit.util.config.Configuration;

/**
 * Handles default configuration and loads data.
 * 
 * @author Nijiko
 */
public class ConfigurationHandler extends DefaultConfiguration {
    private Configuration config;

    public ConfigurationHandler(Configuration config) {
        this.config = config;
    }

    public void load() {
        this.permissionSystem = this.config.getString("plugin.permissions.system", this.permissionSystem);
    }
}
