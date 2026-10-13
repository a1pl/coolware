package coolware.module.visuals.esp;

import coolware.util.math.MathInterpolation;
import coolware.util.render.ColorUtil;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.entity.player.EntityPlayer;
import org.lwjgl.opengl.GL11;

public class PlayerSkeleton {
    private static final Map<EntityPlayer, float[][]> limbAngles = new ConcurrentHashMap<EntityPlayer, float[][]>();
    private static final float RAD_TO_DEG = 180.0F / (float)Math.PI;
    private final Minecraft mc = Minecraft.getMinecraft();
    private final ESPColor espColor;

    public PlayerSkeleton(ESPColor espColor) {
        this.espColor = espColor;
    }

    public static void updateModel(EntityPlayer player, ModelBiped modelBiped) {
        if (player == null || modelBiped == null) return;
        limbAngles.put(
            player,
            new float[][]{
                {modelBiped.bipedHead.rotateAngleX, modelBiped.bipedHead.rotateAngleY, modelBiped.bipedHead.rotateAngleZ},
                {modelBiped.bipedRightArm.rotateAngleX, modelBiped.bipedRightArm.rotateAngleY, modelBiped.bipedRightArm.rotateAngleZ},
                {modelBiped.bipedLeftArm.rotateAngleX, modelBiped.bipedLeftArm.rotateAngleY, modelBiped.bipedLeftArm.rotateAngleZ},
                {modelBiped.bipedRightLeg.rotateAngleX, modelBiped.bipedRightLeg.rotateAngleY, modelBiped.bipedRightLeg.rotateAngleZ},
                {modelBiped.bipedLeftLeg.rotateAngleX, modelBiped.bipedLeftLeg.rotateAngleY, modelBiped.bipedLeftLeg.rotateAngleZ}
            }
        );
    }

    public void render3D(float partialTicks, float lineWidth, boolean whiteColor) {
        if (mc.theWorld == null) return;
        GL11.glPushMatrix();
        begin(lineWidth, whiteColor);

        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player != null && !player.isDead) {
                renderSkeleton(player, partialTicks);
            }
        }

        end();
        GL11.glPopMatrix();
    }

    private void begin(float lineWidth, boolean whiteColor) {
        GL11.glLineWidth(lineWidth);
        GL11.glEnable(3042);
        GL11.glBlendFunc(770, 771);
        GL11.glEnable(2848);
        ColorUtil.glColor(whiteColor ? Color.WHITE : this.espColor.getColor());
        GL11.glDisable(2929);
        GL11.glDisable(3553);
        GL11.glDepthMask(false);
    }

    private void end() {
        GL11.glDepthMask(true);
        GL11.glDisable(3042);
        GL11.glEnable(3553);
        GL11.glDisable(2848);
        GL11.glEnable(2929);
    }

    private void renderSkeleton(EntityPlayer player, float partialTicks) {
        float[][] afloat = limbAngles.get(player);
        if (afloat != null) {
            GL11.glPushMatrix();
            float f = (float)(MathInterpolation.interpolate(player.posX, player.prevPosX, partialTicks) - mc.getRenderManager().renderPosX);
            float f1 = (float)(MathInterpolation.interpolate(player.posY, player.prevPosY, partialTicks) - mc.getRenderManager().renderPosY);
            float f2 = (float)(MathInterpolation.interpolate(player.posZ, player.prevPosZ, partialTicks) - mc.getRenderManager().renderPosZ);
            GL11.glTranslated(f, f1, f2);
            boolean flag = player.isSneaking();
            float rotationYawHead = player.rotationYawHead;
            float renderYawOffset = player.renderYawOffset;
            float prevRenderYawOffset = player.prevRenderYawOffset;
            float f6 = MathInterpolation.interpolate(renderYawOffset, prevRenderYawOffset, partialTicks);
            float f7 = flag ? 0.6F : 0.75F;
            GL11.glRotatef(-f6, 0.0F, 1.0F, 0.0F);
            GL11.glTranslatef(0.0F, 0.0F, flag ? -0.235F : 0.0F);
            drawSkeleton(afloat, f7, flag, f6, rotationYawHead);
            GL11.glPopMatrix();
        }
    }

    private void drawSkeleton(float[][] var1, float var2, boolean var3, float var4, float var5) {
        for (int i = 1; i <= 2; i++) {
            drawLeg(var1[i + 2], i == 1 ? -0.125F : 0.125F, var2);
        }

        GL11.glTranslatef(0.0F, 0.0F, var3 ? 0.25F : 0.0F);
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, var3 ? -0.05F : 0.0F, var3 ? -0.01725F : 0.0F);

        for (int j = 1; j <= 2; j++) {
            drawArm(var1[j], j == 1 ? -0.375F : 0.375F, var2 + 0.55F);
        }

        GL11.glRotatef(var4 - var5, 0.0F, 1.0F, 0.0F);
        drawHead(var1[0], var2);
        GL11.glPopMatrix();
        drawSpineAndPelvis(var2);
    }

    private void drawLeg(float[] var1, float var2, float var3) {
        GL11.glPushMatrix();
        GL11.glTranslatef(var2, var3, 0.0F);
        rotateLimb(var1);
        GL11.glBegin(3);
        GL11.glVertex3i(0, 0, 0);
        GL11.glVertex3f(0.0F, -var3, 0.0F);
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void drawArm(float[] var1, float var2, float var3) {
        GL11.glPushMatrix();
        GL11.glTranslatef(var2, var3, 0.0F);
        rotateLimb(var1);
        GL11.glBegin(3);
        GL11.glVertex3i(0, 0, 0);
        GL11.glVertex3f(0.0F, -0.5F, 0.0F);
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void drawHead(float[] var1, float var2) {
        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, var2 + 0.55F, 0.0F);
        rotateLimb(var1);
        GL11.glBegin(3);
        GL11.glVertex3i(0, 0, 0);
        GL11.glVertex3f(0.0F, 0.3F, 0.0F);
        GL11.glEnd();
        GL11.glPopMatrix();
    }

    private void rotateLimb(float[] var1) {
        if (var1[0] != 0.0F) {
            GL11.glRotatef(var1[0] * RAD_TO_DEG, 1.0F, 0.0F, 0.0F);
        }

        if (var1[1] != 0.0F) {
            GL11.glRotatef(var1[1] * RAD_TO_DEG, 0.0F, 1.0F, 0.0F);
        }

        if (var1[2] != 0.0F) {
            GL11.glRotatef(var1[2] * RAD_TO_DEG, 0.0F, 0.0F, 1.0F);
        }
    }

    private void drawSpineAndPelvis(float var1) {
        GL11.glPushMatrix();
        GL11.glTranslated(0.0, var1, 0.0);
        GL11.glBegin(3);
        GL11.glVertex3f(-0.125F, 0.0F, 0.0F);
        GL11.glVertex3f(0.125F, 0.0F, 0.0F);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, var1, 0.0F);
        GL11.glBegin(3);
        GL11.glVertex3i(0, 0, 0);
        GL11.glVertex3f(0.0F, 0.55F, 0.0F);
        GL11.glEnd();
        GL11.glPopMatrix();

        GL11.glPushMatrix();
        GL11.glTranslatef(0.0F, var1 + 0.55F, 0.0F);
        GL11.glBegin(3);
        GL11.glVertex3f(-0.375F, 0.0F, 0.0F);
        GL11.glVertex3f(0.375F, 0.0F, 0.0F);
        GL11.glEnd();
        GL11.glPopMatrix();
    }
}
