package physica.api.core.item;

import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import java.util.Random;

public interface IItemUpdate {

	Random rand = new Random();

	default void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean par5) {
	}

	default void onEntityItemUpdate(ItemStack stack, EntityItem entity) {
	}

}
