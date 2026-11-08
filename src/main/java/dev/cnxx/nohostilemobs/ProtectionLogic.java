package dev.cnxx.nohostilemobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public final class ProtectionLogic {
    private static final int INTERVAL_TICKS = 5;
    private static final double PUSH_PER_STRENGTH = 0.5; // blocks per tick
    private static final double PUSH_UP = 0.25;

    private ProtectionLogic() {}

    public static void tickLevel(ServerLevel level) {
        if (level.getServer().getTickCount() % INTERVAL_TICKS != 0) return;
        if (!ProtectionManager.hasAnyActive()) return;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) continue;
            ProtectionSettings settings = ProtectionManager.getActive(player.getUUID());
            if (settings != null) protect(level, player, settings);
        }
    }

    private static void protect(ServerLevel level, ServerPlayer player, ProtectionSettings s) {
        List<Entity> targets = level.getEntities(player, player.getBoundingBox().inflate(s.radius()),
                e -> isTarget(e, player, s));

        for (Entity entity : targets) {
            // Repelling projectiles doesn't work reliably (fireballs keep accelerating toward their target).
            if (entity instanceof Projectile || s.behavior() == Behavior.REMOVE) {
                entity.discard();
            } else {
                repel(entity, player, s.strength());
            }
        }
    }

    private static boolean isTarget(Entity e, Player player, ProtectionSettings s) {
        if (!e.isAlive() || !inRange(e.position(), player.position(), s.radius())) return false;
        if (isExcluded(e.getType(), s)) return false;

        if (e instanceof Enemy) {
            if (e.hasCustomName()) return false;
            Entity vehicle = e.getVehicle();
            // Mobs in boats or minecarts are usually trapped on purpose.
            return vehicle == null || vehicle instanceof Enemy;
        }

        if (s.removeProjectiles() && e instanceof Projectile projectile) {
            // Never touch player projectiles, including the player's own tridents.
            if (projectile.getOwner() instanceof Player) return false;
            // Skip arrows stuck in the ground.
            return projectile.getDeltaMovement().lengthSqr() > 1.0E-4;
        }
        return false;
    }

    private static void repel(Entity entity, Player player, float strength) {
        double dx = entity.getX() - player.getX();
        double dz = entity.getZ() - player.getZ();
        double len = Math.sqrt(dx * dx + dz * dz);

        if (len < 1.0E-3) {
            double angle = entity.getRandom().nextDouble() * Math.PI * 2;
            dx = Math.cos(angle);
            dz = Math.sin(angle);
        } else {
            dx /= len;
            dz /= len;
        }

        double push = PUSH_PER_STRENGTH * strength;
        double up = Math.max(entity.getDeltaMovement().y, PUSH_UP);
        entity.setDeltaMovement(dx * push, up, dz * push);
        entity.syncVelocity = true; // otherwise clients don't see the push
    }

    public static boolean shouldBlockSpawn(ServerLevel level, Mob mob) {
        if (!(mob instanceof Enemy) || !ProtectionManager.hasAnyActive()) return false;

        Vec3 pos = mob.position();
        for (ServerPlayer player : level.players()) {
            ProtectionSettings s = ProtectionManager.getActive(player.getUUID());
            if (s == null || !s.blockSpawns() || player.isSpectator()) continue;
            if (isExcluded(mob.getType(), s)) continue;
            if (inRange(pos, player.position(), s.radius())) return true;
        }
        return false;
    }

    private static boolean isExcluded(EntityType<?> type, ProtectionSettings s) {
        return s.excluded().contains(EntityType.getKey(type));
    }

    // Cylinder instead of a box, so the area matches the ring.
    static boolean inRange(Vec3 pos, Vec3 center, double radius) {
        double dx = pos.x - center.x;
        double dz = pos.z - center.z;
        return dx * dx + dz * dz <= radius * radius && Math.abs(pos.y - center.y) <= radius;
    }
}
