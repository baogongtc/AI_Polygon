/*
 * Copyright 2013, Edmodo, Inc. 
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this work except in compliance with the License.
 * You may obtain a copy of the License in the LICENSE file, or at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" 
 * BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language 
 * governing permissions and limitations under the License. 
 */

package com.bd.polygon;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.widget.ImageView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Custom view that provides cropping capabilities to an image.
 */
public class PolygonView extends ImageView {

    // Private Constants ///////////////////////////////////////////////////////////////////////////
    private static final String TAG = PolygonView.class.getName();


    // Member Variables ////////////////////////////////////////////////////////////////////////////

    // The Paint used to draw the white rectangle around the crop area.
    private Paint mBorderPaint;

    // The Paint used to draw the guidelines within the crop area when pressed.
    private Paint mGuidelinePaint;

    // The Paint used to draw the corners of the Border
    private Paint mCornerPaint;

    // The Paint used to darken the surrounding areas outside the crop area.
    private Paint mSurroundingAreaOverlayPaint;


    // Length of one side of the corner handle.
    private float mCornerLength;

    // The bounding box around the Bitmap that we are cropping.
    @NonNull
    private RectF mBitmapRect = new RectF();


    private int mGuidelinesMode = 1;
    private float mRadius;

    // Constructors ////////////////////////////////////////////////////////////////////////////////

    public PolygonView(Context context) {
        super(context);
        init(context, null);
    }

    public PolygonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    public PolygonView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {

        final TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PolygonView, 0, 0);
        mGuidelinesMode = typedArray.getInteger(R.styleable.PolygonView_guidelines, 1);
        int shapeValue = typedArray.getInteger(R.styleable.PolygonView_shape, 0);
        mShape = Shape.getType(shapeValue);
        typedArray.recycle();

        final Resources resources = context.getResources();

        mBorderPaint = PaintUtil.newBorderPaint(resources);
        mGuidelinePaint = PaintUtil.newGuidelinePaint(resources);
        mSurroundingAreaOverlayPaint = PaintUtil.newSurroundingAreaOverlayPaint(resources);
//        mCornerPaint = PaintUtil.newCornerPaint(resources);
        mCornerPaint = PaintUtil.newCircleCornerPaint(resources);
        mCornerLength = resources.getDimension(R.dimen.corner_length);
    }

    // View Methods ////////////////////////////////////////////////////////////////////////////////

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        mBitmapRect = getBitmapRect();
        initCropWindow(mBitmapRect);
    }

    // shape == circle 模式，圆心暂未控制，后期优化的时候，需要修复！
    float mCenterX = 0;
    float mCenterY = 0;

    public void setCenterX(float centerX) {
        this.mCenterX = centerX;
    }

    public void setCenterY(float centerY) {
        this.mCenterY = centerY;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (Shape.RECT == mShape) {
            drawPolygon(canvas);

        } else if (Shape.CIRCLE == mShape) {
            if (null != mCurrentActivePolygon) {
                for (int polygonIndex = 0; polygonIndex < mPolygons.size(); polygonIndex++) {
                    drawCircle(canvas, mPolygons.get(polygonIndex));
                }

            }

        } else if (Shape.OCTAGON == mShape) {
            drawPolygon(canvas);
        }
    }

    private void drawPolygon(Canvas canvas) {
        if ((null != mCurrentActivePolygon) && (0 != mCurrentActivePolygon.size())) {
            for (int polygonIndex = 0; polygonIndex < mPolygons.size(); polygonIndex++) {
                if (0 != mPolygons.get(polygonIndex).getPoints().size()) {
                    drawPolygons(canvas, mPolygons.get(polygonIndex));
                }
            }
        }
    }

    private void drawCircle(Canvas canvas, Polygon polygon) {
        mCenterX = mCenterX == 0 ? (mBitmapRect.left + mBitmapRect.right) / 2 : mCenterX;
        mCenterY = mCenterY == 0 ? (mBitmapRect.top + mBitmapRect.bottom) / 2 : mCenterY;
        Point point = polygon.getPoint(0);
        mRadius = MathUtil.calculateDistance(mCenterX, mCenterY, point.x, point.y);
        drawCircle(canvas, mCenterX, mCenterY, mRadius, mSurroundingAreaOverlayPaint);//darkenedSurroundingArea
        drawCircle(canvas, mCenterX, mCenterY, mRadius, mBorderPaint);// Border
        drawCircleGuideLines(canvas, mCenterX, mCenterY, mRadius, mRadius / 3);
        drawCircleCorners(canvas, point.x, point.y, mCornerPaint);
    }

    private void drawPolygons(Canvas canvas, Polygon polygon) {
        mBorderPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        mBorderPaint.setAntiAlias(false);
        mBorderPaint.setColor(polygon.getPanelColor());
        drawPlines(canvas, polygon, mBorderPaint);
        mCornerPaint.setColor(polygon.getCornerColor());
        for (Point pt :
                polygon.getPoints()) {
            drawCircleCorners(canvas, pt.x, pt.y, mCornerPaint);
        }
    }

    private void drawPlines(Canvas canvas, Polygon polygon, Paint mBorderPaint) {
        Path path = new Path();
        path.moveTo(polygon.getPoint(0).x, polygon.getPoint(0).y);
        for (int i = 1; i < polygon.getPoints().size(); i++) {
            path.lineTo(polygon.getPoint(i).x, polygon.getPoint(i).y);
        }
        path.close();
        canvas.drawPath(path, mBorderPaint);
    }


    private void drawCircleGuideLines(Canvas canvas, float centerX, float centerY, float radius, float distance) {
        double chord = MathUtil.calculateChord(radius, distance);
        Point rightPoint = new Point((int) (centerX + distance), (int) centerY);
        Point leftPoint = new Point((int) (centerX - distance), (int) centerY);
        Point topPoint = new Point((int) centerX, (int) (centerY - distance));
        Point bottomPoint = new Point((int) centerX, (int) (centerY + distance));

        canvas.drawLine(leftPoint.x, leftPoint.y, leftPoint.x, (float) (leftPoint.y - chord / 2), mGuidelinePaint);
        canvas.drawLine(leftPoint.x, leftPoint.y, leftPoint.x, (float) (leftPoint.y + chord / 2), mGuidelinePaint);
        canvas.drawLine(rightPoint.x, rightPoint.y, rightPoint.x, (float) (rightPoint.y - chord / 2), mGuidelinePaint);
        canvas.drawLine(rightPoint.x, rightPoint.y, rightPoint.x, (float) (rightPoint.y + chord / 2), mGuidelinePaint);

        canvas.drawLine(topPoint.x, topPoint.y, (float) (topPoint.x - chord / 2), topPoint.y, mGuidelinePaint);
        canvas.drawLine(topPoint.x, topPoint.y, (float) (topPoint.x + chord / 2), topPoint.y, mGuidelinePaint);
        canvas.drawLine(bottomPoint.x, bottomPoint.y, (float) (bottomPoint.x - chord / 2), bottomPoint.y, mGuidelinePaint);
        canvas.drawLine(bottomPoint.x, bottomPoint.y, (float) (bottomPoint.x + chord / 2), bottomPoint.y, mGuidelinePaint);


    }


    private void drawCircle(Canvas canvas, float centerX, float centerY, float radius, Paint borderPaint) {

        canvas.drawCircle(centerX, centerY, radius, borderPaint);
    }

    private void drawRectBorder(@NonNull Canvas canvas, float leftCoordinate, float topCoordinate, float rightCoordinate, float bottomCoordinate) {
        canvas.drawRect(leftCoordinate,
                topCoordinate,
                rightCoordinate,
                bottomCoordinate,
                mBorderPaint);
    }

    private Shape mShape = Shape.RECT;

    public void setShape(Shape shape) {
        this.mShape = shape;
        invalidate();
    }

    private void drawCircleCorners(@NonNull Canvas canvas, float centerX, float centerY, Paint paint) {
        canvas.drawCircle(centerX, centerY, mCornerLength / 2, paint);
    }

    int startX = 0;
    int startY = 0;

    Point mDragPoint;
    Point mCenterPoint;

    Polygon mCurrentActivePolygon;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // If this View is not enabled, don't allow for touch interactions.
        if (!isEnabled()) {
            return false;
        }
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                lastX = startX = (int) event.getX();
                lastY = startY = (int) event.getY();
                if (Shape.RECT == mShape) {
                    onPolygonActionDown();

                } else if (Shape.CIRCLE == mShape) {
                    onCircleActionDown();

                } else if (Shape.OCTAGON == mShape) {
                    onPolygonActionDown();

                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                getParent().requestDisallowInterceptTouchEvent(false);
                if (null != mCurrentActivePolygon) {
                    if (Shape.OCTAGON == mShape) {
                        int endX = (int) event.getX();
                        int endY = (int) event.getY();
                        if (Polygon.State.INIT == mCurrentActivePolygon.getState()) {
                            int dX = endX - startX;
                            int dY = endY - startY;
                            init(startX, startY, dX, dY);
                        }

                    }
                }
                return true;

            case MotionEvent.ACTION_MOVE:
                if (Shape.RECT == mShape) {
                    //onActionMove(event.getX(), event.getY());
                    onPolygonActionMove(event);
                } else if (Shape.CIRCLE == mShape) {
                    if (null != mCurrentActivePolygon && mCurrentActivePolygon.getPoints().size() > 0) {
                        if (50 > (Math.max(Math.abs(event.getX() - mCurrentActivePolygon.getPoint(0).x), Math.abs(event.getY() - mCurrentActivePolygon.getPoint(0).y)))) {
                            mCurrentActivePolygon.setPoint(0, new Point((int) event.getX(), (int) event.getY()));
                            invalidate();
                        } else if (100 > Math.max(Math.abs(event.getX() - mCenterX), Math.abs(event.getY() - mCenterY))) {
                            mCurrentActivePolygon.setPoint(0, new Point((mCurrentActivePolygon.getPoint(0).x + (int) (event.getX() - mCenterX)), mCurrentActivePolygon.getPoint(0).y + (int) (event.getY() - mCenterY)));
                            setCenterX(event.getX());
                            setCenterY(event.getY());
                            invalidate();
                        }
                    }

                } else if (Shape.OCTAGON == mShape) {
                    onPolygonActionMove(event);

                }
                getParent().requestDisallowInterceptTouchEvent(true);
                return true;

            default:
                return false;
        }
    }

    private void onCircleActionDown() {
        mCenterX = mCenterX == 0 ? (mBitmapRect.left + mBitmapRect.right) / 2 : mCenterX;
        mCenterY = mCenterY == 0 ? (mBitmapRect.top + mBitmapRect.bottom) / 2 : mCenterY;
        //第一步判断是否在编辑点 --- 拉伸
        //第二步判断是否在中心点 --- 移动
        //第三步为创建新的多边形
        if (null != (mDragPoint = attractPoint(startX, startY))) {
            mCurrentActivePolygon.setState(Polygon.State.DRAG);
        } else if (null != (mCenterPoint = attractCircleCenter(startX, startY))) {
            mCurrentActivePolygon.setState(Polygon.State.MOVE);
        } else if (null != (mCurrentActivePolygon = getUnActivedPolygon())) {
            mCurrentActivePolygon.setState(Polygon.State.INIT);
            ;
        } else {
            // Toast.makeText(getContext(), "最多只允许创建" + POLYGON_NUM_LIMIT + "个窗口", Toast.LENGTH_LONG).show();

        }
    }

    private void onPolygonActionMove(MotionEvent event) {
        if (null != mCurrentActivePolygon) {
            int endX = (int) event.getX();
            int endY = (int) event.getY();
            if (Polygon.State.INIT == mCurrentActivePolygon.getState()) {
                int dX = endX - startX;
                int dY = endY - startY;
                init(startX, startY, dX, dY);
            } else if (Polygon.State.DRAG == mCurrentActivePolygon.getState() && null != mDragPoint) {


                if (Shape.RECT == mShape) {

                    int dX = endX - mDragPoint.x;
                    int dY = endY - mDragPoint.y;

                    int index = mCurrentActivePolygon.indexOf(mDragPoint);
                    int preIndex = mCurrentActivePolygon.preview(index);
                    int nextIndex = mCurrentActivePolygon.next(index);

                    if (mCurrentActivePolygon.getPoint(preIndex).x == mDragPoint.x) {
                        mCurrentActivePolygon.updatePoint(preIndex, dX, 0);
                        mCurrentActivePolygon.updatePoint(nextIndex, 0, dY);
                    } else {
                        mCurrentActivePolygon.updatePoint(preIndex, 0, dY);
                        mCurrentActivePolygon.updatePoint(nextIndex, dX, 0);
                    }

                    mDragPoint.set(endX, endY);
                    invalidate();

                } else if (Shape.OCTAGON == mShape) {
                    mDragPoint.set(endX, endY);
                    invalidate();
                }


            } else if (Polygon.State.MOVE == mCurrentActivePolygon.getState() && null != mCenterPoint) {
                int dX = endX - lastX;
                int dY = endY - lastY;
                update(dX, dY);
                lastX = endX;
                lastY = endY;
            }
        }
    }


    private void onPolygonActionDown() {
        //第一步判断是否在编辑点 --- 拉伸
        //第二步判断是否在中心点 --- 移动
        //第三步为创建新的多边形
        if (null != (mDragPoint = attractPoint(startX, startY))) {
            mCurrentActivePolygon.setState(Polygon.State.DRAG);
        } else if (null != (mCenterPoint = attractCenter(startX, startY))) {
            mCurrentActivePolygon.setState(Polygon.State.MOVE);
        } else if (null != (mCurrentActivePolygon = getUnActivedPolygon())) {
            mCurrentActivePolygon.setState(Polygon.State.INIT);
            ;
        } else {
            // Toast.makeText(getContext(), "最多只允许创建" + POLYGON_NUM_LIMIT + "个窗口", Toast.LENGTH_LONG).show();

        }
    }

    private Polygon getUnActivedPolygon() {
        for (int polygonIndex = 0; polygonIndex < mPolygons.size(); polygonIndex++) {
            if (Polygon.State.UN_INIT == mPolygons.get(polygonIndex).getState()) {
                return mPolygons.get(polygonIndex);
            }
        }
        return null;
    }

    int lastX;
    int lastY;

    private void update(int dX, int dY) {
        mCurrentActivePolygon.updatePoints(dX, dY);
        invalidate();
    }


    private void init(int startX, int startY, int dX, int dY) {
        if (Shape.RECT == mShape) {
            mCurrentActivePolygon.setPoint(0, new Point(startX, startY));
            mCurrentActivePolygon.setPoint(1, new Point(startX, startY + dY));
            mCurrentActivePolygon.setPoint(2, new Point(startX + dX, startY + dY));
            mCurrentActivePolygon.setPoint(3, new Point(startX + dX, startY));
        } else if (Shape.OCTAGON == mShape) {
            mCurrentActivePolygon.setPoint(0, new Point(startX, startY));
            mCurrentActivePolygon.setPoint(1, new Point(startX, startY + dY / 2));
            mCurrentActivePolygon.setPoint(2, new Point(startX, startY + dY));
            mCurrentActivePolygon.setPoint(3, new Point(startX + dX / 2, startY + dY));
            mCurrentActivePolygon.setPoint(4, new Point(startX + dX, startY + dY));
            mCurrentActivePolygon.setPoint(5, new Point(startX + dX, startY + dY / 2));
            mCurrentActivePolygon.setPoint(6, new Point(startX + dX, startY));
            mCurrentActivePolygon.setPoint(7, new Point(startX + dX / 2, startY));
        }

        invalidate();
    }

    private Point attractCenter(int x, int y) {

        if (Shape.RECT == mShape) {
            for (int polygonIndex = 0; polygonIndex < mPolygons.size(); polygonIndex++) {
                Point ap = MathUtil.averagePolygon(mPolygons.get(polygonIndex).getPoints());
                if (null != ap) {
                    if (Math.abs(mPolygons.get(polygonIndex).getPoint(0).x - mPolygons.get(polygonIndex).getPoint(2).x) / 2 > Math.abs(x - ap.x) && Math.abs(mPolygons.get(polygonIndex).getPoint(0).y - mPolygons.get(polygonIndex).getPoint(2).y) / 2 > Math.abs(y - ap.y)) {
                        mCurrentActivePolygon = mPolygons.get(polygonIndex);
                        return ap;
                    }
                }
            }
        } else if (Shape.OCTAGON == mShape) {

            for (int polygonIndex = 0; polygonIndex < mPolygons.size(); polygonIndex++) {
                Point ap = MathUtil.averagePolygon(mPolygons.get(polygonIndex).getPoints());
                if (null != ap) {
                    if (100 > (Math.max(Math.abs(x - ap.x), Math.abs(y - ap.y)))) {
                        mCurrentActivePolygon = mPolygons.get(polygonIndex);
                        return ap;
                    }
                }
            }
        }

        return null;
    }

    private Point attractCircleCenter(int x, int y) {

        for (int polygonIndex = 0; polygonIndex < mPolygons.size(); polygonIndex++) {
            if (100 > (Math.max(Math.abs(x - mCenterX), Math.abs(y - mCenterY)))) {
                mCurrentActivePolygon = mPolygons.get(polygonIndex);
                return new Point((int) mCenterX, (int) mCenterY);

            }
        }

        return null;
    }


    public Point attractPoint(int x, int y) {
        for (int polygonIndex = 0; polygonIndex < mPolygons.size(); polygonIndex++) {
            for (int i = 0; i < mPolygons.get(polygonIndex).size(); i++) {
                if (50 > (Math.max(Math.abs(x - mPolygons.get(polygonIndex).getPoint(i).x), Math.abs(y - mPolygons.get(polygonIndex).getPoint(i).y)))) {
                    mCurrentActivePolygon = mPolygons.get(polygonIndex);
                    return mPolygons.get(polygonIndex).getPoint(i);
                }
            }
        }

        return null;
    }

    private RectF getBitmapRect() {

        final Drawable drawable = getDrawable();
        if (drawable == null) {
            return new RectF();
        }
        // Get image matrix values and place them in an array.
        final float[] matrixValues = new float[9];
        getImageMatrix().getValues(matrixValues);

        // Extract the scale and translation values from the matrix.
        final float scaleX = matrixValues[Matrix.MSCALE_X];
        final float scaleY = matrixValues[Matrix.MSCALE_Y];
        final float transX = matrixValues[Matrix.MTRANS_X];
        final float transY = matrixValues[Matrix.MTRANS_Y];

        // Get the width and height of the original bitmap.
        final int drawableIntrinsicWidth = drawable.getIntrinsicWidth();
        final int drawableIntrinsicHeight = drawable.getIntrinsicHeight();

        // Calculate the dimensions as seen on screen.
        final int drawableDisplayWidth = Math.round(drawableIntrinsicWidth * scaleX);
        final int drawableDisplayHeight = Math.round(drawableIntrinsicHeight * scaleY);

        // Get the Rect of the displayed image within the ImageView.
        final float left = Math.max(transX, 0);
        final float top = Math.max(transY, 0);
        final float right = Math.min(left + drawableDisplayWidth, getWidth());
        final float bottom = Math.min(top + drawableDisplayHeight, getHeight());

        return new RectF(left, top, right, bottom);
    }


    private void initCropWindow(@NonNull RectF bitmapRect) {
        if (Shape.RECT == mShape) {
            //initRect(bitmapRect);
            initPolygons(bitmapRect);
        } else if (Shape.CIRCLE == mShape) {
            //initRect(bitmapRect);
            initCircle(bitmapRect);
        } else if (Shape.OCTAGON == mShape) {
            //initRect(bitmapRect);
            initPolygons(bitmapRect);
        }
    }

    List<Polygon> mPolygons;

    private int POLYGON_NUM_LIMIT = 4;

    private int panelColors[] = {Color.parseColor("#7Fd82525"), Color.parseColor("#7F40b23a"), Color.parseColor("#7F04b0e8"), Color.parseColor("#7Fc4a602")};
    private int cornerColors[] = {Color.parseColor("#d82525"), Color.parseColor("#40b23a"), Color.parseColor("#04b0e8"), Color.parseColor("#c4a602")};


    private void initPolygons(@NonNull RectF bitmapRect) {
        mPolygons = new ArrayList<Polygon>();
        for (int i = 0; i < POLYGON_NUM_LIMIT; i++) {
            mPolygons.add(new Polygon(panelColors[i], cornerColors[i]));
        }
    }

    public void reset() {
        for (int i = 0; i < POLYGON_NUM_LIMIT; i++) {
            mPolygons.get(i).clear();
            mPolygons.get(i).setState(Polygon.State.UN_INIT);
        }
        invalidate();
    }

    private void initCircle(@NonNull RectF bitmapRect) {
        mPolygons = new ArrayList<Polygon>();
        for (int i = 0; i < POLYGON_NUM_LIMIT; i++) {
            mPolygons.add(new Polygon());
            mPolygons.get(i).addPoint(new Point(600, 400));
        }
    }

    /**
     * @param polygonIndex: 多边形下标
     * @param M:            水平栅格数
     * @param m:            x 比值
     * @param n             y 比值
     * @return
     */
    public List<Integer> toMatrix(int polygonIndex, int M, int m, int n) {
        if (polygonIndex > mPolygons.size() - 1) {
            return null;
        }
        List<Integer> ret = mPolygons.get(polygonIndex).getDirtyRectGrid(M, m, n);
        if (null != ret) {
            Collections.sort(ret);
            Log.e(TAG, ret.toString());
        }
        return ret;
    }
}
