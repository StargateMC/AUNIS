package mrjake.aunis.block;

import mrjake.aunis.Aunis;
import mrjake.aunis.packet.AunisPacketHandler;
import mrjake.aunis.packet.StateUpdatePacketToClient;
import mrjake.aunis.state.StateTypeEnum;
import mrjake.aunis.tileentity.AsgardTransportTile;
import mrjake.aunis.tileentity.TRControllerTile;
import mrjake.aunis.tileentity.TransportRingsTile;
import mrjake.aunis.util.LinkingHelper;
import mrjake.aunis.util.RingMap;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.item.Item;

public class AsgardTransportBlock extends Block{
	
	private static final String blockName = "asgardtransport_block";
	  
	  public AsgardTransportBlock() {
		  super(Material.IRON);
			
			setRegistryName(Aunis.ModID + ":" + blockName);
			setTranslationKey(Aunis.ModID + "." + blockName);
			
			setSoundType(SoundType.STONE); 
			setCreativeTab(Aunis.aunisCreativeTab);
			
			setLightOpacity(0);
			
			setHardness(3.0f);
			setHarvestLevel("pickaxe", 3);
	  }
	  
	  @Override
		public boolean hasTileEntity(IBlockState state) {
			return true;
		}
		
		@Override
		public AsgardTransportTile createTileEntity(World world, IBlockState state) {
			return new AsgardTransportTile();
		}
	}
