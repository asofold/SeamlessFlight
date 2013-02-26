package me.asofold.bpl.seamlessflight.plshared;

import me.asofold.bpl.seamlessflight.plshared.players.OnlinePlayerMap;
import me.asofold.bpl.seamlessflight.plshared.players.PlayerMap;

import org.bukkit.entity.Player;

public class Players {
	private static final OnlinePlayerMap online = new OnlinePlayerMap();
	
	public static final PlayerMap getOnlinePlayerMap(){
		return online;
	}
	
	public static final Player getPlayerExact(final String name){
		return online.getPlayerExact(name);
	}
}
