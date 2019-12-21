package physica.api.core.inventory;

import net.minecraft.entity.player.EntityPlayer;

import java.util.Collection;

public interface IPlayerUsing {

	default boolean addPlayerUsingGui(EntityPlayer player) {
		return getPlayersUsingGui().contains(player) ? true : getPlayersUsingGui().add(player);
	}

	Collection<EntityPlayer> getPlayersUsingGui();

	default boolean removePlayerUsingGui(EntityPlayer player) {
		return getPlayersUsingGui().contains(player) ? getPlayersUsingGui().remove(player) : true;
	}

}
