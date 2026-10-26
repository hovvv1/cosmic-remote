package dev.cosmic.remote;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.UUID;

/** Une compétence en cours d'exécution. tick() renvoie true quand elle est terminée. */
public abstract class Effect {
    protected final ServerLevel level;
    protected final UUID ownerId;
    protected final Vec3 ground;     // point visé, recalé sur le sol
    protected final Config.Tuning t;
    protected final int skill;
    protected int age = 0;

    protected Effect(ServerLevel level, ServerPlayer owner, int skill, Vec3 target) {
        this(level, owner.getUUID(), skill, target);
    }

    protected Effect(ServerLevel level, UUID ownerId, int skill, Vec3 target) {
        this.level = level;
        this.ownerId = ownerId;
        this.skill = skill;
        this.t = Config.tuning(skill);
        int bx = (int) Math.floor(target.x), bz = (int) Math.floor(target.z);
        // si la visée part dans le ciel, on cherche le sol en dessous
        int gy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, bx, bz);
        double y = (target.y > gy + 2) ? gy : target.y;
        this.ground = new Vec3(target.x, y, target.z);
    }

    /** true = terminé. */
    public abstract boolean tick();

    // ---------- utilitaires communs ----------

    protected double power() { return t.power; }

    /** Durée en ticks ajustée par le curseur de vitesse. */
    protected int ticks(double base) { return Math.max(1, (int) Math.round(base / t.speed)); }

    protected float dmg(double base) { return (float) (base * t.damage); }

    protected ServerPlayer owner() {
        return level.getServer().getPlayerList().getPlayer(ownerId);
    }

    protected boolean canAffect(Entity e) {
        if (!(e instanceof LivingEntity le) || !le.isAlive()) return false;
        if (e instanceof ServerPlayer sp) {
            if (sp.isCreative() || sp.isSpectator()) return false;
            if (sp.getUUID().equals(ownerId) && !Config.data.hurtCaster) return false;
        }
        return true;
    }

    protected List<Entity> entitiesNear(Vec3 c, double r) {
        AABB box = new AABB(c.x - r, c.y - r, c.z - r, c.x + r, c.y + r, c.z + r);
        return level.getEntities((Entity) null, box, e -> true);
    }

    protected void hurt(LivingEntity e, ResourceKey<DamageType> type, float amount) {
        if (amount <= 0) return;
        DamageSource src = new DamageSource(
                level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(type), null, owner());
        e.hurtServer(level, src, amount);
    }

    /** Un bloc qu'on a le droit de détruire ? */
    protected boolean breakable(BlockPos pos) {
        if (!Config.data.breakBlocks) return false;
        BlockState s = level.getBlockState(pos);
        if (s.isAir()) return false;
        return s.getDestroySpeed(level, pos) >= 0 && !s.hasBlockEntity();
    }

    /** Titre style "SA-03 · Singularity" pour les joueurs proches de la cible. */
    protected void announce(String title, String subtitle, int color) {
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceTo(ground) > 128 && !p.getUUID().equals(ownerId)) continue;
            p.connection.send(new ClientboundSetTitlesAnimationPacket(5, 40, 15));
            p.connection.send(new ClientboundSetTitleTextPacket(
                    Component.literal(title).withStyle(Style.EMPTY.withColor(color))));
            p.connection.send(new ClientboundSetSubtitleTextPacket(Component.literal(subtitle)));
        }
    }

    /** Pousse une entité depuis (ou vers) un point. */
    protected void push(Entity e, Vec3 from, double strength, double lift) {
        Vec3 d = e.position().subtract(from);
        if (d.lengthSqr() < 1.0E-4) d = new Vec3(0, 1, 0);
        d = d.normalize().scale(strength);
        e.setDeltaMovement(e.getDeltaMovement().add(d.x, d.y + lift, d.z));
        e.hurtMarked = true;
    }
}
