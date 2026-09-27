/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.clickgui;

import java.awt.Color;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.ScaledResolution;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import primordial.Primordial;
import primordial.module.Category;
import primordial.module.Module;
import primordial.module.ModuleManager;
import primordial.module.impl.render.ClickGui;
import primordial.setting.ModuleMode;
import primordial.setting.Setting;
import primordial.setting.SettingRegistry;
import primordial.setting.SettingType;
import primordial.setting.impl.BooleanSetting;
import primordial.setting.impl.ColorSetting;
import primordial.setting.impl.EnumSetting;
import primordial.setting.impl.ModeSetting;
import primordial.setting.impl.MultiSelectSetting;
import primordial.setting.impl.NumberSetting;
import primordial.setting.impl.TextSetting;
import primordial.ui.clickgui.GuiSettings;
import primordial.ui.clickgui.SmoothRender;
import primordial.ui.clickgui.WyvernBlur;
import primordial.ui.clickgui.WyvernTheme;
import primordial.ui.clickgui.WyvernThemeManager;
import primordial.ui.font.FontUtil;
import primordial.util.math.MathUtils;
import primordial.util.render.RenderUtils;

public class NewClickGui
extends GuiScreen {
    private static final float PANEL_W = 125.0f;
    private static final float PANEL_STEP = 135.0f;
    private static final float HEADER_H = 18.0f;
    private static final float MAX_H = 260.0f;
    private static final float MIN_H = 60.0f;
    private static final float CARD_X = 2.5f;
    private static final float CARD_W = 120.0f;
    private static final float CARD_H = 19.0f;
    private static final float GAP = 4.0f;
    private static final Color BODY = new Color(0, 0, 0, 125);
    private static final Color HEADER = new Color(16, 16, 23, 178);
    private static final Color CARD = new Color(255, 255, 255, 13);
    private static final Color TEXT = new Color(242, 242, 246);
    private static final Color MUTED = new Color(185, 185, 196);
    private static final Color ACCENT = new Color(151, 84, 255);
    public GuiTextField textField;
    private final Map<Module, Boolean> opened = new HashMap<Module, Boolean>();
    private final Map<Module, Float> openAnim = new HashMap<Module, Float>();
    private final Map<Module, Float> enabledAnim = new HashMap<Module, Float>();
    private final Map<BooleanSetting, Float> booleanAnim = new HashMap<BooleanSetting, Float>();
    private final Map<Category, Float> scroll = new EnumMap<Category, Float>(Category.class);
    private final Map<Category, Float> scrollTarget = new EnumMap<Category, Float>(Category.class);
    private final Map<Category, Float> scrollVelocity = new EnumMap<Category, Float>(Category.class);
    private static final float SCROLL_STEP = 23.0f;
    private final Map<Category, Float> panelHeight = new EnumMap<Category, Float>(Category.class);
    private final Map<ColorSetting, Boolean> colorExpanded = new HashMap<ColorSetting, Boolean>();
    private Module bindingModule;
    private NumberSetting draggingNumber;
    private float dragX;
    private float dragWidth;
    private TextSetting editingText;
    private boolean searchActive;
    private String search = "";
    private long lastFrame = System.nanoTime();
    private float transition;
    private boolean closing;
    private float guiX;
    private float guiY;
    private float renderOffset;
    private float screenProgress;
    private float frameDt;
    private int lastMouseX;
    private int lastMouseY;
    private final WyvernThemeManager themeManager = new WyvernThemeManager();
    private float themeScroll;
    private float themeScrollTarget;
    private int modal;
    private int deleteTheme = -1;
    private int selectedCreateColor;
    private String createThemeName = "Custom";
    private final List<Color> createThemeColors = new ArrayList<Color>();
    private boolean draggingPalette;
    private boolean draggingHue;

    public NewClickGui() {
        GuiSettings.initFont();
        for (Category category : Category.VALUES) {
            this.scroll.put(category, Float.valueOf(0.0f));
            this.scrollTarget.put(category, Float.valueOf(0.0f));
            this.panelHeight.put(category, Float.valueOf(60.0f));
        }
        this.createThemeColors.add(new Color(151, 84, 255));
    }

    @Override
    public void initGui() {
        this.lastFrame = System.nanoTime();
        this.transition = 0.0f;
        this.closing = false;
    }

    @Override
    public void drawScreen(int n, int n2, float f) {
        this.lastMouseX = n;
        this.lastMouseY = n2;
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        long l = System.nanoTime();
        float f2 = Math.max(0.001f, Math.min(0.05f, (float)(l - this.lastFrame) / 1.0E9f));
        this.lastFrame = l;
        this.frameDt = f2;
        float f3 = this.closing ? 0.0f : 1.0f;
        this.transition += (f3 - this.transition) * (1.0f - (float)Math.exp(-9.0f * f2));
        if (Math.abs(this.transition - f3) < 0.002f) {
            this.transition = f3;
        }
        float f4 = Math.max(0.0f, Math.min(1.0f, this.transition));
        this.screenProgress = f4 * f4 * f4 * (f4 * (f4 * 6.0f - 15.0f) + 10.0f);
        Category[] categoryArray = Category.VALUES;
        float f5 = (float)(categoryArray.length - 1) * 135.0f + 125.0f;
        this.guiX = ((float)scaledResolution.getScaledWidth() - f5) * 0.5f;
        float f6 = ((float)scaledResolution.getScaledHeight() - 260.0f) * 0.5f;
        float f7 = (float)scaledResolution.getScaledHeight() - 326.0f - 6.0f;
        float f8 = Math.max(4.0f, Math.min(f6, f7));
        float f9 = f8 + 260.0f + 105.0f;
        this.renderOffset = -(1.0f - this.screenProgress) * f9;
        this.guiY = f8 + this.renderOffset;
        if (this.closing && this.transition <= 0.002f) {
            ClickGui.saveNew();
            this.mc.displayGuiScreen(null);
            return;
        }
        WyvernBlur.prepare();
        for (int i = 0; i < categoryArray.length; ++i) {
            this.renderCategory(categoryArray[i], i, n, n2, f2, scaledResolution);
        }
        this.renderSearch(scaledResolution, n, n2);
        this.renderThemeStrip(scaledResolution, n, n2, f2);
        if (this.modal != 0) {
            this.renderThemeModal(scaledResolution, n, n2);
        }
        this.updateNumberDrag(n);
        super.drawScreen(n, n2, f);
    }

    private void renderCategory(Category category, int n, int n2, int n3, float f, ScaledResolution scaledResolution) {
        List<Module> list = this.filtered(category);
        float f2 = Math.max(60.0f, Math.min(260.0f, 22.0f + this.totalHeight(list)));
        float f3 = this.panelHeight.getOrDefault((Object)category, Float.valueOf(f2)).floatValue();
        if (Math.abs((f3 += (f2 - f3) * (1.0f - (float)Math.exp(-14.0f * f))) - f2) < 0.15f) {
            f3 = f2;
        }
        this.panelHeight.put(category, Float.valueOf(f3));
        float f4 = this.guiX + (float)n * 135.0f;
        float f5 = this.guiY;
        WyvernBlur.draw(f4, f5, 125.0f, f3, 4.0f, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), this.screenProgress);
        SmoothRender.roundedRect(f4, f5, 125.0f, f3, 4.0f, new Color(BODY.getRed(), BODY.getGreen(), BODY.getBlue(), (int)((float)BODY.getAlpha() * this.screenProgress)));
        SmoothRender.roundedRect(f4, f5, 125.0f, 18.0f, 4.0f, new Color(HEADER.getRed(), HEADER.getGreen(), HEADER.getBlue(), (int)((float)HEADER.getAlpha() * this.screenProgress)));
        this.drawCornerShade(f4, f5, 125.0f, f3);
        FontUtil fontUtil = GuiSettings.getNormalFont();
        String string = category.name();
        float f6 = fontUtil.getWidth(string);
        float f7 = 3.0f;
        float f8 = f7 + 4.0f + f6;
        SmoothRender.roundedRect(f4 + 62.5f - f8 / 2.0f, f5 + 7.0f, f7, f7, 1.5f, this.themeColor((float)n * 0.65f));
        fontUtil.drawString(string, f4 + 62.5f - f8 / 2.0f + f7 + 4.0f, f5 + 5.0f, MUTED);
        float f9 = f5 + 18.0f;
        float f10 = f3 - 18.0f - 2.0f;
        this.clampScroll(category, list, f10 - 5.0f);
        float f11 = this.scroll.get((Object)category).floatValue();
        float f12 = this.scrollTarget.get((Object)category).floatValue();
        float f13 = this.scrollVelocity.getOrDefault((Object)category, Float.valueOf(0.0f)).floatValue();
        float f14 = Math.min(0.05f, Math.max(0.001f, f));
        float f15 = 460.0f;
        float f16 = 26.0f;
        f13 += ((f12 - f11) * f15 - f13 * f16) * f14;
        f11 += f13 * f14;
        if (Math.abs(f12 - f11) < 0.05f && Math.abs(f13) < 0.6f) {
            f11 = f12;
            f13 = 0.0f;
        }
        this.scrollVelocity.put(category, Float.valueOf(f13));
        this.scroll.put(category, Float.valueOf(f11));
        this.enableScissor(f4, f9, 125.0f, f10, scaledResolution);
        float f17 = f9 + 2.0f + f11;
        Iterator<Module> iterator = list.iterator();
        while (iterator.hasNext()) {
            Module module;
            float f18 = this.animate(this.openAnim, module, this.opened.getOrDefault(module = iterator.next(), false) != false ? 1.0f : 0.0f, f, 12.0f);
            float f19 = this.animate(this.enabledAnim, module, module.isEnabled() ? 1.0f : 0.0f, f, 14.0f);
            float f20 = 19.0f + this.settingsHeight(module) * f18;
            if (f17 + f20 >= f9 && f17 <= f9 + f10) {
                this.renderModule(module, f4, f17, f20, f18, f19, n2, n3, scaledResolution, f9, f10);
            }
            f17 += 4.0f + f20;
        }
        GL11.glDisable(3089);
    }

    private void renderModule(Module module, float f, float f2, float f3, float f4, float f5, int n, int n2, ScaledResolution scaledResolution, float f6, float f7) {
        float f8;
        boolean bl = NewClickGui.inside(n, n2, f + 2.5f, f2, 120.0, Math.min(f3, 19.0f));
        Color color = this.themeColor((f2 - this.guiY) * 0.018f);
        Color color2 = bl ? new Color(255, 255, 255, 22) : CARD;
        Color color3 = NewClickGui.mix(color2, new Color(color.getRed(), color.getGreen(), color.getBlue(), 92), f5);
        SmoothRender.roundedRect(f + 2.5f, f2, 120.0f, f3, 2.0f, color3);
        SmoothRender.roundedOutline(f + 2.5f, f2, 120.0f, f3, 2.0f, 0.55f, new Color(0, 0, 0, 105), color3);
        float f9 = f + 9.0f;
        if (f5 > 0.01f) {
            SmoothRender.roundedRect(f + 7.0f, f2 + 8.0f, 3.0f, 3.0f, 1.5f, new Color(color.getRed(), color.getGreen(), color.getBlue(), (int)(255.0f * f5)));
            f9 += 6.5f * f5;
        }
        float f10 = f + 125.0f - 8.0f;
        float f11 = this.hasSettings(module) ? GuiSettings.getNormalFont().getWidth("\u2022\u2022\u2022") : 0.0f;
        float f12 = f8 = this.hasSettings(module) ? f10 - f11 : f10;
        if (this.hasSettings(module)) {
            GuiSettings.getNormalFont().drawString("\u2022\u2022\u2022", f8, f2 + 6.0f, new Color(210, 210, 218));
        }
        String string = this.bindingModule == module ? "[...]" : (module.isBound() ? "[" + module.getKeyName() + ":" + (module.isHoldMode() ? "H" : "T") + "]" : "");
        float f13 = string.isEmpty() ? 0.0f : GuiSettings.getNormalFont().getWidth(string);
        float f14 = this.hasSettings(module) ? f8 - 5.0f : f10;
        float f15 = f14 - f13;
        if (!string.isEmpty()) {
            GuiSettings.getNormalFont().drawString(string, f15, f2 + 6.5f, this.bindingModule == module ? TEXT : MUTED);
        }
        float f16 = !string.isEmpty() ? f15 : (this.hasSettings(module) ? f8 : f10);
        float f17 = f16 - f9 - 4.0f;
        String string2 = NewClickGui.fitText(module.getName(), f17);
        GuiSettings.getNormalFont().drawString(string2, f9, f2 + 6.5f, module.isEnabled() ? TEXT : MUTED);
        if (f4 > 0.001f && f3 > 19.0f) {
            float f18 = Math.max(f6, f2 + 19.0f);
            float f19 = Math.min(f6 + f7, f2 + f3);
            if (f19 > f18) {
                this.enableScissor(f + 2.5f, f18, 120.0f, f19 - f18, scaledResolution);
                this.renderSettings(module, f, f2 + 19.0f + 2.0f + (1.0f - f4) * 3.0f, f4, f2 + f3, n, n2);
                this.enableScissor(f, f6, 125.0f, f7, scaledResolution);
            }
        }
    }

    private void renderSettings(Module module, float f, float f2, float f3, float f4, int n, int n2) {
        Object object = null;
        Object object2 = null;
        float f5 = 0.0f;
        float f6 = 0.0f;
        float f7 = 0.0f;
        float f8 = 0.0f;
        for (Setting setting : this.visibleValues(module)) {
            float f9 = this.valueHeight(setting);
            if (f2 >= f4) break;
            if (setting.getType() == SettingType.BOOLEAN) {
                BooleanSetting booleanSetting = (BooleanSetting)setting;
                object = booleanSetting;
                var18_21 = booleanSetting.getValue() != false ? 1.0f : 0.0f;
                var19_22 = this.booleanAnim.getOrDefault(booleanSetting, Float.valueOf(var18_21)).floatValue();
                if (Math.abs((var19_22 += (var18_21 - var19_22) * (1.0f - (float)Math.exp(-18.0f * this.frameDt))) - var18_21) < 0.01f) {
                    var19_22 = var18_21;
                }
                f5 = var18_21;
                this.booleanAnim.put(booleanSetting, Float.valueOf(var19_22));
                GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 2.5f, NewClickGui.fade(TEXT, f3));
                f6 = f + 125.0f - 24.0f;
                f7 = f2 + 1.5f;
                f8 = 17.0f;
                float f10 = 8.0f;
                Color color = new Color(28, 28, 38, (int)(230.0f * f3));
                Color color2 = NewClickGui.fade(this.accent(), f3);
                Color color3 = NewClickGui.mix(color, color2, var19_22);
                SmoothRender.roundedRect(f6 - 0.75f, f7 - 0.75f, f8 + 1.5f, f10 + 1.5f, 4.75f, new Color(0, 0, 0, (int)(90.0f * f3)));
                SmoothRender.roundedRect(f6, f7, f8, f10, 4.0f, color3);
                float f11 = f6 + 1.0f + (f8 - 7.0f) * var19_22;
                SmoothRender.roundedRect(f11, f7 + 1.0f, 6.0f, 6.0f, 3.0f, NewClickGui.fade(Color.WHITE, f3));
            } else if (setting.getType() == SettingType.NUMBER) {
                object = (NumberSetting)setting;
                GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 1.5f, NewClickGui.fade(TEXT, f3));
                String string = NewClickGui.format(((Number)((NumberSetting)object).getValue()).doubleValue());
                GuiSettings.getNormalFont().drawString(string, f + 125.0f - 7.0f - GuiSettings.getNormalFont().getWidth(string), f2 + 1.5f, NewClickGui.fade(MUTED, f3));
                f5 = f + 7.0f;
                f6 = f2 + 11.0f;
                f7 = 108.0f;
                SmoothRender.roundedRect(f5, f6, f7, 4.5f, 1.0f, new Color(10, 10, 15, (int)(220.0f * f3)));
                f8 = this.numberPos((NumberSetting)object);
                SmoothRender.roundedRect(f5, f6, f7 * f8, 4.5f, 1.0f, NewClickGui.fade(this.accent(), f3));
                SmoothRender.roundedRect(f5 + f7 * f8 - 3.0f, f6 - 0.75f, 6.0f, 6.0f, 3.0f, NewClickGui.fade(Color.WHITE, f3));
            } else if (setting.getType() == SettingType.ENUM || setting.getType() == SettingType.MODE) {
                object = setting.getType() == SettingType.ENUM ? ((Enum)((EnumSetting)setting).getValue()).name() : ((ModuleMode)((ModeSetting)setting).getValue()).getName();
                GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 2.0f, NewClickGui.fade(TEXT, f3));
                float f12 = GuiSettings.getNormalFont().getWidth((String)object) + 8.0f;
                WyvernBlur.draw(f + 125.0f - 7.0f - f12, f2, f12, 11.0f, 2.0f, new ScaledResolution(this.mc).getScaledWidth(), new ScaledResolution(this.mc).getScaledHeight(), f3);
                SmoothRender.roundedRect(f + 125.0f - 7.0f - f12, f2, f12, 11.0f, 2.0f, new Color(10, 10, 15, (int)(180.0f * f3)));
                GuiSettings.getNormalFont().drawString((String)object, f + 125.0f - 3.0f - f12, f2 + 3.0f, NewClickGui.fade(MUTED, f3));
            } else if (setting.getType() == SettingType.TEXT) {
                object = (TextSetting)setting;
                if (NewClickGui.isBindText((TextSetting)object)) {
                    object2 = this.editingText == object ? "..." : NewClickGui.normalizeBindName(((TextSetting)object).getValue());
                    f5 = NewClickGui.bindBoxWidth((String)object2);
                    f6 = f + 125.0f - 7.0f - f5;
                    GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 2.0f, NewClickGui.fade(TEXT, f3));
                    SmoothRender.roundedRect(f6, f2, f5, 11.0f, 2.0f, new Color(10, 10, 15, (int)(210.0f * f3)));
                    GuiSettings.getNormalFont().drawString((String)object2, f6 + 4.0f, f2 + 3.0f, NewClickGui.fade(this.editingText == object ? TEXT : MUTED, f3));
                } else {
                    GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 1.0f, NewClickGui.fade(TEXT, f3));
                    SmoothRender.roundedRect(f + 7.0f, f2 + 10.0f, 88.0f, 10.0f, 2.0f, new Color(10, 10, 15, (int)(190.0f * f3)));
                    object2 = this.editingText == object ? ((TextSetting)object).getValue() + "|" : ((TextSetting)object).getValue();
                    GuiSettings.getNormalFont().drawString((String)object2, f + 10.0f, f2 + 12.0f, NewClickGui.fade(MUTED, f3));
                }
            } else if (setting.getType() == SettingType.MULTI) {
                MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
                GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 1.0f, NewClickGui.fade(TEXT, f3));
                var18_21 = f + 7.0f;
                var19_22 = f2 + 11.0f;
                for (String string : multiSelectSetting.getOptions()) {
                    boolean bl;
                    float f12 = GuiSettings.getNormalFont().getWidth(string) + 10.0f;
                    if (var18_21 + f12 > f + 118.0f) {
                        var18_21 = f + 7.0f;
                        var19_22 += 13.0f;
                    }
                    Color color = (bl = multiSelectSetting.isSelected(string)) ? NewClickGui.fade(this.accent(), f3) : new Color(24, 24, 32, (int)(210.0f * f3));
                    SmoothRender.roundedRect(var18_21, var19_22, f12, 11.0f, 3.0f, color);
                    if (bl) {
                        SmoothRender.roundedRect(var18_21, var19_22, f12, 11.0f, 3.0f, new Color(255, 255, 255, (int)(28.0f * f3)));
                    }
                    GuiSettings.getNormalFont().drawString(string, var18_21 + 5.0f, var19_22 + 2.5f, bl ? NewClickGui.fade(Color.WHITE, f3) : NewClickGui.fade(MUTED, f3));
                    var18_21 += f12 + 4.0f;
                }
            } else if (setting.getType() == SettingType.COLOR || setting.getType() == SettingType.COLOR_ALPHA) {
                object = (ColorSetting)setting;
                GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 1.0f, NewClickGui.fade(TEXT, f3));
                SmoothRender.roundedRect(f + 125.0f - 21.0f, f2, 14.0f, 9.0f, 2.0f, NewClickGui.fade(((ColorSetting)object).getValue(), f3));
                if (this.colorExpanded.getOrDefault(object, false).booleanValue()) {
                    ((ColorSetting)object).picker().draw((int)f + 8, (int)f2 + 12, 68, 70, n, n2, ((ColorSetting)object).getValue());
                }
            } else {
                GuiSettings.getNormalFont().drawString(setting.getName(), f + 8.5f, f2 + 2.0f, NewClickGui.fade(MUTED, f3));
            }
            f2 += f9 + 2.5f;
        }
    }

    private void renderSearch(ScaledResolution scaledResolution, int n, int n2) {
        float f = this.guiX + ((float)(Category.VALUES.length - 1) * 135.0f + 125.0f) / 2.0f - 60.0f;
        float f2 = this.guiY + 260.0f + 8.0f;
        WyvernBlur.draw(f, f2, 120.0f, 22.0f, 4.0f, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), this.screenProgress);
        SmoothRender.roundedRect(f, f2, 120.0f, 22.0f, 4.0f, new Color(10, 10, 15, (int)(205.0f * this.screenProgress)));
        String string = this.search.isEmpty() && !this.searchActive ? "Search modules..." : this.search;
        GuiSettings.getNormalFont().drawString(string, f + 8.0f, f2 + 8.0f, this.search.isEmpty() ? MUTED : TEXT);
        if (this.searchActive && System.currentTimeMillis() / 500L % 2L == 0L) {
            float f3 = f + 8.0f + GuiSettings.getNormalFont().getWidth(this.search);
            SmoothRender.roundedRect(f3, f2 + 6.5f, 1.0f, 9.0f, 0.5f, Color.WHITE);
        }
    }

    private Color accent() {
        return this.themeColor(0.0f);
    }

    public static Color getSyncedThemeColor(float f) {
        try {
            NewClickGui newClickGui = Primordial.newClickGui;
            if (newClickGui != null) {
                return newClickGui.themeColor(f);
            }
        }
        catch (Throwable throwable) {
            // empty catch block
        }
        return ACCENT;
    }

    private Color themeColor(float f) {
        List<Color> list = this.themeManager.current().getColors();
        if (list.isEmpty()) {
            return Color.WHITE;
        }
        if (list.size() == 1) {
            return list.get(0);
        }
        float f2 = (float)(System.currentTimeMillis() % 7000L) / 7000.0f * (float)list.size() + f;
        int n = (int)Math.floor(f2);
        float f3 = f2 - (float)Math.floor(f2);
        f3 = f3 * f3 * (3.0f - 2.0f * f3);
        Color color = list.get(Math.floorMod(n, list.size()));
        Color color2 = list.get(Math.floorMod(n + 1, list.size()));
        return NewClickGui.mix(color, color2, f3);
    }

    private void drawCornerShade(float f, float f2, float f3, float f4) {
        SmoothRender.roundedOutline(f, f2, f3, f4, 4.0f, 0.9f, new Color(0, 0, 0, (int)(115.0f * this.screenProgress)), new Color(0, 0, 0, 0));
        Color color = new Color(0, 0, 0, (int)(34.0f * this.screenProgress));
        float f5 = 11.0f;
        SmoothRender.roundedRect(f, f2, f5, f5, 4.0f, color);
        SmoothRender.roundedRect(f + f3 - f5, f2, f5, f5, 4.0f, color);
        SmoothRender.roundedRect(f, f2 + f4 - f5, f5, f5, 4.0f, color);
        SmoothRender.roundedRect(f + f3 - f5, f2 + f4 - f5, f5, f5, 4.0f, color);
    }

    private float themeBarX() {
        return this.guiX + ((float)(Category.VALUES.length - 1) * 135.0f + 125.0f) / 2.0f - 125.0f;
    }

    private float themeBarY() {
        return this.guiY + 260.0f + 38.0f;
    }

    private void renderThemeStrip(ScaledResolution scaledResolution, int n, int n2, float f) {
        float f2 = this.themeBarX();
        float f3 = this.themeBarY();
        float f4 = 250.0f;
        float f5 = 28.0f;
        WyvernBlur.draw(f2, f3, f4, f5, 4.0f, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), this.screenProgress);
        SmoothRender.roundedRect(f2, f3, f4, f5, 4.0f, new Color(10, 10, 15, (int)(205.0f * this.screenProgress)));
        float f6 = (float)this.themeManager.themes().size() * 20.0f + 20.0f;
        float f7 = Math.min(0.0f, f4 - 12.0f - f6);
        this.themeScrollTarget = Math.max(f7, Math.min(0.0f, this.themeScrollTarget));
        this.themeScroll += (this.themeScrollTarget - this.themeScroll) * (1.0f - (float)Math.exp(-14.0f * f));
        this.enableScissor(f2 + 4.0f, f3 + 2.0f, f4 - 8.0f, f5 - 4.0f, scaledResolution);
        float f8 = f2 + 6.0f + this.themeScroll;
        String string = null;
        for (int i = 0; i < this.themeManager.themes().size(); ++i) {
            boolean bl;
            WyvernTheme wyvernTheme = this.themeManager.themes().get(i);
            boolean bl2 = bl = i == this.themeManager.currentIndex();
            if (bl) {
                SmoothRender.roundedRect(f8 - 2.0f, f3 + 5.0f, 18.0f, 18.0f, 3.0f, new Color(255, 255, 255, 80));
            }
            this.drawThemeSquare(wyvernTheme, f8, f3 + 7.0f, 14.0f, 14.0f);
            if (NewClickGui.inside(n, n2, f8, f3 + 7.0f, 14.0, 14.0)) {
                string = wyvernTheme.getName();
            }
            f8 += 20.0f;
        }
        SmoothRender.roundedRect(f8, f3 + 7.0f, 14.0f, 14.0f, 3.0f, new Color(255, 255, 255, 24));
        GuiSettings.getNormalFont().drawString("+", f8 + 4.5f, f3 + 10.0f, TEXT);
        GL11.glDisable(3089);
        if (string != null) {
            float f9 = GuiSettings.getNormalFont().getWidth(string) + 8.0f;
            SmoothRender.roundedRect(n + 5, n2 - 13, f9, 12.0f, 3.0f, new Color(8, 8, 12, 225));
            GuiSettings.getNormalFont().drawString(string, (float)(n + 9), (float)(n2 - 10), TEXT);
        }
    }

    private void drawThemeSquare(WyvernTheme wyvernTheme, float f, float f2, float f3, float f4) {
        List<Color> list = wyvernTheme.getColors();
        float f5 = f3 / (float)list.size();
        for (int i = 0; i < list.size(); ++i) {
            Color color = list.get(i);
            float f6 = f + (float)i * f5;
            SmoothRender.roundedRect(f6, f2, i == list.size() - 1 ? f + f3 - f6 : f5 + 0.3f, f4, i == 0 || i == list.size() - 1 ? 2.0f : 0.0f, color);
        }
    }

    private void beginCreateTheme() {
        this.modal = 1;
        this.createThemeName = "Custom " + (this.themeManager.themes().size() + 1);
        this.createThemeColors.clear();
        this.createThemeColors.add(this.accent());
        this.createThemeColors.add(this.themeManager.current().secondary());
        this.selectedCreateColor = 0;
    }

    private void renderThemeModal(ScaledResolution scaledResolution, int n, int n2) {
        float f = this.modal == 1 ? 240.0f : 205.0f;
        float f2 = this.modal == 1 ? 170.0f : 88.0f;
        float f3 = ((float)scaledResolution.getScaledWidth() - f) / 2.0f;
        float f4 = ((float)scaledResolution.getScaledHeight() - f2) / 2.0f;
        WyvernBlur.draw(f3, f4, f, f2, 6.0f, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), 1.0f);
        SmoothRender.roundedRect(f3, f4, f, f2, 6.0f, new Color(9, 9, 14, 235));
        if (this.modal == 2) {
            WyvernTheme wyvernTheme = this.deleteTheme >= 0 && this.deleteTheme < this.themeManager.themes().size() ? this.themeManager.themes().get(this.deleteTheme) : this.themeManager.current();
            GuiSettings.getCategoryFont().drawString("Delete theme?", f3 + 12.0f, f4 + 10.0f, TEXT);
            GuiSettings.getNormalFont().drawString(wyvernTheme.getName(), f3 + 12.0f, f4 + 30.0f, MUTED);
            this.drawThemeSquare(wyvernTheme, f3 + f - 34.0f, f4 + 24.0f, 18.0f, 18.0f);
            this.button(f3 + 12.0f, f4 + f2 - 25.0f, 82.0f, 16.0f, "NO", NewClickGui.inside(n, n2, f3 + 12.0f, f4 + f2 - 25.0f, 82.0, 16.0));
            this.button(f3 + f - 94.0f, f4 + f2 - 25.0f, 82.0f, 16.0f, "YES", NewClickGui.inside(n, n2, f3 + f - 94.0f, f4 + f2 - 25.0f, 82.0, 16.0));
            return;
        }
        GuiSettings.getCategoryFont().drawString("Create theme", f3 + 12.0f, f4 + 9.0f, TEXT);
        SmoothRender.roundedRect(f3 + 12.0f, f4 + 27.0f, f - 24.0f, 16.0f, 3.0f, new Color(20, 20, 28, 230));
        GuiSettings.getNormalFont().drawString(this.createThemeName + (System.currentTimeMillis() / 500L % 2L == 0L ? "|" : ""), f3 + 17.0f, f4 + 32.0f, TEXT);
        float f5 = f3 + 12.0f;
        for (int i = 0; i < this.createThemeColors.size(); ++i) {
            Color color = this.createThemeColors.get(i);
            if (i == this.selectedCreateColor) {
                SmoothRender.roundedRect(f5 - 2.0f, f4 + 48.0f, 18.0f, 18.0f, 3.0f, new Color(255, 255, 255, 75));
            }
            SmoothRender.roundedRect(f5, f4 + 50.0f, 14.0f, 14.0f, 3.0f, color);
            f5 += 20.0f;
        }
        SmoothRender.roundedRect(f5, f4 + 50.0f, 14.0f, 14.0f, 3.0f, new Color(255, 255, 255, 24));
        GuiSettings.getNormalFont().drawString("+", f5 + 4.5f, f4 + 53.0f, TEXT);
        this.drawColorEditor(f3 + 12.0f, f4 + 72.0f, 160.0f, 58.0f, this.createThemeColors.get(this.selectedCreateColor));
        this.button(f3 + 12.0f, f4 + f2 - 25.0f, 82.0f, 16.0f, "CANCEL", NewClickGui.inside(n, n2, f3 + 12.0f, f4 + f2 - 25.0f, 82.0, 16.0));
        this.button(f3 + f - 94.0f, f4 + f2 - 25.0f, 82.0f, 16.0f, "SAVE", NewClickGui.inside(n, n2, f3 + f - 94.0f, f4 + f2 - 25.0f, 82.0, 16.0));
        this.updateColorDrag(n, n2, f3 + 12.0f, f4 + 72.0f, 160.0f, 58.0f);
    }

    private void drawColorEditor(float f, float f2, float f3, float f4, Color color) {
        float f5;
        int n;
        float[] fArray = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        float f6 = fArray[0];
        int n2 = 20;
        int n3 = 8;
        for (n = 0; n < n3; ++n) {
            for (int i = 0; i < n2; ++i) {
                f5 = (float)i / (float)(n2 - 1);
                float f7 = 1.0f - (float)n / (float)(n3 - 1);
                Color color2 = Color.getHSBColor(f6, f5, f7);
                RenderUtils.drawFixedRect(f + (float)i * f3 / (float)n2, f2 + (float)n * (f4 - 10.0f) / (float)n3, (double)(f + (float)(i + 1) * f3 / (float)n2) + 0.5, (double)(f2 + (float)(n + 1) * (f4 - 10.0f) / (float)n3) + 0.5, color2);
            }
        }
        for (n = 0; n < 30; ++n) {
            Color color3 = Color.getHSBColor((float)n / 30.0f, 1.0f, 1.0f);
            RenderUtils.drawFixedRect(f + (float)n * f3 / 30.0f, f2 + f4 - 8.0f, (double)(f + (float)(n + 1) * f3 / 30.0f) + 0.5, f2 + f4, color3);
        }
        float f8 = f + fArray[1] * f3;
        f5 = f2 + (1.0f - fArray[2]) * (f4 - 10.0f);
        SmoothRender.roundedOutline(f8 - 2.0f, f5 - 2.0f, 4.0f, 4.0f, 2.0f, 0.7f, Color.WHITE, new Color(0, 0, 0, 0));
    }

    private void updateColorDrag(int n, int n2, float f, float f2, float f3, float f4) {
        if (!Mouse.isButtonDown(0)) {
            this.draggingPalette = false;
            this.draggingHue = false;
            return;
        }
        Color color = this.createThemeColors.get(this.selectedCreateColor);
        float[] fArray = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
        if (this.draggingPalette || NewClickGui.inside(n, n2, f, f2, f3, f4 - 10.0f)) {
            this.draggingPalette = true;
            float f5 = Math.max(0.0f, Math.min(1.0f, ((float)n - f) / f3));
            float f6 = 1.0f - Math.max(0.0f, Math.min(1.0f, ((float)n2 - f2) / (f4 - 10.0f)));
            this.createThemeColors.set(this.selectedCreateColor, Color.getHSBColor(fArray[0], f5, f6));
        } else if (this.draggingHue || NewClickGui.inside(n, n2, f, f2 + f4 - 8.0f, f3, 8.0)) {
            this.draggingHue = true;
            float f7 = Math.max(0.0f, Math.min(1.0f, ((float)n - f) / f3));
            this.createThemeColors.set(this.selectedCreateColor, Color.getHSBColor(f7, fArray[1], fArray[2]));
        }
    }

    private void button(float f, float f2, float f3, float f4, String string, boolean bl) {
        SmoothRender.roundedRect(f, f2, f3, f4, 3.0f, bl ? new Color(this.accent().getRed(), this.accent().getGreen(), this.accent().getBlue(), 100) : new Color(255, 255, 255, 18));
        GuiSettings.getNormalFont().drawString(string, f + (f3 - GuiSettings.getNormalFont().getWidth(string)) / 2.0f, f2 + 5.0f, bl ? TEXT : MUTED);
    }

    private boolean clickThemeUi(int n, int n2, int n3, ScaledResolution scaledResolution) {
        float f;
        if (this.modal != 0) {
            return this.clickModal(n, n2, n3, scaledResolution);
        }
        float f2 = this.themeBarX();
        if (!NewClickGui.inside(n, n2, f2, f = this.themeBarY(), 250.0, 28.0)) {
            return false;
        }
        float f3 = (float)n - (f2 + 6.0f + this.themeScroll);
        int n4 = (int)Math.floor(f3 / 20.0f);
        if (n4 < 0) {
            return true;
        }
        if (n4 < this.themeManager.themes().size()) {
            if (n3 == 0) {
                this.themeManager.select(n4);
            } else if (n3 == 1 && this.themeManager.themes().size() > 1) {
                this.modal = 2;
                this.deleteTheme = n4;
            }
            return true;
        }
        if (n4 == this.themeManager.themes().size() && n3 == 0) {
            this.beginCreateTheme();
            return true;
        }
        return true;
    }

    private boolean clickModal(int n, int n2, int n3, ScaledResolution scaledResolution) {
        float f = this.modal == 1 ? 240.0f : 205.0f;
        float f2 = this.modal == 1 ? 170.0f : 88.0f;
        float f3 = ((float)scaledResolution.getScaledWidth() - f) / 2.0f;
        float f4 = ((float)scaledResolution.getScaledHeight() - f2) / 2.0f;
        if (n3 != 0 && n3 != 1) {
            return true;
        }
        if (this.modal == 2) {
            if (n3 == 0 && NewClickGui.inside(n, n2, f3 + 12.0f, f4 + f2 - 25.0f, 82.0, 16.0)) {
                this.modal = 0;
                return true;
            }
            if (n3 == 0 && NewClickGui.inside(n, n2, f3 + f - 94.0f, f4 + f2 - 25.0f, 82.0, 16.0)) {
                this.themeManager.delete(this.deleteTheme);
                this.modal = 0;
                return true;
            }
            return true;
        }
        float f5 = f3 + 12.0f;
        for (int i = 0; i < this.createThemeColors.size(); ++i) {
            if (NewClickGui.inside(n, n2, f5, f4 + 48.0f, 18.0, 18.0)) {
                if (n3 == 1 && this.createThemeColors.size() > 1) {
                    this.createThemeColors.remove(i);
                    this.selectedCreateColor = Math.max(0, Math.min(this.selectedCreateColor, this.createThemeColors.size() - 1));
                } else {
                    this.selectedCreateColor = i;
                }
                return true;
            }
            f5 += 20.0f;
        }
        if (n3 == 0 && NewClickGui.inside(n, n2, f5, f4 + 48.0f, 18.0, 18.0)) {
            Color color = this.createThemeColors.get(this.createThemeColors.size() - 1);
            this.createThemeColors.add(color.brighter());
            this.selectedCreateColor = this.createThemeColors.size() - 1;
            return true;
        }
        if (n3 == 0 && NewClickGui.inside(n, n2, f3 + 12.0f, f4 + f2 - 25.0f, 82.0, 16.0)) {
            this.modal = 0;
            return true;
        }
        if (n3 == 0 && NewClickGui.inside(n, n2, f3 + f - 94.0f, f4 + f2 - 25.0f, 82.0, 16.0)) {
            Object object = this.createThemeName.trim();
            if (((String)object).isEmpty()) {
                object = "Custom " + (this.themeManager.themes().size() + 1);
            }
            this.themeManager.add(new WyvernTheme((String)object, this.createThemeColors));
            this.modal = 0;
            return true;
        }
        return true;
    }

    @Override
    protected void mouseClicked(int n, int n2, int n3) throws IOException {
        if (this.closing) {
            return;
        }
        if (this.editingText != null && NewClickGui.isBindText(this.editingText)) {
            this.editingText.setValue(NewClickGui.mouseBindName(n3));
            this.editingText = null;
            ClickGui.saveNew();
            return;
        }
        if (this.bindingModule != null) {
            this.bindingModule.setKey(n3 - 100);
            this.bindingModule.wasKeyDown = false;
            this.bindingModule = null;
            ClickGui.saveNew();
            return;
        }
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        if (this.clickThemeUi(n, n2, n3, scaledResolution)) {
            return;
        }
        float f = this.guiX + ((float)(Category.VALUES.length - 1) * 135.0f + 125.0f) / 2.0f - 60.0f;
        float f2 = this.guiY + 260.0f + 8.0f;
        if (NewClickGui.inside(n, n2, f, f2, 120.0, 22.0)) {
            this.searchActive = true;
            this.editingText = null;
            return;
        }
        this.searchActive = false;
        for (int i = 0; i < Category.VALUES.length; ++i) {
            float f3 = this.guiX + (float)i * 135.0f;
            float f4 = this.guiY;
            Category category = Category.VALUES[i];
            float f5 = this.panelHeight.getOrDefault((Object)category, Float.valueOf(60.0f)).floatValue();
            if (!NewClickGui.inside(n, n2, f3, f4, 125.0, f5)) continue;
            float f6 = f4 + 18.0f + 2.0f + this.scroll.get((Object)category).floatValue();
            Iterator<Module> iterator = this.filtered(category).iterator();
            while (iterator.hasNext()) {
                Module module;
                float f7 = this.openAnim.getOrDefault(module, Float.valueOf(this.opened.getOrDefault(module = iterator.next(), false) != false ? 1.0f : 0.0f)).floatValue();
                float f8 = 19.0f + this.settingsHeight(module) * f7;
                if (NewClickGui.inside(n, n2, f3 + 2.5f, f6, 120.0, 19.0)) {
                    float f9;
                    float f10 = f3 + 125.0f - 8.0f;
                    float f11 = this.hasSettings(module) ? GuiSettings.getNormalFont().getWidth("\u2022\u2022\u2022") : 0.0f;
                    float f12 = f9 = this.hasSettings(module) ? f10 - f11 : f10;
                    String string = this.bindingModule == module ? "[...]" : (module.isBound() ? "[" + module.getKeyName() + ":" + (module.isHoldMode() ? "H" : "T") + "]" : "");
                    float f13 = string.isEmpty() ? 0.0f : GuiSettings.getNormalFont().getWidth(string);
                    float f14 = this.hasSettings(module) ? f9 - 5.0f : f10;
                    float f15 = f14 - f13;
                    if (this.hasSettings(module) && NewClickGui.inside(n, n2, f9 - 3.0f, f6 + 2.0f, f11 + 6.0f, 15.0) && (n3 == 0 || n3 == 1)) {
                        this.opened.put(module, this.opened.getOrDefault(module, false) == false);
                        return;
                    }
                    if (!string.isEmpty() && NewClickGui.inside(n, n2, f15 - 3.0f, f6 + 2.0f, f13 + 6.0f, 15.0) && n3 == 0) {
                        boolean bl;
                        boolean bl2 = bl = !module.isHoldMode();
                        if (bl && module.isEnabled()) {
                            module.toggle();
                        }
                        module.wasKeyDown = false;
                        module.setHoldMode(bl);
                        ClickGui.saveNew();
                        return;
                    }
                    if (n3 == 0) {
                        module.toggle();
                    } else if (n3 == 1 && this.hasSettings(module)) {
                        this.opened.put(module, this.opened.getOrDefault(module, false) == false);
                    } else if (n3 == 2) {
                        this.bindingModule = module;
                    }
                    return;
                }
                if (this.opened.getOrDefault(module, false).booleanValue() && f7 > 0.5f && this.handleSettingClick(module, f3, f6 + 17.5f, n, n2, n3)) {
                    return;
                }
                f6 += 4.0f + f8;
            }
        }
        super.mouseClicked(n, n2, n3);
    }

    private boolean handleSettingClick(Module module, float f, float f2, int n, int n2, int n3) {
        for (Setting setting : this.visibleValues(module)) {
            float f3 = this.valueHeight(setting);
            if (NewClickGui.inside(n, n2, f + 5.0f, f2, 115.0, f3)) {
                if (setting.getType() == SettingType.BOOLEAN) {
                    ((BooleanSetting)setting).toggle();
                } else if (setting.getType() == SettingType.NUMBER) {
                    this.draggingNumber = (NumberSetting)setting;
                    this.dragX = f + 7.0f;
                    this.dragWidth = 108.0f;
                    this.updateNumberDrag(n);
                } else if (setting.getType() == SettingType.ENUM) {
                    if (n3 == 1) {
                        ((EnumSetting)setting).setPrevious();
                    } else {
                        ((EnumSetting)setting).setNext();
                    }
                } else if (setting.getType() == SettingType.MODE) {
                    if (n3 == 1) {
                        ((ModeSetting)setting).setPrevious();
                    } else {
                        ((ModeSetting)setting).setNext();
                    }
                } else if (setting.getType() == SettingType.TEXT) {
                    TextSetting textSetting = (TextSetting)setting;
                    if (NewClickGui.isBindText(textSetting)) {
                        String string = this.editingText == textSetting ? "..." : NewClickGui.normalizeBindName(textSetting.getValue());
                        float f4 = NewClickGui.bindBoxWidth(string);
                        float f5 = f + 125.0f - 7.0f - f4;
                        if (NewClickGui.inside(n, n2, f5, f2, f4, 11.0)) {
                            this.editingText = textSetting;
                            this.searchActive = false;
                        }
                    } else {
                        this.editingText = textSetting;
                        this.searchActive = false;
                    }
                } else if (setting.getType() == SettingType.MULTI) {
                    MultiSelectSetting multiSelectSetting = (MultiSelectSetting)setting;
                    float f6 = f + 7.0f;
                    float f7 = f2 + 11.0f;
                    for (String string : multiSelectSetting.getOptions()) {
                        float f8 = GuiSettings.getNormalFont().getWidth(string) + 10.0f;
                        if (f6 + f8 > f + 118.0f) {
                            f6 = f + 7.0f;
                            f7 += 13.0f;
                        }
                        if (NewClickGui.inside(n, n2, f6, f7, f8, 11.0)) {
                            multiSelectSetting.toggle(string);
                            break;
                        }
                        f6 += f8 + 4.0f;
                    }
                } else if (setting.getType() == SettingType.COLOR || setting.getType() == SettingType.COLOR_ALPHA) {
                    ColorSetting colorSetting = (ColorSetting)setting;
                    if (NewClickGui.inside(n, n2, f + 125.0f - 23.0f, f2, 18.0, 12.0)) {
                        this.colorExpanded.put(colorSetting, this.colorExpanded.getOrDefault(colorSetting, false) == false);
                    } else if (this.colorExpanded.getOrDefault(colorSetting, false).booleanValue()) {
                        colorSetting.picker().mouseClicked(n, n2, n3);
                    }
                }
                return true;
            }
            f2 += f3 + 2.5f;
        }
        return false;
    }

    @Override
    protected void mouseReleased(int n, int n2, int n3) {
        this.draggingNumber = null;
        ClickGui.saveNew();
        super.mouseReleased(n, n2, n3);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int n = Mouse.getEventDWheel();
        if (n == 0) {
            return;
        }
        if (NewClickGui.inside(this.lastMouseX, this.lastMouseY, this.themeBarX(), this.themeBarY(), 250.0, 28.0)) {
            this.themeScrollTarget += n > 0 ? 28.0f : -28.0f;
            return;
        }
        for (int i = 0; i < Category.VALUES.length; ++i) {
            Category category = Category.VALUES[i];
            float f = this.guiX + (float)i * 135.0f;
            List<Module> list = this.filtered(category);
            float f2 = this.panelHeight.getOrDefault((Object)category, Float.valueOf(60.0f)).floatValue();
            if (!NewClickGui.inside(this.lastMouseX, this.lastMouseY, f, this.guiY, 125.0, f2)) continue;
            float f3 = this.scrollTarget.get((Object)category).floatValue() + (n > 0 ? 23.0f : -23.0f);
            f3 = (float)Math.round(f3 / 23.0f) * 23.0f;
            this.scrollTarget.put(category, Float.valueOf(f3));
            break;
        }
    }

    @Override
    protected void keyTyped(char c, int n) throws IOException {
        if (this.modal != 0) {
            if (n == 1) {
                this.modal = 0;
                return;
            }
            if (this.modal == 1) {
                if (n == 14 && this.createThemeName.length() > 0) {
                    this.createThemeName = this.createThemeName.substring(0, this.createThemeName.length() - 1);
                } else if (n == 28) {
                    Object object = this.createThemeName.trim();
                    if (((String)object).isEmpty()) {
                        object = "Custom " + (this.themeManager.themes().size() + 1);
                    }
                    this.themeManager.add(new WyvernTheme((String)object, this.createThemeColors));
                    this.modal = 0;
                } else if (NewClickGui.isAllowed(c) && this.createThemeName.length() < 24) {
                    this.createThemeName = this.createThemeName + c;
                }
            }
            return;
        }
        if (this.bindingModule != null) {
            if (n == 1) {
                this.bindingModule = null;
            } else {
                this.bindingModule.setKey(n == 211 ? 0 : n);
                this.bindingModule = null;
                ClickGui.saveNew();
            }
            return;
        }
        if (this.searchActive) {
            if (n == 1) {
                this.searchActive = false;
                return;
            }
            if (n == 14 && this.search.length() > 0) {
                this.search = this.search.substring(0, this.search.length() - 1);
            } else if (n == 211 && this.search.length() > 0) {
                this.search = this.search.substring(0, this.search.length() - 1);
            } else if (NewClickGui.isAllowed(c) && this.search.length() < 24) {
                this.search = this.search + c;
            }
            return;
        }
        if (this.editingText != null) {
            if (NewClickGui.isBindText(this.editingText)) {
                if (n == 1) {
                    this.editingText = null;
                } else {
                    this.editingText.setValue(n == 211 || n == 14 ? "NONE" : NewClickGui.keyboardBindName(n));
                    this.editingText = null;
                    ClickGui.saveNew();
                }
                return;
            }
            String string = this.editingText.getValue();
            if (n == 1 || n == 28) {
                this.editingText = null;
                ClickGui.saveNew();
                return;
            }
            if (n == 14 && string.length() > 0) {
                this.editingText.setValue(string.substring(0, string.length() - 1));
            } else if (NewClickGui.isAllowed(c)) {
                this.editingText.setValue(string + c);
            }
            return;
        }
        if (n == 1) {
            this.beginClosing();
            return;
        }
        super.keyTyped(c, n);
    }

    private void beginClosing() {
        if (this.closing) {
            return;
        }
        this.closing = true;
        ClickGui.saveNew();
        try {
            this.mc.inGameHasFocus = true;
            this.mc.mouseHelper.grabMouseCursor();
        }
        catch (Throwable throwable) {
            // empty catch block
        }
    }

    @Override
    public void onGuiClosed() {
        ClickGui.saveNew();
        super.onGuiClosed();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }

    private List<Module> filtered(Category category) {
        ArrayList<Module> arrayList = new ArrayList<Module>();
        for (Module module : ModuleManager.getModulesFromCategory(category)) {
            if (!this.search.isEmpty() && !module.getName().toLowerCase(Locale.ROOT).contains(this.search.toLowerCase(Locale.ROOT))) continue;
            arrayList.add(module);
        }
        return arrayList;
    }

    private List<Setting> visibleValues(Module module) {
        ArrayList<Setting> arrayList = new ArrayList<Setting>();
        for (Setting<?> setting : SettingRegistry.getValuesFromModule(module)) {
            if (setting == null || !setting.isOpen()) continue;
            arrayList.add(setting);
        }
        return arrayList;
    }

    private boolean hasSettings(Module module) {
        return !this.visibleValues(module).isEmpty();
    }

    private float settingsHeight(Module module) {
        float f = 0.0f;
        for (Setting setting : this.visibleValues(module)) {
            f += this.valueHeight(setting) + 2.5f;
        }
        return f > 0.0f ? f + 3.0f : 0.0f;
    }

    private int chipRows(MultiSelectSetting multiSelectSetting) {
        int n = 1;
        float f = 7.0f;
        for (String string : multiSelectSetting.getOptions()) {
            float f2 = GuiSettings.getNormalFont().getWidth(string) + 10.0f;
            if (f + f2 > 118.0f) {
                f = 7.0f;
                ++n;
            }
            f += f2 + 4.0f;
        }
        return n;
    }

    private float valueHeight(Setting setting) {
        if (setting.getType() == SettingType.NUMBER) {
            return 22.0f;
        }
        if (setting.getType() == SettingType.TEXT) {
            return NewClickGui.isBindText((TextSetting)setting) ? 12.0f : 22.0f;
        }
        if (setting.getType() == SettingType.COLOR || setting.getType() == SettingType.COLOR_ALPHA) {
            return this.colorExpanded.getOrDefault((ColorSetting)setting, false) != false ? 86.0f : 12.0f;
        }
        if (setting.getType() == SettingType.ENUM || setting.getType() == SettingType.MODE) {
            return 14.0f;
        }
        if (setting.getType() == SettingType.MULTI) {
            return 12.0f + (float)this.chipRows((MultiSelectSetting)setting) * 13.0f;
        }
        return 12.0f;
    }

    private float totalHeight(List<Module> list) {
        float f = 0.0f;
        for (Module module : list) {
            f += 23.0f + this.settingsHeight(module) * this.openAnim.getOrDefault(module, Float.valueOf(this.opened.getOrDefault(module, false) != false ? 1.0f : 0.0f)).floatValue();
        }
        return f;
    }

    private void clampScroll(Category category, List<Module> list, float f) {
        float f2 = this.totalHeight(list);
        float f3 = Math.min(0.0f, f - f2);
        if (f3 >= -0.01f) {
            this.scrollTarget.put(category, Float.valueOf(0.0f));
            this.scroll.put(category, Float.valueOf(0.0f));
            return;
        }
        float f4 = Math.max(f3, Math.min(0.0f, this.scrollTarget.get((Object)category).floatValue()));
        this.scrollTarget.put(category, Float.valueOf(f4));
        float f5 = this.scroll.getOrDefault((Object)category, Float.valueOf(0.0f)).floatValue();
        float f6 = 14.0f;
        if (f5 < f3 - f6) {
            this.scroll.put(category, Float.valueOf(f3 - f6));
            this.scrollVelocity.put(category, Float.valueOf(0.0f));
        } else if (f5 > f6) {
            this.scroll.put(category, Float.valueOf(f6));
            this.scrollVelocity.put(category, Float.valueOf(0.0f));
        }
    }

    private float animate(Map<Module, Float> map, Module module, float f, float f2, float f3) {
        float f4 = map.getOrDefault(module, Float.valueOf(f)).floatValue();
        f4 += (f - f4) * (1.0f - (float)Math.exp(-f3 * f2));
        map.put(module, Float.valueOf(f4));
        return f4;
    }

    private static boolean isBindText(TextSetting textSetting) {
        return textSetting != null && textSetting.getModule() != null && "PacketPot".equalsIgnoreCase(textSetting.getModule().getName()) && textSetting.getName().endsWith(" Key");
    }

    private static String normalizeBindName(String string) {
        if (string == null || string.trim().isEmpty()) {
            return "NONE";
        }
        return string.trim().toUpperCase(Locale.ROOT);
    }

    private static float bindBoxWidth(String string) {
        return Math.max(14.0f, GuiSettings.getNormalFont().getWidth(string) + 8.0f);
    }

    private static String keyboardBindName(int n) {
        String string = Keyboard.getKeyName(n);
        return string == null || string.isEmpty() ? "NONE" : string.toUpperCase(Locale.ROOT);
    }

    private static String mouseBindName(int n) {
        if (n == 0) {
            return "LMB";
        }
        if (n == 1) {
            return "RMB";
        }
        if (n == 2) {
            return "MMB";
        }
        return "M" + (n + 1);
    }

    private float numberPos(NumberSetting numberSetting) {
        double d = ((Number)numberSetting.getMax()).doubleValue() - ((Number)numberSetting.getMin()).doubleValue();
        return d <= 0.0 ? 0.0f : (float)((((Number)numberSetting.getValue()).doubleValue() - ((Number)numberSetting.getMin()).doubleValue()) / d);
    }

    private void updateNumberDrag(int n) {
        if (this.draggingNumber == null) {
            return;
        }
        float f = Math.max(0.0f, Math.min(1.0f, ((float)n - this.dragX) / this.dragWidth));
        double d = ((Number)this.draggingNumber.getMin()).doubleValue();
        double d2 = ((Number)this.draggingNumber.getMax()).doubleValue();
        double d3 = ((Number)this.draggingNumber.getIncrement()).doubleValue();
        double d4 = d + (d2 - d) * (double)f;
        d4 = (double)Math.round(d4 / d3) * d3;
        Object t = this.draggingNumber.getIncrement();
        if (t instanceof Integer) {
            this.draggingNumber.setValue((int)Math.round(d4));
        } else if (t instanceof Float) {
            this.draggingNumber.setValue(Float.valueOf((float)d4));
        } else if (t instanceof Double) {
            this.draggingNumber.setValue(MathUtils.fixFormat(d4, 6));
        } else if (t instanceof Long) {
            this.draggingNumber.setValue(Math.round(d4));
        }
    }

    private void enableScissor(float f, float f2, float f3, float f4, ScaledResolution scaledResolution) {
        int n = scaledResolution.getScaleFactor();
        GL11.glEnable(3089);
        GL11.glScissor((int)(f * (float)n), (int)((float)this.mc.displayHeight - (f2 + f4) * (float)n), Math.max(1, (int)(f3 * (float)n)), Math.max(1, (int)(f4 * (float)n)));
    }

    private static boolean inside(double d, double d2, double d3, double d4, double d5, double d6) {
        return d >= d3 && d <= d3 + d5 && d2 >= d4 && d2 <= d4 + d6;
    }

    private static boolean isAllowed(char c) {
        return c >= ' ' && c != '\u007f';
    }

    private static String fitText(String string, float f) {
        if (f <= 4.0f) {
            return "";
        }
        if (GuiSettings.getNormalFont().getWidth(string) <= f) {
            return string;
        }
        String string2 = "..";
        String string3 = string;
        while (!string3.isEmpty() && GuiSettings.getNormalFont().getWidth(string3 + string2) > f) {
            string3 = string3.substring(0, string3.length() - 1);
        }
        return string3 + string2;
    }

    private static String format(double d) {
        return Math.abs(d - Math.rint(d)) < 1.0E-6 ? String.valueOf((int)Math.rint(d)) : String.format(Locale.US, "%.2f", d).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    private static Color fade(Color color, float f) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), Math.max(0, Math.min(255, (int)((float)color.getAlpha() * f))));
    }

    private static Color mix(Color color, Color color2, float f) {
        f = Math.max(0.0f, Math.min(1.0f, f));
        return new Color((int)((float)color.getRed() + (float)(color2.getRed() - color.getRed()) * f), (int)((float)color.getGreen() + (float)(color2.getGreen() - color.getGreen()) * f), (int)((float)color.getBlue() + (float)(color2.getBlue() - color.getBlue()) * f), (int)((float)color.getAlpha() + (float)(color2.getAlpha() - color.getAlpha()) * f));
    }
}
