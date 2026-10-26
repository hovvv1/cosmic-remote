package dev.cosmic.remote;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * SA-04 · Perseid.
 * Marque au sol -> la météorite entre dans l'atmosphère en biais -> impact -> cratère avec un noyau de magma.
 */
public final class Meteorite extends Effect {
    private final int markTicks = ticks(45);
    private final int fallTicks = ticks(70);
    private final Vec3 start;
    private final double size = 2.2 * power();
    private boolean impacted = false;

    public Meteorite(ServerLevel level, ServerPlayer owner, Vec3 target) {
        super(level, owner, 1, target);
        // elle arrive en biais, dans la direction opposée à celle où regarde le joueur (ou aléatoire)
        double yaw = Math.toRadians(owner.getYRot() + 180 + (level.getRandom().nextDouble() - 0.5) * 60);
        double horiz = 70 * Math.max(1.0, power());
        this.start = ground.add(-Math.sin(yaw) * horiz, 110 * Math.max(1.0, power()), Math.cos(yaw) * horiz);
        announce(Skills.CODE[1] + " · " + Skills.NAME[1], "Météorite en approche...", Skills.COLOR[1]);
        Fx.sound(level, ground, SoundEvents.ELDER_GUARDIAN_CURSE, 2.0F, 0.6F);
    }

    @Override
    public boolean tick() {
        RandomSource rnd = level.getRandom();

        // ---- marque + alerte ----
        if (age <= markTicks) {
            double prog = age / (double) markTicks;
            double r = 12 * power() * (1.0 - 0.6 * prog);
            Fx.ring(level, Fx.dust(0xFF5A1F, 1.5F), ground.add(0, 0.15, 0), r, 56, age * 0.15, 0);
            Fx.ring(level, ParticleTypes.FLAME, ground.add(0, 0.2, 0), r * 0.5, 14, -age * 0.2, 0.01);
            // petit faisceau vertical pour repérer l'endroit
            Fx.p(level, ParticleTypes.END_ROD, ground.x, ground.y + 1 + (age % 20), ground.z, 1, 0.02, 0, 0.02, 0);
            if (age % 12 == 0) Fx.sound(level, ground, SoundEvents.NOTE_BLOCK_BELL, 3.0F, 0.5F + (float) prog);
            return false;
        }

        int t = age - markTicks;

        // ---- chute ----
        if (t <= fallTicks) {
            double f = t / (double) fallTicks;
            double ease = f * f;                          // accélère
            Vec3 pos = start.lerp(ground, ease);
            Vec3 dir = ground.subtract(start).normalize();

            // boule de feu
            Fx.sphere(level, Fx.dust(0xFF3B00, 2.8F), pos, size, 40, 0);
            Fx.sphere(level, Fx.dust(0xFFC04D, 1.6F), pos, size * 0.6, 20, 0);
            Fx.p(level, ParticleTypes.FLAME, pos.x, pos.y, pos.z, 12, size * 0.5, size * 0.5, size * 0.5, 0.05);
            Fx.p(level, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 3, size * 0.4, size * 0.4, size * 0.4, 0);

            // longue traînée derrière
            for (int i = 1; i <= 14; i++) {
                Vec3 q = pos.subtract(dir.scale(i * 1.8 * Math.max(1.0, power())));
                double spread = 0.2 + i * 0.12;
                Fx.p(level, ParticleTypes.LARGE_SMOKE, q.x, q.y, q.z, 2, spread, spread, spread, 0.01);
                if (i < 8) Fx.p(level, ParticleTypes.FLAME, q.x, q.y, q.z, 2, spread * 0.6, spread * 0.6, spread * 0.6, 0.02);
            }

            // grondement qui se rapproche
            if (t % 6 == 0) {
                Fx.sound(level, pos, SoundEvents.FIRECHARGE_USE, 6.0F, 0.4F);
            }
            if (t == fallTicks - 12) {
                Fx.sound(level, pos, SoundEvents.TRIDENT_THUNDER, 8.0F, 0.5F);
            }
            // la boule s'embrase d'étincelles
            if (rnd.nextInt(3) == 0) Fx.p(level, ParticleTypes.LAVA, pos.x, pos.y, pos.z, 4, 1, 1, 1, 0);
            return false;
        }

        // ---- impact ----
        if (!impacted) {
            impacted = true;
            impact();
        }

        // effet de poussière et de braises qui retombent quelques ticks
        int after = t - fallTicks;
        if (after <= 30) {
            Fx.p(level, ParticleTypes.LARGE_SMOKE, ground.x, ground.y + 2, ground.z, 6, 4, 2, 4, 0.05);
            Fx.p(level, ParticleTypes.LAVA, ground.x, ground.y + 4, ground.z, 3, 4, 3, 4, 0);
            return false;
        }
        return true;
    }

    private void impact() {
        Fx.sound(level, ground, SoundEvents.GENERIC_EXPLODE, 8.0F, 0.6F);
        Fx.sound(level, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 8.0F, 0.7F);
        Fx.p(level, ParticleTypes.EXPLOSION_EMITTER, ground.x, ground.y + 1, ground.z, 4, 3, 1, 3, 0);
        Fx.shock(level, Fx.dust(0xFFAA55, 2.4F), ground.add(0, 0.5, 0), 1, 90, 1.1);
        Fx.shock(level, ParticleTypes.LARGE_SMOKE, ground.add(0, 0.5, 0), 2, 60, 0.7);
        Fx.p(level, ParticleTypes.FLAME, ground.x, ground.y + 1, ground.z, 150, 3, 1, 3, 0.5);
        Fx.p(level, Fx.block(Blocks.DIRT.defaultBlockState()), ground.x, ground.y + 1, ground.z, 200, 3, 2, 3, 0.9);

        double blast = 14 * power();
        for (Entity e : entitiesNear(ground, blast)) {
            if (!canAffect(e)) continue;
            double d = e.position().distanceTo(ground);
            if (d > blast) continue;
            double falloff = 1.0 - d / blast;
            push(e, ground.add(0, -1, 0), 1.8 * falloff + 0.3, 0.7 * falloff);
            LivingEntity le = (LivingEntity) e;
            hurt(le, DamageTypes.EXPLOSION, dmg(30 * falloff + 3));
            le.igniteForSeconds((float) (8 * falloff + 2));
        }

        int radius = (int) Math.max(4, Math.min(22, Math.round(8 * power())));
        Skills.spawn(new CraterJob(level, ownerId, 1, ground, radius, 900, Blocks.MAGMA_BLOCK, Blocks.OBSIDIAN));
    }
}
