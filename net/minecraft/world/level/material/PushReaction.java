package net.minecraft.world.level.material;

public enum PushReaction {
   NORMAL,
   DESTROY,
   BLOCK,
   IGNORE,
   PUSH_ONLY;

   // $FF: synthetic method
   private static PushReaction[] $values() {
      return new PushReaction[]{NORMAL, DESTROY, BLOCK, IGNORE, PUSH_ONLY};
   }
}
