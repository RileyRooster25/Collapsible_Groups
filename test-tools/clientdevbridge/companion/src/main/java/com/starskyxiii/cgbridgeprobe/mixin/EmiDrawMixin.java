package com.starskyxiii.cgbridgeprobe.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import com.starskyxiii.cgbridgeprobe.EmiObservation;
import dev.emi.emi.api.stack.EmiIngredient;
import net.minecraft.client.gui.GuiGraphics;
@Mixin(targets="dev.emi.emi.screen.StackBatcher",remap=false)
public abstract class EmiDrawMixin {
 @Inject(target=@Desc(value="render",args={EmiIngredient.class,GuiGraphics.class,int.class,int.class,float.class}),at=@At("RETURN")) private void cg$draw(EmiIngredient i,GuiGraphics g,int x,int y,float t,CallbackInfo c) { EmiObservation.draw(i,x,y); }
}
