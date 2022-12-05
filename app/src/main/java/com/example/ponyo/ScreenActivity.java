package com.example.ponyo;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionText;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.google.firebase.ml.vision.text.RecognizedLanguage;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ScreenActivity extends AppCompatActivity {

    private static final String TAG = "MediaProjectionDemo";
    private static final int PERMISSION_CODE = 1;
    private int mScreenDensity;
    private MediaProjectionManager mProjectionManager;
    private int mDisplayWidth;
    private int mDisplayHeight;
    private MediaProjection mMediaProjection;
    private VirtualDisplay mVirtualDisplay;
    private ImageView mImageView;

    private static int IMAGES_PRODUCED;
    private ImageReader mImageReader;

    private Button endBtn;

    private Bitmap bitmap = null;

    static ArrayList<String> arrayKo =  new ArrayList<String>();
    static ArrayList<String> arrayEn =  new ArrayList<String>();
    static ArrayList<String> arrayIndex = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_screen);
        IMAGES_PRODUCED = 0;

        Intent intent = new Intent(this, BackgroundService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }

        getFirebaseDatabase();

        endBtn =  (Button) findViewById(R.id.endButton);
        endBtn.setVisibility(View.INVISIBLE);
        endBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("test","onClick ");
                finish();
            }
        });

        mImageView = findViewById(R.id.imageView);

        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        mScreenDensity = metrics.densityDpi;
        mProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        mDisplayWidth = metrics.widthPixels;
        mDisplayHeight = metrics.heightPixels;
        shareScreen();

    }

    @Override
    protected void onStop() {
        stopScreenSharing();
        super.onStop();
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        Intent intent = new Intent(this, BackgroundService.class);
        stopService(intent);
        if (mMediaProjection != null) {
            mMediaProjection.stop();
            mMediaProjection = null;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != PERMISSION_CODE) {
            Log.e(TAG, "Unknown request code: " + requestCode);
            Log.e("permission", "no");
            return;
        }
        if (resultCode != RESULT_OK) {
            Toast.makeText(this,
                    "User denied screen sharing permission", Toast.LENGTH_SHORT).show();
            Log.e("permission", "ok");
            finish();
            return;
        }
        mMediaProjection = mProjectionManager.getMediaProjection(resultCode, data);
        mMediaProjection.registerCallback(new MediaProjectionCallback(), null);

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                shareScreen();
            }
        }, 500);

    }
    private class ImageAvailableListener implements ImageReader.OnImageAvailableListener {
        @Override
        public void onImageAvailable(ImageReader reader) {
            try (Image image = mImageReader.acquireLatestImage()) {
                stopScreenSharing();
                if (image != null && IMAGES_PRODUCED == 0) {
                    Image.Plane[] planes = image.getPlanes();
                    ByteBuffer buffer = planes[0].getBuffer();
                    int pixelStride = planes[0].getPixelStride();
                    int rowStride = planes[0].getRowStride();
                    int rowPadding = rowStride - pixelStride * mDisplayWidth;

                    bitmap = Bitmap.createBitmap(mDisplayWidth + rowPadding / pixelStride, mDisplayHeight, Bitmap.Config.ARGB_8888);
                    bitmap.copyPixelsFromBuffer(buffer);

                    Log.e("tag", "/myscreen_" + IMAGES_PRODUCED + ".png");
                    Log.e("bitmap", bitmap.toString());
                    mImageView.setImageBitmap(bitmap);
                    endBtn.setVisibility(View.VISIBLE);
                    FirebaseVisionImage visionImage = FirebaseVisionImage.fromBitmap(bitmap);
                    recognizeText(visionImage);

                    IMAGES_PRODUCED++;
                    Log.e(TAG, "captured image: " + IMAGES_PRODUCED);
                    image.close();

                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    @SuppressLint("WrongConstant")
    private void shareScreen() {
        if (mMediaProjection == null) {
            startActivityForResult(mProjectionManager.createScreenCaptureIntent(),
                    PERMISSION_CODE);
            return;
        }
        mImageReader = ImageReader.newInstance(mDisplayWidth, mDisplayHeight, PixelFormat.RGBA_8888, 1);
        mVirtualDisplay = createVirtualDisplay();
        mImageReader.setOnImageAvailableListener(new ImageAvailableListener(), null);
    }
    private void stopScreenSharing() {
        if (mVirtualDisplay != null) {
            mVirtualDisplay.release();
            mVirtualDisplay = null;
        }
    }
    private VirtualDisplay createVirtualDisplay() {
        Log.e("mDisplayWidth", String.valueOf(mDisplayWidth));
        Log.e("mDisplayHeight", String.valueOf(mDisplayHeight));
        return mMediaProjection.createVirtualDisplay("ScreenSharingDemo",
                mDisplayWidth, mDisplayHeight, mScreenDensity,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                mImageReader.getSurface(), null, null /*Handler*/);
    }

    private class MediaProjectionCallback extends MediaProjection.Callback {
        @Override
        public void onStop() {
            mMediaProjection = null;
            stopScreenSharing();
        }
    }

    private void recognizeText(FirebaseVisionImage image) {
        // [START get_detector_default]
        FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
                .getCloudTextRecognizer();
        // Or, to change the default settings:
        //   FirebaseVisionTextRecognizer detector = FirebaseVision.getInstance()
        //          .getCloudTextRecognizer(options);

        // Or, to provide language hints to assist with language detection:
        // See https://cloud.google.com/vision/docs/languages for supported languages
        /*FirebaseVisionCloudTextRecognizerOptions options = new FirebaseVisionCloudTextRecognizerOptions.Builder()
                .setLanguageHints(Arrays.asList("en", "hi"))
                .build();*/
        // [END get_detector_default]

        // [START run_detector]
        Task<FirebaseVisionText> result =
                detector.processImage(image)
                        .addOnSuccessListener(new OnSuccessListener<FirebaseVisionText>() {
                            @Override
                            public void onSuccess(FirebaseVisionText firebaseVisionText) {
                                // Task completed successfully
                                processTextBlock(firebaseVisionText);
                            }
                        })
                        .addOnFailureListener(
                                new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        // Task failed with an exception
                                        // ...
                                    }
                                });
        // [END run_detector]
    }

    private void processTextBlock(FirebaseVisionText result) {
        // [START mlkit_process_text_block]
        String resultText = result.getText();
        ConstraintLayout container = findViewById(R.id.screen);
        for (FirebaseVisionText.TextBlock block: result.getTextBlocks()) {
            String blockText = block.getText();
            Float blockConfidence = block.getConfidence();
            List<RecognizedLanguage> blockLanguages = block.getRecognizedLanguages();
            Point[] blockCornerPoints = block.getCornerPoints();
            Rect blockFrame = block.getBoundingBox();
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.FILL);
            paint.setColor(Color.WHITE);
            paint.setAntiAlias(true);
            canvas.drawRect(blockFrame,paint);
            mImageView.setImageBitmap(bitmap);
            float height = blockFrame.bottom - blockFrame.top;
            if(block.getLines().size() > 1){
                String temp = "";
                for (FirebaseVisionText.Line line: block.getLines()) {
                    String lineText = line.getText();
                    temp += lineText;
                    Rect lineFrame = line.getBoundingBox();
                    height = lineFrame.bottom - lineFrame.top;
                }
                blockText = temp;
            }
            Log.e("elementText", blockText);
            Log.e("elementFrame", String.valueOf(blockFrame));
            int isMeme = -1;
            SharedPreferences sharedPreferences= getSharedPreferences("lang", MODE_PRIVATE);
            String sourceLang = sharedPreferences.getString("sourceLang","ko");
            String targetLang = sharedPreferences.getString("targetLang","en");
            if(sourceLang.equals("ko") && targetLang.equals("en")){
                for(int i = 0; i < arrayIndex.size(); i++){
                    if(blockText.contains(arrayKo.get(i))){
                        String[] tempStrArr = blockText.split(arrayKo.get(i));
                        String tempStr = "";
                        for(String str : tempStrArr){
                            tempStr += str + "@";
                        }
                        blockText = tempStr.substring(0,tempStr.length()-1);
                        isMeme = i;
                        Log.e("meme", String.valueOf(isMeme));
                        Log.e("elementText", blockText);

                        LinearLayout View = (LinearLayout)findViewById(R.id.memeLayout);
                        Button button = new Button(this);
                        button.setText(arrayEn.get(i));
                        int finalIsMeme = isMeme;
                        button.setOnClickListener(new android.view.View.OnClickListener() {
                            public void onClick(View v) {
                                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q="+arrayKo.get(finalIsMeme)+"&tbm=isch"));
                                startActivity(intent);
                            }
                        });
                        View.addView(button);

                    }
                }
            }
            NaverTranslateTask asyncTask = new NaverTranslateTask(blockText, blockFrame.left, blockFrame.top, height, isMeme);
            asyncTask.execute();
            /*for (FirebaseVisionText.Line line: block.getLines()) {
                String lineText = line.getText();
                Float lineConfidence = line.getConfidence();
                List<RecognizedLanguage> lineLanguages = line.getRecognizedLanguages();
                Point[] lineCornerPoints = line.getCornerPoints();
                Rect lineFrame = line.getBoundingBox();
                for (FirebaseVisionText.Element element: line.getElements()) {
                    String elementText = element.getText();
                    Float elementConfidence = element.getConfidence();
                    List<RecognizedLanguage> elementLanguages = element.getRecognizedLanguages();
                    Point[] elementCornerPoints = element.getCornerPoints();
                    Rect elementFrame = element.getBoundingBox();
                }
            }*/
        }
        // [END mlkit_process_text_block]
    }

    public class NaverTranslateTask extends AsyncTask<Void, Void, String> {

        public String resultText;
        //Naver
        String clientId = "";//애플리케이션 클라이언트 아이디값";
        String clientSecret = "";//애플리케이션 클라이언트 시크릿값";
        //언어선택도 나중에 사용자가 선택할 수 있게 옵션 처리해 주면 된다.
        SharedPreferences sharedPreferences= getSharedPreferences("lang", MODE_PRIVATE);
        String sourceLang = sharedPreferences.getString("sourceLang","ko");
        String targetLang = sharedPreferences.getString("targetLang","en");

        String sourceText;
        float left, top, height;
        int isMeme;

        NaverTranslateTask(String sourceText, float left, float top, float height, int isMeme){
            this.sourceText = sourceText;
            this.left = left;
            this.top = top;
            this.height = height;
            this.isMeme = isMeme;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        //AsyncTask 메인처리
        @Override
        protected String doInBackground(Void... params) {
            //네이버제공 예제 복사해 넣자.
            //Log.d("AsyncTask:", "1.Background");

            try {
                //String text = URLEncoder.encode("만나서 반갑습니다.", "UTF-8");
                String text = URLEncoder.encode(sourceText, "UTF-8");
                String apiURL = "https://openapi.naver.com/v1/papago/n2mt";
                URL url = new URL(apiURL);
                HttpURLConnection con = (HttpURLConnection)url.openConnection();
                con.setRequestMethod("POST");
                con.setRequestProperty("X-Naver-Client-Id", clientId);
                con.setRequestProperty("X-Naver-Client-Secret", clientSecret);
                // post request
                String postParams = "source="+sourceLang+"&target="+targetLang+"&text=" + text;
                con.setDoOutput(true);
                DataOutputStream wr = new DataOutputStream(con.getOutputStream());
                wr.writeBytes(postParams);
                wr.flush();
                wr.close();
                int responseCode = con.getResponseCode();
                BufferedReader br;
                if(responseCode==200) { // 정상 호출
                    br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                } else { // 에러 발생
                    br = new BufferedReader(new InputStreamReader(con.getErrorStream()));
                }
                String inputLine;
                StringBuffer response = new StringBuffer();
                while ((inputLine = br.readLine()) != null) {
                    response.append(inputLine);
                }
                br.close();
                //System.out.println(response.toString());
                return response.toString();

            } catch (Exception e) {
                //System.out.println(e);
                Log.d("error", e.getMessage());
                return null;
            }
        }

        //번역된 결과를 받아서 처리
        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            //최종 결과 처리부
            //Log.e("background result", s.toString()); //네이버에 보내주는 응답결과가 JSON 데이터이다.

            //JSON데이터를 자바객체로 변환해야 한다.
            //Gson을 사용할 것이다.

            Gson gson = new GsonBuilder().create();
            JsonParser parser = new JsonParser();
            JsonElement rootObj = parser.parse(s.toString())
                    //원하는 데이터 까지 찾아 들어간다.
                    .getAsJsonObject().get("message")
                    .getAsJsonObject().get("result");
            //안드로이드 객체에 담기
            TranslatedItem items = gson.fromJson(rootObj.toString(), TranslatedItem.class);
            //Log.d("result", items.getTranslatedText());
            //번역결과를 텍스트뷰에 넣는다.
            resultText = items.getTranslatedText();
            if(isMeme != -1){
                Log.e("Test", String.valueOf(resultText.length() - resultText.indexOf("@")));
                if(resultText.length() - resultText.indexOf("@") == 1){
                    if(resultText.equals("@")) resultText = arrayEn.get(isMeme);
                    else{
                        String[] tempStrArr = resultText.split("@");
                        resultText = tempStrArr[0] + arrayEn.get(isMeme);
                    }
                }else{
                    String[] tempStrArr = resultText.split("@");
                    resultText = tempStrArr[0] + arrayEn.get(isMeme) + tempStrArr[1];
                }
            }
            Log.e("trans", resultText);
            Canvas canvas = new Canvas(bitmap);
            Paint paint = new Paint();
            paint.setTextSize(height);
            paint.setColor(Color.BLACK);
            paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
            if(measureTextWidth(paint, resultText) > mDisplayWidth - left){
                String temp = "";
                String[] splitText = resultText.split(" ");
                int i = 0;
                for(String text : splitText){
                    if(measureTextWidth(paint, temp + text + " ") > mDisplayWidth - left){
                        canvas.drawText(temp, left, top+height + i*(height+5), paint);
                        temp = text + " ";
                        i++;
                    }
                    else{
                        temp += text + " ";
                    }
                }
                canvas.drawText(temp, left, top+height + i*(height+5), paint);
            }
            else canvas.drawText(resultText, left, top+height, paint);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mImageView.setImageBitmap(bitmap);
                }
            });
        }

        //자바용 그릇
        private class TranslatedItem {
            String translatedText;

            public String getTranslatedText() {
                return translatedText;
            }
        }
    }

    public static int measureTextWidth(Paint brush, String text) {
        Rect result = new Rect();
        // Measure the text rectangle to get the height
        brush.getTextBounds(text, 0, text.length(), result);
        return result.width();
    }

    public void getFirebaseDatabase(){
        ValueEventListener postListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                arrayKo.clear();
                arrayEn.clear();
                arrayIndex.clear();
                for (DataSnapshot postSnapshot: dataSnapshot.getChildren()) {
                    String key = postSnapshot.getKey();
                    Meme get = postSnapshot.getValue(Meme.class);
                    arrayKo.add(get.ko);
                    arrayEn.add(get.en);
                    arrayIndex.add(key);
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("getFirebaseDatabase","loadPost:onCancelled", databaseError.toException());
            }
        };
        Query query = FirebaseDatabase.getInstance().getReference().child("meme");
        query.addListenerForSingleValueEvent(postListener);
    }
}