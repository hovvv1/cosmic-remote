package dev.cosmic.remote;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Registre des compétences + boucle de tick. */
public final class Skills {
    private Skills() {}

    public static final String[] CODE = {"SA-03", "SA-04", "SA-05", "SA-06"};
    public static final String[] NAME = {"Singularity", "Perseid", "Helios", "Nova"};
    public static final String[] DESC = {"trou noir", "météorite", "soleil", "supernova"};
    public static final int[] COLOR = {0xB06CFF, 0xFF7A2E, 0xFFD23F, 0x6FD0FF};

    private static final List<Effect> ACTIVE = new ArrayList<>();
    private static final Map<UUID, Integer> SELECTED = new HashMap<>();

    public static int selected(ServerPlayer p) {
        return SELECTED.getOrDefault(p.getUUID(), 0);
    }

    public static int cycle(ServerPlayer p) {
        int next = (selected(p) + 1) % CODE.length;
        SELECTED.put(p.getUUID(), next);
        return next;
    }

    public static Component label(int i) {
        return Component.literal(CODE[i] + " · " + NAME[i] + "  (" + DESC[i] + ")")
                .withStyle(Style.EMPTY.withColor(COLOR[i]));
    }

    /** Lance une compétence. Une seule compétence active par joueur à la fois. */
    public static boolean cast(ServerLevel level, ServerPlayer p, int skill, Vec3 target) {
        for (Effect e : ACTIVE) {
            if (e.ownerId.equals(p.getUUID()) && !(e instanceof CraterJob)) return false;
        }
        Effect fx = switch (skill) {
            case 0 -> new BlackHole(level, p, target);
            case 1 -> new Meteorite(level, p, target);
            case 2 -> new Sun(level, p, target);
            default -> new Supernova(level, p, target);
        };
        ACTIVE.add(fx);
        return true;
    }

    /** Ajoute un effet secondaire (ex : le cratère) pendant le tick en cours. */
    private static final List<Effect> PENDING = new ArrayList<>();
    public static void spawn(Effect e) { PENDING.add(e); }

    public static void tick(MinecraftServer server) {
        Iterator<Effect> it = ACTIVE.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            try {
                e.age++;
                if (e.tick()) it.remove();
            } catch (Exception ex) {
                CosmicRemote.LOG.error("Erreur dans une compétence, annulée", ex);
                it.remove();
            }
        }
        if (!PENDING.isEmpty()) {
            ACTIVE.addAll(PENDING);
            PENDING.clear();
        }
    }

    public static void clear() {
        ACTIVE.clear();
        PENDING.clear();
        SELECTED.clear();
    }
}
