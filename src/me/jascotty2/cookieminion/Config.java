/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 *
 * Description: Configuration handler for CookieMinon
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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import me.jascotty2.libv3_2.util.JsonParser;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;

public class Config {

	final CookieMinion plugin;
	final public HashMap<EntityType, Reward> rewards = new HashMap<EntityType, Reward>();
	public Reward defaultReward = null;

	/**
	 * Should mobs from a mob spawner reward the player?
	 */
	public boolean allowMobSpawnerRewards = false;
	/**
	 * If a mob dies and no player killed it, should it still drop items?
	 */
	public boolean allowNaturalDeathItemDrops = true;
	/**
	 * If true, rewards are not given to the player until they 'pick up' the
	 * reward
	 */
	public boolean usePhysicalMoneyDrops = true;
	public Material moneyDropItem = Material.EMERALD;
	public ChatColor moneyDropColor = ChatColor.GOLD;

	public final List<String> disabledWorlds = new ArrayList<String>();

	public Config(CookieMinion plugin) {
		this.plugin = plugin;
	}

	public boolean reload() {
		plugin.reloadConfig();
		return load();
	}

	public boolean load() {
		// if the config file doesn't exist, create it
		plugin.saveDefaultConfig();
		// clear out any lingering settings
		rewards.clear();
		disabledWorlds.clear();
		defaultReward = null;
		// use bukkit's config loader
		FileConfiguration cfg = plugin.getConfig();

		boolean ok = true;

		allowMobSpawnerRewards = cfg.getBoolean("allowMobSpawnerRewards", allowMobSpawnerRewards);
		allowNaturalDeathItemDrops = cfg.getBoolean("allowNaturalDeathItemDrops", allowNaturalDeathItemDrops);
		usePhysicalMoneyDrops = cfg.getBoolean("physicalMoneyDrops", usePhysicalMoneyDrops);
		String s = cfg.getString("moneyDropItem");
		if (s != null) {
			Material m = Material.matchMaterial(s);
			if (m != null) {
				moneyDropItem = m;
			} else {
				plugin.getLogger().warning("Unknown material for moneyDropItem: " + s);
				ok = false;
			}
		}
		if ((s = cfg.getString("moneyDropColor")) != null) {
			ChatColor c = null;
			for (ChatColor cc : ChatColor.values()) {
				if (cc.name().equalsIgnoreCase(s)) {
					c = cc;
					break;
				}
			}
			if (c != null && c.isColor()) {
				moneyDropColor = c;
			} else {
				plugin.getLogger().warning("Invalid color name for moneyDropColor: " + s);
				ok = false;
			}
		}

		List<String> l = cfg.getStringList("disabledWorlds");
		if (l != null && !l.isEmpty()) {
			disabledWorlds.addAll(l);
		}

		// now for the actual rewards!
		ConfigurationSection sec = cfg.getConfigurationSection("rewards");
		if (sec != null) {
			for (String k : sec.getKeys(false)) {
				ConfigurationSection def = sec.getConfigurationSection(k);
				Reward r = null;
				if (def == null) {
					plugin.getLogger().warning("Unexpected configuration in rewards: " + k);
				} else if (k.equalsIgnoreCase("default")) {
					defaultReward = r = loadReward(def);
				} else if (k.equalsIgnoreCase("player")) {
					// name of player entity type not defined in the enum. *shrug*
					rewards.put(EntityType.PLAYER, r = loadReward(def));
				} else {
					EntityType et = EntityType.fromName(k);
					if (et == null) {
						plugin.getLogger().warning("Unknown EntityType for rewards: " + k);
					} else if (!et.isAlive()) {
						plugin.getLogger().warning("rewards." + k + " does not define a valid LivingEntity");
					} else {
						rewards.put(et, r = loadReward(def));
					}
				}
				if (r == null || r.incompleteLoadError) {
					ok = false;
				}
			}
		}
		
		// copy multiplier settings
		if(defaultReward.multiplierOrder != null && defaultReward.multipliers != null) {
			for(Reward r : rewards.values()) {
				if(r.multiplierOrder == null) {
					r.multiplierOrder = defaultReward.multiplierOrder;
					r.multipliers = defaultReward.multipliers;
				}
			}
		}

		return ok;
	}

	Reward loadReward(ConfigurationSection sec) {
		Reward r = new Reward();
		List<String> l;

		if ((r.minAmount = sec.getDouble("min", Double.NEGATIVE_INFINITY))
				- (r.maxAmount = sec.getDouble("max", Double.NEGATIVE_INFINITY)) != Double.NEGATIVE_INFINITY) {
			r.useVariableReward = true;
			if (r.minAmount > r.maxAmount) {
				double temp = r.maxAmount;
				r.maxAmount = r.minAmount;
				r.minAmount = temp;
			} else if (r.minAmount == r.maxAmount) {
				r.amount = r.minAmount;
				r.useFixedReward = !(r.useVariableReward = false);
			}
		} else {
			r.useFixedReward = (r.amount = sec.getDouble("min", Double.NEGATIVE_INFINITY)) != Double.NEGATIVE_INFINITY;
		}

		r.useDecimalAmounts = sec.getBoolean("allowDecimals", r.useDecimalAmounts);
		r.playerStealsReward = sec.getBoolean("stealReward", r.playerStealsReward);

		if ((r.message = sec.getString("message")) != null) {
			r.message = ChatColor.translateAlternateColorCodes('&', r.message);
		}
		
		Object mo = sec.get("multipliers");
		if(mo instanceof List) {
			r.multipliers = new HashMap<String, Double>();
			r.multiplierOrder = new LinkedList<String>();
			for(Object perm : (List) mo) {
				if(perm instanceof Map) {
					for(Map.Entry<String, Object> e : ((Map<String, Object>) perm).entrySet()) {
						try {
							double v = Double.parseDouble(e.getValue().toString());
							r.multipliers.put(e.getKey(), v);
							r.multiplierOrder.add(e.getKey());
						} catch (NumberFormatException ex) {
							plugin.getLogger().warning("multiplier setting error for " + sec.getCurrentPath() + ": " + e.getKey());
						}
					}
				} else {
					plugin.getLogger().warning("multiplier setting error for " + sec.getCurrentPath() + ": " + perm.toString());
					r.incompleteLoadError = true;
				}
			}
		} else if(mo instanceof Map) {
			// just in case someone missed the list format
			r.multipliers = new HashMap<String, Double>();
			r.multiplierOrder = new LinkedList<String>();
			for(Map.Entry<String, Object> e : ((Map<String, Object>) mo).entrySet()) {
				try {
					double v = Double.parseDouble(e.getValue().toString());
					r.multipliers.put(e.getKey(), v);
					r.multiplierOrder.add(e.getKey());
				} catch (NumberFormatException ex) {
					plugin.getLogger().warning("multiplier setting error for " + sec.getCurrentPath() + ": " + e.getKey());
					r.incompleteLoadError = true;
				}
			}
		} else if(mo != null) {
			plugin.getLogger().warning("multiplier setting error for " + sec.getCurrentPath());
			r.incompleteLoadError = true;
		}
		
		if ((l = sec.getStringList("loot")) != null && !l.isEmpty()) {
			r.loot = new LinkedList<Reward.Item>();
			// full possible: 
			// ITEM:[0-9]{data..{more data..}}@5%50
			// or ITEM:5{data..{more data..}}@5%50
			// every field after item is optional
			Pattern itemPattern = Pattern.compile(
					"([a-zA-Z_0-9]+)"
					+ "(:[0-9]+|:\\[[0-9]+\\-[0-9]+\\])?"
					+ "(\\{.*\\})?"
					+ "(@[0-9]+)?"
					+ "(%([0-9]*\\.)?[0-9]+)?");
			for (String itmStr : l) {
				Matcher m = itemPattern.matcher(itmStr);
				if (m.matches()) {
					Material mat = Material.matchMaterial(m.group(1));
					if (mat == null) {
						plugin.getLogger().warning("Unknown item in definition for " + sec.getCurrentPath() + ": " + itmStr);
						r.incompleteLoadError = true;
					} else {
						// have the item, now load the rest of the values
						Reward.Item itm = new Reward.Item(mat);
						String t; // temp

						// data value
						if ((t = m.group(2)) != null) {
							if (t.startsWith(":[")) {
								// data range
								int mid = t.indexOf('-');
								itm.data = Short.parseShort(t.substring(2, mid));
								if ((itm.dataMax = Short.parseShort(t.substring(mid + 1, t.length() - 1))) < itm.data) {
									short swap = itm.data;
									itm.data = itm.dataMax;
									itm.dataMax = swap;
								}
							} else {
								itm.data = Short.parseShort(t.substring(1));
							}
						}

						// nbt tagData
						if ((t = m.group(3)) != null) {
							try {
								if (!dataValid(mat, itm.extraData = JsonParser.parseJSON(t), itmStr)) {
									r.incompleteLoadError = true;
								}
							} catch (ParseException ex) {
								plugin.getLogger().warning("Unable to parse custom data for " + sec.getCurrentPath() + ": " + itmStr);
								plugin.getLogger().log(Level.SEVERE, "Parse Error:", ex);
								r.incompleteLoadError = true;
							}
						}

						// amount
						if ((t = m.group(4)) != null) {
							itm.amount = Integer.parseInt(t.substring(1));
						}

						// chance
						if ((t = m.group(5)) != null) {
							itm.chance = Double.parseDouble(t.substring(1));
						}
						r.loot.add(itm);
					}
				} else {
					plugin.getLogger().warning("Erroneous item definition for " + sec.getCurrentPath() + ": " + itmStr);
					r.incompleteLoadError = true;
				}
			}
			/*
            - INK_SACK:[1-15]@2%90
            - MELON%10
            - IRON_NUGGET%10
            - SKULL{SkullOwner:"jascotty2"}%.5
            - IRON_SWORD{Enchantments:[id:Looting,lvl:1]}%1
			 */
		}
		r.maxLoot = sec.getInt("maxLoot", 0);
		r.replaceLoot = sec.getBoolean("replaceLoot", false);

		if ((l = sec.getStringList("commands")) != null && !l.isEmpty()) {
			r.commands = new ArrayList<String>(l);
		}

		return r;
	}

	/**
	 * More of a quick sanity check than a thorough validation
	 *
	 * @param mat
	 * @param dat
	 * @param def
	 * @return
	 */
	boolean dataValid(Material mat, Map dat, String def) {

		// root values that are valid for all items:
		// ench, Enchantments, display, AttributeModifiers, Unbreakable, 
		// HideFlags, CanDestroy, PickupDelay, Age
		// special values:
		// SkullOwner (Skull), generation (Book)
		String errors = "";
		Object o;
		for (Object k : dat.keySet()) {
			switch (k.toString().toLowerCase()) {
				case "ench":
				case "enchantments":
					// are the enchantments valid?
					boolean ok = true;
					if ((o = dat.get(k)) instanceof List) {
						for (Object enc : (List) o) {
							if (enc instanceof Map) {
								Object id = ((Map) enc).get("id");
								if (id instanceof String) {
									boolean test = false;
									for(Enchantment ech : Enchantment.values()) {
										if((ech.getName() != null && ech.getName().equalsIgnoreCase(id.toString()))
												|| ech.getClass().getSimpleName().equalsIgnoreCase(id.toString())) {
											test = true;
											break;
										}
									}
									if (!test) {
										plugin.getLogger().warning("Unknown Enchantment: " + id.toString());
										ok = false;
									}
								} else if (id instanceof Integer) {
									if (Enchantment.getById((Integer) id) == null) {
										plugin.getLogger().warning("Unknown Enchantment: " + id.toString());
										ok = false;
									}
								} else {
									ok = false;
								}
								id = ((Map) enc).get("lvl");
								if(id != null && !(id instanceof Integer)) {
									ok = false;
								}
							} else {
								ok = false;
							}
						}
					} else {
						ok = false;
					}
					if (!ok) {
						plugin.getLogger().warning("Erroneous enchantments for " + def);
					}
				case "display":
				case "unbreakable":
					break;
				case "age":
					// verify that this is in range
					if (!((o = dat.get(k)) instanceof Integer)) {
						plugin.getLogger().warning("Bad value for pickupdelay for " + def);
					}
					break;
				case "pickupdelay":
					// verify that this is in range
					if (!((o = dat.get(k)) instanceof Integer) || (Integer) o < 0 || (Integer) o > 32767) {
						plugin.getLogger().warning("Bad value for pickupdelay for " + def);
					}
					break;
				case "hideflags":
					// verify that this is in range
					if (!((o = dat.get(k)) instanceof Integer) || (Integer) o < 0 || (Integer) o > 63) {
						plugin.getLogger().warning("Bad value for hideflags for " + def);
					}
					break;
				case "skullowner":
					if (mat != Material.SKULL && mat != Material.SKULL_ITEM) {
						errors = errors + (errors.isEmpty() ? "" : ", ") + k.toString();
					}
					break;
				case "generation":
					if (mat != Material.BOOK && mat != Material.WRITTEN_BOOK) {
						errors = errors + (errors.isEmpty() ? "" : ", ") + k.toString();
					}
					break;
				// not planning on supporting these right now
				case "attributemodifiers":
				case "candestroy":
				default:
					errors = errors + (errors.isEmpty() ? "" : ", ") + k.toString();
			}
		}
		if (!errors.isEmpty()) {
			plugin.getLogger().warning("Notice: Unexpected data tag" + (errors.indexOf(',') == -1 ? "" : "s") + ": " + errors + " in definition for " + def);
		}
		return errors.isEmpty();
	}
}
