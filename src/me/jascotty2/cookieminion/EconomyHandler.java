/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 *
 * Description: Methods for Economy plugins
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
import net.milkbowl.vault.Vault;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;

public class EconomyHandler {

	protected Economy vaultEcon = null;
	protected final CookieMinion plugin;

	public EconomyHandler(CookieMinion plugin) {
		this.plugin = plugin;
	}

	public void init() {
		final PluginManager pm = plugin.getServer().getPluginManager();

		// attempt to load external economy plugins (using vault API)
		Plugin v = pm.getPlugin("Vault");
		if (v instanceof Vault) {
			RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
			if (rsp != null) {
				vaultEcon = rsp.getProvider();
			}
		}
	}

	public boolean enabled() {
		return vaultEcon != null;
	}

	public boolean hasAccount(Player pl) {
		return vaultEcon != null && vaultEcon.hasAccount(pl);
	}

	public boolean canAfford(Player pl, double amt) {
		return vaultEcon != null && pl != null && vaultEcon.getBalance(pl) >= amt;
	}

	public double getBalance(Player pl) {
		return vaultEcon != null ? vaultEcon.getBalance(pl) : Double.NEGATIVE_INFINITY;
	}

	public double getBalance(String playerName) {
		return vaultEcon != null ? vaultEcon.getBalance(playerName) : Double.NEGATIVE_INFINITY;
	}

	public void addMoney(Player pl, double amt) {
		if (vaultEcon != null) {
			vaultEcon.depositPlayer(pl, amt);
		}
	}

	public void addMoney(String playerName, double amt) {
		if (vaultEcon != null) {
			vaultEcon.depositPlayer(playerName, amt);
		}
	}

	public void subtractMoney(Player pl, double amt) {
		if (vaultEcon != null) {
			vaultEcon.withdrawPlayer(pl, amt);
		}
	}

	public void subtractMoney(String playerName, double amt) {
		if (vaultEcon != null) {
			vaultEcon.withdrawPlayer(playerName, amt);
		}
	}

	public String format(double amt) {
		String s;
		return (vaultEcon != null ? (Character.isDigit((s = vaultEcon.format(0).substring(0, 1)).charAt(0)) ? "$" : s ): "$")
				+ padEnd(NumberFormat.getInstance().format(amt));
		// vault sometimes prints out flotaing point errors...
		//return vaultEcon != null ? vaultEcon.format(amt) : String.format("%.2f", amt);
	}

	public String formatCurrency(double amt) {
		return (vaultEcon == null ? "$" : "")
				+ padEnd(NumberFormat.getInstance().format(amt))
				+ (vaultEcon != null ? " " + (amt == 1 ? vaultEcon.currencyNameSingular() : vaultEcon.currencyNamePlural()) : "");
		// vault sometimes prints out flotaing point errors...
//		return vaultEcon != null
//				? vaultEcon.format(amt) + " " + (amt == 1 ? vaultEcon.currencyNameSingular() : vaultEcon.currencyNamePlural())
//				: String.format("%.2f", amt);
	}
	
	protected String padEnd(String str) {
		final int moneyDecimalPlaces = plugin.config.moneyDecimalPlaces;
		if(moneyDecimalPlaces > 0) {
			int dec = str.indexOf('.');
			if(dec == -1) {
				StringBuilder sb = new StringBuilder();
				sb.append(str).append('.');
				for(int i = 0; i < moneyDecimalPlaces; ++i) {
					sb.append('0');
				}
				return sb.toString();
			} else if(str.length() - dec >= moneyDecimalPlaces) {
				StringBuilder sb = new StringBuilder();
				sb.append(str);
				for(int i = str.length() - dec; i <= moneyDecimalPlaces; ++i) {
					sb.append('0');
				}
				return sb.toString();
			}
		}
		return str;
	}
}
