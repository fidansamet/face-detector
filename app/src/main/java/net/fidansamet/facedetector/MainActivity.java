package net.fidansamet.facedetector;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.SystemClock;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.hardware.Camera;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private static List<PersonLine> personLineList;
    private static List<PersonLine> personsList = new ArrayList<>();
    TextView textView;
    Camera camera;
    SurfaceView surfaceView, transparentView;
    SurfaceHolder surfaceHolder, holderTransparent;
    boolean camCondition = false;
    Button cap, clear;
    Canvas canvas;
    float RectLeft, RectTop, RectRight, RectBottom;
    PhoneView phoneView = new PhoneView();
    private MediaRecorder mMediaRecorder;

    private boolean mInitSuccesful;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (OpenCVLoader.initDebug()) {
            Toast.makeText(getApplicationContext(), "OpenCV load is successful", Toast.LENGTH_SHORT).show();

        } else {
            Toast.makeText(getApplicationContext(), "OpenCV load is failed", Toast.LENGTH_SHORT).show();
        }

        textView = findViewById(R.id.no_item_id);
        recyclerView = findViewById(R.id.recycler_view);
        transparentView = findViewById(R.id.TransparentView);
        surfaceView = findViewById(R.id.cameraView);
        cap = findViewById(R.id.button_image);
        clear = findViewById(R.id.clear_button);

        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        recyclerViewAdapter = new RecyclerViewAdapter(getBaseContext(), personLineList);
        recyclerView.setAdapter(recyclerViewAdapter);

        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        holderTransparent = transparentView.getHolder();
        holderTransparent.addCallback(this);
        holderTransparent.setFormat(PixelFormat.TRANSPARENT);
        transparentView.setZOrderOnTop(true);


        cap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

//                camera.takePicture(null, null, null);   // jpeg?
//
//                canvas = holderTransparent.lockCanvas();
//                if (canvas != null) {
//                    canvas.drawColor(0, PorterDuff.Mode.MULTIPLY);
//                    RectLeft = 650;
//                    RectTop = 250;
//                    RectRight = RectLeft + 100;
//                    RectBottom = RectTop + 300;
//                    canvas = phoneView.Draw(canvas, RectLeft, RectTop, RectRight, RectBottom, Color.GREEN);
//                    RectLeft = 300;
//                    RectTop = 500;
//                    RectRight = RectLeft +100;
//                    RectBottom = RectTop + 100;
//                    canvas = phoneView.Draw(canvas, RectLeft, RectTop, RectRight, RectBottom, Color.BLUE);
//                    holderTransparent.unlockCanvasAndPost(canvas);
//                }
            }
        });

        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                mMediaRecorder.stop();
                mMediaRecorder.reset();
                try {
                    initRecorder(surfaceHolder.getSurface());
                } catch (IOException e) {
                    e.printStackTrace();
                }
//                canvas = holderTransparent.lockCanvas();
//                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
//                holderTransparent.unlockCanvasAndPost(canvas);
//                camCondition = true;
//                camera.startPreview();
            }
        });
    }

    private void initRecorder(Surface surface) throws IOException {
        // It is very important to unlock the camera before doing setCamera
        // or it will results in a black preview
        if(camera == null) {
            camera = Camera.open();
            camera.unlock();
        }

        if(mMediaRecorder == null) {
            mMediaRecorder = new MediaRecorder();
        }

        mMediaRecorder.setPreviewDisplay(surface);
        mMediaRecorder.setCamera(camera);

//        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
//        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
//        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
//        mMediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getAbsolutePath() + "/test.mp4");
//        mMediaRecorder.setVideoSize(640, 480);
//        mMediaRecorder.setVideoFrameRate(20);

        try {
            mMediaRecorder.prepare();
            mMediaRecorder.start();

        } catch (IllegalStateException e) {
            // This is thrown if the previous calls are not called with the
            // proper order
            e.printStackTrace();
        }

        mInitSuccesful = true;

    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_DENIED) {
            ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA}, 100);

        } else {
            camera = Camera.open();
            camera.setDisplayOrientation(0);

            try {
                if(!mInitSuccesful)
                    initRecorder(surfaceHolder.getSurface());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {


//        try {
//            if(!mInitSuccesful)
//                initRecorder(surfaceHolder.getSurface());
//        } catch (IOException e) {
//            e.printStackTrace();
//        }


        if (camCondition) {
            camera.stopPreview();
            camCondition = false;
        }

        if (camera != null) {
            try {

                camera.setPreviewDisplay(surfaceHolder);
                camera.startPreview();
                camCondition = true;

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mMediaRecorder.reset();
        mMediaRecorder.release();
        camera.release();

        // once the objects have been released they can't be reused
        mMediaRecorder = null;
        camera = null;
    }


    public void updateList(){

        Collections.reverse(personLineList);
        personsList.clear();
        personsList.addAll(personLineList);
        Collections.reverse(personLineList);

        if (personsList.size() == 0){

            textView.setVisibility(View.VISIBLE);

        } else {

            textView.setVisibility(View.GONE);
            recyclerViewAdapter.setPeopleList(personsList);
            recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
            recyclerView.setAdapter(recyclerViewAdapter);
        }
    }
}
