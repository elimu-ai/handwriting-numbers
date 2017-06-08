package org.literacyapp.handwriting_numbers.util;

import android.content.Context;
import android.content.res.Resources;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.Log;

import org.literacyapp.contentprovider.dao.AudioDao;
import org.literacyapp.contentprovider.model.content.Letter;
import org.literacyapp.contentprovider.model.content.Number;
import org.literacyapp.contentprovider.model.content.multimedia.Audio;
import org.literacyapp.contentprovider.util.MultimediaHelper;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class which helps releasing the {@link MediaPlayer} instance after
 * finishing playing the audio.
 * <p />
 *
 * See https://developer.android.com/reference/android/media/MediaPlayer.html#create%28android.content.Context,%20int%29
 */
public class MediaPlayerHelper {
    public static final long DEFAULT_PLAYER_DELAY = 1000;

    private static final String INSTRUCTION_NUMBER_1 = "can_you_draw_the_number";
    private static final String INSTRUCTION_NUMBER_2 = "draw_the_number";
    private static final String INSTRUCTION_NUMBER_3 = "now_try_to_draw_the_number";
    private static final String INSTRUCTION_NUMBER_4 = "use_your_finger_to_draw_the_number";

    private static final String LESSON_COMPLETED_1 = "amazing";
    private static final String LESSON_COMPLETED_2 = "fantastic";
    private static final String LESSON_COMPLETED_3 = "great";
    private static final String LESSON_COMPLETED_4 = "great_job";
    private static final String LESSON_COMPLETED_5 = "nice";
    private static final String LESSON_COMPLETED_6 = "well_done";

    private static final String LESSON_FAILED_1 = "try_again";

    public static MediaPlayer play(Context context, int resId) {
        Log.i(MediaPlayerHelper.class.getName(), "play");

        final MediaPlayer mediaPlayer = MediaPlayer.create(context, resId);
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mediaPlayer.release();
            }
        });
        mediaPlayer.start();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();
            }
        });
        return mediaPlayer;
    }

    public static MediaPlayer playInstructionSound(Context context){
        Log.i(context.getClass().getName(), "playInstructionSound");

        List<String> instructionList = new ArrayList<>();
        instructionList.add(INSTRUCTION_NUMBER_1);
        instructionList.add(INSTRUCTION_NUMBER_2);
        instructionList.add(INSTRUCTION_NUMBER_3);
        instructionList.add(INSTRUCTION_NUMBER_4);

        return playRandomResource(context, instructionList);
    }

    public static void playNumberSound(Context context, AudioDao audioDao, Number number){
        Log.i(context.getClass().getName(), "playNumberSound");

        playSound(context, audioDao, number.getValue().toString(), Number.class);
    }

    private static void playSound(Context context, AudioDao audioDao, String text, Class type) {
        Log.i(context.getClass().getName(), "playSound");

        // Look up corresponding Audio
        final Audio audio;
        if (type == Letter.class){
            Log.d(context.getClass().getName(), "Looking up \"letter_sound_" + text + "\"");
            audio = audioDao.queryBuilder()
                    .where(AudioDao.Properties.Transcription.eq("letter_sound_" + text))
                    .unique();
        } else {
            Log.d(context.getClass().getName(), "Looking up \"digit_" + text + "\"");
            audio = audioDao.queryBuilder()
                    .where(AudioDao.Properties.Transcription.eq("digit_" + text))
                    .unique();
        }
        Log.i(context.getClass().getName(), "audio: " + audio);
        if (audio != null) {
            // Play audio
            File audioFile = MultimediaHelper.getFile(audio);
            if (audioFile.exists()){
                Uri uri = Uri.parse(audioFile.getAbsolutePath());
                MediaPlayer mediaPlayer = MediaPlayer.create(context, uri);
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mediaPlayer) {
                        Log.i(getClass().getName(), "onCompletion");
                        mediaPlayer.release();
                    }
                });
                mediaPlayer.start();
            } else {
                // Audio not found. Fall-back to application resource.
                playSoundFromAppResources(context, text, type);
            }
        } else {
            // Audio not found. Fall-back to application resource.
            playSoundFromAppResources(context, text, type);
        }
    }

    private static void playSoundFromAppResources(Context context, String text, Class type){
        Log.i(context.getClass().getName(), "playSoundFromAppResources");

        String audioFileName;
        if (type == Letter.class){
            audioFileName = "letter_sound_" + text;
        } else {
            audioFileName = "digit_" + text;
        }
        int resourceId = context.getResources().getIdentifier(audioFileName, "raw", context.getPackageName());
        try {
            if (resourceId != 0) {
                play(context, resourceId);
            }
        } catch (Resources.NotFoundException e) {
            Log.e(context.getClass().getName(), null, e);
        }
    }

    public static MediaPlayer playLessonCompleted(Context context){
        Log.i(context.getClass().getName(), "playLessonCompleted");

        List<String> lessonCompletedList = new ArrayList<>();
        lessonCompletedList.add(LESSON_COMPLETED_1);
        lessonCompletedList.add(LESSON_COMPLETED_2);
        lessonCompletedList.add(LESSON_COMPLETED_3);
        lessonCompletedList.add(LESSON_COMPLETED_4);
        lessonCompletedList.add(LESSON_COMPLETED_5);
        lessonCompletedList.add(LESSON_COMPLETED_6);

        return playRandomResource(context, lessonCompletedList);
    }

    public static MediaPlayer playLessonFailed(Context context){
        Log.i(context.getClass().getName(), "playLessonFailed");

        List<String> lessonFailedList = new ArrayList<>();
        lessonFailedList.add(LESSON_FAILED_1);

        return playRandomResource(context, lessonFailedList);
    }


    private static MediaPlayer playRandomResource(Context context, List<String> list){
        Resources resources = context.getResources();
        int resourceId = resources.getIdentifier(list.get((int)(Math.random() * list.size())), "raw", context.getPackageName());
        MediaPlayer mediaPlayer = null;
        try {
            if (resourceId != 0) {
                mediaPlayer = play(context, resourceId);
            }
        } catch (Resources.NotFoundException e) {
            Log.e(context.getClass().getName(), null, e);
        }
        return mediaPlayer;
    }
}
