/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.wyvern;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import primordial.util.media.MusicInfo;
import primordial.util.render.RoundedStencilShader;
import primordial.util.render.StencilUtil;

public final class WTexture {
    private static final Map<String, Integer> CACHE = new HashMap<String, Integer>();
    private static final Map<String, Boolean> FAILED = new HashMap<String, Boolean>();
    private static int coverTexture = -1;
    private static int coverVersion = -1;
    private static boolean coverBroken;
    private static long coverRetryAt;
    private static int coverAttempts;

    private WTexture() {
    }

    public static boolean bind(ResourceLocation resourceLocation, int n) {
        if (resourceLocation == null) {
            return false;
        }
        String string = resourceLocation.toString();
        if (Boolean.TRUE.equals(FAILED.get(string))) {
            return false;
        }
        Integer n2 = CACHE.get(string);
        if (n2 == null) {
            n2 = WTexture.upload(resourceLocation, n);
            if (n2 == null) {
                FAILED.put(string, Boolean.TRUE);
                return false;
            }
            CACHE.put(string, n2);
        }
        GlStateManager.bindTexture(n2);
        GL11.glTexParameteri(3553, 10241, 9987);
        GL11.glTexParameteri(3553, 10240, 9729);
        GL11.glTexParameteri(3553, 10242, 33071);
        GL11.glTexParameteri(3553, 10243, 33071);
        try {
            if (GLContext.getCapabilities().GL_EXT_texture_filter_anisotropic) {
                float f = GL11.glGetFloat(34047);
                GL11.glTexParameterf(3553, 34046, Math.min(16.0f, Math.max(1.0f, f)));
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return true;
    }

    public static boolean bindMusicCover(float f, float f2, float f3, float f4) {
        try {
            int n = MusicInfo.getCoverVersion();
            String string = MusicInfo.getCoverPath();
            if (string == null || string.isEmpty()) {
                return false;
            }
            if (coverBroken && System.currentTimeMillis() > coverRetryAt && coverAttempts < 4) {
                coverBroken = false;
                coverVersion = -1;
            }
            if (n != coverVersion) {
                File file;
                coverVersion = n;
                coverBroken = false;
                coverAttempts = 0;
                if (coverTexture > 0) {
                    GL11.glDeleteTextures(coverTexture);
                    coverTexture = -1;
                }
                if (!(file = new File(string)).isFile() || file.length() < 1024L) {
                    coverBroken = true;
                    ++coverAttempts;
                    coverRetryAt = System.currentTimeMillis() + 500L;
                    return false;
                }
                BufferedImage bufferedImage = ImageIO.read(file);
                if (bufferedImage == null) {
                    coverBroken = true;
                    ++coverAttempts;
                    coverRetryAt = System.currentTimeMillis() + 500L;
                    return false;
                }
                coverTexture = WTexture.createTexture(WTexture.fit(bufferedImage, 256));
            }
            if (coverBroken || coverTexture <= 0) {
                return false;
            }
            StencilUtil.initStencilToWrite();
            RoundedStencilShader.draw(f, f2, f3, f3, f3 * 0.28f);
            StencilUtil.readStencilBuffer(1);
            GlStateManager.enableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
            GlStateManager.bindTexture(coverTexture);
            GL11.glTexParameteri(3553, 10241, 9987);
            GL11.glTexParameteri(3553, 10240, 9729);
            GL11.glColor4f(1.0f, 1.0f, 1.0f, f4);
            GL11.glBegin(7);
            GL11.glTexCoord2f(0.0f, 0.0f);
            GL11.glVertex2f(f, f2);
            GL11.glTexCoord2f(0.0f, 1.0f);
            GL11.glVertex2f(f, f2 + f3);
            GL11.glTexCoord2f(1.0f, 1.0f);
            GL11.glVertex2f(f + f3, f2 + f3);
            GL11.glTexCoord2f(1.0f, 0.0f);
            GL11.glVertex2f(f + f3, f2);
            GL11.glEnd();
            GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
            GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
            StencilUtil.uninitStencilBuffer();
            return true;
        }
        catch (Throwable throwable) {
            coverBroken = true;
            ++coverAttempts;
            coverRetryAt = System.currentTimeMillis() + 700L;
            try {
                StencilUtil.uninitStencilBuffer();
            }
            catch (Throwable throwable2) {
                // empty catch block
            }
            return false;
        }
    }

    private static int createTexture(BufferedImage bufferedImage) {
        int n = GL11.glGenTextures();
        GlStateManager.bindTexture(n);
        GL11.glPixelStorei(3317, 4);
        int n2 = 0;
        BufferedImage bufferedImage2 = bufferedImage;
        while (true) {
            WTexture.uploadLevel(bufferedImage2, n2);
            if (bufferedImage2.getWidth() <= 1 && bufferedImage2.getHeight() <= 1) break;
            bufferedImage2 = WTexture.halve(bufferedImage2);
            ++n2;
        }
        GL11.glTexParameteri(3553, 33085, n2);
        return n;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private static Integer upload(ResourceLocation resourceLocation, int n) {
        try {
            BufferedImage bufferedImage;
            Minecraft minecraft = Minecraft.getMinecraft();
            try (InputStream inputStream = minecraft.getResourceManager().getResource(resourceLocation).getInputStream();){
                bufferedImage = ImageIO.read(inputStream);
            }
            if (bufferedImage == null) {
                return null;
            }
            BufferedImage bufferedImage2 = WTexture.fit(bufferedImage, n);
            int n2 = GL11.glGenTextures();
            GlStateManager.bindTexture(n2);
            GL11.glPixelStorei(3317, 4);
            int n3 = 0;
            BufferedImage bufferedImage3 = bufferedImage2;
            while (true) {
                WTexture.uploadLevel(bufferedImage3, n3);
                if (bufferedImage3.getWidth() <= 1 && bufferedImage3.getHeight() <= 1) break;
                bufferedImage3 = WTexture.halve(bufferedImage3);
                ++n3;
            }
            GL11.glTexParameteri(3553, 33085, n3);
            return n2;
        }
        catch (Throwable throwable) {
            System.err.println("[primordial] WTexture failed for " + String.valueOf(resourceLocation) + ": " + String.valueOf(throwable));
            return null;
        }
    }

    private static void uploadLevel(BufferedImage bufferedImage, int n) {
        int n2 = bufferedImage.getWidth();
        int n3 = bufferedImage.getHeight();
        int[] nArray = new int[n2 * n3];
        bufferedImage.getRGB(0, 0, n2, n3, nArray, 0, n2);
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(nArray.length * 4);
        IntBuffer intBuffer = byteBuffer.asIntBuffer();
        intBuffer.put(nArray);
        GL11.glTexImage2D(3553, n, 32856, n2, n3, 0, 32993, 33639, byteBuffer);
    }

    private static BufferedImage fit(BufferedImage bufferedImage, int n) {
        BufferedImage bufferedImage2 = bufferedImage;
        while (Math.max(bufferedImage2.getWidth(), bufferedImage2.getHeight()) > n * 2) {
            bufferedImage2 = WTexture.halve(bufferedImage2);
        }
        int n2 = Math.max(bufferedImage2.getWidth(), bufferedImage2.getHeight());
        if (n2 <= n) {
            return WTexture.toArgb(bufferedImage2);
        }
        float f = (float)n / (float)n2;
        int n3 = Math.max(1, Math.round((float)bufferedImage2.getWidth() * f));
        int n4 = Math.max(1, Math.round((float)bufferedImage2.getHeight() * f));
        return WTexture.resize(bufferedImage2, n3, n4);
    }

    private static BufferedImage halve(BufferedImage bufferedImage) {
        int n = Math.max(1, bufferedImage.getWidth() / 2);
        int n2 = Math.max(1, bufferedImage.getHeight() / 2);
        return WTexture.resize(bufferedImage, n, n2);
    }

    private static BufferedImage resize(BufferedImage bufferedImage, int n, int n2) {
        BufferedImage bufferedImage2 = new BufferedImage(n, n2, 2);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
        graphics2D.drawImage(bufferedImage, 0, 0, n, n2, null);
        graphics2D.dispose();
        return bufferedImage2;
    }

    private static BufferedImage toArgb(BufferedImage bufferedImage) {
        if (bufferedImage.getType() == 2) {
            return bufferedImage;
        }
        BufferedImage bufferedImage2 = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), 2);
        Graphics2D graphics2D = bufferedImage2.createGraphics();
        graphics2D.drawImage((Image)bufferedImage, 0, 0, null);
        graphics2D.dispose();
        return bufferedImage2;
    }
}
