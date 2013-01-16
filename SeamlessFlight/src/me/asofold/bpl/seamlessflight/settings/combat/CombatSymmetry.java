package me.asofold.bpl.seamlessflight.settings.combat;

/**
 * Attacking symmetry. At least one player is flying.
 * @author mc_dev
 *
 */
public enum CombatSymmetry {
	AIR_SURFACE(0),
	AIR_AIR(1),
	SURFACE_AIR(2),
	AIR_OTHER(3),
	OTHER_AIR(4),
	;
	
	
	/** Index for CombatSettings. */
	public final int id;

	private CombatSymmetry(int id){
		this.id = id;
	}
	
	/**
	 * Inefficient (!).
	 * @param id
	 * @return null if nothing matching was found.
	 */
	public static CombatSymmetry getSymmetry(int id){
		for (final CombatSymmetry sym : CombatSymmetry.values()){
			if (sym.id == id) return sym;
		}
		return null;
	}

	/**
	 * NOTE: isFlying will be ignored for non players.
	 * @param isPlayer attacker
	 * @param isFlying attacker
	 * @param isPlayer2 attacker
	 * @param isFlying2 attacker
	 * @return null on invalid inputs.
	 */
	public static CombatSymmetry getSymmetry(boolean isPlayer, boolean isFlying, boolean isPlayer2, boolean isFlying2)
	{
		if (!isFlying && !isFlying2) return null;
		else if (!isPlayer && !isPlayer2) return null;
		else if (!isPlayer) return OTHER_AIR;
		else if (!isPlayer2) return AIR_OTHER;
		else if (isFlying) return isFlying2 ? AIR_AIR : AIR_SURFACE;
		else return SURFACE_AIR; 
	}
}
