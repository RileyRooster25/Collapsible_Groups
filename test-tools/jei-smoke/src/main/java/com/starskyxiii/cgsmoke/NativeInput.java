package com.starskyxiii.cgsmoke;
import com.google.gson.*;import net.minecraft.client.Minecraft;import org.lwjgl.glfw.GLFW;import com.starskyxiii.cgsmoke.mixin.*;
public final class NativeInput {
 public static long calls;
 static Minecraft mc(){return Minecraft.getInstance();}
 static long window(){return mc().getWindow().getWindow();}
 static void key(int k,int a){((KeyboardAccess)mc().keyboardHandler).cg$key(window(),k,GLFW.glfwGetKeyScancode(k),a,0);}
 static void tap(int k){try{key(k,GLFW.GLFW_PRESS);}finally{key(k,GLFW.GLFW_RELEASE);}}
 public static JsonObject execute(JsonObject p){
  String a=p.get("action").getAsString();
  if(!java.util.Set.of("click","text","move","scroll","key").contains(a))throw new IllegalArgumentException("Unsupported smoke input");
  if(a.equals("key"))tap(p.get("key").getAsInt());
  else {
   var w=mc().getWindow();double x=p.get("x").getAsDouble(),y=p.get("y").getAsDouble();
   if(x<0||y<0||x>=w.getGuiScaledWidth()||y>=w.getGuiScaledHeight())throw new IllegalArgumentException("Off-screen input");
   var m=(MouseAccess)mc().mouseHandler;m.cg$move(window(),x*w.getScreenWidth()/w.getGuiScaledWidth(),y*w.getScreenHeight()/w.getGuiScaledHeight());
   if(a.equals("scroll"))m.cg$scroll(window(),0,p.get("dy").getAsDouble());
   else if(!a.equals("move")){try{m.cg$press(window(),0,GLFW.GLFW_PRESS,0);}finally{m.cg$press(window(),0,GLFW.GLFW_RELEASE,0);}}
   if(a.equals("text")){
    String text=p.get("text").getAsString();int previous=p.get("previousLength").getAsInt();
    if(text.length()>256||previous<0||previous>512)throw new IllegalArgumentException("Text bounds");
    tap(GLFW.GLFW_KEY_END);for(int i=0;i<previous;i++)tap(GLFW.GLFW_KEY_BACKSPACE);
    text.codePoints().forEach(ch->((KeyboardAccess)mc().keyboardHandler).cg$character(window(),ch,0));
   }
  }
  JsonObject o=new JsonObject();o.addProperty("calls",++calls);o.addProperty("route","MouseHandler/KeyboardHandler");return o;
 }
}
