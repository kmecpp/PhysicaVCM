package physica.forcefield.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import physica.forcefield.ForcefieldReferences;
import physica.forcefield.common.ForcefieldItemRegister;
import physica.forcefield.common.ForcefieldTabRegister;
import physica.forcefield.common.tile.TileInterdictionMatrix;
import physica.library.block.BlockBaseContainerModelled;

public class BlockInterdictionMatrix extends BlockBaseContainerModelled {

	public BlockInterdictionMatrix() {
		super(Material.iron);
		setHardness(10);
		setResistance(500);
		setHarvestLevel("pickaxe", 2);
		setCreativeTab(ForcefieldTabRegister.forcefieldTab);
		setBlockName(ForcefieldReferences.PREFIX + "interdictionMatrix");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileInterdictionMatrix();
	}

	@Override
	public void registerRecipes() {
		addRecipe(this, "BEB", "ESE", "BEB", 'B', "phyBattery", 'E', "circuitElite", 'S', ForcefieldItemRegister.moduleMap.get("moduleUpgradeShock"));
	}

	@Override
	public boolean shouldCheckWeakPower(IBlockAccess world, int x, int y, int z, int side) {
		return true;
	}

	@Override
	public int isProvidingStrongPower(IBlockAccess p_149748_1_, int p_149748_2_, int p_149748_3_, int p_149748_4_, int p_149748_5_) {
		return 15;
	}
	
	@Override
	public int isProvidingWeakPower(IBlockAccess world, int x, int y, int z, int flag) {
//		TileEntity tile = world.getTileEntity(x, y, z);
//		System.out.println("CHECK");
//		if (tile instanceof TileInterdictionMatrix) {
//			System.out.println("YEPP!");
//			return ((TileInterdictionMatrix) tile).isEmittingRedstoneSignal() ? 15 : 0;
//		}
		return 15;
	}

	@Override
	public boolean canConnectRedstone(IBlockAccess world, int x, int y, int z, int side) {
		return true;
	}

	@Override
	public boolean canProvidePower() {
		return true;
	}

	@Override
	public String getSide() {
		return "Forcefields";
	}

}
