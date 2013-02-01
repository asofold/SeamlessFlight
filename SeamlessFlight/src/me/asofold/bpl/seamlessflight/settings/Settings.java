package me.asofold.bpl.seamlessflight.settings;

import java.io.File;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

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
	
	/** Add all solid blocks to stop ids (not written back to config). */
	public boolean stopSolid = false;
	/** If to always check if flying through stop id blocks. */
	public boolean checkStopAlways = false;
	
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
		cfg.set("stop-solid", stopSolid);
		cfg.set("check-stop-always", checkStopAlways);
	}
	
	public void fromConfig(CompatConfig cfg){
		Settings ref = new Settings();
		combat.fromConfig(cfg, "combat.");
		modes.fromConfig(cfg, "modes.");
		stopIds.clear();
		stopIds.addAll(cfg.getIntegerList("stop-ids", new LinkedList<Integer>(ref.stopIds)));
		stopSolid = cfg.getBoolean("stop-solid", ref.stopSolid);
		checkStopAlways = cfg.getBoolean("check-stop-always", ref.checkStopAlways);
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
	
	/**
	 * Check if a location is to stop flying (through).
	 * @param loc
	 * @return
	 */
	public boolean isStopId(final Location loc) {
		final World world = loc.getWorld();
		if  (loc.getY() > world.getMaxHeight()) return false;
		final int id = world.getBlockTypeIdAt(loc.getBlockX(), loc.getBlockY(), loc.getBlockZ());
		if (id == 0) return false; // Quick return for most cases.
		if (stopIds.contains(id)) return true;
		if (stopSolid){
			final Material mat = Material.getMaterial(id);
			if (mat != null && mat.isSolid()) return true;
		}
		// Otherwise let it pass.
		return false;
	}

}
