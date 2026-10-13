package coolware.util;

import coolware.module.movement.Scaffold;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovementInput;

public class MovementFixUtil {
    private static final Minecraft mc = Minecraft.getMinecraft();
    private static float yawDeficit = 0.0F;

    public static boolean shouldMoveFix() {
        if (mc.thePlayer == null || !RotationUtil.silentActive) return false;
        return Scaffold.wantsMoveFix();
    }

    // Scaffold walks the keys relative to its look, the way the C++ client's Silent correction does.
    private static String mode() {
        return "Straight";
    }

    /** The keys are being remapped to the silent look this tick. */
    public static boolean remaps() {
        return shouldMoveFix() && !"Vanilla".equalsIgnoreCase(mode());
    }

    /**
     * The yaw the player's movement is worked out with (Entity.moveFlying,
     * EntityLivingBase.jump's sprint boost). While the keys are remapped to the
     * silent look, the movement has to use that look too -- the server
     * simulates it with the yaw it was sent -- or the remapped keys walk the
     * wrong way: W with the look behind you became S, walking backwards.
     */
    public static float movementYaw(Entity entity, float ownYaw) {
        if (entity == null || entity != mc.thePlayer || !remaps()) return ownYaw;
        return RotationUtil.serverYaw;
    }

    /**
     * Sprinting is off while Scaffold holds it (Sprint off), and while a silent
     * look is sent that the keys aren't remapped to: the sprint would point
     * away from the look the server has. With the remap, vanilla's own rule
     * (sprint only while moving forward) already keeps it within the look.
     *
     * Note: the external Sprint module is guarded separately in Sprint.onUpdate()
     * by checking shouldMoveFix(), so it never races move-fix via this path.
     */
    public static boolean sprintBlocked() {
        return Scaffold.blocksSprint() || (RotationUtil.silentActive && !remaps());
    }

    public static void fixMovement(MovementInput movementInput) {
        if (!remaps()) {
            yawDeficit = 0.0F;
            return;
        }

        String mode = mode();

        float speed = Math.max(Math.abs(movementInput.moveForward), Math.abs(movementInput.moveStrafe));
        if (speed == 0.0F) return;

        MovementDirection fixedMovementDirection = getMovementDirection(movementInput, mode);

        movementInput.moveForward = fixedMovementDirection.forward ? speed : (fixedMovementDirection.back ? -speed : 0.0F);
        movementInput.moveStrafe = fixedMovementDirection.left ? speed : (fixedMovementDirection.right ? -speed : 0.0F);
    }

    private static MovementDirection getMovementDirection(MovementInput movementInput, String mode) {
        float serverYaw = RotationUtil.serverYaw;
        float yawDifference = mc.thePlayer.rotationYaw - serverYaw;

        if ("Straight".equalsIgnoreCase(mode)) {
            float yawDeficitAdded = MathUtil.toNearest(yawDifference + yawDeficit, 45.0F) - yawDifference;
            yawDifference += yawDeficitAdded;
            yawDeficit -= yawDeficitAdded;
            yawDeficit += MathUtil.toNearest(yawDifference, 45.0F) - yawDifference;
        }
        // Smooth mode: the nearest direction as it is (rounded below).

        // To the nearest of the eight directions. Truncating, a difference that
        // came out a hair under a multiple of 45 (floating point) dropped a whole
        // direction.
        int yawOrdinal = Math.floorMod(Math.round(yawDifference / 45.0F), MovementDirection.values().length);

        MovementDirection inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.EAST : MovementDirection.WEST;
        if (movementInput.moveForward > 0.0F) {
            inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.NORTH_EAST : (movementInput.moveStrafe > 0.0F ? MovementDirection.NORTH_WEST : MovementDirection.NORTH);
        }
        if (movementInput.moveForward < 0.0F) {
            inputMoveDirection = movementInput.moveStrafe < 0.0F ? MovementDirection.SOUTH_EAST : (movementInput.moveStrafe > 0.0F ? MovementDirection.SOUTH_WEST : MovementDirection.SOUTH);
        }

        return MovementDirection.values()[Math.floorMod(inputMoveDirection.ordinal() + yawOrdinal, MovementDirection.values().length)];
    }

    private enum MovementDirection {
        NORTH       (true,  false, false, false),
        NORTH_EAST  (true,  true,  false, false),
        EAST        (false, true,  false, false),
        SOUTH_EAST  (false, true,  true,  false),
        SOUTH       (false, false, true,  false),
        SOUTH_WEST  (false, false, true,  true ),
        WEST        (false, false, false, true ),
        NORTH_WEST  (true,  false, false, true );

        final boolean forward, right, back, left;

        MovementDirection(boolean forward, boolean right, boolean back, boolean left) {
            this.forward = forward;
            this.right = right;
            this.back = back;
            this.left = left;
        }
    }
}
