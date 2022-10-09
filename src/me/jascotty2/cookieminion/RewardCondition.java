package me.jascotty2.cookieminion;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;

public class RewardCondition {

	public final EntityType type;
	public String name = null, uuid = null, permission = null, world = null;
	public Boolean isBaby = null;

	public RewardCondition(EntityType type) {
		this.type = type;
	}

	public boolean matches(LivingEntity e) {
		if (name != null) {
			if (type == EntityType.PLAYER) {
				if (!e.getName().equalsIgnoreCase(name)) {
					return false;
				}
			} else {
				final String n = e.getCustomName() != null ? e.getCustomName() : e.getName();
				if (n == null || !n.equals(name)) {
					return false;
				}
			}
		}
		if (uuid != null) {
			if (e instanceof Player) {
				if (!((Player) e).getUniqueId().toString().equalsIgnoreCase(uuid)) {
					return false;
				}
			} else {
				return false;
			}
		}
		if (isBaby != null) {
			if (e instanceof Ageable) {
				if (isBaby == ((Ageable) e).isAdult()) {
					return false;
				}
			} else if (e instanceof Zombie) {
				if (isBaby != ((Zombie) e).isBaby()) {
					return false;
				}
			} else if (isBaby) {
				return false;
			}
		}
		if (world != null) {
			if(!world.equalsIgnoreCase(e.getWorld().getName())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return "RewardCondition{" + "type=" + type + ", name=" + name + ", uuid=" + uuid + ", permission=" + permission + ", world=" + world + ", isBaby=" + isBaby + '}';
	}

}
