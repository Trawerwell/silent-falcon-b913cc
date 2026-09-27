package aethereal.module.render;

import aethereal.core.Category;
import aethereal.core.EventTarget;
import aethereal.core.Module;
import aethereal.core.ModuleRegister;
import aethereal.core.Primordial;
import aethereal.event.HeadFeatureEvent;
import aethereal.render.ColorUtil;
import aethereal.setting.BooleanSetting;
import aethereal.setting.ColorSetting;
import aethereal.setting.ModeSetting;
import aethereal.setting.SliderSetting;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

@ModuleRegister(name = "China Hat", description = "Настраиваемая декоративная шляпа", category = Category.Render)
public class ChinaHat extends Module {
    private static final int SEGMENTS = 160;
    // Depth testing keeps the hat in the scene and depth writes hide its far side.
    private static final RenderLayer HAT_LAYER = RenderLayer.of("primordial_china_hat",
            VertexFormats.POSITION_COLOR, VertexFormat.DrawMode.QUADS, 1536, false, true,
            RenderLayer.MultiPhaseParameters.builder()
                    .program(RenderPhase.POSITION_COLOR_PROGRAM)
                    .transparency(RenderPhase.TRANSLUCENT_TRANSPARENCY)
                    .depthTest(RenderPhase.LEQUAL_DEPTH_TEST)
                    // Write depth for the hat surface itself: without this, its far
                    // side blends through the near side and looks like a see-through shell.
                    .writeMaskState(RenderPhase.ALL_MASK)
                    .cull(RenderPhase.DISABLE_CULLING)
                    .build(false));
    private final BooleanSetting gradient = new BooleanSetting("Градиент", true);
    private final ModeSetting colorCount = new ModeSetting("Цветов градиента", "3", "1", "2", "3").a(gradient::c);
    private final ColorSetting color = new ColorSetting("Цвет визуализации шляпы", ColorUtil.convertToARGB(96, 153, 255, 255));
    private final ColorSetting color2 = new ColorSetting("Второй цвет шляпы", 0xFFAD72FF).a(() -> gradient.c() && !colorCount.l("1"));
    private final ColorSetting color3 = new ColorSetting("Третий цвет шляпы", 0xFF6CE4D8).a(() -> gradient.c() && colorCount.l("3"));
    private final SliderSetting radius = new SliderSetting("Ширина", 0.60f, 0.40f, 0.90f, 0.01f);
    private final SliderSetting height = new SliderSetting("Высота", 0.30f, 0.10f, 0.60f, 0.01f);
    private final SliderSetting opacity = new SliderSetting("Непрозрачность", 0.60f, 0.10f, 1.0f, 0.05f);
    private final BooleanSetting tip = new BooleanSetting("Светлая верхушка", true);
    private final BooleanSetting radar = new BooleanSetting("Радар", false);
    private final BooleanSetting rotate = new BooleanSetting("Вращение шляпы", false);
    private final BooleanSetting flow = new BooleanSetting("Перелив градиента", false).a(gradient::c);

    public ChinaHat() { a(color, gradient, colorCount, color2, color3, radius, height, opacity, tip, radar, rotate, flow); }

    @EventTarget
    public void onHead(HeadFeatureEvent event) {
        if (!(event.getModel() instanceof BipedEntityModel<?> model)) return;
        PlayerEntity player = event.getPlayer();
        boolean friend = Primordial.getInstance().getModuleProcessor().e().d(player.getName().getString());
        if (player != mc.player && !friend) return;

        int base = player == mc.player ? color.c() : ColorUtil.convertToARGB(80, 220, 135, 220);
        int second = player == mc.player ? color2.c() : base;
        int third = player == mc.player ? color3.c() : base;
        double time = System.nanoTime() * 1.0e-9;
        float r = radius.c(), h = height.c();
        float offset = player.getInventory().getStack(39).isEmpty() ? 0.45f : 0.48f;
        MatrixStack matrices = event.getMatrixStack();
        matrices.push();
        model.head.rotate(matrices);
        matrices.translate(0.0f, -offset, 0.0f);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180.0f));
        if (rotate.c()) matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) (time * 18.0 % 360.0)));
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        VertexConsumer buffer = event.getVertexConsumerProvider().getBuffer(HAT_LAYER);
        float phase = flow.c() ? (float) (time * 0.12) : 0.0f;
        float radarPosition = (float) ((time * 0.12) % 1.0);

        for (int i = 0; i < SEGMENTS; i++) {
            float a = (float) (i * Math.PI * 2.0 / SEGMENTS);
            float b = (float) ((i + 1) * Math.PI * 2.0 / SEGMENTS);
            float ax = (float) Math.sin(a), az = (float) Math.cos(a);
            float bx = (float) Math.sin(b), bz = (float) Math.cos(b);
            int side = alpha(palette(base, second, third, i / (float) SEGMENTS + phase), opacity.c());
            int next = alpha(palette(base, second, third, (i + 1f) / SEGMENTS + phase), opacity.c());
            int top = tip.c() ? ColorUtil.lerpColor(side, (side & 0xFF000000) | 0x00FAFCFF, 0.32f) : side;
            int topNext = tip.c() ? ColorUtil.lerpColor(next, (next & 0xFF000000) | 0x00FAFCFF, 0.32f) : next;
            if (radar.c()) {
                float left = radarIntensity(radarPosition, i / (float) SEGMENTS);
                float right = radarIntensity(radarPosition, (i + 1f) / SEGMENTS);
                // The ray samples the stationary gradient, even when the regular
                // gradient is flowing. Its trail fades over a complete revolution.
                int staticLeft = palette(base, second, third, i / (float) SEGMENTS);
                int staticRight = palette(base, second, third, (i + 1f) / SEGMENTS);
                side = darkRadarColor(side, staticLeft, left * 0.88f);
                next = darkRadarColor(next, staticRight, right * 0.88f);
                top = darkRadarColor(top, staticLeft, left * 0.55f);
                topNext = darkRadarColor(topNext, staticRight, right * 0.55f);
            }
            quad(buffer, matrix, 0,h,0, ax*r,0,az*r, bx*r,0,bz*r, 0,h,0,
                    top,side,next,topNext);
        }
        matrices.pop();
    }

    private int palette(int first, int second, int third, float position) {
        if (!gradient.c()) return first;
        float p = position - (float) Math.floor(position);
        if (colorCount.l("1")) return first;
        if (colorCount.l("2")) return p < 0.5f
                ? ColorUtil.lerpColor(first, second, p * 2f)
                : ColorUtil.lerpColor(second, first, p * 2f - 1f);
        if (p < 1f / 3f) return ColorUtil.lerpColor(first, second, p * 3f);
        if (p < 2f / 3f) return ColorUtil.lerpColor(second, third, p * 3f - 1f);
        return ColorUtil.lerpColor(third, first, p * 3f - 2f);
    }

    private static int alpha(int color, float factor) {
        int a = Math.max(0, Math.min(255, Math.round(((color >>> 24) & 255) * factor)));
        return (color & 0x00FFFFFF) | (a << 24);
    }

    private static float radarIntensity(float sweep, float position) {
        float behind = sweep - position;
        behind -= (float) Math.floor(behind);
        float tail = 0.80f * (float) Math.pow(1.0f - behind, 0.78);
        float ray = 0.20f * (float) Math.exp(-Math.pow(behind / 0.015f, 2.0));
        return Math.min(1.0f, tail + ray);
    }

    private static int darkRadarColor(int color, int staticGradientColor, float intensity) {
        int staticOpaque = 0xFF000000 | (staticGradientColor & 0x00FFFFFF);
        int darkGradient = ColorUtil.lerpColor(staticOpaque, 0xFF050913, 0.62f);
        int mixed = ColorUtil.lerpColor(0xFF000000 | (color & 0x00FFFFFF), darkGradient,
                Math.max(0.0f, Math.min(1.0f, intensity)));
        return (color & 0xFF000000) | (mixed & 0x00FFFFFF);
    }

    private static void quad(VertexConsumer buffer, Matrix4f matrix,
                             float x1,float y1,float z1,float x2,float y2,float z2,
                             float x3,float y3,float z3,float x4,float y4,float z4,
                             int c1,int c2,int c3,int c4) {
        buffer.vertex(matrix,x1,y1,z1).color(c1);
        buffer.vertex(matrix,x2,y2,z2).color(c2);
        buffer.vertex(matrix,x3,y3,z3).color(c3);
        buffer.vertex(matrix,x4,y4,z4).color(c4);
    }
}
