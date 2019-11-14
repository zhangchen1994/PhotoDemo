package com.ps.myapplication;


import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.IOException;
import java.util.List;

/**
 * Created by Ny on 2019-11-14.
 */
public class CameraHelper implements Camera.PreviewCallback {
    private Camera mCamera = null;
    private Camera.Parameters mParameters;
    private SurfaceView mSurfaceView;
    private SurfaceHolder surfaceHolder;
    private Activity mActivity;
    private int mCameraFacing = Camera.CameraInfo.CAMERA_FACING_BACK;
    private int mDisplayOrientation = 0;
    private int picWidth = 1600;
    private int picHeight = 2560;

    public CameraHelper(Activity activity, SurfaceView surfaceView) {
        mActivity = activity;
        mSurfaceView = surfaceView;
    }

    public Camera getCamera() {
        return mCamera;
    }

    public void init() {
        surfaceHolder = mSurfaceView.getHolder();

        surfaceHolder.addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                if (mCamera == null) {
                    openCamera(mCameraFacing);  //打开相机
                }
            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                //释放相机资源
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            }
        });
    }

    private void openCamera(int cameraFacing) {
        boolean supportCameraFacing = supportCameraFacing(cameraFacing);
        if (supportCameraFacing) {
            mCamera = Camera.open(cameraFacing);
            initParameters(mCamera);
            mCamera.setPreviewCallback(this);
            startPreview();
        }
    }

    private Camera.Size getBestSize(int targetWidth, int targetHeight, List<Camera.Size> sizeList) {
        Camera.Size bestSize = null;
        double targetRatio = Double.parseDouble(String.valueOf(targetHeight))/targetWidth;
        double minDiff = targetRatio;

        for (Camera.Size size : sizeList) {
            double supportedRatio = Double.parseDouble(String.valueOf(size.width))/size.height;
            //Log.i("zhangchen","系统支持的宽高 :" + size.height + ", " + size.width + ",比例是 ： " + supportedRatio);
            if (size.width == targetHeight && size.height == targetWidth) {
                bestSize = size;
                break;
            }
            if (Math.abs(supportedRatio - targetRatio) < minDiff) {
                minDiff = Math.abs(supportedRatio - targetRatio);
                bestSize =size;
            }
        }
        Log.i("zhangchen","目标尺寸是 ： " + targetWidth + ", " + targetHeight);
        Log.i("zhangchen", "最优是 ： " + bestSize.height + ", " + bestSize.width);
        return bestSize;
    }

    public void startPreview() {
        try {
            mCamera.setPreviewDisplay(surfaceHolder);
            mCamera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void initParameters(Camera mCamera) {
        mParameters = mCamera.getParameters();
        mParameters.setPreviewFormat(ImageFormat.NV21);//设置预览图片格式
        Camera.Size  bestPreviewSize = getBestSize(mSurfaceView.getWidth(), mSurfaceView.getHeight(), mParameters.getSupportedPreviewSizes());

        mParameters.setPreviewSize(bestPreviewSize.width,bestPreviewSize.height);//预览尺寸

        Camera.Size bestPicSize = getBestSize(picWidth,picHeight, mParameters.getSupportedPictureSizes());
        mParameters.setPictureSize(bestPicSize.width, bestPicSize.height);// 保存图片尺寸

        mParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);

        mCamera.setParameters(mParameters);
        mCamera.setDisplayOrientation(90);
    }

    private boolean supportCameraFacing(int cameraFacing) {
        Camera.CameraInfo cameraInfo = new  Camera.CameraInfo();
        for (int i=0;i < Camera.getNumberOfCameras();i++) {
            Camera.getCameraInfo(i,cameraInfo);
            if (cameraInfo.facing == cameraFacing) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {

    }
}
