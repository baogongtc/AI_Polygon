package com.bd.polygon;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;

import java.nio.ByteBuffer;

public class MainActivity extends Activity {

    private PolygonView mPv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mPv = (PolygonView) findViewById(R.id.polygonView);

    }

    public void onCancel(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("警告");
        builder.setMessage("确定取消么？");
        builder.setNegativeButton("返回编辑", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("确定取消", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //MainActivity.this.finish();
            }
        });
        builder.create().show();
    }

    public void onReset(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
        builder.setTitle("警告");
        builder.setMessage("确定取消么？");
        builder.setNegativeButton("返回编辑", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        builder.setPositiveButton("确定重置", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                mPv.reset();
            }
        });
        builder.create().show();
    }

    public void onCommit(View v) {
        //横屏，宽高交换
        int width = getWindowManager().getDefaultDisplay().getWidth();
        int height = getWindowManager().getDefaultDisplay().getHeight();
        mPv.toMatrix(0,22,height/22,width/18);
        mPv.toMatrix(1,22,height/22,width/18);
        mPv.toMatrix(2,22,height/22,width/18);
        mPv.toMatrix(3,22,height/22,width/18);
    }
}
