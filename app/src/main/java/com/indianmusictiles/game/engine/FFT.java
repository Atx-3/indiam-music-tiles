package com.indianmusictiles.game.engine;

public class FFT {
    private final int n;
    private final int m;
    private final float[] cos;
    private final float[] sin;

    public FFT(int n) {
        this.n = n;
        this.m = (int) (Math.log(n) / Math.log(2));

        if (n != (1 << m)) {
            throw new RuntimeException("FFT length must be power of 2");
        }

        cos = new float[n / 2];
        sin = new float[n / 2];

        for (int i = 0; i < n / 2; i++) {
            cos[i] = (float) Math.cos(-2 * Math.PI * i / n);
            sin[i] = (float) Math.sin(-2 * Math.PI * i / n);
        }
    }

    public void fft(float[] re, float[] im) {
        int i, j, k, n1, n2, a;
        float c, s, t1, t2;

        // Bit-reversal
        j = 0;
        n2 = n / 2;
        for (i = 1; i < n - 1; i++) {
            n1 = n2;
            while (j >= n1) {
                j -= n1;
                n1 /= 2;
            }
            j += n1;

            if (i < j) {
                t1 = re[i];
                re[i] = re[j];
                re[j] = t1;
                t1 = im[i];
                im[i] = im[j];
                im[j] = t1;
            }
        }

        // FFT
        n1 = 0;
        n2 = 1;
        for (i = 0; i < m; i++) {
            n1 = n2;
            n2 += n2;
            a = 0;

            for (j = 0; j < n1; j++) {
                c = cos[a];
                s = sin[a];
                a += 1 << (m - i - 1);

                for (k = j; k < n; k += n2) {
                    t1 = c * re[k + n1] - s * im[k + n1];
                    t2 = s * re[k + n1] + c * im[k + n1];
                    re[k + n1] = re[k] - t1;
                    im[k + n1] = im[k] - t2;
                    re[k] += t1;
                    im[k] += t2;
                }
            }
        }
    }

    public float[] getMagnitude(float[] re, float[] im) {
        float[] mag = new float[n / 2];
        for (int i = 0; i < n / 2; i++) {
            mag[i] = (float) Math.sqrt(re[i] * re[i] + im[i] * im[i]);
        }
        return mag;
    }
}
