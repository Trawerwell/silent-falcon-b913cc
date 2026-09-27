/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.menu;

import java.io.IOException;
import java.util.List;
import net.minecraft.client.gui.ExpandButton;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import primordial.manager.AccountManager;
import primordial.ui.menu.PrimordialMenuRenderer;

public class GuiAccounts
extends GuiScreen {
    private static final int ROSE = -4023387;
    private static final int MUTED = -7700346;
    private static final int TEXT = -1646878;
    private static final int ROW = 22;
    private static final int VISIBLE = 8;
    private final GuiScreen parent;
    private GuiTextField nameField;
    private List<String> accounts = AccountManager.accounts();
    private int selected = -1;
    private int scroll;
    private int lastClickIndex = -1;
    private long lastClickTime;
    private String status = "";
    private int statusColor = -7700346;
    private long statusUntil;

    public GuiAccounts(GuiScreen guiScreen) {
        this.parent = guiScreen;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        this.accounts = AccountManager.accounts();
        this.buttonList.clear();
        int n = Math.min(280, this.width - 40);
        int n2 = this.width / 2 - n / 2;
        int n3 = this.listTop();
        int n4 = n3 + 176 + 16;
        this.nameField = new GuiTextField(1, this.fontRendererObj, n2 + 6, n4 + 6, n - 78, 12);
        this.nameField.setMaxStringLength(16);
        this.nameField.setEnableBackgroundDrawing(false);
        this.nameField.setTextColor(-1646878);
        this.nameField.setDisabledTextColour(-7700346);
        this.nameField.setFocused(true);
        this.buttonList.add(new ExpandButton(1, n2 + n - 68, n4, 68, 24, "add"));
        int n5 = n4 + 32;
        this.buttonList.add(new ExpandButton(2, n2, n5, 88, 20, "login"));
        this.buttonList.add(new ExpandButton(3, n2 + 96, n5, 88, 20, "delete"));
        this.buttonList.add(new ExpandButton(4, n2 + n - 88, n5, 88, 20, "back"));
        this.selectCurrent();
    }

    private int listTop() {
        return Math.max(72, this.height / 2 - 78);
    }

    private void selectCurrent() {
        String string = AccountManager.currentName();
        this.selected = this.indexOf(string);
        if (this.selected < 0 && !this.accounts.isEmpty()) {
            this.selected = this.indexOf(AccountManager.lastUsed());
        }
        if (this.selected < 0 && !this.accounts.isEmpty()) {
            this.selected = 0;
        }
        this.ensureVisible();
    }

    private int indexOf(String string) {
        if (string == null) {
            return -1;
        }
        for (int i = 0; i < this.accounts.size(); ++i) {
            if (!this.accounts.get(i).equalsIgnoreCase(string)) continue;
            return i;
        }
        return -1;
    }

    private void ensureVisible() {
        int n;
        if (this.selected < this.scroll) {
            this.scroll = Math.max(0, this.selected);
        }
        if (this.selected >= this.scroll + 8) {
            this.scroll = Math.max(0, this.selected - 8 + 1);
        }
        if (this.scroll > (n = Math.max(0, this.accounts.size() - 8))) {
            this.scroll = n;
        }
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    public void updateScreen() {
        if (this.nameField != null) {
            this.nameField.updateCursorCounter();
        }
    }

    @Override
    protected void actionPerformed(GuiButton guiButton) throws IOException {
        if (!guiButton.enabled) {
            return;
        }
        if (guiButton.id == 1) {
            this.addFromField();
        } else if (guiButton.id == 2) {
            this.loginSelected();
        } else if (guiButton.id == 3) {
            this.deleteSelected();
        } else if (guiButton.id == 4) {
            this.mc.displayGuiScreen(this.parent);
        }
    }

    private void addFromField() {
        String string = this.nameField.getText().trim();
        if (!AccountManager.isValidName(string)) {
            this.flash("3-16 letters, numbers, _", -4023387);
            return;
        }
        AccountManager.add(string);
        this.refresh();
        this.selected = this.indexOf(string);
        this.ensureVisible();
        this.nameField.setText("");
        this.flash("added " + string, -4023387);
    }

    private void loginSelected() {
        if (this.selected < 0 || this.selected >= this.accounts.size()) {
            String string = this.nameField.getText().trim();
            if (AccountManager.isValidName(string)) {
                AccountManager.login(string);
                this.refresh();
                this.flash("logged in as " + string, -4023387);
                return;
            }
            this.flash("select an account", -7700346);
            return;
        }
        String string = this.accounts.get(this.selected);
        if (AccountManager.login(string)) {
            this.refresh();
            this.flash("logged in as " + string, -4023387);
        }
    }

    private void deleteSelected() {
        if (this.selected < 0 || this.selected >= this.accounts.size()) {
            this.flash("nothing to delete", -7700346);
            return;
        }
        String string = this.accounts.get(this.selected);
        AccountManager.remove(string);
        this.refresh();
        if (this.selected >= this.accounts.size()) {
            this.selected = this.accounts.size() - 1;
        }
        this.ensureVisible();
        this.flash("removed " + string, -7700346);
    }

    private void refresh() {
        this.accounts = AccountManager.accounts();
    }

    private void flash(String string, int n) {
        this.status = string;
        this.statusColor = n;
        this.statusUntil = System.currentTimeMillis() + 2400L;
    }

    @Override
    protected void keyTyped(char c, int n) throws IOException {
        if (n == 1) {
            this.mc.displayGuiScreen(this.parent);
            return;
        }
        if (this.nameField.textboxKeyTyped(c, n)) {
            return;
        }
        if (n == 28 || n == 156) {
            if (!this.nameField.getText().trim().isEmpty()) {
                this.addFromField();
            } else {
                this.loginSelected();
            }
            return;
        }
        if (n == 200) {
            this.moveSelection(-1);
        } else if (n == 208) {
            this.moveSelection(1);
        } else if (n == 211 || n == 14 && this.nameField.getText().isEmpty()) {
            this.deleteSelected();
        }
    }

    private void moveSelection(int n) {
        if (this.accounts.isEmpty()) {
            return;
        }
        this.selected = this.selected < 0 ? 0 : Math.max(0, Math.min(this.accounts.size() - 1, this.selected + n));
        this.ensureVisible();
    }

    @Override
    protected void mouseClicked(int n, int n2, int n3) throws IOException {
        this.nameField.mouseClicked(n, n2, n3);
        super.mouseClicked(n, n2, n3);
        if (n3 != 0) {
            return;
        }
        int n4 = this.rowAt(n, n2);
        if (n4 >= 0) {
            long l = System.currentTimeMillis();
            if (n4 == this.lastClickIndex && l - this.lastClickTime < 280L) {
                this.selected = n4;
                this.loginSelected();
            } else {
                this.selected = n4;
            }
            this.lastClickIndex = n4;
            this.lastClickTime = l;
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int n = Mouse.getEventDWheel();
        if (n == 0) {
            return;
        }
        int n2 = Math.max(0, this.accounts.size() - 8);
        this.scroll = n > 0 ? Math.max(0, this.scroll - 1) : Math.min(n2, this.scroll + 1);
    }

    private int rowAt(int n, int n2) {
        int n3 = Math.min(280, this.width - 40);
        int n4 = this.width / 2 - n3 / 2;
        int n5 = this.listTop();
        if (n < n4 || n > n4 + n3) {
            return -1;
        }
        int n6 = n2 - n5;
        if (n6 < 0 || n6 >= 176) {
            return -1;
        }
        int n7 = this.scroll + n6 / 22;
        return n7 >= 0 && n7 < this.accounts.size() ? n7 : -1;
    }

    @Override
    public void drawScreen(int n, int n2, float f) {
        int n3;
        int n4;
        int n5;
        PrimordialMenuRenderer.render(this.width, this.height, n, n2);
        int n6 = Math.min(280, this.width - 40);
        int n7 = this.width / 2 - n6 / 2;
        int n8 = this.listTop();
        this.drawCenteredString(this.fontRendererObj, "accounts", this.width / 2, n8 - 36, -4023387);
        String string = AccountManager.currentName();
        this.drawCenteredString(this.fontRendererObj, "playing as " + string, this.width / 2, n8 - 22, -7700346);
        GuiAccounts.drawPanel(n7, n8 - 4, n6, 184);
        if (this.accounts.isEmpty()) {
            this.drawCenteredString(this.fontRendererObj, "no saved accounts", this.width / 2, n8 + 88 - 4, -7700346);
        } else {
            for (n5 = 0; n5 < 8 && (n4 = this.scroll + n5) < this.accounts.size(); ++n5) {
                boolean bl;
                n3 = n8 + n5 * 22;
                boolean bl2 = n4 == this.selected;
                boolean bl3 = bl = this.rowAt(n, n2) == n4;
                if (bl2) {
                    GuiAccounts.drawRow(n7 + 3, n3 + 1, n6 - 6, 20, 1438817189);
                } else if (bl) {
                    GuiAccounts.drawRow(n7 + 3, n3 + 1, n6 - 6, 20, 858401329);
                }
                String string2 = this.accounts.get(n4);
                int n9 = bl2 ? -4023387 : -1646878;
                this.fontRendererObj.drawString(string2, n7 + 10, n3 + 7, n9);
                if (!string2.equalsIgnoreCase(string)) continue;
                String string3 = "active";
                int n10 = this.fontRendererObj.getStringWidth(string3);
                this.fontRendererObj.drawString(string3, n7 + n6 - 12 - n10, n3 + 7, -4023387);
            }
        }
        n5 = n8 + 176 + 16;
        n4 = n6 - 72;
        GuiAccounts.drawPanel(n7, n5, n4, 24);
        this.drawCenteredField(n7, n5, n4, 24);
        if (!this.status.isEmpty() && System.currentTimeMillis() < this.statusUntil) {
            this.drawCenteredString(this.fontRendererObj, this.status, this.width / 2, n5 + 58, this.statusColor);
        }
        super.drawScreen(n, n2, f);
        for (n3 = 0; n3 < 8 && GL11.glGetError() != 0; ++n3) {
        }
    }

    private void drawCenteredField(int n, int n2, int n3, int n4) {
        String string = this.nameField.getText();
        boolean bl = string.isEmpty();
        String string2 = bl ? "nickname" : string;
        int n5 = bl ? -9542036 : -1646878;
        int n6 = this.fontRendererObj.getStringWidth(string2);
        int n7 = n + (n3 - n6) / 2;
        int n8 = n2 + (n4 - 8) / 2;
        this.fontRendererObj.drawString(string2, n7, n8, n5);
        if (!bl && this.nameField.isFocused() && (System.currentTimeMillis() / 530L & 1L) == 0L) {
            int n9 = Math.max(0, Math.min(string.length(), this.nameField.getCursorPosition()));
            int n10 = n + (n3 - this.fontRendererObj.getStringWidth(string)) / 2 + this.fontRendererObj.getStringWidth(string.substring(0, n9));
            GuiAccounts.drawRect(n10, n8 - 1, n10 + 1, n8 + 9, -4023387);
        }
    }

    private void clickCenteredField(int n, int n2, int n3) {
        int n4 = Math.min(280, this.width - 40);
        int n5 = this.width / 2 - n4 / 2;
        int n6 = this.listTop() + 176 + 16;
        int n7 = n4 - 72;
        boolean bl = n >= n5 && n < n5 + n7 && n2 >= n6 && n2 < n6 + 24;
        this.nameField.setFocused(bl);
        if (!bl || n3 != 0) {
            return;
        }
        String string = this.nameField.getText();
        int n8 = this.fontRendererObj.getStringWidth(string);
        int n9 = n5 + (n7 - n8) / 2;
        int n10 = n - n9;
        if (n10 <= 0) {
            this.nameField.setCursorPosition(0);
            return;
        }
        this.nameField.setCursorPosition(this.fontRendererObj.trimStringToWidth(string, n10).length());
    }

    private static void drawPanel(int n, int n2, int n3, int n4) {
        GuiAccounts.drawRect(n, n2, n + n3, n2 + n4, -736881640);
    }

    private static void drawRow(int n, int n2, int n3, int n4, int n5) {
        GuiAccounts.drawRect(n, n2, n + n3, n2 + n4, n5);
    }

    private static int scaleA(int n, float f) {
        int n2 = Math.max(0, Math.min(255, (int)((float)(n >>> 24) * f)));
        return n2 << 24 | n & 0xFFFFFF;
    }

    private static void gradient(float f, float f2, float f3, float f4, int n, int n2) {
        if (f3 <= f) {
            return;
        }
        float f5 = (float)(n >> 24 & 0xFF) / 255.0f;
        float f6 = (float)(n >> 16 & 0xFF) / 255.0f;
        float f7 = (float)(n >> 8 & 0xFF) / 255.0f;
        float f8 = (float)(n & 0xFF) / 255.0f;
        float f9 = (float)(n2 >> 24 & 0xFF) / 255.0f;
        float f10 = (float)(n2 >> 16 & 0xFF) / 255.0f;
        float f11 = (float)(n2 >> 8 & 0xFF) / 255.0f;
        float f12 = (float)(n2 & 0xFF) / 255.0f;
        GlStateManager.enableBlend();
        GlStateManager.disableTexture2D();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);
        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(f, f4, 0.0).color(f6, f7, f8, f5).endVertex();
        worldRenderer.pos(f3, f4, 0.0).color(f10, f11, f12, f9).endVertex();
        worldRenderer.pos(f3, f2, 0.0).color(f10, f11, f12, f9).endVertex();
        worldRenderer.pos(f, f2, 0.0).color(f6, f7, f8, f5).endVertex();
        tessellator.draw();
        GlStateManager.shadeModel(7424);
        GlStateManager.enableTexture2D();
        GlStateManager.disableBlend();
    }
}
