package physica.library.inventory;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import physica.api.core.inventory.IPlayerUsing;
import physica.api.core.tile.ITileBaseContainer;
import physica.library.inventory.slot.SlotNull;

import java.util.List;

public class ContainerBase<T extends IPlayerUsing> extends Container {

	protected T host;
	protected EntityPlayer player;
	protected int slotCount;

	public ContainerBase(EntityPlayer player, T node) {
		host = node;
		this.player = player;
	}

	public ContainerBase(EntityPlayer player, T node, Object fillerNull) {
		host = node;
		this.player = player;
		addDefaultPlayerInventory(player, 0);
	}

	public void setSlotCount(int count) {
		this.slotCount = count;
	}

	@Override
	public void onContainerClosed(EntityPlayer entityplayer) {
		if (entityplayer.openContainer == this) {
			host.removePlayerUsingGui(entityplayer);
		}
		super.onContainerClosed(entityplayer);
	}

	@SuppressWarnings("unchecked")
	@Override
	public ItemStack slotClick(int slot, int p_75144_2_, int p_75144_3_, EntityPlayer player) {
		ItemStack toReturn = super.slotClick(slot, p_75144_2_, p_75144_3_, player);

		if (slot >= 0 && slot < inventorySlots.size()) {
			ItemStack itemstack = ((Slot) inventorySlots.get(slot)).getStack();
			ItemStack itemstack1 = (ItemStack) inventoryItemStacks.get(slot);

			if (!ItemStack.areItemStacksEqual(itemstack1, itemstack)) {
				itemstack1 = itemstack == null ? null : itemstack.copy();
				((List<ItemStack>) inventoryItemStacks).set(slot, itemstack1);

				for (int j = 0; j < crafters.size(); ++j) {
					((ICrafting) crafters.get(j)).sendSlotContents(this, slot, itemstack1);
				}
			}
		}
		return toReturn;
	}

	@Override
	public Slot getSlot(int index) {
		if (index < inventorySlots.size()) {
			return (Slot) inventorySlots.get(index);
		}
		return new SlotNull(null, 0, 0, 0);
	}

	public void addDefaultPlayerInventory(EntityPlayer player, int offset) {
		host.addPlayerUsingGui(player);

		int defaultx = 8, defaultY = 84 + offset;
		for (int row = 0; row < 3; ++row) {
			for (int slot = 0; slot < 9; ++slot) {
				addSlotToContainer(new Slot(player.inventory, slot + row * 9 + 9, slot * 18 + defaultx, row * 18 + defaultY));
			}
		}
		for (int slot = 0; slot < 9; ++slot) {
			addSlotToContainer(new Slot(player.inventory, slot, slot * 18 + defaultx, 58 + defaultY));
		}
	}

	@Override
	protected boolean mergeItemStack(ItemStack stack, int min, int max, boolean negative) {
		boolean flag1 = false;
		int k = negative ? max - 1 : min;
		Slot slot;
		ItemStack itemstack1;
		if (stack.isStackable()) {
			while (stack.stackSize > 0 && (!negative && k < max || negative && k >= min)) {
				slot = (Slot) inventorySlots.get(k);
				itemstack1 = slot.getStack();

				if (itemstack1 != null && itemstack1.getItem() == stack.getItem() && (!stack.getHasSubtypes() || stack.getItemDamage() == itemstack1.getItemDamage()) && ItemStack.areItemStackTagsEqual(stack, itemstack1)) {

					int l = itemstack1.stackSize + stack.stackSize;

					if (l <= stack.getMaxStackSize()) {
						stack.stackSize = 0;
						itemstack1.stackSize = l;
						slot.onSlotChanged();
						flag1 = true;
					} else if (itemstack1.stackSize < stack.getMaxStackSize()) {
						stack.stackSize -= stack.getMaxStackSize() - itemstack1.stackSize;
						itemstack1.stackSize = stack.getMaxStackSize();
						slot.onSlotChanged();
						flag1 = true;
					}
				}

				if (negative) {
					--k;
				} else {
					++k;
				}
			}
		}

		if (stack.stackSize > 0) {
			k = negative ? max - 1 : min;
			while (!negative && k < max || negative && k >= min) {
				slot = (Slot) inventorySlots.get(k);
				itemstack1 = slot.getStack();

				if (itemstack1 == null) {
					if (slot.isItemValid(stack)) {
						slot.putStack(stack.copy());
						slot.onSlotChanged();
						stack.stackSize = 0;
						flag1 = true;
						break;
					}
				}

				if (negative) {
					--k;
				} else {
					++k;
				}
			}
		}
		if (flag1 && host instanceof ITileBaseContainer) {
			((ITileBaseContainer) host).onInventoryChanged();
		}
		return flag1;
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int index) {
		Slot slot = (Slot) inventorySlots.get(index);

		if (slot != null && slot.getStack() != null) {
			ItemStack itemStack = slot.getStack();
			ItemStack originalStack = itemStack.copy();

			if (index < slotCount) {
				if (!mergeItemStack(itemStack, slotCount, inventorySlots.size(), true)) {
					return null;
				}
			} else if (!mergeItemStack(itemStack, 0, slotCount, false)) {
				return null;
			}

			if (itemStack.stackSize == 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}

			return originalStack;
		}
		return null;
	}

	@Override
	public boolean canInteractWith(EntityPlayer entityplayer) {
		return host instanceof IInventory ? ((IInventory) host).isUseableByPlayer(entityplayer)
			       : (boolean) (host instanceof TileEntity ? entityplayer.getDistanceSq(((TileEntity) host).xCoord, ((TileEntity) host).yCoord, ((TileEntity) host).zCoord) <= 64 : true);
	}

}
