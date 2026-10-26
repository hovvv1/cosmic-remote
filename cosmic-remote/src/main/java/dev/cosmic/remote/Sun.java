package dev.cosmic.remote;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * SA-05 · Helios.
 * Un soleil se lève au-dessus de la cible, descend lentement, brûle tout, fait fondre la glace,
 * évapore l'eau et vitrifie le sable, puis explose en nova et laisse un cratère au cœur incandescent.
 */
public final class Sun extends Effect {
    private final int markTicks = ticks(40);
    private final int riseTicks = ticks(50);
    private final int lifeTicks = ticks(180);
    private final int novaTicks = ticks(20);
    private final double sunR = 4.5 * power();
    private final double heatR = 26 * power();

    public Sun(ServerLevel level, ServerPlayer owner, Vec3 target) {
        super(level, owner, 2, target);
        announce(Skills.CODE[2] + " · " + Skills.NAME[2], "Un soleil se lève...", Skills.COLOR[2]);
        Fx.sound(level, ground, SoundEvents.BEACON_AMBIENT, 4.0F, 0.5F);
    }

    /** Hauteur du centre du soleil : monte, puis redescend lentement. */
    private Vec3 sunPos(int t) {
        double top = 34 * power(), low = 19 * power();
        double h = (t <= riseTicks)
                ? top
                : top - (top - low) * Math.min(1.0, (t - riseTicks) / (double) lifeTicks);
        return ground.add(0, h, 0);
    }

    @Override
    public boolean tick() {
        RandomSource rnd = level.getRandom();

        // ---- marque : cercle de lumière au sol ----
        if (age <= markTicks) {
            double prog = age / (double) markTicks;
            Fx.ring(level, Fx.dust(0xFFE066, 1.6F), ground.add(0, 0.15, 0), heatR * 0.55 * (1 - 0.3 * prog), 64, age * 0.1, 0);
            Fx.ring(level, ParticleTypes.END_ROD, ground.add(0, 0.2, 0), 5 * power(), 16, age * 0.3, 0.02);
            if (age % 10 == 0) Fx.sound(level, ground, SoundEvents.BEACON_POWER_SELECT, 3.0F, 0.5F + (float) prog);
            return false;
        }
        int t = age - markTicks;

        // ---- apparition + vie du soleil ----
        if (t <= riseTicks + lifeTicks) {
            double grow = Math.min(1.0, t / (double) riseTicks);
            Vec3 c = sunPos(t);
            drawSun(rnd, c, sunR * grow, t, 1.0);
            heat(rnd, c, t, grow);
            if (t % 30 == 0) Fx.sound(level, c, SoundEvents.FIRECHARGE_USE, 6.0F, 0.4F);
            return false;
        }

        // ---- nova : le soleil enfle puis explose ----
        int n = t - riseTicks - lifeTicks;
        Vec3 c = sunPos(riseTicks + lifeTicks);
        if (n <= novaTicks) {
            double prog = n / (double) novaTicks;
            drawSun(rnd, c, sunR * (1 + 1.6 * prog), t, 1.0 + 2 * prog);
            Fx.sphere(level, Fx.dust(0xFFFFFF, 3.0F), c, sunR * (1 + 1.6 * prog) * 1.05, 40, 0);
            if (n == 1) Fx.sound(level, c, SoundEvents.WARDEN_SONIC_BOOM, 8.0F, 0.5F);
            return false;
        }
        detonate(c);
        return true;
    }

    private void drawSun(RandomSource rnd, Vec3 c, double r, int t, double intensity) {
        if (r < 0.2) return;
        // surface : or -> blanc
        Fx.sphere(level, Fx.dust(0xFFC83D, 3.2F), c, r, (int) (60 * intensity), 0);
        Fx.sphere(level, Fx.dust(0xFFF6C8, 2.2F), c, r * 0.8, 30, 0);
        Fx.sphere(level, ParticleTypes.FLAME, c, r * 1.02, 14, 0.02);
        // couronne : rayons qui sortent
        for (int i = 0; i < 14; i++) {
            double u = rnd.nextDouble() * 2 - 1, a = rnd.nextDouble() * Math.PI * 2, s = Math.sqrt(1 - u * u);
            Vec3 dir = new Vec3(s * Math.cos(a), u, s * Math.sin(a));
            Fx.p(level, ParticleTypes.END_ROD, c.x + dir.x * r, c.y + dir.y * r, c.z + dir.z * r, 0, dir.x, dir.y, dir.z, 0.12 + rnd.nextDouble() * 0.2);
        }
        // éruptions solaires : arches qui tournent
        for (int k = 0; k < 2; k++) {
            double base = t * 0.08 + k * Math.PI;
            for (int i = 0; i <= 18; i++) {
                double f = i / 18.0;
                double arch = Math.sin(f * Math.PI);
                double ang = base + f * 0.9;
                double rr = r * (1.0 + 0.55 * arch);
                Fx.p(level, Fx.dust(0xFF7A1A, 1.5F), c.x + Math.cos(ang) * rr, c.y + arch * r * 0.9, c.z + Math.sin(ang) * rr, 1, 0, 0, 0, 0);
            }
        }
        // faisceau de chaleur vers le sol
        for (int i = 0; i < 6; i++) {
            double f = rnd.nextDouble();
            Fx.p(level, ParticleTypes.FLAME, c.x + (rnd.nextDouble() - 0.5) * r, c.y - f * (c.y - ground.y),
                    c.z + (rnd.nextDouble() - 0.5) * r, 1, 0.1, 0, 0.1, 0.0);
        }
    }

    private void heat(RandomSource rnd, Vec3 c, int t, double grow) {
        // entités : brûlure
        if (t % 10 == 0) {
            for (Entity e : entitiesNear(ground.add(0, heatR * 0.4, 0), heatR)) {
                if (!canAffect(e)) continue;
                double dx = e.getX() - ground.x, dz = e.getZ() - ground.z;
                if (Math.sqrt(dx * dx + dz * dz) > heatR * grow) continue;
                LivingEntity le = (LivingEntity) e;
                le.igniteForSeconds(6);
                hurt(le, DamageTypes.IN_FIRE, dmg(3));
            }
        }
        // blocs : transformations de chaleur
        if (t % 4 == 0 && Config.data.breakBlocks) {
            int tries = (int) (40 * power());
            double rr = heatR * 0.8 * grow;
            for (int i = 0; i < tries; i++) {
                double a = rnd.nextDouble() * Math.PI * 2, rad = rr * Math.sqrt(rnd.nextDouble());
                int x = (int) Math.floor(ground.x + Math.cos(a) * rad), z = (int) Math.floor(ground.z + Math.sin(a) * rad);
                int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z) - 1;
                BlockPos p = new BlockPos(x, y, z);
                BlockState s = level.getBlockState(p);
                Block b = s.getBlock();
                BlockState to = null;
                if (b == Blocks.SNOW || b == Blocks.POWDER_SNOW) to = Blocks.AIR.defaultBlockState();
                else if (b == Blocks.SNOW_BLOCK || b == Blocks.ICE || b == Blocks.PACKED_ICE) to = Blocks.WATER.defaultBlockState();
                else if (b == Blocks.WATER) to = Blocks.AIR.defaultBlockState();
                else if (b == Blocks.SAND || b == Blocks.RED_SAND) to = Blocks.GLASS.defaultBlockState();
                else if (b == Blocks.GRASS_BLOCK || b == Blocks.MYCELIUM) to = Blocks.COARSE_DIRT.defaultBlockState();
                else if (s.is(net.minecraft.tags.BlockTags.LEAVES)) to = Blocks.AIR.defaultBlockState();
                else if (b == Blocks.COBBLESTONE || b == Blocks.STONE) to = rnd.nextInt(6) == 0 ? Blocks.MAGMA_BLOCK.defaultBlockState() : null;
                if (to != null) {
                    level.setBlock(p, to, 3);
                    Fx.p(level, ParticleTypes.LARGE_SMOKE, x + 0.5, y + 1.2, z + 0.5, 3, 0.2, 0.3, 0.2, 0.02);
                }
            }
        }
    }

    private void detonate(Vec3 c) {
        Fx.sound(level, c, SoundEvents.GENERIC_EXPLODE, 8.0F, 0.5F);
        Fx.sound(level, ground, SoundEvents.LIGHTNING_BOLT_THUNDER, 8.0F, 0.5F);
        Fx.p(level, ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 6, 4, 4, 4, 0);
        Fx.sphere(level, Fx.dust(0xFFFFFF, 4.0F), c, sunR * 2.6, 200, 0);
        for (int i = 0; i < 4; i++) {
            Fx.shock(level, Fx.dust(i % 2 == 0 ? 0xFFD23F : 0xFFFFFF, 2.6F), ground.add(0, 0.5 + i * 0.4, 0), 1 + i, 90, 1.3 - i * 0.2);
        }
        Fx.p(level, ParticleTypes.FLAME, ground.x, ground.y + 1, ground.z, 200, 6, 1, 6, 0.6);

        double blast = 22 * power();
        for (Entity e : entitiesNear(ground, blast)) {
            if (!canAffect(e)) continue;
            double d = e.position().distanceTo(ground);
            if (d > blast) continue;
            double falloff = 1.0 - d / blast;
            push(e, ground.add(0, -2, 0), 1.6 * falloff + 0.3, 0.6 * falloff);
            LivingEntity le = (LivingEntity) e;
            hurt(le, DamageTypes.EXPLOSION, dmg(28 * falloff + 2));
            le.igniteForSeconds((float) (10 * falloff + 3));
        }

        int radius = (int) Math.max(4, Math.min(22, Math.round(7 * power())));
        Skills.spawn(new CraterJob(level, ownerId, 2, ground, radius, 700, Blocks.GLOWSTONE, Blocks.MAGMA_BLOCK));
    }
}
