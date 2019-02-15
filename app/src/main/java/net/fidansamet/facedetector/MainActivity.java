package net.fidansamet.facedetector;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.PorterDuff;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.hardware.Camera;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MainActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private RecyclerView recyclerView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private static List<PersonLine> personLineList;
    private static List<PersonLine> personsList = new ArrayList<>();
    TextView textView, searching;
    int dot_counter = 0;
    String update_searching;
    Camera camera;
    SurfaceView surfaceView, transparentView;
    SurfaceHolder surfaceHolder, holderTransparent;
    boolean camCondition = false;
    Button cap, clear;
    Canvas canvas;
    private Paint mPaint = new Paint();
    private Paint mPaintText;
    float RectLeft, RectTop, RectRight, RectBottom;
    public static Bitmap mBitmap;
    int  deviceHeight,deviceWidth;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.no_item_id);
        searching = findViewById(R.id.searching_view);
        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getBaseContext()));
        recyclerViewAdapter = new RecyclerViewAdapter(getBaseContext(), personLineList);
        recyclerView.setAdapter(recyclerViewAdapter);
        update_searching =  getString(R.string.searching);
        getWindow().setFormat(PixelFormat.UNKNOWN);
        surfaceView = findViewById(R.id.cameraView);
        surfaceHolder = surfaceView.getHolder();
        surfaceHolder.addCallback(this);
        surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_NORMAL);
        cap = findViewById(R.id.button_image);
        transparentView = findViewById(R.id.TransparentView);
        holderTransparent = transparentView.getHolder();
        holderTransparent.addCallback(this);
        holderTransparent.setFormat(PixelFormat.TRANSPARENT);
        transparentView.setZOrderOnTop(true);
        clear = findViewById(R.id.clear_button);
        deviceWidth=getScreenWidth();
        deviceHeight=getScreenHeight();


        final Camera.PictureCallback mPictureCallback = new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
//                FileOutputStream outputStream = null;
//
//                try {
//                    File directory = getDir("Camera", Context.MODE_PRIVATE);
//                    // Create imageDir
//                    File mypath=new File(directory,System.currentTimeMillis() + ".jpg");
//                    outputStream = new FileOutputStream(mypath);
//                    System.out.println(mypath);
//                    outputStream.write(data);
//                    outputStream.close();
//                } catch (FileNotFoundException e) {
//                    e.printStackTrace();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } finally {
//
//                }
            }
        };

        cap.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                camera.takePicture(null, null, mPictureCallback);   // jpeg?
                canvas = holderTransparent.lockCanvas();
                if (canvas != null) {
                    canvas.drawColor(0, PorterDuff.Mode.MULTIPLY);
                    RectLeft = 650;
                    RectTop = 250;
                    RectRight = RectLeft + deviceWidth - 730;
                    RectBottom = RectTop + 300;
                    Draw(RectLeft, RectTop, RectRight, RectBottom, Color.GREEN);
                    RectLeft = 300;
                    RectTop = 500;
                    RectRight = RectLeft + deviceWidth - 900;
                    RectBottom = RectTop + 100;
                    Draw(RectLeft, RectTop, RectRight, RectBottom, Color.BLUE);
                    holderTransparent.unlockCanvasAndPost(canvas);
                }
            }
        });


        clear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                canvas = holderTransparent.lockCanvas();
                canvas.drawColor(0, PorterDuff.Mode.CLEAR);
                holderTransparent.unlockCanvasAndPost(canvas);
                camCondition = true;
                camera.startPreview();

            }
        });


        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    while (!isInterrupted()) {
                        Thread.sleep(1000);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (dot_counter%4 == 0) {
                                    update_searching = getString(R.string.searching);
                                    dot_counter++;
                                } else {
                                    update_searching = update_searching + ".";
                                    dot_counter++;
                                }
                                searching.setText(update_searching);
                            }
                        });
                    }
                } catch (InterruptedException e) {
                }
            }
        };
        thread.start();
    }

    public static int getScreenWidth() {
        return Resources.getSystem().getDisplayMetrics().widthPixels;
    }


    public static int getScreenHeight() {
        return Resources.getSystem().getDisplayMetrics().heightPixels;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        camera = Camera.open(); //open a camera
        camera.setDisplayOrientation(90);

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

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

        camera.stopPreview();
    }

    public void Draw(float RectLeft, float RectTop, float RectRight, float RectBottom, int color) {


        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setColor(color);
        paint.setStrokeWidth(3);
        canvas.drawRect(RectLeft, RectTop, RectRight, RectBottom, paint);


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
