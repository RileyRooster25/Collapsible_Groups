package com.starskyxiii.cgsmoke.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
import net.minecraft.client.KeyboardHandler;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(KeyboardHandler.class)
public interface KeyboardAccess {
 @Invoker("keyPress") void cg$key(long w,int key,int scan,int action,int mods);
 @Invoker("charTyped") void cg$character(long w,int c,int mods);
}
