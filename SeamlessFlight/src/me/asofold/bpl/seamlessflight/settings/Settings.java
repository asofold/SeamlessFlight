package me.asofold.bpl.seamlessflight.settings;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfig;
import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfigFactory;
import me.asofold.bpl.seamlessflight.config.compatlayer.ConfigUtil;
import me.asofold.bpl.seamlessflight.settings.combat.CombatSettings;
import me.asofold.bpl.seamlessflight.settings.restrictions.ModeSettings;

/**
 * Root config.
 * @author mc_dev
 *
 */
public class Settings {
	public final CombatSettings combat = new CombatSettings();
	
	public final ModeSettings modes = new ModeSettings();
	
	public final Set<Integer> stopIds = new HashSet<Integer>();
	
	public Settings(){
		for ( Integer id : new int[]{
			    8, 9, // water
			    10, 11, // lava
			    30, // cobweb
			    65, // ladder
			    106, // Vines
			    119, // end portal
			}){
				stopIds.add(id);
			}
	}
	
	public void toConfig(CompatConfig cfg){
		combat.toConfig(cfg, "combat.");
		modes.toConfig(cfg, "modes.");
		cfg.set("stop-ids", new LinkedList<Integer>(stopIds));
	}
	
	public void fromConfig(CompatConfig cfg){
		Settings ref = new Settings();
		combat.fromConfig(cfg, "combat.");
		modes.fromConfig(cfg, "modes.");
		stopIds.clear();
		stopIds.addAll(cfg.getIntegerList("stop-ids", new LinkedList<Integer>(ref.stopIds)));
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
