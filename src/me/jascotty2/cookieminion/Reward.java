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
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class Reward {

	protected static final Random RNG = new Random();
	private static final Pattern WORD_BOUNDS_PATTERN = Pattern.compile("\\b(\\w)");

	public boolean useFixedReward = false, useVariableReward = false,
			hasLootReward = false, replaceLoot = false, useDecimalAmounts = true,
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

	public static class Item {

		public final Material itemMaterial;
		double chance = 100;
		int amount = 1;
		short data = 0;
		short dataMax = 0;
		Map extraData = null;

		public Item(Material itemMaterial) {
			this.itemMaterial = itemMaterial;
		}

		public ItemStack getItemStack() {
			ItemStack it = new ItemStack(itemMaterial, amount);
			if (data != dataMax) {
				it.setDurability((short) (data + RNG.nextInt(dataMax - data)));
			} else if (data != 0) {
				it.setDurability(data);
			}
			// todo: extraData
			return it;
		}
	}

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
		if (!hasLootReward || loot == null || loot.isEmpty()) {
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
}
