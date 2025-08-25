package com.example.billboarddetector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.FileOutputStream;
import java.io.IOException;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    // Class variables
    private PreviewView previewView;
    private Button captureButton;
    private TextView resultTextView;
    private ImageCapture imageCapture;
    private ExecutorService cameraExecutor;
    private FusedLocationProviderClient fusedLocationClient;
    private Location currentLocation;

    // ActivityResultLauncher for permissions
    private final ActivityResultLauncher<String[]> multiplePermissionsLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    if (!isGranted) {
                        allGranted = false;
                        break;
                    }
                }

                if (allGranted) {
                    startCamera();
                    getCurrentLocation();
                } else {
                    Toast.makeText(this, "Permissions not granted.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize views
        previewView = findViewById(R.id.camera_preview);
        captureButton = findViewById(R.id.capture_button);
        resultTextView = findViewById(R.id.result_textview);

        // Initialize executor
        cameraExecutor = Executors.newSingleThreadExecutor();

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Request permissions
        multiplePermissionsLauncher.launch(new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.ACCESS_FINE_LOCATION
        });

        // Set up the capture button listener
        captureButton.setOnClickListener(v -> takePhoto());
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                imageCapture = new ImageCapture.Builder().build();

                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;

                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);

            } catch (ExecutionException | InterruptedException e) {
                Log.e("CameraX", "Use case binding failed", e);
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private File compressImage(File originalFile) {
        try {
            // Decode bounds only
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(originalFile.getPath(), opts);

            // crude downsample: aim ~2MP
            int reqPixels = 2_000_000;
            int inSample = 1;
            long totalPixels = (long) opts.outWidth * (long) opts.outHeight;
            while (totalPixels / (inSample * inSample) > reqPixels) inSample <<= 1;

            opts.inJustDecodeBounds = false;
            opts.inSampleSize = Math.max(1, inSample);
            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getPath(), opts);

            if (bitmap == null) {
                Log.w("ImageCompression", "decodeFile returned null; using original.");
                return originalFile;
            }

            File compressedFile = new File(getOutputDirectory(), "compressed_" + originalFile.getName());
            try (FileOutputStream fos = new FileOutputStream(compressedFile)) {
                boolean ok = bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
                if (!ok) {
                    Log.w("ImageCompression", "Bitmap compress failed; using original.");
                    return originalFile;
                }
            }

            long originalSize = originalFile.length() / 1024;
            long compressedSize = compressedFile.length() / 1024;
            Log.d("ImageCompression", "Original: " + originalSize + " KB, Compressed: " + compressedSize + " KB");
            return compressedFile;

        } catch (Exception e) {
            Log.e("ImageCompression", "Compression error; using original.", e);
            return originalFile;
        }
    }

    private void takePhoto() {
        Log.d("Flow", "takePhoto: start");

        if (imageCapture == null) {
            Log.e("CameraX", "imageCapture is null; camera not bound yet");
            return;
        }

        File photoFile = new File(getOutputDirectory(),
                new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US)
                        .format(System.currentTimeMillis()) + ".jpg");

        Log.d("Flow", "Output file: " + photoFile.getAbsolutePath());

        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(
                outputOptions,
                cameraExecutor,
                new ImageCapture.OnImageSavedCallback() {
                    @Override
                    public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                        Log.d("Flow", "onImageSaved: fileExists=" + photoFile.exists() + " size=" + photoFile.length());
                        File compressedPhotoFile = compressImage(photoFile);
                        Log.d("Flow", "compressed: exists=" + compressedPhotoFile.exists() + " size=" + compressedPhotoFile.length());
                        uploadImageAndLocation(compressedPhotoFile, currentLocation);
                    }

                    @Override
                    public void onError(@NonNull ImageCaptureException exception) {
                        Log.e("CameraX", "Photo capture failed: " + exception.getMessage(), exception);
                    }
                }
        );
    }

    private void uploadImageAndLocation(File imageFile, Location location) {
        if (location == null) {
            runOnUiThread(() -> Toast.makeText(this, "Location not available. Cannot upload.", Toast.LENGTH_SHORT).show());
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), imageFile);
        MultipartBody.Part body = MultipartBody.Part.createFormData("image", imageFile.getName(), requestFile);

        RequestBody latitude = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(location.getLatitude()));
        RequestBody longitude = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(location.getLongitude()));

        ApiService apiService = RetrofitClient.getApiService();
        Call<AnalysisResult> call = apiService.uploadBillboard(body, latitude, longitude);

        runOnUiThread(() -> resultTextView.setText("Analyzing..."));

        call.enqueue(new Callback<AnalysisResult>() {
            @Override
            public void onResponse(@NonNull Call<AnalysisResult> call, @NonNull Response<AnalysisResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    AnalysisResult result = response.body();
                    String resultText = "Authorized: " + result.isAuthorized() + "\nReason: " + result.getReason();
                    resultTextView.setText(resultText);
                } else {
                    resultTextView.setText("Error: " + response.code() + " " + response.message());
                }
            }

            @Override
            public void onFailure(@NonNull Call<AnalysisResult> call, @NonNull Throwable t) {
                Log.e("Network", "Upload failed", t);
                resultTextView.setText("Upload failed: " + t.getMessage());
            }
        });
    }

    private File getOutputDirectory() {
        // Prefer external app-specific Pictures dir
        File externalPics = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        if (externalPics != null && (externalPics.exists() || externalPics.mkdirs())) {
            File appDir = new File(externalPics, getString(R.string.app_name));
            if (!appDir.exists()) appDir.mkdirs();
            if (appDir.exists()) return appDir;
        }

        // Defensive fallback to internal app files
        File internal = getFilesDir();
        File appDir = new File(internal, "images");
        if (!appDir.exists()) appDir.mkdirs();
        return appDir.exists() ? appDir : internal;
    }

    @SuppressLint("MissingPermission")
    private void getCurrentLocation() {
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        currentLocation = location;
                        Toast.makeText(this, "Location: " + location.getLatitude() + ", " + location.getLongitude(), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Could not get location.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
