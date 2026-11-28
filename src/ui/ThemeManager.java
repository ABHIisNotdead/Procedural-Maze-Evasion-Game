package ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

/**
 * Manages loading, caching, and rendering of biome textures, item sprites, and ambient color palettes.
 */
public class ThemeManager {

    public static class BiomeAssets {
        public final Image wall;
        public final Image path;
        public final Image crackedWall;
        public final Color wallBorderColor;
        public final Color ambientTint;

        public BiomeAssets(Image wall, Image path, Image crackedWall, Color wallBorderColor, Color ambientTint) {
            this.wall = wall;
            this.path = path;
            this.crackedWall = crackedWall;
            this.wallBorderColor = wallBorderColor;
            this.ambientTint = ambientTint;
        }
    }

    private static final Map<String, BiomeAssets> biomes = new HashMap<>();
    private static Image fishTreatIcon;
    private static Image catnipIcon;
    private static Image portalRuneIcon;
    private static Image kittenIcon;
    private static Image[] kittenVariants;
    private static Image bushIcon;
    private static Image crackedWallIcon;

    static {
        loadAssets();
    }

    public static void loadAssets() {
        try {
            // Load Item Sprites
            fishTreatIcon = loadImageSafe("assets/themes/fish_treat.png");
            catnipIcon = loadImageSafe("assets/themes/catnip.png");
            portalRuneIcon = loadImageSafe("assets/themes/portal_rune.png");
            bushIcon = loadImageSafe("assets/themes/bush.png");
            crackedWallIcon = loadImageSafe("assets/themes/cracked_wall.png");
            kittenIcon = loadImageSafe("assets/kitten.png");
            kittenVariants = new Image[]{
                    kittenIcon,
                    loadImageSafe("assets/kitten_blue.png"),
                    loadImageSafe("assets/kitten_lilac.png")
            };

            // Biomes
            biomes.put("GARDEN", new BiomeAssets(
                    loadImageSafe("assets/themes/garden_wall.png"),
                    loadImageSafe("assets/themes/garden_path.png"),
                    loadImageSafe("assets/themes/garden_cracked_wall.png"),
                    new Color(25, 45, 20),
                    new Color(40, 75, 35, 35)
            ));

            biomes.put("DUNGEON", new BiomeAssets(
                    loadImageSafe("assets/themes/dungeon_wall.png"),
                    loadImageSafe("assets/themes/dungeon_path.png"),
                    loadImageSafe("assets/themes/dungeon_cracked_wall.png"),
                    new Color(28, 32, 42),
                    new Color(30, 35, 48, 45)
            ));

            biomes.put("TEMPLE", new BiomeAssets(
                    loadImageSafe("assets/themes/temple_wall.png"),
                    loadImageSafe("assets/themes/temple_path.png"),
                    loadImageSafe("assets/themes/temple_cracked_wall.png"),
                    new Color(95, 72, 42),
                    new Color(85, 65, 30, 35)
            ));

            biomes.put("CYBER", new BiomeAssets(
                    loadImageSafe("assets/themes/cyber_wall.png"),
                    loadImageSafe("assets/themes/cyber_path.png"),
                    loadImageSafe("assets/themes/cyber_cracked_wall.png"),
                    new Color(0, 140, 200),
                    new Color(0, 60, 110, 40)
            ));
        } catch (Exception e) {
            System.out.println("Theme asset initialization warning: " + e.getMessage());
        }
    }

    /**
     * Loads an image from classpath resources inside a JAR first, falling back to local filesystem.
     */
    public static BufferedImage loadImageSafe(String path) {
        String resourcePath = path.startsWith("/") ? path : "/" + path;
        try (InputStream is = ThemeManager.class.getResourceAsStream(resourcePath)) {
            if (is != null) {
                BufferedImage img = ImageIO.read(is);
                if (img != null) return img;
            }
        } catch (IOException ignored) {}

        try {
            File f = new File(path);
            if (f.exists()) {
                BufferedImage img = ImageIO.read(f);
                if (img != null) return img;
            }
        } catch (IOException ignored) {}

        // Fallback procedural placeholder
        BufferedImage placeholder = new BufferedImage(64, 64, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = placeholder.createGraphics();
        g.setColor(new Color(100, 100, 120));
        g.fillRect(0, 0, 64, 64);
        g.dispose();
        return placeholder;
    }

    public static BiomeAssets getBiome(String theme) {
        if (theme == null) theme = "GARDEN";
        BiomeAssets biome = biomes.get(theme.toUpperCase());
        return biome != null ? biome : biomes.get("GARDEN");
    }

    public static Image getFishTreatIcon() {
        return fishTreatIcon;
    }

    public static Image getCatnipIcon() {
        return catnipIcon;
    }

    public static Image getPortalRuneIcon() {
        return portalRuneIcon;
    }

    public static Image getKittenIcon() {
        return kittenIcon;
    }

    public static Image getKittenIcon(int index) {
        if (kittenVariants == null || kittenVariants.length == 0) return kittenIcon;
        return kittenVariants[Math.abs(index) % kittenVariants.length];
    }

    public static Image getBushIcon() {
        return bushIcon;
    }

    public static Image getCrackedWallIcon() {
        return crackedWallIcon;
    }
}
