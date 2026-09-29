package org.lab.pp.gen;

import java.util.Arrays;
import java.util.Random;

public class ZipfLoadGenerator implements LoadGenerator {

    @Override
    public long[] generate() {
        return generateZipf(
                1 << 20,
                1,
                1023,
                1.15,
                123456789L
        );
    }

    private long[] generateZipf(int size, int min, int max, double exponent, long seed) {
        int n = max - min + 1;

        double[] cdf = new double[n];
        double sum = 0.0;

        for (int k = min; k <= max; k++) {
            sum += 1.0 / Math.pow(k, exponent);
            cdf[k - min] = sum;
        }

        for (int i = 0; i < n; i++) {
            cdf[i] /= sum;
        }
        cdf[n - 1] = 1.0;

        Random random = new Random(seed);
        long[] values = new long[size];

        for (int i = 0; i < size; i++) {
            double u = random.nextDouble();

            int idx = Arrays.binarySearch(cdf, u);
            if (idx < 0) {
                idx = -idx - 1;
            }

            values[i] = min + idx;
        }

        return values;
    }
}
