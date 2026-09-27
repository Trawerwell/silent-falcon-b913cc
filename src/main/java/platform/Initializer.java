package platform;


import aethereal.core.Primordial;
import net.fabricmc.api.ClientModInitializer;

public class Initializer implements ClientModInitializer {


    public void onInitializeClient() {
        new Primordial();
    }
}
