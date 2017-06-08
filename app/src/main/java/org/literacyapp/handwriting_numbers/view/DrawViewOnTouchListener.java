package org.literacyapp.handwriting_numbers.view;

import android.content.Context;
import android.content.Intent;
import android.graphics.PointF;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Toast;

import org.literacyapp.handwriting_numbers.FinalActivity;
import org.literacyapp.handwriting_numbers.util.MediaPlayerHelper;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

/**
 * Created by sladomic on 05.05.17.
 */

public class DrawViewOnTouchListener implements View.OnTouchListener {
    private int failedCounter = 0;
    private PointF mTmpPoint = new PointF();

    private float mLastX;
    private float mLastY;

    private DrawView mDrawView;
    private DrawModel mModel;

    private TensorFlowInferenceInterface inferenceInterface;

    private int numberToWrite;

    private static final int INPUT_SIZE = 28;
    private static final String INPUT_NAME = "input";
    private static final String OUTPUT_NAME = "output";
    // 1 channel because it's a grayscale image
    private static final int CHANNELS = 1;
    private static final boolean LOG_STATS = false;
    private static final int OUTPUT_SIZE = 10;
    private static final float THRESHOLD = 1.0f;

    private Context context;

    public DrawViewOnTouchListener(DrawView mDrawView, DrawModel mModel, TensorFlowInferenceInterface inferenceInterface, int numberToWrite, Context context) {
        this.mDrawView = mDrawView;
        this.mModel = mModel;
        this.inferenceInterface = inferenceInterface;
        this.numberToWrite = numberToWrite;
        this.context = context;
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        int action = event.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN) {
            processTouchDown(event);
            return true;

        } else if (action == MotionEvent.ACTION_MOVE) {
            processTouchMove(event);
            return true;

        } else if (action == MotionEvent.ACTION_UP) {
            processTouchUp();
            return true;
        }
        return false;
    }

    private void processTouchDown(MotionEvent event) {
        mLastX = event.getX();
        mLastY = event.getY();
        mDrawView.calcPos(mLastX, mLastY, mTmpPoint);
        float lastConvX = mTmpPoint.x;
        float lastConvY = mTmpPoint.y;
        mModel.startLine(lastConvX, lastConvY);
    }

    private void processTouchMove(MotionEvent event) {
        float x = event.getX();
        float y = event.getY();

        mDrawView.calcPos(x, y, mTmpPoint);
        float newConvX = mTmpPoint.x;
        float newConvY = mTmpPoint.y;
        mModel.addLineElem(newConvX, newConvY);

        mLastX = x;
        mLastY = y;
        mDrawView.invalidate();
    }

    private void processTouchUp() {
        mModel.endLine();
        float pixels[] = mDrawView.getPixelData(INPUT_SIZE);

        inferenceInterface.feed(INPUT_NAME, pixels, 1, INPUT_SIZE, INPUT_SIZE, CHANNELS);
        inferenceInterface.run(new String[]{OUTPUT_NAME}, LOG_STATS);
        float[] outputs = new float[OUTPUT_SIZE];
        inferenceInterface.fetch(OUTPUT_NAME, outputs);

        if (outputs.length > 0) {
            int recognizedNumber = -1;
            for (int i=0; i < outputs.length; i++){
                if (outputs[i] >= THRESHOLD){
                    recognizedNumber = i;
                }
            }
            Log.i(getClass().getName(), "numberToWrite: " + numberToWrite + ", recognized number: " + recognizedNumber);
            if (numberToWrite == recognizedNumber){
                Intent intent = new Intent(context, FinalActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                failedCounter++;
                if (failedCounter > 2){
                    mModel.clear();
                    mDrawView.reset();
                    mDrawView.invalidate();
                    MediaPlayerHelper.playLessonFailed(context);
                    failedCounter = 0;
                }
            }
        }
    }
}
