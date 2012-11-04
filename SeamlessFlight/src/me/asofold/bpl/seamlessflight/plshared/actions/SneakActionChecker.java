package me.asofold.bpl.seamlessflight.plshared.actions;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public abstract class SneakActionChecker extends ActionChecker {

	@Override
	public boolean getToggleState(String user) {
		Player player = Bukkit.getServer().getPlayerExact(user);
		if (player == null){
			removeUserData(user);
			return false;
		}
		return player.isSneaking();
	}

	@Override
	public abstract void onAction(String user, int fold, ActionType actionType);

}
