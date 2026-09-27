/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.wyvern;

public final class WSpring {
    private float value;
    private float velocity;
    private float target;
    private final float stiffness;
    private final float damping;
    private long lastNanos;
    private boolean primed;

    public WSpring(float f, float f2) {
        this.stiffness = f;
        this.damping = f2;
    }

    public static WSpring pop() {
        return new WSpring(220.0f, 16.0f);
    }

    public void setTarget(float f) {
        this.target = f;
    }

    public void snap(float f) {
        this.value = f;
        this.velocity = 0.0f;
        this.target = f;
        this.primed = true;
    }

    public float update(float f) {
        this.target = f;
        long l = System.nanoTime();
        if (!this.primed) {
            this.value = f;
            this.lastNanos = l;
            this.primed = true;
            return this.value;
        }
        float f2 = Math.min(0.05f, Math.max(0.0f, (float)(l - this.lastNanos) / 1.0E9f));
        this.lastNanos = l;
        float f3 = -this.stiffness * (this.value - this.target);
        this.velocity += f3 * f2;
        this.velocity -= this.damping * this.velocity * f2;
        this.value += this.velocity * f2;
        if (Math.abs(this.value - this.target) < 0.0015f && Math.abs(this.velocity) < 0.01f) {
            this.value = this.target;
            this.velocity = 0.0f;
        }
        return this.value;
    }

    public float getValue() {
        return this.value;
    }
}
