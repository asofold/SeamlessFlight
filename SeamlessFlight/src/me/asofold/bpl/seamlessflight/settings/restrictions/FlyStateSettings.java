package me.asofold.bpl.seamlessflight.settings.restrictions;

import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfig;

public class FlyStateSettings {

	/** Reset period for time usage counting. */
	public long msReset = 0;
	
	/** Maximum flying time in this mode within msReset period. */
	public long msMaxFly = 0;
	
	public String bypassPermission = "seamlessflight.fly.bypass.maxtime";
	
	
	
	
	public void toConfig(CompatConfig cfg, String prefix) {
		cfg.set(prefix + "seconds-reset", (int) (msReset / 1000L));
		cfg.set(prefix + "seconds-max-fly", (int) (msMaxFly / 1000L));
	}

	public void fromConfig(CompatConfig cfg, String prefix) {
		FlyStateSettings ref = new FlyStateSettings();
		msReset = 1000L * cfg.getInt(prefix + "seconds-reset", (int) (ref.msReset / 1000L));
		msMaxFly= 1000L * cfg.getInt(prefix + "seconds-max-fly", (int) (ref.msMaxFly / 1000L));
	}

}
