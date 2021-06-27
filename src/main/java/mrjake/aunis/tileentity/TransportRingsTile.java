package mrjake.aunis.tileentity;
import net.minecraft.util.EnumFacing;
import com.stargatemc.api.CoreAPI;
import gcewing.sg.util.FakeTeleporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import com.stargatemc.data.PerWorldData;
import noppes.npcs.controllers.FactionController;
import ic2.api.energy.tile.IEnergyEmitter;
import ic2.api.energy.EnergyNet;
import ic2.api.energy.event.EnergyTileLoadEvent;
import ic2.api.energy.event.EnergyTileUnloadEvent;
import net.minecraftforge.common.MinecraftForge;
import li.cil.oc.api.machine.Arguments;
import li.cil.oc.api.machine.Callback;
import li.cil.oc.api.machine.Context;
import li.cil.oc.api.network.Environment;
import net.minecraft.entity.player.EntityPlayer;
import li.cil.oc.api.network.Message;
import li.cil.oc.api.network.Node;
import mrjake.aunis.Aunis;
import mrjake.aunis.AunisProps;
import mrjake.aunis.block.AunisBlocks;
import mrjake.aunis.config.AunisConfig;
import mrjake.aunis.gui.RingsGUI;
import mrjake.aunis.packet.AunisPacketHandler;
import mrjake.aunis.packet.StateUpdatePacketToClient;
import mrjake.aunis.packet.StateUpdateRequestToServer;
import mrjake.aunis.packet.transportrings.StartPlayerFadeOutToClient;
import mrjake.aunis.renderer.transportrings.TransportRingsRenderer;
import mrjake.aunis.sound.AunisSoundHelper;
import mrjake.aunis.sound.SoundEventEnum;
import mrjake.aunis.stargate.EnumScheduledTask;
import mrjake.aunis.state.State;
import mrjake.aunis.state.StateProviderInterface;
import mrjake.aunis.state.StateTypeEnum;
import mrjake.aunis.state.TransportRingsGuiState;
import mrjake.aunis.state.TransportRingsRendererState;
import mrjake.aunis.state.TransportRingsStartAnimationRequest;
import mrjake.aunis.tesr.RendererInterface;
import mrjake.aunis.tesr.RendererProviderInterface;
import mrjake.aunis.tileentity.util.ScheduledTask;
import mrjake.aunis.tileentity.util.ScheduledTaskExecutorInterface;
import mrjake.aunis.transportrings.ParamsSetResult;
import mrjake.aunis.transportrings.TransportResult;
import mrjake.aunis.transportrings.TransportRings;
import mrjake.aunis.util.AunisAxisAlignedBB;
import mrjake.aunis.util.ILinkable;
import mrjake.aunis.util.RingAddressEntry;
import mrjake.aunis.util.RingMap;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ITickable;
import net.minecraft.util.Rotation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import net.minecraftforge.fml.common.network.NetworkRegistry.TargetPoint;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import noppes.npcs.entity.EntityCustomNpc;
import zmaster587.advancedRocketry.dimension.DimensionManager;
import zmaster587.advancedRocketry.dimension.DimensionProperties;
import ic2.api.energy.tile.IEnergySink;

@Optional.Interface(iface = "li.cil.oc.api.network.Environment", modid = "opencomputers")
public class TransportRingsTile extends TileEntity implements IEnergySink, ITickable, RendererProviderInterface, StateProviderInterface, ScheduledTaskExecutorInterface, ILinkable, Environment {
	
	// ---------------------------------------------------------------------------------
	// Ticking and loading
    private boolean loaded = false;
    private int maxSafeInput = 32768;
    private int powerTier = 6;
    private double energyBuffer = 0;
    private int update = 0;
    private double energyMax = 10000000;
	
	public static final int FADE_OUT_TOTAL_TIME = 2 * 120; // 2s
	public static final int TIMEOUT_TELEPORT = FADE_OUT_TOTAL_TIME/2;

	public static final int TIMEOUT_FADE_OUT = (int) (30 + TransportRingsRenderer.INTERVAL_UPRISING*TransportRingsRenderer.RING_COUNT + TransportRingsRenderer.ANIMATION_SPEED_DIVISOR * Math.PI);	
	public static final int RINGS_CLEAR_OUT = (int) (15 + TransportRingsRenderer.INTERVAL_FALLING*TransportRingsRenderer.RING_COUNT + TransportRingsRenderer.ANIMATION_SPEED_DIVISOR * Math.PI);
		
	private static final AunisAxisAlignedBB LOCAL_TELEPORT_BOX = new AunisAxisAlignedBB(-1, 2, -1, 2, 4.5, 2);
	private AunisAxisAlignedBB globalTeleportBox;
	
	private List<Entity> teleportList = new ArrayList<>();
        private int frequency;
        private String address = null;
        
        public String changeCharInPosition(int position, char ch, String str){
          char[] charArray = str.toCharArray();
          charArray[position] = ch;
          return new String(charArray);
        }
        
        public String getResolvedAddress() {
            DimensionProperties props = DimensionManager.getEffectiveDimId(this.world, this.getPos());
            if (props == null || props == DimensionManager.defaultSpaceDimensionProperties || props.getId() == 0)  {
                return null;
            } else {
                return changeCharInPosition(6,Integer.toString(0).charAt(0),props.getName());
            }
        }
        
        void unload() {
            if (!world.isRemote && loaded) {
                MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
                loaded = false;
            }
        }
        
	@Override
	public void update() {		
		if (!world.isRemote) {
			ScheduledTask.iterate(scheduledTasks, world.getTotalWorldTime());
						if (!loaded) {
				            loaded = true;
				            MinecraftForge.EVENT_BUS.post(new EnergyTileLoadEvent(this));
				        }
                        if (address != null && getResolvedAddress() == null) {
                            System.out.println("Removing old address: " + address + " from ring due to rings moving!");
                            RingMap.removeOldAddress(this, address);
                        }
                        if (address == null && getResolvedAddress() != null) {
                            RingMap.addOrUpdateEntryFor(this, getResolvedAddress());
                            System.out.println("Adding address: " + address + " to ring!");
                        }
		}
	}
	
        public int getFrequency() {
            return this.frequency;
        }
        
        public void setFrequency(int freq) {
            this.frequency = freq;
        }
        
        public String getAddress() {
            return this.address;
        }
        public void setAddress(String address) {
            this.address = address;
        }
        
	@Override
	public void onLoad() {
		
		if (!world.isRemote) {
			setBarrierBlocks(false, false);
			
			Aunis.ocWrapper.joinOrCreateNetwork(this);
			globalTeleportBox = LOCAL_TELEPORT_BOX.offset(pos);
                        this.setBusy(false);
		}
		else {
			renderer = new TransportRingsRenderer(world, LOCAL_TELEPORT_BOX);
			AunisPacketHandler.INSTANCE.sendToServer(new StateUpdateRequestToServer(pos, StateTypeEnum.RENDERER_STATE));
		}
	}
	
	
	// ---------------------------------------------------------------------------------
	// Scheduled task
	
	List<ScheduledTask> scheduledTasks = new ArrayList<>();
	
	@Override
	public void addTask(ScheduledTask scheduledTask) {
		scheduledTask.setExecutor(this);
		scheduledTask.setTaskCreated(world.getTotalWorldTime());
		
		scheduledTasks.add(scheduledTask);
		markDirty();
	}
	
	@Override
	public void executeTask(EnumScheduledTask scheduledTask, NBTTagCompound customData) {
		switch (scheduledTask) {
			case RINGS_START_ANIMATION:
				animationStart();
				setBarrierBlocks(true, true);
				
				addTask(new ScheduledTask(EnumScheduledTask.RINGS_FADE_OUT));
				addTask(new ScheduledTask(EnumScheduledTask.RINGS_SOLID_BLOCKS, 35));
				break;
				
			case RINGS_SOLID_BLOCKS:
				setBarrierBlocks(true, false);
				break;
				
			case RINGS_FADE_OUT:
				teleportList = world.getEntitiesWithinAABB(Entity.class, globalTeleportBox);
				
				for (Entity entity : teleportList) {
					if (entity instanceof EntityPlayerMP) {
						AunisPacketHandler.INSTANCE.sendTo(new StartPlayerFadeOutToClient(), (EntityPlayerMP) entity);
					}
				}
				
				addTask(new ScheduledTask(EnumScheduledTask.RINGS_TELEPORT));
				break;
				
			case RINGS_TELEPORT:
				BlockPos teleportVector = targetRingsPos.subtract(pos);
				
				for (Entity entity : teleportList) {
                    if (entity instanceof EntityCustomNpc && ((EntityCustomNpc)entity).stats.spawnCycle != 3 && ((EntityCustomNpc)entity).stats.spawnCycle != 4)continue;
					if (!excludedEntities.contains(entity)) {
						boolean transported = false;
						BlockPos ePos = entity.getPosition().add(teleportVector);	
                                                if (targetDimension != this.world.provider.getDimension()) {
    												if (targetDimension == 0) {
    													CoreAPI.logAudit("Refusing to transport anything to DIM0!", false);
    													continue;
    												}
                                                    if (entity instanceof EntityPlayer) {
                                                    		if (FactionController.instance.getFactionFromName("Ascended").isFriendlyToPlayer((EntityPlayer)entity) || !PerWorldData.isCreative(targetDimension)) {
                                                                CoreAPI.logAudit(CoreAPI.getLocationString((EntityPlayer)entity) + " is being teleported by rings from : " + this.getPos().toString() + " on : " + this.world.provider.getDimension() + " to : " + targetDimension + " at : " + ePos.toString(), false);
                                                                CoreAPI.teleporter((EntityPlayer)entity, targetDimension, ePos);
                                                                transported = true;
                                                    		} else {
                                                    			CoreAPI.sendMessage((EntityPlayer)entity, "A higher power prevents you travelling to the destination without being friendly with the Ascended...", true);
                                                    			continue;
                                                    		}
                                                    } else {
                                                    	if (!PerWorldData.isCreative(targetDimension)) {
                                                    		FakeTeleporter fakeTeleporter = new FakeTeleporter();
                                                        	entity = entity.changeDimension(targetDimension, fakeTeleporter);
                                                            transported = true;
                                                    	}
                                                    }
                                                } else {
                                                	transported = true;
                                                }
                                                
						if (transported) entity.setPositionAndUpdate(ePos.getX(), ePos.getY(), ePos.getZ());
					}
				}
				
				teleportList.clear();
				excludedEntities.clear();
				
				addTask(new ScheduledTask(EnumScheduledTask.RINGS_CLEAR_OUT));
				break;
				
			case RINGS_CLEAR_OUT:
				setBarrierBlocks(false, false);
				setBusy(false);
				
				TransportRingsTile targetRingsTile = (TransportRingsTile) CoreAPI.getWorldForDimension(targetDimension).getTileEntity(targetRingsPos);
				if (targetRingsTile != null)
					targetRingsTile.setBusy(false);
				
				sendSignal(ocContext, "transportrings_teleport_finished", initiating);
				
				break;
				
			default:
				throw new UnsupportedOperationException("EnumScheduledTask."+scheduledTask.name()+" not implemented on "+this.getClass().getName());
		}
	}
	
	
	// ---------------------------------------------------------------------------------
	// Teleportation
	private BlockPos targetRingsPos = new BlockPos(0, 0, 0);
	private List<Entity> excludedEntities = new ArrayList<>();
        private int targetDimension;
	private Object ocContext;
	private boolean initiating;
	
	/**
	 * True if there is an active transport.
	 */
	private boolean busy = false;
	
	public boolean isBusy() {
		return busy;
	}

	public void setBusy(boolean busy) {
		this.busy = busy;
	}
	
	public List<Entity> startAnimationAndTeleport(BlockPos targetRingsPos, List<Entity> excludedEntities, int waitTime, boolean initiating) {
		this.targetRingsPos = targetRingsPos;
		this.excludedEntities = excludedEntities;
		
		addTask(new ScheduledTask(EnumScheduledTask.RINGS_START_ANIMATION, waitTime));
		sendSignal(ocContext, "transportrings_teleport_start", initiating);
		this.initiating = initiating;
		
		return world.getEntitiesWithinAABB(Entity.class, globalTeleportBox);
	}
	
	public void animationStart() {
		rendererState.animationStart = world.getTotalWorldTime();
		rendererState.ringsUprising = true;
		rendererState.isAnimationActive = true;
				
		TargetPoint point = new TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 512);
		AunisPacketHandler.INSTANCE.sendToAllTracking(new StateUpdatePacketToClient(pos, StateTypeEnum.RINGS_START_ANIMATION, new TransportRingsStartAnimationRequest(rendererState.animationStart)), point);
	}

	/**
	 * Checks if Rings are linked to Rings at given address.
	 * If yes, it starts teleportation.
	 * 
	 * @param address Target rings address
	 */
	
	public void setBufferRandom() {
		Random r = new Random();
	    energyBuffer = r.nextInt((int)energyMax);
		markDirty();
	}
	
	public TransportResult attemptTransportTo(int address, int waitTime) {
                
                if (this.getAddress() == null || this.frequency == -1)
                    return TransportResult.NOT_LINKED;
        
        if (!com.stargatemc.data.LocationData.isPositionProtected(this.world, this.getPos()) && this.energyBuffer < 1000000) {
        	return TransportResult.INSUFFICIENT_POWER;
        }
        
		if (checkIfObstructed()) {
			return TransportResult.OBSTRUCTED;
		}
		
		if (isBusy()) {
			return TransportResult.BUSY;
		}
		
		TransportRings rings = null;
                
                try {
                    rings = RingMap.getRingsForFrequency(this.getAddress(), this.getFrequency()).get(address-1).getRings().getRings();
                } catch (Exception e) {
                    return TransportResult.NO_SUCH_ADDRESS;
                }
                		
		// Binding exists
		if (rings != null) {
			BlockPos targetRingsPos = rings.getPos();
			TransportRingsTile targetRingsTile = RingMap.getRingsForFrequency(this.getAddress(), this.getFrequency()).get(address-1).getRings();
                        
			if (targetRingsTile.getAddress() == null || targetRingsTile.frequency != this.getFrequency()) {
				return TransportResult.NO_SUCH_ADDRESS;
			}
                        
			if (targetRingsTile.checkIfObstructed()) {
				return TransportResult.OBSTRUCTED_TARGET;
			}
			
			if (targetRingsTile.isBusy()) {
				return TransportResult.BUSY_TARGET;
			}
			
                        if (targetRingsTile.equals(this)) {
                            return TransportResult.DIAL_SELF_FAIL;
                        }
                        
			this.setBusy(true);
			targetRingsTile.setBusy(true);
			
			List<Entity> excludedFromReceivingSite = world.getEntitiesWithinAABB(Entity.class, globalTeleportBox);
			this.energyBuffer -= 1000000;
			markDirty();
			List<Entity> excludedEntities = targetRingsTile.startAnimationAndTeleport(pos, excludedFromReceivingSite, waitTime, false);
			this.targetDimension = targetRingsTile.world.provider.getDimension();
                        startAnimationAndTeleport(targetRingsPos, excludedEntities, waitTime, true);
			
			return TransportResult.OK;
		}
		
		else {
			return TransportResult.NO_SUCH_ADDRESS;
		}
	}
	
	private static final List<BlockPos> invisibleBlocksTemplate = Arrays.asList(
			new BlockPos(0, 2, 2),
			new BlockPos(1, 2, 2),
			new BlockPos(2, 2, 1)
	);
	
	private boolean checkIfObstructed() {
		if (AunisConfig.ringsConfig.ignoreObstructionCheck)
			return false;
		
		for(int y=0; y<3; y++) {
			for (Rotation rotation : Rotation.values()) {
				for (BlockPos invPos : invisibleBlocksTemplate) {
					
					BlockPos newPos = new BlockPos(this.pos).add(invPos.rotate(rotation)).add(0, y, 0);
					IBlockState newState = world.getBlockState(newPos);
					Block newBlock = newState.getBlock();
					
					if (!newBlock.isAir(newState, world, newPos) && !newBlock.isReplaceable(world, newPos)) {
						Aunis.info(newPos + " obstructed with " + world.getBlockState(newPos));
						return true;
					}
				}
			}
		}
		
		return false;
	}
	
	private void setBarrierBlocks(boolean set, boolean passable) {
		IBlockState invBlockState = AunisBlocks.INVISIBLE_BLOCK.getDefaultState();
		
		if (passable)
			invBlockState = invBlockState.withProperty(AunisProps.HAS_COLLISIONS, false);
		
		for(int y=1; y<3; y++) {
			for (Rotation rotation : Rotation.values()) {
				for (BlockPos invPos : invisibleBlocksTemplate) {
					BlockPos newPos = this.pos.add(invPos.rotate(rotation)).add(0, y, 0);
					
					if (set)
						world.setBlockState(newPos, invBlockState, 3);
					else {
						if (world.getBlockState(newPos).getBlock() == AunisBlocks.INVISIBLE_BLOCK)
							world.setBlockToAir(newPos);
					}
				}
			}
		}
	}
	
	
	// ---------------------------------------------------------------------------------
	// Controller
	private BlockPos linkedController;
	
	public void setLinkedController(BlockPos pos) {
		this.linkedController = pos;
		
		markDirty();
	}
	
	public BlockPos getLinkedController() {
		return linkedController;
	}
	
	public boolean isLinked() {
		return linkedController != null;
	}
	
	public TRControllerTile getLinkedControllerTile(World world) {
		return (linkedController != null ? ((TRControllerTile) world.getTileEntity(linkedController)) : null);
	}
	
	@Override
	public boolean canLinkTo() {
		return !isLinked();
	}
	
	// ---------------------------------------------------------------------------------
	// Rings network
	private TransportRings rings;
	
	public void clearRings() {
		this.rings = null;
	}
	public TransportRings getRings() {
		if (rings == null)
			rings = new TransportRings(getPos(),world.provider.getDimension());
		
		return rings;
	}
	
	/**
	 * Gets clone of {@link TransportRingsTile#rings} object. Sets the distance from
	 * callerPos to this tile. Called from {@link TransportRingsTile#addRings(TransportRingsTile)}.
	 * 
	 * @param callerPos - calling tile position
	 * @return - clone of this rings info
	 */
	public TransportRings getClonedRings(BlockPos callerPos, int dimension) {
		return getRings().cloneWithNewDistance(callerPos, dimension);
	}
	
	/**
	 * Contains neighborhooding rings(clones of {@link TransportRingsTile#rings}) with distance set to this tile
	 */
	public Map<Integer, TransportRings> ringsMap = new HashMap<>();
	
	/**
	 * Adds rings to {@link TransportRingsTile#ringsMap}, by cloning caller's {@link TransportRingsTile#rings} and
	 * setting distance
	 * 
	 * @param caller - Caller rings tile
	 */
	public void addRings(TransportRingsTile caller) {
		TransportRings clonedRings = caller.getClonedRings(caller.getPos(), caller.world.provider.getDimension());
		
		if (clonedRings.isInGrid()) {
			ringsMap.put(clonedRings.getAddress(), clonedRings);
			
			markDirty();
		}
	}
	
	public void removeRings(int address) {	
		if (ringsMap.remove(address) != null)
			markDirty();
	}
	
	public void removeAllRings() {
		for (TransportRings rings : ringsMap.values()) {
			
			TransportRingsTile ringsTile = (TransportRingsTile) world.getTileEntity(rings.getPos());
			if (ringsTile != null) ringsTile.removeRings(getRings().getAddress());
		}
	}
	
	public ParamsSetResult setRingsParams(int address, String name) {
		int x = pos.getX();
		int z = pos.getZ();

		int radius = AunisConfig.ringsConfig.rangeFlat;
		
		int y = pos.getY();
		int vertical = AunisConfig.ringsConfig.rangeVertical;
		
		List<TransportRingsTile> ringsTilesInRange = new ArrayList<>();
		
		for (BlockPos newRingsPos : BlockPos.getAllInBoxMutable(new BlockPos(x-radius, y-vertical, z-radius), new BlockPos(x+radius, y+vertical, z+radius))) {
			if (world.getBlockState(newRingsPos).getBlock() == AunisBlocks.TRANSPORT_RINGS_BLOCK && !pos.equals(newRingsPos)) {
				
				TransportRingsTile newRingsTile = (TransportRingsTile) world.getTileEntity(newRingsPos);	
				ringsTilesInRange.add(newRingsTile);

				int newRingsAddress = newRingsTile.getClonedRings(pos,newRingsTile.world.provider.getDimension()).getAddress();
				if (newRingsAddress == address && newRingsAddress != -1) {					
					return ParamsSetResult.DUPLICATE_ADDRESS;
				}
			}
		}
		
		removeAllRings();
		
		getRings().setAddress(address);
		getRings().setName(name);
		
		for (TransportRingsTile newRingsTile : ringsTilesInRange) {
			this.addRings(newRingsTile);
			newRingsTile.addRings(this);
		}
		
		markDirty();
		return ParamsSetResult.OK;
	}
	
	
	// ---------------------------------------------------------------------------------
	// NBT data
	
	@Override
	protected void setWorldCreate(World worldIn) {
		setWorld(worldIn);
	}
	
	@Override
	public NBTTagCompound writeToNBT(NBTTagCompound compound) {
		compound.setTag("rendererState", rendererState.serializeNBT());
		
		compound.setTag("ringsData", getRings().serializeNBT());
		if (linkedController != null)
			compound.setLong("linkedController", linkedController.toLong());
		
		compound.setInteger("ringsMapLength", ringsMap.size());
		compound.setInteger("frequency", frequency);
		if (this.address != null) compound.setString("address", address);
		
		int i = 0;
		for (TransportRings rings : ringsMap.values()) {
			compound.setTag("ringsMap" + i, rings.serializeNBT());
			
			i++;
		}
		
		compound.setTag("scheduledTasks", ScheduledTask.serializeList(scheduledTasks));
		
		compound.setInteger("teleportListSize", teleportList.size());
		for (int j=0; j<teleportList.size(); j++)
			compound.setInteger("teleportList"+j, teleportList.get(j).getEntityId());
		
		compound.setInteger("excludedSize", excludedEntities.size());
		for (int j=0; j<excludedEntities.size(); j++)
			compound.setInteger("excluded"+j, excludedEntities.get(j).getEntityId());
		
		compound.setLong("targetRingsPos", targetRingsPos.toLong());
		compound.setBoolean("busy", isBusy());
		compound.setDouble("buffer", this.energyBuffer);
		
		if (node != null) {
			NBTTagCompound nodeCompound = new NBTTagCompound();
			node.save(nodeCompound);
			
			compound.setTag("node", nodeCompound);
		}
		
		compound.setBoolean("initiating", initiating);
		
		return super.writeToNBT(compound);
	}
	
	@Override
	public void readFromNBT(NBTTagCompound compound) {
		try {
			rendererState.deserializeNBT(compound.getCompoundTag("rendererState"));
			ScheduledTask.deserializeList(compound.getCompoundTag("scheduledTasks"), scheduledTasks, this);
			if (compound.hasKey("frequency")) this.frequency = compound.getInteger("frequency");
			if (compound.hasKey("address")) this.address = compound.getString("address");
			if (compound.hasKey("buffer")) this.energyBuffer = compound.getDouble("buffer");
			teleportList = new ArrayList<>();
			int size = compound.getInteger("teleportListSize");
			for (int j=0; j<size; j++)
				teleportList.add(world.getEntityByID(compound.getInteger("teleportList"+j)));
				
			excludedEntities = new ArrayList<>();
			size = compound.getInteger("excludedSize");
			for (int j=0; j<size; j++)
				excludedEntities.add(world.getEntityByID(compound.getInteger("excluded"+j)));
			
			targetRingsPos = BlockPos.fromLong(compound.getLong("targetRingsPos"));
			
		} catch (NullPointerException | IndexOutOfBoundsException | ClassCastException e) {
			Aunis.info("Exception at reading NBT");
			Aunis.info("If loading world used with previous version and nothing game-breaking doesn't happen, please ignore it");

			e.printStackTrace();
		}
		
		if (compound.hasKey("ringsData"))
			getRings().deserializeNBT(compound.getCompoundTag("ringsData"));
		
		if (compound.hasKey("linkedController"))
			linkedController = BlockPos.fromLong(compound.getLong("linkedController"));
		
		if (compound.hasKey("ringsMapLength")) {
			int len = compound.getInteger("ringsMapLength");
			
			ringsMap.clear();
			
			for (int i=0; i<len; i++) {
				TransportRings rings = new TransportRings(compound.getCompoundTag("ringsMap" + i));
				
				ringsMap.put(rings.getAddress(), rings);
			}
		}
		
		if (node != null && compound.hasKey("node"))
			node.load(compound.getCompoundTag("node"));
		
		setBusy(compound.getBoolean("busy"));
		initiating = compound.getBoolean("initiating");
		
		super.readFromNBT(compound);
	}
	
	
	// ---------------------------------------------------------------------------------	
	// States

	@Override
	public State getState(StateTypeEnum stateType) {
		switch (stateType) {
			case RENDERER_STATE:
				return rendererState;
		
			case GUI_STATE:
				return new TransportRingsGuiState(getRings(), ringsMap.values());
				
			default:
				return null;
		}
	}
	
	@Override
	public State createState(StateTypeEnum stateType) {
		switch (stateType) {
			case RENDERER_STATE:
				return new TransportRingsRendererState();
		
			case RINGS_START_ANIMATION:
				return new TransportRingsStartAnimationRequest();
				
			case GUI_STATE:
				return new TransportRingsGuiState();
				
			default:
				return null;
		}
	}
	
	@SideOnly(Side.CLIENT)
	private RingsGUI openGui;
	
	@Override
	@SideOnly(Side.CLIENT)
	public void setState(StateTypeEnum stateType, State state) {		
		switch (stateType) {
			case RENDERER_STATE:
				renderer.setState((TransportRingsRendererState) state);
				break;
		
			case RINGS_START_ANIMATION:
				AunisSoundHelper.playSoundEventClientSide(world, pos.up(3), SoundEventEnum.RINGS_TRANSPORT);
				renderer.animationStart(((TransportRingsStartAnimationRequest) state).animationStart);
				break;
		
			case GUI_STATE:
				
				if (openGui == null || !openGui.isOpen) {
					openGui = new RingsGUI(pos, (TransportRingsGuiState) state);
					Minecraft.getMinecraft().displayGuiScreen(openGui);
				}
				
				else {
					openGui.state = (TransportRingsGuiState) state;
				}
				
				break;
				
			default:
				break;
		}
	}
	
	// ---------------------------------------------------------------------------------
	// Renders
	TransportRingsRenderer renderer;
	TransportRingsRendererState rendererState = new TransportRingsRendererState();
	
	@Override
	public RendererInterface getRenderer() {
		return renderer;
	}
	
	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		return new AxisAlignedBB(pos.add(-4, 0, -4), pos.add(4, 7, 4));
	}
	
	
	// ------------------------------------------------------------------------
	// OpenComputers
	
	@Override
	public void onChunkUnload() {
		if (node != null)
			node.remove();
		
		unload();
	}

	@Override
	public void invalidate() {
		if (node != null)
			node.remove();
		unload();
		super.invalidate();
	}
	
	// ------------------------------------------------------------
	// Node-related work
	private Node node = Aunis.ocWrapper.createNode(this, "transportrings");
	
	@Override
	@Optional.Method(modid = "opencomputers")
	public Node node() {
		return node;
	}

	@Override
	@Optional.Method(modid = "opencomputers")
	public void onConnect(Node node) {}

	@Override
	@Optional.Method(modid = "opencomputers")
	public void onDisconnect(Node node) {}

	@Override
	@Optional.Method(modid = "opencomputers")
	public void onMessage(Message message) {}
	
	public void sendSignal(Object context, String name, Object... params) {
		Aunis.ocWrapper.sendSignalToReachable(node, (Context) context, name, params);
	}
	
	// ------------------------------------------------------------
	// Methods
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] isInGrid(Context context, Arguments args) {
		return new Object[] { rings.isInGrid() };
	}
	
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] getAddress(Context context, Arguments args) {
		if (!rings.isInGrid())
			return new Object[] { "NOT_IN_GRID", "Use setAddressAndName" };
		
		return new Object[] { rings.getAddress() };
	}
	
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] getName(Context context, Arguments args) {
		if (!rings.isInGrid())
			return new Object[] { "NOT_IN_GRID", "Use setAddressAndName" };
		
		return new Object[] { rings.getName() };
	}
        
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] getFrequency(Context context, Arguments args) {
		
		return new Object[] { this.getFrequency() };
	}
        
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] setFrequency(Context context, Arguments args) {
		
		int freq = args.checkInteger(0);
		
		if (freq < 1)
			throw new IllegalArgumentException("bad argument #1 (frequency out of range, allowed <0+>)");
		RingAddressEntry entry = RingMap.getForTileEntity(this);
                if (RingMap.availableSlots(this.getAddress(), freq) < 1)
			throw new IllegalArgumentException("bad argument #1 (frequency allocation exhausted)");
                
		return new Object[] { RingMap.get().updateFrequency(entry, freq) };
	}
        
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] setAddress(Context context, Arguments args) {
		if (!rings.isInGrid())
			return new Object[] { "NOT_IN_GRID", "Use setAddressAndName" };
		
		int address = args.checkInteger(0);
		
		if (address < 1 || address > 6)
			throw new IllegalArgumentException("bad argument #1 (address out of range, allowed <1..6>)");
				
		return new Object[] { setRingsParams(address, rings.getName()) };
	}
	
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] setName(Context context, Arguments args) {
		if (!rings.isInGrid())
			return new Object[] { "NOT_IN_GRID", "Use setAddressAndName" };
		
		String name = args.checkString(0);
		setRingsParams(rings.getAddress(), name);
		
		return new Object[] {};
	}
	
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] setAddressAndName(Context context, Arguments args) {
		int address = args.checkInteger(0);
		String name = args.checkString(1);
		
		if (address < 1 || address > 6)
			throw new IllegalArgumentException("bad argument #1 (address out of range, allowed <1..6>)");
				
		return new Object[] { setRingsParams(address, name) };
	}
	
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] getAvailableRings(Context context, Arguments args) {
		if (!rings.isInGrid())
			return new Object[] { "NOT_IN_GRID", "Use setAddressAndName" };
		
		Map<Integer, String> values = new HashMap<>(ringsMap.size());
		
		for (Map.Entry<Integer, TransportRings> rings : ringsMap.entrySet())
			values.put(rings.getKey(), rings.getValue().getName());
		
		return new Object[] { values };
	}
	
	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] getAvailableRingsAddresses(Context context, Arguments args) {
		if (!rings.isInGrid())
			return new Object[] { "NOT_IN_GRID", "Use setAddressAndName" };
		
		return new Object[] { ringsMap.keySet() };
	}
	

	@Optional.Method(modid = "opencomputers")
	@Callback
	public Object[] attemptTransportTo(Context context, Arguments args) {
		if (!rings.isInGrid())
			return new Object[] { "NOT_IN_GRID", "Use setAddressAndName" };
		
		int address = args.checkInteger(0);

		if (address < 1 || address > 6)
			throw new IllegalArgumentException("bad argument #1 (address out of range, allowed <1..6>)");
		
		ocContext = context;
		
		return new Object[] { attemptTransportTo(address, 0) };
	}
	
    //------------------------- IEnergyAcceptor -------------------------

    @Override
    public boolean acceptsEnergyFrom(IEnergyEmitter emitter, EnumFacing direction) {
    	if (!direction.equals(EnumFacing.UP)) {
    		return true;
    	} else {
    		return false;
    	}
    }

    //------------------------- IEnergySink -------------------------

    @Override
    public double getDemandedEnergy() {
        double eu = Math.min(energyMax - energyBuffer, maxSafeInput);
        return eu;
    }

    @Override
    public double injectEnergy(EnumFacing directionFrom, double amount, double voltage) {
        energyBuffer += amount;
        if (update++ > 10) { // We dont' need 20 packets per second to the client....
            markDirty();
            update = 0;
        }
        return 0;
    }

    @Override
    public int getSinkTier() {
        return powerTier;  //HV
    }

}
