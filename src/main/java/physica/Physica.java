package physica;

import java.io.File;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.Mod.Metadata;
import cpw.mods.fml.common.ModMetadata;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLLoadCompleteEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.relauncher.Side;
import net.minecraftforge.common.MinecraftForge;
import physica.api.core.load.ContentLoader;
import physica.api.core.load.LoadPhase;
import physica.core.client.ClientRegister;
import physica.core.common.CoreBlockRegister;
import physica.core.common.CoreItemRegister;
import physica.core.common.CoreRecipeRegister;
import physica.core.common.CoreTabRegister;
import physica.core.common.CoreWorldGenRegister;
import physica.core.common.command.CommandPhysica;
import physica.core.common.configuration.ConfigCore;
import physica.core.common.event.FulminationEventHandler;
import physica.core.common.event.WrenchEventHandler;
import physica.library.net.EnergyNetworkRegistry;
import physica.library.net.energy.EnergyNetwork;
import physica.library.network.netty.PacketSystem;
import physica.library.recipe.IRecipeRegister;
import physica.library.recipe.RecipeSide;
import physica.proxy.sided.CommonProxy;

@Mod(modid = CoreReferences.DOMAIN, name = CoreReferences.NAME, version = CoreReferences.VERSION)
public class Physica {

	@SidedProxy(clientSide = "physica.proxy.sided.ClientProxy", serverSide = "physica.proxy.sided.ServerProxy")
	public static CommonProxy	sidedProxy;
	public static ContentLoader	proxyLoader	= new ContentLoader();

	@Instance(CoreReferences.NAME)
	public static Physica		INSTANCE;
	@Metadata(CoreReferences.DOMAIN)
	public static ModMetadata	metadata;

	public static File			configFolder;
	public static ConfigCore	config;

	@EventHandler
	public void preInit(FMLPreInitializationEvent event)
	{
		INSTANCE = this;
		configFolder = new File(event.getModConfigurationDirectory(), "/" + CoreReferences.DOMAIN);
		NetworkRegistry.INSTANCE.registerGuiHandler(this, sidedProxy);
		proxyLoader.addContent(sidedProxy);
		proxyLoader.addContent(PacketSystem.INSTANCE);
		proxyLoader.addContent(config = new ConfigCore());

		proxyLoader.addContent(new CoreTabRegister());
		proxyLoader.addContent(new CoreBlockRegister());
		proxyLoader.addContent(new CoreItemRegister());

		if (event.getSide() == Side.CLIENT)
		{
			proxyLoader.addContent(new ClientRegister());
		}

		proxyLoader.addContent(new CoreRecipeRegister());
		proxyLoader.addContent(new CoreWorldGenRegister());

		MinecraftForge.EVENT_BUS.register(FulminationEventHandler.INSTANCE);
		MinecraftForge.EVENT_BUS.register(EnergyNetworkRegistry.INSTANCE);
		FMLCommonHandler.instance().bus().register(EnergyNetworkRegistry.INSTANCE);
		MinecraftForge.EVENT_BUS.register(new EnergyNetwork.NetworkLoader());
		MinecraftForge.EVENT_BUS.register(WrenchEventHandler.INSTANCE);

		metadata.authorList = CoreReferences.Metadata.AUTHORS;
		metadata.autogenerated = false;
		metadata.credits = CoreReferences.Metadata.CREDITS;
		metadata.description = CoreReferences.Metadata.DESCRIPTION;
		metadata.modId = CoreReferences.DOMAIN;
		metadata.name = CoreReferences.NAME;
		metadata.updateUrl = CoreReferences.Metadata.UPDATE_URL;
		metadata.url = CoreReferences.Metadata.URL;
		metadata.version = CoreReferences.VERSION;
		proxyLoader.callRegister(LoadPhase.CreativeTabRegister);
		proxyLoader.callRegister(LoadPhase.ConfigRegister);
		proxyLoader.callRegister(LoadPhase.RegisterObjects);
		proxyLoader.callRegister(LoadPhase.PreInitialize);
		proxyLoader.callRegister(LoadPhase.ClientRegister);
	}

	@EventHandler
	public void init(FMLInitializationEvent event)
	{
		proxyLoader.callRegister(LoadPhase.Initialize);
		proxyLoader.callRegister(LoadPhase.EntityRegister);
		proxyLoader.callRegister(LoadPhase.FluidRegister);
		proxyLoader.callRegister(LoadPhase.WorldRegister);
	}

	@EventHandler
	public void postInit(FMLPostInitializationEvent event)
	{
		proxyLoader.callRegister(LoadPhase.PostInitialize);
	}

	@EventHandler
	public void loadComplete(FMLLoadCompleteEvent event)
	{
		proxyLoader.callRegister(LoadPhase.OnStartup);
		IRecipeRegister.InitializeSide(RecipeSide.Core);
	}

	@EventHandler
	public void serverStarting(FMLServerStartingEvent event)
	{
		event.registerServerCommand(new CommandPhysica());
	}
}
