package physica.library.inventory.slot;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.inventory.IInventory;
import org.lwjgl.opengl.GL11;
import physica.library.client.gui.GuiContainerBase;
import physica.library.inventory.tooltip.ToolTip;
import physica.library.inventory.tooltip.ToolTipSlot;

public class SlotFluidHolder extends SlotBase {

	private final ToolTipSlot toolTipBase;

	public SlotFluidHolder(IInventory inventory, int slotIndex, int x, int y, String info) {
		super(inventory, slotIndex, x, y);
		toolTipBase = new ToolTipSlot(this, info);
	}

	@Override
	protected void drawIcon(Gui gui, int x, int y) {
		Minecraft.getMinecraft().renderEngine.bindTexture(GuiContainerBase.GUI_COMPONENTS);
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
		gui.drawTexturedModalRect(x, y, 0, 18 * 2, 18, 18);
	}

	@Override
	public ToolTip getToolTip() {
		return toolTipBase;
	}

}
