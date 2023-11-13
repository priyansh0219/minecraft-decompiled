package net.minecraft.world.level.levelgen;

import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;

public class NoiseInterpolator {
   private double[][] slice0;
   private double[][] slice1;
   private final int cellCountY;
   private final int cellCountZ;
   private final int cellNoiseMinY;
   private final NoiseInterpolator.NoiseColumnFiller noiseColumnFiller;
   private double noise000;
   private double noise001;
   private double noise100;
   private double noise101;
   private double noise010;
   private double noise011;
   private double noise110;
   private double noise111;
   private double valueXZ00;
   private double valueXZ10;
   private double valueXZ01;
   private double valueXZ11;
   private double valueZ0;
   private double valueZ1;
   private final int firstCellXInChunk;
   private final int firstCellZInChunk;

   public NoiseInterpolator(int var1, int var2, int var3, ChunkPos var4, int var5, NoiseInterpolator.NoiseColumnFiller var6) {
      this.cellCountY = var2;
      this.cellCountZ = var3;
      this.cellNoiseMinY = var5;
      this.noiseColumnFiller = var6;
      this.slice0 = allocateSlice(var2, var3);
      this.slice1 = allocateSlice(var2, var3);
      this.firstCellXInChunk = var4.x * var1;
      this.firstCellZInChunk = var4.z * var3;
   }

   private static double[][] allocateSlice(int var0, int var1) {
      int var2 = var1 + 1;
      int var3 = var0 + 1;
      double[][] var4 = new double[var2][var3];

      for(int var5 = 0; var5 < var2; ++var5) {
         var4[var5] = new double[var3];
      }

      return var4;
   }

   public void initializeForFirstCellX() {
      this.fillSlice(this.slice0, this.firstCellXInChunk);
   }

   public void advanceCellX(int var1) {
      this.fillSlice(this.slice1, this.firstCellXInChunk + var1 + 1);
   }

   private void fillSlice(double[][] var1, int var2) {
      for(int var3 = 0; var3 < this.cellCountZ + 1; ++var3) {
         int var4 = this.firstCellZInChunk + var3;
         this.noiseColumnFiller.fillNoiseColumn(var1[var3], var2, var4, this.cellNoiseMinY, this.cellCountY);
      }

   }

   public void selectCellYZ(int var1, int var2) {
      this.noise000 = this.slice0[var2][var1];
      this.noise001 = this.slice0[var2 + 1][var1];
      this.noise100 = this.slice1[var2][var1];
      this.noise101 = this.slice1[var2 + 1][var1];
      this.noise010 = this.slice0[var2][var1 + 1];
      this.noise011 = this.slice0[var2 + 1][var1 + 1];
      this.noise110 = this.slice1[var2][var1 + 1];
      this.noise111 = this.slice1[var2 + 1][var1 + 1];
   }

   public void updateForY(double var1) {
      this.valueXZ00 = Mth.lerp(var1, this.noise000, this.noise010);
      this.valueXZ10 = Mth.lerp(var1, this.noise100, this.noise110);
      this.valueXZ01 = Mth.lerp(var1, this.noise001, this.noise011);
      this.valueXZ11 = Mth.lerp(var1, this.noise101, this.noise111);
   }

   public void updateForX(double var1) {
      this.valueZ0 = Mth.lerp(var1, this.valueXZ00, this.valueXZ10);
      this.valueZ1 = Mth.lerp(var1, this.valueXZ01, this.valueXZ11);
   }

   public double calculateValue(double var1) {
      return Mth.lerp(var1, this.valueZ0, this.valueZ1);
   }

   public void swapSlices() {
      double[][] var1 = this.slice0;
      this.slice0 = this.slice1;
      this.slice1 = var1;
   }

   @FunctionalInterface
   public interface NoiseColumnFiller {
      void fillNoiseColumn(double[] var1, int var2, int var3, int var4, int var5);
   }
}
