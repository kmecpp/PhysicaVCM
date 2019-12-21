package physica.nuclear.common.tile;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.util.ForgeDirection;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTank;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import physica.api.core.abstraction.AbstractionLayer;
import physica.api.core.abstraction.Face;
import physica.api.core.inventory.IGuiInterface;
import physica.library.energy.ElectricityUtilities;
import physica.library.energy.base.Unit;
import physica.library.recipe.RecipeSystem;
import physica.library.tile.TileBasePoweredContainer;
import physica.nuclear.client.gui.GuiChemicalExtractor;
import physica.nuclear.common.inventory.ContainerChemicalExtractor;
import physica.nuclear.common.recipe.type.ChemicalExtractorRecipe;

import java.util.List;

public class TileChemicalExtractor extends TileBasePoweredContainer implements IGuiInterface, IFluidHandler {

	public static final int TICKS_REQUIRED = 400;
	public static final int POWER_USAGE = ElectricityUtilities.convertEnergy(1500, Unit.WATT, Unit.RF);
	public static final int SLOT_ENERGY = 0;
	public static final int SLOT_INPUT = 1;
	public static final int SLOT_OUTPUT = 2;
	private static final int[] ACCESSIBLE_SLOTS_DOWN = new int[] {SLOT_OUTPUT};
	private static final int[] ACCESSIBLE_SLOTS_UP = new int[] {SLOT_INPUT};

	protected FluidTank waterTank = new FluidTank(new FluidStack(FluidRegistry.WATER, 0), 5000);

	protected int operatingTicks = 0;

	@Override
	public void updateServer(int ticks) {
		super.updateServer(ticks);
		if (hasEnoughEnergy()) {
			ItemStack output = getStackInSlot(SLOT_OUTPUT);
			ItemStack input = getStackInSlot(SLOT_INPUT);
			if (canProcess(output, input)) {
				if (operatingTicks < TICKS_REQUIRED) {
					operatingTicks++;
				} else {
					process(input, output);
					operatingTicks = 0;
				}
				extractEnergy();
			} else {
				operatingTicks = 0;
			}
			drainBattery(SLOT_ENERGY);
		} else {
			drainBattery(SLOT_ENERGY);
		}
		ItemStack input = getStackInSlot(SLOT_INPUT);
		if (input != null) {
			if (input.getItem() == Items.water_bucket && waterTank.getFluidAmount() < waterTank.getCapacity()) {
				setInventorySlotContents(SLOT_INPUT, new ItemStack(Items.bucket));
				waterTank.fill(new FluidStack(FluidRegistry.WATER, 1000), true);
			}
		}
	}

	public boolean canProcess(ItemStack output, ItemStack input) {
		if (input != null) {
			ChemicalExtractorRecipe recipe = RecipeSystem.getRecipe(getClass(), input);
			if (recipe != null) {
				if (waterTank.getFluidAmount() > recipe.getWaterUse() && (output == null ? true : output.getItem() == recipe.getOutput().getItem() && output.stackSize + recipe.getOutput().stackSize <= output.getMaxStackSize())) {
					return true;
				}
			}
		}
		return false;
	}

	private void process(ItemStack input, ItemStack output) {
		ChemicalExtractorRecipe recipe = RecipeSystem.getRecipe(getClass(), input);
		waterTank.drain(recipe.getWaterUse(), true);

		input.stackSize--;
		if (input.stackSize <= 0) {
			setInventorySlotContents(SLOT_INPUT, null);
		} else {
			setInventorySlotContents(SLOT_INPUT, input);
		}

		if (output != null) {
			output.stackSize = output.stackSize + recipe.getOutput().stackSize;
		} else {
			setInventorySlotContents(SLOT_OUTPUT, new ItemStack(recipe.getOutput().getItem(), recipe.getOutput().stackSize));
		}

	}

	@Override
	public void writeToNBT(NBTTagCompound nbt) {
		super.writeToNBT(nbt);
		NBTTagCompound compound = new NBTTagCompound();
		waterTank.writeToNBT(compound);
		nbt.setTag("Tank", compound);
	}

	@Override
	public void readFromNBT(NBTTagCompound nbt) {
		super.readFromNBT(nbt);
		NBTTagCompound compound = (NBTTagCompound) nbt.getTag("Tank");
		waterTank.readFromNBT(compound);
	}

	@Override
	public void writeClientGuiPacket(List<Object> dataList, EntityPlayer player) {
		super.writeClientGuiPacket(dataList, player);
		dataList.add(operatingTicks);
		dataList.add(waterTank.writeToNBT(new NBTTagCompound()));
	}

	@Override
	public void readClientGuiPacket(ByteBuf buf, EntityPlayer player) {
		super.readClientGuiPacket(buf, player);
		operatingTicks = buf.readInt();
		waterTank.readFromNBT(ByteBufUtils.readTag(buf));
	}

	public int getOperatingTicks() {
		return operatingTicks;
	}

	public FluidTank getTank() {
		return waterTank;
	}

	@Override
	public int getSizeInventory() {
		return 3;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return slot != SLOT_OUTPUT && stack != null
			       && (slot == SLOT_ENERGY ? AbstractionLayer.Electricity.isItemElectric(stack) : slot == SLOT_INPUT && stack.getItem() == Items.water_bucket || RecipeSystem.isRecipeInput(getClass(), stack));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getClientGuiElement(int id, EntityPlayer player) {
		return new GuiChemicalExtractor(player, this);
	}

	@Override
	public Container getServerGuiElement(int id, EntityPlayer player) {
		return new ContainerChemicalExtractor(player, this);
	}

	@Override
	public int getPowerUsage() {
		return POWER_USAGE;
	}

	@Override
	public int[] getAccessibleSlotsFromFace(Face face) {
		return face == Face.DOWN ? ACCESSIBLE_SLOTS_DOWN : face == Face.UP ? ACCESSIBLE_SLOTS_UP : ACCESSIBLE_SLOTS_NONE;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, Face face) {
		return isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, Face face) {
		return true;
	}

	@Override
	public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
		return waterTank.fill(resource, doFill);
	}

	@Override
	public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
		return null;
	}

	@Override
	public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
		return null;
	}

	@Override
	public boolean canFill(ForgeDirection from, Fluid fluid) {
		return fluid == FluidRegistry.WATER;
	}

	@Override
	public boolean canDrain(ForgeDirection from, Fluid fluid) {
		return false;
	}

	@Override
	public FluidTankInfo[] getTankInfo(ForgeDirection from) {
		return new FluidTankInfo[] {waterTank.getInfo()};
	}

}
