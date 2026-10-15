package com.starskyxiii.cgsmoke;
import com.google.gson.*;import com.sun.net.httpserver.*;import net.minecraft.client.Minecraft;import java.net.*;import java.nio.charset.StandardCharsets;import java.nio.file.*;import java.util.*;import java.util.concurrent.*;
public final class Probe {
 static final Queue<Runnable> tasks=new ConcurrentLinkedQueue<>();static boolean started,draining;static String session;
 public static void tick(){
  if(!started){started=true;start();}if(draining)return;draining=true;try{Runnable r;while((r=tasks.poll())!=null)r.run();}finally{draining=false;}
 }
 static void start(){try{
  if(!Boolean.getBoolean("cgsmoke.enabled"))return;session=System.getProperty("cgsmoke.session");
  var mc=Minecraft.getInstance();var marker=JsonParser.parseString(Files.readString(mc.gameDirectory.toPath().resolve(".cg-smoke-owned.json"))).getAsJsonObject();
  if(session==null||!session.equals(marker.get("session").getAsString()))throw new SecurityException("Instance ownership mismatch");
  var server=HttpServer.create(new InetSocketAddress("127.0.0.1",Integer.getInteger("cgsmoke.port")),0);
  server.createContext("/rpc",exchange->{try{
   if(!("Bearer "+session).equals(exchange.getRequestHeaders().getFirst("Authorization"))){exchange.sendResponseHeaders(403,-1);return;}
   byte[] bytes=exchange.getRequestBody().readNBytes(16385);if(bytes.length>16384)throw new IllegalArgumentException("Request too large");
   var request=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8)).getAsJsonObject();String method=request.get("method").getAsString();var params=request.getAsJsonObject("params");
   CompletableFuture<JsonObject> future=new CompletableFuture<>();tasks.add(()->{try{future.complete(call(method,params));}catch(Throwable e){future.completeExceptionally(e);}});
   JsonObject response=new JsonObject();try{response.add("result",future.get(120,TimeUnit.SECONDS));}catch(Exception e){response.addProperty("error",e.toString());}
   byte[] result=response.toString().getBytes(StandardCharsets.UTF_8);exchange.sendResponseHeaders(200,result.length);exchange.getResponseBody().write(result);
  }finally{exchange.close();}});
  server.setExecutor(Executors.newSingleThreadExecutor(r->{Thread t=new Thread(r,"CG smoke HTTP");t.setDaemon(true);return t;}));server.start();
 }catch(Exception e){throw new IllegalStateException("Smoke bridge startup",e);}}
 static JsonObject call(String method,JsonObject p)throws Exception{
  var mc=Minecraft.getInstance();JsonObject o=new JsonObject();
  switch(method){
   case "identity"->{o.addProperty("session",session);o.addProperty("pid",ProcessHandle.current().pid());o.addProperty("gameDir",mc.gameDirectory.getAbsolutePath());o.addProperty("minecraft",net.minecraft.SharedConstants.getCurrentVersion().getName());}
   case "observe"->{return Observation.snapshot();}
   case "input"->{return NativeInput.execute(p);}
   case "world.create"->{
    String name="CG JEI Smoke";if(mc.level!=null||mc.getLevelSource().levelExists(name))throw new IllegalStateException("Smoke world already exists");
    var settings=new net.minecraft.world.level.LevelSettings(name,net.minecraft.world.level.GameType.CREATIVE,false,net.minecraft.world.Difficulty.PEACEFUL,true,new net.minecraft.world.level.GameRules(),net.minecraft.world.level.WorldDataConfiguration.DEFAULT);
    mc.createWorldOpenFlows().createFreshLevel(name,settings,new net.minecraft.world.level.levelgen.WorldOptions(4815162342L,false,false),registry->registry.registryOrThrow(net.minecraft.core.registries.Registries.WORLD_PRESET).getHolderOrThrow(net.minecraft.world.level.levelgen.presets.WorldPresets.FLAT).value().createWorldDimensions());
   }
   case "screenshot"->{try(var image=net.minecraft.client.Screenshot.takeScreenshot(mc.getMainRenderTarget())){o.addProperty("png",Base64.getEncoder().encodeToString(image.asByteArray()));}}
   case "quit"->{if(mc.level!=null)mc.level.disconnect();mc.clearLevel();mc.stop();}
   default->throw new IllegalArgumentException("Unknown method: "+method);
  }return o;
 }
}
