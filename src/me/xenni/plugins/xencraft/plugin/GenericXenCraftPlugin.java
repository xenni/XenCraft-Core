package me.xenni.plugins.xencraft.plugin;

import me.xenni.plugins.xencraft.XenCraftCorePlugin;
import me.xenni.plugins.xencraft.util.XenCraftLogger;
import me.xenni.plugins.xencraft.util.XenCraftPluginData;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class GenericXenCraftPlugin extends JavaPlugin
{
    public static GenericXenCraftPlugin getXenCraftPlugin(PluginManager manager, String pluginname)
    {
        Plugin plugin = manager.getPlugin(pluginname);
        if (plugin == null || !(plugin instanceof GenericXenCraftPlugin))
        {
            return null;
        }

        return (GenericXenCraftPlugin)plugin;
    }
    public static <P extends GenericXenCraftPlugin> P getXenCraftPlugin(PluginManager manager, String pluginname, Class<P> pluginclass)
    {
        Plugin plugin = manager.getPlugin(pluginname);
        if (plugin == null || !(pluginclass.isInstance(plugin)))
        {
            return null;
        }
        else
        {
            return pluginclass.cast(plugin);
        }
    }
    public static <P extends GenericXenCraftPlugin> P connectToXenCraftPlugin(Plugin self, String pluginname, Class<P> pluginclass)
    {
        P instance = getXenCraftPlugin(self.getServer().getPluginManager(), pluginname, pluginclass);
        instance.registerDependent(self);
        return instance;
    }

    protected final ArrayList<Plugin> dependents = new ArrayList<Plugin>();
    protected XenCraftCorePlugin xenCraftCorePlugin;
    private Hashtable<String, Configuration> configCache = new Hashtable<String, Configuration>();
    private PluginManager pluginManager;
    private String pluginName;
    private XenCraftPluginData pluginData;

    protected abstract void onPluginEnable();
    protected void onPluginDisable()
    {
    }

    public void registerDependent(Plugin plugin)
    {
        if (!dependents.contains(plugin))
        {
            dependents.add(plugin);
        }
    }

    private Configuration getConfigFile(String path) throws IOException
    {
        File configfile = new File(path);
        File parent = configfile.getParentFile();
        if (!parent.exists())
        {
            log("Creating configuration data directory: '" + parent.getCanonicalPath() + "'...");
            if (parent.mkdirs())
            {
                log("Created configuration data directory.", 1);
            }
            else
            {
                log("Unable to create configuration data directory.", Level.SEVERE, 1);
                throw new IOException("Unable to create configuration data directory '" + parent.getCanonicalPath() + "'.");
            }
        }
        if (!configfile.exists())
        {
            log("Creating configuration file: '" + configfile.getCanonicalPath() + "'...");
            if(configfile.createNewFile())
            {
                log("Created configuration file.", 1);
            }
            else
            {
                log("Unable to create configuration file.", Level.SEVERE, 1);
                throw new IOException("Unable to create configuration file '" + parent.getCanonicalPath() + "'.");
            }
        }

        Configuration config = new Configuration(configfile);
        config.load();
        configCache.put(path, config);
        return config;
    }

    public Configuration getSharedConfiguration(String path) throws IOException
    {
        return getConfigFile(this.xenCraftCorePlugin.getDataFolder().getCanonicalPath() + "/Shared/" + path);
    }
    public Configuration tryGetSharedConfiguration(String path)
    {
        try
        {
            return getSharedConfiguration(path);
        }
        catch (IOException ex)
        {
            return null;
        }
    }

    public Configuration getConfiguration(String path) throws IOException
    {
        return getConfigFile(this.getDataFolder().getCanonicalPath() + "/" + path);
    }
    public Configuration tryGetConfiguration(String path)
    {
        try
        {
            return getConfiguration(path);
        }
        catch (IOException ex)
        {
            return null;
        }
    }
    public Configuration getConfiguration()
    {
        try
        {
            return getConfiguration("config.yml");
        }
        catch (IOException ex)
        {
            return super.getConfiguration();
        }
    }

    private Logger minecraftLogger;
    protected void logDisconnectedFromXenCraft(String message, Level level)
    {
        minecraftLogger.log(level, ("[" + pluginName + "] " + message));
    }

    protected XenCraftLogger logger;
    public XenCraftLogger getLogger()
    {
        return logger;
    }

    public XenCraftPluginData getPluginDataForPlayer(String playername) throws IOException
    {
        return xenCraftCorePlugin.getDataForPlayer(playername).getPluginData(this);
    }
    public XenCraftPluginData getPluginDataForPlayer(Player player) throws IOException
    {
        return xenCraftCorePlugin.getDataForPlayer(player).getPluginData(this);
    }

    public XenCraftPluginData getPluginData() throws IOException
    {
        if (pluginData != null)
        {
            return pluginData;
        }

        File dataFile = new File(xenCraftCorePlugin.getDataFolder().getCanonicalPath() + "/Plugins/Data/" + pluginName.toLowerCase() + ".xcpd");
        if (dataFile.exists())
        {
            try
            {
                ObjectInputStream in = new ObjectInputStream(new FileInputStream(dataFile));
                Object data = in.readObject();
                in.close();

                if (data instanceof XenCraftPluginData)
                {
                    pluginData = (XenCraftPluginData)data;
                    return pluginData;
                }
                else
                {
                    dataFile.delete();
                }
            }
            catch (ClassNotFoundException ex)
            {
                throw new Error("ClassNotFoundException occurred while loading plugin data instance. Mod or data file may be out of date.", ex);
            }
        }

        pluginData = new XenCraftPluginData(this);
        return pluginData;
    }

    protected void log(String message, Level level, int indent)
    {
        logger.log(message, level, indent);
    }
    protected void log(String message, Level level)
    {
        logger.log(message, level, 0);
    }
    protected void log(String message, int indent)
    {
        logger.log(message, Level.INFO, indent);
    }
    protected void log(String message)
    {
        logger.log(message, Level.INFO, 0);
    }

    public final void onLoad()
    {
        pluginName = this.getDescription().getName();
        minecraftLogger = Logger.getLogger("Minecraft");

        logDisconnectedFromXenCraft("Loaded version: '" + this.getDescription().getVersion() + "'", Level.INFO);
    }

    public final void onEnable()
    {
        pluginManager = this.getServer().getPluginManager();

        xenCraftCorePlugin = XenCraftCorePlugin.getInstance(pluginManager);
        if (xenCraftCorePlugin == null)
        {
            logDisconnectedFromXenCraft("Unable to connect to XenCraft Core!", Level.SEVERE);
            logDisconnectedFromXenCraft("Plugin will now be disabled.", Level.INFO);

            pluginManager.disablePlugin(this);

            return;
        }

        logger = new XenCraftLogger(xenCraftCorePlugin, pluginName);

        log("Connected to XenCraft Core: '" + xenCraftCorePlugin.getDescription().getVersion() + "'.");

        log("Enabling...");

        if (xenCraftCorePlugin.Dependents.contains(this))
        {
            log("Already registered with XenCraft Core.", Level.FINE, 1);
        }
        else
        {
            log("Registering with XenCraft Core...", Level.FINE, 1);
            xenCraftCorePlugin.Dependents.add(this);
            log("Completed registering with XenCraft Core.", Level.FINE, 2);
        }

        log("Handing-off to plugin-specific enabler...", Level.FINE, 1);
        onPluginEnable();

        log("Enabled.");
    }

    public final void onDisable()
    {
        if (xenCraftCorePlugin == null)
        {
            return;
        }

        log("Disabling...");

        log("Disabling dependant plugins:", 1);
        for (Plugin plugin : dependents)
        {
            log("Plugin '" + plugin.getDescription().getFullName() + "'", 2);
            pluginManager.disablePlugin(plugin);
        }

        log("Completed disabling dependant plugins.", 2);

        log("Handing-off to plugin-specific disabler...", Level.FINE, 1);
        onPluginDisable();

        if (pluginData != null)
        {
            log("Saving plugin data...", 1);
            try
            {
                File dataFile = new File(xenCraftCorePlugin.getDataFolder().getCanonicalPath() + "/Plugins/Data/" + pluginName.toLowerCase() + ".xcpd");
                File parent = dataFile.getParentFile();
                if (!parent.exists() && !parent.mkdirs())
                {
                    throw new IOException("Unable to create plugin data directory tree.");
                }
                if (!dataFile.exists() && !dataFile.createNewFile())
                {
                    throw new IOException("Unable to create plugin data file.");
                }

                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(dataFile, false));
                out.writeObject(pluginData);
                out.flush();
                out.close();

                log("Completed saving plugin data.", 2);
            }
            catch (IOException ex)
            {
                log("Unable to save plugin data: " + ex.getMessage(), Level.SEVERE, 2);
            }
        }

        log("Purging caches...", Level.FINER, 1);
        dependents.clear();
        configCache.clear();
        pluginData = null;
        log("Completed purging caches.", Level.FINER, 2);

        log("Disabled.");
    }
}
