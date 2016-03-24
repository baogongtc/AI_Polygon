package com.bd.polygon;

import android.graphics.Point;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class MathUtil {

    /**
     * Calculates the distance between two points (x1, y1) and (x2, y2).
     */
    public static float calculateDistance(float x1, float y1, float x2, float y2) {

        final float side1 = x2 - x1;
        final float side2 = y2 - y1;

        return (float) Math.sqrt(side1 * side1 + side2 * side2);
    }

    //弦长公式
    public static double calculateChord(float radius, float distance) {

        return 2 * Math.sqrt(radius * radius - distance * distance);
    }

    // 中心点坐标计算
    public static Point averagePolygon(List<Point> points) {
        if (null == points || points.size() == 0) {
            return null;
        }
        int x = 0;
        int y = 0;
        for (Point p :
                points) {
            x += p.x;
            y += p.y;
        }
        return new Point(x / points.size(), y / points.size());
    }

    //得到最大最小矩形框坐标
    public static int[] getRectPoints(List<Point> points) {
        if (null == points || points.size() == 0) {
            return null;
        }
        int[] result = new int[4];
        //初始化为第一个点
        result[0] = points.get(0).x;
        result[1] = points.get(0).y;

        for (Point p :
                points) {

            result[0] = result[0] > p.x ? p.x : result[0];
            result[1] = result[1] > p.y ? p.y : result[1];
            result[2] = result[2] < p.x ? p.x : result[2];
            result[3] = result[3] < p.y ? p.y : result[3];

        }
        Log.e("getRectPoints", result[0] + "\t" + result[1] + "\t" + result[2] + "\t" + result[3] + "\t");
        return result;
    }

    /**
     * @param M: 水平栅格数
     * @param m: x 比值
     * @param n  y 比值
     * @return
     */
    public static List<Integer> getDirtyRectGrid(int M, int m, int n, int[] rect) {
        if (null == rect)
            return null;

        Log.e("rect:", rect[0] + "\t" + rect[1] + "\t" + rect[2] + "\t" + rect[3]);
        List<Integer> ret = new ArrayList<Integer>();
        int x1 = rect[0] < 0 ? 0 : rect[0] / m + 1;
        int y1 = rect[1] < 0 ? 0 : rect[1] / n + 1;

        int x2 = rect[2] % m > 0 || rect[2] / m == 0 ? rect[2] / m + 1 : rect[2] / m;
        int y2 = rect[3] % n > 0 || rect[3] / n == 0 ? rect[3] / n + 1 : rect[3] / n;

        for (int i = x1+1; i < x2 + 1; i++) {
            for (int j = y1; j < y2 + 1; j++) {
                ret.add(i + j * M);
            }
        }
        return ret;
    }
}
