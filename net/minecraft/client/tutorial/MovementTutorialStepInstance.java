package net.minecraft.client.tutorial;

import net.minecraft.client.gui.components.toasts.TutorialToast;
import net.minecraft.client.player.Input;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TranslatableComponent;

public class MovementTutorialStepInstance implements TutorialStepInstance {
   private static final int MINIMUM_TIME_MOVED = 40;
   private static final int MINIMUM_TIME_LOOKED = 40;
   private static final int MOVE_HINT_DELAY = 100;
   private static final int LOOK_HINT_DELAY = 20;
   private static final int INCOMPLETE = -1;
   private static final Component MOVE_TITLE = new TranslatableComponent("tutorial.move.title", new Object[]{Tutorial.key("forward"), Tutorial.key("left"), Tutorial.key("back"), Tutorial.key("right")});
   private static final Component MOVE_DESCRIPTION = new TranslatableComponent("tutorial.move.description", new Object[]{Tutorial.key("jump")});
   private static final Component LOOK_TITLE = new TranslatableComponent("tutorial.look.title");
   private static final Component LOOK_DESCRIPTION = new TranslatableComponent("tutorial.look.description");
   private final Tutorial tutorial;
   private TutorialToast moveToast;
   private TutorialToast lookToast;
   private int timeWaiting;
   private int timeMoved;
   private int timeLooked;
   private boolean moved;
   private boolean turned;
   private int moveCompleted = -1;
   private int lookCompleted = -1;

   public MovementTutorialStepInstance(Tutorial var1) {
      this.tutorial = var1;
   }

   public void tick() {
      ++this.timeWaiting;
      if (this.moved) {
         ++this.timeMoved;
         this.moved = false;
      }

      if (this.turned) {
         ++this.timeLooked;
         this.turned = false;
      }

      if (this.moveCompleted == -1 && this.timeMoved > 40) {
         if (this.moveToast != null) {
            this.moveToast.hide();
            this.moveToast = null;
         }

         this.moveCompleted = this.timeWaiting;
      }

      if (this.lookCompleted == -1 && this.timeLooked > 40) {
         if (this.lookToast != null) {
            this.lookToast.hide();
            this.lookToast = null;
         }

         this.lookCompleted = this.timeWaiting;
      }

      if (this.moveCompleted != -1 && this.lookCompleted != -1) {
         if (this.tutorial.isSurvival()) {
            this.tutorial.setStep(TutorialSteps.FIND_TREE);
         } else {
            this.tutorial.setStep(TutorialSteps.NONE);
         }
      }

      if (this.moveToast != null) {
         this.moveToast.updateProgress((float)this.timeMoved / 40.0F);
      }

      if (this.lookToast != null) {
         this.lookToast.updateProgress((float)this.timeLooked / 40.0F);
      }

      if (this.timeWaiting >= 100) {
         if (this.moveCompleted == -1 && this.moveToast == null) {
            this.moveToast = new TutorialToast(TutorialToast.Icons.MOVEMENT_KEYS, MOVE_TITLE, MOVE_DESCRIPTION, true);
            this.tutorial.getMinecraft().getToasts().addToast(this.moveToast);
         } else if (this.moveCompleted != -1 && this.timeWaiting - this.moveCompleted >= 20 && this.lookCompleted == -1 && this.lookToast == null) {
            this.lookToast = new TutorialToast(TutorialToast.Icons.MOUSE, LOOK_TITLE, LOOK_DESCRIPTION, true);
            this.tutorial.getMinecraft().getToasts().addToast(this.lookToast);
         }
      }

   }

   public void clear() {
      if (this.moveToast != null) {
         this.moveToast.hide();
         this.moveToast = null;
      }

      if (this.lookToast != null) {
         this.lookToast.hide();
         this.lookToast = null;
      }

   }

   public void onInput(Input var1) {
      if (var1.up || var1.down || var1.left || var1.right || var1.jumping) {
         this.moved = true;
      }

   }

   public void onMouse(double var1, double var3) {
      if (Math.abs(var1) > 0.01D || Math.abs(var3) > 0.01D) {
         this.turned = true;
      }

   }
}
