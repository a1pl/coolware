package coolware.module.combat;

import coolware.Coolware;
import coolware.module.Category;
import coolware.module.Module;
import coolware.setting.BooleanSetting;
import coolware.setting.ModeSetting;
import coolware.setting.NumberSetting;
import coolware.util.BlinkUtil;
import coolware.util.RotationUtil;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.entity.Entity;
import net.minecraft.item.EnumAction;
import net.minecraft.network.play.server.S12PacketEntityVelocity;
import net.minecraft.network.play.server.S19PacketEntityStatus;
import net.minecraft.network.play.server.S27PacketExplosion;
import net.minecraft.util.MovementInput;
import net.minecraft.util.Vec3;
import org.lwjgl.input.Keyboard;

public class Velocity extends Module {

    public static BooleanSetting zeroCheck;
    public static BooleanSetting ignoreTeleport;
    public static BooleanSetting ignoreExplosions;
    public static ModeSetting velocityMode;
    public static NumberSetting xMulti;
    public static NumberSetting yMulti;
    public static NumberSetting zMulti;
    public static NumberSetting maxDelayTicks;
    public static BooleanSetting delayOutgoing;
    public static ModeSetting jumpResetMode;
    public static NumberSetting sprintFOV;

    private static boolean shouldJump = false;
    private static boolean shouldDelay = false;
    private static int blinkStartTick = -1;
    private static Vec3 lastVelocity = null;
    private static boolean isDamage = false;
    private static boolean isDelayOutgoing = false;
    private static int ticksExisted = 0;

    public Velocity() {
        super("Velocity", "Reduces or eliminates knockback taken.", Category.COMBAT, Keyboard.KEY_NONE);

        zeroCheck = new BooleanSetting("0 Check", this, true);
        ignoreTeleport = new BooleanSetting("Ignore Teleport", this, true);
        ignoreExplosions = new BooleanSetting("Ignore Explosions", this, true);

        velocityMode = new ModeSetting("Mode", this, "Dynamic", "Vanilla", "Keep", "Delay", "Jump_Reset", "Dynamic");

        xMulti = new NumberSetting("Velocity X", this, 1.0, -2.0, 2.0, 0.05);
        yMulti = new NumberSetting("Velocity Y", this, 1.0, -2.0, 2.0, 0.05);
        zMulti = new NumberSetting("Velocity Z", this, 1.0, -2.0, 2.0, 0.05);

        maxDelayTicks = new NumberSetting("Max Delay Ticks", this, 5.0, 1.0, 20.0, 1.0);
        delayOutgoing = new BooleanSetting("Delay Outgoing", this, false);

        jumpResetMode = new ModeSetting("Jump", this, "Sprint_Only", "Force_Sprint", "Sprint_Only", "Normal");
        sprintFOV = new NumberSetting("Sprint FOV", this, 180.0, 1.0, 180.0, 1.0);

        addSetting(zeroCheck);
        addSetting(ignoreTeleport);
        addSetting(ignoreExplosions);
        addSetting(velocityMode);
        addSetting(xMulti);
        addSetting(yMulti);
        addSetting(zMulti);
        addSetting(maxDelayTicks);
        addSetting(delayOutgoing);
        addSetting(jumpResetMode);
        addSetting(sprintFOV);
    }

    public static boolean handleVelocityPacket(S12PacketEntityVelocity packet) {
        Velocity velocity = getModule();
        if (velocity == null || !velocity.isToggled() || mc.thePlayer == null || mc.theWorld == null) {
            return false;
        }

        if (packet.getEntityID() != mc.thePlayer.getEntityId()) {
            return false;
        }

        if (!isDamage) {
            isDamage = true;
            return false;
        }
        isDamage = !ignoreTeleport.isEnabled();

        Vec3 originalVelocity = new Vec3(
                (double) packet.getMotionX() / 8000.0D,
                (double) packet.getMotionY() / 8000.0D,
                (double) packet.getMotionZ() / 8000.0D
        );

        if (Math.abs(originalVelocity.xCoord) <= 0.1 && Math.abs(originalVelocity.zCoord) <= 0.1 && zeroCheck.isEnabled()) {
            return false;
        }

        if (!shouldDelay) lastVelocity = originalVelocity;

        String mode = velocityMode.getMode();

        if ("Vanilla".equalsIgnoreCase(mode)) {
            packet.setMotionX((int) (originalVelocity.xCoord * xMulti.getValue() * 8000.0D));
            packet.setMotionY((int) (originalVelocity.yCoord * yMulti.getValue() * 8000.0D));
            packet.setMotionZ((int) (originalVelocity.zCoord * zMulti.getValue() * 8000.0D));
            return false; // let Minecraft process packet with modified velocity
        } else if ("Keep".equalsIgnoreCase(mode)) {
            double newX = originalVelocity.xCoord * xMulti.getValue() + mc.thePlayer.motionX * (1.0 - xMulti.getValue());
            double newY = originalVelocity.yCoord * yMulti.getValue() + mc.thePlayer.motionY * (1.0 - yMulti.getValue());
            double newZ = originalVelocity.zCoord * zMulti.getValue() + mc.thePlayer.motionZ * (1.0 - zMulti.getValue());
            packet.setMotionX((int) (newX * 8000.0D));
            packet.setMotionY((int) (newY * 8000.0D));
            packet.setMotionZ((int) (newZ * 8000.0D));
            return false;
        } else if ("Jump_Reset".equalsIgnoreCase(mode) || "Dynamic".equalsIgnoreCase(mode)) {
            String jMode = jumpResetMode.getMode();
            if (!mc.thePlayer.isSprinting() && "Sprint_Only".equalsIgnoreCase(jMode)) {
                return false;
            }

            if (mc.thePlayer.onGround || "Jump_Reset".equalsIgnoreCase(mode)) {
                shouldJump |= mc.thePlayer.onGround;
                return false;
            }

            // Fallthrough to delay logic for in-air velocity in Dynamic mode
            if (shouldDelay) return false;

            BlinkUtil.pushBlink(false, true, packet);

            if (delayOutgoing.isEnabled()) {
                BlinkUtil.pushBlink(true, false, null);
                isDelayOutgoing = true;
            }

            shouldDelay = true;
            blinkStartTick = ticksExisted;
            return true; // Cancel immediate processing, blink it
        } else if ("Delay".equalsIgnoreCase(mode)) {
            if (shouldDelay) return false;

            BlinkUtil.pushBlink(false, true, packet);

            if (delayOutgoing.isEnabled()) {
                BlinkUtil.pushBlink(true, false, null);
                isDelayOutgoing = true;
            }

            shouldDelay = true;
            blinkStartTick = ticksExisted;
            return true;
        }

        return false;
    }

    public static boolean handleExplosionPacket(S27PacketExplosion packet) {
        Velocity velocity = getModule();
        if (velocity == null || !velocity.isToggled() || mc.thePlayer == null) {
            return false;
        }

        if (ignoreExplosions.isEnabled()) {
            isDamage = false;
            return true; // Ignore knockback from explosion completely
        }

        return false;
    }

    public static void handleEntityStatusPacket(S19PacketEntityStatus packet) {
        Velocity velocity = getModule();
        if (velocity == null || !velocity.isToggled() || mc.thePlayer == null || mc.theWorld == null) {
            return;
        }

        Entity entity = packet.getEntity(mc.theWorld);
        if (entity != null && entity.getEntityId() == mc.thePlayer.getEntityId()) {
            isDamage = true;
        }
    }

    @Override
    public void onUpdate() {
        if (mc.thePlayer == null || mc.theWorld == null) return;
        ticksExisted++;

        // Rotation hook for Force_Sprint Jump Reset
        if (shouldJump && lastVelocity != null && "Force_Sprint".equalsIgnoreCase(jumpResetMode.getMode()) && isWithinSprintFOV()) {
            float targetYaw = getYawFromVelocity(lastVelocity.xCoord, lastVelocity.zCoord);
            RotationUtil.setSilentRotation(targetYaw, RotationUtil.serverPitch);

            if (mc.thePlayer.getHeldItem() != null && (mc.thePlayer.getHeldItem().getItemUseAction() == EnumAction.BLOCK || !mc.thePlayer.isUsingItem())) {
                KeyBinding.setKeyBindState(mc.gameSettings.keyBindUseItem.getKeyCode(), false);
            }
        }

        // Finish Dynamic / Delay Blink
        if (BlinkUtil.isBlinking(true, true) && shouldStopBlink()) {
            BlinkUtil.popBlink(isDelayOutgoing, true);
            isDelayOutgoing = false;
            shouldJump |= "Dynamic".equalsIgnoreCase(velocityMode.getMode()) && mc.thePlayer.onGround;
            resetVelocity();
        }
    }

    public static void onMovementInput(MovementInput movementInput) {
        Velocity velocity = getModule();
        if (velocity == null || !velocity.isToggled() || mc.thePlayer == null || movementInput == null) {
            return;
        }

        if (!shouldJump) return;

        if ("Force_Sprint".equalsIgnoreCase(jumpResetMode.getMode()) && isWithinSprintFOV()) {
            movementInput.moveForward = movementInput.sneak ? 0.3F : 1.0F;
            movementInput.moveStrafe = 0.0F;
            KeyBinding.setKeyBindState(mc.gameSettings.keyBindSprint.getKeyCode(), true);
        }

        movementInput.jump |= mc.thePlayer.onGround;
        shouldJump = false;
    }

    private static boolean isWithinSprintFOV() {
        if (lastVelocity == null || mc.thePlayer == null) return false;

        float yaw = getYawFromVelocity(lastVelocity.xCoord, lastVelocity.zCoord);
        return 180.0F - Math.abs(((mc.thePlayer.rotationYaw - yaw) % 360.0F) - 180.0F) <= (float) sprintFOV.getValue();
    }

    private static float getYawFromVelocity(double x, double z) {
        float yaw = (float) (Math.toDegrees(Math.atan2(x + z, x - z)) + 45.0D);
        return yaw > 180.0F ? yaw - 360.0F : yaw;
    }

    private static boolean shouldStopBlink() {
        return blinkStartTick != -1 && shouldDelay && (ticksExisted - blinkStartTick > (int) maxDelayTicks.getValue() || (mc.thePlayer.onGround && "Dynamic".equalsIgnoreCase(velocityMode.getMode())));
    }

    private static void resetVelocity() {
        blinkStartTick = -1;
        shouldDelay = false;
    }

    private static Velocity getModule() {
        if (Coolware.getInstance() != null && Coolware.getInstance().getModuleManager() != null) {
            return Coolware.getInstance().getModuleManager().getModule(Velocity.class);
        }
        return null;
    }

    @Override
    public void onEnable() {
        shouldJump = false;
        shouldDelay = false;
        blinkStartTick = -1;
        lastVelocity = null;
        isDamage = false;
        isDelayOutgoing = false;
    }

    @Override
    public void onDisable() {
        if (blinkStartTick != -1) {
            BlinkUtil.popBlink(isDelayOutgoing, true);
            isDelayOutgoing = false;
            resetVelocity();
        }
        shouldJump = false;
    }
}
