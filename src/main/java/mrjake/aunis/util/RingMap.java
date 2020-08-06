//------------------------------------------------------------------------------------------------
//
//   SG Craft - Dimension map
//
//------------------------------------------------------------------------------------------------
package mrjake.aunis.util;

import java.util.*;
import com.stargatemc.api.CoreAPI;
import com.stargatemc.constants.ConsoleMessageType;
import com.stargatemc.constants.SpawnType;
import com.stargatemc.data.LocationData;
import com.stargatemc.data.Spawn;
import com.stargatemc.data.SpawnData;

import gcewing.sg.BaseUtils;
import mrjake.aunis.tileentity.TransportRingsTile;
import net.minecraft.nbt.*;
import net.minecraft.world.*;
import net.minecraft.world.storage.*;
import net.minecraftforge.common.util.Constants;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.BlockPos.MutableBlockPos;
import net.minecraft.world.World;
//import net.minecraftforge.common.*;

import net.minecraft.util.math.BlockPos;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import zmaster587.advancedRocketry.stations.SpaceObjectManager;
import zmaster587.advancedRocketry.stations.SpaceStationObject;

public class RingMap extends WorldSavedData {

    public static int getAvailableFrequency(String address) {
        return get().getAvailableFrequencyInstanced(address);
    }
    public int getAvailableFrequencyInstanced(String address) {
        HashMap<Integer,ArrayList<RingAddressEntry>> ringmap = getRingsForAddressByFrequencyInstanced(address);
        if (ringmap.isEmpty()) return 0; // Use 0 as first frequency
        int highestFrequency = 0;
        for (int frequency : ringmap.keySet()) {
            if (frequency > highestFrequency) highestFrequency = frequency;
            if (ringmap.get(frequency).size() <= 5) return frequency;
        }
        return (highestFrequency+1);        
    }
    
    protected Map<String, ArrayList<RingAddressEntry>> addressToEntries = new HashMap<>();
    
    public RingMap(String name) {
        super(name);
    }
    
    public static int getNumberOfRingNets() {
    	return RingMap.get().getNumberOfRingNetworks();
    }

    public static int getNumberOfRingPlatforms() {
    	return RingMap.get().getNumberOfRings();
    }
    
    public int getNumberOfRingNetworks() {
    	return addressToEntries.keySet().size();
    }
    
    public int getNumberOfRings() {
    	int count = 0;
    	for (String ring : addressToEntries) {
    		count += (addressToEntries.get(ring).size());
    	}
    	return count;
    }
    
    public static RingMap get() {
        World world = BaseUtils.getWorldForDimension(0);
        return BaseUtils.getWorldData(world, RingMap.class, "stargatemc-ring_map");
    }
    
    public static RingAddressEntry getForTileEntity(TransportRingsTile tile) {
        if (tile.getAddress() == null) return null;
        for (RingAddressEntry rae : getRingsForFrequency(tile.getAddress(),tile.getFrequency())) {
            if (rae.getRings().equals(tile)) return rae;
        }
        return null;
    }
    
    public static ArrayList<RingAddressEntry> getRingsForFrequency(String address, int frequency) {
        return get().getRingsForFrequencyInstanced(address, frequency);
    }
    public ArrayList<RingAddressEntry> getRingsForFrequencyInstanced(String address, int frequency) {
        return getRingsForAddressByFrequencyInstanced(address).get(frequency);
    }
    public static HashMap<Integer,ArrayList<RingAddressEntry>> getRingsForAddressByFrequency(String address) {
        return get().getRingsForAddressByFrequencyInstanced(address);
    }
    
    public static int availableSlots(String address, int frequency) {
        return 6 - getRingsForFrequency(address,frequency).size();
    }
    
    public boolean updateFrequency(RingAddressEntry entry, int newFrequency) {
        if (availableSlots(entry.getAddress(), newFrequency) < 1) return false;
        int oldFrequency = entry.getFrequency();
        entry.setFrequency(newFrequency);
        markDirty();
        updateRings(entry.getAddress(),oldFrequency);
        updateRings(entry.getAddress(),entry.getFrequency());
        return true;
    }
    
    public HashMap<Integer,ArrayList<RingAddressEntry>> getRingsForAddressByFrequencyInstanced(String address) {
        HashMap<Integer,ArrayList<RingAddressEntry>> map = new HashMap<Integer,ArrayList<RingAddressEntry>>();
        if (addressToEntries.get(address) == null || addressToEntries.get(address).isEmpty()) return map;
        int count = 0;
        while (count < addressToEntries.get(address).size()) {
            RingAddressEntry re = getEntryForIndex(address,count);
            if (re != null) { 
                if (map.containsKey(re.getFrequency())) {
                    ArrayList<RingAddressEntry> entries = map.get(re.getFrequency());
                    entries.add(re);
                    map.replace(re.getFrequency(), entries);
                } else {
                    ArrayList<RingAddressEntry> entries = new ArrayList<RingAddressEntry>();
                    entries.add(re);
                    map.put(re.getFrequency(), entries);
                }
            }
            count++;
        }
        return map;
    }
    
//    public static TransportRingsTile getNearest(World w, BlockPos pos) {
//        return RingMap.get().getNearestRing(w, pos);
//    }
//    
    
    public String getRingsNameForBlockPos(World w, BlockPos pos) {
        DimensionProperties props = DimensionManager.getEffectiveDimId(w, pos);
        
        if (props == null || props == DimensionManager.defaultSpaceDimensionProperties) {
            return "In FTL";
        } else {
            Spawn s = SpawnData.getNearestSpawn(SpawnData.getSpawns(), props);
            if (s == null) return "Unknown";
            if (w.provider.getDimension() == -2) {
                SpaceStationObject object = (SpaceStationObject)SpaceObjectManager.getSpaceManager().getSpaceStationFromBlockCoords(pos);
                return "Orbiting " + props.getName() + "(" + (object.getId() == s.getIdentifier() && s.getType().equals(SpawnType.Station) ? s.getName() : "Station: " + object.getId()) + ")";
            } else {
                Random r = new Random();
                int randomX = r.nextInt(256);
                int randomZ = r.nextInt(256);
                if (r.nextBoolean()) randomX *= -1;
                if (r.nextBoolean()) randomZ *= -1;
                if (LocationData.isPositionProtected(w,pos) && props.getId() == s.getIdentifier() && s.getType().equals(SpawnType.Planet)) return s.getName();
                return (props.getName() + "(Near " + (pos.getX() + randomX) + "," + (pos.getZ() + randomZ) + ")");
            }
        }
    }
    public void updateRings(String address, int frequency) {
        if (this.getRingsForFrequencyInstanced(address, frequency) == null || this.getRingsForFrequencyInstanced(address, frequency).isEmpty()) return;
        int count = 1;
		try {
                    for (RingAddressEntry rae : this.getRingsForFrequencyInstanced(address, frequency)) {
                                TransportRingsTile trt = rae.getRings();
                                trt.ringsMap.clear();
                    }
                } catch (Exception e) {
                    System.out.println("Failed to clear rings for address: " + address + " and freq: " + frequency);
                    e.printStackTrace();
                }
        try {
            for (RingAddressEntry rae : this.getRingsForFrequencyInstanced(address, frequency)) {
            	List<TransportRingsTile> ringsTilesInRange = new ArrayList<>();
                for (RingAddressEntry srae : this.getRingsForFrequencyInstanced(rae.getRings().getAddress(), rae.getRings().getFrequency())) {
                    try {
                    	ringsTilesInRange.add(srae.getRings());
                    } catch (Exception e) {
                    	e.printStackTrace();
                        System.out.println("Failed to consider rings for address: " + srae.getAddress() + " on dim: " + srae.getDimension() + " at position: " + srae.getBlockPos().toString() + " and freq: " + frequency);
                    }
                }
				rae.getRings().getRings().setName(getRingsNameForBlockPos(rae.getRings().getWorld(), rae.getRings().getPos()));
				rae.getRings().getRings().setAddress(count++);
				
				for (TransportRingsTile newRingsTile : ringsTilesInRange) {
					rae.getRings().addRings(newRingsTile);
					newRingsTile.addRings(rae.getRings());
				}
				
				rae.getRings().markDirty();
        }
        } catch (Exception e) {
                    System.out.println("Failed to update rings for address: " + address + " and freq: " + frequency);
                    e.printStackTrace();
        }
    }
//    public TransportRingsTile getNearestRing(World w, BlockPos pos) {
//        DimensionProperties props = DimensionManager.getEffectiveDimId(w,pos);
//        if (props == null) return null;
//        if (!RingMap.get().addressToEntries.containsKey(props.getName())) return null;
//        double distance = -1;
//        TransportRingsTile ring = null;
//        for (TransportRingsTile entry : RingMap.getRingsForAddressByFrequency(props.getName())) {
//            if (entry.getDimension() != w.provider.getDimension()) continue;
//            double currentDistance = CoreAPI.distance(entry.getBlockPos(), pos);
//            if (distance == -1 || currentDistance < distance) {
//                distance = currentDistance;
//                ring = entry;
//            }
//        }
//        return (ring != null ? ring : null);
//    }
    
    public static boolean hasProtectedRing(String address) {
        return RingMap.getLastProtectedRing(address) != null;
    }
    public static boolean hasRing(String address) {
        return RingMap.get().getNumFor(address) != 0;
    }
    public static int getNumberOfRings(String address) {
        return RingMap.get().getNumFor(address);
    }
    public static TransportRingsTile getLastRing(String address) {
        return RingMap.get().getLastFor(address);
    }
    
    public static TransportRingsTile getFirstRing(String address) {
        return RingMap.get().getFirstFor(address);
    }
    
    public TransportRingsTile getFirstFor(String address) {
        TransportRingsTile te = null;
        while (getNumFor(address) > 0) {
            te = getForIndex(address,0);
            if (te != null) return te;
        }
        return null;
    }
    
    public static TransportRingsTile getLastProtectedRing(String address) {
        return RingMap.get().getLastProtected(address);
    }
    
    public TransportRingsTile getLastProtected(String address) {
        TransportRingsTile te = null;
        int count = getNumFor(address)-1;
        while (count >= 0) {
            //System.out.println("Processing index: " + count + ", with entry: " + addressToEntries.get(address).get(count).toString());
            validateEntry(addressToEntries.get(address).get(count));
            if (LocationData.isPositionProtected(CoreAPI.getWorldForDimension(addressToEntries.get(address).get(count).getDimension()), addressToEntries.get(address).get(count).getBlockPos())) te = getForIndex(address,count);
            //if (te != null) System.out.println("Returning gate!");
            if (te != null) return te;
            count--;
        }
        return null;
    }
    
    public void validateEntry(RingAddressEntry entry) {
        if (entry.getRings() == null) return;
        if (entry.getFrequency() != entry.getRings().getFrequency()) {
            entry.setFrequency(entry.getRings().getFrequency());
            markDirty();
        }
    }
    public RingAddressEntry getEntryForIndex(String address, int index) {
        if (address == null) return null;
        if (getNumFor(address) == 0) return null;
        if (addressToEntries.get(address).get(index) == null) {
            //System.out.println("Found null address entry for : " + address + ", index: " + index);
            return null;
        }
        try {
            World w = CoreAPI.getWorldForDimension(addressToEntries.get(address).get(index).getDimension());
            if (w != null) {
                System.out.println("Found world : " + w.provider.getDimension());
            } else {
                System.out.println("Couldnt find dimension: " + addressToEntries.get(address).get(index).getDimension());
            }
            TransportRingsTile te = addressToEntries.get(address).get(index).getRings();
            if (te != null && te instanceof TransportRingsTile && te.getResolvedAddress().equals(address)) return addressToEntries.get(address).get(index);
        } catch (Exception e) {
            System.out.println("Failed to locate gate: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Removing invalid ring " + index + " from RingMap for address : " + address);
        addressToEntries.get(address).remove(index);
        markDirty();
        return null;
    }
    public TransportRingsTile getForIndex(String address, int index) {
        if (address == null) return null;
        if (getNumFor(address) == 0) return null;
        if (addressToEntries.get(address).get(index) == null) {
            //System.out.println("Found null address entry for : " + address + ", index: " + index);
            return null;
        }
        try {
            World w = CoreAPI.getWorldForDimension(addressToEntries.get(address).get(index).getDimension());
            if (w != null) {
                System.out.println("Found world : " + w.provider.getDimension());
            } else {
                System.out.println("Couldnt find dimension: " + addressToEntries.get(address).get(index).getDimension());
            }
            TransportRingsTile te = addressToEntries.get(address).get(index).getRings();
            if (te != null && te instanceof TransportRingsTile && DimensionManager.getEffectiveDimId(te.getWorld(), te.getPos()).getName().equals(address)) return te;
        } catch (Exception e) {
            System.out.println("Failed to locate gate: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println("Removing invalid ring " + index + " from RingMap for address : " + address);
        addressToEntries.get(address).remove(index);
        markDirty();
        return null;
    }
    
    public TransportRingsTile getLastFor(String address) {
        TransportRingsTile te = null;
        while (getNumFor(address) > 0) {
            te = getForIndex(address,addressToEntries.get(address).size()-1);
            if (te != null) return te;
        }
        return null;
    }
    
    public int getNumFor(String address) {
        if (addressToEntries.containsKey(address)) {
            return addressToEntries.get(address).size();
        }
        System.out.println("Found no rings for : " + address);
        return 0;
    }

    public static void removeOldAddress(TransportRingsTile te, String address) {
        get().removeFor(te, address);
        te.setAddress(null);
        te.setFrequency(-1);
    }
    public void removeFor(TransportRingsTile te, String address) {
        RingAddressEntry entry = new RingAddressEntry(te,address);
        if (addressToEntries.containsKey(entry.getAddress())) {
            ArrayList<RingAddressEntry> entries = new ArrayList<RingAddressEntry>(addressToEntries.get(entry.getAddress()));
            for (RingAddressEntry e : addressToEntries.get(entry.getAddress())) {
                if (entry.getDimension() == e.getDimension() && entry.getX() == e.getX() && entry.getY() == e.getY() && entry.getZ() == e.getZ()) entries.remove(e);
            }
            addressToEntries.replace(entry.getAddress(), entries);
            markDirty();
        }
        updateRings(address,te.getFrequency());
    }
    
    public static void addOrUpdateEntry(RingAddressEntry entry) {
        RingMap.get().addOrUpdateFor(entry);
    }
    
    public void addOrUpdateFor(RingAddressEntry entry) {
        if (addressToEntries.containsKey(entry.getAddress())) {
         ArrayList<RingAddressEntry> entries = addressToEntries.get(entry.getAddress());
         boolean found = false;
         boolean changed = false;
        for (RingAddressEntry e : addressToEntries.get(entry.getAddress())) {
            if (e.getDimension() == entry.getDimension() && e.getX() == entry.getX() && entry.getY() == e.getY() && e.getZ() == entry.getZ()) {
                if (e.getFrequency() != entry.getRings().getFrequency()) {
                    System.out.println("Updating ring for : " + entry.getAddress());
                    e.setFrequency(entry.getRings().getFrequency());
                    changed = true;
                }
                found = true;
            }
        }
        if (!found) {
            System.out.println("Loading ring for : " + entry.getAddress());
            entries.add(entry);
        }
        if (!found || changed) {
         addressToEntries.replace(entry.getAddress(), entries);
         markDirty();
        }
        } else {
            ArrayList<RingAddressEntry> entries = new ArrayList<RingAddressEntry>();
            entries.add(entry);
            addressToEntries.put(entry.getAddress(),entries);
            markDirty();
        }
    }
    public static void addOrUpdateEntryFor(TransportRingsTile te, String address) {
        RingMap.get().addOrUpdateFor(te,address);
    }
    public void addOrUpdateFor(TransportRingsTile te, String address) {
        if (address != null) {
        RingAddressEntry entry = new RingAddressEntry(te,address);
        te.setAddress(address);
        te.setFrequency(this.getAvailableFrequencyInstanced(address));
        addOrUpdateFor(entry);
        updateRings(address,te.getFrequency());
        CoreAPI.sendConsoleEntry("Added address " + te.getAddress() + " and frequency: " + te.getFrequency() +  " for : " + te.getWorld().provider.getDimension() + ", pos: " + te.getPos().toString(), ConsoleMessageType.FINE);
        } else {
            te.setAddress(null);
            te.setFrequency(-1);
            CoreAPI.sendConsoleEntry("Failed to add address for : " + te.getWorld().provider.getDimension() + ", pos: " + te.getPos().toString(), ConsoleMessageType.FINE);
        }
    }
    
    @Override
    public void readFromNBT(NBTTagCompound nbt) {
        NBTTagList addresses = nbt.getTagList("addresses",Constants.NBT.TAG_COMPOUND);
        if (addresses != null) System.out.println("Found addresses NBT with : " + addresses.tagCount() + " tags.");
        for (int i = 0; i < addresses.tagCount() ; i++) {
            NBTTagCompound tag = addresses.getCompoundTagAt(i);
            RingAddressEntry entry = RingAddressEntry.fromNBT(tag);
            System.out.println("Loading ring entry from NBT: " + entry.getAddress() + "," + entry.getDimension() + "," + entry.getFrequency()+ "," + entry.getX() + "," + entry.getY() + "," + entry.getZ());
            addOrUpdateFor(entry);
        }
    }
    
    @Override
    public NBTTagCompound writeToNBT(NBTTagCompound nbt) {
        NBTTagList addresses = new NBTTagList();
        for (String address : this.addressToEntries.keySet()) {
            for (RingAddressEntry entry : this.addressToEntries.get(address)) {            
                System.out.println("Saving stargate entry to NBT: " + entry.getAddress() + "," + entry.getDimension() + "," + entry.getFrequency()+ "," + entry.getX() + "," + entry.getY() + "," + entry.getZ());
                addresses.appendTag(entry.toNBT());
            }
        }
        System.out.println("Saving " + addresses.tagCount() + " addresses to RingMap.");
        nbt.setTag("addresses", addresses);
        return nbt;
    }

}
