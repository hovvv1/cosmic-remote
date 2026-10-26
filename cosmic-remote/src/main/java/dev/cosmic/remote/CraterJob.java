package dev.cosmic.remote;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Creuse un cratère en forme de bol, couche par couche (de haut en bas), pour ne pas faire lagger.
 * À la fin, place un "cœur" brillant au fond (comme les cratères de l'addon d'origine).
 */
public final class CraterJob extends Effect {
    private final List<BlockPos> todo = new ArrayList<>();
    private final BlockPos center;
    private final int radius;
    private final int perTick;
    private final BlockState heart;
    private final BlockState shell;
    private boolean heartPlaced = false;

    public CraterJob(ServerLevel level, java.util.UUID owner, int skill, Vec3 at,
                     int radius, int perTick, Block heart, Block shell) {
        super(level, owner, skill, at);
        this.radius = radius;
        this.perTick = perTick;
        this.heart = heart == null ? null : heart.defaultBlockState();
        this.shell = shell == null ? null : shell.defaultBlockState();
        this.center = BlockPos.containing(at.x, at.y, at.z);

        // bol : sphère centrée un peu sous la surface, aplatie en hauteur
        int cy = center.getY() - (int) Math.round(radius * 0.35);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = -radius; dy <= radius; dy++) {
                    double d = Math.sqrt(dx * dx + dz * dz + (dy * 1.35) * (dy * 1.35));
                    if (d <= radius) {
                        todo.add(new BlockPos(center.getX() + dx, cy + dy, center.getZ() + dz));
                    }
                }
            }
        }
        // du haut vers le bas, et du centre vers l'extérieur pour un effet "excavation"
        todo.sort((a, b) -> {
            int c = Integer.compare(b.getY(), a.getY());
            if (c != 0) return c;
            return Double.compare(a.distSqr(center), b.distSqr(center));
        });
    }

    @Override
    public boolean tick() {
        RandomSource rnd = level.getRandom();
        int n = 0;
        while (!todo.isEmpty() && n < perTick) {
            BlockPos p = todo.remove(0);
            if (breakable(p)) {
                BlockState old = level.getBlockState(p);
                level.setBlock(p, Blocks.AIR.defaultBlockState(), 2);
                if (rnd.nextInt(14) == 0) {
                    Fx.p(level, Fx.block(old), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, 6, 0.4, 0.4, 0.4, 0.15);
                }
            }
            n++;
        }
        if (todo.isEmpty() && !heartPlaced) {
            placeHeart();
            heartPlaced = true;
            return true;
        }
        return false;
    }

    /** Un petit noyau brillant, enfoncé au fond du cratère. */
    private void placeHeart() {
        if (heart == null || !Config.data.breakBlocks) return;
        int cy = center.getY() - (int) Math.round(radius * 0.35) - (int) Math.round(radius * 0.74);
        BlockPos base = new BlockPos(center.getX(), cy + 1, center.getZ());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    BlockPos p = base.offset(dx, dy, dz);
                    if (!breakable(p) && !level.getBlockState(p).isAir()) continue;
                    boolean core = dx == 0 && dy == 0 && dz == 0;
                    BlockState s = core || shell == null ? heart : (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) <= 2 ? shell : null);
                    if (s != null) level.setBlock(p, s, 2);
                }
            }
        }
        Fx.p(level, net.minecraft.core.particles.ParticleTypes.END_ROD,
                base.getX() + 0.5, base.getY() + 1.5, base.getZ() + 0.5, 30, 0.5, 0.8, 0.5, 0.05);
    }
}
