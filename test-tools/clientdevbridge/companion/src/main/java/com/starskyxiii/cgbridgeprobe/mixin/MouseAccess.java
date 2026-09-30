package com.starskyxiii.cgbridgeprobe.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(MouseHandler.class)
public interface MouseAccess {
 @Invoker("onMove") void cg$move(long w,double x,double y);
 @Invoker("onPress") void cg$press(long w,int button,int action,int mods);
 @Invoker("onScroll") void cg$scroll(long w,double dx,double dy);
}
