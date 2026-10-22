package com.starskyxiii.cgsmoke;
import com.google.gson.*;import net.minecraft.client.Minecraft;import net.minecraft.client.gui.components.EditBox;import com.starskyxiii.collapsible_groups.viewer.ViewerLifecycleCoordinator;
public final class Observation {
 public static long frame,tick;public static JsonObject managerButton;public static JsonArray overlayCells=new JsonArray();
 static JsonObject completed=new JsonObject();static Object drawn;static long at;
 public static JsonObject rect(int x,int y,int w,int h){JsonObject o=new JsonObject();o.addProperty("x",x);o.addProperty("y",y);o.addProperty("w",w);o.addProperty("h",h);return o;}
 public static void begin(){frame++;managerButton=null;overlayCells=new JsonArray();}
 public static void end(){
  var mc=Minecraft.getInstance();JsonObject o=new JsonObject();o.addProperty("frame",frame);o.addProperty("tick",tick);o.addProperty("inputCalls",NativeInput.calls);
  o.addProperty("screen",mc.screen==null?"none":mc.screen.getClass().getSimpleName());o.addProperty("inWorld",mc.level!=null);
  o.addProperty("guiWidth",mc.getWindow().getGuiScaledWidth());o.addProperty("guiHeight",mc.getWindow().getGuiScaledHeight());
  try{var adapter=ViewerLifecycleCoordinator.global().activeAdapter().orElse(null);o.addProperty("indexReady",adapter!=null&&adapter.groupIndex().ready());
   o.add("cells",overlayCells);if(managerButton!=null)o.add("managerButton",managerButton);
   var runtime=com.starskyxiii.collapsible_groups.compat.jei.runtime.JeiRuntimeHolder.get();
   if(runtime!=null&&runtime.getIngredientListOverlay().isListDisplayed()||managerButton!=null){
    EditBox e=(EditBox)Reflect.field(runtime.getIngredientListOverlay(),"searchField");JsonObject r=rect(e.getX(),e.getY(),e.getWidth(),e.getHeight());r.addProperty("value",e.getValue());o.add("search",r);
   }
  }catch(Throwable e){o.addProperty("error",e.toString());}
  completed=o;drawn=mc.screen;at=System.nanoTime();
 }
 public static JsonObject snapshot(){var o=completed.deepCopy();o.addProperty("fresh",drawn==Minecraft.getInstance().screen&&System.nanoTime()-at<2_000_000_000L&&Minecraft.getInstance().getOverlay()==null);return o;}
}
