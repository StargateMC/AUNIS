package mrjake.aunis.tileentity;

import net.minecraft.util.EnumFacing;
import com.stargatemc.api.CoreAPI;
import gcewing.sg.util.FakeTeleporter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import mrjake.aunis.tesr.RendererInterface;
import mrjake.aunis.tesr.RendererProviderInterface;
import mrjake.aunis.tileentity.util.ScheduledTask;
import mrjake.aunis.tileentity.util.ScheduledTaskExecutorInterface;
import mrjake.aunis.util.AunisAxisAlignedBB;
import mrjake.aunis.util.ILinkable;
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
public class AsgardTransportTile extends TileEntity implements IEnergySink, ITickable, RendererProviderInterface, StateProviderInterface, ScheduledTaskExecutorInterface, ILinkable, Environment{

	
	 private boolean loaded = false;
	    private int maxSafeInput = 32768;
	    private int powerTier = 6;
	    private double energyBuffer = 0;
	    private int update = 0;
	    private double energyMax = 1000000;

	
	private Node node = Aunis.ocWrapper.createNode(this, "Asgard Transporter");
	@Override
	@Optional.Method(modid = "opencomputers")
	public Node node() {
		return node;
	}
	
	void unload() {
        if (!world.isRemote && loaded) {
            MinecraftForge.EVENT_BUS.post(new EnergyTileUnloadEvent(this));
            loaded = false;
        }
    }
	
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


	@Override
	public boolean canLinkTo() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void addTask(ScheduledTask scheduledTask) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void executeTask(EnumScheduledTask scheduledTask, NBTTagCompound customData) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public State getState(StateTypeEnum stateType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public State createState(StateTypeEnum stateType) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setState(StateTypeEnum stateType, State state) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public RendererInterface getRenderer() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void update() {
		// TODO Auto-generated method stub
		
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
