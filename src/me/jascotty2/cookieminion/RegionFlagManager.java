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
import com.sk89q.worldguard.protection.flags.StateFlag;
import com.sk89q.worldguard.protection.regions.RegionContainer;
import com.sk89q.worldguard.protection.regions.RegionQuery;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.Location;

public class RegionFlagManager {

	WorldGuardPlugin wgPlugin;
	boolean hookInstalled = false;
	static StateFlag FLAG = new StateFlag("cookiemonster", true);

	public RegionFlagManager(WorldGuardPlugin wgPlugin) {
		this.wgPlugin = wgPlugin;
	}

	public void hookWG() {
		try {
			WorldGuard.getInstance().getFlagRegistry().register(FLAG);
			hookInstalled = true;
		} catch (Exception ex) {
			Object o = WorldGuard.getInstance().getFlagRegistry().get(FLAG.getName());
			if (o != null && StateFlag.class.isAssignableFrom(o.getClass())) {
				FLAG = (StateFlag) o;
				hookInstalled = true;
			}
			if (!hookInstalled) {
				Bukkit.getServer().getLogger().log(Level.WARNING, "Could not add flag {0} to WorldGuard", FLAG.getName());
			}
		}
	}

	public boolean rewardsAllowed(Location l) {
		if (!hookInstalled || FLAG == null) {
			return true;
		}
		RegionContainer container = WorldGuard.getInstance().getPlatform().getRegionContainer();
		RegionQuery query = container.createQuery();
		com.sk89q.worldedit.util.Location loc = BukkitAdapter.adapt(l);
		return query.testState(loc, (RegionAssociable) null, FLAG);

	}
}
