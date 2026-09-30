package com.starskyxiii.cgbridgeprobe.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.starskyxiii.cgbridgeprobe.NativeInput;
import net.minecraft.client.gui.screens.Screen;
@Mixin(Screen.class)
public abstract class ModifierMixin {
 @Inject(method="hasShiftDown",at=@At("HEAD"),cancellable=true) private static void cg$shift(CallbackInfoReturnable<Boolean> c) { if(NativeInput.active)c.setReturnValue((NativeInput.modifiers&1)!=0); }
 @Inject(method="hasControlDown",at=@At("HEAD"),cancellable=true) private static void cg$control(CallbackInfoReturnable<Boolean> c) { if(NativeInput.active)c.setReturnValue((NativeInput.modifiers&2)!=0); }
 @Inject(method="hasAltDown",at=@At("HEAD"),cancellable=true) private static void cg$alt(CallbackInfoReturnable<Boolean> c) { if(NativeInput.active)c.setReturnValue((NativeInput.modifiers&4)!=0); }
}
