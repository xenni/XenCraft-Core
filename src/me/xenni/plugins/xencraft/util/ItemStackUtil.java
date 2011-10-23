package me.xenni.plugins.xencraft.util;

import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.*;

import com.google.code.regexp.NamedPattern;
import com.google.code.regexp.NamedMatcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;

public final class ItemStackUtil
{
    //[<AMOUNT> ]<BLOCK NAME|ID>[:<DATA NAME|ID>][ <DURABILITY>%]
    private static NamedPattern regexItemStack = NamedPattern.compile("^((?<amt>-?\\d+) )?(?<mat>(\\d+)|([a-zA-Z_]+))(\\:(?<dat>(\\d+)|([a-zA-Z_]+)))?( (?<dmg>((\\d+(\\.\\d+)?)%)|(@\\d+)))?$");
    private static NamedMatcher matcherItemStack;


    private static Hashtable<Integer, Hashtable<String, Byte>> dataNames = new Hashtable<Integer, Hashtable<String, Byte>>();
    private static Hashtable<Integer, Hashtable<Byte, String>> dataValues = new Hashtable<Integer, Hashtable<Byte, String>>();

    private static Hashtable<String, Integer> dataAliases = new Hashtable<String, Integer>();
    private static Hashtable<Integer, String> preferredNames = new Hashtable<Integer, String>();
    private static Hashtable<String, Material> cleanNameCache = null;

    public static void initCleanNameCache()
    {
        if (cleanNameCache == null)
        {
            Material[] all = Material.values();
            cleanNameCache = new Hashtable<String, Material>(all.length);

            for (int i = 0; i < all.length; i++)
            {
                if (all[i].name().contains("_"))
                {
                    cleanNameCache.put(all[i].name().toLowerCase().replace("_", ""), all[i]);
                }
            }
        }
    }
    public static void setPreferredMaterialName(Integer material, String name)
    {
        preferredNames.put(material, name);
        addMaterialAlias(material, name);
    }
    public static void addMaterialAlias(Integer material, String alias)
    {
        String lalias = alias.toLowerCase();
        dataAliases.put(lalias, material);
        if (lalias.contains("_"))
        {
            dataAliases.put(lalias.replace("_", ""), material);
        }
    }
    public static void setDataName(Integer material, Byte data, String alias)
    {
        if (!dataValues.contains(material))
        {
            dataValues.put(material, new Hashtable<Byte, String>(1));
        }

        dataValues.get(material).put(data, alias.replace("_", ""));

        addDataAlias(material, data, alias);
    }
    public static void addDataAlias(Integer material, Byte data, String alias)
    {
        if (!dataNames.containsKey(material))
        {
            dataNames.put(material, new Hashtable<String, Byte>(2));
        }

        String lalias = alias.toLowerCase();

        dataNames.get(material).put(lalias, data);
        if (lalias.contains("_"))
        {
            dataNames.get(material).put(lalias.replace("_", ""), data);
        }
    }

    public static void clearAliases()
    {
        dataAliases.clear();
        preferredNames.clear();
        dataNames.clear();
        dataValues.clear();
    }

    public static ItemStack parse(String rep)
    {
        if (rep.equals("(nothing)"))
        {
            return new ItemStack(0, 0);
        }

        if (matcherItemStack == null)
        {
            matcherItemStack = regexItemStack.matcher(rep);
        }
        else
        {
            matcherItemStack.reset(rep);
        }

        NamedMatcher matcher = regexItemStack.matcher(rep);
        if (!matcher.find())
        {
            return null;
        }

        Material material;
        if (matcher.group("mat") == null)
        {
            return null;
        }

        int matid;
        try
        {
            matid = Integer.parseInt(matcher.group("id"));
        }
        catch (NumberFormatException ex)
        {
            matid = -1;
        }

        material = (matid == -1 ? matchMaterial(matcher.group("mat")) : Material.getMaterial(matid));
        if (material == null)
        {
            return null;
        }

        int amount = 1;
        if (matcher.group("amt") != null)
        {
            amount = Integer.parseInt(matcher.group("amt"));
        }

        short durability = 0;
        if (matcher.group("dmg") != null)
        {
            if (material.isBlock())
            {
                return null;
            }

            String dmgval = matcher.group("dmg");
            if (dmgval.startsWith("@"))
            {
                durability = Short.parseShort(dmgval.substring(1));
                if (durability > material.getMaxDurability() || durability < 0)
                {
                    return null;
                }
            }
            else if (dmgval.endsWith("%"))
            {
                float factor = (Float.parseFloat(dmgval.substring(0, dmgval.length() - 1)) / 100.0f);
                if (factor > 1 || factor < 0)
                {
                    return null;
                }

                durability = (short)(material.getMaxDurability() - (short)Math.floor(((float)material.getMaxDurability()) * factor));
            }
            else
            {
                return null;
            }
        }

        Byte data = 0;
        if (matcher.group("dat") != null)
        {
            try
            {
                data = Byte.parseByte(matcher.group("dat"));
            }
            catch (NumberFormatException ex)
            {
                data = parseDataName(material.getId(), matcher.group("dat"));
            }
            if (data == null)
            {
                return null;
            }
        }

        ItemStack stack = new ItemStack(material, amount, durability, data);
        if (durability != 0)
        {
            stack.setDurability(durability);
        }

        return stack;
    }

    public static Byte parseDataName(Integer material, String rep)
    {
        return (dataNames.containsKey(material) ? dataNames.get(material).get(rep.toLowerCase()) : null);
    }

    public static Material matchMaterial(String name)
    {
        String lname = name.toLowerCase();

        if (dataAliases.containsKey(lname))
        {
            return Material.getMaterial(dataAliases.get(lname));
        }

        Material material = Material.matchMaterial(name);

        if (material == null)
        {
            String cname = lname.replace("_", "");

            if (dataAliases.contains(cname))
            {
                return Material.getMaterial(dataAliases.get(cname));
            }

            if (cleanNameCache.containsKey(cname))
            {
                return cleanNameCache.get(cname);
            }
        }

        return material;
    }

    public static String toString(Material material)
    {
        if (preferredNames.containsKey(material.getId()))
        {
            return preferredNames.get(material.getId()).replace("_", "");
        }

        return material.name().replace("_", "").toLowerCase();
    }

    public static String toString(MaterialData data)
    {
        if (dataValues.containsKey(data.getItemTypeId()))
        {
            Hashtable<Byte, String> entries = dataValues.get(data.getItemTypeId());
            if (entries.containsKey(data.getData()))
            {
                return entries.get(data.getData());
            }
        }

        if (data instanceof Cake)
        {
            return Integer.toString(((Cake)data).getSlicesEaten());
        }
        if (data instanceof Coal)
        {
            return ((Coal)data).getType().name();
        }
        if (data instanceof Colorable)
        {
            return ((Colorable)data).getColor().name();
        }
        if (data instanceof LongGrass)
        {
            return ((LongGrass)data).getSpecies().name();
        }
        if (data instanceof Step)
        {
            return ((Step)data).getMaterial().name();
        }
        if (data instanceof Tree)
        {
            return ((Tree)data).getSpecies().name();
        }

        return null;
    }

    public static String toString(ItemStack stack)
    {
        int amount = stack.getAmount();
        if (amount == 0)
        {
            return "(nothing)";
        }

        Material material = stack.getType();
        MaterialData data = stack.getData();
        byte dataData = (data == null ? 0 : data.getData());
        String dataName = ((dataData != 0) ? toString(data) : null);

        if (material.isBlock())
        {
            return (
                (amount == 1 ? "" : (amount + " ")) +
                (
                    dataName == null ?
                    (dataData == 0 ?
                        toString(material) :
                        (toString(material) + ":" + dataData)
                    ) :
                    (toString(material) + ":" + dataName)
                )
            );
        }
        else
        {
            short damage = stack.getDurability();
            int dmgfactor = (int)Math.round((((double)(material.getMaxDurability() - damage)) / ((double)material.getMaxDurability())) * 100.0);
            if (dmgfactor > 100)
            {
                damage = material.getMaxDurability();
            }

            return (
                (amount == 1 ? "" : (amount + " ")) +
                (
                    dataName == null ?
                    (dataData == 0 ?
                        toString(material) :
                        (toString(material) + ":" + dataData)
                    ) :
                    (toString(material) + ":" + dataName)
                ) +
                ((damage == 0 || damage == material.getMaxDurability()) ? "" : (" " + dmgfactor + "%"))
            );
        }
    }

    public enum ItemStackCoalesceMode
    {
        ONE_STACK,
        STACKS_OF_64,
        STACKS_OF_MATERIAL_MAX_STACK_SIZE
    }

    private static final class ItemStackInfo
    {
        public int material;
        public byte data;
        public short durability;
        public int amount;

        public ItemStackInfo(ItemStack stack)
        {
            material = stack.getTypeId();
            durability = stack.getDurability();
            amount = stack.getAmount();

            MaterialData datainstance = stack.getData();
            data = (datainstance == null ? 0 : datainstance.getData());
        }

        public ItemStack toStack()
        {
            return createItemStack(material, data, durability, amount);
        }

        public static ItemStack createItemStack(int material, byte data, short durability, int amount)
        {
            ItemStack stack = new ItemStack(material, amount, (short)0, data);
            stack.setDurability(durability);

            return stack;
        }
    }

    public static ItemStack shallowClone(ItemStack stack, int amount)
    {
        ItemStack copy = new ItemStack(stack.getTypeId());
        copy.setAmount(amount);
        copy.setDurability(stack.getDurability());
        MaterialData data = stack.getData();
        if (data != null)
        {
            copy.setData(data);
        }

        return copy;
    }

    public static ItemStack shallowClone(ItemStack stack)
    {
        ItemStack copy = new ItemStack(stack.getTypeId());
        copy.setAmount(stack.getAmount());
        copy.setDurability(stack.getDurability());
        MaterialData data = stack.getData();
        if (data != null)
        {
            copy.setData(data);
        }

        return copy;
    }

    public static ArrayList<ItemStack> coalesce(Collection<ItemStack> value, ItemStackCoalesceMode coalesceMode, boolean keepEmptyStacks)
    {
        if (value == null)
        {
            return new ArrayList<ItemStack>(0);
        }

        ArrayList<ItemStackInfo> infos = new ArrayList<ItemStackInfo>();
        for(ItemStack stack : value)
        {
            if (stack == null)
            {
                continue;
            }

            boolean notfound = true;
            for (ItemStackInfo info : infos)
            {
                if (
                    info.material == stack.getTypeId() &&
                    info.data == (stack.getData() == null ? 0 : stack.getData().getData()) &&
                    info.durability == stack.getDurability()
                )
                {
                    notfound = false;
                    info.amount += stack.getAmount();
                    break;
                }
            }
            if (notfound)
            {
                infos.add(new ItemStackInfo(stack));
            }
        }

        ArrayList<ItemStack> stacks = new ArrayList<ItemStack>();
        for (ItemStackInfo info : infos)
        {
            if (!keepEmptyStacks && info.amount == 0)
            {
                continue;
            }

            if (coalesceMode == ItemStackCoalesceMode.ONE_STACK || info.amount <= 1)
            {
                stacks.add(info.toStack());
            }
            else
            {
                int stacksize = (coalesceMode == ItemStackCoalesceMode.STACKS_OF_64 ? 64 : Material.getMaterial(info.material).getMaxStackSize());

                for (; info.amount >= stacksize; info.amount -= stacksize)
                {
                    stacks.add(ItemStackInfo.createItemStack(info.material, info.data, info.durability, stacksize));
                }
                if (info.amount > 0)
                {
                    stacks.add(info.toStack());
                }
            }
        }

        return stacks;
    }

    public static boolean isTypeEquivalent(ItemStack a, ItemStack b)
    {
        if (a == null || b == null)
        {
            return false;
        }

        if (a.getTypeId() != b.getTypeId())
        {
            return false;
        }
        if (a.getData() != null || b.getData() != null)
        {
            if (a.getData() == null || b.getData() == null)
            {
                return false;
            }

            if (a.getData().getData() != b.getData().getData())
            {
                return false;
            }
        }
        if (a.getDurability() != b.getDurability())
        {
            return false;
        }

        return true;
    }

    public static boolean removeFromInventory(Inventory inventory, ItemStack items)
    {
        if (!inventoryContains(inventory, items))
        {
            return false;
        }

        ItemStack[] contents = inventory.getContents();
        int left = items.getAmount();
        for (int i = 0; (i < contents.length && left > 0); i++)
        {
            if (isTypeEquivalent(items, contents[i]))
            {
                if (left >= contents[i].getAmount())
                {
                    left -= contents[i].getAmount();
                    inventory.remove(contents[i]);
                }
                else
                {
                    contents[i].setAmount(contents[i].getAmount() - left);
                }
            }
        }

        return (left == 0);
    }

    public static boolean inventoryContains(Inventory inventory, ItemStack items)
    {
        ItemStack[] acontents = inventory.getContents();
        ArrayList<ItemStack> contents = new ArrayList<ItemStack>(acontents.length);
        for (int i = 0; i < acontents.length; i++)
        {
            contents.add(acontents[i]);
        }

        contents = coalesce(contents, ItemStackCoalesceMode.ONE_STACK, false);
        for (ItemStack content : contents)
        {
            if (isTypeEquivalent(items, content) && items.getAmount() <= content.getAmount())
            {
                return true;
            }
        }

        return false;
    }

    private ItemStackUtil() { }
}