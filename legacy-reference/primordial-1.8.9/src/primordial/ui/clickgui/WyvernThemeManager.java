/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.clickgui;

import java.awt.Color;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import net.minecraft.client.Minecraft;
import primordial.ui.clickgui.WyvernTheme;

public final class WyvernThemeManager {
    private final List<WyvernTheme> themes = new ArrayList<WyvernTheme>();
    private int current;
    private final File file;

    public WyvernThemeManager() {
        this.file = new File(Minecraft.getMinecraft().mcDataDir, "primordial/themes.txt");
        this.load();
    }

    public List<WyvernTheme> themes() {
        return this.themes;
    }

    public WyvernTheme current() {
        if (this.themes.isEmpty()) {
            this.defaults();
        }
        this.current = Math.max(0, Math.min(this.current, this.themes.size() - 1));
        return this.themes.get(this.current);
    }

    public int currentIndex() {
        return this.current;
    }

    public void select(int n) {
        if (n >= 0 && n < this.themes.size()) {
            this.current = n;
            this.save();
        }
    }

    public void add(WyvernTheme wyvernTheme) {
        this.themes.add(wyvernTheme);
        this.current = this.themes.size() - 1;
        this.save();
    }

    public boolean delete(int n) {
        if (this.themes.size() <= 1 || n < 0 || n >= this.themes.size()) {
            return false;
        }
        this.themes.remove(n);
        if (this.current >= this.themes.size()) {
            this.current = this.themes.size() - 1;
        } else if (n < this.current) {
            --this.current;
        }
        this.save();
        return true;
    }

    public void save() {
        try {
            File file = this.file.getParentFile();
            if (file != null) {
                file.mkdirs();
            }
            PrintWriter printWriter = new PrintWriter(new OutputStreamWriter((OutputStream)new FileOutputStream(this.file), StandardCharsets.UTF_8));
            printWriter.println("current=" + this.current);
            for (WyvernTheme wyvernTheme : this.themes) {
                String string = Base64.getEncoder().encodeToString(wyvernTheme.getName().getBytes(StandardCharsets.UTF_8));
                StringBuilder stringBuilder = new StringBuilder(string).append(':');
                for (int i = 0; i < wyvernTheme.getColors().size(); ++i) {
                    if (i > 0) {
                        stringBuilder.append(',');
                    }
                    stringBuilder.append(String.format("%08X", wyvernTheme.getColors().get(i).getRGB()));
                }
                printWriter.println(stringBuilder);
            }
            printWriter.close();
        }
        catch (Throwable throwable) {
            System.err.println("[Themes] Could not save themes.txt: " + throwable.getMessage());
        }
    }

    private void load() {
        if (!this.file.exists()) {
            this.defaults();
            this.save();
            return;
        }
        try {
            List<String> list = Files.readAllLines(this.file.toPath(), StandardCharsets.UTF_8);
            for (String string : list) {
                if (string.startsWith("current=")) {
                    this.current = Integer.parseInt(string.substring(8));
                    continue;
                }
                int n = string.indexOf(58);
                if (n <= 0) continue;
                String string2 = new String(Base64.getDecoder().decode(string.substring(0, n)), StandardCharsets.UTF_8);
                ArrayList<Color> arrayList = new ArrayList<Color>();
                for (String string3 : string.substring(n + 1).split(",")) {
                    try {
                        arrayList.add(new Color((int)Long.parseLong(string3, 16), true));
                    }
                    catch (Exception exception) {
                        // empty catch block
                    }
                }
                if (arrayList.isEmpty()) continue;
                this.themes.add(new WyvernTheme(string2, arrayList));
            }
        }
        catch (Throwable throwable) {
            System.err.println("[Themes] Could not load themes.txt: " + throwable.getMessage());
        }
        if (this.themes.isEmpty()) {
            this.defaults();
        }
        this.current = Math.max(0, Math.min(this.current, this.themes.size() - 1));
    }

    private void defaults() {
        this.themes.clear();
        this.addD("Cyber Blue", -16725761, -16119276);
        this.addD("Olive", -6514859, -14145506);
        this.addD("Violet Void", -4980481, -14811096);
        this.addD("Sunset", -41472, -14150656);
        this.addD("Neon Green", -12976364, -16116726);
        this.addD("Abyss Blue", -16750900, -16119266);
        this.addD("Sakura", -18491, -1);
        this.addD("Cyber Dragon", -2354116, -10496);
        this.addD("Aurora", -16711681, -7114533);
        this.addD("Matrix", -16711936, -16777216);
        this.addD("Hellfire", -65536, -29696);
        this.addD("Frozen Soul", -5383962, -16777077);
        this.addD("Gold Dust", -10496, -16777216);
        this.addD("Neon Wave", -60269, -7722014);
        this.addD("Desert Rose", -1193553, -38476);
        this.addD("Ghost", -5658199, -657931);
        this.addD("Ruby Red", -2092705, -14811126);
        this.addD("Oceanic", -16746562, -16772056);
        this.addD("Forest", -14513374, -16772096);
        this.addD("Cyberpunk", -65281, -16711681);
        this.addD("Solar", -29696, -47872);
        this.addD("Deep Sea", -16777088, -16744193);
        this.addD("Autumn", -7650029, -23296);
        this.addD("Polar", -984833, -5192482);
        this.addD("Volcanic", -47872, -16777216);
        this.addD("Royal", -8689426, -10496);
        this.addD("Pastel Pink", -11812, -1);
        this.addD("Grape", -9491011, -6467875);
        this.addD("Steel", -9404272, -4144960);
        this.addD("Ocean", -16630134, -16746570);
        this.addD("Fire", -1689274, -918802);
        this.addD("Night", -15457987, -220399);
        this.addD("Rose Gold", -4755847, -6943);
        this.addD("Deep Purple", -13625036, -7722014);
        this.addD("Toxic", -5374161, -16751616);
        this.addD("Ice", -983041, -16711681);
        this.addD("Electric", -8521217, -16776961);
        this.addD("Candy", -38476, -16181);
        this.addD("Starlight", -15132304, -4144960);
        this.addD("Emerald Dream", -16751616, -13447886);
        this.addD("Synthwave", -65409, -16711681);
        this.addD("Bloodlust", -7667712, -65536);
        this.addD("Thunder", -13676721, -10496);
        this.addD("Mocha", -12703965, -2634552);
        this.addD("Amethyst", -11861886, -6723892);
        this.addD("Arctic", -16711681, -983041);
        this.addD("Shadow", -15461356, -9868951);
        this.addD("Vampire", -14155776, -8388480);
        this.current = 0;
    }

    private void addD(String string, int n, int n2) {
        this.themes.add(new WyvernTheme(string, Arrays.asList(new Color(n, true), new Color(n2, true))));
    }
}
