/**
 * Copyright (C) 2018 Jacob Scott <jascottytechie@gmail.com>
 *
 * Description: Entity event listener for CookieMinion
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

import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.projectiles.ProjectileSource;

public class EntityListener implements Listener {

	final CookieMinion plugin;
	long damageDelay = 5000; // 5 seconds
	private int absorptionTask = -1;
	final String lorePrefix = ChatColor.MAGIC + "CookieMonster";

	public EntityListener(CookieMinion plugin) {
		this.plugin = plugin;
	}

	public void startItemTask() {
		if (absorptionTask == -1) {
			absorptionTask = plugin.getServer().getScheduler().scheduleSyncRepeatingTask(plugin, absorptionRunner, 5, 40);
		}
	}

	public void stopItemTask() {
		if (absorptionTask != -1) {
			plugin.getServer().getScheduler().cancelTask(absorptionTask);
			absorptionTask = -1;
		}
	}

	public static class Damage {

		Player source;
		long attackTime;

		Damage(Player source) {
			this.source = source;
			this.attackTime = System.currentTimeMillis();
		}

		void update() {
			this.attackTime = System.currentTimeMillis();
		}

		void update(Player source) {
			this.source = source;
			this.attackTime = System.currentTimeMillis();
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof LivingEntity && plugin.isEnabled(event.getEntity().getLocation())) {
			// killer isn't always set correctly
			Entity dm = event.getDamager();
			Player pc;
			if (dm instanceof Projectile) {
				ProjectileSource s = ((Projectile) dm).getShooter();
				if (s instanceof Player) {
					pc = (Player) s;
				} else {
					return;
				}
			} else if (dm instanceof Player) {
				pc = (Player) dm;
			} else {
				return;
			}
			// use the entity's datastore to track damage
			event.getEntity().setMetadata("playerKiller", new FixedMetadataValue(plugin, new Damage(pc)));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event) {
		if ((event.getCause() == DamageCause.POISON
				|| event.getCause() == DamageCause.MAGIC
				|| event.getCause() == DamageCause.WITHER)
				&& event.getEntity() instanceof LivingEntity
				&& plugin.isEnabled(event.getEntity().getLocation())) {
			List<MetadataValue> mvs = event.getEntity().getMetadata("potionKiller");
			MetadataValue mv = null;
			if (mvs.size() > 1) {
				for (MetadataValue dat : mvs) {
					if (dat.getOwningPlugin().equals(plugin)) {
						mv = dat;
						break;
					}
				}
			} else if (!mvs.isEmpty()) {// && mvs.get(0).getOwningPlugin().equals(plugin)) {
				mv = mvs.get(0);
			}
			if (mv != null && mv.value() instanceof Player) {
				event.getEntity().setMetadata("playerKiller", new FixedMetadataValue(plugin, new Damage((Player) mv.value())));
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPotionSplashEvent(PotionSplashEvent event) {
		if (event.getEntity().getShooter() instanceof Player && plugin.isEnabled(event.getEntity().getLocation())) {
			Player p = (Player) event.getEntity().getShooter();
			for (LivingEntity e : event.getAffectedEntities()) {
				if (p != e) {
					e.setMetadata("potionKiller", new FixedMetadataValue(plugin, p));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onCreatureSpawn(CreatureSpawnEvent event) {
		if (event.getSpawnReason() == CreatureSpawnEvent.SpawnReason.SPAWNER) {
			event.getEntity().setMetadata("spawner_spawned", new FixedMetadataValue(plugin, true));
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		if (plugin.isReward(event.getEntityType()) && plugin.isEnabled(event.getEntity().getLocation())) {

			// let's check if this was killed by a player
			List<MetadataValue> mvs = event.getEntity().getMetadata("playerKiller");
			MetadataValue mv = null;
			if (mvs.size() > 1) {
				for (MetadataValue dat : mvs) {
					if (dat.getOwningPlugin().equals(plugin)) {
						mv = dat;
						break;
					}
				}
			} else if (!mvs.isEmpty()) {// && mvs.get(0).getOwningPlugin().equals(plugin)) {
				mv = mvs.get(0);
			}
			if (mv != null && mv.value() instanceof Damage
					&& System.currentTimeMillis() - ((Damage) mv.value()).attackTime < damageDelay) {
				// check if rewards are allowed for this kill (checking here to not clear drops)
				if (plugin.config.allowMobSpawnerRewards
						|| event.getEntity().getMetadata("spawner_spawned").isEmpty()) {
					// let's grab the reward and distribute the spoils!
					Reward r = plugin.getReward(event.getEntityType());
					Player p = ((Damage) mv.value()).source;
					if (r.replaceLoot) {
						event.getDrops().clear();
					}
					double cash;
					String cashStr = "$0";
					if (plugin.econ.enabled()) {
						cash = r.getRewardAmount(p, plugin.config.moneyDecimalPlaces);
						if (r.playerStealsReward && event.getEntity() instanceof Player) {
							double max = plugin.econ.getBalance((Player) event.getEntity());
							if (max < cash) {
								cash = max;
							}
							plugin.econ.subtractMoney((Player) event.getEntity(), cash);
						}
						if (cash != Double.NaN && cash != 0) {
							cashStr = plugin.econ.format(cash);
							if (plugin.config.usePhysicalMoneyDrops && cash > 0) {
								ItemStack it = new ItemStack(plugin.config.moneyDropItem);
								ItemMeta m = it.getItemMeta();
								m.setDisplayName(plugin.config.moneyDropColor + cashStr);
								m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
								m.setUnbreakable(true);
								m.setLore(Arrays.asList(lorePrefix + cash));
								it.setItemMeta(m);

								Item eit = event.getEntity().getWorld().dropItemNaturally(event.getEntity().getEyeLocation(), it);
								eit.setCustomName(m.getDisplayName());
								eit.setCustomNameVisible(true);

							} else {
								if (cash < 0 && !plugin.econ.canAfford(p, -cash)) {
									plugin.econ.subtractMoney(p, plugin.econ.getBalance(p));
								} else if (cash < 0) {
									plugin.econ.subtractMoney(p, -cash);
								} else {
									plugin.econ.addMoney(p, cash);
								}
							}
						}
					}
					// and the other kill events:
					event.getDrops().addAll(r.getRewardLoot());
					event.setDroppedExp(r.getXP(event.getDroppedExp()));
					r.sendMessage(p, event.getEntity(), cashStr);
					r.runRewardCommands(p, plugin.commander, event.getEntity(), cashStr);
				}
			} else if (!plugin.config.allowNaturalDeathItemDrops) {
				// pretty sure this is a 'natural' death, so let's kill the loot
				event.getDrops().clear();
				event.setDroppedExp(0);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPickupItem(EntityPickupItemEvent event) {
		String cashStr;
		final ItemMeta itm = event.getItem().getItemStack().getItemMeta();
		if (itm.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)
				&& itm.hasLore()
				&& (cashStr = itm.getLore().get(0)).startsWith(lorePrefix)) {
			event.setCancelled(true);
			if (event.getEntity() instanceof Player) {
				event.getItem().remove();
				try {
					onMoneyPickup((Player) event.getEntity(), Double.parseDouble(cashStr.substring(lorePrefix.length())));
				} catch (NumberFormatException e) {
					plugin.getLogger().log(Level.SEVERE, "Unexpected Conflicting cash item found", e);
				}
			}
		}
	}

	void onMoneyPickup(Player p, double amount) {
		plugin.econ.addMoney(p, amount);
		p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, .8F, 20);
	}

	Runnable absorptionRunner = new Runnable() {
		@Override
		public void run() {
			String cashStr;
			for (World w : Bukkit.getWorlds()) {
				if (plugin.isEnabled(w)) {
					final List<Player> players = w.getPlayers();
					if (!players.isEmpty()) {
						for (Item eit : w.getEntitiesByClass(Item.class)) {
							final ItemMeta im = eit.getItemStack().getItemMeta();
							if (im.hasItemFlag(ItemFlag.HIDE_UNBREAKABLE)
									&& im.hasLore()
									&& (cashStr = im.getLore().get(0)).startsWith(lorePrefix)) {

								double x = eit.getLocation().getX(), z = eit.getLocation().getZ();
								double closest = 16;
								Player closestPlayer = null;
								for (Player p : players) {
									if (!p.isDead()) {
										final double dx = p.getLocation().getX() - x,
												dz = p.getLocation().getZ() - z,
												d = dx * dx + dz * dz;
										if (d < closest) {
											closest = d;
											closestPlayer = p;
										}
									}
								}
								if (closestPlayer != null) {
									// this player 'picks up' the reward
									eit.remove();
									try {
										onMoneyPickup(closestPlayer, Double.parseDouble(cashStr.substring(lorePrefix.length())));
									} catch (NumberFormatException e) {
										plugin.getLogger().log(Level.SEVERE, "Unexpected Conflicting cash item found", e);
									}
								}
							}
						}
					}
				}
			}
		}
	};

}
