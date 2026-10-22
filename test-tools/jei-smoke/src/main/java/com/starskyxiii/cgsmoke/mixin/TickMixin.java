package com.starskyxiii.cgsmoke.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;import net.minecraft.client.Minecraft;import com.starskyxiii.cgsmoke.*;
@Mixin(Minecraft.class) public abstract class TickMixin {
 @Inject(method="tick",at=@At("TAIL")) private void cg$tick(CallbackInfo c){ Observation.tick++;Probe.tick(); }
}
