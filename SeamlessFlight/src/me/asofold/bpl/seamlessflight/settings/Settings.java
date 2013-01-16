package me.asofold.bpl.seamlessflight.settings;

import java.io.File;

import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfig;
import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfigFactory;
import me.asofold.bpl.seamlessflight.config.compatlayer.ConfigUtil;
import me.asofold.bpl.seamlessflight.settings.combat.CombatSettings;

/**
 * Root config.
 * @author mc_dev
 *
 */
public class Settings {
	public final CombatSettings combat = new CombatSettings();
	
	public void toConfig(CompatConfig cfg){
		combat.toConfig(cfg, "combat.");
	}
	
	/**
	 * 
	 * @param cfg
	 * @return If values have been taken from the default settings.
	 */
	public void fromConfig(CompatConfig cfg){
		combat.fromConfig(cfg, "combat.");
	}
	
	// -----
	
	public static CompatConfig getDefaultConfig(){
		CompatConfig cfg = CompatConfigFactory.getConfig(null);
		new Settings().toConfig(cfg);
		return cfg;
	}
	

	
	/**
	 * Read the settings from a file, add missing entries if writeDefaults is set.
	 * @param file
	 * @param writeDefaults If to write back missing default entries.
	 * @return
	 */
	public static Settings readSettings(File file, boolean writeDefaults){
		CompatConfig defaults = getDefaultConfig();
		CompatConfig cfg = CompatConfigFactory.getConfig(file);
		Settings settings = new Settings();
		if (file.exists()){
			cfg.load();
		}
		if (ConfigUtil.forceDefaults(defaults, cfg) && writeDefaults){
			cfg.save();
		}
		settings.fromConfig(cfg);
		return settings;
	}
}
