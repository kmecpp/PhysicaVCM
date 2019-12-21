package physica.api.core;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Random;

public class PhysicaAPI {

	public static final Logger logger = LogManager.getLogger("PhysicaAPI");
	public static final Random random = new Random();

	public static boolean isDebugMode = false;

}
