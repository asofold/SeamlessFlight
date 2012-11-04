package me.asofold.bpl.seamlessflight.plshared.flymode;

import me.asofold.bpl.seamlessflight.plshared.actions.ActionType;
import me.asofold.bpl.seamlessflight.plshared.actions.SneakActionChecker;

public class FlyModeActionChecker extends SneakActionChecker {

	private FlyMode flyMode;

	public FlyModeActionChecker(FlyMode flyMode) {
		this.flyMode = flyMode;
	}

	@Override
	public void onAction(String user, int fold, ActionType actionType) {
		flyMode.onActionCallBack(user, fold, actionType);
	}

}
