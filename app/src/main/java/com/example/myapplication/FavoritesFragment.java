package com.example.myapplication;

import android.Manifest;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.camera.core.AspectRatio;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

import android.widget.ImageButton;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraControl;
import androidx.camera.core.CameraInfo;

import android.database.Cursor;
import android.provider.MediaStore;
import android.widget.ImageView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class FavoritesFragment extends Fragment {

    private static final String TAG = "FavoritesFragment";
    private static final int REQUEST_CODE_PERMISSIONS = 10;
    private static final String[] REQUIRED_PERMISSIONS = {Manifest.permission.CAMERA};

    private PreviewView previewView;
    private Button btnTakePhoto;
    private ImageButton btnToggleCamera, btnFlash;
    private ListenableFuture<ProcessCameraProvider> cameraProviderFuture;
    private ImageCapture imageCapture;
    private CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

    private TFLiteHelper tfliteHelper;
    private CameraOverlay cameraOverlay;
    private Camera camera;
    private boolean isFlashOn = false;



    public FavoritesFragment() { }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_favorites_fragment, container, false);

        previewView = view.findViewById(R.id.previewView);
        btnTakePhoto = view.findViewById(R.id.btnTakePhoto);
        cameraOverlay = view.findViewById(R.id.cameraOverlay);
        btnFlash = view.findViewById(R.id.btnFlash);
        btnToggleCamera = view.findViewById(R.id.btnToggleCamera);


        tfliteHelper = new TFLiteHelper(getContext()); // initialize TFLite


        // Check permissions
        if (allPermissionsGranted()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(getActivity(), REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        }

        btnToggleCamera.setOnClickListener(v -> toggleCamera());
        btnTakePhoto.setOnClickListener(v -> takePhoto());
        btnFlash.setOnClickListener(v -> toggleFlashMode());


        return view;
    }

    private void startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(getContext());
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                bindPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error starting camera", e);
            }
        }, ContextCompat.getMainExecutor(getContext()));
    }

    private void bindPreview(ProcessCameraProvider cameraProvider) {
        Preview preview = new Preview.Builder().build();
        imageCapture = new ImageCapture.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(ImageCapture.FLASH_MODE_AUTO)
                .build();


        preview.setSurfaceProvider(previewView.getSurfaceProvider());

        try {
            cameraProvider.unbindAll();
            camera = cameraProvider.bindToLifecycle((LifecycleOwner) this, cameraSelector, preview, imageCapture);
        } catch (Exception e) {
            Log.e(TAG, "Use case binding failed", e);
        }
    }

    private void toggleFlashMode() {
        if (imageCapture == null) return;

        int currentMode = imageCapture.getFlashMode();
        int newMode;

        switch (currentMode) {
            case ImageCapture.FLASH_MODE_OFF:
                newMode = ImageCapture.FLASH_MODE_ON;
                btnFlash.setImageResource(R.drawable.ic_flash_on);
                break;
            case ImageCapture.FLASH_MODE_ON:
                newMode = ImageCapture.FLASH_MODE_AUTO;
                btnFlash.setImageResource(R.drawable.ic_flash_auto);
                break;
            default: // AUTO
                newMode = ImageCapture.FLASH_MODE_OFF;
                btnFlash.setImageResource(R.drawable.ic_flash_off);
                break;
        }

        imageCapture.setFlashMode(newMode);
    }


    private void toggleCamera() {
        cameraSelector = (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA)
                ? CameraSelector.DEFAULT_FRONT_CAMERA
                : CameraSelector.DEFAULT_BACK_CAMERA;
        startCamera();
    }

    private void takePhoto() {
        if (imageCapture == null) return;

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "IMG_" + timeStamp + ".jpg";

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/MushroomApp");

        ImageCapture.OutputFileOptions outputOptions =
                new ImageCapture.OutputFileOptions.Builder(
                        requireContext().getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        contentValues
                ).build();

        imageCapture.takePicture(outputOptions, ContextCompat.getMainExecutor(getContext()),
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e(TAG, "Photo capture failed: " + exception.getMessage(), exception);
                        Toast.makeText(getContext(), "Photo capture failed!", Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults output) {
                        Uri savedUri = output.getSavedUri();
                        Toast.makeText(getContext(), "Photo saved to Gallery!", Toast.LENGTH_SHORT).show();
                        Log.d(TAG, "Photo saved: " + savedUri);

                        try {
                            Bitmap bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), savedUri);
                            ClassificationResult result = tfliteHelper.classify(bitmap);

                            Intent intent = new Intent(getContext(), ResultActivity.class);
                            intent.putExtra("photoUri", savedUri.toString());
                            intent.putExtra("prediction", result.label);      // send label
                            intent.putExtra("confidence", result.confidence); // send confidence
                            startActivity(intent);



                        } catch (IOException e) {
                            e.printStackTrace();
                            Toast.makeText(getContext(), "Failed to load photo", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void classifyPhoto(File photoFile) {
        try {
            FileInputStream fis = new FileInputStream(photoFile);
            Bitmap bitmap = BitmapFactory.decodeStream(fis);
            fis.close();

            ClassificationResult result = tfliteHelper.classify(bitmap);

            Intent intent = new Intent(getContext(), ResultActivity.class);
            intent.putExtra("photoPath", photoFile.getAbsolutePath());
            intent.putExtra("prediction", result.label);      // send label
            intent.putExtra("confidence", result.confidence); // send confidence
            startActivity(intent);


        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Failed to load photo for classification", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean allPermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera();
            } else {
                Toast.makeText(getContext(), "Permissions not granted by the user.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (cameraProviderFuture != null) {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                cameraProvider.unbindAll();
            } catch (ExecutionException | InterruptedException e) {
                Log.e(TAG, "Error shutting down camera", e);
            }
        }
    }
}
