/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 *
 * Description: Hooks for adding a custom flag to WorldGuard
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
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.flags.DefaultFlag;
import com.sk89q.worldguard.protection.flags.Flag;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.flags.registry.SimpleFlagRegistry;
import com.sk89q.worldguard.protection.managers.RegionManager;
import java.util.logging.Level;
import me.jascotty2.libv3.util.ReflectionUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class RegionFlagManager {

	WorldGuardPlugin wgPlugin;
	boolean hookInstalled = false;
	public static final StateFlag FLAG = new StateFlag("cookiemonster", true);

	public RegionFlagManager(WorldGuardPlugin wgPlugin) {
		this.wgPlugin = wgPlugin;
	}

	public void hookWG() {
		for (Flag<?> flag : DefaultFlag.getDefaultFlags()) {
			if (flag.getName().equalsIgnoreCase(FLAG.getName())) {
				return;
			}
		}
		try {
			// add this flag to the list of flags
			Flag<?>[] flags = new Flag<?>[DefaultFlag.flagsList.length + 1];
			System.arraycopy(DefaultFlag.flagsList, 0, flags, 0, DefaultFlag.flagsList.length);
			flags[DefaultFlag.flagsList.length] = FLAG;

			ReflectionUtils.setStaticField(DefaultFlag.class.getField("flagsList"), flags);

			// register this flag in the registry
			SimpleFlagRegistry flagRegistry = (SimpleFlagRegistry) ReflectionUtils.getPrivateField(WorldGuardPlugin.class, wgPlugin, "flagRegistry");
			flagRegistry.register(FLAG);

			// sanity check
			for (int i = 0; i < DefaultFlag.getFlags().length; ++i) {
				Flag<?> flag1 = DefaultFlag.getFlags()[i];
				if (flag1 == null) {
					throw new RuntimeException("Flag[" + i + "] is null");
				}
			}

			wgPlugin.getLogger().info(String.format("Custom Flag %s added by CookieMinion", FLAG.getName()));

		} catch (Exception ex) {
			Bukkit.getServer().getLogger().log(Level.WARNING, "Could not add flag {0} to WorldGuard", FLAG.getName());
		}
	}

	public boolean rewardsAllowed(Location l) {
		final RegionManager mgr = wgPlugin.getRegionManager(l.getWorld());
		if (mgr != null) {
			ApplicableRegionSet matches = mgr.getApplicableRegions(l);

			return matches.allows(FLAG);
		}
		return true;
	}
}
