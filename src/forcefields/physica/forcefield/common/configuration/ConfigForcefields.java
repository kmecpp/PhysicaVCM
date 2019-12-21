package physica.forcefield.common.configuration;

import net.minecraftforge.common.config.Configuration;
import physica.CoreReferences;
import physica.api.core.load.IContent;
import physica.api.core.load.LoadPhase;
import physica.forcefield.PhysicaForcefields;

import java.io.File;

public class ConfigForcefields implements IContent {

	public static String CATEGORY = "FORCEFIELDS_GENERAL";

	public static float FORCEFIELD_HEALTHLOSS_MODIFIER = 1f;
	public static float FORCEFIELD_INTERIOR_MODULE_DOWNSIZE = 4;

	@Override
	public void register(LoadPhase phase) {
		if (phase == LoadPhase.ConfigRegister) {
			Configuration configuration = new Configuration(new File(PhysicaForcefields.configFolder, "ForceFields.cfg"), CoreReferences.VERSION);
			configuration.load();
			FORCEFIELD_HEALTHLOSS_MODIFIER = configuration.getFloat("forcefieldHealthLossModifier", CATEGORY, FORCEFIELD_HEALTHLOSS_MODIFIER, 0.01f, 10000f, "Forcefield health loss Modifier");
			FORCEFIELD_INTERIOR_MODULE_DOWNSIZE = configuration.getFloat("forcefieldInteriorModuleDownsize", CATEGORY, FORCEFIELD_INTERIOR_MODULE_DOWNSIZE, 1f, 100f,
				"Interior module downsizer. The max radius for the interior module will be the radius of the forcefield divided by this value.");
			configuration.save();
		}
	}

}
