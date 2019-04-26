package me.jascotty2.cookieminion;

import org.bukkit.entity.Ageable;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;

public class RewardCondition {
	public final EntityType type;
	public String name = null;
	public Boolean isBaby = null;
	public RewardCondition(EntityType type) {
		this.type = type;
	}
	
	public boolean matches(LivingEntity e) {
		if(name != null) {
			if(type == EntityType.PLAYER) {
				if(!e.getName().equalsIgnoreCase(name))
					return false;
			} else {
				final String n = e.getCustomName() != null ? e.getCustomName() : e.getName();
				if(n == null || !n.equals(name))
					return false;
			}
		}
		if(isBaby != null) {
			if(e instanceof Ageable) {
				if(((Ageable) e).isAdult() == isBaby)
					return false;
			} else if(isBaby)
				return false;
		}
		return true;
	}
}
