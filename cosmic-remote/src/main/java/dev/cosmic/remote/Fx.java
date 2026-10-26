package dev.cosmic.remote;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Tous les appels "risqués" (particules, sons) passent ici : si une signature change
 * dans ta version de Minecraft, il n'y a qu'ici à corriger.
 */
public final class Fx {
    private Fx() {}

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(rgb, size);
    }

    public static BlockParticleOption block(BlockState state) {
        return new BlockParticleOption(ParticleTypes.BLOCK, state);
    }

    /** Envoie une particule à TOUS les joueurs du monde (la portée par défaut de 32 blocs est trop courte pour un soleil). */
    public static void p(ServerLevel l, ParticleOptions o, double x, double y, double z,
                         int n, double dx, double dy, double dz, double speed) {
        for (ServerPlayer pl : l.players()) {
            if (pl.distanceToSqr(x, y, z) < 256.0 * 256.0) {
                l.sendParticles(pl, o, true, true, x, y, z, n, dx, dy, dz, speed);
            }
        }
    }

    public static void p(ServerLevel l, ParticleOptions o, Vec3 v, int n, double spread, double speed) {
        p(l, o, v.x, v.y, v.z, n, spread, spread, spread, speed);
    }

    /** Points aléatoires sur une sphère. */
    public static void sphere(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n, double speed) {
        RandomSource rnd = l.getRandom();
        for (int i = 0; i < n; i++) {
            double u = rnd.nextDouble() * 2 - 1;
            double a = rnd.nextDouble() * Math.PI * 2;
            double s = Math.sqrt(1 - u * u);
            p(l, o, c.x + r * s * Math.cos(a), c.y + r * u, c.z + r * s * Math.sin(a), 1, 0, 0, 0, speed);
        }
    }

    /** Anneau horizontal. */
    public static void ring(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n, double phase, double speed) {
        for (int i = 0; i < n; i++) {
            double a = phase + i * (Math.PI * 2 / n);
            p(l, o, c.x + r * Math.cos(a), c.y, c.z + r * Math.sin(a), 1, 0, 0, 0, speed);
        }
    }

    /** Anneau qui part vers l'extérieur (onde de choc). */
    public static void shock(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n, double outward) {
        for (int i = 0; i < n; i++) {
            double a = i * (Math.PI * 2 / n);
            double cx = Math.cos(a), sz = Math.sin(a);
            p(l, o, c.x + r * cx, c.y, c.z + r * sz, 0, cx, 0.02, sz, outward);
        }
    }

    public static void sound(ServerLevel l, Vec3 at, SoundEvent s, float vol, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    public static void sound(ServerLevel l, Vec3 at, Holder<SoundEvent> s, float vol, float pitch) {
        l.playSound(null, at.x, at.y, at.z, s, SoundSource.PLAYERS, vol, pitch);
    }

    public static int lerpColor(int a, int b, double t) {
        t = Math.max(0, Math.min(1, t));
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int g = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
