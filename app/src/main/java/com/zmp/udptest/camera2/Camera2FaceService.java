package com.zmp.udptest.camera2;

import android.Manifest;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.util.Size;
import android.view.Surface;

import com.zmp.udptest.AndroidEncode;
import com.zmp.udptest.CameraConfig;
import com.zmp.udptest.SocketStreamUtils;
import com.zmp.udptest.ThreadPoolProxyFactory;

import org.greenrobot.eventbus.EventBus;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

/**
 * @author zmp
 * <p>
 * Camera2 实现后台无界面预览数据
 * 数据接收EventBus {@link Camera2FaceService#initImageReader}
 * EventBus.getDefault().post(bytes)
 */
public class Camera2FaceService extends Service {

        private static final String TAG = "Camera2FaceService";

        public static final String ACTION_KEY = "action_key";

        public static final int ACTION_OPEN_CAMERA = 1;

        public static final int ACTION_CLOSE_CAMERA = 2;

        private ImageReader mImageReader;

        private Handler backgroundHandler;

        private Handler udpHandler;

        private CaptureRequest.Builder mPreviewRequestBuilder;

        private CameraDevice mCameraDevice;

        private volatile boolean isOpen;

        private String multicastHost = "224.0.0.1";

        private InetAddress group;

        private MulticastSocket mss;

        private final int port = 8005;

        private AndroidEncode mEncode;


        private boolean h264 = true;

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
                return null;
        }


        @Override
        public void onCreate() {
                super.onCreate();
                Log.d(TAG, "onCreate: ");
                HandlerThread handlerThread = new HandlerThread(TAG);
                handlerThread.start();
                backgroundHandler = new Handler(handlerThread.getLooper());
                HandlerThread udpThread = new HandlerThread(TAG + TAG);
                udpThread.start();
                udpHandler = new Handler(udpThread.getLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                                switch (msg.what) {
                                        case 0:
                                                try {
                                                        group = InetAddress.getByName(multicastHost);
                                                        try {
                                                                mss = new MulticastSocket(port);
                                                                mss.joinGroup(group);
                                                                if (h264) {
                                                                        sendEmptyMessage(2);
                                                                }
                                                                Log.e(TAG, "joinGroup: success");
                                                        } catch (IOException e) {
                                                                e.printStackTrace();
                                                                Log.e(TAG, "handleMessage: ", e);
                                                                mss = null;
                                                                sendEmptyMessageDelayed(0, 10 * 1000);
                                                        }

                                                } catch (UnknownHostException e1) {
                                                        e1.printStackTrace();
                                                        mss = null;
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                }
                                                break;
                                        case 1:
                                                if (mss == null) {
                                                        break;
                                                }
                                                byte[] buffer = (byte[]) msg.obj;
                                                sendHeadData(buffer.length);
                                                sendUdpData(buffer, 0, buffer.length);
                                                sendEndData(buffer.length);
                                                break;

                                        case 2:
                                                ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(
                                                        new Runnable() {
                                                                @Override
                                                                public void run() {

                                                                        byte[] buffers = new byte[1024];
                                                                        DatagramPacket dp = new DatagramPacket(buffers, buffers.length);
                                                                        Log.e(TAG, "handleMessage: receive");
                                                                        try {
                                                                                while (mss != null) {
                                                                                        mss.receive(dp);
                                                                                        byte[] data = dp.getData();
                                                                                        mergeDatum(data, dp.getLength());
                                                                                }
                                                                        } catch (IOException e) {
                                                                                e.printStackTrace();
                                                                                mss = null;
                                                                                removeMessages(0);
                                                                                sendEmptyMessageDelayed(0, 10 * 1000);
                                                                        }
                                                                }
                                                        }
                                                );
                                                break;
                                        default:
                                }
                        }
                };
                udpHandler.sendEmptyMessage(0);
                initImageReader();
        }

        private void mergeDatum(byte[] data, int length) {
                if (length == openArr.length) {
                        for (int i = 0; i < length; i++) {
                                if (data[i] != openArr[i]) {
                                        return;
                                }
                        }
                        if (arr != null) {
                                Log.e(TAG, "mergeDatum open");
                                udpHandler.obtainMessage(1, arr).sendToTarget();
                        }
                }
        }

        byte[] startArr = {0x0a, 0x0a, 0x00, 0x00, 0x00, 0x00};

        byte[] endArr = {0x0b, 0x0b, 0x00, 0x00, 0x00, 0x00};

        byte[] openArr = {0x0c, 0x0c, 0x0c, 0x0c, 0x0c, 0x0c};

        private void sendUdpData(byte[] buffer, int start, int length) {
                if (length <= start) {
                        return;
                }
                byte[] bytes;
                if (length - start > 1024) {
                        bytes = Arrays.copyOfRange(buffer, start, start + 1024);
                } else {
                        bytes = Arrays.copyOfRange(buffer, start, length);
                }
                final DatagramPacket dp = new DatagramPacket(
                        bytes, bytes.length, group, port);
                try {
                        mss.send(dp);
                        Log.d(TAG, "mss.send(dp);");
                } catch (IOException e) {
                        e.printStackTrace();
                        return;
                }
                start += 1024;
                sendUdpData(buffer, start, length);
        }


        private void sendEndData(int length) {
                byte[] int32 = SocketStreamUtils.getInt32(length);
                endArr[2] = int32[0];
                endArr[3] = int32[1];
                endArr[4] = int32[2];
                endArr[5] = int32[3];
                final DatagramPacket dp = new DatagramPacket(
                        endArr, endArr.length, group, port);
                try {
                        mss.send(dp);
                        Log.d(TAG, "mss.send(sendEndData);");
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }

        private void sendHeadData(int length) {
                byte[] int32 = SocketStreamUtils.getInt32(length);
                startArr[2] = int32[0];
                startArr[3] = int32[1];
                startArr[4] = int32[2];
                startArr[5] = int32[3];
                final DatagramPacket dp = new DatagramPacket(
                        startArr, startArr.length, group, port);
                try {
                        mss.send(dp);
                        Log.d(TAG, "mss.send(sendHeadData);");
                } catch (IOException e) {
                        e.printStackTrace();
                }
        }


        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
                Log.d(TAG, "onStartCommand: ");
                if (intent != null) {
                        int actionKey = intent.getIntExtra(ACTION_KEY, -1);
                        switch (actionKey) {
                                case 1:
                                        backgroundHandler.post(new Runnable() {
                                                @Override
                                                public void run() {
                                                        initCamera();
                                                }
                                        });
                                        break;
                                case 2:
                                        asyncCloseCamera();
                                        break;
                                default:
                                        break;
                        }
                }

                return super.onStartCommand(intent, flags, startId);
        }

        private void asyncCloseCamera() {
                backgroundHandler.removeCallbacksAndMessages(null);
                backgroundHandler.post(new Runnable() {
                        @Override
                        public void run() {
                                closeCamera();
                        }
                });
        }

        private void initImageReader() {
                mImageReader = ImageReader.newInstance(CameraConfig.WIDTH, CameraConfig.HEIGHT, ImageFormat.YV12, 3);

                mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() {
                        @Override
                        public void onImageAvailable(ImageReader reader) {
                                Log.d("initImageReader", "initImageReader");
                                Image image = reader.acquireLatestImage();
                                if (image == null) {
                                        return;
                                }
                                if (h264) {
                                        byte[] nv12;
                                        nv12 = ImageUtil.YUV_420_888(image);
                                        if (nv12 != null) {
                                                mEncode.encoderYUV420(nv12);
                                        }
                                } else {
                                        byte[] obj = ImageUtil.YUV_420_888toNV21(image);
                                        if (obj != null) {
                                                EventBus.getDefault().post(obj);
                                                udpHandler.removeMessages(1);
                                                udpHandler.obtainMessage(1, obj).sendToTarget();
                                        }
                                }
                                image.close();
                        }
                }, backgroundHandler);

                mEncode = new AndroidEncode(CameraConfig.WIDTH, CameraConfig.HEIGHT, CameraConfig.vbitrate, CameraConfig.framerate, new AndroidEncode.IEncoderListener() {
                        @Override
                        public void onH264(byte[] data) {
                                Log.d("initImageReader", "onH264:" + data.length + "---" + isFirst);
                                EventBus.getDefault().post(data);
                                if (!isFirst) {
                                        isFirst = true;
                                        arr = data;
                                }

                                if (h264) {
                                        udpHandler.removeMessages(1);
                                        udpHandler.obtainMessage(1, data).sendToTarget();
                                }
                        }
                });

        }

        boolean isFirst = false;

        byte[] arr;

        private synchronized void initCamera() {
                if (isOpen) {
                        return;
                }
                backgroundHandler.removeCallbacksAndMessages(null);
                CameraManager mCameraManager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
                if (mCameraManager == null) {
                        Log.d(TAG, "mCameraManager == null");
                        return;
                }
                String[] idList;
                try {
                        idList = mCameraManager.getCameraIdList();

                        Log.e(TAG, "initCamera: " + Arrays.toString(idList));
                } catch (CameraAccessException e) {
                        e.printStackTrace();
                        return;
                }
                if (idList.length == 0) {
                        return;
                }
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                        return;
                }
                try {
                        CameraCharacteristics cameraCharacteristics = mCameraManager.getCameraCharacteristics("0");
                        StreamConfigurationMap map = cameraCharacteristics.get(
                                CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                        Size[] outputSizes = map.getOutputSizes(ImageFormat.YUV_420_888);
                        for (Size outputSize : outputSizes) {
                                Log.d(TAG, "outputSize: " + outputSize.getWidth() + "---" + outputSize.getHeight());
                        }
                        Size min = Collections.min(Arrays.asList(outputSizes), new Comparator<Size>() {
                                @Override
                                public int compare(Size o1, Size o2) {
                                        return o1.getWidth() * o1.getHeight() - o2.getWidth() * o2.getHeight();
                                }
                        });
                        Log.d(TAG, "outputSize: " + min.getWidth());
                        mCameraManager.openCamera(idList[0], openStateCallback, backgroundHandler);
                        isOpen = true;
                } catch (CameraAccessException e) {
                        e.printStackTrace();
                        Log.d(TAG, "outputSize: CameraAccessException");
                }

        }


        CameraDevice.StateCallback openStateCallback = new CameraDevice.StateCallback() {

                @Override
                public void onOpened(@NonNull CameraDevice camera) {
                        createCameraPreviewSession(camera);
                }

                @Override
                public void onDisconnected(@NonNull CameraDevice camera) {
                        asyncCloseCamera();
                }

                @Override
                public void onError(@NonNull CameraDevice camera, int error) {
                        asyncCloseCamera();
                }
        };

        private void createCameraPreviewSession(CameraDevice camera) {
                try {
                        this.mCameraDevice = camera;
                        Surface surface = mImageReader.getSurface();
                        mPreviewRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                        mPreviewRequestBuilder.addTarget(surface);
                        camera.createCaptureSession(Collections.singletonList(surface), captureSessionCallback, backgroundHandler);
                } catch (CameraAccessException e) {
                        e.printStackTrace();
                }

        }

        private CameraCaptureSession mSession;


        CameraCaptureSession.StateCallback captureSessionCallback = new CameraCaptureSession.StateCallback() {

                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                        mSession = session;
                        try {
                                mPreviewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                                mPreviewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraMetadata.FLASH_MODE_TORCH);
                                session.setRepeatingRequest(mPreviewRequestBuilder.build(), null, backgroundHandler);
                        } catch (CameraAccessException e) {
                                e.printStackTrace();
                                Log.e("linc", "set preview builder failed." + e.getMessage());
                        }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                        asyncCloseCamera();
                }
        };

        private synchronized void closeCamera() {
                Log.d(TAG, "closeCamera");
                if (!isOpen) {
                        return;
                }
                if (mSession != null) {
                        try {
                                mSession.stopRepeating();
                        } catch (CameraAccessException e) {
                                e.printStackTrace();
                        }
                        mSession = null;
                }
                if (mCameraDevice != null) {
                        mCameraDevice.close();
                        mCameraDevice = null;
                }
                isOpen = false;
        }

        @Override
        public void onDestroy() {
                super.onDestroy();
                Log.d(TAG, "onDestroy: ");
                asyncCloseCamera();
                if (mss != null) {
                        try {
                                mss.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        mss = null;
                }

                mEncode.releaseMediaCodec();
                udpHandler.removeCallbacks(null);
        }

}
