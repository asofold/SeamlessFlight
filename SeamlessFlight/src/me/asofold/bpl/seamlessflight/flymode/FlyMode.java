package me.asofold.bpl.seamlessflight.flymode;


import java.util.HashSet;
import java.util.Set;

import me.asofold.bpl.seamlessflight.flymode.FlyConfig.Timing;
import me.asofold.bpl.seamlessflight.plshared.Blocks;
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
import org.bukkit.util.Vector;


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
	
	/**
	 * Extra blocks that stop flying on checks.
	 */
	public final Set<Integer> stopIds = new HashSet<Integer>();
	
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
	
	/** Acceleration factor for normal mode. */
	public double fNormal = 1.1;
	/** Maximum velocity for normal mode. */
	public double maxNormal = 0.8;
	/** Acceleration factor for speed mode. */
	public double fSpeed = 1.2;
	/** Maximum velocity for speed mode. */
	public double maxSpeed = 1.6;
	
	public boolean smoothing = true;
	public double smoothingWeight = 0.25;
	
	protected Settings settings;
	protected ModeSettings modeSettings;
//	private final Random random = new Random(System.currentTimeMillis()-113);
//	private final double incHovSPazz = 0.1;
	
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
		FlyConfig fc = getFlyConfig(user);
		if ( fc == null) return;
		if ( fc.flyState == FlyState.DISABLED) return;
		Player player = Bukkit.getServer().getPlayerExact(user);
		if ( player == null) return; // ignore (inconsistency?)
		boolean isFlying = fc.flyState != FlyState.OFF; // nofly already checked
		String message = null;
		if ( isFlying){
			if ((player.getGameMode() == GameMode.CREATIVE) || !canFlyThrough(player)){
				fc.setFlying(false);
				//player.setFallDistance(0.0f);
				fc.noFallBlock = Blocks.FBlockPos(player.getLocation());
				fc.tsNoFall = System.currentTimeMillis() + 1500;
				if (!player.getAllowFlight()) player.setAllowFlight(true);
				player.sendMessage(ChatColor.YELLOW+"FLY: "+ChatColor.RED+"off");
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
			// nothing, maybe change to hover in special mode, later. 
			break;
		case LONG_START:
			if ( fold == 2){
				// speed increase or lift-off
				message = doSpeedIncrease(player, fc);
				increase = true;
			} 
			else if (fold>=3){
				// lift off / change to hover
				message = doHoverMode(player, fc);
			} 
			else{
				// ignore
			}
			break;
		case SHORT_RELEASE:
			if ( fold == 1){
				// single tap = speed decrease or switch to hover !
				if ( isFlying){
					message = doSpeedDecrease(player, fc);
				} else{
					if  (player.getLocation().getBlock().getRelative(BlockFace.DOWN).getTypeId() == 0) message = doHoverMode(player, fc);
				}
			} 
			else if (fold >= 2){
				// switch to hover 
				if (isFlying) message = doHoverMode(player, fc);
			} 
			else{
				// ignore
			}
			break;
		case CLICKING:
			if ( fold >1){
				if ( !isFlying){
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
						if (fc.flyState == FlyState.HOVER){
							player.setFlying(false);
						}
						else if (altFs == FlyState.HOVER){
							player.setFlying(true);
							fc.hoverY = player.getLocation().getY();
						}
						fc.flyState = altFs;
					}
				}
			}
		}
		if (message != null) player.sendMessage(message);
	}
	
	public String doSpeedDecrease(Player player, FlyConfig fc) {
		if (!fc.isFlying()) return null;
		if ( fc.flyState == FlyState.SPEED ){
			// -> normal
			fc.flyState = FlyState.NORMAL;
			if (!player.getAllowFlight()) player.setAllowFlight(true);
			if (player.isFlying()) player.setFlying(false);
		} 
		else if ( fc.flyState == FlyState.HOVER){
			// ignore
			if (!player.getAllowFlight()) player.setAllowFlight(true);
			if (!player.isFlying()) player.setFlying(true);
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
		fc.hoverY = player.getLocation().getY();
		if (!player.getAllowFlight()) player.setAllowFlight(true);
		if (!player.isFlying()) player.setFlying(true);
		return message;
	}

	public String doSpeedIncrease(Player player, FlyConfig fc) {
		String message = null;
		if (!player.getAllowFlight()) player.setAllowFlight(true);
		if ( !fc.isFlying()){
			fc.setFlying(true);
			message = ChatColor.YELLOW+"FLY: "+ChatColor.GREEN+"on"; // TODO
		} else if ( fc.flyState == FlyState.HOVER){
			fc.flyState = FlyState.NORMAL;
			if (player.isFlying()) player.setFlying(false);
		} else if (fc.flyState != FlyState.SPEED){
			fc.flyState = FlyState.SPEED;
			if (player.isFlying()) player.setFlying(false);
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
	public final FlyResult processMove(final Player player, final FlyConfig fc, final Location fromLoc, final Location toLoc, final boolean forceFull){
		final long ts = System.currentTimeMillis();
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
			if (fc.flyState == FlyState.HOVER){
				if (!player.isFlying()) player.setFlying(true);
			}
			else{
				if (player.isFlying()) player.setFlying(false);
			}
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
								if (fc.flyState == FlyState.HOVER){
									player.setFlying(false);
								}
								else if (altFs == FlyState.HOVER){
									player.setFlying(true);
									fc.hoverY = player.getLocation().getY();
								}
								fc.flyState = altFs;
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
				}
			}
			else fc.abort = false;
			if (player.getGameMode() == GameMode.CREATIVE){
				abort = true;
				stop = true;
			}
		} else if (fc.abort) abort = true;
		
		if (abort){
			player.setFlying(false);
			player.setAllowFlight(player.getGameMode() == GameMode.CREATIVE);
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
		Vector v = player.getVelocity(); // eTo.subtract(fromLoc).toVector();
		// TODO: Make constants configurable and/or player/whatever-dependent + RENAME!
		final double vY = v.getY();
//		double vX = v.getX();
		final double l = v.length();
		boolean checkEnd = false;
		final double cY = loc.getY();
		final double cZ = loc.getZ();
		final double cX = loc.getX();
		Vector resV = null;
		if (actionChecker.getToggleState(player.getName())){ 
			if (!fullCheck) return res; // No double speed up / accel
			final double aF ;
			final double aMax;
			final double aH = 0.2;
			final Vector vBase;
			vBase = loc.getDirection().normalize();
			if ( fc.flyState == FlyState.SPEED){
				// TODO cleanup / find something simpler
				aF =  fSpeed;	
				aMax = maxSpeed;
			} else if ( fc.flyState == FlyState.HOVER){
				// mild speed
				aF = 0.0;
				aMax = 0.0;
			} else{
				// mild speed
				aF = fNormal;
				aMax = maxNormal;
			}
			if (aF != 0.0){
				resV = vBase.multiply(l*aF);
				if ( smoothing ) resV.add(v.multiply(smoothingWeight));
				if ( resV.getY() > -0.1 ) resV = resV.add(new Vector(0.0,aH,0.0));
				if ( resV.lengthSquared() >aMax ){
					resV = resV.normalize().multiply(aMax);
				}
			}
			else{
				resV = null;
			}
			fc.hoverY = ((int) cY);
		} else{
			// rather hover mode or just descending slowly
			if ( fc.flyState == FlyState.HOVER){
				resV = null;
//				// TODO: reorganize for efficiency (with fullCheck)
//				if ( fc.hoverY == -10000) fc.hoverY = cY;
//				// LILYPADS:
				final double d = Math.abs(cX-fc.lastX)+Math.abs(cZ-fc.lastZ); //Math.max(Math.abs(cX-fc.lastX)+Math.abs(cZ-fc.lastZ), Math.abs(vX)+Math.abs(vY));
////				Block block = loc.getBlock();
////				int id = block.getTypeId();
				final double dY = cY-fc.hoverY;
//				if ( dY<0 ){
//					// ASCEND
//					if ( ts < fc.tsExpireHover){
//						v = v.setY(Math.max(0.4, vY));
//						resV = v;
//					} else {
//						v = v.setY(Math.max(0.2, vY));
//						fc.tsExpireHover = ts + 300; // TODO: maybe remove
//						resV = v;
//					}
//				} else{ 
//					// Not moving fast, horizontally
					if ((Math.abs(cY-fc.lastY)<0.001)){
						// stationary...
						if (d<0.001){
							if ( forceFull ) checkEnd = true;
						}
					} else if ( (vY<0) && (dY<0.05)){
						v.setY(0.0);
						resV = v;
						if (d<0.001){
							if ( forceFull ) checkEnd = true;
						}
					}
//					else if ( d<0.05){
////						FlyUtil.refreshLilyPadMinimal(player, fc);
//						if (random.nextInt(1000)<2){
//							if (vY>=0) v = v.setY(vY+incHovSPazz);
//							else{
//								v.setY(incHovSPazz);
//							}
//							resV = v;
//						}
//					}
//					else {
//						fc.tsExpireHover = ts + 830;
//					}
//				} 
			} else if (fc.flyState == FlyState.SPEED){
				// break: y towards zero 
				v = v.setY(v.getY() * .75); // TODO
				if (l<0.12 && (Math.abs(cY-fc.lastY)<0.001)) checkEnd = true;
				resV = v;
			} else{
//				if ( (toLoc!= null) && (fromLoc != null)){
//					Vector addV = toLoc.toVector().subtract(fromLoc.toVector()).normalize().multiply(0.2);
//					v = v.add(addV);
//				}
				// fc.speedMode = false;
				// break: y towards zero 
				if (l<0.12 && (Math.abs(cY-fc.lastY)<0.001)) checkEnd = true;
				v = v.setY(v.getY() * .75); // TODO
				resV = v;
			}
		}
		
		if ( checkEnd ){
			if (checkStop(loc)){//isSafeStand(loc)){//.getBlock().getRelative(BlockFace.DOWN).getLocation())){
				// end flying
				fc.setFlying(false);
				res.configChanged = true;
				res.removeSurvey = true;
//				player.setFallDistance(0.0f);
				fc.noFallBlock = Blocks.FBlockPos(loc);
				fc.tsNoFall = ts + 1500;
				player.setAllowFlight(player.getGameMode() == GameMode.CREATIVE);
				player.setFlying(false);
				//event.setTo(loc);
				return res;
			}
		}
		
		// if (vY<0 && v.lengthSquared()<tol) player.setVelocity(new Vector(0,0,0)); // TODO
//		player.setFallDistance(0.0f);
		fc.lastVY = vY; // TODO: or v.getY
		fc.lastY = cY;
		fc.lastX = cX;
		fc.lastZ = cZ;
		if ( resV != null ) player.setVelocity(resV);

//		Location to = event.getFrom().add(v);
//		
//		to.setPitch(eTo.getPitch());
//		to.setYaw(eTo.getYaw());
		// event.setTo(to);
		// TODO: event.setTo() ?
		
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
		}
		throw new IllegalStateException("Bad FlyState: " + fc.flyState);
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
			if ( sched.isQueued(actionCheckerId) || sched.isCurrentlyRunning(actionCheckerId)){
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
			// TODO: setFlying(false)? maybe inconsistencies possible...
			return;
		}
		else if (fc != null && fc.isFlying()){
			// Set to flying.
			player.setAllowFlight(true);
			player.setFlying(fc.flyState == FlyState.HOVER);
			// For safety also set noFallBlock.
			fc.noFallBlock = Blocks.FBlockPos(player.getLocation());
			fc.tsNoFall = System.currentTimeMillis();
		}
		else{
			player.setAllowFlight(false);
			player.setFlying(false);
		}
	}

	public void setSettings(Settings settings) {
		this.settings = settings;
		modeSettings = settings.modes;
		// Fill in some values for fast access.
		// TODO: maybe use an array later, for block specs/flags ?
		stopIds.clear();
		stopIds.addAll(settings.stopIds);
	}

}
