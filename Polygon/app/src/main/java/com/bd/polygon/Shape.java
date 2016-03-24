package com.bd.polygon;

/**
 * Description : <Content><br>
 * CreateTime : 2016/3/22 10:14
 *
 * @author KevinLiu
 * @version <v1.0>
 * @Editor : KevinLiu
 * @ModifyTime : 2016/3/22 10:14
 * @ModifyDescription : <Content>
 */
public enum Shape {

    RECT(0),//矩形
    OVAL(1),//椭圆
    CIRCLE(2),//圆
    HEXAGON(3), //六边形
    OCTAGON(4);//八边形

    int value;

    Shape(int type) {
        this.value = type;
    }

    public static Shape getType(int type){
        switch (type){
            case 0:
                return RECT;
            case 1:
                return OVAL;
            case 2:
                return CIRCLE;
            case 3:
                return HEXAGON;
            case 4:
                return OCTAGON;
        }
        return  RECT;
    }
}
