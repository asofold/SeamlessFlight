package me.asofold.bpl.seamlessflight.settings.combat;

import java.util.HashSet;
import java.util.Set;

import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfig;

/**
 * Combat-related settings.
 * 
 * 
 * TODO: concept  !
 * 
 * 
 * 
 * 
 * permission based control.
 * 
 * 
 * @author mc_dev
 *
 */
public class CombatSettings {
	
	public final Set<String> eventClassNames = new HashSet<String>(50);
	
	private final CombatSymmetrySettings[] symmetries = new CombatSymmetrySettings[5];
	
	/**
	 * Default id to use for out of range inputs.
	 */
	private final int defaultSymmetryId = CombatSymmetry.AIR_SURFACE.id;
	
	public CombatSettings(){
		for (int i = 0; i < symmetries.length; i ++){
			symmetries[i] = new CombatSymmetrySettings();
		}
	}
	
	/**
	 * 
	 * @param symmetry
	 * @return
	 */
	public CombatSymmetrySettings getSymmetrySettings(final CombatSymmetry symmetry){
		return symmetry == null ? symmetries[defaultSymmetryId] : getSymmetrySettings(symmetry.id);
	}
	
	/**
	 * 
	 * @param symmetry
	 * @return
	 */
	public CombatSymmetrySettings getSymmetrySettings(final int id){
		if (id < 0 || id >= symmetries.length) return symmetries[defaultSymmetryId];
		else return symmetries[id];
	}

	public void toConfig(CompatConfig cfg, String prefix) {
		for (int i = 0; i < symmetries.length; i++){
			CombatSymmetry sym = CombatSymmetry.getSymmetry(i);
			if (sym == null) continue;
			if (symmetries[i] == null) continue; // Just in case.
			symmetries[i].toConfig(cfg, prefix + "symmetry." + sym.name() + ".");
			
		}
	}
	
	public void fromConfig(CompatConfig cfg, String prefix) {
//		CombatSettings defaults = new CombatSettings();
		for (int i = 0; i < symmetries.length; i++){
			CombatSymmetry sym = CombatSymmetry.getSymmetry(i);
			if (sym == null) continue;
			if (symmetries[i] == null) continue; // Just in case.
			symmetries[i].fromConfig(cfg, prefix + "symmetry." + sym.name() + ".");
			
		}
	}
}
