package com.example.poopy.ui.camera;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;

import com.example.poopy.R;


public class CameraActivity extends AppCompatActivity {

    public static Intent intent;
    public static AppCompatActivity cameraActivity;

    private TextureView textureView;
    private CameraPreview cameraPreview;
    private Button btnCapture;

    private String TAG = "CAM_ACTIVITY";

    static final int REQUEST_CAM = 1;
    static final int REQUEST_STORAGE = 2;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.fragment_cam);

        intent = getIntent();
        cameraActivity = CameraActivity.this;

        btnCapture = (Button) findViewById(R.id.btnCapture);
        textureView = (TextureView) findViewById(R.id.cameraTextureView);

        cameraPreview = new CameraPreview(this, textureView, btnCapture);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode){
            case REQUEST_CAM:
                for (int i = 0 ; i < permissions.length ; i++){
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.CAMERA)){
                        if (grantResult == PackageManager.PERMISSION_GRANTED){
                            Log.d(TAG, "Preview Set !!!");
                            cameraPreview.openCamera();
                        } else {
                            Toast.makeText(this, "카메라 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }
                break;
            case REQUEST_STORAGE:
                for (int i = 0 ; i < permissions.length ; i++){
                    String permission = permissions[i];
                    int grantResult = grantResults[i];
                    if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                        if (grantResult == PackageManager.PERMISSION_GRANTED){
                            Log.d(TAG, "Preview Set !!!");
                            cameraPreview.openCamera();
                        } else {
                            Toast.makeText(this, "저장소 권한이 필요합니다.", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    }
                }
                break;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        cameraPreview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        cameraPreview.onPause();
    }
}