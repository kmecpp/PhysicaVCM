package physica.library.inventory.tooltip;

import net.minecraft.util.StatCollector;

import java.awt.Rectangle;

public class ToolTip {

	public Rectangle area;
	public String info;

	public ToolTip(Rectangle area, String info) {
		this.area = area;
		this.info = info.trim();
	}

	public String getLocalizedTooltip() {
		return localize(info);
	}

	protected String localize(String key) {
		String base = "";
		return key != null ? (base = StatCollector.translateToLocal(key)).isEmpty() ? key : base : base;
	}

	public boolean shouldShowAt(int x, int y) {
		return area.contains(x, y);
	}

	public boolean shouldShow() {
		return true;
	}

}
