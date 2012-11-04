package me.asofold.bpl.seamlessflight.plshared.actions;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Check toggleable actions!
 * +Override methods and call onToggleAction whenever the action is toggled (like a ToggleSneakEvent).
 * NOTE: NOT FOR CONCURRENT USE.
 * 
 * TODO: maybe use maxfold (use as short press).
 * @author mc_dev
 *
 */
public abstract class ActionChecker implements Runnable{
	
	class UserData{
		String user ;
		int fold = 0;
		/**
		 * Last use, used for garbage collection.
		 */
		public long ts = 0;
		public long tsDown = 0;
		boolean needsPoll = false;
		boolean isIdle = true;
		public UserData(String user){
			this.user = user;
		}
		public void reset() {
			ts = 0;
			tsDown = 0;
			needsPoll = false;
			isIdle = true;
			fold = 0;
		}
		public int hashCode(){
			return user.hashCode();
		}
		public boolean equals(Object o){
			if (o instanceof UserData){
				return user.equals(((UserData)o).user);
			} else return false;
		}
	}
	
	int taskId = -1;
	
	Map<String, UserData> userData = new HashMap<String, ActionChecker.UserData>();
	
	Set<UserData> needPoll = new HashSet<UserData>();
	
	long tsCleanup = 0;
	
	/**
	 * Duration in ms, after this data wll be forgotten.
	 */
	public long periodCleanup = 60000;
	
	/**
	 * Maximum duration for a hold to be counted as a singular press+release.
	 */
	long maxDurActive = 400;
	
	/**
	 * This is for the internal polling.
	 * You may remove user data during this call.
	 * @param user
	 * @return
	 */
	public abstract boolean getToggleState(String user);
	
	/**
	 * Override: This is called on finish of an action.
	 * UserData entries are reset after calling (!).
	 * Event types:
	 * long release: isActive=false + isShort=false
	 * long press start: isActive=true + isShort=false
	 * short release: isActive=false + isShort=true
	 * clicking: isActive = true + isShort=true
	 * @param user
	 * @param fold number of actions involved
	 * @param isActive current state when calling
	 * @param isShort if this has been a short action or a long one, if isActive is set this always will be false.
	 */
	public abstract void onAction(String user, int fold, ActionType actionType);
	
	/**
	 * Check if a player is currently not performing a click-series (!). 
	 * @param user
	 * @return
	 */
	public boolean isIdle(String user){
		// TODO:
		return true;
	}
	
	/**
	 * Call this on toggling the action.
	 * @param user
	 * @param isActive the state that should be there after toggling.
	 */
	public void onToggleAction(String user, boolean isActive){
		long ts = System.currentTimeMillis();
		if (ts > tsCleanup + periodCleanup) cleanup();
		UserData data = getUserData(user);
		if ( data.needsPoll ){
			needPoll.remove(data);
			data.needsPoll = false;
		}
		if ( isActive){
			// ON
			if (ts-data.tsDown<maxDurActive){
				// NEEDS POLL if will be held for a longer time (for event) !
				data.needsPoll = true;
				needPoll.add(data);
			} else{
				// TODO: reset !
				data.reset();
			}
			// Set in any case:
			data.fold++;
			data.isIdle = false;
			data.tsDown = ts;
			
			// CLICKING (anyway)
			onAction(user, data.fold, ActionType.CLICKING);
		} else{
			// OFF
			if (ts-data.tsDown<maxDurActive){
				// NEEDS POLLING (if nothing follows)
				data.needsPoll = true;
				data.isIdle = false;
				needPoll.add(data);
			} else{
				// long release event
				onAction(user, data.fold, ActionType.LONG_RELEASE);
				data.reset();
			}
		}
		// Set in any case:
		data.ts = ts;
		
	}
	
	/**
	 * Forget old user data entries.
	 */
	public void cleanup(){
		long ts = System.currentTimeMillis();
		Set<String> rem = new HashSet<String>();
		for ( String user : userData.keySet()){
			UserData data = userData.get(user);
			// TODO: periodCleanup is a very relaxed limit. (could be mixture of checking flags and maxDurActive)
			if (ts > data.ts+periodCleanup) rem.add(user);
		}
		for ( String user :rem){
			removeUserData(user);
		}
		tsCleanup = ts;
	}
	
	public UserData getUserData(String user){
		UserData data = userData.get(user);
		if ( data == null){
			data = new UserData(user);
			userData.put(user, data);
		}
		return data;
	}
	
	public void removeUserData(String user){
		UserData data = userData.remove(user);
		if (data!=null ) needPoll.remove(data);
	}
	
	/**
	 * Call this on polling or use run().
	 * This checks UserData entries for expiration, thus can throw quick-toggle events and such.
	 */
	public void poll(){
		long ts = System.currentTimeMillis();
		if (ts > tsCleanup + periodCleanup) cleanup();
		List<UserData> rem = new LinkedList<UserData>();
		for ( UserData data: needPoll){
			String user = data.user;
			if (!userData.containsKey(user)) continue; // (inconsistency?)
			if (!data.needsPoll) continue; // (inconsistency?)
			if ( ts-data.tsDown < maxDurActive) continue; // Can not yet be judged.
			boolean isActive = getToggleState(user);
			if ( isActive){
				// long action started
				onAction(user,data.fold, ActionType.LONG_START);
			} else{
				// short action release
				onAction(user, data.fold, ActionType.SHORT_RELEASE);
			}
			data.reset();
			data.ts = ts;
			rem.add(data);
		}
		for ( UserData data : rem) {
			needPoll.remove(data);
		}
	}
	
	@Override
	public void run(){
		poll();
	}
	
}	
