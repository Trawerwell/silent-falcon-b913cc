/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.font;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.Locale;
import javax.vecmath.Vector2d;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.StringUtils;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import primordial.util.render.shader.ShaderManager;

public class FontUtil {
    private Font font;
    private boolean fractionalMetrics;
    public CharacterData[] characterData;
    private int[] colorCodes;
    private static int RANDOM_OFFSET = 1;
    public static final char colorCode = '\u00a7';
    public static final String colorIndex = "0123456789abcdefklmnor";
    private long ms = -1L;
    private float texCoordX;
    private float texCoordY;
    private float oversample = 1.0f;
    private int textureId;
    private boolean autoBegin = true;
    private int atlas;
    public float MAX_HEIGHT;

    public FontUtil(Font font, boolean bl, Style style) {
        this(font, 1330, bl, style, 1.0f);
    }

    public FontUtil(Font font, int n, boolean bl, Style style) {
        this(font, n, bl, style, 1.0f);
    }

    public FontUtil(Font font, int n, boolean bl, Style style, float f) {
        this(font, n, true, bl, style, f);
    }

    public FontUtil(Font font, int n, boolean bl, boolean bl2, Style style) {
        this(font, n, bl, bl2, style, 1.0f);
    }

    public FontUtil(Font font, int n, boolean bl, boolean bl2, Style style, float f) {
        long l = System.currentTimeMillis();
        this.oversample = Math.max(1.0f, f);
        this.colorCodes = new int[32];
        this.font = font;
        this.fractionalMetrics = bl;
        switch (style.ordinal()) {
            case 0: {
                this.characterData = this.setup(new CharacterData[n], 0, bl2);
                break;
            }
            case 1: {
                this.characterData = this.setup(new CharacterData[n], 1, bl2);
                break;
            }
            case 2: {
                this.characterData = this.setup(new CharacterData[n], 2, bl2);
            }
        }
        System.out.println("A Font created in: " + (System.currentTimeMillis() - l) + "ms (Size: " + font.getSize() + ", Resolution: " + this.atlas + ")");
    }

    private CharacterData[] setup(CharacterData[] characterDataArray, int n, boolean bl) {
        char c;
        int n2;
        this.generateColors();
        Font font = this.font.deriveFont(n, this.font.getSize2D() * this.oversample);
        BufferedImage bufferedImage = new BufferedImage(1, 1, 2);
        Graphics2D graphics2D = bufferedImage.createGraphics();
        graphics2D.setFont(font);
        this.applyHints(graphics2D, bl);
        FontMetrics fontMetrics = graphics2D.getFontMetrics();
        float f = 0.0f;
        for (int i = 0; i < characterDataArray.length; ++i) {
            if (!FontUtil.shouldRasterize(i)) continue;
            Rectangle2D rectangle2D = fontMetrics.getStringBounds(FontUtil.glyphText(i), graphics2D);
            f = Math.max(f, (float)rectangle2D.getHeight());
        }
        float f2 = Math.max(12.0f, f + 4.0f);
        for (n2 = 512; !this.packingFits(characterDataArray.length, fontMetrics, graphics2D, f2, n2) && n2 < 8192; n2 += 256) {
        }
        graphics2D.dispose();
        this.textureId = GL11.glGenTextures();
        this.atlas = n2;
        this.texCoordX = 1.0f / (float)n2;
        this.texCoordY = 1.0f / (float)n2;
        BufferedImage bufferedImage2 = new BufferedImage(n2, n2, 2);
        Graphics2D graphics2D2 = bufferedImage2.createGraphics();
        graphics2D2.setFont(font);
        graphics2D2.setColor(Color.WHITE);
        this.applyHints(graphics2D2, bl);
        float f3 = 4.0f;
        float f4 = (float)fontMetrics.getAscent() + 2.0f;
        for (int i = 0; i < characterDataArray.length; ++i) {
            if (!FontUtil.shouldRasterize(i)) continue;
            c = (char)i;
            String string = FontUtil.glyphText(i);
            Rectangle2D rectangle2D = fontMetrics.getStringBounds(string, graphics2D2);
            float f5 = (float)rectangle2D.getWidth() + 8.0f * this.oversample;
            float f6 = (float)rectangle2D.getHeight();
            if (f3 + f5 + 4.0f > (float)n2) {
                f3 = 4.0f;
                f4 += f2;
            }
            if (f4 + f2 > (float)n2) {
                System.err.println("[FontUtil] Atlas overflow at U+" + Integer.toHexString(i));
                break;
            }
            graphics2D2.drawString(string, f3, f4);
            float f7 = Math.max(this.oversample, (float)fontMetrics.stringWidth(string));
            characterDataArray[i] = new CharacterData(c, f3, f4 - (float)fontMetrics.getAscent(), f5, f6, f7);
            this.MAX_HEIGHT = Math.max(this.MAX_HEIGHT, f6);
            f3 += f5 + 4.0f;
        }
        graphics2D2.dispose();
        ByteBuffer byteBuffer = BufferUtils.createByteBuffer(n2 * n2 * 4);
        this.putTexture(n2, n2, bufferedImage2, byteBuffer);
        byteBuffer.flip();
        GlStateManager.bindTexture(this.textureId);
        c = this.oversample >= 3.5f ? (char)'\u2601' : '\u2600';
        GL11.glTexParameteri(3553, 10241, c);
        GL11.glTexParameteri(3553, 10240, c);
        GL11.glTexImage2D(3553, 0, 6408, n2, n2, 0, 6408, 5121, byteBuffer);
        return characterDataArray;
    }

    private void applyHints(Graphics2D graphics2D, boolean bl) {
        if (!bl) {
            return;
        }
        graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        graphics2D.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, this.fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
    }

    private static boolean shouldRasterize(int n) {
        return n <= 1327 || n >= 8192 && n <= 8303 || n >= 8592 && n <= 8703 || n >= 9472 && n <= 10175;
    }

    private static String glyphText(int n) {
        if (n == 9876) {
            return "\u00d7";
        }
        if (n == 10010) {
            return "+";
        }
        if (n == 9733 || n == 9734) {
            return "*";
        }
        if (n == 9829 || n == 10084) {
            return "v";
        }
        return String.valueOf((char)n);
    }

    private boolean packingFits(int n, FontMetrics fontMetrics, Graphics2D graphics2D, float f, int n2) {
        float f2 = 4.0f;
        float f3 = (float)fontMetrics.getAscent() + 2.0f;
        for (int i = 0; i < n; ++i) {
            if (!FontUtil.shouldRasterize(i)) continue;
            Rectangle2D rectangle2D = fontMetrics.getStringBounds(FontUtil.glyphText(i), graphics2D);
            float f4 = (float)rectangle2D.getWidth() + 8.0f * this.oversample;
            if (f2 + f4 + 4.0f > (float)n2) {
                f2 = 4.0f;
                f3 += f;
            }
            if (f3 + f > (float)n2) {
                return false;
            }
            f2 += f4 + 4.0f;
        }
        return true;
    }

    private void putTexture(int n, int n2, BufferedImage bufferedImage, ByteBuffer byteBuffer) {
        for (int i = 0; i < n2; ++i) {
            for (int j = 0; j < n; ++j) {
                int n3 = bufferedImage.getRGB(j, i);
                int n4 = n3 >> 16 & 0xFF;
                int n5 = n3 >> 8 & 0xFF;
                int n6 = n3 & 0xFF;
                int n7 = n3 >> 24 & 0xFF;
                byteBuffer.put((byte)n4);
                byteBuffer.put((byte)n5);
                byteBuffer.put((byte)n6);
                byteBuffer.put((byte)n7);
            }
        }
    }

    public void drawString(String string, float f, float f2, int n) {
        this.drawString(string, f, f2, new Color(n));
    }

    public void drawString(String string, float f, float f2, Color color) {
        this.renderString(string, f, f2, color, false);
    }

    public void drawCenteredString(String string, float f, float f2, int n) {
        this.drawCenteredString(string, f, f2, new Color(n));
    }

    public void drawCenteredString(String string, float f, float f2, Color color) {
        float f3 = this.getWidth(string) / 2.0f;
        float f4 = this.getHeight(string) / 2.0f;
        this.renderString(string, f - f3, f2 - f4, color, false);
    }

    public void drawBorderedString(String string, float f, float f2, int n, int n2, float f3) {
        this.drawBorderedString(string, f, f2, new Color(n), new Color(n2), f3);
    }

    public void drawBorderedString(String string, float f, float f2, Color color, Color color2, float f3) {
        String string2 = StringUtils.stripControlCodes(string);
        this.renderString(string2, f - f3, f2, color2, false);
        this.renderString(string2, f + f3, f2, color2, false);
        this.renderString(string2, f, f2 - f3, color2, false);
        this.renderString(string2, f, f2 + f3, color2, false);
        this.renderString(string, f, f2, color, false);
    }

    public void drawBorderedString(String string, float f, float f2, Color color, float f3) {
        ShaderManager shaderManager = ShaderManager.getInstance();
        if (this.ms == -1L) {
            this.ms = System.currentTimeMillis();
        }
        shaderManager.loadShader("make_gold");
        shaderManager.loadData("make_gold", "amount", Float.valueOf((float)(this.ms - System.currentTimeMillis() - 1L) / 2000.0f));
        shaderManager.loadData("make_gold", "offset", Float.valueOf(5.0f));
        String string2 = StringUtils.stripControlCodes(string);
        this.renderString(string2, f - f3, f2, Color.white, false);
        this.renderString(string2, f + f3, f2, Color.white, false);
        this.renderString(string2, f, f2 - f3, Color.white, false);
        this.renderString(string2, f, f2 + f3, Color.black, false);
        shaderManager.stop("make_gold");
        this.renderString(string, f, f2, color, false);
    }

    private void renderString(String string, float f, float f2, Color color, boolean bl) {
        if (string.length() != 0) {
            if (this.autoBegin) {
                this.begin();
            }
            f -= 2.0f;
            f2 -= 2.0f;
            f += 0.5f;
            f2 += 0.5f;
            float f3 = 2.0f * this.oversample;
            f *= f3;
            f2 *= f3;
            CharacterData[] characterDataArray = this.characterData;
            boolean bl2 = false;
            boolean bl3 = false;
            boolean bl4 = false;
            int n = string.length();
            float f4 = 255.0f;
            Color color2 = color;
            GlStateManager.color((float)color2.getRed() / 255.0f, (float)color2.getGreen() / 255.0f, (float)color2.getBlue() / 255.0f, color2.getAlpha());
            for (int i = 0; i < n; ++i) {
                int n2;
                char c = string.charAt(i);
                int n3 = n2 = i > 0 ? (int)string.charAt(i - 1) : 46;
                if (n2 == 167) continue;
                if (c == '\u00a7') {
                    int n4 = colorIndex.indexOf(string.toLowerCase(Locale.ENGLISH).charAt(i + 1));
                    if (n4 >= 16) {
                        if (n4 == 16) {
                            bl4 = true;
                            continue;
                        }
                        if (n4 == 17) {
                            characterDataArray = this.characterData;
                            continue;
                        }
                        if (n4 == 18) {
                            bl3 = true;
                            continue;
                        }
                        if (n4 == 19) {
                            bl2 = true;
                            continue;
                        }
                        if (n4 == 20) {
                            characterDataArray = this.characterData;
                            continue;
                        }
                        bl4 = false;
                        bl3 = false;
                        bl2 = false;
                        characterDataArray = this.characterData;
                        GlStateManager.color(1.0f, 1.0f, 1.0f, (float)color.getAlpha() / 255.0f);
                        color2 = new Color(255, 255, 255, color.getAlpha());
                        continue;
                    }
                    bl4 = false;
                    bl3 = false;
                    bl2 = false;
                    characterDataArray = this.characterData;
                    if (n4 < 0) {
                        n4 = 15;
                    }
                    int n5 = this.colorCodes[n4];
                    float f5 = (float)(n5 >> 16) / 255.0f;
                    float f6 = (float)(n5 >> 8 & 0xFF) / 255.0f;
                    float f7 = (float)(n5 & 0xFF) / 255.0f;
                    GlStateManager.color(f5, f6, f7, (float)color.getAlpha() / 255.0f);
                    color2 = new Color((int)(f5 * 255.0f), (int)(f6 * 255.0f), (int)(f7 * 255.0f));
                    continue;
                }
                if (bl4) {
                    c = (char)(c + (char)RANDOM_OFFSET);
                }
                this.drawChar(c, characterDataArray, f, f2, color2, bl);
                if (c >= characterDataArray.length || characterDataArray[c] == null) continue;
                CharacterData characterData = characterDataArray[c];
                if (bl3) {
                    this.drawLine(new Vector2d(0.0, (double)characterData.height / 2.0), new Vector2d(characterData.width, (double)characterData.height / 2.0), 3.0f);
                }
                if (bl2) {
                    this.drawLine(new Vector2d(0.0, (double)characterData.height - 15.0), new Vector2d(characterData.width, (double)characterData.height - 15.0), 3.0f);
                }
                f += characterData.advance;
            }
            if (this.autoBegin) {
                this.end();
            }
        }
    }

    public float getWidth(String string) {
        float f = 0.0f;
        CharacterData[] characterDataArray = this.characterData;
        int n = string.length();
        for (int i = 0; i < n; ++i) {
            int n2;
            char c = string.charAt(i);
            int n3 = n2 = i > 0 ? (int)string.charAt(i - 1) : 46;
            if (n2 == 167 || c == '\u00a7' || c >= characterDataArray.length || characterDataArray[c] == null) continue;
            CharacterData characterData = characterDataArray[c];
            f += characterData.advance / (2.0f * this.oversample);
        }
        return f + 2.0f;
    }

    public float getHeight(String string) {
        float f = 0.0f;
        CharacterData[] characterDataArray = this.characterData;
        int n = string.length();
        for (int i = 0; i < n; ++i) {
            int n2;
            char c = string.charAt(i);
            int n3 = n2 = i > 0 ? (int)string.charAt(i - 1) : 46;
            if (n2 == 167 || c == '\u00a7' || c >= characterDataArray.length || characterDataArray[c] == null) continue;
            CharacterData characterData = characterDataArray[c];
            f = Math.max(f, characterData.height);
        }
        return f / (2.0f * this.oversample) - 2.0f;
    }

    private void drawChar(char c, CharacterData[] characterDataArray, float f, float f2, Color color, boolean bl) {
        float f3;
        float f4;
        if (c >= characterDataArray.length || characterDataArray[c] == null) {
            return;
        }
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        CharacterData characterData = characterDataArray[c];
        float f5 = characterData.positionX - 4.0f * this.oversample;
        float f6 = characterData.positionY;
        float f7 = characterData.width;
        float f8 = characterData.height;
        if (bl) {
            float f9 = this.oversample;
            f4 = (f += f9) + characterData.width;
            f3 = (f2 += f9) + characterData.height;
            worldRenderer.pos(f, f3, 0.0).tex(f5 * this.texCoordX, (f6 + f8) * this.texCoordY).color(Color.black).endVertex();
            worldRenderer.pos(f4, f3, 0.0).tex((f5 + f7) * this.texCoordX, (f6 + f8) * this.texCoordY).color(Color.black).endVertex();
            worldRenderer.pos(f4, f2, 0.0).tex((f5 + f7) * this.texCoordX, f6 * this.texCoordY).color(Color.black).endVertex();
            worldRenderer.pos(f, f2, 0.0).tex(f5 * this.texCoordX, f6 * this.texCoordY).color(Color.black).endVertex();
            f -= this.oversample;
            f2 -= this.oversample;
        }
        f4 = f + characterData.width;
        f3 = f2 + characterData.height;
        worldRenderer.pos(f, f3, 0.0).tex(f5 * this.texCoordX, (f6 + f8) * this.texCoordY).color(color).endVertex();
        worldRenderer.pos(f4, f3, 0.0).tex((f5 + f7) * this.texCoordX, (f6 + f8) * this.texCoordY).color(color).endVertex();
        worldRenderer.pos(f4, f2, 0.0).tex((f5 + f7) * this.texCoordX, f6 * this.texCoordY).color(color).endVertex();
        worldRenderer.pos(f, f2, 0.0).tex(f5 * this.texCoordX, f6 * this.texCoordY).color(color).endVertex();
    }

    public void drawStringWithShadow(String string, float f, float f2, int n) {
        this.drawStringWithShadow(string, f, f2, new Color(n));
    }

    public void drawStringWithShadow(String string, float f, float f2, Color color) {
        this.renderString(string, f, f2, color, true);
    }

    public void drawLine(Vector2d vector2d, Vector2d vector2d2, float f) {
        GL11.glDisable(3553);
        GL11.glLineWidth(f);
        GL11.glBegin(1);
        GL11.glVertex2d(vector2d.x, vector2d.y);
        GL11.glVertex2d(vector2d2.x, vector2d2.y);
        GL11.glEnd();
        GL11.glEnable(3553);
    }

    private void generateColors() {
        for (int i = 0; i < 32; ++i) {
            int n = (i >> 3 & 1) * 85;
            int n2 = (i >> 2 & 1) * 170 + n;
            int n3 = (i >> 1 & 1) * 170 + n;
            int n4 = (i & 1) * 170 + n;
            if (i == 6) {
                n2 += 85;
            }
            if (i >= 16) {
                n2 /= 4;
                n3 /= 4;
                n4 /= 4;
            }
            this.colorCodes[i] = (n2 & 0xFF) << 16 | (n3 & 0xFF) << 8 | n4 & 0xFF;
        }
    }

    public void begin() {
        this.begin(770, 771, 0.0f, 0.0f, 0.0f, 1.0f);
    }

    public void begin(int n, int n2, float f, float f2, float f3, float f4) {
        GlStateManager.pushMatrix();
        GlStateManager.forceBindTexture(this.textureId());
        double d = 0.5 / (double)this.oversample;
        GlStateManager.scale(d, d, 1.0);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(n, n2);
        GL14.glBlendColor(f, f2, f3, f4);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
    }

    public void end() {
        Tessellator.getInstance().draw();
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.popMatrix();
    }

    public boolean autoBegin() {
        return this.autoBegin;
    }

    public void setAutoBegin(boolean bl) {
        this.autoBegin = bl;
    }

    public String trimStringToWidth(String string, int n) {
        return this.trimStringToWidth(string, n, false);
    }

    public String trimStringToWidth(String string, int n, boolean bl) {
        StringBuilder stringBuilder = new StringBuilder();
        float f = 0.0f;
        int n2 = bl ? string.length() - 1 : 0;
        int n3 = bl ? -1 : 1;
        boolean bl2 = false;
        boolean bl3 = false;
        for (int i = n2; i >= 0 && i < string.length() && f < (float)n; i += n3) {
            char c = string.charAt(i);
            CharacterData characterData = c < this.characterData.length ? this.characterData[c] : null;
            float f2 = characterData == null ? 4.0f : characterData.advance / (2.0f * this.oversample);
            float f3 = f2;
            if (bl2) {
                bl2 = false;
                if (c != 'l' && c != 'L') {
                    if (c == 'r' || c == 'R') {
                        bl3 = false;
                    }
                } else {
                    bl3 = true;
                }
            } else if (f2 < 0.0f) {
                bl2 = true;
            } else {
                f += f2;
                if (bl3) {
                    f += 1.0f;
                }
            }
            if (f > (float)n) break;
            if (bl) {
                stringBuilder.insert(0, c);
                continue;
            }
            stringBuilder.append(c);
        }
        return stringBuilder.toString();
    }

    public float getCharAdvance(char c) {
        if (c >= this.characterData.length || this.characterData[c] == null) {
            return 0.0f;
        }
        return this.characterData[c].advance / (2.0f * this.oversample);
    }

    public int textureId() {
        return this.textureId;
    }

    public boolean canRenderFully(String string) {
        if (string == null) {
            return true;
        }
        int n = string.length();
        int n2 = 0;
        while (n2 < n) {
            char c = string.charAt(n2);
            int n3 = n2 > 0 ? (int)string.charAt(n2 - 1) : 46;
            int n4 = n3;
            if (n3 == 167 || c == '\u00a7') {
                ++n2;
                continue;
            }
            if (c != ' ' && c != '\u00a0') {
                if (c >= this.characterData.length) {
                    return false;
                }
                CharacterData characterData = this.characterData[c];
                if (characterData == null || characterData.width <= 8.0f * this.oversample) {
                    return false;
                }
            }
            ++n2;
        }
        return true;
    }

    public static enum Style {
        REGULAR,
        BOLD,
        ITALIC;

    }

    public static class CharacterData {
        public char character;
        public float positionX;
        public float positionY;
        public float width;
        public float height;
        public float advance;
        public int textureId;

        public CharacterData(char c, float f, float f2, float f3, float f4, float f5) {
            this.character = c;
            this.positionX = f;
            this.positionY = f2;
            this.width = f3;
            this.height = f4;
            this.advance = f5;
        }
    }
}
