package org.vmstudio.watersplash.core.config;

import com.google.common.collect.Lists;
import org.vmstudio.watersplash.core.config.enums.EffectSpawningRule;
import org.vmstudio.watersplash.core.config.enums.Resolution;
import org.vmstudio.watersplash.core.render.WakeColor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;

public class WakesConfig {
    public static final String GENERAL = "general";
    public static final String APPEARANCE = "appearance";
    public static final String DEBUG = "debug";

    public static boolean disableMod = false;

    public static EffectSpawningRule boatSpawning = EffectSpawningRule.SIMULATION_AND_PLANES;
    public static EffectSpawningRule playerSpawning = EffectSpawningRule.ONLY_SIMULATION;
    public static EffectSpawningRule otherPlayersSpawning = EffectSpawningRule.ONLY_SIMULATION;
    public static EffectSpawningRule mobSpawning = EffectSpawningRule.ONLY_SIMULATION;
    public static EffectSpawningRule itemSpawning = EffectSpawningRule.ONLY_SIMULATION;

    public static float wavePropagationFactor = 0.95f;
    public static float waveDecayFactor = 0.5f;
    public static int initialStrength = 20;
    public static int paddleStrength = 100;
    public static int splashStrength = 100;

    public static Resolution wakeResolution = Resolution.SIXTEEN;
    public static float wakeOpacity = 1.0f;
    public static float blendStrength = 0.5f;
    public static boolean firstPersonEffects = false;
    public static boolean spawnParticles = true;
    public static float shaderLightPassthrough = 0.5f;

    public static float splashPlaneWidth = 2.0f;
    public static float splashPlaneHeight = 1.5f;
    public static float splashPlaneDepth = 3.0f;
    public static float splashPlaneOffset = 0.0f;
    public static float splashPlaneGap = 1.0f;
    public static int splashPlaneResolution = 5;
    public static float maxSplashPlaneVelocity = 0.5f;
    public static float splashPlaneScale = 0.8f;

    public static List<Float> wakeColorIntervals = Lists.newArrayList(0.05f, 0.15f, 0.2f, 0.35f, 0.52f, 0.6f, 0.7f, 0.9f);
    public static List<String> wakeColors = Lists.newArrayList(
            "#00000000",
            "#289399a6",
            "#649ea5b0",
            "#b4c4cad1",
            "#00000000",
            "#b4c4cad1",
            "#ffffffff",
            "#b4c4cad1",
            "#649ea5b0"
    );

    public static boolean debugColors = false;
    public static boolean drawDebugBoxes = false;
    public static boolean showDebugInfo = false;
    public static int floodFillDistance = 2;
    public static int floodFillTickDelay = 2;

    public static float vrSplashStrength = 1.0f;
    public static float mouseSplashStrength = 1.0f;
    public static double minSpeedForWaves = 0.2;
    public static double minSpeedForSplash = 0.08;

    private static File configFile;

    public static void init(File configDir) {
        configFile = new File(configDir, "watersplash.properties");
        load();
        save();
    }

    public static void load() {
        if (!configFile.exists()) {
            return;
        }

        Properties props = new Properties();
        try (FileInputStream fis = new FileInputStream(configFile)) {
            props.load(fis);

            disableMod = Boolean.parseBoolean(props.getProperty("disableMod", String.valueOf(disableMod)));
            wavePropagationFactor = Float.parseFloat(props.getProperty("wavePropagationFactor", String.valueOf(wavePropagationFactor)));
            waveDecayFactor = Float.parseFloat(props.getProperty("waveDecayFactor", String.valueOf(waveDecayFactor)));
            initialStrength = Integer.parseInt(props.getProperty("initialStrength", String.valueOf(initialStrength)));
            splashStrength = Integer.parseInt(props.getProperty("splashStrength", String.valueOf(splashStrength)));
            wakeOpacity = Float.parseFloat(props.getProperty("wakeOpacity", String.valueOf(wakeOpacity)));
            firstPersonEffects = Boolean.parseBoolean(props.getProperty("firstPersonEffects", String.valueOf(firstPersonEffects)));
            spawnParticles = Boolean.parseBoolean(props.getProperty("spawnParticles", String.valueOf(spawnParticles)));
            vrSplashStrength = Float.parseFloat(props.getProperty("vrSplashStrength", String.valueOf(vrSplashStrength)));
            mouseSplashStrength = Float.parseFloat(props.getProperty("mouseSplashStrength", String.valueOf(mouseSplashStrength)));
            minSpeedForWaves = Double.parseDouble(props.getProperty("minSpeedForWaves", String.valueOf(minSpeedForWaves)));
            minSpeedForSplash = Double.parseDouble(props.getProperty("minSpeedForSplash", String.valueOf(minSpeedForSplash)));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        Properties props = new Properties();

        props.setProperty("disableMod", String.valueOf(disableMod));
        props.setProperty("wavePropagationFactor", String.valueOf(wavePropagationFactor));
        props.setProperty("waveDecayFactor", String.valueOf(waveDecayFactor));
        props.setProperty("initialStrength", String.valueOf(initialStrength));
        props.setProperty("splashStrength", String.valueOf(splashStrength));
        props.setProperty("wakeOpacity", String.valueOf(wakeOpacity));
        props.setProperty("firstPersonEffects", String.valueOf(firstPersonEffects));
        props.setProperty("spawnParticles", String.valueOf(spawnParticles));
        props.setProperty("vrSplashStrength", String.valueOf(vrSplashStrength));
        props.setProperty("mouseSplashStrength", String.valueOf(mouseSplashStrength));
        props.setProperty("minSpeedForWaves", String.valueOf(minSpeedForWaves));
        props.setProperty("minSpeedForSplash", String.valueOf(minSpeedForSplash));

        try (FileOutputStream fos = new FileOutputStream(configFile)) {
            props.store(fos, "Water Splash Configuration");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static WakeColor getWakeColor(int i) {
        if (i >= wakeColors.size()) {
            return new WakeColor(0xFFFFFFFF);
        }
        return new WakeColor(wakeColors.get(i));
    }
}
