package me.asofold.bpl.seamlessflight.flymode;

import org.bukkit.Location;


/**
 * Processing result.
 * Initial settings should be such that nothing needs to be done.
 * @author mc_dev
 *
 */
public class FlyResult {
	/**
	 * FlyConfig has been changed.
	 * Refers to: hoverMode, isFlying, nofly
	 */
	public boolean configChanged = false;
	/**
	 * Remove player from survey (move events), i.e. flying really stopped.
	 */
	public boolean removeSurvey = false;
	
	public Location setTo = null;
	
	
	public FlyResult setConfigChanged(boolean c){
		configChanged = c;
		return this;
	}
	public FlyResult setRemoveSurvey(boolean r){
		removeSurvey = r;
		return this;
	}
	public void setTo(Location to) {
		this.setTo = to;
	}
}
