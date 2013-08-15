package me.asofold.bpl.seamlessflight.settings.restrictions;

import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfig;
import me.asofold.bpl.seamlessflight.flymode.FlyState;

public class FlyStateSettings {

	/** Reset period for time usage counting. */
	public long msReset = 0;
	
	/** Maximum flying time in this mode within msReset period. */
	public long msMaxFly = 0;
	
	public float flySpeed = 0.1f;
	
	public FlyStateSettings(FlyState state) {
		flySpeed = state.defaultFlySpeed;
	}

	public void toConfig(CompatConfig cfg, String prefix, FlyState state) {
		cfg.set(prefix + "seconds-reset", (int) (msReset / 1000L));
		cfg.set(prefix + "seconds-max-fly", (int) (msMaxFly / 1000L));
		cfg.set(prefix + "fly-speed", (double) flySpeed);
	}

	public void fromConfig(CompatConfig cfg, String prefix, FlyState state) {
		FlyStateSettings ref = new FlyStateSettings(state);
		msReset  = 1000L * cfg.getInt(prefix + "seconds-reset", (int) (ref.msReset / 1000L));
		msMaxFly = 1000L * cfg.getInt(prefix + "seconds-max-fly", (int) (ref.msMaxFly / 1000L));
		flySpeed = cfg.getDouble(prefix + "fly-speed", (double) state.defaultFlySpeed).floatValue(); 
	}

}
