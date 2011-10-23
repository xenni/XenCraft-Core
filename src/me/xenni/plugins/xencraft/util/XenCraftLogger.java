package me.xenni.plugins.xencraft.util;

import me.xenni.plugins.xencraft.XenCraftCorePlugin;

import java.util.logging.Level;

public final class XenCraftLogger
{
    public final XenCraftCorePlugin corePlugin;
    public final String pluginName;

    public XenCraftLogger(XenCraftCorePlugin plugin, String name)
    {
        corePlugin = plugin;
        pluginName = name;
    }

    public XenCraftLogger getForPluginName(String name)
    {
        return new XenCraftLogger(corePlugin, name);
    }

    public void log(String message, Level level, int indent)
    {
        corePlugin.log(pluginName, message, level, indent);
    }
    public void log(String message, Level level)
    {
        corePlugin.log(pluginName, message, level, 0);
    }
    public void log(String message, int indent)
    {
        corePlugin.log(pluginName, message, Level.INFO, indent);
    }
    public void log(String message)
    {
        corePlugin.log(pluginName, message, Level.INFO, 0);
    }
}
