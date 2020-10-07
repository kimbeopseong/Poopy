package com.example.poopy.ui.camera;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.ExifInterface;
import android.media.Image;
import android.media.ImageReader;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;

import com.example.poopy.utils.ResultActivity;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

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
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Semaphore;

import xyz.hasnat.sweettoast.SweetToast;


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
    private String level="1";
    private String status="None";

    private StorageReference mStorageRef;
    private String currentUID, currentPID;
    private FirebaseAuth mAuth;

    private FirebaseFirestore db;
    private Intent intent;

    private String poopy_uri, date, stat, lv, currentName;
    private Mat image_input, image_output;

    private  String mCurrentPhotoPath;

    private static final SparseIntArray ORIENTATIONS = new SparseIntArray(4);

    static{
        System.loadLibrary("opencv_java4");
        System.loadLibrary("imageprocessing");
    }

    static {
        ORIENTATIONS.append(Surface.ROTATION_0, 90);
        ORIENTATIONS.append(Surface.ROTATION_90, 0);
        ORIENTATIONS.append(Surface.ROTATION_180, 270);
        ORIENTATIONS.append(Surface.ROTATION_270, 180);
    }

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
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
        date = simpleDateFormat.format(mDate);

        db = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUID = Objects.requireNonNull(mAuth.getCurrentUser()).getUid();

        intent = CameraActivity.intent;
        currentPID = intent.getStringExtra("pid");
        currentName = intent.getStringExtra("Name");

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
            mCameraDevice.createCaptureSession(Collections.singletonList(surface),
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
            deviceRotation = Objects.requireNonNull(mContext.getDisplay()).getRotation();
            int sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            int surfaceRotation = ORIENTATIONS.get(deviceRotation);
            int jpegOrientation = (surfaceRotation + sensorOrientation + 270) % 360;
            captureBuilder.set(CaptureRequest.JPEG_ORIENTATION, jpegOrientation);


            File path = mkFilePath(mContext);
            Log.e(TAG, "파일 경로 지정 완료 !!!");

            String pic_name = String.format("%s.jpg", date);
            final File file = new File(path, pic_name);
            mCurrentPhotoPath = file.getAbsolutePath();

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

                        //OpenCV imageprocessing(비트맵에서 Mat => Mat에서 다시 비트맵으로 변환)
                        Bitmap tmp = resizingImage.copy(Bitmap.Config.ARGB_8888, true);
                        image_input = new Mat();
                        Utils.bitmapToMat(tmp, image_input);
                        //고양이 배변 사진 전경 저장(foreground)
                        Bitmap foreground = imageprocess_and_save();

                        SaveBitmapToFileCache(foreground, mkFilePath(mContext).toString(), String.format("%s.jpg", date));
                        Bitmap foregroundcopy= foreground.copy(Bitmap.Config.ARGB_8888,true);


                        if(foregroundcopy !=null){
                            int w = foregroundcopy.getWidth();
                            int h = foregroundcopy.getHeight();

                            long[][] colorArray= new long[w][h];
                            for (int i = 0; i < w; i++) {
                                for(int j=0; j< h; j++){
                                    long color= foregroundcopy.getPixel(i, j);
                                    colorArray[i][j]=color;
                                    //int n = (int) Long.parseLong("ffff8000", 16);
                                }
                            }

                            boolean detect=false;
                            int c=0;
                            for (int i =0; i< 255; i++){
                                for(int y =0; y< 255; y++){
                                    if(colorArray[i][y]>=-3700000 &&colorArray[i][y]<=-130000){
                                        c++;
                                    }
                                }
                            }
                            if(c>=10000){
                                level="4";
                                status="위급한 상태입니다";
                            }
                            else if(c>=6000){
                                level="3";
                                status="고양이 건강이 좋지 않습니다";
                            } else if (c >= 1000) {
                                level="2";
                                status="주위를 기울여야 합니다.";
                            }
                            else {
                                level="1";
                                status="건강합니다";
                            }
                            //if(colorArray !=null){

                            //}
                        }else {
                            //오류
                        }

                        ByteArrayOutputStream stream = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                        bytes = stream.toByteArray();
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

                                                                                                    //DB 내에 저장
                private void save(byte[] bytes) throws IOException {
                    OutputStream output = null;
                    Log.e(TAG, "세이브 함수 진입 !!!");
                    try{
                        output = new FileOutputStream(file);
                        output.write(bytes);

                        File rotatingFile = new File(mCurrentPhotoPath);
                        Bitmap image = MediaStore.Images.Media
                                .getBitmap(mContext.getContentResolver(), Uri.fromFile(rotatingFile));

                        ExifInterface exif = new ExifInterface(mCurrentPhotoPath);
                        int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED);
                        int exifDegree = exifOrientationToDegrees(exifOrientation);
                        image = rotate(image, exifDegree);
                        Uri rotateUri = getImageUri(mContext, image);

                        final StorageReference riversRef = mStorageRef.child("Feeds").child(currentUID).child(Objects.requireNonNull(intent.getExtras().get("Name")).toString()).child(date+".jpg");
                        UploadTask uploadTask = riversRef.putFile(rotateUri);

                        Task<Uri> uriTask = uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                            @Override
                            public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                                if (!task.isSuccessful()){
                                    SweetToast.error(mContext, "Poopy Photo Error: " + Objects.requireNonNull(task.getException()).getMessage());
                                    Log.e(TAG, "Error: " + task.getException().getMessage());
                                }
                                poopy_uri = riversRef.getDownloadUrl().toString();
                                return riversRef.getDownloadUrl();
                            }
                        }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()){
                                    poopy_uri = Objects.requireNonNull(task.getResult()).toString();

                                    final HashMap<String, Object> update_poopy_data = new HashMap<>();
                                    update_poopy_data.put("poopy_uri", poopy_uri);
                                    update_poopy_data.put("date", date);
                                    update_poopy_data.put("stat", status);
                                    update_poopy_data.put("lv", level);

                                    db.collection("Users").document(currentUID).collection("Cat")
                                            .document(currentPID)
                                            .collection("PoopData").document().set(update_poopy_data, SetOptions.merge())
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    Intent goResult = callResult(update_poopy_data);
                                                    mContext.startActivity(goResult);
                                                    CameraActivity cameraActivity = (CameraActivity) CameraActivity.cameraActivity;
                                                    cameraActivity.finish();
                                                }
                                            });
                                } else if (!task.isSuccessful()){
                                    Log.e(TAG, "Error: " + Objects.requireNonNull(task.getException()).getMessage());
                                }
                            }
                        });

                    } catch(Exception e){
                        e.getMessage();
                        Log.e(TAG, "세이브 시도 중 문제가 발생하였습니다 !!!" + e.getMessage());
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
        File filePath;
                                                                                                    // Android SDK 버전이 29보다 낮은 경우
        if (Build.VERSION.SDK_INT < 29){
            filePath = new File(Environment.getExternalStorageDirectory().getAbsolutePath(),"poopy");
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
            filePath = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)+"/poopy");
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

    private Intent callResult(HashMap<String, Object> map){
        Intent result = new Intent(mContext, ResultActivity.class);
        result.putExtra("uri", poopy_uri);
        result.putExtra("date", date);
        result.putExtra("name", currentName);
        result.putExtra("pid", currentPID);
        return result;
    }

    public static void SaveBitmapToFileCache(Bitmap bitmap, String strFilePath, String filename) {
        File file = new File(strFilePath);

        if (!file.exists())
            file.mkdirs();

        File fileCacheItem = new File(strFilePath + filename);
        OutputStream out = null;

        try {
            fileCacheItem.createNewFile();
            out = new FileOutputStream(fileCacheItem);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);

        } catch (Exception e) {
            e.printStackTrace();

        } finally {
            try {
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public native void imageprocessing(long input_image, long ouput_image);

    //call imageprocessing JNI function
    public Bitmap imageprocess_and_save() {
        if (image_output == null)
            image_output = new Mat();
        imageprocessing(image_input.getNativeObjAddr(), image_output.getNativeObjAddr());
        Bitmap bitmapOutput = Bitmap.createBitmap(image_output.cols(), image_output.rows(), Bitmap.Config.ARGB_8888);
        //image_output to Bitmap
        Utils.matToBitmap(image_output, bitmapOutput);
        return bitmapOutput;
    }

    public int exifOrientationToDegrees(int exifOrientation) {
        if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        }
        else if(exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        else{
            return 90;
        }
    }

    public Bitmap rotate(Bitmap bitmap, int degrees) {
        if(degrees != 0 && bitmap != null) {
            Matrix m = new Matrix();
            m.setRotate(degrees, (float) bitmap.getWidth() / 2,
                    (float) bitmap.getHeight() / 2);
            try {
                Bitmap converted = Bitmap.createBitmap(bitmap, 0, 0,
                        bitmap.getWidth(), bitmap.getHeight(), m, true);
                if(bitmap != converted) {
                    bitmap.recycle();
                    bitmap = converted;
                }
            } catch(OutOfMemoryError ex) {
                // 메모리가 부족하여 회전을 시키지 못할 경우 그냥 원본을 반환
            }
        }
        return bitmap;
    }

    public static Uri getImageUri(Context context, Bitmap inImage) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        inImage.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(context.getContentResolver(), inImage, "Title", null);
        return Uri.parse(path);
    }
}
