package dev.cosmic.remote;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

/** Réglages dans config/cosmic_remote.json (puissance, dégâts, vitesse par compétence). */
public final class Config {
    public static final class Tuning {
        public double power = 1.0;   // taille / portée de l'effet (0.25 à 3)
        public double damage = 1.0;  // multiplicateur de dégâts (0 à 5)
        public double speed = 1.0;   // vitesse de déroulement (0.25 à 3)
    }

    public static final class Data {
        public boolean noCooldown = false;
        public int cooldownSeconds = 20;
        public boolean hurtCaster = false;   // l'invocateur est-il blessé par ses propres effets ?
        public boolean breakBlocks = true;   // false = effets visuels et dégâts seulement
        public int maxRange = 160;           // portée de visée
        public Tuning blackHole = new Tuning();
        public Tuning meteorite = new Tuning();
        public Tuning sun = new Tuning();
        public Tuning supernova = new Tuning();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static Data data = new Data();

    public static Tuning tuning(int skill) {
        return switch (skill) {
            case 0 -> data.blackHole;
            case 1 -> data.meteorite;
            case 2 -> data.sun;
            default -> data.supernova;
        };
    }

    public static void load() {
        Path file = FabricLoader.getInstance().getConfigDir().resolve("cosmic_remote.json");
        try {
            if (Files.exists(file)) {
                Data read = GSON.fromJson(Files.readString(file), Data.class);
                if (read != null) data = read;
            }
            clamp();
            Files.writeString(file, GSON.toJson(data));
        } catch (Exception e) {
            CosmicRemote.LOG.warn("Impossible de lire la config, valeurs par défaut utilisées", e);
        }
    }

    private static void clamp() {
        if (data.blackHole == null) data.blackHole = new Tuning();
        if (data.meteorite == null) data.meteorite = new Tuning();
        if (data.sun == null) data.sun = new Tuning();
        if (data.supernova == null) data.supernova = new Tuning();
        for (int i = 0; i < 4; i++) {
            Tuning t = tuning(i);
            t.power = Math.max(0.25, Math.min(3.0, t.power));
            t.damage = Math.max(0.0, Math.min(5.0, t.damage));
            t.speed = Math.max(0.25, Math.min(3.0, t.speed));
        }
        data.cooldownSeconds = Math.max(0, Math.min(600, data.cooldownSeconds));
        data.maxRange = Math.max(16, Math.min(512, data.maxRange));
    }
}
