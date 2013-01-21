package me.asofold.bpl.seamlessflight.settings.restrictions;

import java.util.HashMap;
import java.util.Map;

import me.asofold.bpl.seamlessflight.config.compatlayer.CompatConfig;
import me.asofold.bpl.seamlessflight.flymode.FlyState;

/**
 * 
 * @author mc_dev
 *
 */
public class ModeSettings {
	
	public final Map<FlyState, FlyStateSettings> states = new HashMap<FlyState, FlyStateSettings>();
	
	public ModeSettings(){
		for (FlyState state : FlyState.values()){
			if (state.isFlying){
				FlyStateSettings fsf = new FlyStateSettings();
				fsf.bypassPermission = fsf.bypassPermission + "." + state.name().toLowerCase();
				states.put(state, fsf);
			}
		}
	}
	
	public void toConfig(CompatConfig cfg, String prefix){
		for (FlyState state : FlyState.values()){
			if (state.isFlying){
				states.get(state).toConfig(cfg, prefix + state.name() + ".");
			}
		}
	}
	

	public void fromConfig(CompatConfig cfg, String prefix){
		for (FlyState state : FlyState.values()){
			if (state.isFlying){
				states.get(state).fromConfig(cfg, prefix + state.name() + ".");
			}
		}
	}
}
