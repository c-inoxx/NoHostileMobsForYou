package dev.cnxx.nohostilemobs.mixin;

import dev.cnxx.nohostilemobs.ProtectionLogic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.NaturalSpawner;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    @Inject(method = "isValidPositionForMob", at = @At("HEAD"), cancellable = true)
    private static void nohostilemobs$blockNearProtectedPlayers(ServerLevel level, Mob mob, double distanceSqr,
                                                               CallbackInfoReturnable<Boolean> cir) {
        if (ProtectionLogic.shouldBlockSpawn(level, mob)) {
            cir.setReturnValue(false);
        }
    }
}
