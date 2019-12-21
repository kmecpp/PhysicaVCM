package physica.nuclear.client.gui;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.StatCollector;
import physica.api.core.utilities.IBaseUtilities;
import physica.library.client.gui.GuiContainerBase;
import physica.library.energy.ElectricityDisplay;
import physica.library.energy.ElectricityUtilities;
import physica.library.energy.base.Unit;
import physica.library.inventory.tooltip.ToolTipTank;
import physica.nuclear.NuclearReferences;
import physica.nuclear.common.inventory.ContainerCentrifuge;
import physica.nuclear.common.tile.TileGasCentrifuge;

import java.awt.Rectangle;

@SideOnly(Side.CLIENT)
public class GuiCentrifuge extends GuiContainerBase<TileGasCentrifuge> implements IBaseUtilities {

	public Rectangle AREA_HEX_TANK = new Rectangle(8, 18, meterWidth, meterHeight);

	public GuiCentrifuge(EntityPlayer player, TileGasCentrifuge host) {
		super(new ContainerCentrifuge(player, host), host);
		ySize += 10;
	}

	@Override
	public void initGui() {
		super.initGui();
		addToolTip(new ToolTipTank(AREA_HEX_TANK, "gui.centrifuge.hex_tank", host.getTank()));
	}

	@Override
	protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		super.drawGuiContainerForegroundLayer(mouseX, mouseY);
		drawString("Status: " + (host.hasEnoughEnergy() ? host.canProcess() ? "Processing" : "Lacking hexafluoride" : "Insufficient Power"), 8, 73);
		drawString("Usage: " + ElectricityDisplay.getDisplayShort(ElectricityUtilities.convertEnergy(host.getPowerUsage(), Unit.RF, Unit.WATT), Unit.WATT), 8, 83);
		drawStringCentered(StatCollector.translateToLocal("tile." + NuclearReferences.PREFIX + "centrifuge.gui"), xSize / 2, 5);
	}

	@Override
	protected void drawGuiContainerBackgroundLayer(float f, int mouseX, int mouseY) {
		super.drawGuiContainerBackgroundLayer(f, mouseX, mouseY);
		drawFluidTank(AREA_HEX_TANK.x, AREA_HEX_TANK.y, host.getTank());
		renderFurnaceCookArrow(36, 36, host.getOperatingTicks(), TileGasCentrifuge.TICKS_REQUIRED);
	}

}
