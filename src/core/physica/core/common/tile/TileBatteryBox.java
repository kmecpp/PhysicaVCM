package physica.core.common.tile;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import physica.api.core.abstraction.AbstractionLayer;
import physica.api.core.abstraction.Face;
import physica.api.core.electricity.IElectricityHandler;
import physica.api.core.inventory.IGuiInterface;
import physica.core.client.gui.GuiBatteryBox;
import physica.core.common.block.BlockBatteryBox.EnumBatteryBox;
import physica.core.common.inventory.ContainerBatteryBox;
import physica.library.location.GridLocation;
import physica.library.tile.TileBasePoweredContainer;

public class TileBatteryBox extends TileBasePoweredContainer implements IElectricityHandler, IGuiInterface {

	public static final int SLOT_INPUT = 0;
	public static final int SLOT_OUTPUT = 1;
	private static final int[] ACCESSIBLE_SLOTS_DOWN = new int[] {SLOT_OUTPUT};
	private static final int[] ACCESSIBLE_SLOTS_UP = new int[] {SLOT_INPUT};
	private TileEntity cachedOutput;

	@Override
	public int getElectricCapacity(Face from) {
		return EnumBatteryBox.values()[getBlockMetadata()].getCapacity();
	}

	@Override
	public void updateServer(int ticks) {
		super.updateServer(ticks);
		drainBattery(SLOT_INPUT);

		Face out = getFacing().getOpposite().getRelativeSide(Face.WEST);
		if (cachedOutput == null || cachedOutput.isInvalid()) {
			cachedOutput = null;
			GridLocation loc = getLocation();
			TileEntity outputTile = World().getTileEntity(loc.xCoord + out.offsetX, loc.yCoord + out.offsetY, loc.zCoord + out.offsetZ);
			if (AbstractionLayer.Electricity.isElectricReceiver(outputTile)) {
				cachedOutput = outputTile;
			}
		}
		if (cachedOutput != null) {
			if (AbstractionLayer.Electricity.canConnectElectricity(cachedOutput, out.getOpposite())) {
				setElectricityStored(getElectricityStored() - AbstractionLayer.Electricity.receiveElectricity(cachedOutput, out.getOpposite(), Math.min(getElectricCapacity(Face.UNKNOWN) / 500, getElectricityStored()), false));
			}
		}
		fillBattery(SLOT_OUTPUT);
	}

	@Override
	public int extractElectricity(Face from, int maxExtract, boolean simulate) {
		int removed = from != getFacing().getOpposite().getRelativeSide(Face.WEST) ? 0 : Math.min(Math.min(getElectricCapacity(Face.UNKNOWN) / 500, getElectricityStored()), Math.min(getElectricityStored(from), maxExtract));
		if (!simulate) {
			setElectricityStored(getElectricityStored(from) - removed);
		}
		return removed;
	}

	@Override
	public int receiveElectricity(Face from, int maxReceive, boolean simulate) {
		maxReceive = Math.min(maxReceive, getElectricCapacity(Face.UNKNOWN) / 500);
		int capacityLeft = getElectricCapacity(from) - getElectricityStored();
		int received = from != getFacing().getOpposite().getRelativeSide(Face.EAST) ? 0 : Math.min(maxReceive, capacityLeft);
		if (!simulate) {
			setElectricityStored(getElectricityStored() + received);
		}
		return received;
	}

	@Override
	public boolean canConnectElectricity(Face from) {
		return from == getFacing().getOpposite().getRelativeSide(Face.WEST) || from == getFacing().getOpposite().getRelativeSide(Face.EAST);
	}

	@Override
	public int[] getAccessibleSlotsFromFace(Face face) {
		return face == Face.UP ? ACCESSIBLE_SLOTS_UP : face == Face.DOWN ? ACCESSIBLE_SLOTS_DOWN : ACCESSIBLE_SLOTS_NONE;
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		return AbstractionLayer.Electricity.isItemElectric(stack);
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, Face face) {
		return face == Face.UP ? slot == SLOT_INPUT && isItemValidForSlot(slot, stack) : false;
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, Face face) {
		return face == Face.DOWN ? slot == SLOT_OUTPUT && isItemValidForSlot(slot, stack) : false;
	}

	@Override
	public int getSizeInventory() {
		return 2;
	}

	@Override
	public int getElectricityStored(Face from) {
		return super.getElectricityStored(from);
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getClientGuiElement(int id, EntityPlayer player) {
		return new GuiBatteryBox(player, this);
	}

	@Override
	public Container getServerGuiElement(int id, EntityPlayer player) {
		return new ContainerBatteryBox(player, this);
	}

}
