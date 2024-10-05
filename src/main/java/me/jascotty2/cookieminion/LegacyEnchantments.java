package me.jascotty2.cookieminion;

import org.bukkit.enchantments.Enchantment;

public class LegacyEnchantments {
	
	public static Enchantment getById(int id) {
        return switch (id) {
            case 0 -> Enchantment.PROTECTION;
            case 1 -> Enchantment.FIRE_PROTECTION;
            case 2 -> Enchantment.FEATHER_FALLING;
            case 3 -> Enchantment.BLAST_PROTECTION;
            case 4 -> Enchantment.PROJECTILE_PROTECTION;
            case 5 -> Enchantment.RESPIRATION;
            case 6 -> Enchantment.AQUA_AFFINITY;
            case 7 -> Enchantment.THORNS;
            case 8 -> Enchantment.DEPTH_STRIDER;
            case 9 -> Enchantment.FROST_WALKER;
            case 10 -> Enchantment.BINDING_CURSE;
            case 16 -> Enchantment.SHARPNESS;
            case 17 -> Enchantment.SMITE;
            case 18 -> Enchantment.BANE_OF_ARTHROPODS;
            case 19 -> Enchantment.KNOCKBACK;
            case 20 -> Enchantment.FIRE_ASPECT;
            case 21 -> Enchantment.LOOTING;
            case 22 -> Enchantment.SWEEPING_EDGE;
            case 32 -> Enchantment.EFFICIENCY;
            case 33 -> Enchantment.SILK_TOUCH;
            case 34 -> Enchantment.UNBREAKING;
            case 35 -> Enchantment.FORTUNE;
            case 48 -> Enchantment.POWER;
            case 49 -> Enchantment.PUNCH;
            case 50 -> Enchantment.FLAME;
            case 51 -> Enchantment.INFINITY;
            case 61 -> Enchantment.LUCK_OF_THE_SEA;
            case 62 -> Enchantment.LURE;
            default -> null;
        };
    }
}
