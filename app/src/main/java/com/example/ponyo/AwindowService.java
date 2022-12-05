package com.example.ponyo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;

public class AwindowService extends Service {

    private final int START_DRAG = 0;
    private final int END_DRAG = 1;
    private int isMoving;
    private float offset_x, offset_y;
    private boolean start_yn = true;

    private WindowManager wm;
    private View mView;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();

        // Android O 이상일 경우 Foreground 서비스를 실행
        // Notification channel 설정.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //final String strId = getString(R.string.noti_channel_id);
            final String strId = "id";
            final String strTitle = getString(R.string.app_name);
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            NotificationChannel channel = notificationManager.getNotificationChannel(strId);
            if (channel == null) {
                channel = new NotificationChannel(strId, strTitle, NotificationManager.IMPORTANCE_HIGH);
                notificationManager.createNotificationChannel(channel);
            }

            Notification notification = new NotificationCompat.Builder(this, strId).build();
            startForeground(1, notification);
        }

        LayoutInflater inflate = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        // inflater 를 사용하여 layout 을 가져오자
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        // 윈도우매니저 설정

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                // Android O 이상인 경우 TYPE_APPLICATION_OVERLAY 로 설정
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        |WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
                PixelFormat.TRANSLUCENT);


        params.gravity = Gravity.LEFT|Gravity.CENTER_VERTICAL;
        // 위치 지정

        mView = inflate.inflate(R.layout.view_in_service, null);
        // view_in_service.xml layout 불러오기
        // mView.setOnTouchListener(onTouchListener);
        // Android O 이상의 버전에서는 터치리스너가 동작하지 않는다. ( TYPE_APPLICATION_OVERLAY 터치 미지원)


        final ImageButton btn_img =  (ImageButton) mView.findViewById(R.id.btn_img);
        btn_img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("test","onClick ");
                clickFunction();
            }
        });


        // btn_img 에 android:filterTouchesWhenObscured="true" 속성 추가하면 터치리스너가 동작한다.
        btn_img.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if (motionEvent.getAction() == MotionEvent.ACTION_DOWN) {
                    if (start_yn) {
                        offset_x = motionEvent.getRawX();
                        offset_y = motionEvent.getRawY();

                        start_yn = false;
                    }
                    //Toast.makeText(MainActivity.this, "Drag Start", Toast.LENGTH_SHORT).show();
                    isMoving = START_DRAG;
                } else if (motionEvent.getAction() == MotionEvent.ACTION_UP) {
                    //Toast.makeText(MainActivity.this, "Drag End", Toast.LENGTH_SHORT).show();
                    isMoving = END_DRAG;
                }else if (motionEvent.getAction() == MotionEvent.ACTION_MOVE) {
                    if (isMoving == START_DRAG) {
                        params.x = (int) (motionEvent.getRawX() - offset_x);
                        params.y = (int) (motionEvent.getRawY() - offset_y);
                        wm.updateViewLayout(mView, params);
                    }
                }
                return false;
            }
        });

        wm.addView(mView, params); // 윈도우에 layout 을 추가 한다.
    }

    void clickFunction(){
        Intent dialogIntent = new Intent(this, ScreenActivity.class);
        dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(dialogIntent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            stopForeground(true); // Foreground service 종료
        }


        if(wm != null) {
            if(mView != null) {
                wm.removeView(mView); // View 초기화
                mView = null;
            }
            wm = null;
        }
    }
}