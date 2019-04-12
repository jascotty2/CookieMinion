package me.jascotty2.cookieminion;

import org.bukkit.enchantments.Enchantment;

public class LegacyEnchantments {
	
	public static Enchantment getById(int id) {
		switch(id) {
			case 0:
				return Enchantment.PROTECTION_ENVIRONMENTAL;
			case 1:
				return Enchantment.PROTECTION_FIRE;
			case 2:
				return Enchantment.PROTECTION_FALL;
			case 3:
				return Enchantment.PROTECTION_EXPLOSIONS;
			case 4:
				return Enchantment.PROTECTION_PROJECTILE;
			case 5:
				return Enchantment.OXYGEN;
			case 6:
				return Enchantment.WATER_WORKER;
			case 7:
				return Enchantment.THORNS;
			case 8:
				return Enchantment.DEPTH_STRIDER;
			case 9:
				return Enchantment.FROST_WALKER;
			case 10:
				return Enchantment.BINDING_CURSE;
			case 16:
				return Enchantment.DAMAGE_ALL;
			case 17:
				return Enchantment.DAMAGE_UNDEAD;
			case 18:
				return Enchantment.DAMAGE_ARTHROPODS;
			case 19:
				return Enchantment.KNOCKBACK;
			case 20:
				return Enchantment.FIRE_ASPECT;
			case 21:
				return Enchantment.LOOT_BONUS_MOBS;
			case 22:
				return Enchantment.SWEEPING_EDGE;
			case 32:
				return Enchantment.DIG_SPEED;
			case 33:
				return Enchantment.SILK_TOUCH;
			case 34:
				return Enchantment.DURABILITY;
			case 35:
				return Enchantment.LOOT_BONUS_BLOCKS;
			case 48:
				return Enchantment.ARROW_DAMAGE;
			case 49:
				return Enchantment.ARROW_KNOCKBACK;
			case 50:
				return Enchantment.ARROW_FIRE;
			case 51:
				return Enchantment.ARROW_INFINITE;
			case 61:
				return Enchantment.LUCK;
			case 62:
				return Enchantment.LURE;
		}
		return null;
	}
}
