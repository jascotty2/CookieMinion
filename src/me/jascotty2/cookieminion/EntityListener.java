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
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
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
import org.bukkit.event.inventory.InventoryPickupItemEvent;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
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
		short lootLevel;
		double damage;

		Damage(Player source) {
			this.source = source;
			this.attackTime = System.currentTimeMillis();
		}

		Damage(Player source, double damage, ItemStack hand) {
			this.source = source;
			this.damage = damage;
			this.attackTime = System.currentTimeMillis();
			lootLevel = (short) Math.min(3, hand == null || !hand.containsEnchantment(Enchantment.LOOT_BONUS_MOBS) ? 0 : 
					hand.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS));
		}

		void update(double damage, ItemStack hand) {
			this.damage += damage;
			this.attackTime = System.currentTimeMillis();
			lootLevel = (short) Math.min(3, hand == null || !hand.containsEnchantment(Enchantment.LOOT_BONUS_MOBS) ? 0 : 
					hand.getEnchantmentLevel(Enchantment.LOOT_BONUS_MOBS));
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
			playerDamage(event.getEntity(), pc, event.getDamage());
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onPotionSplashEvent(PotionSplashEvent event) {
		if (event.getEntity().getShooter() instanceof Player && plugin.isEnabled(event.getEntity().getLocation())) {
			Player p = (Player) event.getEntity().getShooter();
			for (LivingEntity e : event.getAffectedEntities()) {
				if (p != e) {
					e.setMetadata("CookieMonster_potionKiller", new FixedMetadataValue(plugin, p));
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onDamage(EntityDamageEvent event) {
		if ((event.getCause() == DamageCause.POISON
				|| event.getCause() == DamageCause.MAGIC
				|| event.getCause() == DamageCause.WITHER)
				&& event.getEntity() instanceof LivingEntity
				&& plugin.isEnabled(event.getEntity().getLocation())) {
			List<MetadataValue> mvs = event.getEntity().getMetadata("CookieMonster_potionKiller");
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
				playerDamage(event.getEntity(), (Player) mv.value(), event.getDamage());
			}
		}
	}

	void playerDamage(Entity entity, Player player, double damage) {
		// use the entity's datastore to track damage
		if (plugin.config.splitRewardsEvenly) {
			// check for existing data first
			List<MetadataValue> mvs = entity.getMetadata("CookieMonster_playerKiller");
			MetadataValue mv = null;
			if (mvs.size() > 1) {
				for (MetadataValue dat : mvs) {
					if (dat.getOwningPlugin().equals(plugin)) {
						mv = dat;
						break;
					}
				}
			} else if (!mvs.isEmpty()) {
				mv = mvs.get(0);
			}
			if (mv != null && mv.value() instanceof List) {
				List l = (List) mv.value();
				for (Object d : l) {
					if (d instanceof Damage && ((Damage)d).source.equals(player)) {
						((Damage)d).update(damage, player.getInventory().getItemInMainHand());
						return;
					}
				}
				l.add(new Damage(player, damage, player.getInventory().getItemInMainHand()));
			} else {
				LinkedList<Damage> dmg = new LinkedList<Damage>();
				dmg.add(new Damage(player, damage, player.getInventory().getItemInMainHand()));
				entity.setMetadata("CookieMonster_playerKiller", new FixedMetadataValue(plugin, dmg));
			}
		} else {
			entity.setMetadata("CookieMonster_playerKiller", new FixedMetadataValue(plugin, new Damage(player, 0, player.getInventory().getItemInMainHand())));
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
		final LivingEntity entity = event.getEntity();
		final Reward r;
		// armor stands are marked as living for some reason
		if (entity.getType() != EntityType.ARMOR_STAND
				&& plugin.isReward(event.getEntityType())
				&& plugin.isEnabled(entity.getLocation())
				&& (r = plugin.getReward(event.getEntity())) != null
				&& r.enabled) {

			// let's check if this was killed by a player
			List<MetadataValue> mvs = entity.getMetadata("CookieMonster_playerKiller");
			// clear metadata
			entity.removeMetadata("CookieMonster_playerKiller", plugin);
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
			// check to see if this entity was just attacked
			Damage latest = null;
			List<Damage> l = null;
			if (mv != null) {
				if (mv.value() instanceof Damage) {
					final Damage d = (Damage) mv.value();
					latest = d;
				} else if (mv.value() instanceof List && !(l = (List) mv.value()).isEmpty()) {
					for (Damage d : l) {
						if (latest == null || d.attackTime > latest.attackTime) {
							latest = d;
						}
					}
				}
			}
			boolean naturalDeath = !(latest != null && System.currentTimeMillis() - latest.attackTime < damageDelay);
			
			if(!naturalDeath && (r.condition != null && r.condition.permission != null)
					&& !latest.source.hasPermission(r.condition.permission))
				return;
			
			// check if rewards are allowed for this kill
			if (!naturalDeath && (plugin.config.allowMobSpawnerRewards
					|| entity.getMetadata("spawner_spawned").isEmpty())) {

				// let's grab the reward and distribute the spoils!
				
				if(entity instanceof Player) {
					final Location loc = entity.getLocation();
					for(ItemStack it : r.getRewardLoot(latest == null ? 0 : latest.lootLevel)) {
						if(it.getType() == Material.PLAYER_HEAD && ((SkullMeta) it.getItemMeta()).getOwner().equalsIgnoreCase("@victim")) {
							SkullMeta skull = (SkullMeta) it.getItemMeta();
							skull.setOwningPlayer((Player) entity);
							if(skull.getDisplayName() == null || skull.getDisplayName().isEmpty())
								skull.setDisplayName(ChatColor.YELLOW + ((Player) entity).getDisplayName() + "'s Head");
							it.setItemMeta(skull);
						}
						entity.getWorld().dropItemNaturally(loc, it);
					}
				} else {
					if (r.replaceLoot) {
						event.getDrops().clear();
					}
					event.getDrops().addAll(r.getRewardLoot(latest == null ? 0 : latest.lootLevel));
					event.setDroppedExp(r.getXP(event.getDroppedExp()));
				}

				// cash rewards
				if (plugin.econ.enabled()) {
					if (l != null) {
						// calculate portion
						double total = 0;
						for (Damage d : l) {
							total += d.damage;
						}
						// distribute rewards
						for (Damage d : l) {
							//if(r.condition == null || r.condition.permission == null || d.source.hasPermission(r.condition.permission))
							rewardCashForKill(entity, r, d.source, d.damage / total);
						}
					} else {
						rewardCashForKill(entity, r, ((Damage) mv.value()).source, 1);
					}
				}
			}
			if (naturalDeath && !plugin.config.allowNaturalDeathItemDrops) {
				// pretty sure this is a 'natural' death, so let's kill the loot
				event.getDrops().clear();
				event.setDroppedExp(0);
			}
		}
	}

	void rewardCashForKill(LivingEntity e, Reward r, Player p, double portion) {
		double cash;
		String cashStr = "$0";
		cash = r.getRewardAmount(p, plugin.config.moneyDecimalPlaces, portion);

		if (r.playerStealsReward && e instanceof Player) {
			double max = plugin.econ.getBalance((Player) e);
			if (max < cash) {
				cash = max;
			}
			plugin.econ.subtractMoney((Player) e, cash);
		}
		if (cash != Double.MIN_VALUE && cash != 0) {
			cashStr = plugin.econ.format(cash);
			if (plugin.config.usePhysicalMoneyDrops && cash > 0) {
				ItemStack it = new ItemStack(plugin.config.moneyDropItem);
				ItemMeta m = it.getItemMeta();
				m.setDisplayName(plugin.config.moneyDropColor + cashStr);
				m.addItemFlags(ItemFlag.HIDE_UNBREAKABLE);
				m.setUnbreakable(true);
				m.setLore(Arrays.asList(lorePrefix + cash));
				it.setItemMeta(m);

				Item eit = e.getWorld().dropItemNaturally(e.getEyeLocation(), it);
				eit.setCustomName(m.getDisplayName());
				eit.setCustomNameVisible(true);

				eit.setMetadata("CookieMonster_MoneyDrop", new FixedMetadataValue(plugin, p));

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
		if (!cashStr.matches(".0+(\\.0+)?")) {
			r.sendMessage(p, e, cashStr);
		}
		r.runRewardCommands(p, plugin.commander, e, cashStr);
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerPickupItem(EntityPickupItemEvent event) {
		String cashStr;
		final ItemMeta m;
        List<String> lore;
		if (/*event.getItem().hasMetadata("CookieMonster_MoneyDrop")
				&&*/ 
                (m = event.getItem().getItemStack().getItemMeta()) != null
                && (lore = m.getLore()) != null
                && !lore.isEmpty()
				&& (cashStr = lore.get(0)).startsWith(lorePrefix)) {
			event.setCancelled(true);
			if (event.getEntity() instanceof Player) {

				// check if this player can pick this up
				if (plugin.config.moneyDropOnlyForKiller) {
					final Player p;
					List<MetadataValue> meta = event.getItem().getMetadata("CookieMonster_MoneyDrop");
					if (!meta.isEmpty() && meta.get(0).value() instanceof Player
							&& (p = (Player) meta.get(0).value()).isOnline()
							&& !p.equals(event.getEntity())) {
						return;
					}
				}

				event.getItem().remove();
				try {
					onMoneyPickup((Player) event.getEntity(), Double.parseDouble(cashStr.substring(lorePrefix.length())));
				} catch (NumberFormatException e) {
					plugin.getLogger().log(Level.SEVERE, "Unexpected Conflicting cash item found", e);
				}
			}
		}
	}
	
	public void onInventoryPickupItemEvent(InventoryPickupItemEvent event) {
		final Item eit = event.getItem();
		if (eit.hasMetadata("CookieMonster_MoneyDrop")) {
			event.setCancelled(true);
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
							final ItemMeta im;
							if (eit.hasMetadata("CookieMonster_MoneyDrop")
									&& (im = eit.getItemStack().getItemMeta()).hasLore()
									&& (cashStr = im.getLore().get(0)).startsWith(lorePrefix)) {

								double x = eit.getLocation().getX(), z = eit.getLocation().getZ();
								double closest = 16;
								Player closestPlayer = null;
								boolean loop = true;

								if (plugin.config.moneyDropOnlyForKiller) {
									List<MetadataValue> meta = eit.getMetadata("CookieMonster_MoneyDrop");
									final Player p;
									if (!meta.isEmpty() && meta.get(0).value() instanceof Player
											&& (p = (Player) meta.get(0).value()).isOnline()) {
										// only check this one player, then
										loop = false;
										final double dx = p.getLocation().getX() - x,
												dz = p.getLocation().getZ() - z,
												d = dx * dx + dz * dz;
										if (d < closest) {
											closest = d;
											closestPlayer = p;
										}
									}
								}
								if (loop) {
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
