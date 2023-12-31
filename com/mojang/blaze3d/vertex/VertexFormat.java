package com.mojang.blaze3d.vertex;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.UnmodifiableIterator;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.stream.Collectors;

public class VertexFormat {
   private final ImmutableList<VertexFormatElement> elements;
   private final ImmutableMap<String, VertexFormatElement> elementMapping;
   private final IntList offsets = new IntArrayList();
   private final int vertexSize;
   private int vertexArrayObject;
   private int vertexBufferObject;
   private int indexBufferObject;

   public VertexFormat(ImmutableMap<String, VertexFormatElement> var1) {
      this.elementMapping = var1;
      this.elements = var1.values().asList();
      int var2 = 0;

      VertexFormatElement var4;
      for(UnmodifiableIterator var3 = var1.values().iterator(); var3.hasNext(); var2 += var4.getByteSize()) {
         var4 = (VertexFormatElement)var3.next();
         this.offsets.add(var2);
      }

      this.vertexSize = var2;
   }

   public String toString() {
      int var10000 = this.elementMapping.size();
      return "format: " + var10000 + " elements: " + (String)this.elementMapping.entrySet().stream().map(Object::toString).collect(Collectors.joining(" "));
   }

   public int getIntegerSize() {
      return this.getVertexSize() / 4;
   }

   public int getVertexSize() {
      return this.vertexSize;
   }

   public ImmutableList<VertexFormatElement> getElements() {
      return this.elements;
   }

   public ImmutableList<String> getElementAttributeNames() {
      return this.elementMapping.keySet().asList();
   }

   public boolean equals(Object var1) {
      if (this == var1) {
         return true;
      } else if (var1 != null && this.getClass() == var1.getClass()) {
         VertexFormat var2 = (VertexFormat)var1;
         return this.vertexSize != var2.vertexSize ? false : this.elementMapping.equals(var2.elementMapping);
      } else {
         return false;
      }
   }

   public int hashCode() {
      return this.elementMapping.hashCode();
   }

   public void setupBufferState() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(this::_setupBufferState);
      } else {
         this._setupBufferState();
      }
   }

   private void _setupBufferState() {
      int var1 = this.getVertexSize();
      ImmutableList var2 = this.getElements();

      for(int var3 = 0; var3 < var2.size(); ++var3) {
         ((VertexFormatElement)var2.get(var3)).setupBufferState(var3, (long)this.offsets.getInt(var3), var1);
      }

   }

   public void clearBufferState() {
      if (!RenderSystem.isOnRenderThread()) {
         RenderSystem.recordRenderCall(this::_clearBufferState);
      } else {
         this._clearBufferState();
      }
   }

   private void _clearBufferState() {
      ImmutableList var1 = this.getElements();

      for(int var2 = 0; var2 < var1.size(); ++var2) {
         VertexFormatElement var3 = (VertexFormatElement)var1.get(var2);
         var3.clearBufferState(var2);
      }

   }

   public int getOrCreateVertexArrayObject() {
      if (this.vertexArrayObject == 0) {
         this.vertexArrayObject = GlStateManager._glGenVertexArrays();
      }

      return this.vertexArrayObject;
   }

   public int getOrCreateVertexBufferObject() {
      if (this.vertexBufferObject == 0) {
         this.vertexBufferObject = GlStateManager._glGenBuffers();
      }

      return this.vertexBufferObject;
   }

   public int getOrCreateIndexBufferObject() {
      if (this.indexBufferObject == 0) {
         this.indexBufferObject = GlStateManager._glGenBuffers();
      }

      return this.indexBufferObject;
   }

   public static enum Mode {
      LINES(4, 2, 2),
      LINE_STRIP(5, 2, 1),
      DEBUG_LINES(1, 2, 2),
      DEBUG_LINE_STRIP(3, 2, 1),
      TRIANGLES(4, 3, 3),
      TRIANGLE_STRIP(5, 3, 1),
      TRIANGLE_FAN(6, 3, 1),
      QUADS(4, 4, 4);

      public final int asGLMode;
      public final int primitiveLength;
      public final int primitiveStride;

      private Mode(int var3, int var4, int var5) {
         this.asGLMode = var3;
         this.primitiveLength = var4;
         this.primitiveStride = var5;
      }

      public int indexCount(int var1) {
         int var2;
         switch(this) {
         case LINE_STRIP:
         case DEBUG_LINES:
         case DEBUG_LINE_STRIP:
         case TRIANGLES:
         case TRIANGLE_STRIP:
         case TRIANGLE_FAN:
            var2 = var1;
            break;
         case LINES:
         case QUADS:
            var2 = var1 / 4 * 6;
            break;
         default:
            var2 = 0;
         }

         return var2;
      }

      // $FF: synthetic method
      private static VertexFormat.Mode[] $values() {
         return new VertexFormat.Mode[]{LINES, LINE_STRIP, DEBUG_LINES, DEBUG_LINE_STRIP, TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN, QUADS};
      }
   }

   public static enum IndexType {
      BYTE(5121, 1),
      SHORT(5123, 2),
      INT(5125, 4);

      public final int asGLType;
      public final int bytes;

      private IndexType(int var3, int var4) {
         this.asGLType = var3;
         this.bytes = var4;
      }

      public static VertexFormat.IndexType least(int var0) {
         if ((var0 & -65536) != 0) {
            return INT;
         } else {
            return (var0 & '\uff00') != 0 ? SHORT : BYTE;
         }
      }

      // $FF: synthetic method
      private static VertexFormat.IndexType[] $values() {
         return new VertexFormat.IndexType[]{BYTE, SHORT, INT};
      }
   }
}
