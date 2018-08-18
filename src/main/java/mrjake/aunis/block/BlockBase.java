package mrjake.aunis.block;

import mrjake.aunis.Aunis;
import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;

public class BlockBase extends Block {
	public BlockBase(Material material, SoundType sound, String name) {
		super(material);
		
		setRegistryName(name);
		setUnlocalizedName(Aunis.ModID + "." + name);
		
		setSoundType(sound); 
		setCreativeTab(Aunis.aunisCreativeTab);
		// setCreativeTab(Aunis.oreModTab);
	}
	
	public Item getItemBlock() {
		return new ItemBlock(this).setRegistryName(this.getRegistryName());
	}
	
	public void registerModel() {
		Aunis.proxy.registerItemRenderer(Item.getItemFromBlock(this), 0, getRegistryName());
	}
}