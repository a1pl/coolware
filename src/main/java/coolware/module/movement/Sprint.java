package coolware.module.movement;

import coolware.module.Category;
import coolware.module.Module;
import coolware.util.MovementFixUtil;
import org.lwjgl.input.Keyboard;

public class Sprint extends Module {
    public Sprint() {
        super("Sprint", "Automatically sprints when walking forward", Category.MOVEMENT, Keyboard.KEY_V);
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null) return;

        // When move-fix is active (KillAura silent rotations) or Scaffold is
        // controlling sprint, do NOT force-set sprinting here. The remapped-key
        // path already drives sprint through vanilla's own forward-check, so
        // calling setSprinting(true) on top of it makes the server observe sprint
        // at a yaw it never sent — the exact pattern anticheat flags as omni-sprint.
        if (MovementFixUtil.shouldMoveFix() || Scaffold.blocksSprint()) return;

        if (mc.thePlayer.moveForward > 0 && !mc.thePlayer.isSneaking() && !mc.thePlayer.isCollidedHorizontally) {
            mc.thePlayer.setSprinting(true);
        }
    }
}
