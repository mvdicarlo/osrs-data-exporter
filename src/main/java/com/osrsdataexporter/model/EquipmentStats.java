package com.osrsdataexporter.model;

import lombok.Value;
import net.runelite.client.game.ItemEquipmentStats;

/**
 * Combat and defensive stats for an equipped item.
 * Values are sourced from {@code ItemEquipmentStats} via the RuneLite {@code ItemManager}.
 */
@Value
public class EquipmentStats
{
	// Attack bonuses
	int attackStab;
	int attackSlash;
	int attackCrush;
	int attackMagic;
	int attackRanged;

	// Defence bonuses
	int defenceStab;
	int defenceSlash;
	int defenceCrush;
	int defenceMagic;
	int defenceRanged;

	// Other bonuses
	int meleeStrength;
	int rangedStrength;
	float magicDamage;
	int prayer;

	// Misc
	int attackSpeed;

	/** Equipment slot index (0 = head, 1 = cape, 2 = neck, 3 = weapon, 4 = body, etc.). */
	int slot;

	/** Whether this is a two-handed weapon, occupying both the weapon and shield slots. */
	boolean twoHanded;

	/**
	 * Creates an {@code EquipmentStats} from a RuneLite {@link ItemEquipmentStats}.
	 *
	 * @param eq   the equipment stats returned by {@code ItemManager}
	 * @param slot the equipment slot index the item occupies
	 */
	public static EquipmentStats from(ItemEquipmentStats eq, int slot)
	{
		return new EquipmentStats(
			eq.getAstab(),
			eq.getAslash(),
			eq.getAcrush(),
			eq.getAmagic(),
			eq.getArange(),
			eq.getDstab(),
			eq.getDslash(),
			eq.getDcrush(),
			eq.getDmagic(),
			eq.getDrange(),
			eq.getStr(),
			eq.getRstr(),
			eq.getMdmg(),
			eq.getPrayer(),
			eq.getAspeed(),
			slot,
			eq.isTwoHanded()
		);
	}
}

