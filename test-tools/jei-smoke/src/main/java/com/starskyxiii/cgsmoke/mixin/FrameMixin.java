package com.starskyxiii.cgsmoke.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.starskyxiii.cgsmoke.Observation;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.DeltaTracker;
@Mixin(GameRenderer.class)
public abstract class FrameMixin {
 @Inject(method="render",at=@At("HEAD")) private void cg$begin(DeltaTracker d,boolean a,CallbackInfo c) { Observation.begin(); }
 @Inject(method="render",at=@At("TAIL")) private void cg$end(DeltaTracker d,boolean a,CallbackInfo c) { Observation.end(); }
}
