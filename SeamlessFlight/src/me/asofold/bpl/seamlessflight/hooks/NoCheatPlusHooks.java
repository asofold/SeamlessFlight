package me.asofold.bpl.seamlessflight.hooks;

import me.asofold.bpl.seamlessflight.SeamlessFlight;

import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.checks.CheckType;
import fr.neatmonster.nocheatplus.checks.access.IViolationInfo;
import fr.neatmonster.nocheatplus.checks.moving.MovingData;
import fr.neatmonster.nocheatplus.checks.moving.Velocity;
import fr.neatmonster.nocheatplus.hooks.AbstractNCPHook;
import fr.neatmonster.nocheatplus.hooks.NCPHookManager;

/**
 * Hook stuff for NoCheatPlus.
 * @author mc_dev
 *
 */
public class NoCheatPlusHooks {
	
	protected static CheckType[] checkTypes = new CheckType[]{
		CheckType.MOVING_NOFALL,
		CheckType.MOVING_SURVIVALFLY,
		};
	
	public static class SeamlessNCPHook extends AbstractNCPHook {
		private SeamlessFlight plugin;

		public SeamlessNCPHook(SeamlessFlight plugin){
			this.plugin = plugin;
		}
		
		@Override
		public String getHookName() {
			return "SeamlessFlight(native)";
		}

		@Override
		public String getHookVersion() {
			return "1.0.0";
		}
		
		public CheckType[] getCheckTypes() {
			return checkTypes;
		}

		@Override
		public final boolean onCheckFailure(final CheckType checkType, final Player player, final IViolationInfo info) {
			if (plugin.isFlying(player)) return true;
			else return false;
		}
	}
	
	/**
	 * Register hook to cancel violation treatment for flying. the vls will still increase, though. To reset them use resetViolations.
	 *  
	 * @param plugin
	 * @return hokk id.
	 */
	public static Integer registerNCPHook(SeamlessFlight plugin){
		SeamlessNCPHook hook = new SeamlessNCPHook(plugin); 
		Integer id = NCPHookManager.addHook(hook.getCheckTypes(), hook);
		return id;
	}
	
	public static void unregisterHook(Integer id){
			NCPHookManager.removeHook(id);
	}
	
	/**
	 * Reset violations from flying.
	 * @param player
	 * @return
	 */
	public static boolean resetViolations(final Player player){
		try{
			final MovingData data = MovingData.getData(player);
			data.survivalFlyVL = 0;
			data.noFallVL = 0;
			data.clearNoFallData();
			data.clearFlyData();
			try{
				data.addHorizontalVelocity(new Velocity(0.5, 10, 7));
			}
			catch(Throwable t){}
			return true;
		} catch (Throwable t){
			return false;
		}
	}
}
