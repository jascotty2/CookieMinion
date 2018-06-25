/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 *
 * Description: Bukkit Plugin: Rewards for Monster Kills
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

import java.util.logging.Level;
import me.jascotty2.libv3.bukkit.util.QuietCommander;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.plugin.java.JavaPlugin;

public class CookieMinion extends JavaPlugin {

	final static String CHAT_PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "CookieMinion" + ChatColor.GRAY + "] ";
	final Config config = new Config(this);
	final EntityListener listener = new EntityListener(this);
	final EconomyHandler econ = new EconomyHandler(this);
	final QuietCommander commander = new QuietCommander(this);

	@Override
	public void onEnable() {
		// don't run alongside the full version
		if(getServer().getPluginManager().isPluginEnabled("CookieMonster")) {
			getLogger().info("Conflicts with CookieMonster - disabling CookieMinion");
			this.setEnabled(false);
			return;
		}
		config.load();
		econ.init();
		if (!econ.enabled()) {
			getLogger().warning("Vault plugin not found - currency rewards are disabled");
		}
		getServer().getPluginManager().registerEvents(listener, this);
		if (config.usePhysicalMoneyDrops) {
			listener.startItemTask();
		}
	}

	@Override
	public void onDisable() {
		listener.stopItemTask();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (args.length == 1 && args[0].equalsIgnoreCase("reload")) {
			try {
				if (config.reload()) {
					sender.sendMessage(CHAT_PREFIX + ChatColor.GREEN + "Config Reloaded!");
				} else {
					sender.sendMessage(CHAT_PREFIX + ChatColor.LIGHT_PURPLE + "Config Reloaded with errors!");
				}
				if (config.usePhysicalMoneyDrops) {
					listener.startItemTask();
				} else {
					listener.stopItemTask();
				}
			} catch (Throwable t) {
				sender.sendMessage(CHAT_PREFIX + ChatColor.RED + "Reload Failed!");
				getLogger().log(Level.SEVERE, "Config Reload Error", t);
			}
		} else {
			sender.sendMessage(CHAT_PREFIX + ChatColor.RED + "Command Error\n/cookieminion reload");
		}
		return true;
	}

	public boolean isEnabled(World w) {
		return w == null || config.disabledWorlds.isEmpty() || !config.disabledWorlds.contains(w.getName());
	}

	public boolean isReward(EntityType e) {
		return e != null && e.isAlive() && (config.defaultReward != null || config.rewards.containsKey(e));
	}

	public Reward getReward(EntityType e) {
		return config.rewards.containsKey(e) ? config.rewards.get(e) : config.defaultReward;
	}
}
