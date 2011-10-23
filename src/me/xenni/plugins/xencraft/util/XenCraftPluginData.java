package me.xenni.plugins.xencraft.util;

import me.xenni.plugins.xencraft.plugin.GenericXenCraftPlugin;

import java.io.Serializable;

public final class XenCraftPluginData implements Serializable
{
    public final String pluginName;
    public String pluginVersion;
    private Serializable data;

    public <T extends Serializable> void setData(T value)
    {
        data = value;
    }

    @SuppressWarnings("unchecked")
    public <T extends Serializable> T getData()
    {
        return (T)data;
    }

    public XenCraftPluginData(GenericXenCraftPlugin plugin)
    {
        pluginName = plugin.getDescription().getName();
        pluginVersion = plugin.getDescription().getVersion();
    }
}
