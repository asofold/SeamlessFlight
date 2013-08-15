package me.asofold.bpl.seamlessflight.flymode;

import java.util.HashMap;
import java.util.Map;

import me.asofold.bpl.seamlessflight.plshared.Blocks;
import me.asofold.bpl.seamlessflight.plshared.blocks.FBlockPos;


/**
 * Save in PlayerData (!) to have a default value: isFlying, nofly, hoverMode
 * TODO: Cleanup access methods (so using them is ok, without need for extras).
 * @author mc_dev
 *
 */
public class FlyConfig {
	
	protected static class Timing{
		public long tsExpire = 0;
		public long msUse = 0;
	}

	public FlyState flyState = FlyState.DISABLED; // default
	
	/** Last tick processMove was called. */
	public int tickProcess = 0;
	public long tsPermCheck = 0;
	// TODO: other stuff, smoothing, vert+horiz-speed/slope, ...
	
	
	public final Map<FlyState, Timing> timings = new HashMap<FlyState, Timing>();
	
	public boolean abort = false;
	
	public long sequence = 0;
	
	public int stopCount = 0; 
	
	/**
	 * TODO: maybe get rid of this block
	 */
	public FBlockPos noFallBlock = null;
	/**
	 * Don't take fall damage till then.
	 */
	public long tsNoFall = 0;
	
	/**
	 * Optional: if set, not all action will be performed unless expired.
	 * Use this to add polling.
	 */
	public long tsAction = 0;
	
	public boolean toggle(){
		stopCount = 0;
		if (flyState == FlyState.DISABLED){
			setFlying(false);
			return false;
		}
		if (flyState == FlyState.OFF){
			setFlying(true);
			return true;
		}
		else{
			setFlying(false);
			return false;
		}
	}
	
	/**
	 * NOTE: This can not remove fake lilypads !
	 * @param flying
	 */
	public void  setFlying(boolean flying){
		// TODO: adjustments for state changes ?
		// TODO: cleanups
		stopCount = 0;
		if (flyState == FlyState.DISABLED) flying = false;
		if (!flying){
			flyState = FlyState.OFF;
			abort = false;
		} else{
			 // TODO
			flyState = FlyState.NORMAL;
			noFallBlock = null;
		}
	}
	
	
	/**
	 * Returns true if disabled.
	 * @return
	 */
	public boolean toggleNofly(){
		stopCount = 0;
		setNofly(!(flyState == FlyState.DISABLED));
		if ( flyState == FlyState.DISABLED ) {
			return true;
		} else{
			return false;
		}
	}
	
	public void setNofly(boolean nofly){
		stopCount = 0;
		if (nofly){
			setFlying(false);
			flyState = FlyState.DISABLED;
		}
		else{
			if (flyState == FlyState.DISABLED){
				setFlying(true);
			}
		}
	}
	
	public final boolean isFlying(){
		return flyState != FlyState.DISABLED && flyState != FlyState.OFF;
	}	
	
	/**
	 * Check if to use fall damage and reset location.
	 * Does not check isFlying() !
	 * @param w
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	public boolean useFallDamage(String w, int x, int y, int z){
		if (System.currentTimeMillis() < tsNoFall) return false;
		if ( noFallBlock == null ) return true;
		boolean res = Blocks.exceedsDistance(noFallBlock, w, x, y, z, 1);
		noFallBlock = null;
		return res;
	}
}
