package edmt.dev.androidcamera2api;

import android.app.Activity;
import android.content.Intent;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.PyObject;
import com.chaquo.python.Python;
import com.chaquo.python.android.AndroidPlatform;

import java.util.Locale;

public class InterpretationActivity extends AppCompatActivity {
    TextToSpeech tts;
    LinearLayout myScreen;
    TextView startView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_interpretation);
        myScreen = findViewById(R.id.interpretScreen);
        startView = findViewById(R.id.textView4);
        tts = new TextToSpeech(InterpretationActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.speak("Before starting, it was informed that the interpretation process takes a few minutes to be completed. Tab the screen when you are ready to start.",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "InstructionID");
                } else {
                    Toast.makeText(getApplicationContext(), "Feature not supported on your device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        myScreen.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startView.setText(" Interpretation in Progress ... ");

                startView.post(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Thread.sleep(300);
                            tts.speak("Starting interpretation and notification will be made when completed.",
                                    TextToSpeech.QUEUE_FLUSH,
                                    null,
                                    "InstructionID");

                            if (! Python.isStarted()) {
                                Python.start(new AndroidPlatform(InterpretationActivity.this));
                            }

                            Python py = Python.getInstance();
                            String sourcePath = Environment.getExternalStorageDirectory() + "/Captured_Image.jpg";
                            String resultPath = Environment.getExternalStorageDirectory() + "/Analysed_Image.jpg";
                            // should change the file path to use asset
                            String modelPath = Environment.getExternalStorageDirectory() + "/resnet50_coco_best_v2.0.1.h5";
                            PyObject builtIns = py.getBuiltins();
                            PyObject imageai = py.getModule("imageai.Detection");
                            PyObject detector = imageai.callAttr("ObjectDetection");
                            detector.callAttr("setModelTypeAsRetinaNet");
                            detector.callAttr("setModelPath",modelPath);
                            detector.callAttr("loadModel","fast");
                            PyObject detections = detector.callAttr("detectObjectsFromImage", sourcePath, resultPath, "file","file",false,30,true,true);

                            // Return detected object
                            PyObject count = builtIns.callAttr("len",detections);
                            String sCount = count.toString();
                            int myCount = Integer.parseInt(sCount);

                            PyObject myDict;
                            String[] detectedObjects = new String[myCount];
                            String[] probability = new String[myCount];

                            for(int i = 0; i < myCount; i++)
                            {
                                myDict = detections.callAttr("__getitem__", i);
                                detectedObjects[i] = myDict.callAttr("__getitem__","name").toString();
                                probability[i] = myDict.callAttr("__getitem__", "percentage_probability").toString();
                            }

                            Intent resultDone = new Intent();
                            resultDone.putExtra("objectCount", myCount);
                            resultDone.putExtra("detectedObjects", detectedObjects);
                            resultDone.putExtra("probability", probability);
                            setResult(Activity.RESULT_OK, resultDone);
                            finish();

                        } catch (Exception e) {
                            e.getLocalizedMessage();
                        }
                    }
                });
            }
        });
    }

    @Override
    protected void onDestroy() {
        // Stop the TextToSpeech Engine
        tts.stop();
        tts.shutdown();
        tts = null;
        myScreen = null;
        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();

        super.onDestroy();
    }
}
