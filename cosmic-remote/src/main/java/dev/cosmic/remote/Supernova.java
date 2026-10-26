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

import java.util.HashSet;
import java.util.Set;

/**
 * SA-06 · Nova.
 * Une étoile bleue s'allume au-dessus de la cible, se contracte, puis explose en coquilles de lumière
 * successives qui balaient tout ; les entités sont blessées une seule fois par coquille. Cratère léger au sol.
 */
public final class Supernova extends Effect {
    private final int markTicks = ticks(40);
    private final int chargeTicks = ticks(90);
    private final int waveTicks = ticks(60);
    private final double maxR = 46 * power();
    private final Vec3 star;
    private final Set<Integer> hitInner = new HashSet<>();
    private final Set<Integer> hitOuter = new HashSet<>();

    public Supernova(ServerLevel level, ServerPlayer owner, Vec3 target) {
        super(level, owner, 3, target);
        this.star = ground.add(0, 24 * power(), 0);
        announce(Skills.CODE[3] + " · " + Skills.NAME[3], "Une étoile s'allume...", Skills.COLOR[3]);
        Fx.sound(level, ground, SoundEvents.BEACON_ACTIVATE, 4.0F, 1.4F);
    }

    @Override
    public boolean tick() {
        RandomSource rnd = level.getRandom();

        // ---- marque ----
        if (age <= markTicks) {
            double prog = age / (double) markTicks;
            Fx.ring(level, Fx.dust(0x6FD0FF, 1.5F), ground.add(0, 0.15, 0), 14 * power() * (1 - 0.4 * prog), 60, age * 0.14, 0);
            Fx.ring(level, ParticleTypes.SNOWFLAKE, ground.add(0, 0.3, 0), 6 * power(), 18, -age * 0.25, 0.01);
            if (age % 10 == 0) Fx.sound(level, ground, SoundEvents.AMETHYST_BLOCK_RESONATE, 3.0F, 0.8F + (float) prog);
            return false;
        }
        int t = age - markTicks;

        // ---- charge : l'étoile se contracte et pulse ----
        if (t <= chargeTicks) {
            double prog = t / (double) chargeTicks;
            double r = (1.5 + 3.0 * Math.sin(prog * Math.PI * 6) * 0.15 + 4 * (1 - prog)) * power();
            Fx.sphere(level, Fx.dust(0xBFEFFF, 2.8F), star, r, 60, 0);
            Fx.sphere(level, Fx.dust(0xFFFFFF, 2.0F), star, r * 0.6, 25, 0);
            // matière aspirée vers l'étoile
            Fx.sphere(level, ParticleTypes.REVERSE_PORTAL, star, (14 * (1 - prog) + 2) * power(), 60, 0.0);
            Fx.sphere(level, ParticleTypes.END_ROD, star, (10 * (1 - prog) + 1) * power(), 12, 0.0);
            if (t % 15 == 0) Fx.sound(level, star, SoundEvents.BEACON_AMBIENT, 4.0F, 0.6F + (float) prog * 1.2F);
            return false;
        }

        // ---- explosion : coquilles d'onde ----
        int w = t - chargeTicks;
        if (w <= waveTicks) {
            double prog = w / (double) waveTicks;
            if (w == 1) {
                Fx.sound(level, star, SoundEvents.WARDEN_SONIC_BOOM, 10.0F, 0.5F);
                Fx.sound(level, star, SoundEvents.GENERIC_EXPLODE, 10.0F, 0.4F);
                Fx.p(level, ParticleTypes.EXPLOSION_EMITTER, star.x, star.y, star.z, 8, 4, 4, 4, 0);
            }
            double r1 = maxR * (1 - Math.pow(1 - prog, 2));      // coquille extérieure, bleue
            double r2 = r1 * 0.72;                               // coquille intérieure, blanche
            double r3 = r1 * 0.45;                               // noyau, or
            Fx.sphere(level, Fx.dust(0x6FD0FF, 3.0F), star, r1, 160, 0);
            Fx.sphere(level, Fx.dust(0xFFFFFF, 2.6F), star, r2, 110, 0);
            Fx.sphere(level, Fx.dust(0xFFC040, 2.2F), star, r3, 70, 0);
            Fx.sphere(level, ParticleTypes.END_ROD, star, r1 * 0.9, 20, 0.1);
            // anneau équatorial rapide
            Fx.ring(level, Fx.dust(0xFFFFFF, 2.0F), star, r1, 80, w * 0.2, 0);

            sweep(r1, hitOuter, 18, 1.8);
            sweep(r2, hitInner, 12, 1.2);

            // éclairs sur la zone au sol pendant la vague
            if (w % 5 == 0) {
                double a = rnd.nextDouble() * Math.PI * 2, d = rnd.nextDouble() * 12 * power();
                Fx.p(level, ParticleTypes.ELECTRIC_SPARK, ground.x + Math.cos(a) * d, ground.y + 1, ground.z + Math.sin(a) * d, 40, 1, 2, 1, 0.4);
            }
            if (w % 12 == 0) Fx.sound(level, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 6.0F, 0.6F);
            return false;
        }

        // ---- fin : cratère léger ----
        int radius = (int) Math.max(3, Math.min(18, Math.round(5 * power())));
        Skills.spawn(new CraterJob(level, ownerId, 3, ground, radius, 600, Blocks.SEA_LANTERN, Blocks.PRISMARINE));
        Fx.sound(level, ground, SoundEvents.AMETHYST_CLUSTER_BREAK, 6.0F, 0.5F);
        return true;
    }

    /** Fait passer une coquille de rayon r : chaque entité n'est touchée qu'une fois. */
    private void sweep(double r, Set<Integer> already, double baseDamage, double knock) {
        for (Entity e : entitiesNear(star, r + 4)) {
            if (!canAffect(e) || already.contains(e.getId())) continue;
            double d = e.position().distanceTo(star);
            if (d > r) continue;
            already.add(e.getId());
            double falloff = Math.max(0.15, 1.0 - d / maxR);
            push(e, star, knock * falloff + 0.3, 0.45 * falloff);
            hurt((LivingEntity) e, DamageTypes.EXPLOSION, dmg(baseDamage * falloff + 2));
        }
    }
}
