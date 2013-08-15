package me.asofold.bpl.seamlessflight.flymode;

/**
 * TODO: maybe use
 * @author mc_dev
 *
 */
public enum FlyState {
	DISABLED(false),
	OFF(false),
	HOVER(Constants.DEFAULT_FLY_SPEED),
	NORMAL(1.5f * Constants.DEFAULT_FLY_SPEED),
	SPEED(2.5f * Constants.DEFAULT_FLY_SPEED);
	
	public final boolean isFlying;
	
	public final float defaultFlySpeed;
	
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
		this(isFlying, Constants.DEFAULT_FLY_SPEED);
	}
	
	private FlyState(float defaultFlySpeed){
		this(true, defaultFlySpeed);
	}
	
	private FlyState(boolean isFlying, float defaultFlySpeed){
		this.isFlying = isFlying;
		this.defaultFlySpeed = defaultFlySpeed;
	}
}
