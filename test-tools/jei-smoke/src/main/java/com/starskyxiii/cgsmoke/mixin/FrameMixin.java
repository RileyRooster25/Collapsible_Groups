package com.starskyxiii.cgsmoke.mixin;
import org.spongepowered.asm.mixin.*;import org.spongepowered.asm.mixin.injection.*;import org.spongepowered.asm.mixin.injection.callback.*;import net.minecraft.client.renderer.GameRenderer;import com.starskyxiii.cgsmoke.Observation;
@Mixin(GameRenderer.class) public abstract class FrameMixin {
 @Inject(method="render",at=@At("HEAD")) private void cg$begin(float t,long n,boolean b,CallbackInfo c){Observation.begin();}
 @Inject(method="render",at=@At("TAIL")) private void cg$end(float t,long n,boolean b,CallbackInfo c){Observation.end();}
}
