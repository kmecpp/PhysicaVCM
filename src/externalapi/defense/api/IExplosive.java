package defense.api;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import defense.client.model.missile.ModelMissileBase;
import net.minecraft.client.model.ModelBase;
import net.minecraft.entity.Entity;
import net.minecraft.util.IIcon;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;

/**
 * An interface used to find various types of explosive's information.
 *
 * @author Calclavia
 */
public interface IExplosive extends ITier {

	/**
	 * @return Gets the explosive's ID.
	 */
	public int getID();

	/**
	 * @return The unique name key in the language file.
	 */
	public String getUnlocalizedName();

	/**
	 * @return Gets the specific translated name of the block versions of the
	 * explosive.
	 */
	public String getExplosiveName();

	/**
	 * @return Gets the specific translated name of the grenade versions of the
	 * explosive.
	 */
	public String getGrenadeName();

	/**
	 * @return Gets the specific translated name of the missile versions of the
	 * explosive.
	 */
	public String getMissileName();

	/**
	 * @return Gets the specific translated name of the minecart versions of the
	 * explosive.
	 */
	public String getMinecartName();

	/**
	 * @return The tier of the explosive.
	 */
	@Override
	public int getTier();

	/**
	 * Creates a new explosion at a given location.
	 *
	 * @param world  The world in which the explosion takes place.
	 * @param x      The X-Coord
	 * @param y      The Y-Coord
	 * @param z      The Z-Coord
	 * @param entity Entity that caused the explosion.
	 */
	public void createExplosion(World world, double x, double y, double z, Entity entity);

	@SideOnly(Side.CLIENT)
	public ModelBase getBlockModel();

	@SideOnly(Side.CLIENT)
	public ModelMissileBase getMissileModel();

	@SideOnly(Side.CLIENT)
	public ResourceLocation getBlockResource();

	@SideOnly(Side.CLIENT)
	public IIcon getIcon();

}
