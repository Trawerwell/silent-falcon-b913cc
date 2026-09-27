package aethereal.render;

import aethereal.module.render.CustomSky;
import aethereal.core.Primordial;
import aethereal.config.ThemeInfo;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.Defines;
import net.minecraft.client.gl.ShaderProgram;
import net.minecraft.client.gl.ShaderProgramKey;
import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Draws into Minecraft's sky pass before terrain and entities are rendered. */
public final class CustomSkyRenderer {
    private static final Logger LOGGER = LogManager.getLogger(CustomSkyRenderer.class);
    private static final ShaderProgramKey PROGRAM = new ShaderProgramKey(
            Identifier.of("primordial", "core/custom_sky"), VertexFormats.POSITION_COLOR, Defines.EMPTY);
    private static final ShaderProgramKey[] SOURCE_PROGRAMS = {
            new ShaderProgramKey(Identifier.of("primordial", "core/custom_sky_caustic"), VertexFormats.POSITION_COLOR, Defines.EMPTY),
            new ShaderProgramKey(Identifier.of("primordial", "core/custom_sky_plasma"), VertexFormats.POSITION_COLOR, Defines.EMPTY)
    };
    private static boolean shaderFailed;

    private CustomSkyRenderer() {
    }

    public static boolean render(Camera camera, CustomSky settings) {
        if (shaderFailed) return false;
        var client = MinecraftClient.getInstance();
        if (camera == null || client.getFramebuffer() == null) return false;
        int width = client.getFramebuffer().textureWidth;
        int height = client.getFramebuffer().textureHeight;
        if (width <= 0 || height <= 0) return false;

        try {
            int style = settings.getStyleIndex();
            ShaderProgram shader = RenderSystem.setShader(style >= 5 ? SOURCE_PROGRAMS[style - 5] : PROGRAM);
            if (shader == null) return false;
            // Match the projection currently used for the world, including sprint FOV changes.
            float projectionY = Math.abs(RenderSystem.getProjectionMatrix().m11());
            float fov = projectionY > 0.0001f
                    ? (float) Math.toDegrees(2.0 * Math.atan(1.0 / projectionY))
                    : client.options.getFov().getValue().floatValue();
            float time = (float) ((System.nanoTime() / 1_000_000_000.0) % 4096.0);
            if (style >= 5) {
                int theme = style == 5 ? settings.getCausticColor()
                        : Primordial.getInstance().getModuleProcessor().o().a(ThemeInfo.PRIMARY).toIntColor();
                float red = ((theme >>> 16) & 255) / 255.0f;
                float green = ((theme >>> 8) & 255) / 255.0f;
                float blue = (theme & 255) / 255.0f;
                setVec2(shader, style == 6 ? "u_Resolution" : "uResolution", width, height);
                setVec2(shader, style == 6 ? "u_CameraDir" : "uCameraDir",
                        (float) Math.toRadians(-camera.getYaw()), (float) Math.toRadians(camera.getPitch()));
                setFloat(shader, style == 6 ? "u_Fov" : "uFov", fov);
                setFloat(shader, style == 6 ? "u_Scale" : "uScale", settings.getScale());
                if (style == 6) {
                    setFloat(shader, "u_Time", time * settings.getSpeed());
                    setVec4(shader, "u_Color", red * settings.getBrightness(), green * settings.getBrightness(),
                            blue * settings.getBrightness(), settings.getOpacity());
                    setVec4(shader, "u_Color2", blue * settings.getBrightness(), red * settings.getBrightness(),
                            green * settings.getBrightness(), settings.getOpacity());
                } else {
                    setFloat(shader, "uTime", time);
                    setFloat(shader, "uSpeed", settings.getSpeed());
                    setFloat(shader, "uIntensity", settings.getIntensity());
                    setFloat(shader, "uAlpha", settings.getOpacity());
                    setVec3(shader, "uColor", red * settings.getBrightness(), green * settings.getBrightness(),
                            blue * settings.getBrightness());
                }
            } else {
                Vector3f forward = camera.getRotation().transform(new Vector3f(0.0f, 0.0f, -1.0f));
                Vector3f right = camera.getRotation().transform(new Vector3f(1.0f, 0.0f, 0.0f));
                Vector3f up = camera.getRotation().transform(new Vector3f(0.0f, 1.0f, 0.0f));
                setVec3(shader, "Forward", forward);
                setVec3(shader, "Right", right);
                setVec3(shader, "Up", up);
                setFloat(shader, "Aspect", (float) width / height);
                setFloat(shader, "TanHalfFov", (float) Math.tan(Math.toRadians(fov * 0.5f)));
                setFloat(shader, "Time", time * settings.getSpeed());
                setFloat(shader, "Style", style);
                setFloat(shader, "Brightness", settings.getBrightness());
                setFloat(shader, "GlowStrength", settings.getGlowStrength());
                int[] nebula = settings.getNebulaColors();
                for (int i = 0; i < nebula.length; i++) {
                    int rgb = nebula[i];
                    setVec3(shader, "NebulaColor" + (i + 1), ((rgb >>> 16) & 255) / 255f,
                            ((rgb >>> 8) & 255) / 255f, (rgb & 255) / 255f);
                }
            }

            RenderSystem.disableDepthTest();
            RenderSystem.depthMask(false);
            RenderSystem.disableCull();
            if (style >= 5) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
            } else RenderSystem.disableBlend();
            try {
                BufferBuilder vertices = Tessellator.getInstance().begin(
                        VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR);
                vertices.vertex(-1.0f, -1.0f, 0.0f).color(0xFFFFFFFF);
                vertices.vertex(-1.0f, 1.0f, 0.0f).color(0xFFFFFFFF);
                vertices.vertex(1.0f, 1.0f, 0.0f).color(0xFFFFFFFF);
                vertices.vertex(1.0f, -1.0f, 0.0f).color(0xFFFFFFFF);
                BufferRenderer.drawWithGlobalProgram(vertices.end());
            } finally {
                RenderSystem.enableDepthTest();
                RenderSystem.depthMask(true);
                RenderSystem.enableCull();
                RenderSystem.disableBlend();
            }
            return true;
        } catch (RuntimeException error) {
            shaderFailed = true;
            LOGGER.warn("Custom Sky shader could not be rendered; using vanilla sky", error);
            return false;
        }
    }

    private static void setFloat(ShaderProgram shader, String name, float value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value);
    }

    private static void setVec3(ShaderProgram shader, String name, Vector3f value) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(value.x, value.y, value.z);
    }
    private static void setVec3(ShaderProgram shader, String name, float x, float y, float z) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y, z);
    }
    private static void setVec2(ShaderProgram shader, String name, float x, float y) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y);
    }
    private static void setVec4(ShaderProgram shader, String name, float x, float y, float z, float w) {
        Uniform uniform = shader.getUniform(name);
        if (uniform != null) uniform.set(x, y, z, w);
    }
}
