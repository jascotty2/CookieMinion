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

import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.WorldGuardPlugin;
import com.sk89q.worldguard.protection.association.RegionAssociable;
import com.sk89q.worldguard.protection.flags.Flags;
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.logging.Level;
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
		try {
			WorldGuard.getInstance().getFlagRegistry().register(FLAG);
		} catch (Exception ex) {
			Bukkit.getServer().getLogger().log(Level.WARNING, "Could not add flag {0} to WorldGuard", FLAG.getName());
		}
		/*
		if(WorldGuard.getInstance().getFlagRegistry().get(FLAG.getName()) == null) {
			FlagRegistry fr = WorldGuard.getInstance().getFlagRegistry();
			if(fr instanceof SimpleFlagRegistry) {
				boolean in = ((SimpleFlagRegistry) fr).isInitialized();
				((SimpleFlagRegistry) fr).setInitialized(false);
				fr.register(FLAG);
				((SimpleFlagRegistry) fr).setInitialized(in);
			} else {
				Bukkit.getServer().getLogger().log(Level.WARNING, "Could not add flag {0} to WorldGuard", FLAG.getName());
			}
		}*/
		/*
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
		}*/
	}

	public boolean rewardsAllowed(Location l) {
		
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(l);
		return query.testState(loc, (RegionAssociable) null, Flags.BUILD);
		
	}
}
