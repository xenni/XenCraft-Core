package me.xenni.plugins.xencraft.util;

import org.xeustechnologies.jcl.JarClassLoader;
import org.xeustechnologies.jcl.JclObjectFactory;

public final class ModuleUtil
{
    @SuppressWarnings("unchecked")
    public static <T> T loadClassFromJar(String name, String jar)
    {
        JarClassLoader jcl = new JarClassLoader();
        JclObjectFactory factory = JclObjectFactory.getInstance(true);

        try
        {
            jcl.add(jar);
            return (T)factory.create(jcl, name);
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T loadClass(String name)
    {
        try
        {
            Class<?> classinstance = ModuleUtil.class.getClassLoader().loadClass(name);
            return (T)classinstance.newInstance();
        }
        catch (Exception ex)
        {
            return null;
        }
    }

    private ModuleUtil()
    {
    }
}
