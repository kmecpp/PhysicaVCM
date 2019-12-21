package physica.nuclear.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import physica.library.block.BlockBaseContainerModelled;
import physica.nuclear.NuclearReferences;
import physica.nuclear.common.NuclearTabRegister;
import physica.nuclear.common.tile.TileMeltedReactor;

import java.util.ArrayList;

public class BlockMeltedReactor extends BlockBaseContainerModelled {

	public BlockMeltedReactor() {
		super(Material.iron);
		setHardness(250.0f);
		setResistance(999.0f);
		setHarvestLevel("pickaxe", 3);
		setCreativeTab(NuclearTabRegister.nuclearPhysicsTab);
		setBlockName(NuclearReferences.PREFIX + "meltedReactor");
		setBlockBounds(1.0f / 8.0f, 0, 1.0f / 8.0f, 1 - 1.0f / 8.0f, 5.0f / 8.0f, 1 - 1.0f / 8.0f);
	}

	@Override
	public ArrayList<ItemStack> getDrops(World world, int x, int y, int z, int metadata, int fortune) {
		return new ArrayList<>();
	}

	@Override
	public String getSide() {
		return "Nuclear";
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileMeltedReactor();
	}

	@Override
	public void registerRecipes() {
	}

}
