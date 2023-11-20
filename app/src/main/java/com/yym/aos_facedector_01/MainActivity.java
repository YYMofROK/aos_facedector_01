package com.yym.aos_facedector_01;

import android.Manifest;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.RectF;
import android.media.Image;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceView;
import android.widget.ImageView;
import android.widget.Toast;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.Camera;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceDetection;
import com.google.mlkit.vision.face.FaceDetector;
import com.google.mlkit.vision.face.FaceDetectorOptions;
import com.google.mlkit.vision.face.FaceLandmark;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.View;



public class MainActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION_REQUEST_CODE = 1001;
    private PreviewView previewView;
    private Camera camera;
    private ExecutorService cameraExecutor;
    private ImageCapture imageCapture;
    private double previousAverageRGB = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 세로 모드로 고정
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        previewView = findViewById(R.id.previewView);
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            initializeCamera();
        }
    }

    private void initializeCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();

                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());

                FaceDetectorOptions options =
                        new FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                                .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_ALL)
                                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                                .setMinFaceSize(0.15f)
                                .enableTracking()
                                .build();
                FaceDetector faceDetector = FaceDetection.getClient(options);

                // ImageCapture 설정
                imageCapture = new ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build();

                // ImageAnalysis 설정
                ImageAnalysis imageAnalysis = new ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
//                        .setTargetResolution(new Size(640, 480))
                        .build();

                imageAnalysis.setAnalyzer(cameraExecutor, new ImageAnalysis.Analyzer() {
                    @Override
                    public void analyze(@NonNull ImageProxy imageProxy) {
                        processFaceDetection(imageProxy, faceDetector);
                    }
                });

                CameraSelector cameraSelector = new CameraSelector.Builder()
                        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
                        .build();

                camera = cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis, imageCapture);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, ContextCompat.getMainExecutor(this));
    }




    private void processFaceDetection(ImageProxy imageProxy, FaceDetector faceDetector) {
        InputImage inputImage = InputImage.fromMediaImage(imageProxy.getImage(), imageProxy.getImageInfo().getRotationDegrees());
        faceDetector.process(inputImage)
                .addOnSuccessListener(faces -> {

                    Log.d("check:::::", "faces:::::" + String.valueOf(faces.toArray().length));

                    if( faces.toArray().length > 0 ) {
                        for (Face face : faces) {
                            drawRectangleOnFace(face, imageProxy);
                        }
                    }else{
                        // 이미지뷰에 null 설정하여 화면을 비움
                        ImageView imageView = findViewById(R.id.imageView);
                        runOnUiThread(() -> imageView.setImageBitmap(null));

                    }// end if


                })
                .addOnFailureListener(e -> {
                    e.printStackTrace();
                })
                .addOnCompleteListener(result -> imageProxy.close());
    }

/*
    // 기종별 가중치를 리턴하는 함수
    public float[] getDevice_scale_add_Rectang(ImageProxy imageProxy) {

        String deviceModel_Name = Build.MODEL;

        Log.d("check:::::", "deviceModel:::::" + String.valueOf(deviceModel_Name));

        Map<String, Map<String, Integer>> deviceModel_Map = new HashMap<>();
        Map<String, Integer> innerMap = new HashMap<>();
        innerMap.put("box_X", 1);
        innerMap.put("box_Y", 1);
        deviceModel_Map.put("SM-T225N", innerMap);





            // 이제 'a' 키로 맵을 참조하고 'aaa' 키로 값을 가져올 수 있습니다.
        int value = a.get("a").get("aaa");

        /*
        float[] scale = {1.0f, 1.0f};

        // 이미지 회전 각도 가져오기
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

        // 이미지 회전 각도에 따라 좌표 조정
        if (rotationDegrees == 0 || rotationDegrees == 180) {
            // 여기서 원하는 대로 값을 조정하세요
            scale[0] = 2.0f;
            scale[1] = 3.0f;
        }

        return scale;


    }
*/


    private void drawRectangleOnFace(Face face, ImageProxy imageProxy) {

        // 이미지 프록시의 크기 가져오기
        int imageWidth = imageProxy.getWidth();
        int imageHeight = imageProxy.getHeight();

        //  영상 출력 뷰 의 화면 크기 가져오기
        int viewWidth = previewView.getWidth();
        int viewHeight = previewView.getHeight();


        // 실제 이미지 크기와 이미지뷰 크기를 비교하여 비율 계산
        float scaleX = viewWidth / (float) imageWidth;
        float scaleY = viewHeight / (float) imageHeight;

        // 얼굴의 좌표 가져오기
        Rect boundingBox = face.getBoundingBox();

        // Rect를 RectF로 변환
        RectF boundingBoxF = new RectF(boundingBox);

        // 이미지 회전 각도 가져오기
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

        // 이미지 회전 각도에 따라 좌표 조정
        if (rotationDegrees == 90 || rotationDegrees == 270) {

            scaleX *= 1.4;
            scaleY *= 1;

            // 비율을 곱하여 이미지뷰 상의 좌표로 변환
            boundingBoxF.left *= scaleX;
            boundingBoxF.top *= (scaleY - 1 );
            boundingBoxF.right *= scaleX;
            boundingBoxF.bottom *= ( scaleY - 0.7 );

            // 이미지가 90도 또는 270도 회전된 경우 좌표를 좌우 반전
            boundingBoxF.left = viewWidth - boundingBoxF.left;
            boundingBoxF.right = viewWidth - boundingBoxF.right;

            // 좌우를 반전한 후 좌표를 교환
            float temp = boundingBoxF.left;
            boundingBoxF.left = boundingBoxF.right;
            boundingBoxF.right = temp;
        }// end if


        // 이미지 회전 각도에 따라 좌표 조정
        if (rotationDegrees == 0 || rotationDegrees == 180) {

            scaleX *= 1;
            scaleY *= 1.2;

            // 비율을 곱하여 이미지뷰 상의 좌표로 변환
            boundingBoxF.left *= scaleX;
            boundingBoxF.top *= (scaleY - 0.8 );
            boundingBoxF.right *= scaleX;
            boundingBoxF.bottom *= ( scaleY - 0.2 );

            // 이미지가 90도 또는 270도 회전된 경우 좌표를 좌우 반전
            boundingBoxF.left = viewWidth - boundingBoxF.left;
            boundingBoxF.right = viewWidth - boundingBoxF.right;

            // 좌우를 반전한 후 좌표를 교환
            float temp = boundingBoxF.left;
            boundingBoxF.left = boundingBoxF.right;
            boundingBoxF.right = temp;

        }// end if

        Log.d("check:::::", "rotationDegrees:::::" + String.valueOf(rotationDegrees));
        Log.d("check:::::", "previewView.getWidth():::::" + String.valueOf(viewWidth));
        Log.d("check:::::", "previewView.getHeight():::::" + String.valueOf(viewHeight));
        Log.d("check:::::", "imageProxy.getWidth():::::" + String.valueOf(imageWidth));
        Log.d("check:::::", "imageProxy.getHeight():::::" + String.valueOf(imageHeight));
//
//        Log.d("check:::::", "scaleX:::::" + String.valueOf(scaleX));
//        Log.d("check:::::", "scaleY:::::" + String.valueOf(scaleY));


//        Log.d("check:::::", "boundingBoxF.left:::::" + String.valueOf(boundingBoxF.left));
//        Log.d("check:::::", "boundingBoxF.top:::::" + String.valueOf(boundingBoxF.top));
//        Log.d("check:::::", "boundingBoxF.right:::::" + String.valueOf(boundingBoxF.right));
//        Log.d("check:::::", "boundingBoxF.bottom:::::" + String.valueOf(boundingBoxF.bottom));


        // 화면에 그리기 위한 Paint 객체 생성
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);

        // Bitmap을 생성하여 Canvas에 그리기
        Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawRoundRect(boundingBoxF, 16, 16, paint);


        // 랜드마크 정보 가져오기
        List<FaceLandmark> landmarks = face.getAllLandmarks();


        // 랜드마크에 점 그리기
        int landmarkIndex = 0;
        for (FaceLandmark landmark : landmarks) {
            drawPointOnLandmark(landmark.getPosition(), landmarkIndex++, imageProxy, canvas, scaleX, scaleY);
        }

        // 이미지뷰에 Bitmap 설정하여 화면에 출력
        ImageView imageView = findViewById(R.id.imageView);
        runOnUiThread(() -> imageView.setImageBitmap(bitmap));

    }


    // 랜드마크 좌표에 점을 그리는 메서드 추가
    private void drawPointOnLandmark(PointF landmarkPosition, int landmarkIndex,ImageProxy imageProxy, Canvas canvas, float scaleX, float scaleY) {
        // 좌표 변환
        float viewWidth = previewView.getWidth();
        float viewHeight = previewView.getHeight();

        // 비율을 곱하여 이미지뷰 상의 좌표로 변환
        float landmarkX = 0.0F;
        float landmarkY = 0.0F;

        // 이미지 회전 각도 가져오기
        int rotationDegrees = imageProxy.getImageInfo().getRotationDegrees();

        // 이미지 회전 각도에 따라 좌표 조정
        if (rotationDegrees == 90 || rotationDegrees == 270) {
            landmarkX = landmarkPosition.x * scaleX;
            landmarkY = (float) (landmarkPosition.y * ( scaleY - 0.6 ));

            landmarkX = viewWidth - landmarkX;
        }

        if (rotationDegrees == 0 || rotationDegrees == 180) {
            landmarkX = landmarkPosition.x * scaleX;
            landmarkY = (float) (landmarkPosition.y * ( scaleY - 0.2 ));
            landmarkX = viewWidth - landmarkX;
        }

        // 화면에 그리기 위한 Paint 객체 생성
        Paint paint = new Paint();
        paint.setColor(Color.GREEN);
        paint.setStyle(Paint.Style.FILL);
        paint.setStrokeWidth(10);

        float textSize = 20f; // 텍스트 크기 조절
        paint.setTextSize(textSize);

        // Canvas에 점 그리기
//        canvas.drawPoint(landmarkX, landmarkY, paint);

        // 랜드마크 번호 표시
        canvas.drawText(String.valueOf(landmarkIndex), landmarkX, landmarkY, paint);
    }








    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeCamera();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
}
