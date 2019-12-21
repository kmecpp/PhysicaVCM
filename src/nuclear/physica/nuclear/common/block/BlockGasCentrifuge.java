package physica.nuclear.common.block;

import net.minecraft.block.material.Material;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;
import physica.api.core.abstraction.Face;
import physica.api.core.misc.IRotatable;
import physica.core.common.CoreItemRegister;
import physica.library.block.BlockBaseContainerModelled;
import physica.nuclear.NuclearReferences;
import physica.nuclear.common.NuclearTabRegister;
import physica.nuclear.common.tile.TileChemicalBoiler;
import physica.nuclear.common.tile.TileGasCentrifuge;

public class BlockGasCentrifuge extends BlockBaseContainerModelled {

	public BlockGasCentrifuge() {
		super(Material.iron);
		setHardness(10);
		setResistance(5);
		setHarvestLevel("pickaxe", 2);
		setCreativeTab(NuclearTabRegister.nuclearPhysicsTab);
		setBlockName(NuclearReferences.PREFIX + "centrifuge");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileGasCentrifuge();
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack item) {
		super.onBlockPlacedBy(world, x, y, z, entity, item);
		IRotatable tile = (IRotatable) world.getTileEntity(x, y, z);
		for (Face dir : Face.VALID) {
			if (dir.ordinal() > 1) {
				if (world.getTileEntity(x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ) instanceof TileChemicalBoiler) {
					tile.setFacing(dir.getOpposite());
				}
			}
		}
	}

	@Override
	public void registerRecipes() {
		addRecipe(this, "ICI", "TMT", "TPT", 'I', "ingotSteel", 'T', CoreItemRegister.itemEmptyCell, 'M', "motor", 'P', "plateSteel", 'C', "circuitAdvanced");

	}

	@Override
	public String getSide() {
		return "Nuclear";
	}

}
