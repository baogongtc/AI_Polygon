package com.bd.polygon;

import android.graphics.Point;

import java.util.ArrayList;
import java.util.List;

/**
 * Description : <Content><br>
 * CreateTime : 2016/3/22 11:15
 *
 * @author KevinLiu
 * @version <v1.0>
 * @Editor : KevinLiu
 * @ModifyTime : 2016/3/22 11:15
 * @ModifyDescription : <Content>
 */
public class Polygon {

    private State state = State.UN_INIT;

    private int cornerColor;

    private int panelColor;

    public enum State {
        UN_INIT,
        INIT,
        MOVE,
        DRAG

    }

    public int getCornerColor() {
        return cornerColor;
    }

    public void setCornerColor(int cornerColor) {
        this.cornerColor = cornerColor;
    }

    public int getPanelColor() {
        return panelColor;
    }

    public void setPanelColor(int panelColor) {
        this.panelColor = panelColor;
    }

    public State getState() {
        return state;
    }

    public synchronized void  setState(State state) {
        this.state = state;
    }

    private List<Point> mPoints = new ArrayList<Point>();

    public Polygon() {
    }

    public Polygon(List points) {
        mPoints.addAll(points);
    }

    public Polygon(int panelColor,int cornerColor) {
        this.panelColor = panelColor;
        this.cornerColor = cornerColor;
    }

    public List<Point> getPoints() {
        return this.mPoints;
    }

    public Polygon addPoint(Point point) {
        mPoints.add(point);
        return this;
    }

    public int opposite(int index) {
        if (mPoints.size() % 2 != 0) {
            return -1;
        }

        if (index < mPoints.size() / 2) {
            return index + mPoints.size() / 2;
        } else {
            return index - mPoints.size() / 2;
        }

    }

    public int next(int index) {
        return index == mPoints.size() - 1 ? 0 : index + 1;
    }

    public int preview(int index) {
        return index == 0 ? mPoints.size() - 1 : index - 1;
    }

    public Polygon setPoint(int index, Point point) {
        if (index >= size()) {
            mPoints.add(index, point);
        } else {
            mPoints.set(index, point);
        }
        return this;
    }

    public Point getPoint(int index) {
        return mPoints.get(index);
    }

    public int size() {
        return mPoints.size();
    }

    public int indexOf(Point point) {
        return mPoints.indexOf(point);
    }

    public void updatePoints(int dX, int dY) {
        for (Point p :
                mPoints) {
            p.set(p.x + dX, p.y + dY);
        }
    }

    public Point updatePoint(int index, int dX, int dY) {
        Point p = getPoint(index);
        p.set(p.x + dX, p.y + dY);
        return p;
    }

    /**
     *
     * @param M:            水平栅格数
     * @param m:            x 比值
     * @param n             y 比值
     * @return
     */
    public List<Integer> getDirtyRectGrid(int M, int m, int n) {
        return MathUtil.getDirtyRectGrid(M, m, n, MathUtil.getRectPoints(mPoints));
    }

    public void clear(){
        mPoints.clear();
    }


}
