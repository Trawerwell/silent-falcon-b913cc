/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.wyvern;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.Color;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

public final class WMsdfFont {
    private static int program;
    private static int uSampler;
    private static int uRange;
    private static int uThickness;
    private static int uSmoothness;
    private static int uTexSize;
    private static boolean shaderFailed;
    private final String name;
    private final ResourceLocation atlasLocation;
    private final ResourceLocation dataLocation;
    private final Map<Integer, Glyph> glyphs = new HashMap<Integer, Glyph>();
    private final Map<Integer, Map<Integer, Float>> kernings = new HashMap<Integer, Map<Integer, Float>>();
    private float distanceRange = 4.0f;
    private float atlasWidth = 1.0f;
    private float atlasHeight = 1.0f;
    private boolean loaded;
    private boolean broken;
    private static final String COLOR_INDEX = "0123456789abcdef";
    private static final int[] VANILLA_COLORS;

    public WMsdfFont(String string) {
        this.name = string;
        this.atlasLocation = new ResourceLocation("primordial/msdf/" + string + ".png");
        this.dataLocation = new ResourceLocation("primordial/msdf/" + string + ".json");
    }

    public String getName() {
        return this.name;
    }

    public boolean isUsable() {
        this.ensureLoaded();
        return this.loaded && !this.broken && program != 0;
    }

    /*
     * WARNING - Removed try catching itself - possible behaviour change.
     */
    private void ensureLoaded() {
        if (this.loaded || this.broken) {
            return;
        }
        try {
            JsonObject jsonObject;
            JsonObject jsonObject2;
            Minecraft minecraft = Minecraft.getMinecraft();
            try (InputStreamReader inputStreamReader = new InputStreamReader(minecraft.getResourceManager().getResource(this.dataLocation).getInputStream(), "UTF-8");){
                jsonObject2 = new JsonParser().parse(inputStreamReader).getAsJsonObject();
            }
            JsonObject jsonObject3 = jsonObject2.getAsJsonObject("atlas");
            this.distanceRange = jsonObject3.get("distanceRange").getAsFloat();
            this.atlasWidth = jsonObject3.get("width").getAsFloat();
            this.atlasHeight = jsonObject3.get("height").getAsFloat();
            for (JsonElement jsonElement : jsonObject2.getAsJsonArray("glyphs")) {
                jsonObject = jsonElement.getAsJsonObject();
                Glyph glyph = new Glyph();
                glyph.advance = jsonObject.get("advance").getAsFloat();
                if (jsonObject.has("planeBounds") && jsonObject.has("atlasBounds")) {
                    JsonObject jsonObject4 = jsonObject.getAsJsonObject("planeBounds");
                    JsonObject jsonObject5 = jsonObject.getAsJsonObject("atlasBounds");
                    glyph.planeLeft = jsonObject4.get("left").getAsFloat();
                    glyph.planeBottom = jsonObject4.get("bottom").getAsFloat();
                    glyph.planeRight = jsonObject4.get("right").getAsFloat();
                    glyph.planeTop = jsonObject4.get("top").getAsFloat();
                    glyph.u0 = jsonObject5.get("left").getAsFloat() / this.atlasWidth;
                    glyph.u1 = jsonObject5.get("right").getAsFloat() / this.atlasWidth;
                    glyph.v0 = 1.0f - jsonObject5.get("top").getAsFloat() / this.atlasHeight;
                    glyph.v1 = 1.0f - jsonObject5.get("bottom").getAsFloat() / this.atlasHeight;
                    glyph.drawable = true;
                }
                this.glyphs.put(jsonObject.get("unicode").getAsInt(), glyph);
            }
            if (jsonObject2.has("kerning")) {
                for (JsonElement jsonElement : jsonObject2.getAsJsonArray("kerning")) {
                    jsonObject = jsonElement.getAsJsonObject();
                    int n = jsonObject.get("unicode1").getAsInt();
                    int n2 = jsonObject.get("unicode2").getAsInt();
                    float f = jsonObject.get("advance").getAsFloat();
                    Map<Integer, Float> map = this.kernings.get(n);
                    if (map == null) {
                        map = new HashMap<Integer, Float>();
                        this.kernings.put(n, map);
                    }
                    map.put(n2, Float.valueOf(f));
                }
            }
            WMsdfFont.ensureProgram();
            this.loaded = true;
        }
        catch (Throwable throwable) {
            this.broken = true;
            System.err.println("[primordial] msdf font '" + this.name + "' failed: " + String.valueOf(throwable));
        }
    }

    private static void ensureProgram() {
        if (program != 0 || shaderFailed || !OpenGlHelper.shadersSupported) {
            return;
        }
        try {
            String string = "#version 120\nvarying vec2 uv;\nvarying vec4 col;\nvoid main(){\n gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n uv = gl_MultiTexCoord0.xy;\n col = gl_Color;\n}";
            String string2 = "#version 120\nuniform sampler2D tex;\nuniform float range;\nuniform float thickness;\nuniform float smoothness;\nuniform vec2 texSize;\nvarying vec2 uv;\nvarying vec4 col;\nfloat median(vec3 c){ return max(min(c.r,c.g), min(max(c.r,c.g), c.b)); }\nvoid main(){\n float dist = median(texture2D(tex, uv).rgb) - 0.5 + thickness;\n vec2 h = vec2(dFdx(uv.x), dFdy(uv.y)) * texSize;\n float pixels = range * inversesqrt(max(h.x*h.x + h.y*h.y, 1e-8));\n float a = smoothstep(-smoothness, smoothness, dist * pixels);\n if (a <= 0.001) discard;\n gl_FragColor = vec4(col.rgb, col.a * a);\n}";
            int n = WMsdfFont.compile(35633, string);
            int n2 = WMsdfFont.compile(35632, string2);
            int n3 = GL20.glCreateProgram();
            GL20.glAttachShader(n3, n);
            GL20.glAttachShader(n3, n2);
            GL20.glLinkProgram(n3);
            GL20.glDeleteShader(n);
            GL20.glDeleteShader(n2);
            if (GL20.glGetProgrami(n3, 35714) == 0) {
                throw new IllegalStateException(GL20.glGetProgramInfoLog(n3, 4096));
            }
            program = n3;
            uSampler = GL20.glGetUniformLocation(program, "tex");
            uRange = GL20.glGetUniformLocation(program, "range");
            uThickness = GL20.glGetUniformLocation(program, "thickness");
            uSmoothness = GL20.glGetUniformLocation(program, "smoothness");
            uTexSize = GL20.glGetUniformLocation(program, "texSize");
        }
        catch (Throwable throwable) {
            shaderFailed = true;
            System.err.println("[primordial] msdf shader failed: " + String.valueOf(throwable));
        }
    }

    private static int compile(int n, String string) {
        int n2 = GL20.glCreateShader(n);
        GL20.glShaderSource(n2, string);
        GL20.glCompileShader(n2);
        if (GL20.glGetShaderi(n2, 35713) == 0) {
            throw new IllegalStateException(GL20.glGetShaderInfoLog(n2, 4096));
        }
        return n2;
    }

    public float getHeight(float f) {
        return f * 0.7f;
    }

    public float getWidth(String string, float f) {
        this.ensureLoaded();
        if (string == null || string.isEmpty() || !this.loaded) {
            return 0.0f;
        }
        float f2 = 0.0f;
        int n = -1;
        boolean bl = false;
        for (int i = 0; i < string.length(); ++i) {
            Float f3;
            char c = string.charAt(i);
            if (bl) {
                bl = false;
                continue;
            }
            if (c == '\u00a7') {
                bl = true;
                continue;
            }
            Glyph glyph = this.glyphs.get(c);
            if (glyph == null) continue;
            Map<Integer, Float> map = this.kernings.get(n);
            if (map != null && (f3 = map.get(c)) != null) {
                f2 += f3.floatValue() * f;
            }
            f2 += glyph.advance * f;
            n = c;
        }
        return f2;
    }

    public boolean canRender(String string) {
        this.ensureLoaded();
        if (!this.loaded) {
            return false;
        }
        boolean bl = false;
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (bl) {
                bl = false;
                continue;
            }
            if (c == '\u00a7') {
                bl = true;
                continue;
            }
            if (c == ' ' || this.glyphs.containsKey(c)) continue;
            return false;
        }
        return true;
    }

    public float coverage(String string) {
        this.ensureLoaded();
        if (!this.loaded) {
            return 0.0f;
        }
        if (string == null || string.isEmpty()) {
            return 1.0f;
        }
        int n = 0;
        int n2 = 0;
        boolean bl = false;
        for (int i = 0; i < string.length(); ++i) {
            char c = string.charAt(i);
            if (bl) {
                bl = false;
                continue;
            }
            if (c == '\u00a7') {
                bl = true;
                continue;
            }
            if (c == ' ') continue;
            ++n;
            if (!this.glyphs.containsKey(c)) continue;
            ++n2;
        }
        return n == 0 ? 1.0f : (float)n2 / (float)n;
    }

    public String trimToWidth(String string, float f, float f2) {
        float f3;
        if (string == null || f2 <= 0.0f) {
            return "";
        }
        if (this.getWidth(string, f) <= f2) {
            return string;
        }
        String string2 = "..";
        StringBuilder stringBuilder = new StringBuilder();
        float f4 = this.getWidth(string2, f);
        float f5 = 0.0f;
        for (int i = 0; i < string.length() && !(f5 + (f3 = this.getWidth(String.valueOf(string.charAt(i)), f)) + f4 > f2); ++i) {
            stringBuilder.append(string.charAt(i));
            f5 += f3;
        }
        return String.valueOf(stringBuilder) + string2;
    }

    public float draw(String string, float f, float f2, float f3, Color color) {
        return this.draw(string, f, f2, f3, color.getRed(), color.getGreen(), color.getBlue(), color.getAlpha());
    }

    public float draw(String string, float f, float f2, float f3, int n) {
        int n2 = n >>> 24;
        return this.draw(string, f, f2, f3, n >> 16 & 0xFF, n >> 8 & 0xFF, n & 0xFF, n2 == 0 ? 255 : n2);
    }

    public float draw(String string, float f, float f2, float f3, int n, int n2, int n3, int n4) {
        this.ensureLoaded();
        if (string == null || string.isEmpty() || !this.loaded || program == 0) {
            return 0.0f;
        }
        if (n4 <= 2) {
            return this.getWidth(string, f3);
        }
        Minecraft minecraft = Minecraft.getMinecraft();
        minecraft.getTextureManager().bindTexture(this.atlasLocation);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.enableTexture2D();
        GlStateManager.disableAlpha();
        GlStateManager.depthMask(false);
        GL20.glUseProgram(program);
        GL20.glUniform1i(uSampler, 0);
        GL20.glUniform1f(uRange, this.distanceRange);
        GL20.glUniform1f(uThickness, 0.05f);
        GL20.glUniform1f(uSmoothness, 0.57f);
        GL20.glUniform2f(uTexSize, this.atlasWidth, this.atlasHeight);
        float f4 = f2 + f3 * 0.7f;
        float f5 = f;
        int n5 = -1;
        boolean bl = false;
        float f6 = (float)n / 255.0f;
        float f7 = (float)n2 / 255.0f;
        float f8 = (float)n3 / 255.0f;
        float f9 = (float)n4 / 255.0f;
        float f10 = f6;
        float f11 = f7;
        float f12 = f8;
        GL11.glBegin(7);
        for (int i = 0; i < string.length(); ++i) {
            Float f13;
            char c = string.charAt(i);
            if (bl) {
                bl = false;
                int n6 = COLOR_INDEX.indexOf(Character.toLowerCase(c));
                if (n6 >= 0 && n6 < 16) {
                    int n7 = VANILLA_COLORS[n6];
                    f6 = (float)(n7 >> 16 & 0xFF) / 255.0f;
                    f7 = (float)(n7 >> 8 & 0xFF) / 255.0f;
                    f8 = (float)(n7 & 0xFF) / 255.0f;
                    continue;
                }
                if (Character.toLowerCase(c) != 'r') continue;
                f6 = f10;
                f7 = f11;
                f8 = f12;
                continue;
            }
            if (c == '\u00a7') {
                bl = true;
                continue;
            }
            Glyph glyph = this.glyphs.get(c);
            if (glyph == null) continue;
            Map<Integer, Float> map = this.kernings.get(n5);
            if (map != null && (f13 = map.get(c)) != null) {
                f5 += f13.floatValue() * f3;
            }
            if (glyph.drawable) {
                float f14 = f5 + glyph.planeLeft * f3;
                float f15 = f5 + glyph.planeRight * f3;
                float f16 = f4 - glyph.planeTop * f3;
                float f17 = f4 - glyph.planeBottom * f3;
                GL11.glColor4f(f6, f7, f8, f9);
                GL11.glTexCoord2f(glyph.u0, glyph.v0);
                GL11.glVertex2f(f14, f16);
                GL11.glColor4f(f6, f7, f8, f9);
                GL11.glTexCoord2f(glyph.u0, glyph.v1);
                GL11.glVertex2f(f14, f17);
                GL11.glColor4f(f6, f7, f8, f9);
                GL11.glTexCoord2f(glyph.u1, glyph.v1);
                GL11.glVertex2f(f15, f17);
                GL11.glColor4f(f6, f7, f8, f9);
                GL11.glTexCoord2f(glyph.u1, glyph.v0);
                GL11.glVertex2f(f15, f16);
            }
            f5 += glyph.advance * f3;
            n5 = c;
        }
        GL11.glEnd();
        GL20.glUseProgram(0);
        GlStateManager.depthMask(true);
        GlStateManager.enableAlpha();
        GL11.glColor4f(1.0f, 1.0f, 1.0f, 1.0f);
        GlStateManager.color(1.0f, 1.0f, 1.0f, 1.0f);
        return f5 - f;
    }

    public float drawVCentered(String string, float f, float f2, float f3, float f4, Color color) {
        this.ensureLoaded();
        float f5 = Float.MAX_VALUE;
        float f6 = -3.4028235E38f;
        boolean bl = false;
        if (this.loaded && string != null) {
            for (int i = 0; i < string.length(); ++i) {
                char c = string.charAt(i);
                if (bl) {
                    bl = false;
                    continue;
                }
                if (c == '\u00a7') {
                    bl = true;
                    continue;
                }
                Glyph glyph = this.glyphs.get(c);
                if (glyph == null || !glyph.drawable) continue;
                float f7 = f4 * 0.7f - glyph.planeTop * f4;
                float f8 = f4 * 0.7f - glyph.planeBottom * f4;
                f5 = Math.min(f5, f7);
                f6 = Math.max(f6, f8);
            }
        }
        if (f5 > f6) {
            return this.draw(string, f, f2 + (f3 - this.getHeight(f4)) / 2.0f, f4, color);
        }
        float f9 = f6 - f5;
        float f10 = f2 + (f3 - f9) / 2.0f - f5;
        return this.draw(string, f, f10, f4, color);
    }

    public float drawCentered(String string, float f, float f2, float f3, Color color) {
        return this.draw(string, f - this.getWidth(string, f3) / 2.0f, f2, f3, color);
    }

    static {
        VANILLA_COLORS = new int[]{0, 170, 43520, 43690, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA, 0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};
    }

    private static final class Glyph {
        float advance;
        float planeLeft;
        float planeBottom;
        float planeRight;
        float planeTop;
        float u0;
        float v0;
        float u1;
        float v1;
        boolean drawable;

        private Glyph() {
        }
    }
}
