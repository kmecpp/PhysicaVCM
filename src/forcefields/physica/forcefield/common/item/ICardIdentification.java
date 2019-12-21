package physica.forcefield.common.item;

import net.minecraft.item.ItemStack;

import java.util.UUID;

public interface ICardIdentification {

	boolean hasPermission(ItemStack itemStack, Permission permission);

	boolean addPermission(ItemStack itemStack, Permission permission);

	boolean removePermission(ItemStack itemStack, Permission permission);

	String getUsername(ItemStack itemStack);

	void setUsername(ItemStack itemStack, String username);

	UUID getUniqueId(ItemStack itemStack);

	void setUniqueId(ItemStack itemStack, UUID uniqueId);

}
