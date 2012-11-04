package me.asofold.bpl.seamlessflight.plshared;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;

/**
 * Auxiliary methods for teleportation.
 * TODO: convenience methods using Shared.getPlugin()
 * @author mc_dev
 *
 */
public class Teleport {
	
	public static HashSet<Byte> transparent_all = new HashSet<Byte>();
	public static HashSet<Byte> transparent_tp = new HashSet<Byte>();
	public static Set<Byte> transparent_tp_unsafe = new HashSet<Byte>();
	
	/**
	 * @deprecated use static{}
	 */
	public static void init(){
		
	}
	static{
		// transparent_all -> to get all blocks
				for (int  i =0; i<256; i++){
					transparent_all.add((byte) i);
				}
				
				// transparent for line of sight looking for tp position
				for (int i : new int[]{
						0,6,8,9,10,11,26,31,32,37,38,39,40,50,51,55,59,63,64,68,69,71,75,76,77,83,90,
						// whats 26 ?
						27,28,65,66,70,72,78, 104,105,106,115,117, 119,122 // not sure about these:
						// add doors ?
						
				}){
					transparent_tp.add((byte) i);
				}
				// dangerous blocks to tp on.
				for (int i : new int[]{
						8, 9, 10, 11, 26, 30, 46, 51, 55, 
						63, 64, 70, 71, 72, 81, 90, 96, 119, 
				}){
					transparent_tp_unsafe.add((byte) i);
				}
	}
	
	
	 /**
     * Check if the player can be put into this block (mind: two blocks needed.).
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static boolean isSafeTransparent(World world, double x, double y, double z){
    	return isSafeTransparent(world.getBlockTypeIdAt(Blocks.floor(x), Blocks.floor(y), Blocks.floor(z)));
    }
    	
    public static boolean isSafeTransparent(Location loc){
    	return isSafeTransparent(loc.getBlock().getTypeId());
    }
    
    public static boolean isSafeTransparent(int btp){
    	// TODO: adjust to new data values !
    	if ( (btp<0) || (btp>255) ){
    		// TODO
    		return true;
    	} else if (transparent_tp.contains((byte) btp) && !transparent_tp_unsafe.contains((byte)btp)){
    		return true;
    	}else{
    		return false;
    	}
    }
    
    /**
     * Check if the player can stand on this block, safely.
     * @param world
     * @param x
     * @param y
     * @param z
     * @return
     */
    public static boolean isSafeStand(World world, double x, double y, double z){
    	return isSafeStand(world.getBlockTypeIdAt(Blocks.floor(x),Blocks.floor(y),Blocks.floor(z)));
    }
    
    public static boolean isSafeStand(World world, int x, int y, int z){
    	return isSafeStand(world.getBlockTypeIdAt(x,y,z));
    }
    
    public static boolean isSafeStand(Location loc){
    	return isSafeStand(loc.getBlock().getTypeId());
    }
    
    /**
     * Check if the player can stand on this block, safely.
     * @param loc
     * @return
     */
    public static boolean isSafeStand(Block block){
    	return isSafeStand(block.getTypeId());
    }
    public static boolean isSafeStand(int btp){
    	if ( (btp<0) || (btp>255) ){
    		return false;
    	} 
    	else if (transparent_tp.contains((byte)btp)){
    		return false;
    	} 
    	else if (transparent_tp_unsafe.contains((byte)btp)){
    		return false;
    	} 
    	else{
    		return true;
    	}
    }
    
    /**
     * Checks if player.teleport(pos) is safe.
     * @param loc
     * @return
     */
    public static boolean isTPPos( Location loc){
    	/// HACK !? use get maybe 
    	World world = loc.getWorld();
		double x =  loc.getX();
		double y = loc.getY();
		double z = loc.getZ();
		return isTPPos(world, x, y, z);
    }
    
	public static boolean isTPPos( World world, double x, double y, double z){
		if ( !isSafeTransparent(world, x, y, z) ) return false;
		if ( !isSafeTransparent(world, x, y+1, z) ) return false;
		if ( !isSafeStand(world, x, y-1, z) ) return false;
		return true;
    }
    
    public static boolean teleport(Player player, Location loc){
    	return teleport(player, loc, false);
    }
    
    // TODO: safePos -> find safe position to stand on (vertical).
    /**
     *
     * @param player
     * @param loc
     * @param highest
     * @param reallySafe
     * @param plugin
     * @return
     */
    public static boolean teleport(Player player, Location loc, boolean highest){
    	World world = loc.getWorld();
    	loc = loc.clone();
    	Chunk chunk = world.getChunkAt(loc);
    	if (!chunk.isLoaded()) chunk.load();
    	if ( highest){
			int y = world.getHighestBlockAt(loc.getBlockX(), loc.getBlockZ()).getY()+1;
			loc.setY(y+1);
		} 
    	Block blc = loc.getBlock().getRelative(BlockFace.DOWN);
		Material mat = blc.getType();
		byte data = blc.getData();
    	player.sendBlockChange(blc.getLocation(), mat, data);
		if (player.teleport(loc)){
			// HACK (kind of)
			
			// TODO: send even more blocks or the lowest non transparent one ?
			player.sendBlockChange(blc.getLocation(), mat, data);
			return true;
		} else return false;
    }


	/**
	 * Swift passage: (safeTP) to/up the last block of line of sight, that is really visible.
	 * BUGS: occasionally does teleport into some cave (!), probably needs updating transparent blocks etc.
	 * TODO: split into parts, configurable..
	 * @param player
	 */
	public static boolean swiftPassage(Player player) {
		return swiftPassage(player, 300);
	}
	
	/**
	 * Swift passage: (safeTP) to/up the last block of line of sight, that is really visible.
	 * BUGS: occasionally does teleport into some cave (!), probably needs updating transparent blocks etc.
	 * TODO: split into parts, configurable..
	 * @param player
	 */
	public static boolean swiftPassage(Player player, int dist) {
		Location loc = Teleport.getSwiftPassageLoc(player, dist);
		if ( loc == null){
			player.sendMessage("pluginlib - swift passage failed.");
			return false;
		}
		if (!teleport(player, loc)){
			player.sendMessage("pluginlib - swift passage failed.");
			return false;
		}
		return true;
	}

	/**
	 * Convenience method to just get the location for swift pass through.
	 * Returns null on failure.
	 * @param player
	 * @return
	 */
	public static Location getSwiftPassageLoc(Player player, int dist) {
		List<Block> los = player.getLineOfSight(Teleport.transparent_tp, dist);
		if ( los.isEmpty()) return null;
		Block target = los.get(los.size()-1); // last block
//		World world = player.getWorld();
		boolean found = false;
		Location pLoc = player.getLocation();
		int x = pLoc.getBlockX();
		int y = pLoc.getBlockY();
		int z = pLoc.getBlockZ();
		for (Block block : los){
			if ( block.getTypeId()!=0){
				if (Teleport.isSafeStand( block.getLocation())){
					target = block;
					found = true;
					break;
				}
			}
			if (Blocks.exceedsDistance(x,y,z, block.getX(), block.getY(), block.getZ(), dist)) break;
		}
		if ( !found ) return null;
		Location loc = target.getLocation().clone();
		loc.setY(loc.getY()+1);
		if (!isTPPos(loc)) return null;
		loc.setYaw(pLoc.getYaw());
		loc.setPitch(pLoc.getPitch());
		setMiddle(loc);
		return loc;
	}

	/**
	 * Swift pass through: TP to next position that is (safeTP) after the first non-transparent block / series of blocks along the line of sight.
	 * TODO: split into parts, configurable..
	 * @param player
	 */
	public static boolean swiftPassThrough(Player player){
		return swiftPassThrough(player, 100);
	}
	
	/**
	 * Swift pass through: TP to next position that is (safeTP) after the first non-transparent block / series of blocks along the line of sight.
	 * TODO: split into parts, configurable..
	 * @param player
	 */
	public static boolean swiftPassThrough(Player player, int dist) {
		Location loc = Teleport.getSwiftPassThroughLoc(player, dist);
		if ( loc == null){
			player.sendMessage("pluginlib - could not find position to pass through to.");
			return false;
		}
		if (teleport(player, loc)){
			return true; //player.sendMessage("pluginlib - swift pass through...");
		} else{
			player.sendMessage("pluginlib - swift pass through failed.");
			return false;
		}
	}
	
	public static Location getSwiftPassThroughLoc(Player player, int dist) {
		List<Block> los = player.getLineOfSight(Teleport.transparent_all, dist);
		if (los.isEmpty()) return null;
		Block target = null;
		boolean solidFound = false;
		for (Block block : los){
			int bid = block.getTypeId(); 
			if (!Teleport.transparent_tp.contains((byte)bid)){
				solidFound = true;
			} else if (solidFound){
				if (Teleport.isSafeTransparent(block.getLocation())){
					target = block;
					break;
				}
			} 
		}
		if (target == null){
			return null;
		}
		if (!solidFound) return null;
		// check downwards
		Block temp = target;
		target = null;
		int x = temp.getX();
		int z = temp.getZ();
		World world = temp.getWorld();
		for ( int y = temp.getY(); y>=0; y-- ){
			if ( isSafeTransparent(world, x, y, z)){
				// TODO: more efficient
				if (Teleport.isTPPos(world, x, y, z)){
					target = new Location(world, x,y,z).getBlock();
					// TODO: cleanup
					break;
				}
				// continue
			} else {
				break;
			}
			y--;
		}
		if ( target == null){
			if ( Teleport.isTPPos(world, x, temp.getY()+1, z)){
				target = new Location(world, x,temp.getY()+1,z).getBlock();
			} else return null;
		}
		Location loc = target.getLocation().clone();
		Location pLoc = player.getLocation();
		if (Blocks.exceedsDistance(pLoc, loc, dist)) return null;
		loc.setYaw(pLoc.getYaw());
		loc.setPitch(pLoc.getPitch());
		setMiddle(loc);
		return loc;
	}


	public static void setMiddle(Location loc){
		double x = loc.getBlockX();
		loc.setX(x+.5);
		double z = loc.getBlockZ();
		loc.setZ(z+.5);
	}
	
//	public static boolean prefixTp(Player player, String prefix){
//		Player target = Players.getOnlinePlayerByPrefix(prefix);
//		if ( target == null) return false;
//		else if ( player.getName().equalsIgnoreCase(target.getName()) ) return false; 
//		else return Teleport.teleport(player, target.getLocation());
//	
//	}
//
//	public static boolean prefixTpHere(Player player, String prefix) {
//		Player target = Players.getOnlinePlayerByPrefix(prefix);
//		if ( target == null) return false;
//		else if ( player.getName().equalsIgnoreCase(target.getName()) ) return false; 
//		else return Teleport.teleport(target, player.getLocation());
//	}
}
