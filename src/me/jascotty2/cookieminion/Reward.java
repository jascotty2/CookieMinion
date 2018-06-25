/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 *
 * Description: Mob kill rewards
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program. If not, see <http://www.gnu.org/licenses/>.
 */
package me.jascotty2.cookieminion;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

public class Reward {

	protected static final Random RNG = new Random();
	private static final Pattern WORD_BOUNDS_PATTERN = Pattern.compile("\\b(\\w)");

	public boolean useFixedReward = false, useVariableReward = false,
			replaceLoot = false, useDecimalAmounts = true,
			playerStealsReward = false;
	public double amount, minAmount, maxAmount;
	public List<String> commands = null;
	public String message = null;
	public int maxLoot = 0;
	public List<Item> loot = null;
	public Map<String, Double> multipliers = null;
	public List<String> multiplierOrder = null;
	/**
	 * Internal/Future use: if the item does not have all of the settings from
	 * the config definition
	 */
	public boolean incompleteLoadError = false;

	/*
        # each item has a type or id, a data value [or a data range], max number, and a percentage chance to drop 
        # you can also set some NBT values in curly brackets {}
        loot: 
            - INK_SACK:[1-15]@2%90
            - MELON%10
            - IRON_NUGGET%10
            - SKULL{SkullOwner:"jascotty2"}%.5
            - IRON_SWORD{Enchantments:[id:Looting,lvl:1]}%1
        # The loot table is checked sequentially
        # use this if you want it to stop once it's provided a certain number of drops
        maxLoot: 1
        # Set replace to true if you don't want the mob to drop its default loot
        replaceLoot: false
	
	 */
	public List<ItemStack> getRewardLoot() {
		if (loot == null || loot.isEmpty()) {
			return Collections.EMPTY_LIST;
		}
		List<ItemStack> itms = new ArrayList<ItemStack>();
		for (Item it : loot) {
			if (RNG.nextDouble() * 100 < it.chance) {
				itms.add(it.getItemStack());
				if (maxLoot != 0 && itms.size() >= maxLoot) {
					break;
				}
			}
		}

		return itms;
	}

	public double getRewardAmount(Player p) {
		if (!useFixedReward && !useVariableReward) {
			return Double.NaN;
		}
		double multi = 1;
		if (multipliers != null && multiplierOrder != null && !multiplierOrder.isEmpty()) {
			for (String perm : multiplierOrder) {
				if (p.hasPermission(perm) && multipliers.containsKey(perm)) {
					multi = multipliers.get(perm);
					break;
				}
			}
		}
		if (useFixedReward) {
			return multi * amount;
		} else {//if (useVariableReward) {
			return (useDecimalAmounts
					? Math.round((minAmount + (multi * RNG.nextDouble() * (maxAmount - minAmount))) * 100) / 100.
					: Math.round(minAmount + (multi * RNG.nextInt((int) (maxAmount - minAmount)))));
		}
	}

	public void sendMessage(Player p, Entity entity, double reward) {
		if (message != null && p != null) {
			String msg = message.replace("$money$", NumberFormat.getInstance().format(reward));// a $entity$ and earned $$money$!
			if (msg.contains("$entity$")) {
				if (entity instanceof Player) {
					msg = msg.replace("$entity$", ((Player) entity).getDisplayName());
				} else {
					if (entity.getCustomName() != null) {
						msg = msg.replace("$entity$", entity.getCustomName());
					} else {
						msg = msg.replace("$entity$", toTitleCase(entity.getType().name().replace("_", " ").toLowerCase()));
					}
				}
			}
			p.sendMessage(msg);
		}
	}

	public void runRewardCommands(Player p, CommandSender as, Entity entity, double reward) {
		if (commands != null && !commands.isEmpty()) {
			String cmdTry = null;
			try {
				OfflinePlayer players[] = p.getServer().getOfflinePlayers();
				for (String cmd : commands) {
					// variables to check for:
					// @r: grab a random player that is not our player
					// can't choose a random player that isn't us if we're the only one on
					if (cmd.contains("@r") && players.length == 1) {
						continue;
					}
					while (cmd.contains("@r")) {
						int i = RNG.nextInt(players.length);
						if (players[i].getUniqueId().equals(p.getUniqueId())) {
							if (++i >= players.length) {
								i = 0;
							}
						}
						cmd = cmd.replaceFirst("@r", players[i].getName());
					}

					// next to check: @p = us
					cmd = cmd.replace("@p", p.getName());

					// custom variables:
					cmd = cmd.replace("$money$", NumberFormat.getInstance().format(reward));// a $entity$ and earned $$money$!
					if (cmd.contains("$entity$")) {
						if (entity instanceof Player) {
							cmd = cmd.replace("$entity$", ((Player) entity).getDisplayName());
						} else {
							if (entity.getCustomName() != null) {
								cmd = cmd.replace("$entity$", entity.getCustomName());
							} else {
								cmd = cmd.replace("$entity$", toTitleCase(entity.getType().name().replace("_", " ").toLowerCase()));
							}
						}
					}

					if (cmd.contains("@a")) {
						for (OfflinePlayer pl : players) {
							runCommand(cmdTry = cmd.replace("@a", pl.getName()), as);
						}
					} else {
						runCommand(cmdTry = cmd, as);
					}
				}
			} catch (Throwable t) {
				Bukkit.getLogger().log(Level.SEVERE, "[CookieMinion] Failed to execute command: " + cmdTry, t);
			}
		}
	}

	protected static void runCommand(String cmd, CommandSender as) {
		if (cmd.startsWith("/")) {
			cmd = cmd.substring(1);
		}
		try {
			if (cmd.toLowerCase().startsWith("say ")) {
				// custom formatting :)
				as.getServer().broadcastMessage(ChatColor.translateAlternateColorCodes('&', cmd.substring(4)));
			} else {
				as.getServer().dispatchCommand(as, cmd);
			}
		} catch (Exception e) {
			// just in case... :(
			// Craftbukkit's org/bukkit/craftbukkit/command/VanillaCommandWrapper execute(CommandSender, String, String[])
			// does not check if there is a valid ICommandListener for the sender type :(
			as.getServer().dispatchCommand(as.getServer().getConsoleSender(), cmd);
		}
	}

	private static String toTitleCase(String s) {
		StringBuffer sb = new StringBuffer(s.length());
		Matcher mat = WORD_BOUNDS_PATTERN.matcher(s);
		while (mat.find()) {
			mat.appendReplacement(sb, mat.group().toUpperCase());
		}
		mat.appendTail(sb);
		return sb.toString();
	}

	public static class Item {

		public final Material itemMaterial;
		double chance = 100;
		int amount = 1;
		short data = 0;
		short dataMax = 0;
		Map extraData = null;

		public Item(Material itemMaterial) {
			if (itemMaterial == Material.SKULL) {
				this.itemMaterial = Material.SKULL_ITEM;
			} else {
				this.itemMaterial = itemMaterial;
			}
		}

		public ItemStack getItemStack() {
			ItemStack it = new ItemStack(itemMaterial, amount);
			if (data != dataMax) {
				it.setDurability((short) (data + RNG.nextInt(dataMax - data)));
			} else if (data != 0) {
				it.setDurability(data);
			}
			// does this item have additional metadata?
			if (extraData != null) {
				ItemMeta itm = it.getItemMeta();
				Object o;
				for (Object k : extraData.keySet()) {
					switch (k.toString().toLowerCase()) {
						case "ench":
						case "enchantments":
							// add enchatments to the item
							if ((o = extraData.get(k)) instanceof List) {
								for (Object enc : (List) o) {
									if (enc instanceof Map) {
										Enchantment toAdd = null;
										int lvl = 1;
										Object id = ((Map) enc).get("id");
										if (id instanceof String) {
											for (Enchantment ech : Enchantment.values()) {
												if ((ech.getName() != null && ech.getName().equalsIgnoreCase(id.toString()))
														|| ech.getClass().getSimpleName().equalsIgnoreCase(id.toString())) {
													toAdd = ech;
													break;
												}
											}
										} else if (id instanceof Integer) {
											toAdd = Enchantment.getById((Integer) id);
										}
										if (toAdd != null) {
											id = ((Map) enc).get("lvl");
											if (id != null && (id instanceof Integer)) {
												lvl = (Integer) id;
											}
											itm.addEnchant(toAdd, lvl, true);
										}
									}
								}
							}
							break;
						case "display":
							itm.setDisplayName(ChatColor.translateAlternateColorCodes('&', extraData.get(k).toString()));
							break;
						case "unbreakable":
							if ((o = extraData.get(k)) instanceof Integer) {
								itm.setUnbreakable(((Integer) o) != 0);
							}
							break;
						case "hideflags":
							if ((o = extraData.get(k)) instanceof Integer) {
								int flag = (Integer) o;
								if (flag >= 32 && (flag = flag - 32) >= 0) {
									itm.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
								}
								if (flag >= 16 && (flag = flag - 16) >= 0) {
									itm.addItemFlags(ItemFlag.HIDE_PLACED_ON);
								}
								if (flag >= 8 && (flag = flag - 8) >= 0) {
									itm.addItemFlags(ItemFlag.HIDE_DESTROYS);
								}
								if (flag >= 4 && (flag = flag - 4) >= 0) {
									itm.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
								}
								if (flag >= 2 && (flag = flag - 2) >= 0) {
									itm.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
								}
								if (flag >= 1) {
									itm.addItemFlags(ItemFlag.HIDE_ENCHANTS);
								}
							}
							break;
						case "skullowner":
							if (itm instanceof SkullMeta) {
								it.setDurability((short)3);
								((SkullMeta) itm).setOwner(extraData.get(k).toString());
							}
							break;
						case "generation":
							if(itm instanceof BookMeta) {
								BookMeta bm = (BookMeta) itm;
								switch(extraData.get(k).toString().toLowerCase()) {
									case "original":
										bm.setGeneration(BookMeta.Generation.ORIGINAL);
										break;
									case "copy of original":
										bm.setGeneration(BookMeta.Generation.COPY_OF_ORIGINAL);
										break;
									case "copy of a copy":
										bm.setGeneration(BookMeta.Generation.COPY_OF_COPY);
										break;
									case "tattered":
										bm.setGeneration(BookMeta.Generation.TATTERED);
								}
							}
							break;
						/**** Custom NBT Tags!! ****/
						case "title":
							if(itm instanceof BookMeta) {
								BookMeta bm = (BookMeta) itm;
								bm.setTitle(ChatColor.translateAlternateColorCodes('&', extraData.get(k).toString()));
							}
							break;
						case "author":
							if(itm instanceof BookMeta) {
								BookMeta bm = (BookMeta) itm;
								bm.setAuthor(ChatColor.translateAlternateColorCodes('&', extraData.get(k).toString()));
							}
							break;
						case "pages":
							if(itm instanceof BookMeta) {
								BookMeta bm = (BookMeta) itm;
								if((o = extraData.get(k)) instanceof List) {
									for(Object page : (List) o) {
										bm.addPage(ChatColor.translateAlternateColorCodes('&', page.toString()));
									}
								} else {
									bm.addPage(ChatColor.translateAlternateColorCodes('&', o.toString()));
								}
							}
							break;
						case "lore":
							if((o = extraData.get(k)) instanceof List) {
								ArrayList<String> lore = new ArrayList<String>(((List)o).size());
								for(Object page : (List) o) {
									lore.add(ChatColor.translateAlternateColorCodes('&', page.toString()));
								}
								itm.setLore(lore);
							} else {
								itm.setLore(Arrays.asList(ChatColor.translateAlternateColorCodes('&', o.toString())));
							}
					}
				}
				it.setItemMeta(itm);
			}
			return it;
		}
	}

}
