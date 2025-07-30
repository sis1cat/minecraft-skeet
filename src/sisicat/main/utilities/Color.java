package sisicat.main.utilities;

public class Color {

    public static final float[]

            red = new float[]{255, 0, 0, 255},
            green = new float[]{0, 255, 0, 255},
            blue = new float[]{255, 0, 255, 255},

            c12 = new float[]{12, 12, 12, 255},
            c15 = new float[]{15, 15, 15, 255},
            c25 = new float[]{25, 25, 25, 255},
            c35 = new float[]{35, 35, 35, 255},
            c40 = new float[]{40, 40, 40, 255},
            c50 = new float[]{50, 50, 50, 255},
            c60 = new float[]{60, 60, 60, 255},
            c90 = new float[]{90, 90, 90, 255},
            c115 = new float[]{115, 115, 115, 255},
            c140 = new float[]{140, 140, 140, 255},
            c165 = new float[]{165, 165, 165, 255},
            c190 = new float[]{190, 190, 190, 255},
            c205 = new float[]{205, 205, 205, 255},

            cSpecial = new float[]{170, 170, 100, 255};

    public static float[] rgbToHsv(float r, float g, float b) {

        r /= 255.0f;
        g /= 255.0f;
        b /= 255.0f;

        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;

        float
                h = 0,
                s = 0;

        if (delta != 0) {

            if (max == r)
                h = (g - b) / delta;
            else if (max == g)
                h = 2 + (b - r) / delta;
            else
                h = 4 + (r - g) / delta;

        }

        h = (h * 60) % 360;

        if (h < 0)
            h += 360;

        if (max != 0)
            s = delta / max;

        return new float[]{h, s, max};

    }

    public static float[] hsvToRgb(float h, float s, float v) {

        float
                c = v * s,
                x = c * (1 - Math.abs(((h / 60) % 2) - 1)),
                m = v - c;

        float
                rPrime = 0,
                gPrime = 0,
                bPrime = 0;

        if (h >= 0 && h < 60) {
            rPrime = c;
            gPrime = x;
            bPrime = 0;
        } else if (h >= 60 && h < 120) {
            rPrime = x;
            gPrime = c;
            bPrime = 0;
        } else if (h >= 120 && h < 180) {
            rPrime = 0;
            gPrime = c;
            bPrime = x;
        } else if (h >= 180 && h < 240) {
            rPrime = 0;
            gPrime = x;
            bPrime = c;
        } else if (h >= 240 && h < 300) {
            rPrime = x;
            gPrime = 0;
            bPrime = c;
        } else if (h >= 300 && h < 360) {
            rPrime = c;
            gPrime = 0;
            bPrime = x;
        }

        int r = Math.round((rPrime + m) * 255);
        int g = Math.round((gPrime + m) * 255);
        int b = Math.round((bPrime + m) * 255);

        return new float[]{r, g, b};

    }

    public static class ColorPicker {

        public static float[] getSVOffsets(float r, float g, float b) {
            float[] hsv = rgbToHsv(r, g, b);
            float xOffset = Math.min(hsv[1] / 0.85f, 1f);
            float yOffset = (1f - Math.min(hsv[2] / 0.85f, 1f));
            return new float[]{xOffset, yOffset};
        }

        public static float getHOffset(float r, float g, float b) {
            float[] hsv = rgbToHsv(r, g, b);
            return 1 - hsv[0] / 360.0f;
        }

    }

}
