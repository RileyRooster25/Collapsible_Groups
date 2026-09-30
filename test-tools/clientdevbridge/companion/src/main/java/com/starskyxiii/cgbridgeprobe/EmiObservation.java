package com.starskyxiii.cgbridgeprobe;
import com.google.gson.*;
import dev.emi.emi.api.stack.*;
import dev.emi.emi.screen.EmiScreenManager;
import com.starskyxiii.collapsible_groups.compat.emi.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
public final class EmiObservation {
 public static void draw(EmiIngredient i,int x,int y) {
  if(!Observation.inEmiSearch)return;
  JsonObject o=Observation.rect(x,y,16,16);
  if(i instanceof GroupHeaderEmiStack h) { o.addProperty("role","header");o.addProperty("groupId",h.groupId());o.addProperty("expanded",h.header().expanded()); }
  else { o.addProperty("role","ingredient");JsonArray ids=new JsonArray();for(var s:i.getEmiStacks())ids.add((s instanceof FluidEmiStack?"fluid:":"item:")+s.getId());o.add("ids",ids);if(ids.size()==1)o.add("id",ids.get(0)); }
   if(i instanceof ProjectedChildEmiIngredient child) {o.addProperty("role","child");o.addProperty("groupId",child.parentGroupId());}
   Observation.overlayCells.add(o);
 }
 public static void finish() {
  if(!(Minecraft.getInstance().screen instanceof AbstractContainerScreen<?>))return;
  if(EmiScreenManager.search!=null&&EmiScreenManager.search.visible)Observation.search(EmiScreenManager.search);
  var button=EmiOverlayController.instance();
  if(Boolean.TRUE.equals(Reflect.field(button,"visible")))Observation.managerButton=Observation.rect(Reflect.integer(button,"x"),Reflect.integer(button,"y"),20,20);
 }
}
