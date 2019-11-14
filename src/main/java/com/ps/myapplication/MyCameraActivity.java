package com.ps.myapplication;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.media.ExifInterface;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Ny on 2019-11-14.
 */
public class MyCameraActivity extends Activity {
    private SurfaceView surfaceView;
    private Button button;
    private Button buttonSave;
    private Button buttonCancel;
    private byte[] mData;
    private LocationManager locationManager;
    private String locationProvider;
    private CameraHelper helper;
    private SensorManager sensorManager;
    private int axisX;
    private int axisY;
    private float[] accelerValues;
    private float[] magneticValues;
    private List<Double> mAzimuthList = new ArrayList<>();
    private List<Double> mPitchList = new ArrayList<>();
    private List<Double> mRollList = new ArrayList<>();
    private Sensor sensorG;
    private Sensor sensorC;
    private TextView tv_azimuth;
    private TextView tv_pitch;
    private TextView tv_roll;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.camera_activity);
        surfaceView = findViewById(R.id.surfaceView);
        button = findViewById(R.id.but_ok);
        buttonSave = findViewById(R.id.but_save);
        buttonCancel = findViewById(R.id.but_cancel);
        tv_azimuth = findViewById(R.id.tv_azimuth);
        tv_pitch = findViewById(R.id.tv_pitch);
        tv_roll = findViewById(R.id.tv_roll);
        registerSensorListener();

        locationManager = (LocationManager) getSystemService(getApplicationContext().LOCATION_SERVICE);
        helper = new CameraHelper(this, surfaceView);

        helper.init();
        initListener();
    }

    private void registerSensorListener() {
        if (sensorManager == null) {
            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            sensorG = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorC = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            sensorManager.registerListener(sensorEventListener, sensorG, SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(sensorEventListener, sensorC, SensorManager.SENSOR_DELAY_UI);
        }
        updateCoordinate();
    }

    private void updateCoordinate() {
        WindowManager windowManager = getWindowManager();
        Display display = windowManager.getDefaultDisplay();
        int screenRotaion = display.getRotation();
        switch (screenRotaion) {
            case Surface.ROTATION_0:
                axisX = SensorManager.AXIS_Y;
                axisY = SensorManager.AXIS_X;
                break;
            case Surface.ROTATION_90:
                axisX = SensorManager.AXIS_X;
                axisY = SensorManager.AXIS_Y;
                break;
            case Surface.ROTATION_180:
                break;
            case Surface.ROTATION_270:
                break;
        }
    }

    private final SensorEventListener sensorEventListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
                 accelerValues = event.values;
            }
            if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
                magneticValues = event.values;
            }
            get();
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private void get() {
        if (accelerValues == null || magneticValues == null) {
            return;
        }
        float[] inR = new float[9]; //计算出旋转矩阵
        SensorManager.getRotationMatrix(inR,null,accelerValues, magneticValues);
        float[] outR = new float[9];
        float[] orientationValues = new float[3];
        SensorManager.remapCoordinateSystem(inR,axisX,axisY,outR);
        SensorManager.getOrientation(outR, orientationValues);

        double azimuth = Math.toDegrees(orientationValues[0]);
        double pitch = Math.toDegrees(orientationValues[1]);
        double roll = Math.toDegrees(orientationValues[2]);
        Log.i("zhangchen", "azimuth = azimuth = " + azimuth);
        if (azimuth < 90) {
            azimuth = azimuth + 270;
        } else {
            azimuth = azimuth -90;
        }
        double cachePich = pitch;
        pitch = Math.abs(roll) - 90;
        roll = cachePich;

        Log.i("zhangchen", "azimuth = " + azimuth);
        if (mAzimuthList.size() < 10) {
            mAzimuthList.add(azimuth);
        } else {
            double avg =  getAvgAzimuth(mAzimuthList);
            mAzimuthList.clear();
            String azimuthStr = String.format("%.3f", avg);
            tv_azimuth.setText("方位角:" + azimuthStr);
        }
        if (mPitchList.size() < 10) {
            mPitchList.add(pitch);
        } else {
            double avg =  getAvgAzimuth(mPitchList);
            mPitchList.clear();
            String pitchStr = String.format("%.3f", avg);
            tv_pitch.setText("俯仰角:" + pitchStr);
        }
        if (mRollList.size() < 10) {
            mRollList.add(roll);
        } else {
            double avg =  getAvgAzimuth(mRollList);
            mRollList.clear();
            String rollStr = String.format("%.3f", avg);
            tv_roll.setText("翻滚角:" + rollStr);
        }
    }

    private double getAvgAzimuth(List<Double> azimuthList) {
        double sum = 0;
        int count = azimuthList.size();
        for (Double num : azimuthList) {
            sum = sum+num;
        }
        double avg = sum/count;

        return avg;
    }

    private void initListener() {
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                helper.getCamera().takePicture(new Camera.ShutterCallback() {
                    @Override
                    public void onShutter() {

                    }
                }, null, new Camera.PictureCallback() {
                    @Override
                    public void onPictureTaken(byte[] data, Camera camera) {
                        buttonSave.setVisibility(View.VISIBLE);
                        buttonCancel.setVisibility(View.VISIBLE);
                        button.setVisibility(View.INVISIBLE);
                        mData = data;
                    }
                });
            }
        });
        buttonSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSave.setVisibility(View.INVISIBLE);
                buttonCancel.setVisibility(View.INVISIBLE);
                button.setVisibility(View.VISIBLE);
                savePic(mData);
                helper.startPreview();
            }
        });
        buttonCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buttonSave.setVisibility(View.INVISIBLE);
                buttonCancel.setVisibility(View.INVISIBLE);
                button.setVisibility(View.VISIBLE);
            }
        });
    }

    private void savePic(final byte[] data) {
        Thread thread = new Thread() {
            @Override
            public void run() {
                long temp = System.currentTimeMillis();
                File file = new File(Environment.getExternalStorageDirectory() + "/地理数据采集/临时绘制方案/" + temp + ".jpg");
                File fileDir = new File(Environment.getExternalStorageDirectory() + "/地理数据采集/临时绘制方案/");
                // 创建FileOutputStream对象
                FileOutputStream outputStream = null;
                // 创建BufferedOutputStream对象
                BufferedOutputStream bufferedOutputStream = null;

                try {
                    if (!fileDir.exists()) {
                        fileDir.mkdir();
                    }
                    file.createNewFile();
                    // 获取FileOutputStream对象
                    outputStream = new FileOutputStream(file);
                    // 获取BufferedOutputStream对象
                    bufferedOutputStream = new BufferedOutputStream(outputStream);
                    // 往文件所在的缓冲输出流中写byte数据
                    bufferedOutputStream.write(data);
                    // 刷出缓冲输出流，该步很关键，要是不执行flush()方法，那么文件的内容是空的。
                    bufferedOutputStream.flush();

                    setPictureDegreeZero(file.getPath());
                    getLocation(file.getPath());

                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    // 关闭创建的流对象
                    if (outputStream != null) {
                        try {
                            outputStream.close();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (bufferedOutputStream != null) {
                        try {
                            bufferedOutputStream.close();
                        } catch (Exception e2) {
                            e2.printStackTrace();
                        }
                    }
                }
            }
        };
        thread.start();
    }

    private void getLocation(String path) {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_COARSE);//低精度，如果设置为高精度，依然获取不了location。
        criteria.setAltitudeRequired(false);//不要求海拔
        criteria.setBearingRequired(false);//不要求方位
        criteria.setCostAllowed(true);//允许有花费
        criteria.setPowerRequirement(Criteria.POWER_LOW);//低功耗

        //从可用的位置提供器中，匹配以上标准的最佳提供器
        locationProvider = locationManager.getBestProvider(criteria, true);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        Location location = locationManager.getLastKnownLocation(locationProvider);
        Log.d("zhangchen", "onCreate: " + (location == null) + "..");
        if (location != null) {
            setPictureLocation(path, location);
        }
    }

    private void setPictureLocation(String path, Location location) {
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, String.valueOf(location.getLatitude()));
            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, String.valueOf(location.getLongitude()));
            exifInterface.saveAttributes();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPictureDegreeZero(String path) {
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            // 修正图片的旋转角度，设置其不旋转。这里也可以设置其旋转的角度，可以传值过去，
            // 例如旋转90度，传值ExifInterface.ORIENTATION_ROTATE_90，需要将这个值转换为String类型的
            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(ExifInterface.ORIENTATION_ROTATE_90));
            exifInterface.saveAttributes();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
