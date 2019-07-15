package net.fidansamet.facedetector;

import android.content.Context;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class CameraView extends SurfaceView implements SurfaceHolder.Callback {
    private Camera camera = null;

    public CameraView(Context context, SurfaceView maLayoutPreview) {
        super(context);
        final SurfaceHolder surfaceHolder = maLayoutPreview.getHolder();
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        camera = Camera.open();

        // Set the Hotfix for Google Glass
        setCameraParameters(camera);

        // Show the Camera display
        try {
            camera.setPreviewDisplay(holder);
        } catch (Exception e) {
            releaseCamera();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // Start the preview for surfaceChanged
        if (camera != null) {
            camera.startPreview();
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // Do not hold the camera during surfaceDestroyed - view should be gone
        releaseCamera();
    }

     // Important HotFix for Google Glass (post-XE11) update
    public void setCameraParameters(Camera camera) {
        if (camera != null) {
            final Parameters parameters = camera.getParameters();
            parameters.setPreviewFpsRange(30000, 30000);
            parameters.setPreviewSize(1280, 720); // 1920, 1080
            camera.setParameters(parameters);
        }
    }

     // Release the camera from use
    public void releaseCamera() {
        if (camera != null) {
            // Call stopPreview() to stop updating the preview surface.
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public Camera getCamera() {
        return camera;
    }
}
