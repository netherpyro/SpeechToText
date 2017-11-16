package com.issart.speechtotext;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import ru.yandex.speechkit.Error;
import ru.yandex.speechkit.PhraseSpotter;
import ru.yandex.speechkit.PhraseSpotterListener;
import ru.yandex.speechkit.PhraseSpotterModel;
import ru.yandex.speechkit.Recognition;
import ru.yandex.speechkit.Recognizer;
import ru.yandex.speechkit.RecognizerListener;
import ru.yandex.speechkit.SpeechKit;

/**
 * Skeleton of an Android Things activity.
 * <p>
 * Android Things peripheral APIs are accessible through the class
 * PeripheralManagerService. For example, the snippet below will open a GPIO pin and
 * set it to HIGH:
 * <p>
 * <pre>{@code
 * PeripheralManagerService service = new PeripheralManagerService();
 * mLedGpio = service.openGpio("BCM6");
 * mLedGpio.setDirection(Gpio.DIRECTION_OUT_INITIALLY_LOW);
 * mLedGpio.setValue(true);
 * }</pre>
 * <p>
 * For more complex peripherals, look for an existing user-space driver, or implement one if none
 * is available.
 *
 * @see <a href="https://github.com/androidthings/contrib-drivers#readme">https://github.com/androidthings/contrib-drivers#readme</a>
 */
public class MainActivity extends Activity implements RecognizerListener, PhraseSpotterListener {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final String API_KEY_FOR_TESTS_ONLY = "88f01949-8470-467f-acef-d10aa292a1ed";
    private static final String API_KEY = "88f01949-8470-467f-acef-d10aa292a1ed";

    private ProgressBar progressBar;
    private TextView recognitionStatus;
    private TextView spotterStatus;
    private TextView currentLang;
    private TextView recognitionResult;
    private Button startBtn;
    private Button cancelBtn;

    private Recognizer mRecognizer;
    private String lang = Recognizer.Language.RUSSIAN;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate()");
        setContentView(R.layout.activity_main);

        SpeechKit.getInstance().configure(this, API_KEY_FOR_TESTS_ONLY);
        SpeechKit.getInstance().setParameter("disableantimat", "true");

        // Specify the PhraseSpotter model (check the asset folder for the used one).
        PhraseSpotterModel model = new PhraseSpotterModel("phrase-spotter/commands");
        // Don't forget to load the model.
        Error loadResult = model.load();
        if (loadResult.getCode() != Error.ERROR_OK) {
            updateSpotterStatus("Error occurred during model loading: " + loadResult.getString());
        } else {
            // Set the listener.
            PhraseSpotter.setListener(this);
            // Set the model.
            Error setModelResult = PhraseSpotter.setModel(model);
            handleError(setModelResult);
        }
        startBtn = findViewById(R.id.start_btn);
        startBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createAndStartRecognizer();
            }
        });

        cancelBtn = findViewById(R.id.cancel_btn);
        cancelBtn.setEnabled(false);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetRecognizer();
            }
        });

        progressBar = findViewById(R.id.voice_power_bar);
        recognitionStatus = findViewById(R.id.recognition_state_text);
        spotterStatus = findViewById(R.id.spotter_state_text);
        recognitionResult = findViewById(R.id.result);
        currentLang = findViewById(R.id.language_text);
        currentLang.setText(lang);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart()");
        // Don't forget to call start.
        startPhraseSpotter();
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop()");
        Error stopResult = PhraseSpotter.stop();
        handleError(stopResult);
        resetRecognizer();
    }

    private void resetRecognizer() {
        if (mRecognizer != null) {
            mRecognizer.cancel();
            mRecognizer = null;
        }

        startBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
    }

    @Override
    public void onRecordingBegin(Recognizer recognizer) {
        Log.d(TAG, "onRecordingBegin()");
        updateRecognitionStatus("Recording begin");
    }

    @Override
    public void onSpeechDetected(Recognizer recognizer) {
        Log.d(TAG, "onSpeechDetected()");
        updateRecognitionStatus("Speech detected");
    }

    @Override
    public void onSpeechEnds(Recognizer recognizer) {
        Log.d(TAG, "onSpeechEnds()");
        updateRecognitionStatus("Speech ends");
    }

    @Override
    public void onRecordingDone(Recognizer recognizer) {
        Log.d(TAG, "onRecordingDone()");
        updateRecognitionStatus("Recording done");
        startBtn.setEnabled(true);
        cancelBtn.setEnabled(false);
    }

    @Override
    public void onSoundDataRecorded(Recognizer recognizer, byte[] bytes) {
    }

    @Override
    public void onPowerUpdated(Recognizer recognizer, float power) {
        updateProgress((int) (power * progressBar.getMax()));
    }

    @Override
    public void onPartialResults(Recognizer recognizer, Recognition recognition, boolean b) {
        updateRecognitionStatus("Partial results " + recognition.getBestResultText());
    }

    @Override
    public void onRecognitionDone(Recognizer recognizer, Recognition recognition) {
        Log.d(TAG, "onRecognitionDone()");
        updateRecognitionResult(recognition.getBestResultText());
        updateProgress(0);
    }

    @Override
    public void onError(Recognizer recognizer, ru.yandex.speechkit.Error error) {
        if (error.getCode() == Error.ERROR_CANCELED) {
            Log.d(TAG, "onError():: cancelled");
            updateRecognitionStatus("Cancelled");
            updateProgress(0);
        } else {
            Log.d(TAG, "onError()::" + error.getString());
            updateRecognitionStatus("Error occurred " + error.getString());
            resetRecognizer();
        }
    }

    private void createAndStartRecognizer() {
        // Reset the current recognizer.
        resetRecognizer();
        // To create a new recognizer, specify the language, the model - a scope of recognition to get the most appropriate results,
        // set the listener to handle the recognition events.
        mRecognizer = Recognizer.create(lang, Recognizer.Model.NOTES, this);
        // Don't forget to call start on the created object.
        mRecognizer.start();
        startBtn.setEnabled(false);
        cancelBtn.setEnabled(true);
    }

    private void updateRecognitionResult(String text) {
        recognitionResult.setText(text);
    }

    private void updateRecognitionStatus(final String text) {
        recognitionStatus.setText(text);
    }

    private void updateProgress(int progress) {
        progressBar.setProgress(progress);
    }

    //region SPOTTER
    private void startPhraseSpotter() {
        Log.d(TAG, "startPhraseSpotter()");
        Error startResult = PhraseSpotter.start();
        handleError(startResult);
    }

    private void handleError(Error error) {
        if (error.getCode() != Error.ERROR_OK) {
            updateSpotterStatus("Error occurred: " + error.getString());
        }
    }

    private void switchLanguage(int langCode) {
        switch (langCode) {
            case 8:
                lang = Recognizer.Language.RUSSIAN;
                break;
            case 6:
                lang = Recognizer.Language.ENGLISH;
                break;
            case 2:
                lang = Recognizer.Language.TURKISH;
                break;
            case 3:
                lang = Recognizer.Language.UKRAINIAN;
                break;
        }

        currentLang.setText(lang);
    }

    @Override
    public void onPhraseSpotted(String s, int i) {
        Log.d(TAG, "onPhraseSpotted()::the phrase is " + s);
        if (i == 6 || i == 8) {
            switchLanguage(i);
        } else if (i == 7) {
            createAndStartRecognizer();
        }
    }

    @Override
    public void onPhraseSpotterStarted() {
        Log.d(TAG, "onPhraseSpotterStarted()");
        updateSpotterStatus("Started");
    }

    @Override
    public void onPhraseSpotterStopped() {
        Log.d(TAG, "onPhraseSpotterStopped()");
        updateSpotterStatus("Stopped");
    }

    @Override
    public void onPhraseSpotterError(Error error) {
        Log.d(TAG, "onPhraseSpotterError()");
        handleError(error);
    }

    private void updateSpotterStatus(final String text) {
        spotterStatus.setText(text);
    }
    //endregion
}
