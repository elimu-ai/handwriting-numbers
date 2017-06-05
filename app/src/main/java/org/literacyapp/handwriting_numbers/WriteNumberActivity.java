package org.literacyapp.handwriting_numbers;

import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.TextView;

import org.literacyapp.contentprovider.ContentProvider;
import org.literacyapp.contentprovider.dao.AudioDao;
import org.literacyapp.contentprovider.dao.DaoSession;
import org.literacyapp.contentprovider.model.content.Number;
import org.literacyapp.handwriting_numbers.util.MediaPlayerHelper;
import org.literacyapp.handwriting_numbers.view.DrawModel;
import org.literacyapp.handwriting_numbers.view.DrawView;
import org.literacyapp.handwriting_numbers.view.DrawViewOnTouchListener;
import org.tensorflow.contrib.android.TensorFlowInferenceInterface;

import java.util.List;

public class WriteNumberActivity extends AppCompatActivity implements View.OnTouchListener {

    private AudioDao audioDao;
    private Number number;

    private static final int PIXEL_WIDTH = 28;

    private DrawModel mModel;
    private DrawView mDrawView;

    private static final String MODEL_FILE = "file:///android_asset/expert-graph.pb";

    private TensorFlowInferenceInterface inferenceInterface;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(getClass().getName(), "onCreate");
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_write);

        DaoSession daoSession = ContentProvider.getDaoSession();
        audioDao = daoSession.getAudioDao();

        List<Number> unlockedNumbers = ContentProvider.getUnlockedNumbers();
        number = unlockedNumbers.get((int)(Math.random() * unlockedNumbers.size()));
        Log.i(getClass().getName(), "number: " + number);
        TextView textView = (TextView) findViewById(R.id.textView);
        textView.setText(number.getValue().toString());
        // Set on listener to restart the drawing with a blank screen
        textView.setOnTouchListener(this);

        initTensorFlowAndLoadModel();

        mModel = new DrawModel(PIXEL_WIDTH, PIXEL_WIDTH);

        mDrawView = (DrawView) findViewById(R.id.view_draw);
        mDrawView.setModel(mModel);
        DrawViewOnTouchListener listener = new DrawViewOnTouchListener(mDrawView, mModel, inferenceInterface, number.getValue(), getApplicationContext());
        mDrawView.setOnTouchListener(listener);
    }

    private void initTensorFlowAndLoadModel() {
        Log.i(getClass().getName(), "initTensorFlowAndLoadModel");
        try {
            inferenceInterface = new TensorFlowInferenceInterface(getAssets(), MODEL_FILE);
            Log.d(getClass().getName(), "Load Success");
        } catch (final Exception e) {
            throw new RuntimeException("Error initializing TensorFlow!", e);
        }
    }

    @Override
    protected void onResume() {
        Log.i(getClass().getName(), "onResume");
        super.onResume();

        mDrawView.onResume();

        MediaPlayer mediaPlayer = MediaPlayerHelper.playInstructionSound(getApplicationContext());
        if (mediaPlayer != null){
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    MediaPlayerHelper.playNumberSound(getApplicationContext(), audioDao, number);
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mDrawView.onPause();
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        mModel.clear();
        mDrawView.reset();
        mDrawView.invalidate();
        return true;
    }
}
