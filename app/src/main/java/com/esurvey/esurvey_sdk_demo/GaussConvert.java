package com.esurvey.esurvey_sdk_demo;

public class GaussConvert {
    public static double a = 6378137.0;
    public static double f = 298.257222101;

    public GaussConvert() {
    }

    public static double[] BL_xy6(double B, double L) {
        return BL_xy(B, L, 6, true);
    }

    public static void BL_xy(double B, double L, int beltWidth) {
        BL_xy(B, L, beltWidth, true);
    }

    public static double[] BL_xy3(double B, double L) {
        return BL_xy(B, L, 3, true);
    }

    public static double[] BL_xy(double B, double L, int beltWidth, boolean assumedCoord) {
        int beltNum = (int)Math.ceil((L - (beltWidth == 3 ? 1.5 : 0.0)) / (double)beltWidth);
        if (beltWidth == 3 && (double)(beltNum * 3) == L - 1.5) {
            ++beltNum;
        }

        L -= (double)(beltNum * beltWidth - (beltWidth == 6 ? 3 : 0));
        double[] xy = Bl_xy(B, L, beltWidth);
        if (assumedCoord) {
            xy[1] += (double)(500000 + beltNum * 1000000);
        }

        return xy;
    }

    public static double[] Bl_xy(double B, double dL, int beltWidth) {
        double[] xy = new double[2];
        double ee = (2.0 * f - 1.0) / f / f;
        double ee2 = ee / (1.0 - ee);
        double rB = B * Math.PI / 180.0;
        double tB = Math.tan(rB);
        double m = Math.cos(rB) * dL * Math.PI / 180.0;
        double N = a / Math.sqrt(1.0 - ee * Math.sin(rB) * Math.sin(rB));
        double it2 = ee2 * Math.pow(Math.cos(rB), 2.0);
        xy[0] = m * m / 2.0 + (5.0 - tB * tB + 9.0 * it2 + 4.0 * it2 * it2) * Math.pow(m, 4.0) / 24.0 + (61.0 - 58.0 * tB * tB + Math.pow(tB, 4.0)) * Math.pow(m, 6.0) / 720.0;
        xy[0] = MeridianLength(B, a, f) + N * tB * xy[0];
        xy[1] = N * (m + (1.0 - tB * tB + it2) * Math.pow(m, 3.0) / 6.0 + (5.0 - 18.0 * tB * tB + Math.pow(tB, 4.0) + 14.0 * it2 - 58.0 * tB * tB * it2) * Math.pow(m, 5.0) / 120.0);
        return xy;
    }

    public static double[] xy3_BL(double x, double y) {
        return xy_BL(x, y, 3);
    }

    public static double[] xy_BL(double x, double y, int beltWidth) {
        double[] BL = new double[2];
        int beltNum = 0;
        if (y > 1000000.0) {
            beltNum = (int)Math.ceil(y / 1000000.0) - 1;
            y -= (double)(1000000 * beltNum + 500000);
        }

        BL = xy_Bl(x, y, beltWidth);
        BL[1] += (double)(beltWidth * beltNum - (beltWidth == 6 ? 3 : 0));
        return BL;
    }

    private static double[] xy_Bl(double x, double y, int beltWidth) {
        double[] BL = new double[2];
        double ee = (2.0 * f - 1.0) / f / f;
        double ee2 = ee / (1.0 - ee);
        double cA = 1.0 + 3.0 * ee / 4.0 + 45.0 * ee * ee / 64.0 + 175.0 * Math.pow(ee, 3.0) / 256.0 + 11025.0 * Math.pow(ee, 4.0) / 16384.0;
        double cB = 3.0 * ee / 4.0 + 15.0 * ee * ee / 16.0 + 525.0 * Math.pow(ee, 3.0) / 512.0 + 2205.0 * Math.pow(ee, 4.0) / 2048.0;
        double cC = 15.0 * ee * ee / 64.0 + 105.0 * Math.pow(ee, 3.0) / 256.0 + 2205.0 * Math.pow(ee, 4.0) / 4096.0;
        double cD = 35.0 * Math.pow(ee, 3.0) / 512.0 + 315.0 * Math.pow(ee, 4.0) / 2048.0;
        double cE = 315.0 * Math.pow(ee, 4.0) / 131072.0;
        double Bf = x / (a * (1.0 - ee) * cA);

        do {
            BL[0] = Bf;
            Bf = (x + a * (1.0 - ee) * (cB * Math.sin(2.0 * Bf) / 2.0 - cC * Math.sin(4.0 * Bf) / 4.0 + cD * Math.sin(6.0 * Bf) / 6.0) - cE * Math.sin(8.0 * Bf) / 8.0) / (a * (1.0 - ee) * cA);
        } while(Math.abs(BL[0] - Bf) > 1.0E-11);

        double N = a / Math.sqrt(1.0 - ee * Math.pow(Math.sin(Bf), 2.0));
        double V2 = 1.0 + ee2 * Math.pow(Math.cos(Bf), 2.0);
        double it2 = ee2 * Math.pow(Math.cos(Bf), 2.0);
        double tB2 = Math.pow(Math.tan(Bf), 2.0);
        BL[0] = Bf - V2 * Math.tan(Bf) / 2.0 * (Math.pow(y / N, 2.0) - (5.0 + 3.0 * tB2 + it2 - 9.0 * it2 * tB2) * Math.pow(y / N, 4.0) / 12.0 + (61.0 + 90.0 * tB2 + 45.0 * tB2 * tB2) * Math.pow(y / N, 6.0) / 360.0);
        BL[1] = (y / N - (1.0 + 2.0 * tB2 + it2) * Math.pow(y / N, 3.0) / 6.0 + (5.0 + 28.0 * tB2 + 24.0 * tB2 * tB2 + 6.0 * it2 + 8.0 * it2 * tB2) * Math.pow(y / N, 5.0) / 120.0) / Math.cos(Bf);
        BL[0] = BL[0] * 180.0 / Math.PI;
        BL[1] = BL[1] * 180.0 / Math.PI;
        return BL;
    }

    public static double MeridianLength(double B, double a, double f) {
        double ee = (2.0 * f - 1.0) / f / f;
        double rB = B * Math.PI / 180.0;
        double cA = 1.0 + 3.0 * ee / 4.0 + 45.0 * Math.pow(ee, 2.0) / 64.0 + 175.0 * Math.pow(ee, 3.0) / 256.0 + 11025.0 * Math.pow(ee, 4.0) / 16384.0;
        double cB = 3.0 * ee / 4.0 + 15.0 * Math.pow(ee, 2.0) / 16.0 + 525.0 * Math.pow(ee, 3.0) / 512.0 + 2205.0 * Math.pow(ee, 4.0) / 2048.0;
        double cC = 15.0 * Math.pow(ee, 2.0) / 64.0 + 105.0 * Math.pow(ee, 3.0) / 256.0 + 2205.0 * Math.pow(ee, 4.0) / 4096.0;
        double cD = 35.0 * Math.pow(ee, 3.0) / 512.0 + 315.0 * Math.pow(ee, 4.0) / 2048.0;
        double cE = 315.0 * Math.pow(ee, 4.0) / 131072.0;
        return a * (1.0 - ee) * (cA * rB - cB * Math.sin(2.0 * rB) / 2.0 + cC * Math.sin(4.0 * rB) / 4.0 - cD * Math.sin(6.0 * rB) / 6.0 + cE * Math.sin(8.0 * rB) / 8.0);
    }
}
