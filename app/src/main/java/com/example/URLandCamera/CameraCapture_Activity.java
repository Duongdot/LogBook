package com.example.URLandCamera;

import android.content.ContentValues;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;

public class CameraCapture_Activity extends AppCompatActivity {
    private PreviewView previewView;
    private final String[] REQUIRED_PERMISSIONS = new String[]{"android.permission.CAMERA"};
    private ImageCapture imageCapture;
    private ImageView imageView;
    private TextView itemLabel;
    private int currentIndex = -1;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_main);
        previewView = findViewById(R.id.previewView);
        Button buttonTakePhoto = findViewById(R.id.btnTakePhoto);
        Button btnNext = findViewById(R.id.btnNext);
        Button btnPrev = findViewById(R.id.btnPrev);
        imageView = findViewById(R.id.imageCamera);
        itemLabel = findViewById(R.id.name);
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            int REQUEST_CODE_PERMISSIONS = 101;
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        buttonTakePhoto.setOnClickListener(v -> takePhoto());
        btnNext.setOnClickListener(v -> {
            currentIndex++;
            setAnimationLeftToRight();
            setAnimationRightToLeft();
            setImageFromStorage();
        });
        btnPrev.setOnClickListener(v -> {
            currentIndex--;
            setAnimationRightToLeft();
            setAnimationLeftToRight();
            setImageFromStorage();
        });

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            Toast.makeText(this, "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            finish();
        }

    }


    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getApplicationContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    public void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                // Camera provider is now guaranteed to be available
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                // Set up the view finder use case to display camera preview
                Preview preview = new Preview.Builder().build();
                // Choose the camera by requiring a lens facing
                CameraSelector cameraSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

                // Image capture use case
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();
                // Connect the preview use case to the previewView
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                // Attach use cases to the camera with the same lifecycle owner
                Camera camera = cameraProvider.bindToLifecycle((this), cameraSelector, preview, imageCapture);
            } catch (InterruptedException | ExecutionException e) {
                // handle InterruptedException.
                Toast.makeText(this, "Error:" + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        long timestamp = System.currentTimeMillis();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, timestamp);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");

        ImageCapture.OutputFileOptions outputFileOptions =
                new ImageCapture.OutputFileOptions.Builder(getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues).build();
        imageCapture.takePicture(outputFileOptions, getExecutor(),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(ImageCapture.OutputFileResults outputFileResults) {

                        List<String> imageAbsolutePaths = getImageAbsolutePaths();
                        // display recent captured photo
                        Glide.with(CameraCapture_Activity.this).load(imageAbsolutePaths.size()-1)
                                .centerCrop()
                                .into(imageView);
                        itemLabel.setText(outputFileResults.getSavedUri().getPath());
                        Toast.makeText(CameraCapture_Activity.this, "Photo has been saved successfully. " + imageAbsolutePaths.size() + "@" + outputFileResults.getSavedUri().getPath(), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onError(ImageCaptureException exception) {
                        Toast.makeText(CameraCapture_Activity.this, "Error saving photo: " + exception.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    private void setImageFromStorage() {
        List<String> imageAbsolutePaths = getImageAbsolutePaths();
        final int count = imageAbsolutePaths.size();

        if (count == 0) {
            Toast.makeText(CameraCapture_Activity.this, "Not photo found", Toast.LENGTH_SHORT).show();
        } else {

            if (currentIndex == count) {
                currentIndex = 0;
            }
            else if (currentIndex < 0) {
                currentIndex = count - 1;
            }
            final String imagePath = imageAbsolutePaths.get(currentIndex);
            itemLabel.setText(imagePath);
            Glide.with(this).load(imagePath)
                    .centerCrop()
                    .into(imageView);
        }
    }

    private List<String> getImageAbsolutePaths() {
        final List<String> paths = new ArrayList();
        final Uri uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        final String[] projection = {MediaStore.MediaColumns.DATA,
                MediaStore.Images.Media.BUCKET_DISPLAY_NAME};
        final String orderBy = MediaStore.Images.Media.DATE_TAKEN;
        final Cursor cursor = this.getContentResolver().query(uri, projection, null,
                null, orderBy);
        final int column_index_data = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
        while (cursor.moveToNext()) {
            final String absolutePathOfImage = cursor.getString(column_index_data);
            paths.add(absolutePathOfImage);
        }
        cursor.close();
        return paths;
    }

    private void setAnimationLeftToRight() {
        // Animation using pre-defined android Slide Left-to-Right
        Animation in_left = AnimationUtils.loadAnimation(this, android.R.anim.slide_in_left);
        imageView.setAnimation(in_left);
    }

    private void setAnimationRightToLeft() {
        // Animation using pre-defined android Slide Left-to-Right
        Animation in_right = AnimationUtils.loadAnimation(this, android.R.anim.slide_out_right);
        imageView.setAnimation(in_right);
    }

    Executor getExecutor() {
        return ContextCompat.getMainExecutor(this);
    }
}