package me.asofold.bpl.seamlessflight.flymode;


import me.asofold.bpl.seamlessflight.flymode.FlyConfig.Timing;
import me.asofold.bpl.seamlessflight.plshared.Blocks;
import me.asofold.bpl.seamlessflight.plshared.Players;
import me.asofold.bpl.seamlessflight.plshared.Teleport;
import me.asofold.bpl.seamlessflight.plshared.actions.ActionChecker;
import me.asofold.bpl.seamlessflight.plshared.actions.ActionType;
import me.asofold.bpl.seamlessflight.settings.Settings;
import me.asofold.bpl.seamlessflight.settings.restrictions.FlyStateSettings;
import me.asofold.bpl.seamlessflight.settings.restrictions.ModeSettings;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;


/**
 * Flying related methods. All static access for easy integration.
 * Final for ... errr .... 
 * TODO: refactor to not use clone !
 * TODO: Add boolean flags to FlyConfig and a global one for messages ?
 * TODO: Flags if to use defaultPermissions ! SO FAR : IGNORED
 * 
 * @author mc_dev
 *
 */
public abstract class FlyMode{
	/**
	 * This will be used for user inputs.
	 * Set your own ActionChecker instance, for processing another kind of event (delegate to onActionCallBack).
	 * Must be set, for at least actionChecker.getToggleState will be used.
	 */
	public final ActionChecker actionChecker = new FlyModeActionChecker(this);
	
	int actionCheckerId = -1;
	
	/**
	 * 
	 */
	public long msFlyCheck = 600;
	
	/**
	 * Name of the permission to check (root perm)
	 */
	public String rootPerm = "seamlessflight.fly";
	
	/**
	 * ms till next full check, important if extra polling is used.
	 * Set to a negative value to always full check. 
	 */
	public long msFullCheck = 43;
	
	/**
	 * Period for polling sneak states.
	 */
	public long periodActionChecker = 5;
	
	protected Settings settings;
	protected ModeSettings modeSettings;
	
	public FlyMode(Settings settings){
		setSettings(settings);
	}
	
	/**
	 * Override this.
	 * @param player
	 * @param perm
	 * @return
	 */
	public boolean hasPermission( Player player, String perm ){
		return player.hasPermission(perm);
	}
	
	/**
	 * Necessary for builtin ActionChecker.
	 * If you set your own actionChecker, then this method may remain empty.
	 * @param playerName 
	 * @return FlyConfig instance for the player. May be null if the player should be ignored.
	 */
	public abstract FlyConfig getFlyConfig(String playerName);

	/**
	 * This is an auxiliary method to easily install an own ActionChecker or use external events.
	 * Arguments define an event that triggers certain FlyMode behavior.
	 * @see ActionChecker#onAction
	 * @param user
	 * @param fold
	 * @param isActive
	 * @param hold
	 */
	public void onActionCallBack(String user, int fold, ActionType actionType) {
		final FlyConfig fc = getFlyConfig(user);
		if (fc == null) return;
		if (fc.flyState == FlyState.DISABLED) return;
		final Player player = Players.getPlayerExact(user);
		if (player == null) return; // ignore (inconsistency?)
		final boolean isFlying = fc.flyState != FlyState.OFF; // nofly already checked
		String message = null;
		if (isFlying){
			if ((player.getGameMode() == GameMode.CREATIVE) || !canFlyThrough(player)){
				// Ignore game mode for most.
				player.setFlySpeed(Constants.DEFAULT_FLY_SPEED);
				//player.setFallDistance(0.0f);
				fc.noFallBlock = Blocks.FBlockPos(player.getLocation());
				fc.tsNoFall = System.currentTimeMillis() + 8000;
				if (!player.getAllowFlight()) player.setAllowFlight(true);
				player.sendMessage(ChatColor.YELLOW+"FLY: "+ChatColor.YELLOW+"creative");
				return;
			}
		} else if (!canLiftOff(player)){
			return;
		} 
		else if (player.getGameMode() == GameMode.CREATIVE){
			if (!player.getAllowFlight()) player.setAllowFlight(true);
			return;
		}
		
		if (!hasPermission(player, rootPerm+".use")){
			return; // TODO: more fine grained defaultPermissions + fallback
		}
		
		boolean increase = false;
		switch  ( actionType){
		case LONG_RELEASE:
			if (fold == 17){ // Artificial (toggle flight).
				// speed increase or lift-off
				message = doSpeedIncrease(player, fc);
				increase = true;
			}
    		else {
    			// nothing, maybe change to hover in special mode, later.
    		}
			break;
		case LONG_START:
			if (fold >= 2){
				// lift off / change to hover
				message = doHoverMode(player, fc);
			} 
			else{
				// ignore
			}
			break;
		case SHORT_RELEASE:
			// tapping = speed decrease or switch to hover !
			if ( isFlying){
				message = doSpeedDecrease(player, fc);
			} else{
				if  (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getTypeId() == 0) message = doHoverMode(player, fc);
			}
			break;
		case CLICKING:
			if (fold > 1){
				if (!isFlying){
					if  (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getTypeId() == 0){
						message = doHoverMode(player, fc);
					}
				}
			}
			break;
		default: // For future compatibility.
			break;
		}
		
		if (fc.isFlying()){
			// Check validity:
			final FlyStateSettings fsSet = modeSettings.states.get(fc.flyState);
			if (fsSet != null && fsSet.msMaxFly > 0 && fsSet.msReset > 0){
				final Timing timing = fc.timings.get(fc.flyState);
				final long ts = System.currentTimeMillis();
				if (timing != null && timing.tsExpire > ts && timing.msUse > fsSet.msMaxFly
						&& !player.hasPermission(rootPerm + ".bypass.maxtime." + fc.flyState.name().toLowerCase())){
					// Invalid !
					final FlyState altFs = getAlternativeFlyState(player, fc, ts, increase);
					if (altFs == null){
						message = ChatColor.YELLOW + "FLY: " + ChatColor.GRAY + " (" + ChatColor.DARK_RED + fc.flyState.name().toLowerCase() + ChatColor.GRAY + ")";
						fc.abort = true;
					}
					else{
						message = ChatColor.YELLOW + "FLY: " + ChatColor.LIGHT_PURPLE + altFs.name().toLowerCase() + ChatColor.GRAY + " (" + ChatColor.DARK_RED + fc.flyState.name().toLowerCase() + ChatColor.GRAY + ")";
						// Set to alternate fly state.
						final float flySpeed = modeSettings.states.get(altFs).flySpeed;
						fc.flyState = altFs;
						if (player.getFlySpeed() != flySpeed) {
							player.setFlySpeed(flySpeed);
						}
					}
				}
			}
		}
		if (message != null) player.sendMessage(message);
	}
	
	public String doSpeedDecrease(Player player, FlyConfig fc) {
		if (!fc.isFlying()) return null;
		if (!player.getAllowFlight()) player.setAllowFlight(true);
		if (!player.isFlying()) player.setFlying(true);
		if ( fc.flyState == FlyState.SPEED ){
			// -> normal
			fc.flyState = FlyState.NORMAL;
		} 
		else if ( fc.flyState == FlyState.HOVER){
			// ignore
		} 
		else{
			// normal speed -> hover
			return doHoverMode(player, fc);
		}
		return null;
	}

	/**
	 * Also covers lift-off (!)
	 * @param player
	 * @param fc
	 */
	public String doHoverMode(Player player, FlyConfig fc) {
		String message = null;
		if ( !fc.isFlying()){
			// start in hover mode
			message = ChatColor.YELLOW+"FLY: "+ChatColor.GREEN+"on"; // TODO
		}
		fc.flyState = FlyState.HOVER;
		if (!player.getAllowFlight()) player.setAllowFlight(true);
		if (!player.isFlying()) player.setFlying(true);
		return message;
	}

	public String doSpeedIncrease(Player player, FlyConfig fc) {
		String message = null;
		if (!player.getAllowFlight()) player.setAllowFlight(true);
		if (!player.isFlying()) player.setFlying(true);
		if ( !fc.isFlying()){
			fc.setFlying(true);
			message = ChatColor.YELLOW+"FLY: "+ChatColor.GREEN+"on"; // TODO
		} else if ( fc.flyState == FlyState.HOVER){
			fc.flyState = FlyState.NORMAL;
		} else if (fc.flyState != FlyState.SPEED){
			fc.flyState = FlyState.SPEED;
			message = ChatColor.YELLOW+"FLY: "+ChatColor.LIGHT_PURPLE+"speed"; // TODO
		} // else ignore.
		return message;
	}
	
	/**
	 * Call this on player move events AND for every tick polling (then keep msFullCheck at a reasonable value<50)
	 * @param player
	 * @param config
	 * @param fromLoc may be null / UNUSED
	 * @param toLoc may be null / UNUSED
	 * @return
	 */
	public final FlyResult processMove(final Player player, final FlyConfig fc, final Location fromLoc, final Location toLoc, final int tick, final boolean forceFull){
		final long ts = System.currentTimeMillis();
		fc.tickProcess = tick;
		fc.sequence ++;
		final boolean fullCheck = forceFull || (ts > fc.tsAction); // if false: prevent double adding speed etc.
		if (fullCheck) fc.tsAction = ts+this.msFullCheck;
		// TODO: use full check
		final FlyResult res = new FlyResult();
		if (!fc.isFlying()){
			//abort = true;
			res.removeSurvey = true;
			fc.setFlying(false);
			res.configChanged = true;
			return res;
		}
		final Location loc = player.getLocation();	
		int y = loc.getBlockY();
		boolean abort = false;
		boolean stop = false;
		
		if (settings.checkStopAlways){
			if (settings.isStopId(loc) && !hasPermission(player, rootPerm+".bypass.stop.flythrough")){
				abort = true;
				stop = true;
			}
		}
		
		FlyStateSettings fsSet = modeSettings.states.get(fc.flyState);
		Timing timing = fc.timings.get(fc.flyState);
		if (timing == null){
			// New timing.
			timing = new Timing();
			timing.tsExpire = ts + fsSet.msReset;
			fc.timings.put(fc.flyState, timing);
		}
		if (fullCheck){
			timing.msUse += msFullCheck;
			// Somewhat inaccurate.
		}
		if ( ts-fc.tsPermCheck > msFlyCheck ){
			if (!player.getAllowFlight()) player.setAllowFlight(true);
			if (!player.isFlying()) player.setFlying(true);
			if (fsSet.msMaxFly > 0 && fsSet.msReset > 0){
				if (timing.tsExpire < ts){
					// Timing expired.
					timing.msUse = 0;
					timing.tsExpire = ts + fsSet.msReset;
				}
				else{
					// Valid timing, check if over time.
					if (timing.msUse > fsSet.msMaxFly){
						if (player.hasPermission(rootPerm + ".bypass.maxtime." + fc.flyState.name().toLowerCase())){
							// Ignore it.
							timing.msUse = 0;
							timing.tsExpire = ts + fsSet.msReset;
						}
						else{
							// TODO: Find the appropriate mode to use !
							final FlyState altFs = getAlternativeFlyState(player, fc, ts, false);
							if (altFs == null){
								player.sendMessage(ChatColor.YELLOW + "FLY: " + ChatColor.GRAY + " (" + ChatColor.DARK_RED + fc.flyState.name().toLowerCase() + ChatColor.GRAY + ")");
								abort = true;
							}
							else{
								player.sendMessage(ChatColor.YELLOW + "FLY: " + ChatColor.LIGHT_PURPLE + altFs.name().toLowerCase() + ChatColor.GRAY + " (" + ChatColor.DARK_RED + fc.flyState.name().toLowerCase() + ChatColor.GRAY + ")");
								// Set to alternate fly state.
								fc.flyState = altFs;
								fsSet = modeSettings.states.get(altFs);
								timing = fc.timings.get(altFs);
								if (timing == null){
									timing = new Timing();
									timing.tsExpire = ts + fsSet.msReset;
									fc.timings.put(altFs, timing);
								}
							}
						}
					}
				}
				// TODO: Check timings !
			}
			fc.tsPermCheck = ts;
			if ( !hasPermission(player, rootPerm+".use")) abort = true; // TODO: more fine grained defaultPermissions
			else if (settings.isStopId(loc)){
				if (!hasPermission(player, rootPerm+".bypass.stop.flythrough")){
					abort = true;
					stop = true;
					player.setFlying(false);
					player.setAllowFlight(false);
				}
			}
			else fc.abort = false;
			if (player.getGameMode() == GameMode.CREATIVE){
				abort = true;
				stop = true;
			}
		} else if (fc.abort) abort = true;
		
		if (abort){
			if (player.getFlySpeed() != Constants.DEFAULT_FLY_SPEED) {
				player.setFlySpeed(Constants.DEFAULT_FLY_SPEED);
			}
			if (player.getGameMode() == GameMode.CREATIVE){
				player.setAllowFlight(true);
				// Keep flying state.
			} else {
				player.setFlying(false);
			}
//			fc.tsPermCheck = ts - Math.min(msFlyCheck/2, 300); // TODO slight problem maybe
			fc.abort = true;
			fc.flyState = FlyState.NORMAL; // ?
			res.configChanged = true;
			// Check if is on ground or other and set to not flying.
			if (y>loc.getWorld().getMaxHeight()) stop = false;
			else if (Teleport.isTPPos(loc)) stop = true; 
			else if (y<0) stop = true;
			else{
				Block block = loc.getBlock();
				if ( block.getTypeId()!=0) stop = true;
				else if (block.getRelative(BlockFace.DOWN).getTypeId()!=0) stop = true;
			}
			if (stop){
				fc.setFlying(false);
//				player.setFallDistance(0.0f);
				res.configChanged = true;
				res.removeSurvey = true;
				fc.noFallBlock = Blocks.FBlockPos(loc);
				fc.tsNoFall = ts + 1500;
			}
			return res;
		} else fc.abort = false;
		// Now for real :)
		if (player.getFlySpeed() != fsSet.flySpeed) {
			player.setFlySpeed(fsSet.flySpeed);
		}
		boolean checkEnd = false;
		if (fc.stopCount > 0 || (fc.sequence % 4) == 3) {
			checkEnd = true;
		}
		if (checkEnd){
			if (checkStop(loc)){//isSafeStand(loc)){//.getBlock().getRelative(BlockFace.DOWN).getLocation())){
				fc.stopCount ++;
				if (fc.stopCount >= 40 || fc.stopCount >= 10 && fromLoc != null && fromLoc.distanceSquared(toLoc) <= 0.22 * 0.22) {
					// end flying
					fc.stopCount = 0;
					fc.setFlying(false);
					res.configChanged = true;
					res.removeSurvey = true;
//					player.setFallDistance(0.0f);
					fc.noFallBlock = Blocks.FBlockPos(loc);
					fc.tsNoFall = ts + 1500;
					return res;
				}
			} else {
				fc.stopCount = 0;
			}
		}
		
//		player.setFallDistance(0.0f);
		
		return res;
	}

	protected  FlyState getAlternativeFlyState(final Player player, final FlyConfig fc, final long ts, boolean increase) {
		switch(fc.flyState){
		case HOVER:
			if (canUseFlyState(player, fc, FlyState.NORMAL, ts)) return FlyState.NORMAL;
			else if (canUseFlyState(player, fc, FlyState.SPEED, ts)) return FlyState.SPEED;
			else return null;
		case NORMAL:
			if (increase){
				if (canUseFlyState(player, fc, FlyState.SPEED, ts)) return FlyState.SPEED;
				else if (canUseFlyState(player, fc, FlyState.HOVER, ts)) return FlyState.HOVER;
				else return null;
			}
			else{
				if (canUseFlyState(player, fc, FlyState.HOVER, ts)) return FlyState.HOVER;
				else if (canUseFlyState(player, fc, FlyState.SPEED, ts)) return FlyState.SPEED;
				else return null;
			}
		case SPEED:
			if (canUseFlyState(player, fc, FlyState.NORMAL, ts)) return FlyState.NORMAL;
			else if (canUseFlyState(player, fc, FlyState.HOVER, ts)) return FlyState.HOVER;
			else return null;
		default:
			throw new IllegalStateException("Bad FlyState: " + fc.flyState);
		}
		
	}
	
	private boolean canUseFlyState(final Player player, final FlyConfig fc, final FlyState state, final long ts){
		final Timing timing = fc.timings.get(state);
		final FlyStateSettings fsSet = modeSettings.states.get(state); 
		if (timing == null || fsSet.msMaxFly <= 0 || fsSet.msReset <= 0 || ts > timing.tsExpire || timing.msUse < fsSet.msMaxFly || player.hasPermission(rootPerm + ".bypass.maxtime." + state.name().toLowerCase())){
			return true;
		}
		else{
			return false;
		}
	}

	/**
	 * Return if the position is a stop position for landing (!).
	 * @param loc
	 * @return
	 */
	public boolean checkStop(Location loc){
		boolean safe = false; // not rreally safe but end of flight
		int y = loc.getBlockY();
		int id;
		Block block;
		final int ref = loc.getWorld().getMaxHeight() + 1;
		if ( y == ref){
			block = loc.getWorld().getBlockAt(loc.getBlockX(), y-1, loc.getBlockZ());
			id = block.getTypeId();
		} else if (y>ref) return false;
		else{
			block = loc.getBlock();
			block = block.getRelative(BlockFace.DOWN);
			id = block.getTypeId();
		}
		safe = Teleport.isSafeStand(id);
		return safe;
	}
	
	public boolean canLiftOff(final Player player){
		if (settings.isStopId(player.getLocation())){
			if (!hasPermission(player, rootPerm+".bypass.stop.liftoff")) return false;
		}
		return true;
	}

	public boolean canFlyThrough(final Player player){
		if (settings.isStopId(player.getLocation())){
			if (!hasPermission(player, rootPerm+".bypass.stop.flythrough")) return false;
		}
		return true;
	}
		
	public int registerActionChecker(Plugin plugin){
		BukkitScheduler sched = Bukkit.getServer().getScheduler();
		if (actionCheckerId != -1){
			if (sched.isQueued(actionCheckerId) || sched.isCurrentlyRunning(actionCheckerId)){
				sched.cancelTask(actionCheckerId);
			}
		}
		actionCheckerId = sched.scheduleSyncRepeatingTask(plugin, this.actionChecker, 0, periodActionChecker);
		return actionCheckerId;
	}
	
	public void cancelActionChecker(){
		if (actionCheckerId == -1) return;
		 Bukkit.getServer().getScheduler().cancelTask(actionCheckerId);
	}
	
	/**
	 * Use on login to update flying / allowFlight, ignores creative mode, allows null FlyConfig argument.
	 * @param player
	 * @param fc may be null
	 */
	public final void adapt(final Player player, final FlyConfig fc){
		if (player.getGameMode() == GameMode.CREATIVE){
			if (!player.getAllowFlight()) player.setAllowFlight(true);
			player.setFlySpeed(Constants.DEFAULT_FLY_SPEED);
			// TODO: check onground maybe and set flying.
			// TODO: setFlying(false)? maybe inconsistencies possible...
			return;
		}
		else if (fc != null && fc.isFlying() && !fc.abort){
			// Set to flying.
			player.setAllowFlight(true);
			player.setFlying(true);
			player.setFlySpeed(modeSettings.states.get(fc.flyState).flySpeed);
			// For safety also set noFallBlock.
			fc.noFallBlock = Blocks.FBlockPos(player.getLocation());
			fc.tsNoFall = System.currentTimeMillis();
		}
		else{
			// Allow toggling if just off.
			player.setAllowFlight(fc != null && fc.flyState == FlyState.OFF);
			player.setFlying(false);
		}
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
		modeSettings = settings.modes;
		// TODO: maybe use an array later, for block specs/flags ?
	}

}
