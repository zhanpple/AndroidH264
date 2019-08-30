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
import java.io.OutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

/**
 * @author zmp
 * <p>
 * Camera2 实现后台无界面预览数据
 * 数据接收EventBus {@link Camera2FaceService3#initImageReader}
 * EventBus.getDefault().post(bytes)
 *
 *
 * UDP发送组播消息  TCP连接
 */
public class Camera2FaceService3 extends Service {

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

        private final int PORT = 8005;

        private final int PORT2 = 8006;

        private AndroidEncode mEncode;

        private final List<Socket> clients = Collections.synchronizedList(new ArrayList<Socket>());

        private ServerSocket mServerSocket;

        private boolean isCreated;

        private Handler tcpHandler;

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
                return null;
        }


        @Override
        public void onCreate() {
                super.onCreate();
                Log.d(TAG, "onCreate: ");
                isCreated = true;
                HandlerThread handlerThread = new HandlerThread(TAG);
                handlerThread.start();
                backgroundHandler = new Handler(handlerThread.getLooper());
                initUdp();
                initTcp();
                initImageReader();
        }

        private void initTcp() {
                HandlerThread tcpThread = new HandlerThread(TAG + TAG + TAG);
                tcpThread.start();
                tcpHandler = new Handler(tcpThread.getLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                                if (msg.what == 0) {
                                        try {
                                                mServerSocket = new ServerSocket(PORT2);
                                                while (isCreated) {
                                                        final Socket clientSocket = mServerSocket.accept();
                                                        handleSocket(clientSocket);
                                                }
                                        } catch (IOException e) {
                                                e.printStackTrace();
                                                sendEmptyMessageDelayed(0, 5 * 1000);
                                        }
                                }
                        }
                };
                tcpHandler.sendEmptyMessage(0);
        }

        private synchronized void handleSocket(Socket clientSocket) {
                synchronized (clients) {
                        clients.add(clientSocket);
                        sendH264AllClients(arr);
                }
        }

        public final synchronized void sendH264AllClients(final byte[] h264) {
                synchronized (clients) {
                        if (h264 == null || h264.length == 0 || clients.isEmpty()) {
                                return;
                        }
                }
                ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(new Runnable() {
                        @Override
                        public void run() {
                                synchronized (clients) {
                                        Iterator<Socket> socketIterator = clients.iterator();
                                        while (socketIterator.hasNext()) {
                                                Socket client = socketIterator.next();
                                                if (client == null) {
                                                        break;
                                                }
                                                OutputStream outputStream;
                                                try {
                                                        outputStream = client.getOutputStream();
                                                        byte[] h264Data = getH264Data(h264);
                                                        outputStream.write(h264Data);
                                                        outputStream.flush();
                                                } catch (IOException e) {
                                                        try {
                                                                client.close();
                                                        } catch (IOException e1) {
                                                                e1.printStackTrace();
                                                        }
                                                        socketIterator.remove();
                                                        e.printStackTrace();
                                                }

                                        }
                                }
                        }
                });
        }


        private void initUdp() {
                HandlerThread udpThread = new HandlerThread(TAG + TAG);
                udpThread.start();
                udpHandler = new Handler(udpThread.getLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                                switch (msg.what) {
                                        case 0:
                                                try {
                                                        group = InetAddress.getByName(multicastHost);
                                                        mss = new MulticastSocket(PORT);
                                                        mss.joinGroup(group);
                                                        sendEmptyMessage(1);
                                                        Log.e(TAG, "joinGroup: success");
                                                } catch (Exception e1) {
                                                        e1.printStackTrace();
                                                        mss = null;
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                }
                                                break;
                                        case 1:
                                                if (mss == null) {
                                                        break;
                                                }
                                                try {
                                                        final DatagramPacket dp = new DatagramPacket(
                                                                openArr, openArr.length, group, PORT);
                                                        mss.send(dp);
                                                        sendEmptyMessageDelayed(1, 2 * 1000);
                                                } catch (IOException e) {
                                                        e.printStackTrace();
                                                        mss = null;
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                }
                                                break;

                                        default:
                                }
                        }
                };
                udpHandler.sendEmptyMessage(0);
        }


        byte[] startArr = {0x0a, 0x0a, 0x00, 0x00, 0x00, 0x00};

        byte[] openArr = {0x0c, 0x0c, 0x0c, 0x0c, 0x0c, 0x0c};

        private byte[] getH264Data(byte[] buffer) {
                byte[] bytes = new byte[buffer.length + startArr.length];
                byte[] int32 = SocketStreamUtils.getInt32(buffer.length);
                startArr[2] = int32[0];
                startArr[3] = int32[1];
                startArr[4] = int32[2];
                startArr[5] = int32[3];
                System.arraycopy(startArr, 0, bytes, 0, startArr.length);
                System.arraycopy(buffer, 0, bytes, startArr.length, buffer.length);
                return bytes;
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
                                byte[] nv12 = ImageUtil.YUV_420_888(image);
                                if (nv12 != null) {
                                        mEncode.encoderYUV420(nv12);
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
                                sendH264AllClients(data);
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
                isCreated = false;
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
                ThreadPoolProxyFactory.getNormalThreadPoolProxy().shutDown();
                udpHandler.removeCallbacks(null);
                tcpHandler.removeCallbacks(null);

        }

}
