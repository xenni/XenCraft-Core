package me.xenni.plugins.xencraft;

import me.xenni.plugins.xencraft.plugin.GenericXenCraftPlugin;
import me.xenni.plugins.xencraft.util.ItemStackUtil;
import me.xenni.plugins.xencraft.util.XenCraftPlayerData;
import me.xenni.plugins.xencraft.util.XenCraftPluginData;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.event.Event;
import org.bukkit.event.player.PlayerListener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.logging.Level;

public final class XenCraftCorePlugin extends JavaPlugin
{
    public static XenCraftCorePlugin getInstance(PluginManager pluginManager)
    {
        Plugin plugin = pluginManager.getPlugin("XenCraftCore");
        if (plugin == null || !(plugin instanceof XenCraftCorePlugin))
        {
            return null;
        }
        else
        {
            return (XenCraftCorePlugin)plugin;
        }
    }

    protected static final class XenCraftCorePlayerListener extends PlayerListener
    {
        private XenCraftCorePlugin xenCraftCorePlugin;

        public XenCraftCorePlayerListener(XenCraftCorePlugin core)
        {
            xenCraftCorePlugin = core;
            xenCraftCorePlugin.log(null, "PlayerListener", Level.FINER, 3);

            PluginManager manager = xenCraftCorePlugin.getServer().getPluginManager();

            xenCraftCorePlugin.log(null, "PlayerLogin (High)", Level.FINEST, 4);
            manager.registerEvent(Event.Type.PLAYER_LOGIN, this, Event.Priority.High, xenCraftCorePlugin);
        }

        public void onPlayerLogin(PlayerLoginEvent event)
        {
            Player player = event.getPlayer();
            String playername = player.getName();

            if (xenCraftCorePlugin.getDataForPlayer(player) == null)
            {
                xenCraftCorePlugin.log(null, "Kicked player '" + playername + "': Player data provider reported a failure in loading data.", Level.SEVERE);
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "[XenCraft] Player data provider reported a failure in loading your data. Contact the server administrator for more details.");
            }
            else
            {
                xenCraftCorePlugin.log(null, "Loaded data for player '" + playername + "'.");
            }
        }
    }

    private Logger xcLogger;
    public final Hashtable<String, XenCraftPlayerData> LoadedConfigs = new Hashtable<String, XenCraftPlayerData>();
    public final ArrayList<Plugin> Dependents = new ArrayList<Plugin>();

    private PluginManager pluginManager;
    private XenCraftCorePlayerListener playerListener;

    public void log(String plugin, String message, Level level, int indent)
    {
        String prefix = "[XenCraft]";
        if (plugin != null)
        {
            prefix += ("[" + plugin + "]");
        }
        for (; indent >= 0; indent--)
        {
            prefix += " ";
        }

        xcLogger.log(level, (prefix + message));
    }

    public void log(String plugin, String message, Level level)
    {
        log(plugin, message, level, 0);
    }
    public void log(String plugin, String message, int indent)
    {
        log(plugin, message, Level.INFO, indent);
    }
    public void log(String plugin, String message)
    {
        log(plugin, message, Level.INFO, 0);
    }

    private void log(String message, Level level, int indent)
    {
        log(null, message, level, indent);
    }
    private void log(String message, Level level)
    {
        log(null, message, level, 0);
    }
    private void log(String message, int indent)
    {
        log(null, message, Level.INFO, indent);
    }
    private void log(String message)
    {
        log(null, message, Level.INFO, 0);
    }

    public XenCraftPlayerData getDataForPlayer(String playername)
    {
        try
        {
            return XenCraftPlayerData.GetForPlayer(this, playername);
        }
        catch (Exception ex)
        {
            log("Unable to get data for player '" + playername + "': " + ex.getMessage(), Level.SEVERE);

            return null;
        }
    }
    public XenCraftPlayerData getDataForPlayer(Player player)
    {
        try
        {
            return XenCraftPlayerData.GetForPlayer(this, player);
        }
        catch (Exception ex)
        {
            log("Unable to get data for player '" + player.getName() + "': " + ex.getMessage(), Level.SEVERE);

            return null;
        }
    }

    public void onLoad()
    {
        xcLogger = Logger.getLogger("Minecraft");
        log("Loaded version: '" + this.getDescription().getVersion() + "'", 0);
    }

    public void onEnable()
    {
        log("Enabling...");

        log("Pre-initializing...", Level.FINE, 1);
        pluginManager = this.getServer().getPluginManager();
        ItemStackUtil.initCleanNameCache();
        log("Completed pre-initializing.", Level.FINE, 2);

        log("Loading material aliases:", 1);

        int aliascount = 0;
        int datacount = 0;
        try
        {
            Configuration aliases = new Configuration(new File(getDataFolder().getCanonicalPath() + "/aliases.yml"));
            aliases.load();

            for (ConfigurationNode itemalias : aliases.getNodeList("itemaliases", null))
            {
                Material item;
                String matname = itemalias.getString("itemname");
                if (matname == null)
                {
                    int matid = itemalias.getInt("itemid", -1);
                    if (matid == -1)
                    {
                        log("Unable to determine material identity.", Level.WARNING, 2);
                        continue;
                    }
                    item = Material.getMaterial(matid);
                    if (item == null)
                    {
                        log("Unable to determine identity of material #" + matid + ".", Level.WARNING, 2);
                        continue;
                    }
                }
                else
                {
                    item = Material.matchMaterial(matname);
                    if (item == null)
                    {
                        log("Unable to determine identity of material '" + matname + "'.", Level.WARNING, 2);
                        continue;
                    }
                }
                log("Loading Material alias '" + item.name() + "':", Level.FINE, 2);
                aliascount++;

                String prefname = itemalias.getString("name");
                if (prefname != null)
                {
                    ItemStackUtil.setPreferredMaterialName(item.getId(), prefname);
                    log("Preferred name: '" + prefname + "'", Level.FINER, 3);
                }

                for (String name : itemalias.getStringList("aliases", null))
                {
                    ItemStackUtil.addMaterialAlias(item.getId(), name);
                    log("Alias: '" + name + "'", Level.FINER, 3);
                }
            }
            log("Completed loading aliases for " + aliascount + " items.", 2);

            for (ConfigurationNode dataalias : aliases.getNodeList("dataaliases", null))
            {
                Material item;
                String matname = dataalias.getString("itemname");
                if (matname == null)
                {
                    int matid = dataalias.getInt("itemid", -1);
                    if (matid == -1)
                    {
                        log("Unable to determine material identity.", Level.WARNING, 2);
                        continue;
                    }
                    item = Material.getMaterial(matid);
                    if (item == null)
                    {
                        log("Unable to determine identity of material #" + matid + ".", Level.WARNING, 2);
                        continue;
                    }
                }
                else
                {
                    item = Material.matchMaterial(matname);
                    if (item == null)
                    {
                        log("Unable to determine identity of material '" + matname + "'.", Level.WARNING, 2);
                        continue;
                    }
                }
                log("Loading data alias for material '" + item.name() + "':", Level.FINE, 2);

                for (ConfigurationNode datavalue : dataalias.getNodeList("values", null))
                {
                    byte dataval = (byte)datavalue.getInt("data", -1);
                    if (dataval < 0)
                    {
                        log("Unable to parse configuration node 'data' for material '" + item.name() + "'.", Level.WARNING, 2);
                        continue;
                    }

                    log("Data value " + dataval + ":", Level.FINER, 3);
                    datacount++;

                    String prefname = datavalue.getString("name");
                    if (prefname != null)
                    {
                        ItemStackUtil.setDataName(item.getId(), dataval, prefname);
                        log("Preferred name: '" + prefname + "'", Level.FINEST, 4);
                    }
                    for (String name : datavalue.getStringList("aliases", null))
                    {
                        ItemStackUtil.addDataAlias(item.getId(), dataval, name);
                        log("Alias: '" + name + "'", Level.FINEST, 4);
                    }
                }
            }
            log("Completed loading aliases for " + datacount + " data values.", 2);
        }
        catch (IOException ex)
        {
            log("Unable to load aliases: " + ex.getMessage(), Level.WARNING, 2);
        }

        log("Loading player configurations:", 1);
        for (String playername : XenCraftPlayerData.getExistingPlayers(this))
        {
            log("Loading '" + playername + "'.", 2);
            XenCraftPlayerData data = getDataForPlayer(playername);
            if (data == null)
            {
                log("Player data provider reported a failure in loading data. (See above for possible error details.)", Level.SEVERE, 3);
                log("Player '" + playername + "' will be unable to login.", Level.WARNING, 3);
                log("This issue may be resolved by deleting '" + playername + ".xcp', but this will erase all of \"" + playername + "\"'s data.", Level.INFO, 3);
            }
        }
        log("Completed loading " + LoadedConfigs.size() + " players.", 2);

        log("Post-initializing...", Level.FINE, 1);
        log("Registering listeners:", Level.FINER, 2);
        playerListener = new XenCraftCorePlayerListener(this);
        log("Completed registering listeners.", Level.FINER, 3);
        log("Completed post-initializing.", Level.FINE, 2);

        log("Enabled.");
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if (label.equals("xcplugins"))
        {
            if (args.length != 0)
            {
                return false;
            }

            if (sender.hasPermission("xencraft.admin.listplugins"))
            {
                Plugin[] loadedplugins = pluginManager.getPlugins();
                int plugincount = 0;

                sender.sendMessage("[XenCraft] Loaded XenCraft plugins:");
                for (Plugin loadedplugin : loadedplugins)
                {
                    if (loadedplugin instanceof GenericXenCraftPlugin)
                    {
                        PluginDescriptionFile descr = loadedplugin.getDescription();
                        sender.sendMessage("    " + descr.getName() + " (" + descr.getVersion() + ")");
                        plugincount++;
                    }
                }
                sender.sendMessage("  (" + plugincount + " Total)");
            }
            else
            {
                sender.sendMessage("[XenCraft] You do not have permission to do that.");
            }

            return true;
        }
        else if (label.equals("xcresetplugin"))
        {
            if (args.length == 0 || args.length > 1)
            {
                return false;
            }

            if (sender.hasPermission("xencraft.admin.resetplugin"))
            {
                GenericXenCraftPlugin plugin = GenericXenCraftPlugin.getXenCraftPlugin(pluginManager, args[0]);
                if (plugin == null)
                {
                    sender.sendMessage("[XenCraft] Plugin '" + args[0] + "' could not be found.");
                    return true;
                }

                boolean enabled = pluginManager.isPluginEnabled(plugin);
                if (enabled)
                {
                    pluginManager.disablePlugin(plugin);
                }

                try
                {
                    plugin.getPluginData().setData(null);
                }
                catch (IOException ex)
                {
                    sender.sendMessage("[XenCraft] Unable to perform operation: " + ex.getMessage());
                }
                sender.sendMessage("[XenCraft] Data for plugin '" + args[0] + "' successfully purged.");

                if (enabled)
                {
                    pluginManager.enablePlugin(plugin);
                }
            }
            else
            {
                sender.sendMessage("[XenCraft] You do not have permission to do that.");
            }

            return true;
        }
        else if (label.equals("xcresetplayer"))
        {
            if (args.length == 0 || args.length > 2)
            {
                return false;
            }

            if (sender.hasPermission("xencraft.admin.resetplayer"))
            {
                Player player = getServer().getPlayer(args[0]);
                if (player == null)
                {
                    sender.sendMessage("[XenCraft] Could not find player '" + args[0] + "'.");
                    return true;
                }

                if (sender != player && player.hasPermission("xencraft.admin.blockreset") && !sender.hasPermission("xencraft.admin.bypassblockreset"))
                {
                    sender.sendMessage("[XenCraft] You do not have permission to do that to '" + player.getDisplayName() + "'.");
                    return true;
                }

                ArrayList<GenericXenCraftPlugin> plugins = new ArrayList<GenericXenCraftPlugin>();
                if (args.length > 1)
                {
                    if (args[1].equals("*"))
                    {
                        XenCraftPlayerData data = getDataForPlayer(player);
                        if (data == null)
                        {
                            sender.sendMessage("[XenCraft] Global data for player " + player.getName() + " could not be found.");
                        }
                        else
                        {
                            data.clearPluginData();
                            sender.sendMessage("[XenCraft] Global data for player " + player.getName() + " successfully reset.");
                            sender.sendMessage("[XenCraft] You may need to restart the server for the effects to take place.");
                        }
                        return true;
                    }

                    GenericXenCraftPlugin plugin = GenericXenCraftPlugin.getXenCraftPlugin(pluginManager, args[1]);
                    if (plugin == null)
                    {
                        sender.sendMessage("[XenCraft] Plugin '" + args[1] + "' could not be found.");
                        return true;
                    }
                    plugins.add(plugin);
                }
                else
                {
                    Plugin[] loadedplugins = pluginManager.getPlugins();
                    for (Plugin loadedplugin : loadedplugins)
                    {
                        if (loadedplugin instanceof GenericXenCraftPlugin)
                        {
                            plugins.add((GenericXenCraftPlugin) loadedplugin);
                        }
                    }
                    if (plugins.size() == 0)
                    {
                        sender.sendMessage("[XenCraft] No plugins found.");
                        return true;
                    }
                }

                for (GenericXenCraftPlugin plugin : plugins)
                {
                    String pluginName = plugin.getDescription().getName();
                    try
                    {
                        XenCraftPluginData data = plugin.getPluginDataForPlayer(args[0]);
                        if (data == null)
                        {
                            continue;
                        }

                        boolean enabled = pluginManager.isPluginEnabled(plugin);

                        if (enabled)
                        {
                            pluginManager.disablePlugin(plugin);
                        }
                        data.setData(null);
                        if (enabled)
                        {
                            pluginManager.enablePlugin(plugin);
                        }
                        sender.sendMessage("[XenCraft] Successfully purged data for plugin '" + pluginName + "'.");
                    }
                    catch (IOException ex)
                    {
                        sender.sendMessage("[XenCraft] Unable to perform operation: " + ex.getMessage());
                    }
                }
            }
            else
            {
                sender.sendMessage("[XenCraft] You do not have permission to do that.");
            }

            return true;
        }

        return false;
    }

    public void onDisable()
    {
        log("Disabling...");

        log("Disabling dependant plugins:", 1);
        for (Plugin plugin : Dependents)
        {
            log("Plugin '" + plugin.getDescription().getFullName() + "'", 2);
            pluginManager.disablePlugin(plugin);
        }
        log("Completed disabling dependant plugins.", 2);

        log("Saving player configurations:", 1);
        for (Entry<String,XenCraftPlayerData> playerData : LoadedConfigs.entrySet())
        {
            log("Saving '" + playerData.getKey() + "'", 2);
            try
            {
                playerData.getValue().save();
            }
            catch (Exception ex)
            {
                log("Error: " + ex.getMessage(), Level.SEVERE, 3);
                log("\"" + playerData.getKey() + "\"'s data may be out of date on reload.", Level.WARNING, 3);
            }
        }
        log("Completed saving " + LoadedConfigs.size() + " players.", 2);

        LoadedConfigs.clear();
        Dependents.clear();
        ItemStackUtil.clearAliases();

        log("Disabled.");
    }
}
