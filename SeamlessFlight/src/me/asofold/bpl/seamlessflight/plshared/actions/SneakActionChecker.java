package me.asofold.bpl.seamlessflight.plshared.actions;

import me.asofold.bpl.seamlessflight.plshared.Players;

import org.bukkit.entity.Player;

public abstract class SneakActionChecker extends ActionChecker {

	@Override
	public boolean getToggleState(String user) {
		Player player = Players.getPlayerExact(user);
		if (player == null){
			removeUserData(user);
			return false;
		}
		return player.isSneaking();
	}

	@Override
	public abstract void onAction(String user, int fold, ActionType actionType);

}
