package physica.forcefield.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import physica.forcefield.ForcefieldReferences;
import physica.forcefield.common.ForcefieldItemRegister;
import physica.forcefield.common.ForcefieldTabRegister;
import physica.forcefield.common.tile.TileCoercionDriver;
import physica.library.block.BlockBaseContainerModelled;

public class BlockCoercionDriver extends BlockBaseContainerModelled {

	public BlockCoercionDriver() {
		super(Material.iron);
		setHardness(10);
		setResistance(500);
		setHarvestLevel("pickaxe", 2);
		setCreativeTab(ForcefieldTabRegister.forcefieldTab);
		setBlockName(ForcefieldReferences.PREFIX + "coercionDriver");
		setBlockBounds(0, 0, 0, 1, 0.95f, 1);
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileCoercionDriver();
	}

	@Override
	public void registerRecipes() {
		addRecipe(this, "SES", "FBF", "SES", 'S', "plateSteel", 'F', ForcefieldItemRegister.itemFocusMatrix, 'B', "phyBattery", 'E', "circuitElite");
	}

	@Override
	public String getSide() {
		return "Forcefields";
	}

}
