package dev.cosmic.remote;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ItemEntity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/**
 * SA-03 · Singularity.
 * Phases : marque au sol -> naissance -> aspiration + disque d'accrétion -> effondrement -> explosion + cratère.
 */
public final class BlackHole extends Effect {
    private final int markTicks = ticks(50);
    private final int lifeTicks = ticks(220);
    private final int collapseTicks = ticks(30);
    private final double coreR = 1.6 * power();
    private final double pullR = 20 * power();
    private final double eatMax = 7 * power();
    private final Vec3 center;

    public BlackHole(ServerLevel level, ServerPlayer owner, Vec3 target) {
        super(level, owner, 0, target);
        this.center = ground.add(0, 5.0 * power(), 0);
        announce(Skills.CODE[0] + " · " + Skills.NAME[0], "Trou noir en formation...", Skills.COLOR[0]);
        Fx.sound(level, ground, SoundEvents.BEACON_ACTIVATE, 3.0F, 0.5F);
    }

    @Override
    public boolean tick() {
        RandomSource rnd = level.getRandom();

        // ---- Phase 1 : marque au sol (compte à rebours) ----
        if (age <= markTicks) {
            double prog = age / (double) markTicks;
            double r = (eatMax + 4) * (1.0 - 0.5 * prog);
            Fx.ring(level, Fx.dust(0x7A2BFF, 1.4F), ground.add(0, 0.15, 0), r, 48, age * 0.12, 0);
            Fx.ring(level, ParticleTypes.REVERSE_PORTAL, ground.add(0, 0.2, 0), r * 0.6, 20, -age * 0.2, 0.02);
            if (age % 10 == 0) Fx.sound(level, ground, SoundEvents.PORTAL_AMBIENT, 2.0F, 0.6F + (float) prog);
            if (age == markTicks) {
                Fx.sound(level, center, SoundEvents.END_PORTAL_SPAWN, 4.0F, 0.6F);
            }
            return false;
        }

        int t = age - markTicks;

        // ---- Phase 2 : le trou noir vit ----
        if (t <= lifeTicks) {
            double grow = Math.min(1.0, t / 25.0);
            double r = coreR * grow;
            drawHole(rnd, r, t);
            pullAndEat(rnd, t, grow);
            if (t % 40 == 0) Fx.sound(level, center, SoundEvents.ENDER_DRAGON_GROWL, 2.5F, 0.4F);
            return false;
        }

        // ---- Phase 3 : effondrement ----
        int c = t - lifeTicks;
        if (c <= collapseTicks) {
            double prog = c / (double) collapseTicks;
            double r = coreR + 9 * power() * (1 - prog);
            Fx.sphere(level, ParticleTypes.REVERSE_PORTAL, center, r, 90, 0.0);
            Fx.sphere(level, Fx.dust(0xFFFFFF, 1.6F), center, r * 0.6, 20, 0);
            if (c == 1) Fx.sound(level, center, SoundEvents.WARDEN_SONIC_BOOM, 5.0F, 0.5F);
            pullAndEat(rnd, t, 1.0);
            return false;
        }

        // ---- Phase 4 : explosion ----
        detonate();
        return true;
    }

    private void drawHole(RandomSource rnd, double r, int t) {
        // noyau noir
        Fx.sphere(level, Fx.dust(0x000000, 2.5F), center, r, 55, 0);
        Fx.p(level, ParticleTypes.SQUID_INK, center.x, center.y, center.z, 4, r * 0.4, r * 0.4, r * 0.4, 0.02);

        // horizon des événements : fin liseré blanc-violet
        Fx.sphere(level, Fx.dust(0xE8D0FF, 0.8F), center, r * 1.12, 12, 0);

        // disque d'accrétion : 2 bras en spirale, couleur chaude à l'intérieur, froide à l'extérieur
        double spin = t * 0.25;
        for (int arm = 0; arm < 2; arm++) {
            for (int i = 0; i < 38; i++) {
                double f = i / 38.0;
                double rad = r * 1.6 + f * r * 3.6;
                double a = spin * (1.5 - f) + arm * Math.PI + f * 5.0;
                int col = Fx.lerpColor(0xFFF2C0, 0x7A2BFF, f);
                Vec3 pt = center.add(Math.cos(a) * rad, Math.sin(a * 2) * 0.12 * rad, Math.sin(a) * rad);
                Fx.p(level, Fx.dust(col, 1.1F), pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            }
        }
        // jets polaires
        for (int i = 0; i < 6; i++) {
            double h = 1 + rnd.nextDouble() * 9 * power();
            Fx.p(level, ParticleTypes.END_ROD, center.x, center.y + h, center.z, 1, 0.05, 0, 0.05, 0.0);
            Fx.p(level, ParticleTypes.END_ROD, center.x, center.y - h, center.z, 1, 0.05, 0, 0.05, 0.0);
        }
    }

    private void pullAndEat(RandomSource rnd, int t, double grow) {
        // aspiration
        for (Entity e : entitiesNear(center, pullR)) {
            boolean item = e instanceof ItemEntity;
            if (!item && !canAffect(e)) continue;
            Vec3 d = center.subtract(e.position().add(0, e.getBbHeight() * 0.5, 0));
            double dist = Math.max(0.5, d.length());
            if (dist > pullR) continue;
            double strength = Math.min(0.55, (0.10 + 0.9 / dist) * grow) * Math.min(1.5, power());
            Vec3 v = d.normalize().scale(strength);
            // un peu de rotation autour du trou pour l'effet "tourbillon"
            Vec3 swirl = new Vec3(-d.z, 0, d.x).normalize().scale(strength * 0.35);
            e.setDeltaMovement(e.getDeltaMovement().scale(0.8).add(v).add(swirl));
            e.hurtMarked = true;
            if (item && dist < coreR * 1.5) e.discard();
            if (e instanceof LivingEntity le && dist < coreR * 3.0 && t % 8 == 0) {
                hurt(le, DamageTypes.MAGIC, dmg(4));
            }
        }

        // dévoration des blocs : rayon qui grandit avec le temps
        if (t % 3 == 0 && Config.data.breakBlocks) {
            double rr = Math.min(eatMax, 1.5 + t * 0.045 * power());
            int tries = (int) (30 * power());
            for (int i = 0; i < tries; i++) {
                double u = rnd.nextDouble() * 2 - 1, a = rnd.nextDouble() * Math.PI * 2;
                double s = Math.sqrt(1 - u * u), rad = rr * Math.cbrt(rnd.nextDouble());
                BlockPos p = BlockPos.containing(center.x + rad * s * Math.cos(a), center.y + rad * u, center.z + rad * s * Math.sin(a));
                if (breakable(p)) {
                    var old = level.getBlockState(p);
                    level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                    // le bloc "file" vers le trou
                    Vec3 dir = center.subtract(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5).normalize();
                    Fx.p(level, Fx.block(old), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 0, dir.x, dir.y, dir.z, 0.6);
                }
            }
        }
    }

    private void detonate() {
        Fx.sound(level, center, SoundEvents.GENERIC_EXPLODE, 6.0F, 0.5F);
        Fx.sound(level, center, SoundEvents.LIGHTNING_BOLT_THUNDER, 6.0F, 0.5F);
        Fx.p(level, ParticleTypes.EXPLOSION_EMITTER, center.x, center.y, center.z, 3, 2, 2, 2, 0);
        for (int i = 0; i < 4; i++) {
            Fx.shock(level, Fx.dust(i % 2 == 0 ? 0xB06CFF : 0xFFFFFF, 2.0F), center.add(0, -i * 0.5, 0), 1.0 + i, 72, 0.9 - i * 0.12);
        }
        Fx.sphere(level, ParticleTypes.END_ROD, center, 3, 80, 0.8);

        double blast = 16 * power();
        for (Entity e : entitiesNear(center, blast)) {
            if (!canAffect(e)) continue;
            double d = e.position().distanceTo(center);
            if (d > blast) continue;
            double falloff = 1.0 - d / blast;
            push(e, center, 2.2 * falloff + 0.4, 0.5 * falloff);
            hurt((LivingEntity) e, DamageTypes.EXPLOSION, dmg(26 * falloff + 2));
        }

        // cratère avec un cœur de crying obsidian violet au fond
        int radius = (int) Math.max(4, Math.min(24, Math.round(7 * power())));
        Skills.spawn(new CraterJob(level, ownerId, 0, ground, radius, 700, Blocks.CRYING_OBSIDIAN, Blocks.OBSIDIAN));
    }
}
