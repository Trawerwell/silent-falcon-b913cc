package aethereal.ui.shader;

import net.minecraft.client.gl.Uniform;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.util.Identifier;
import org.joml.Vector4f;

public class RectangleShader extends Shader {
    public Uniform c;
    public Uniform d;
    public Uniform e;
    public Uniform f;

    public RectangleShader() {
        super(Identifier.of("primordial", "core/rect/rect"), VertexFormats.POSITION_COLOR);
    }

    @Override
    protected void b() {
        this.c = a("uSize");
        this.d = a("uRadius");
        this.e = a("uSmoothness");
        this.f = a("uOutlineWidth");
        setFillProgress(1.0f);
    }

    public void a(float width, float height) {
        if (this.c != null) {
            this.c.set(width, height);
        }
    }

    public void a(Vector4f radius) {
        if (this.d != null) {
            this.d.set(radius.x, radius.z, radius.w, radius.y);
        }
    }

    public void a(float smoothness) {
        if (this.e != null) {
            this.e.set(smoothness);
        }
    }

    public void b(float width) {
        if (this.f != null) {
            this.f.set(width);
        }
    }

    public void setFillProgress(float progress) {
        Uniform fill = a("uFillProgress");
        if (fill != null) fill.set(progress);
    }
}
