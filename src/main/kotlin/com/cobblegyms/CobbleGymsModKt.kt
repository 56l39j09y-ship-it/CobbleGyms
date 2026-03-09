import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleGymsModKt implements ModInitializer {
    public static final String MOD_ID = "cobblegyms";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // Initialize the CobbleGyms system
        LOGGER.info("Initializing CobbleGyms Mod...");
        // Add any other initialization code here
    }
}