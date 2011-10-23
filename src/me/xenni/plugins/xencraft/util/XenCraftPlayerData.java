package me.xenni.plugins.xencraft.util;

import me.xenni.plugins.xencraft.XenCraftCorePlugin;
import me.xenni.plugins.xencraft.plugin.GenericXenCraftPlugin;
import org.bukkit.entity.Player;
import org.bukkit.util.config.Configuration;

import java.io.*;
import java.util.ArrayList;
import java.util.logging.Level;

public final class XenCraftPlayerData
{
    public static ArrayList<String> getExistingPlayers(XenCraftCorePlugin core)
    {
        ArrayList<String> results = new ArrayList<String>();

        try
        {
            String playerdatapath = (core.getDataFolder().getCanonicalPath() + "/Players/");
            File playerdatadir = new File(playerdatapath);
            if (!playerdatadir.exists())
            {
                core.log(null, "Creating player configuration data directory: '" + playerdatapath + "'...");
                if (playerdatadir.mkdirs())
                {
                    core.log(null, "Successfully created player configuration data directory.", Level.INFO, 1);
                    return results;
                }
                else
                {
                    core.log(null, "Unable to create player configuration data directory.", Level.SEVERE, 1);
                    core.log(null, "Fatal Error: Initiating Disable...", Level.SEVERE);
                    core.getPluginLoader().disablePlugin(core);
                    return null;
                }
            }

            for (String playerfile : playerdatadir.list(
                    new FilenameFilter() {
                        public boolean accept(File dir, String name) {
                            return name.endsWith(".xcp");
                        }
                    }
            ))
            {
                results.add(playerfile.substring(0, (playerfile.length() - 4)).toLowerCase());
            }
        }
        catch (IOException ex)
        {
        }

        return results;
    }

    private XenCraftCorePlugin xenCraftCorePlugin;
    private File configFile;
    private Player player;
    public final String playerName;
    public Configuration config;
    private ArrayList<XenCraftPluginData> pluginData = new ArrayList<XenCraftPluginData>();
    private String pluginDataStorePath;
    private File pluginDataStoreDir;

    public Player getPlayer()
    {
        if (player == null)
        {
            player = xenCraftCorePlugin.getServer().getPlayer(playerName);
        }
        return player;
    }

    private void initConfigFile() throws IOException
    {
        String configPath = (xenCraftCorePlugin.getDataFolder().getCanonicalPath() + "/Players/" + playerName + ".xcp");

        configFile = new File(configPath);
        File parent = configFile.getParentFile();
        if (!parent.exists())
        {
            if (!parent.mkdirs())
            {
                throw new IOException("Unable to create directory tree for file '" + configPath + "'.");
            }
        }
        if (!configFile.exists())
        {
            if (!configFile.createNewFile())
            {
                throw new IOException("Unable to create file '" + configPath + "'.");
            }
        }

        config = new Configuration(configFile);
        config.load();
    }
    private void initPlayerPluginData() throws IOException
    {
        pluginDataStorePath = (xenCraftCorePlugin.getDataFolder().getCanonicalPath() + "/Players/Data/" + playerName.toLowerCase() + "/");
        pluginDataStoreDir = new File(pluginDataStorePath);
    }

    private XenCraftPlayerData(XenCraftCorePlugin core, String name) throws Exception
    {
        xenCraftCorePlugin = core;
        playerName = name;

        initConfigFile();
        initPlayerPluginData();
    }

    private XenCraftPlayerData(XenCraftCorePlugin core, Player playerobj) throws Exception
    {
        xenCraftCorePlugin = core;
        player = playerobj;
        playerName = player.getName();

        initConfigFile();
        initPlayerPluginData();
    }

    public boolean hasPluginData(String pluginname)
    {
        pluginname = pluginname.toLowerCase();

        for (XenCraftPluginData data : pluginData)
        {
            if (data.pluginName.equals(pluginname))
            {
                return true;
            }
        }

        return (
            pluginDataStoreDir.exists() &&
            new File(pluginDataStoreDir + pluginname + ".xcppd").exists()
        );
    }

    public boolean hasPluginData(GenericXenCraftPlugin plugin) throws IOException
    {
        return hasPluginData(plugin.getPluginData().pluginName);
    }

    public XenCraftPluginData getPluginData(GenericXenCraftPlugin plugin) throws IOException
    {
        String pluginname = plugin.getDescription().getName().toLowerCase();

        for (XenCraftPluginData data : pluginData)
        {
            if (data.pluginName.equals(pluginname))
            {
                return data;
            }
        }

        if (pluginDataStoreDir.exists())
        {
            File dataFile = new File(pluginDataStoreDir + "/" + pluginname + ".xcppd");
            if (dataFile.exists())
            {
                try
                {
                    ObjectInputStream in = new ObjectInputStream(new FileInputStream(dataFile));
                    Object data = in.readObject();
                    in.close();

                    if (data instanceof XenCraftPluginData)
                    {
                        XenCraftPluginData dataInstance = (XenCraftPluginData)data;
                        pluginData.add(dataInstance);
                        return dataInstance;
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
        }

        XenCraftPluginData data = new XenCraftPluginData(plugin);
        pluginData.add(data);
        return data;
    }

    public void clearPluginData()
    {
        pluginData.clear();
    }

    public void save() throws IOException
    {
        if (!config.save())
        {
            throw new IOException("Unable to save yaml configuration file.");
        }

        if (pluginData.size() > 0)
        {
            String dataStoragePath = (xenCraftCorePlugin.getDataFolder().getCanonicalPath() + "/Players/Data/" + playerName.toLowerCase() + "/");

            File dataStorge = new File(dataStoragePath);
            if (!dataStorge.exists() && !dataStorge.mkdirs())
            {
                throw new IOException("Unable to create player data directory tree.");
            }

            ArrayList<XenCraftPluginData> pluginDataCopy = new ArrayList<XenCraftPluginData>(pluginData);

            for (File item : dataStorge.listFiles(
                new FilenameFilter()
                {
                    public boolean accept(File dir, String name)
                    {
                        return name.endsWith(".xcppd");
                    }
                }
            ))
            {
                String fileName = item.getName();
                String filePluginName = fileName.substring(0, (fileName.length() - 6)).toLowerCase();
                XenCraftPluginData data = null;
                for (XenCraftPluginData dataitem : pluginDataCopy)
                {
                    if (dataitem.pluginName.equals(filePluginName))
                    {
                        data = dataitem;
                        if (data == null)
                        {
                            item.delete();
                        }
                        else
                        {
                            ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(item, false));
                            out.writeObject(data);
                            out.flush();
                            out.close();
                        }
                        break;
                    }
                }
                if (data != null)
                {
                    pluginData.remove(data);
                }
            }

            for (XenCraftPluginData data : pluginDataCopy)
            {
                File outfile = new File(dataStoragePath + data.pluginName + ".xcppd");
                if (!outfile.createNewFile())
                {
                    throw new IOException("Unable to create plugin player data file for plugin '" + data.pluginName + "'.");
                }

                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(outfile, false));
                out.writeObject(data);
                out.flush();
                out.close();
            }

            pluginDataCopy.clear();
        }
    }

    public void reload()
    {
        config.load();

        pluginData.clear();
    }

    public static XenCraftPlayerData GetForPlayer(XenCraftCorePlugin core, String name) throws Exception
    {
        name = name.toLowerCase();

        if (core.LoadedConfigs.containsKey(name))
        {
            return core.LoadedConfigs.get(name);
        }
        else
        {
            XenCraftPlayerData data = new XenCraftPlayerData(core, name);
            core.LoadedConfigs.put(name, data);
            return data;
        }
    }

    public static XenCraftPlayerData GetForPlayer(XenCraftCorePlugin core, Player player) throws Exception
    {
        String name = player.getName().toLowerCase();

        if (core.LoadedConfigs.containsKey(name))
        {
            return core.LoadedConfigs.get(name);
        }
        else
        {
            XenCraftPlayerData data = new XenCraftPlayerData(core, player);
            core.LoadedConfigs.put(name, data);
            return data;
        }
    }
}
