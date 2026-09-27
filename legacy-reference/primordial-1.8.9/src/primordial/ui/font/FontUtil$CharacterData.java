/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.font;

public static class FontUtil.CharacterData {
    public char character;
    public float positionX;
    public float positionY;
    public float width;
    public float height;
    public float advance;
    public int textureId;

    public FontUtil.CharacterData(char c, float f, float f2, float f3, float f4, float f5) {
        this.character = c;
        this.positionX = f;
        this.positionY = f2;
        this.width = f3;
        this.height = f4;
        this.advance = f5;
    }
}
