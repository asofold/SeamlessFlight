package me.asofold.bpl.seamlessflight.flymode;

/**
 * TODO: maybe use
 * @author mc_dev
 *
 */
public enum FlyState {
	OFF(false),
	NORMAL,
	SPEED,
	HOVER,
	DISABLED(false);
	
	public final boolean isFlying;
	
	/**
	 * A flying state.
	 */
	private FlyState(){
		this(true);
	}

	/**
	 * 
	 * @param isFlying If this state means flying.
	 */
	private FlyState(boolean isFlying){
		this.isFlying = isFlying;
	}
}
