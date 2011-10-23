package me.xenni.plugins.xencraft.util;

import java.util.ArrayList;

public final class CommandUtil
{
    public static String[] groupArgs(String[] args)
    {
        ArrayList<String> results = new ArrayList<String>();

        String context = null;
        StringBuilder current = null;

        for (int i = 0; i < args.length; i++)
        {
            if (context != null)
            {
                current.append(" " + args[i]);
                if (args[i].endsWith(context))
                {
                    if (context.equals("\""))
                    {
                        String str = current.toString();
                        results.add(str.substring(1, str.length() - 1));
                    }
                    else
                    {
                        results.add(current.toString());
                    }

                    current = null;
                    context = null;
                }
            }
            else if (args[i].startsWith("\""))
            {
                current = new StringBuilder(args[i]);
                context = "\"";
            }
            else if (args[i].startsWith("["))
            {
                current = new StringBuilder(args[i]);
                context = "]";
            }
            else
            {
                results.add(args[i]);
            }
        }

        String[] strs = new String[results.size()];
        for (int i = 0; i < results.size(); i++)
        {
            strs[i] = results.get(i);
        }
        return strs;
    }

    private CommandUtil()
    {
    }
}
