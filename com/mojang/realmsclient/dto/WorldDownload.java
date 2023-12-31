package com.mojang.realmsclient.dto;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.realmsclient.util.JsonUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class WorldDownload extends ValueObject {
   private static final Logger LOGGER = LogManager.getLogger();
   public String downloadLink;
   public String resourcePackUrl;
   public String resourcePackHash;

   public static WorldDownload parse(String var0) {
      JsonParser var1 = new JsonParser();
      JsonObject var2 = var1.parse(var0).getAsJsonObject();
      WorldDownload var3 = new WorldDownload();

      try {
         var3.downloadLink = JsonUtils.getStringOr("downloadLink", var2, "");
         var3.resourcePackUrl = JsonUtils.getStringOr("resourcePackUrl", var2, "");
         var3.resourcePackHash = JsonUtils.getStringOr("resourcePackHash", var2, "");
      } catch (Exception var5) {
         LOGGER.error("Could not parse WorldDownload: {}", var5.getMessage());
      }

      return var3;
   }
}
