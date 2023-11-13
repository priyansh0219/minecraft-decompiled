package net.minecraft.world;

import com.google.common.collect.Maps;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Timer;
import java.util.UUID;
import java.util.Map.Entry;
import net.minecraft.SharedConstants;
import net.minecraft.Util;

public class Snooper {
   private static final String POLL_HOST = "http://snoop.minecraft.net/";
   private static final long DATA_SEND_FREQUENCY = 900000L;
   private static final int SNOOPER_VERSION = 2;
   final Map<String, Object> fixedData = Maps.newHashMap();
   final Map<String, Object> dynamicData = Maps.newHashMap();
   final String token = UUID.randomUUID().toString();
   final URL url;
   final SnooperPopulator populator;
   private final Timer timer = new Timer("Snooper Timer", true);
   final Object lock = new Object();
   private final long startupTime;
   private boolean started;
   int count;

   public Snooper(String var1, SnooperPopulator var2, long var3) {
      try {
         this.url = new URL("http://snoop.minecraft.net/" + var1 + "?version=2");
      } catch (MalformedURLException var6) {
         throw new IllegalArgumentException();
      }

      this.populator = var2;
      this.startupTime = var3;
   }

   public void start() {
      if (!this.started) {
      }

   }

   private void populateFixedData() {
      this.setJvmArgs();
      this.setDynamicData("snooper_token", this.token);
      this.setFixedData("snooper_token", this.token);
      this.setFixedData("os_name", System.getProperty("os.name"));
      this.setFixedData("os_version", System.getProperty("os.version"));
      this.setFixedData("os_architecture", System.getProperty("os.arch"));
      this.setFixedData("java_version", System.getProperty("java.version"));
      this.setDynamicData("version", SharedConstants.getCurrentVersion().getId());
      this.populator.populateSnooperInitial(this);
   }

   private void setJvmArgs() {
      int[] var1 = new int[]{0};
      Util.getVmArguments().forEach((var2) -> {
         int var10004 = var1[0];
         int var10001 = var1[0];
         var1[0] = var10004 + 1;
         this.setDynamicData("jvm_arg[" + var10001 + "]", var2);
      });
      this.setDynamicData("jvm_args", var1[0]);
   }

   public void prepare() {
      this.setFixedData("memory_total", Runtime.getRuntime().totalMemory());
      this.setFixedData("memory_max", Runtime.getRuntime().maxMemory());
      this.setFixedData("memory_free", Runtime.getRuntime().freeMemory());
      this.setFixedData("cpu_cores", Runtime.getRuntime().availableProcessors());
      this.populator.populateSnooper(this);
   }

   public void setDynamicData(String var1, Object var2) {
      synchronized(this.lock) {
         this.dynamicData.put(var1, var2);
      }
   }

   public void setFixedData(String var1, Object var2) {
      synchronized(this.lock) {
         this.fixedData.put(var1, var2);
      }
   }

   public Map<String, String> getValues() {
      LinkedHashMap var1 = Maps.newLinkedHashMap();
      synchronized(this.lock) {
         this.prepare();
         Iterator var3 = this.fixedData.entrySet().iterator();

         Entry var4;
         while(var3.hasNext()) {
            var4 = (Entry)var3.next();
            var1.put((String)var4.getKey(), var4.getValue().toString());
         }

         var3 = this.dynamicData.entrySet().iterator();

         while(var3.hasNext()) {
            var4 = (Entry)var3.next();
            var1.put((String)var4.getKey(), var4.getValue().toString());
         }

         return var1;
      }
   }

   public boolean isStarted() {
      return this.started;
   }

   public void interrupt() {
      this.timer.cancel();
   }

   public String getToken() {
      return this.token;
   }

   public long getStartupTime() {
      return this.startupTime;
   }
}
