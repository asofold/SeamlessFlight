package me.asofold.bpl.seamlessflight.plshared.actions;

public enum ActionType {
	/**
	 * Release after holding long.
	 */
	LONG_RELEASE,
	/**
	 * Release after tapping shortly.
	 */
	SHORT_RELEASE,
	/**
	 * Start of holding long.
	 */
	LONG_START,
	/**
	 * Used while clicking, other events might follow this one for releasing (!)!
	 */
	CLICKING,
	;
}
