package edmt.dev.androidcamera2api;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.chaquo.python.*;
import com.chaquo.python.android.AndroidPlatform;

import java.io.FileNotFoundException;
import java.lang.*;
import java.io.File;
import java.io.FileOutputStream;
import java.lang.reflect.Array;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Objects;
import java.util.Vector;

public class ResultActivity extends AppCompatActivity {
    // TTS speaker
    TextToSpeech tts;

    // imageView object for captured image
    // Layout for captured result
    ImageView imageView;
    LinearLayout capturedLayout;
    LinearLayout resultLayout;

    TextView objectText;
    TextView quantityText;
    TextView percentageText;

    // status of interpretation
    int interpreted;
    int informed = 0;
    int currentObject = 0;

    // source and result path of the scene image
    String sourcePath = Environment.getExternalStorageDirectory() + "/Captured_Image.jpg";
    String resultPath = Environment.getExternalStorageDirectory() + "/Analysed_Image.jpg";

    // bitmap holder for the image
    Bitmap capturedBitmap;
    Bitmap resultBitmap;

    // decimal format to 2 dp
    DecimalFormat decimalPointFormat = new DecimalFormat(".##");  // formatting of decimal point

    // array that hold the result for information
    ArrayList<String> uniqueObject = new ArrayList<String>();
    ArrayList<Integer> quantity = new ArrayList<Integer>();
    ArrayList<Float> totalProbability = new ArrayList<Float>();
    int objectCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // set the content of this screen
        setContentView(R.layout.activity_result);

        // find the object for image view
        capturedLayout = findViewById(R.id.imageViewLayout);
        imageView = findViewById(R.id.imageView);

        // status of interpretation set to 0 (not interpreted)
        interpreted = 0;

        // initialise the TTS speaker
        tts = new TextToSpeech(ResultActivity.this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.ENGLISH);
                    tts.speak("Image captured successfully. Tab the screen to proceed with interpretation",
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "InstructionID");
                } else {
                    Toast.makeText(getApplicationContext(), "Feature not supported on your device", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // set the image for viewing
        capturedBitmap = BitmapFactory.decodeFile(sourcePath);
        imageView.setImageBitmap(capturedBitmap);

        // set the on click handler for this screen
        capturedLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                    interpreted = 1;
                    capturedBitmap.recycle();
                    if(tts.isSpeaking()){
                        tts.stop();
                    }
                    Intent myIntent = new Intent(getBaseContext(),   InterpretationActivity.class);
                    startActivityForResult(myIntent,1);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(interpreted == 1) {
            setContentView(R.layout.activity_result_interpreted);
            resultLayout = findViewById(R.id.resultViewLayout);

            ImageView resultView = findViewById(R.id.resultView);
            objectText = findViewById(R.id.textView6);
            quantityText = findViewById(R.id.textView7);
            percentageText = findViewById(R.id.textView8);

            resultBitmap = BitmapFactory.decodeFile(resultPath);
            resultView.setImageBitmap(resultBitmap);

            resultLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (informed == 0) {
                        if (objectCount == 0) {
                            objectText.setText("  Object: No Object detected");
                            quantityText.setText("  Quantity: --");
                            percentageText.setText("  Average Probability: --");
                            informed = 1;
                        }

                        if (currentObject < uniqueObject.size()) {
                            objectText.setText("  Object: " + uniqueObject.get(currentObject));
                            quantityText.setText("  Quantity: " + Integer.toString(quantity.get(currentObject)));
                            percentageText.setText("  Average Percentage: " + String.valueOf(decimalPointFormat.format(totalProbability.get(currentObject) / quantity.get(currentObject))));

                            percentageText.post(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        Thread.sleep(750);
                                        String sentence;
                                        while (tts.isSpeaking()) ;
                                        sentence = "Object " + String.valueOf(currentObject + 1);
                                        tts.speak(sentence,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "InstructionID");
                                        while (tts.isSpeaking()) ;
                                        tts.speak(uniqueObject.get(currentObject),
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "InstructionID");

                                        while (tts.isSpeaking()) ;
                                        sentence = "Quantity. " + Integer.toString(quantity.get(currentObject));
                                        tts.speak(sentence,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "InstructionID");

                                        while (tts.isSpeaking()) ;
                                        sentence = "Average percentage. " + String.valueOf(decimalPointFormat.format(totalProbability.get(currentObject) / quantity.get(currentObject)));
                                        tts.speak(sentence,
                                                TextToSpeech.QUEUE_FLUSH,
                                                null,
                                                "InstructionID");
                                        currentObject = currentObject + 1;

                                        if (currentObject == uniqueObject.size()) {
                                            while (tts.isSpeaking()) ;
                                            informed = 1;
                                            tts.speak("All of the detected object has been informed. Tab screen to end and start new interpretation.",
                                                    TextToSpeech.QUEUE_FLUSH,
                                                    null,
                                                    "InstructionID");
                                        } else {
                                            while (tts.isSpeaking()) ;
                                            tts.speak("Tab screen for detail of next object",
                                                    TextToSpeech.QUEUE_FLUSH,
                                                    null,
                                                    "InstructionID");
                                        }
                                    } catch (Exception e) {
                                        e.getLocalizedMessage();
                                    }

                                }
                            });
                        }
                    } else {
                        finish();
                        resultBitmap.recycle();
                    }
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        tts.speak("Interpretation completed. Summarizing the result.",
                TextToSpeech.QUEUE_FLUSH,
                null,
                "InstructionID");

        if(resultCode == Activity.RESULT_OK) {
            capturedBitmap.recycle();

            objectCount = data.getIntExtra("objectCount", 0);

            if(objectCount != 0){
                String[] detectionResult = new String[objectCount];
                String[] probability = new String[objectCount];

                probability = data.getStringArrayExtra("probability");
                detectionResult = data.getStringArrayExtra("detectedObjects");

                for(int i = 0; i<objectCount; i++)
                {
                    if(i == 0)
                    {
                        uniqueObject.add(detectionResult[i]);
                        quantity.add(1);
                        totalProbability.add(Float.parseFloat(probability[i]));
                    }

                    else
                    {
                        if(!(uniqueObject.contains(detectionResult[i]))){
                            uniqueObject.add(detectionResult[i]);
                            quantity.add(1);
                            totalProbability.add(Float.parseFloat(probability[i]));
                        }

                        else{
                            quantity.set(uniqueObject.indexOf(detectionResult[i]), quantity.get(uniqueObject.indexOf(detectionResult[i]))+1);
                            totalProbability.set(uniqueObject.indexOf(detectionResult[i]), totalProbability.get(uniqueObject.indexOf(detectionResult[i]))+Float.parseFloat(probability[i]));
                        }
                    }
                }
            }

            else{
                while(tts.isSpeaking());
                tts.speak("No object was detected.",
                        TextToSpeech.QUEUE_FLUSH,
                        null,
                        "InstructionID");
            }
        }
    }

    @Override
    protected void onDestroy() {
        // Stop the TextToSpeech Engine
        tts.stop();
        tts.shutdown();
        //rotatedBitmap.recycle();
        capturedBitmap.recycle();
        resultBitmap.recycle();
        capturedBitmap = null;
        resultBitmap = null;

        System.runFinalization();
        Runtime.getRuntime().gc();
        System.gc();

        super.onDestroy();
    }
}
