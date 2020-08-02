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

	@Override
	public boolean acceptsEnergyFrom(IEnergyEmitter arg0, EnumFacing arg1) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Node node() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void onConnect(Node arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onDisconnect(Node arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onMessage(Message arg0) {
		// TODO Auto-generated method stub
		
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

	@Override
	public double getDemandedEnergy() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int getSinkTier() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public double injectEnergy(EnumFacing arg0, double arg1, double arg2) {
		// TODO Auto-generated method stub
		return 0;
	}
}
