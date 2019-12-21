package physica.core.common;

import physica.CoreReferences;
import physica.api.core.abstraction.AbstractionLayer;
import physica.api.core.load.IContent;
import physica.api.core.load.LoadPhase;
import physica.core.common.items.ItemBattery;
import physica.core.common.items.ItemMultimeter;
import physica.core.common.items.ItemWrench;
import physica.library.item.ItemDescriptable;
import physica.library.item.ItemMetaHolder;

public class CoreItemRegister implements IContent {

	public static ItemDescriptable itemEmptyCell;
	public static ItemMetaHolder itemMetaCircuit;
	public static ItemMetaHolder itemMetaPlate;
	public static ItemMetaHolder itemMetaIngot;
	public static ItemMetaHolder itemMetaBlend;
	public static ItemDescriptable itemMotor;
	public static ItemWrench itemWrench;
	public static ItemBattery itemBattery;
	public static ItemMultimeter itemMultimeter;

	@Override
	public void register(LoadPhase phase) {
		if (phase == LoadPhase.RegisterObjects) {
			AbstractionLayer.Registering.registerItem(itemMetaCircuit = new ItemMetaHolder("circuit_basic", CoreReferences.PREFIX).addSubItem("circuit_advanced").addSubItem("circuit_elite"), "item.metaCircuit");
			AbstractionLayer.Registering.registerItem(itemMetaPlate = new ItemMetaHolder("plateIron", CoreReferences.PREFIX).addSubItem("plateSteel").addSubItem("plateLead"), "item.metaPlate");
			AbstractionLayer.Registering.registerItem(
				itemMetaIngot = new ItemMetaHolder("ingotTin", CoreReferences.PREFIX).addSubItem("ingotCopper").addSubItem("ingotSteel").addSubItem("ingotLead").addSubItem("ingotSilver").addSubItem("ingotSuperConductive"),
				"item.metaIngot");
			AbstractionLayer.Registering.registerItem(itemMetaBlend = new ItemMetaHolder("blendSuperConductive", CoreReferences.PREFIX), "item.metaBlend");
			AbstractionLayer.Registering.registerItem(itemWrench = new ItemWrench(), itemWrench.getUnlocalizedName());
			AbstractionLayer.Registering.registerItem(itemMotor = (ItemDescriptable) new ItemDescriptable(CoreReferences.PREFIX, "motor").setMaxStackSize(64), itemMotor.getUnlocalizedName());
			AbstractionLayer.Registering.registerItem(itemBattery = new ItemBattery("phyBattery"), itemBattery.getUnlocalizedName());
			AbstractionLayer.Registering.registerItem(itemMultimeter = new ItemMultimeter(), itemMultimeter.getUnlocalizedName());
			AbstractionLayer.Registering.registerItem(itemEmptyCell = (ItemDescriptable) new ItemDescriptable(CoreReferences.PREFIX, "emptyCell").setMaxStackSize(64), itemEmptyCell.getUnlocalizedName());
			itemMetaPlate.setTextureFolder("plate");
			itemMetaIngot.setTextureFolder("ingot");
			itemMetaBlend.setTextureFolder("blend");
			itemMetaCircuit.setTextureFolder("circuit");
			itemMetaPlate.addOreDictionaryInput("plateIron", 0);
			itemMetaPlate.addOreDictionaryInput("plateSteel", 1);
			itemMetaPlate.addOreDictionaryInput("plateLead", 2);
			itemMetaIngot.addOreDictionaryInput("ingotTin", 0);
			itemMetaIngot.addOreDictionaryInput("ingotCopper", 1);
			itemMetaIngot.addOreDictionaryInput("ingotSteel", 2);
			itemMetaIngot.addOreDictionaryInput("ingotLead", 3);
			itemMetaIngot.addOreDictionaryInput("ingotSilver", 4);
			itemMetaIngot.addOreDictionaryInput("ingotSuperConductive", 5);
			itemMetaBlend.addOreDictionaryInput("blendSuperConductive", 0);
			itemMetaCircuit.addOreDictionaryInput("circuitBasic", 0);
			itemMetaCircuit.addOreDictionaryInput("circuitAdvanced", 1);
			itemMetaCircuit.addOreDictionaryInput("circuitElite", 2);
			itemMotor.addOreDictionaryInput("motor", 0);
			AbstractionLayer.Registering.registerOre("phyBattery", CoreItemRegister.itemBattery);
		}
	}

}
