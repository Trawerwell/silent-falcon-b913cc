/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.font;

public class StringUtil {
    private static final String[] WORDS = new String[]{"\u0307\u0307", "\u0307", "\u0130", "I\u0307\u0307\u0307", "\u0644\u064f\u0644\u064f\u0635\u0651\u0628\u064f\u0644\u064f\u0644\u0635\u0651\u0628\u064f\u0631\u0631\u064b", "I\u0307", "I\u0307\u0307"};
    private static final String BANNED_WORD = "Banned Word!";

    public static String combine(Object ... args) {
        StringBuilder builder = new StringBuilder();
        Object[] objectArray = args;
        int n = args.length;
        int n2 = 0;
        while (n2 < n) {
            Object o = objectArray[n2];
            builder.append(o);
            ++n2;
        }
        return builder.toString();
    }

    public static String preventCrash(String str) {
        return str.replace("\u0307", "");
    }

    public static String toString(Object[] array, String combiner) {
        StringBuilder builder = new StringBuilder();
        int i = 0;
        while (i < array.length - 1) {
            if (!"".equals(array[i])) {
                builder.append(array[i]);
                builder.append(combiner);
            }
            ++i;
        }
        Object last = array[array.length - 1];
        if (!"".equals(last)) {
            builder.append(last);
        }
        return builder.toString();
    }
}
