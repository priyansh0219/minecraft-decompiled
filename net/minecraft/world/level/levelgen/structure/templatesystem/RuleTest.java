package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import java.util.Random;
import net.minecraft.core.Registry;
import net.minecraft.world.level.block.state.BlockState;

public abstract class RuleTest {
   public static final Codec<RuleTest> CODEC;

   public abstract boolean test(BlockState var1, Random var2);

   protected abstract RuleTestType<?> getType();

   static {
      CODEC = Registry.RULE_TEST.dispatch("predicate_type", RuleTest::getType, RuleTestType::codec);
   }
}
