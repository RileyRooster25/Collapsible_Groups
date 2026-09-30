package com.starskyxiii.cgbridgeprobe.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.starskyxiii.cgbridgeprobe.Observation;
import dev.emi.emi.runtime.EmiDrawContext;
@Mixin(targets="dev.emi.emi.screen.EmiScreenManager$ScreenSpace",remap=false)
public abstract class EmiSpaceMixin {
 @Shadow @Final public boolean search;
 @Inject(method="render",at=@At("HEAD")) private void cg$begin(EmiDrawContext d,int x,int y,float t,int offset,CallbackInfo c) { Observation.inEmiSearch=search; }
 @Inject(method="render",at=@At("RETURN")) private void cg$end(EmiDrawContext d,int x,int y,float t,int offset,CallbackInfo c) { Observation.inEmiSearch=false; }
}
