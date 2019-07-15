package net.fidansamet.facedetector;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.FileObserver;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;

import com.google.android.glass.content.Intents;
import com.google.android.glass.media.Sounds;
import com.google.android.glass.touchpad.Gesture;
import com.google.android.glass.touchpad.GestureDetector;
import com.google.android.glass.widget.CardBuilder;

import android.hardware.Camera.PictureCallback;

import org.apache.http.HttpResponse;
import org.apache.http.HttpVersion;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MIME;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.DefaultHttpClient;

import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.view.SurfaceView;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainActivity extends Activity  {

    private static final int TAKE_PICTURE_REQUEST = 1;
    private static final int TAKE_VIDEO_REQUEST = 2;
    private GestureDetector mGestureDetector = null;
    private CameraView cameraView = null;

    private static final String TAG = "camera";
    private final MediaType MEDIA_TYPE_JPEG = MediaType.parse("image/jpeg");
    private final String SERVER_URL = "SERVER_URL";
    private OkHttpClient client = new OkHttpClient();

    private int counterr = 0;
    private final int sample_rate = 5;
    ArrayList<String> person_names = new ArrayList<String>();
    private boolean detectionRunning = false;       // start detection
    private CardBuilder card = null;
    ViewGroup.LayoutParams layoutParamsControl = new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT,
            LayoutParams.MATCH_PARENT);
    private final int delayMili = 300;
    AudioManager mAudioManager = null;
    TextView tv = null;
    private enum roles {Security, Hostess1, Hostess2, Doctor, Manager};
    private roles my_role = roles.Hostess1;     // current role
    private String person_info = "";

    //Camera Preview Callback Sample images using this method.
    private final Camera.PreviewCallback mmmm = new Camera.PreviewCallback() {

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.d(TAG, "Frame taken!");
            if (counterr % sample_rate == 0){       // Sample images with given period.
                counterr = 0;
                handleImage(data, camera);
            }
            counterr ++;
        }
    };

    public void handleImage(byte[] data, Camera camera){
        File pictureFile = getOutputMediaFile();
        if (pictureFile == null) {
            Log.d(TAG, "Error creating media file, check storage permissions");
            return;
        }

        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = parameters.getPreviewSize();
        YuvImage image = new YuvImage(data, ImageFormat.NV21,
                size.width, size.height, null);
        Rect rectangle = new Rect();
        rectangle.bottom = size.height;
        rectangle.top = 0;
        rectangle.left = 0;
        rectangle.right = size.width;
        ByteArrayOutputStream out2 = new ByteArrayOutputStream();
        image.compressToJpeg(rectangle, 80, out2);

        try {       // save image
            FileOutputStream fos = new FileOutputStream(pictureFile);
            fos.write(out2.toByteArray());
            fos.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {       // send image to server
            uploadImage(pictureFile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Callback for picture capturing! Not in use
    private final Camera.PictureCallback mPictureCallback = new PictureCallback() {

        @Override
        public void onPictureTaken(byte[] data, Camera camera) {

            Log.d(TAG, "Picture taken!");
            File pictureFile = getOutputMediaFile();

            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
                return;
            }

            try {       // save image
                FileOutputStream fos = new FileOutputStream(pictureFile);
                fos.write(data);
                fos.close();
                Toast t = (Toast) Toast.makeText(getApplicationContext(), "Saved JPEG!", Toast.LENGTH_SHORT);
                t.show();

            } catch (FileNotFoundException e) {
                Log.d(TAG, "File not found: " + e.getMessage());
            } catch (IOException e) {
                Log.d(TAG, "Error accessing file: " + e.getMessage());
            }

            try {
                uploadImage(pictureFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    // Face detector works but no need to use it.
    private final Camera.FaceDetectionListener mFDListener = new Camera.FaceDetectionListener() {

        @Override
        public void onFaceDetection(Camera.Face[] faces, Camera camera) {

            Log.d(TAG, "Picture taken!");
            File pictureFile = getOutputMediaFile();

            if (pictureFile == null) {
                Log.d(TAG, "Error creating media file, check storage permissions");
            }
        }
    };

    // Create a File for saving an image
    private static File getOutputMediaFile(){
        // To be safe, you should check that the SDCard is mounted using Environment.getExternalStorageState() before doing this.
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "MyCameraApp");
        // This location works best if you want the created images to be shared
        // between applications and persist after your app has been uninstalled.
        // Create the storage directory if it does not exist
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                Log.d(TAG, "failed to create directory");
                return null;
            }
        }

        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(new Date());
        File mediaFile;
        String path = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM) + "/MyCameraApp/IMG_"+ timeStamp + ".jpg";
        mediaFile = new File(path);
        Log.d(TAG, "PATH: " + path);

        return mediaFile;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tv = (TextView)findViewById(R.id.footer);
        int SDK_INT = android.os.Build.VERSION.SDK_INT;
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder()
                .permitAll().build();
        StrictMode.setThreadPolicy(policy);
        SurfaceView maLayoutPreview = (SurfaceView) findViewById(R.id.surfaceview);
        cameraView = new CameraView(this, maLayoutPreview);     // Initiate CameraView
        mGestureDetector = createGestureDetector(this);     // Turn on Gestures
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);         // Audio Manager
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Do not hold the camera during onPause
        if (cameraView != null) {
            cameraView.getCamera().setPreviewCallback(null);
            cameraView.releaseCamera();
        }
    }

    // Gesture detection for fingers on the Glass
    private GestureDetector createGestureDetector(Context context) {

        final GestureDetector gestureDetector = new GestureDetector((GestureDetector.OnGestureListener) context);
        gestureDetector.setBaseListener(new GestureDetector.BaseListener() {        //Create a base listener for generic gestures
            @Override
            public boolean onGesture(Gesture gesture) {
                if (cameraView != null) {       // Make sure view is initiated

                    if (gesture == Gesture.TAP) {       // Tap with a single finger for photo

                        if (!detectionRunning) {
                            mAudioManager.playSoundEffect(Sounds.SUCCESS);
                            cameraView.getCamera().setPreviewCallback(mmmm);
                            detectionRunning = true;

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    //Displaying Toast with Hello Javatpoint message
                                    Toast warning = Toast.makeText(getApplicationContext(), "Face detection started!", Toast.LENGTH_SHORT);
                                    warning.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
                                    warning.show();

                                    Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            warning.cancel();
                                        }
                                    }, delayMili);

                                }
                            });
                        }
                        else{
                            mAudioManager.playSoundEffect(Sounds.DISMISSED);
                            cameraView.getCamera().setPreviewCallback(null);
                            detectionRunning = false;
                            Log.d(TAG, "Frame stopped!");

                            runOnUiThread(new Runnable() {
                                public void run() {
                                    //Displaying Toast with Hello Javatpoint message
                                    Toast warning = Toast.makeText(getApplicationContext(), "Face detection paused!", Toast.LENGTH_SHORT);
                                    warning.setGravity(Gravity.BOTTOM|Gravity.CENTER_HORIZONTAL, 0, 0);
                                    warning.show();

                                    Handler handler = new Handler();
                                    handler.postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            warning.cancel();
                                        }
                                    }, delayMili);

                                    TextView tv= (TextView)findViewById(R.id.footer);
                                    tv.setText("");
                                }
                            });
                        }
                        return true;
                    } else if (gesture == Gesture.TWO_TAP) {
                        // Tap with 2 fingers for video
                        startActivityForResult(new Intent(MediaStore.ACTION_VIDEO_CAPTURE),
                                TAKE_VIDEO_REQUEST);
                        return true;
                    }
                }
                return false;
            }
        });

        return gestureDetector;
    }

    @Override
    public boolean onGenericMotionEvent(MotionEvent event) {
        // Send generic motion events to the gesture detector
        return mGestureDetector != null && mGestureDetector.onMotionEvent(event);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // Handle photos
        if (requestCode == TAKE_PICTURE_REQUEST && resultCode == RESULT_OK) {
            String picturePath = data.getStringExtra(Intents.EXTRA_PICTURE_FILE_PATH);
            processPictureWhenReady(picturePath);
        }

        // Handle videos
        if (requestCode == TAKE_VIDEO_REQUEST && resultCode == RESULT_OK) {
            String picturePath = data.getStringExtra(Intents.EXTRA_VIDEO_FILE_PATH);
            processPictureWhenReady(picturePath);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

     // Process picture - from example GDK
    private void processPictureWhenReady(final String picturePath) {

        final File pictureFile = new File(picturePath);
        Log.d(TAG, "Image path: " + picturePath );

        if (pictureFile == null){
            Log.d(TAG, "Error creating media file, check storage permissions");
            return;
        }

        if (pictureFile.exists()) {
            // The picture is ready; process it.
            Log.d(TAG, "Here???");

        } else {
            // The file does not exist yet. Before starting the file observer, you can update your UI to let the user know that the application is
            // waiting for the picture (for example, by displaying the thumbnail image and a progress indicator).
            final File parentDirectory = pictureFile.getParentFile();
            final FileObserver observer = new FileObserver(parentDirectory.getPath()) {
                // Protect against additional pending events after CLOSE_WRITE is handled.
                private boolean isFileWritten;

                @Override
                public void onEvent(int event, String path) {
                    if (!isFileWritten) {
                        // For safety, make sure that the file that was created in
                        // the directory is actually the one that we're expecting.
                        final File affectedFile = new File(parentDirectory, path);
                        isFileWritten = (event == FileObserver.CLOSE_WRITE
                                && affectedFile.equals(pictureFile));

                        if (isFileWritten) {
                            stopWatching();
                            // Now that the file is ready, recursively call
                            // processPictureWhenReady again (on the UI thread).
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    processPictureWhenReady(picturePath);
                                }
                            });
                        }
                    }
                }
            };
            observer.startWatching();
        }
    }

    // Create request using image file
    public Request createRequest(File file){

            RequestBody req = new MultipartBody.Builder().setType(MultipartBody.FORM).addFormDataPart
                    ("imfile", file.getName(), RequestBody.create(MEDIA_TYPE_JPEG, file)).build();

            return new Request.Builder()
                    .url(SERVER_URL)
                    .post(req)
                    .build();
    }

    //  handle server response
    public void handleReturn(JSONObject jj){

        String person_name = "";
        String person_department = "";
        String person_clock = "";
        String person_doctor = "";
        String person_lastinfo = "";
        String person_sgk = "";
        String person_rand1 = "";
        String person_rand2 = "";
        int person_is_vip = 0;

        // Do something with the response
        if (jj != null) {

            for (Iterator key = jj.keys(); key.hasNext(); ) {
                try {
                    JSONObject name = (JSONObject) jj.get((String) key.next());
                    JSONArray bbs = name.getJSONArray("BB");
                    person_name = name.getString("name");
                    person_department = name.getString("department");
                    person_clock= name.getString("time");
                    person_doctor = name.getString("doctor");
                    person_lastinfo = name.getString("last_info");
                    person_rand1 = name.getString("rand1");
                    person_rand2 = name.getString("rand2");
                    person_sgk = name.getString("sgk");
                    person_is_vip = name.getInt("vip");

                } catch (JSONException e1) {
                    e1.printStackTrace();
                }

                switch(my_role) {
                    case Security:
                        person_info = person_name;
                        break;
                    case Hostess1:
                        person_info = person_name + ", " + person_department + ", " + person_clock + ", " + person_doctor + ", " + person_sgk + "\n"
                                + person_rand1;
                        break;
                    case Hostess2:
                        person_info = person_name  + "\n" + person_rand2;
                        break;
                    case Doctor:
                        person_info = person_name + ", " + person_sgk + ", Lastly: " + person_lastinfo;
                        break;
                    case Manager:
                        person_info = person_name + " " + person_department + " " + person_rand1 + " " + person_rand2;
                        break;
                }

                Log.d("API", person_info);

                runOnUiThread(new Runnable() {
                    public void run() {
                        tv.setText(person_info);
                    }
                });
            }
        }
    }

    // Sync Image Request
    public void uploadImage_Sync(File pictureFile) throws IOException {

        try{
            Request request = createRequest(pictureFile);
            Response response = client.newCall(request).execute();
            Log.d("response", "uploadImage:"+response.body().string());
            JSONObject aa = new JSONObject(response.body().string());
            handleReturn(aa);

        } catch (UnknownHostException  e) {
            Log.e(TAG, "Error: " + e.getLocalizedMessage());
        } catch (Exception e) {
            Log.e(TAG, "Other Error: " + e.getLocalizedMessage());
        }
    }

    // Async image request!
    void uploadImage(File pictureFile) throws IOException{

        Request request = createRequest(pictureFile);
        client.newCall(request)
                .enqueue(new Callback() {
                    @Override
                    public void onFailure(final Call call, IOException e) {
                        // Error
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Log.d("ERROR!!!", "uploadImage");
                                // For the example, you can show an error dialog or a toast on the main UI thread
                            }
                        });
                    }

                    @Override
                    public void onResponse(Call call, final Response response) throws IOException {

                        String res = null;
                        if (response.body() != null) {
                            res = response.body().string();
                        }

                        try {
                            JSONObject aa = new JSONObject(res);
                            // use aa in run thread
                            handleReturn(aa);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
    }

    // Image Req using Default http client!
    public void sendPostReq(File pictureFile, byte[] data){

        // Send Image to Server
        try {

            String result = "";
            HttpClient client = new DefaultHttpClient();
            client.getParams().setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);

            HttpPost post = new HttpPost("SERVER_URL");
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            builder.setCharset(MIME.UTF8_CHARSET);

            builder.addBinaryBody("Filedata", data, ContentType.MULTIPART_FORM_DATA, pictureFile.getName());

            post.setEntity(builder.build());

            JSONObject json = new JSONObject();

            new StringEntity(json.toString());

            org.apache.http.HttpResponse response = client.execute(post);
            int status = response.getStatusLine().getStatusCode();

            Log.d("status", "" + status);
            if (status == 200) {
                Log.d("status", "yes");
            }
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // No need for that method
    private void scanMedia(String path) {
        File file = new File(path);
        Uri uri = Uri.fromFile(file);
        Intent scanFileIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri);
        sendBroadcast(scanFileIntent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // Stop the preview and release the camera.
        // Execute your logic as quickly as possible so the capture happens quickly.
        return keyCode != KeyEvent.KEYCODE_CAMERA && super.onKeyDown(keyCode, event);
    }
}
