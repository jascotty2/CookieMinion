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

import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import java.util.List;
import java.util.logging.Level;
import me.jascotty2.libv3.bukkit.util.QuietCommander;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public class CookieMinion extends JavaPlugin {

	final static String CHAT_PREFIX = ChatColor.GRAY + "[" + ChatColor.AQUA + "CookieMinion" + ChatColor.GRAY + "] ";
	final Config config = new Config(this);
	final EntityListener listener = new EntityListener(this);
	final EconomyHandler econ = new EconomyHandler(this);
	final QuietCommander commander = new QuietCommander(this);
	RegionFlagManager regions = null;

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
		
		Plugin plugin = getServer().getPluginManager().getPlugin("WorldGuard");
		if (plugin != null && (plugin instanceof WorldGuardPlugin)) {
			regions = new RegionFlagManager((WorldGuardPlugin) plugin);
			regions.hookWG();
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

	public boolean isEnabled(Location l) {
		return l == null || 
				!(config.disabledWorlds.contains(l.getWorld().getName()) || 
				(regions != null && !regions.rewardsAllowed(l)));
	}

	public boolean isEnabled(World w) {
		return w == null || config.disabledWorlds.isEmpty() || !config.disabledWorlds.contains(w.getName());
	}
	
	public boolean isReward(EntityType e) {
		// armor stands are marked as living for some reason
		return e != EntityType.ARMOR_STAND && e != null && e.isAlive()
				&& (config.defaultReward != null || config.rewards.containsKey(e));
	}

	public Reward getReward(LivingEntity e) {
		if(e == null)
			return null;
		if(!config.rewards.containsKey(e.getType()))
			return config.defaultReward;
		List<Reward> rl = config.rewards.get(e.getType());
		// process checks in defined order
		for(Reward r : rl)
			if(r.condition != null && r.condition.matches(e))
				return r;
		// return any that don't have a condition
		for(Reward r : rl)
			if(r.condition == null)
				return r;
		return null;
	}
}
