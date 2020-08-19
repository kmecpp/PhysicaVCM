package physica.forcefield.common.tile;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import io.netty.buffer.ByteBuf;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.world.EnumSkyBlock;
import net.minecraftforge.fluids.FluidTank;
import physica.api.core.abstraction.Face;
import physica.api.core.inventory.IGuiInterface;
import physica.api.core.misc.IExplosionHandler;
import physica.api.core.tile.ITileBase;
import physica.api.forcefield.IFortronConstructor;
import physica.api.forcefield.IInvFortronTile;
import physica.forcefield.PhysicaForcefields;
import physica.forcefield.client.gui.GuiFortronFieldConstructor;
import physica.forcefield.common.ConstructorWorldData;
import physica.forcefield.common.ForcefieldBlockRegister;
import physica.forcefield.common.ForcefieldEventHandler;
import physica.forcefield.common.ForcefieldFluidRegister;
import physica.forcefield.common.ForcefieldItemRegister;
import physica.forcefield.common.calculations.ConstructorCalculationThread;
import physica.forcefield.common.configuration.ConfigForcefields;
import physica.forcefield.common.inventory.ContainerFortronFieldConstructor;
import physica.library.location.GridLocation;
import physica.library.network.IPacket;
import physica.library.network.netty.PacketSystem;
import physica.library.network.packet.PacketTile;
import physica.library.tile.TileBaseContainer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TileFortronFieldConstructor extends TileBaseContainer implements IInvFortronTile, IGuiInterface, IFortronConstructor, IExplosionHandler {

	public static final int[] SLOT_UPGRADES = new int[] {12, 13, 14, 15, 16, 17};
	public static final int[] SLOT_MODULES = new int[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};

	public static final Integer[] SLOT_NORTH = new Integer[] {4, 6};
	public static final Integer[] SLOT_SOUTH = new Integer[] {5, 7};
	public static final Integer[] SLOT_EAST = new Integer[] {9, 10};
	public static final Integer[] SLOT_WEST = new Integer[] {1, 2};
	public static final Integer[] SLOT_UP = new Integer[] {0, 8};
	public static final Integer[] SLOT_DOWN = new Integer[] {3, 11};
	public static final HashMap<List<Integer>, String> SLOT_MAP = new HashMap<>();

	static {
		SLOT_MAP.put(Arrays.asList(SLOT_NORTH), "North");
		SLOT_MAP.put(Arrays.asList(SLOT_SOUTH), "South");
		SLOT_MAP.put(Arrays.asList(SLOT_EAST), "East");
		SLOT_MAP.put(Arrays.asList(SLOT_WEST), "West");
		SLOT_MAP.put(Arrays.asList(SLOT_UP), "Up");
		SLOT_MAP.put(Arrays.asList(SLOT_DOWN), "Down");
	}

	public static final int SLOT_FREQUENCY = 18;
	public static final int SLOT_TYPE = 19;

	public static final int BASE_FORTRON = 1300;
	public static final int MAX_HEALTH_LOSS = 10000;
	protected boolean isActivated = false;
	protected Set<ITileBase> fortronConnections = new HashSet<>();

	public Set<GridLocation> calculatedFieldPoints = Collections.synchronizedSet(new HashSet<>());
	public Set<TileFortronField> activeFields = new HashSet<>();

	private GridLocation location;
	private ConstructorCalculationThread calculationThread;
	private int[] cachedCoordinates = new int[3];
	private int[] cachedInformation = new int[9];
	private boolean hasInterior = false;
	protected FluidTank fortronTank = new FluidTank(ForcefieldFluidRegister.LIQUID_FORTRON, 0, getMaxFortron());

	public boolean hasUpgrade(String name) {
		return findModule(ForcefieldItemRegister.moduleMap.get(name), SLOT_UPGRADES[0], SLOT_UPGRADES[SLOT_UPGRADES.length - 1]);
	}

	public int xCoordShifted() {
		return cachedCoordinates[0];
	}

	public int yCoordShifted() {
		return cachedCoordinates[1];
	}

	public int zCoordShifted() {
		return cachedCoordinates[2];
	}

	public int getMaxFortron() {
		return getFortronUse() * 40 + BASE_FORTRON;
	}

	public int getFortronUse() {
		return cachedInformation[7] + cachedInformation[8] + (shouldDisintegrate ? 25000 : 0);
	}

	private int healthLost = 0;
	private boolean isOverriden;

	public int getHealthLost() {
		return healthLost;
	}

	@Override
	public void receiveExplosionEnergy(double energy) {
		damageForcefield((int) energy);
	}

	public void damageForcefield(int amount) {
		int fortronUse = getFortronUse();
		if (fortronUse < 5 || activeFields.size() < maximumForceFieldCount / 1.5) {
			return; //Blast goes right through the field
		}
		double increase = (amount / (double) fortronUse) * ConfigForcefields.FORCEFIELD_HEALTHLOSS_MODIFIER * 10000;
		healthLost += increase;
		if (healthLost >= MAX_HEALTH_LOSS || increase > MAX_HEALTH_LOSS) {
			destroyField(true);
			GridLocation loc = getLocation();
			loc.setBlockAir(World());
//			if (Loader.isModLoaded(CoreReferences.DOMAIN + "nuclearphysics")) {
//				EntityItem antimatter = new EntityItem(World(), loc.xCoord, loc.yCoord, loc.zCoord, new ItemStack(physica.nuclear.common.NuclearItemRegister.itemAntimatterCell1Gram));
//				World().spawnEntityInWorld(antimatter);
//			}
			World().createExplosion(null, loc.xCoord, loc.yCoord, loc.zCoord, 10F, true);
		}
	}

	@Override
	public void onChunkUnload() {
		ForcefieldEventHandler.INSTANCE.unregisterConstructor(this);
	}

	@Override
	public boolean canRecieveFortron(IInvFortronTile from) {
		return from instanceof TileFortronCapacitor;
	}

	@Override
	public FluidTank getFortronTank() {
		return fortronTank;
	}

	@Override
	public boolean canSendBeam() {
		return false;
	}

	@Override
	public AxisAlignedBB getRenderBoundingBox() {
		AxisAlignedBB bb = super.getRenderBoundingBox();
		bb.maxY += 1;
		return bb;
	}

	@Override
	public void onInventoryChanged() {
		super.onInventoryChanged();
		updateFieldTerms();
		int ret = 0;
		for (int i = 0; i < getSizeInventory(); i++) {
			if (i != SLOT_FREQUENCY) {
				ItemStack stack = getStackInSlot(i);
				if (stack != null) {
					if (stack.getItem() == ForcefieldItemRegister.itemMetaManipulationModule || stack.getItem() == ForcefieldItemRegister.itemMetaShapeModule
						    || stack.getItem() == ForcefieldItemRegister.itemMetaUpgradeModule && stack.getItemDamage() >= 3 && stack.getItemDamage() <= 6) {
						ret += stack.stackSize;
					}
				}
			}
		}
		if (ret != moduleCount) {
			moduleCount = ret;
			destroyField(false);
		}
		GridLocation loc = getLocation();
		ItemStack translate = ForcefieldItemRegister.moduleMap.get("moduleManipulationTranslate");
		cachedCoordinates[0] = loc.xCoord + getModuleCountIn(translate, SLOT_EAST[0], +SLOT_EAST[1]) - getModuleCountIn(translate, SLOT_WEST[0], SLOT_WEST[1]);
		cachedCoordinates[1] = loc.yCoord + getModuleCountIn(translate, SLOT_UP[0], SLOT_UP[1]) - getModuleCountIn(translate, SLOT_DOWN[0], SLOT_DOWN[1]);
		cachedCoordinates[2] = loc.zCoord + getModuleCountIn(translate, SLOT_SOUTH[0], SLOT_SOUTH[1]) - getModuleCountIn(translate, SLOT_NORTH[0], SLOT_NORTH[1]);
		ItemStack scaleModule = ForcefieldItemRegister.moduleMap.get("moduleManipulationScale");
		hasInterior = hasUpgrade("moduleUpgradeInterior");
		cachedInformation[0] = (int) (cachedCoordinates[0]
			                              + Math.min(64, getModuleCountIn(scaleModule, TileFortronFieldConstructor.SLOT_EAST[0], TileFortronFieldConstructor.SLOT_EAST[1])) / (hasInterior ? ConfigForcefields.FORCEFIELD_INTERIOR_MODULE_DOWNSIZE : 1));
		cachedInformation[1] = (int) Math.min(255,
			Math.max(0, cachedCoordinates[1] + getModuleCountIn(scaleModule, TileFortronFieldConstructor.SLOT_UP[0], TileFortronFieldConstructor.SLOT_UP[1]) / (hasInterior ? ConfigForcefields.FORCEFIELD_INTERIOR_MODULE_DOWNSIZE : 1)));
		cachedInformation[2] = (int) (cachedCoordinates[2]
			                              + Math.min(64, getModuleCountIn(scaleModule, TileFortronFieldConstructor.SLOT_SOUTH[0], TileFortronFieldConstructor.SLOT_SOUTH[1])) / (hasInterior ? ConfigForcefields.FORCEFIELD_INTERIOR_MODULE_DOWNSIZE : 1));
		cachedInformation[3] = (int) (cachedCoordinates[0]
			                              - Math.min(64, getModuleCountIn(scaleModule, TileFortronFieldConstructor.SLOT_WEST[0], TileFortronFieldConstructor.SLOT_WEST[1])) / (hasInterior ? ConfigForcefields.FORCEFIELD_INTERIOR_MODULE_DOWNSIZE : 1));
		cachedInformation[4] = (int) Math.max(0,
			cachedCoordinates[1] - getModuleCountIn(scaleModule, TileFortronFieldConstructor.SLOT_DOWN[0], TileFortronFieldConstructor.SLOT_DOWN[1]) / (hasInterior ? ConfigForcefields.FORCEFIELD_INTERIOR_MODULE_DOWNSIZE : 1));
		cachedInformation[5] = (int) (cachedCoordinates[2]
			                              - Math.min(64, getModuleCountIn(scaleModule, TileFortronFieldConstructor.SLOT_NORTH[0], TileFortronFieldConstructor.SLOT_NORTH[1])) / (hasInterior ? ConfigForcefields.FORCEFIELD_INTERIOR_MODULE_DOWNSIZE : 1));
		cachedInformation[6] = Math.min(64, getModuleCount(scaleModule, TileFortronFieldConstructor.SLOT_MODULES[0], TileFortronFieldConstructor.SLOT_MODULES[TileFortronFieldConstructor.SLOT_MODULES.length - 1]) / 6);
		cachedInformation[7] = BASE_FORTRON * getModuleCount(ForcefieldItemRegister.moduleMap.get("moduleManipulationScale"), 0, 11);
		cachedInformation[8] = 1 + BASE_FORTRON * getModuleCount(ForcefieldItemRegister.moduleMap.get("moduleUpgradeSpeed"), SLOT_UPGRADES[0], SLOT_UPGRADES[SLOT_UPGRADES.length - 1]);
		World().updateLightByType(EnumSkyBlock.Block, loc.xCoord, loc.yCoord, loc.zCoord);
	}

	@Override
	public void onFirstUpdate() {
		invalidateConnections();
		fortronConnections.clear();
		findNearbyConnections(TileFortronCapacitor.class);
		if (!World().isRemote) {
			ConstructorWorldData.register(this);
		}
		onInventoryChanged();
	}

	@Override
	public void updateCommon(int ticks) {
		super.updateCommon(ticks);
		if (getStackInSlot(SLOT_FREQUENCY) != null) {
			if (getStackInSlot(SLOT_FREQUENCY).stackTagCompound != null) {
				setFrequency(getStackInSlot(SLOT_FREQUENCY).stackTagCompound.getInteger("frequency"));
			}
		}
		fortronTank.setCapacity(getMaxFortron());
		if (fortronTank.getCapacity() < fortronTank.getFluidAmount()) {
			fortronTank.getFluid().amount = fortronTank.getCapacity();
		}
	}

	@Override
	public boolean isProtecting(double x, double y, double z) {
		if (!isFullySealed) {
			return false;
		}
		int index = getProjectorMode();
		if (index == ForcefieldItemRegister.moduleMap.get("moduleShapeCube").getItemDamage()) {
			int xRadiusPos = cachedInformation[0];
			int yRadiusPos = cachedInformation[1];
			int zRadiusPos = cachedInformation[2];
			int xRadiusNeg = cachedInformation[3];
			int yRadiusNeg = cachedInformation[4];
			int zRadiusNeg = cachedInformation[5];
			return x >= xRadiusNeg && x <= xRadiusPos && y >= yRadiusNeg && y <= yRadiusPos && z >= zRadiusNeg && z <= zRadiusPos;
		} else if (index == ForcefieldItemRegister.moduleMap.get("moduleShapeSphere").getItemDamage() || index == ForcefieldItemRegister.moduleMap.get("moduleShapeHemisphere").getItemDamage()) {
			float radius = cachedInformation[6] + 0.4F;
			if (hasInterior) {
				radius /= ConfigForcefields.FORCEFIELD_INTERIOR_MODULE_DOWNSIZE;
			}
			if (y < 0 || y > 255) {
				return false;
			} else if (index == ForcefieldItemRegister.moduleMap.get("moduleShapeHemisphere").getItemDamage()) {
				return getLocation().getDistance(x, y, z) <= radius && y >= yCoordShifted();
			} else {
				return getLocation().getDistance(x, y, z) <= radius;
			}
		} else if (index == ForcefieldItemRegister.moduleMap.get("moduleShapePyramid").getItemDamage()) {
			int xPos = cachedInformation[0];
			int yPos = cachedInformation[1];
			int zPos = cachedInformation[2];
			int xNeg = cachedInformation[3];
			int yNeg = cachedInformation[4];
			int zNeg = cachedInformation[5];
			yPos += yCoordShifted() - yNeg;
			if (y >= yNeg && y <= yPos) {
				double diff = y - yNeg + 1;
				xNeg += diff;
				xPos -= diff;
				zNeg += diff;
				zPos -= diff;
				return x >= xNeg && x <= xPos && z >= zNeg && z <= zPos;
			}
		}
		return false;
	}

	@Override
	public boolean isFinishedConstructing() {
		return !isDestroying && calculatedFieldPoints.isEmpty() && isActivated() && getStackInSlot(SLOT_TYPE) != null && fortronTank.getFluidAmount() > getFortronUse() && getFortronUse() > 0 && activeFields.size() > 0;
	}

	public boolean isCalculating;
	public boolean isCurrentlyConstructing;
	public boolean isDestroying;
	public int moduleCount;
	public int forcefieldType = -1;

	public void setCalculating(boolean b) {
		isCalculating = b;
	}

	public int delay = 0;
	public boolean isFullySealed = false;

	@Override
	public void updateServer(int ticks) {
		super.updateServer(ticks);
		isActivated = isOverriden ? true : isPoweredByRedstone();
		if (!ForcefieldEventHandler.INSTANCE.isConstructorRegistered(this)) {
			ForcefieldEventHandler.INSTANCE.registerConstructor(this);
		}
		if (!isCalculating) {
			isFullySealed = isFinishedConstructing() && activeFields.size() >= maximumForceFieldCount;
		}
		ItemStack type = getStackInSlot(SLOT_TYPE);
		if (type != null) {
			if (forcefieldType != type.getItemDamage()) {
				destroyField(false);
				forcefieldType = type.getItemDamage();
			}
		}
		if (ticks % 20 == 0) {
			validateConnections();
		}
		if (isDestroying) {
			if (activeFields.isEmpty()) {
				isDestroying = false;
				delay = 40;
			} else {
				int size = 0;
				Iterator<TileFortronField> iterator = activeFields.iterator();
				while (iterator.hasNext()) {
					if (size++ > 250) {
						break;
					}
					TileFortronField field = iterator.next();
					iterator.remove();
					field.invalidate();
					field.getLocation().setBlockAirNonUpdate(World());
				}
			}
		}
		if (ticks > 5) {
			int fortronUse = getFortronUse();
			if (isActivated() && type != null && fortronTank.getFluidAmount() > fortronUse && fortronUse > 0) {
				fortronTank.drain(fortronUse, true);
				if (!isDestroying) {
					healthLost = Math.max(0, healthLost - 1);
					if (!isCalculating && calculatedFieldPoints.isEmpty() && activeFields.isEmpty() && !isCurrentlyConstructing) {
						if (delay > 0) {
							if (getFortronTank().getFluidAmount() >= fortronUse) {
								delay--;
							}
						} else {
							delay = 40;
							setCalculating(true);
							calculationThread = new ConstructorCalculationThread(this);
							calculationThread.start();
							Logger.getGlobal().log(Level.INFO, "Started forcefield generation at: " + getLocation());
						}

					} else if (!isCalculating && !calculatedFieldPoints.isEmpty()) {
						projectField();
						isCurrentlyConstructing = true;
					}
				}
			} else {
				if (fortronTank.getFluidAmount() < fortronUse) {
					delay = 100;
				}
				destroyField(false);
			}
		}
	}

	private boolean shouldSponge = false;
	public boolean shouldDisintegrate = false;
	private boolean shouldStabilize = false;
	private boolean hasCollectionModule = false;
	private int totalGeneratedPerTick = 0;
	public int maximumForceFieldCount = 0;

	public void destroyField(boolean instant) {
		healthLost = 0;
		isDestroying = true;
		isFullySealed = false;
		isCurrentlyConstructing = false;
		maximumForceFieldCount = 0;

		if (calculationThread != null) {
			calculationThread.interrupt();
			calculationThread = null;
		}
		calculatedFieldPoints.clear();

		if (instant) {
			for (TileFortronField field : activeFields) {
				field.getLocation().setBlockAirNonUpdate(World());
			}
			activeFields.clear();
		}
	}

	public void updateFieldTerms() {
		shouldSponge = hasUpgrade("moduleUpgradeSponge");
		shouldDisintegrate = hasUpgrade("moduleUpgradeDisintegration");
		shouldStabilize = hasUpgrade("moduleUpgradeStabilize");
		hasCollectionModule = hasUpgrade("moduleUpgradeCollection");
		totalGeneratedPerTick = 1 + 2 * getModuleCount(ForcefieldItemRegister.moduleMap.get("moduleUpgradeSpeed"), SLOT_UPGRADES[0], SLOT_UPGRADES[SLOT_UPGRADES.length - 1]) / ((shouldSponge ? 2 : 5) / (shouldStabilize ? 2 : 1));
		if (shouldSponge) {
			totalGeneratedPerTick /= 2;
		} else if (shouldDisintegrate) {
			totalGeneratedPerTick /= hasCollectionModule ? 5 : 4;
		} else if (shouldStabilize) {
			totalGeneratedPerTick /= 3;
		}
	}

	protected void projectField() {
		Set<GridLocation> finishedQueueItems = new HashSet<>();
		int currentlyGenerated = 0, currentlyMissed = 0;

		GridLocation loc = getLocation();
		ArrayList<TileFortronFieldConstructor> relevantConstructors = shouldDisintegrate ? ForcefieldEventHandler.INSTANCE.getRelevantConstructors(World(), loc.xCoord, loc.yCoord, loc.zCoord) : null;
		for (GridLocation fieldPoint : calculatedFieldPoints) {
			if (currentlyGenerated >= totalGeneratedPerTick) {
				break;
			}
			if (currentlyMissed >= 500) {
				break;
			}
			finishedQueueItems.add(fieldPoint);
			Block block = fieldPoint.getBlock(World());
			if (block == ForcefieldBlockRegister.blockFortronField) {
				TileFortronField field = (TileFortronField) fieldPoint.getTile(World());
				if (field != null && field.getConstructorCoord().equals(loc)) {
					activeFields.add(field);
					currentlyGenerated++;
					continue;
				}
			}
			if (shouldSponge) {
				if (block.getMaterial().isLiquid()) {
					fieldPoint.setBlockAir(World());
					for (Face direction : Face.VALID) {
						Block adjacentBlock = World().getBlock(fieldPoint.xCoord + direction.offsetX, fieldPoint.yCoord + direction.offsetY, fieldPoint.zCoord + direction.offsetZ);
						if (adjacentBlock.getMaterial().isLiquid()) {
							fieldPoint.setBlockAir(World());
						}
					}
					currentlyGenerated++;
				} else if (block.getMaterial() == Material.air) {
					currentlyMissed++;
				}
			} else {
				if (shouldDisintegrate) {
					boolean skip = false;
					if (relevantConstructors != null) {
						for (TileFortronFieldConstructor constructor : relevantConstructors) {
							if (constructor != this && constructor.isProtecting(fieldPoint.xCoord, fieldPoint.yCoord, fieldPoint.zCoord)) {
								currentlyMissed++;
								skip = true;
								break;
							}
						}
					}
					if (skip) {
						continue;
					}
					if (block.getBlockHardness(World(), fieldPoint.xCoord, fieldPoint.yCoord, fieldPoint.zCoord) != -1) { // Location usually has no effect
						if (hasCollectionModule) {
							for (ItemStack stack : block.getDrops(World(), fieldPoint.xCoord, fieldPoint.yCoord, fieldPoint.zCoord, fieldPoint.getMetadata(World()), 0)) {
								TileInterdictionMatrix.mergeIntoInventory(this, stack);
							}
						}
						if (block.getMaterial() == Material.air) {
							currentlyMissed++;
							continue;
						}
						fieldPoint.setBlockAirNonUpdate(World());
						currentlyGenerated++;
					}
				} else if (block.getMaterial().isReplaceable()) {
					if (shouldStabilize) {
						for (Face dir : Face.VALID) {
							TileEntity entity = World().getTileEntity(loc.xCoord + dir.offsetX, loc.yCoord + dir.offsetY, loc.zCoord + dir.offsetZ);
							if (entity != null && entity instanceof IInventory) {
								IInventory inv = (IInventory) entity;
								boolean didPlace = false;
								for (int slot = 0; slot < inv.getSizeInventory(); slot++) {
									ItemStack stack = inv.getStackInSlot(slot);
									if (stack != null) {
										if (stack.getItem() instanceof ItemBlock
											    && entity.getWorldObj().canPlaceEntityOnSide(((ItemBlock) stack.getItem()).field_150939_a, fieldPoint.xCoord, fieldPoint.yCoord, fieldPoint.zCoord, false, 0, null, stack)) {
											int meta = stack.getHasSubtypes() ? stack.getItemDamage() : 0;
											((ItemBlock) stack.getItem()).placeBlockAt(stack, null, entity.getWorldObj(), fieldPoint.xCoord, fieldPoint.yCoord, fieldPoint.zCoord, 0, 0, 0, 0, meta);
											inv.decrStackSize(slot, 1);
											didPlace = true;
											break;
										}
									}
								}
								if (didPlace) {
									currentlyGenerated++;
									break;
								}
							}
						}
					} else {
						fieldPoint.setBlockNonUpdate(World(), ForcefieldBlockRegister.blockFortronField);
						TileEntity tile = fieldPoint.getTile(World());
						if (tile instanceof TileFortronField) {
							TileFortronField field = (TileFortronField) tile;
							field.setConstructor(this);
							activeFields.add(field);
						}
						currentlyGenerated++;
					}
				} else if (block.getBlockHardness(World(), fieldPoint.xCoord, fieldPoint.yCoord, fieldPoint.zCoord) == -1) {
					maximumForceFieldCount--;
				}
			}
		}
		calculatedFieldPoints.removeAll(finishedQueueItems);
	}

	@Override
	public void invalidate() {
		super.invalidate();
		invalidateConnections();
		if (!World().isRemote) {
			ConstructorWorldData.remove(this);
		}
	}

	@Override
	public Set<ITileBase> getFortronConnections() {
		return fortronConnections;
	}

	private int frequency;
	private int fieldColorMultiplier = PhysicaForcefields.DEFAULT_COLOR;

	@Override
	public int getFrequency() {
		return frequency;
	}

	@Override
	public void setFrequency(int freq) {
		int oldFrequency = frequency;
		frequency = freq;
		if (oldFrequency != freq) {
			onFirstUpdate();
		}
	}

	@Override
	public void writeClientGuiPacket(List<Object> dataList, EntityPlayer player) {
		super.writeClientGuiPacket(dataList, player);
		dataList.add(isActivated);
		dataList.add(isCurrentlyConstructing && !calculatedFieldPoints.isEmpty());
		dataList.add(frequency);
		dataList.add(healthLost);
		dataList.add(fieldColorMultiplier);
		dataList.add(fortronTank.writeToNBT(new NBTTagCompound()));
		dataList.add(isFullySealed);
	}

	@Override
	public void readClientGuiPacket(ByteBuf buf, EntityPlayer player) {
		super.readClientGuiPacket(buf, player);

		isActivated = buf.readBoolean();
		isCurrentlyConstructing = buf.readBoolean();
		frequency = buf.readInt();
		healthLost = buf.readInt();
		fieldColorMultiplier = buf.readInt();
		fortronTank.readFromNBT(ByteBufUtils.readTag(buf));
		isFullySealed = buf.readBoolean();
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setBoolean("override", isOverriden);
		tag.setInteger("frequency", frequency);
		tag.setBoolean("isCalculating", isCalculating);
		tag.setBoolean("isCurrentlyConstructing", isCurrentlyConstructing);
		tag.setBoolean("isDestroying", isDestroying);
		tag.setBoolean("isActivated", isActivated);
		tag.setInteger("fieldColorMultiplier", fieldColorMultiplier);
		tag.setInteger("moduleCount", moduleCount);
		tag.setInteger("forcefieldType", forcefieldType);
		tag.setBoolean("isFullySealed", isFullySealed);
		fortronTank.writeToNBT(tag);
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		isOverriden = tag.getBoolean("override");
		frequency = tag.getInteger("frequency");
		isActivated = tag.getBoolean("isActivated");
		isCurrentlyConstructing = false;
		isDestroying = tag.getBoolean("isDestroying");
		fieldColorMultiplier = tag.getInteger("fieldColorMultiplier");
		moduleCount = tag.getInteger("moduleCount");
		forcefieldType = tag.getInteger("forcefieldType");
		isFullySealed = tag.getBoolean("isFullySealed");
		fortronTank.readFromNBT(tag);
	}

	@Override
	public boolean isItemValidForSlot(int slot, ItemStack stack) {
		if (stack == null || stack.getItem() == null) {
			return false;
		}
		if (slot >= 0 && slot <= 11) {
			return stack.getItem() == ForcefieldItemRegister.itemMetaManipulationModule;
		}
		if (slot >= 12 && slot <= 17) {
			return stack.getItem() == ForcefieldItemRegister.itemMetaUpgradeModule;
		}
		if (slot == SLOT_FREQUENCY) {
			return stack.getItem() == ForcefieldItemRegister.itemFrequency;
		}
		if (slot == SLOT_TYPE) {
			return stack.getItem() == ForcefieldItemRegister.itemMetaShapeModule;
		}
		return true;
	}

	@Override
	public boolean canInsertItem(int slot, ItemStack stack, Face face) {
		return isItemValidForSlot(slot, stack);
	}

	@Override
	public boolean canExtractItem(int slot, ItemStack stack, Face face) {
		return isItemValidForSlot(slot, stack);
	}

	@Override
	public int getSizeInventory() {
		return 21;
	}

	@Override
	public boolean isActivated() {
		return isActivated;
	}

	@Override
	@SideOnly(Side.CLIENT)
	public GuiScreen getClientGuiElement(int id, EntityPlayer player) {
		return new GuiFortronFieldConstructor(player, this);
	}

	@Override
	public Container getServerGuiElement(int id, EntityPlayer player) {
		return new ContainerFortronFieldConstructor(player, this);
	}

	public int getProjectorMode() {
		return getStackInSlot(TileFortronFieldConstructor.SLOT_TYPE) != null ? getStackInSlot(TileFortronFieldConstructor.SLOT_TYPE).getItemDamage() : -1;
	}

	public int fieldColorMultiplier() {
		return fieldColorMultiplier;
	}

	public void setFieldColorMultiplier(int fieldColorMultiplier) {
		this.fieldColorMultiplier = fieldColorMultiplier;
	}

	@Override
	public GridLocation getLocation() {
		if (location == null) {
			location = super.getLocation();
		}
		return location;
	}

	public static final int GUI_BUTTON_PACKET_ID = 22;

	public void actionPerformed(int buttonId, Side side) {
		if (side == Side.CLIENT) {
			GridLocation loc = getLocation();
			PacketSystem.INSTANCE.sendToServer(new PacketTile("", GUI_BUTTON_PACKET_ID, loc.xCoord, loc.yCoord, loc.zCoord, buttonId));
		} else if (side == Side.SERVER) {
			isOverriden = !isOverriden;
		}
	}

	@Override
	public void readCustomPacket(int id, EntityPlayer player, Side side, IPacket type) {
		if (id == GUI_BUTTON_PACKET_ID && side.isServer() && type instanceof PacketTile) {
			actionPerformed(((PacketTile) type).customInteger, side);
		}
	}

}
