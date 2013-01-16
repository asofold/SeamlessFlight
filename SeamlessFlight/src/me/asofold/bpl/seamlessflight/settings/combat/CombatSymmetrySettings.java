package me.asofold.bpl.seamlessflight.settings.combat;

import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfig;

/**
 * Sub settings for a certain symmetry.
 * @author mc_dev
 *
 */
public class CombatSymmetrySettings {
	public boolean allowCloseCombat = true;
	public boolean allowPotions = true;
	public boolean allowProjectiles = true;
	// allowPassiveEffects // (thorns etc. once it can be distinguished)
	
	
	public void toConfig(CompatConfig cfg, String prefix) {
		cfg.set(prefix + "allow-close-combat", allowCloseCombat);
		cfg.set(prefix + "allow-potions", allowPotions);
		cfg.set(prefix + "allow-projectiles", allowProjectiles);
	}
	
	public void fromConfig(CompatConfig cfg, String prefix) {
		CombatSymmetrySettings defaults = new CombatSymmetrySettings();
		allowCloseCombat = cfg.getBoolean(prefix + "allow-close-combat", defaults.allowCloseCombat);
		allowPotions = cfg.getBoolean(prefix + "allow-potions", defaults.allowPotions);
		allowProjectiles = cfg.getBoolean(prefix + "allow-projectiles", defaults.allowProjectiles);
	}
}
