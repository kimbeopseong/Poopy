package com.example.poopy.ui.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.SessionConfiguration;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import org.jetbrains.annotations.Nullable;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Semaphore;


public class CameraPreview extends Thread {

    private final String TAG = "CameraPreview";
    private Context mContext;
    private Size previewSize;
    private CameraDevice mCameraDevice;
    private CaptureRequest.Builder mPreviewBuilder;
    private CameraCaptureSession mPreviewSession;
    private TextureView mPreview;
    private Button capture;
    private StreamConfigurationMap map;

    private int deviceRotation;
    private String date;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

    static{
        System.loadLibrary("opencv_java4");
        System.loadLibrary("imageprocessing");
    }

    private Mat image_input, image_output;

    /** Preview 객체 설정   */
    public CameraPreview(Context context, TextureView textureView, Button button){

        mContext = context;
        mPreview = textureView;
        capture = button;
        capture.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.R)
            @Override
            public void onClick(View view) {
                takePicture();
            }
        });
                                                                                                    // date = 파일 이름 될 예정. 현재 시각.
        long now = System.currentTimeMillis();
        Date mDate = new Date(now);
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd_hh:mm:ss");
        date = simpleDateFormat.format(mDate);

    }
                                                                                                    // cameraID 가져오기
    private String getBackFacingCameraId(CameraManager cameraManager){
        try{
            for (final String cameraId : cameraManager.getCameraIdList()){
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                int cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) return cameraId;
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        return null;
    }

    private CameraDevice.StateCallback mStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice cameraDevice) {
            Log.e(TAG, "onOpened");
            mCameraDevice = cameraDevice;
            startPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice cameraDevice) {
            Log.e(TAG, "onDisconnected");
        }

        @Override
        public void onError(@NonNull CameraDevice cameraDevice, int i) {

        }
    };
                                                                                                    // OpenCamera
    public void openCamera(){
        CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
        Log.e(TAG, "openCamera !!!");

        try {
            String cameraId = getBackFacingCameraId(manager);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(cameraId);
            map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            previewSize = map.getOutputSizes(SurfaceTexture.class)[0];

            int permissionCamera =
                    ContextCompat.checkSelfPermission(mContext,
                            Manifest.permission.CAMERA);
            int permissionStorage =
                    ContextCompat.checkSelfPermission(mContext,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE);

            if (permissionCamera == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions((Activity) mContext,
                        new String[]{Manifest.permission.CAMERA}, CameraActivity.REQUEST_CAM);
            } else if (permissionStorage == PackageManager.PERMISSION_DENIED){
                ActivityCompat.requestPermissions((Activity) mContext,
                        new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        CameraActivity.REQUEST_STORAGE);
            } else {
                manager.openCamera(cameraId, mStateCallback, null);
            }
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        Log.e(TAG, "openCamera: End");
    }

    private TextureView.SurfaceTextureListener surfaceTextureListener =
            new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e(TAG, "onSurfaceTextureAvailable: width = "+ i + ", height = " + i1);
            openCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(@NonNull SurfaceTexture surfaceTexture, int i, int i1) {
            Log.e(TAG, "onSurfaceTextureSizeChanged");
        }

        @Override
        public boolean onSurfaceTextureDestroyed(@NonNull SurfaceTexture surfaceTexture) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(@NonNull SurfaceTexture surfaceTexture) {

        }
    };
                                                                                                    // Preview 시작
    protected void startPreview(){
        if (null == mCameraDevice || !mPreview.isAvailable() || null == previewSize){
            Log.e(TAG, "startPreview: fail. return");
            return;
        }

        SurfaceTexture texture = mPreview.getSurfaceTexture();
        if (null == texture){
            Log.e(TAG, "Texture is null. return.");
            return;
        }

        texture.setDefaultBufferSize(previewSize.getWidth(), previewSize.getHeight());
        Surface surface = new Surface(texture);

        try{
            mPreviewBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
        } catch (CameraAccessException e){
            e.printStackTrace();
        }
        mPreviewBuilder.addTarget(surface);

        try{
            mCameraDevice.createCaptureSession(Arrays.asList(surface),
                    new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    mPreviewSession = cameraCaptureSession;
                    updatePreview();
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {
                    Toast.makeText(mContext, "onConfigureFailed", Toast.LENGTH_LONG).show();
                }
            }, null);
        } catch (CameraAccessException e){
            e.getMessage();
        }
    }

    protected void updatePreview(){
        if (null == mCameraDevice){
            Log.e(TAG, "updatePreview error. return");
        }

        mPreviewBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
        HandlerThread thread = new HandlerThread("CameraPreview");
        thread.start();
        Handler backgroundHandler = new Handler(thread.getLooper());

        try{
            mPreviewSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e){
            e.getMessage();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    public void takePicture() {
        if (null == mCameraDevice){
            Log.e(TAG, "CameraDevice is null. return");
            return;
        }
        Log.e(TAG, "촬영 함수에 진입하였습니다 !!!");

        try{
            int width = 640;
            int height = 480;

            final ImageReader reader = ImageReader.newInstance(width, height, ImageFormat.JPEG, 1);
            List<Surface> outputSurfaces = new ArrayList<>(2);
            outputSurfaces.add(reader.getSurface());
            outputSurfaces.add(new Surface(mPreview.getSurfaceTexture()));
            Log.e(TAG, "이미지리더 객체 생성 !!!");

            final CaptureRequest.Builder captureBuilder =
                    mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            captureBuilder.addTarget(reader.getSurface());
            captureBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
            Log.e(TAG, "캡쳐빌더 객체 생성 !!!");
                                                                                                        // Capture Image 회전 설정
            CameraManager manager = (CameraManager) mContext.getSystemService(Context.CAMERA_SERVICE);
            CameraCharacteristics characteristics = manager.getCameraCharacteristics(mCameraDevice.getId());
            deviceRotation = mContext.getDisplay().getRotation();
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int surfaceRotation = ORIENTATIONS.get(deviceRotation);
            int jpegOrientation = (surfaceRotation + sensorOrientation + 270) % 360;
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);


            File path = mkFilePath(mContext);
            Log.e(TAG, "파일 경로 지정 완료 !!!");

            String pic_name = String.format("%s.jpg", date);
            final File file = new File(path, pic_name);
            final Uri uri = Uri.fromFile(file);

            ImageReader.OnImageAvailableListener readerListener = new ImageReader.OnImageAvailableListener() {
                @Override
                public void onImageAvailable(ImageReader imageReader) {
                    Image image = null;
                    Log.e(TAG, "이미지리더 리스너 객체 함수 진입 !!!");
                    try{
                        image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.capacity()];
                        buffer.get(bytes);

                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
                        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
                        Bitmap resizingImage = null;

                        bitmap.createBitmap(bitmap, 0, 0, 640, 480);
                        resizingImage = Bitmap.createScaledBitmap(bitmap, 255, 255, true);

                        //OpenCV imageprocessing(bitmapToMat => matToBitmap)함수 이용한 전경검출 2020.07.13 BJH
                        Bitmap tmp = resizingImage.copy(Bitmap.Config.ARGB_8888, true);
                        image_input = new Mat();
                        Utils.bitmapToMat(tmp, image_input);
                        //poop photo's foreground
                        Bitmap foreground = imageprocess_and_save();
                        if(foreground != null)
                            Log.d(TAG, "foreground is set");

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        byte[] pic = stream.toByteArray();
                        bytes = pic;
                        save(bytes);
                    } catch (FileNotFoundException e){
                        e.printStackTrace();
                    } catch (IOException e){
                        e.printStackTrace();
                    } finally {
                        if (image != null){
                            image.close();
                            reader.close();
                        }
                    }
                }

                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    Log.e(TAG, "세이브 함수 진입 !!!");
                    try{
                        output = new FileOutputStream(file);
                        output.write(bytes);

                        /**
                         *
                         * 여기에 Firebase 관련 저장 기능을 가진 내용을 작성하면 될 듯.
                         *
                         * */

                    } catch(Exception e){
                        e.getMessage();
                        Log.e(TAG, "세이브 시도 중 문제가 발생하였습니다 !!!");
                    } finally {
                        if (null != output){
                            try{
                                output.close();
                            } catch (IOException e){
                                e.printStackTrace();
                            }
                        }
                    }
                }

            };

            HandlerThread thread = new HandlerThread("CameraCapture");
            thread.start();
            Log.e(TAG, "핸들러가 시작 되었습니다 !!!");
            final Handler backgroundHandler = new Handler(thread.getLooper());
            reader.setOnImageAvailableListener(readerListener, backgroundHandler);

            final CameraCaptureSession.CaptureCallback captureListener = new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    Toast.makeText(mContext, "Saved on "+file, Toast.LENGTH_SHORT).show();
                    startPreview();
                }
            };

            mCameraDevice.createCaptureSession(outputSurfaces, new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
                    try{
                        cameraCaptureSession.capture(captureBuilder.build(), captureListener, backgroundHandler);
                    } catch (CameraAccessException e){
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession cameraCaptureSession) {

                }
            }, backgroundHandler);

        } catch (CameraAccessException e){
            e.printStackTrace();
        }
    }

    private File mkFilePath(Context context){
        File filePath = null;
                                                                                                    // Android SDK 버전이 29보다 낮은 경우
        if (Build.VERSION.SDK_INT < 29){
            filePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/poopy");
            Log.e(TAG, "android SDK_INT < 29 !!!");
            if (!filePath.exists())
                try{
                    filePath.mkdirs();
                } catch (Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "Failed to create file path !!!");
                }
        }
                                                                                                    // Android SDK 버전이 29보다 높거나 같은 경우
        else {
            filePath = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "poopy");
            Log.e(TAG, "android SDK_INT >= 29 !!!");
            if (!filePath.mkdirs())
                Log.e(TAG, "Failed to create file path !!!");
        }
        return filePath;
    }

    public void setSurfaceTextureListener(){
        mPreview.setSurfaceTextureListener(surfaceTextureListener);
    }

    public void onResume(){
        Log.d(TAG, "onResume");
        setSurfaceTextureListener();
    }

    private Semaphore mCameraOpenCloseLock = new Semaphore(1);

    public void onPause(){
        Log.d(TAG, "onPause");

        try{
            mCameraOpenCloseLock.acquire();
            if (null != mCameraDevice){
                mCameraDevice.close();
                mCameraDevice = null;
                Log.d(TAG, "CameraDevice Closed !!!");
            }
        } catch (InterruptedException e){
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            mCameraOpenCloseLock.release();
        }
    }

    //native함수 선언 2020.07.13 BJH
    public native void imageprocessing(long input_image, long ouput_image);

    //imageprocessing JNI function 호출 2020.07.13 BJH
    public Bitmap imageprocess_and_save() {
        if (image_output == null)
            image_output = new Mat();
        imageprocessing(image_input.getNativeObjAddr(), image_output.getNativeObjAddr());
        Bitmap bitmapOutput = Bitmap.createBitmap(image_output.cols(), image_output.rows(), Bitmap.Config.ARGB_8888);
        //image_output to Bitmap
        Utils.matToBitmap(image_output, bitmapOutput);
        return bitmapOutput;
    }

}
