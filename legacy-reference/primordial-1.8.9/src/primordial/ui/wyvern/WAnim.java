/*
 * Decompiled with CFR 0.152.
 */
package primordial.ui.wyvern;

public class WAnim {
    private float value;
    private float startValue;
    private float target;
    private long duration;
    private long startTime;
    private long lastFrame;
    private Easing easing;

    public WAnim(long l, Easing easing) {
        this.duration = l;
        this.easing = easing;
        this.startTime = System.currentTimeMillis();
    }

    public void setDuration(long l) {
        this.duration = l;
    }

    public void setEasing(Easing easing) {
        this.easing = easing;
    }

    public void setValue(float f) {
        this.value = f;
    }

    public void setStartValue(float f) {
        this.startValue = f;
    }

    public void update(boolean bl) {
        this.update(bl ? 1.0f : 0.0f);
    }

    public void update(float f) {
        long l = System.currentTimeMillis();
        if (this.lastFrame == 0L) {
            this.lastFrame = l;
            this.target = f;
            return;
        }
        float f2 = Math.min(0.25f, Math.max(0.0f, (float)(l - this.lastFrame) / 1000.0f));
        this.lastFrame = l;
        this.target = f;
        if (this.duration <= 0L) {
            this.value = f;
            return;
        }
        float f3 = 3200.0f / (float)this.duration * this.easingSpeed();
        float f4 = 1.0f - (float)Math.exp(-f2 * f3);
        this.value += (this.target - this.value) * f4;
        if (Math.abs(this.target - this.value) < 7.0E-4f) {
            this.value = this.target;
        }
    }

    private float easingSpeed() {
        switch (this.easing.ordinal()) {
            case 1: {
                return 1.0f;
            }
            case 4: {
                return 1.15f;
            }
            case 2: {
                return 0.9f;
            }
            case 3: {
                return 0.8f;
            }
        }
        return 1.0f;
    }

    public float getValue() {
        return this.value;
    }

    public float getTarget() {
        return this.target;
    }

    private float ease(float f) {
        switch (this.easing.ordinal()) {
            case 1: {
                float f2 = 1.0f - f;
                return 1.0f - f2 * f2 * f2;
            }
            case 4: {
                float f3 = 1.0f - f;
                return 1.0f - f3 * f3;
            }
            case 2: {
                return (float)Math.sin((double)f * Math.PI / 2.0);
            }
            case 3: {
                return (float)Math.sqrt(1.0 - ((double)f - 1.0) * ((double)f - 1.0));
            }
        }
        return f;
    }

    public static enum Easing {
        LINEAR,
        CUBIC_OUT,
        SINE_OUT,
        CIRC_OUT,
        QUAD_OUT;

    }
}
