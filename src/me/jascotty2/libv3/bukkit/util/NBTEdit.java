package me.jascotty2.libv3.bukkit.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.inventory.ItemStack;

public class NBTEdit {

	public static ItemStack setFromJson(ItemStack item, String json) {
		if (!enabled) {
			return item;
		}
		if (item != null) {
			try {
				Object stack = asNMSCopy(item);
				Object tag = loadNBTTagCompound(json);
				itemStackSetTag(stack, tag);
				return asBukkitCopy(stack);
			} catch (Exception e) {
				Bukkit.getLogger().log(Level.WARNING, "NBT Edit Error", e);
			}
		}
		return null;
	}

	private static final Map<String, Class<?>> classCache = new HashMap();
	private static final Map<String, Method> methodCache = new HashMap();
	private final static String serverPackagePath = Bukkit.getServer().getClass().getPackage().getName();
	private final static String serverPackageVersion = serverPackagePath.substring(serverPackagePath.lastIndexOf('.') + 1);
	private static boolean enabled = true;

	static {
		try {
			methodCache.put("obc.CraftItemStack.asNMSCopy", getOBCClass("inventory.CraftItemStack").getMethod("asNMSCopy", ItemStack.class));
			methodCache.put("obc.CraftItemStack.asBukkitCopy", getOBCClass("inventory.CraftItemStack").getMethod("asBukkitCopy", getNMSClass("ItemStack")));
			methodCache.put("nms.ItemStack.hasTag", getNMSClass("ItemStack").getMethod("hasTag"));
			methodCache.put("nms.ItemStack.getTag", getNMSClass("ItemStack").getMethod("getTag"));
			methodCache.put("nms.ItemStack.setTag", getNMSClass("ItemStack").getMethod("setTag", getNMSClass("NBTTagCompound")));
			methodCache.put("nms.MojangsonParser.parse", getNMSClass("MojangsonParser").getMethod("parse", String.class));
			//methodCache.put("nms.NBTTagCompound", getNMSClass("NBTTagCompound")
		} catch (Exception e) {
			Bukkit.getLogger().log(Level.WARNING, "Error loading NMS handler", e);
			enabled = false;
		}
	}

	private static Class getNMSClass(String name) {
		if (classCache.containsKey("nms." + name)) {
			return classCache.get("nms." + name);
		}
		try {
			Class c = Class.forName("net.minecraft.server." + serverPackageVersion + "." + name);
			classCache.put("nms." + name, c);
			return c;
		} catch (ClassNotFoundException e) {
			Bukkit.getLogger().log(Level.WARNING, "NMS Error", e);
			classCache.put("nms." + name, null);
			return null;
		}
	}

	private static Class getOBCClass(String name) {
		if (classCache.containsKey("obc." + name)) {
			return classCache.get("obc." + name);
		}
		try {
			Class c = Class.forName("org.bukkit.craftbukkit." + serverPackageVersion + "." + name);
			classCache.put("obc." + name, c);
			return c;
		} catch (ClassNotFoundException e) {
			Bukkit.getLogger().log(Level.WARNING, "OBC Error", e);
			classCache.put("obc." + name, null);
			return null;
		}
	}

	private static Object asNMSCopy(ItemStack item) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodCache.get("obc.CraftItemStack.asNMSCopy").invoke(null, item);
	}

	private static ItemStack asBukkitCopy(Object item) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return (ItemStack) methodCache.get("obc.CraftItemStack.asBukkitCopy").invoke(null, item);
	}

	private static boolean itemStackHasTag(Object item) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodCache.get("nms.ItemStack.hasTag").invoke(item).equals(true);
	}

	private static Object itemStackGetTag(Object item) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodCache.get("nms.ItemStack.getTag").invoke(item);
	}

	private static Object itemStackSetTag(Object item, Object tag) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodCache.get("nms.ItemStack.setTag").invoke(item, tag);
	}

	private static Object loadNBTTagCompound(String json) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
		return methodCache.get("nms.MojangsonParser.parse").invoke(null, json);
	}
}
