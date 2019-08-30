package com.zmp.client;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.Socket;
import java.util.Arrays;

/**
 * @author zmp
 *
 * UDP发送组播消息  TCP连接
 */
public class MainActivity3 extends AppCompatActivity {

        private MulticastSocket ds;

        private String multicastHost = "224.0.0.1";

        private final int port = 8005;

        private final int port2 = 8006;

        private MyBaseFrameView circleImageView;

        private Handler handler;

        private final String TAG = "MainActivity";

        private AndroidDecode androidDecode;

        private InetAddress group;


        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);
                Log.e(TAG, "onCreate: joinGroup");
                circleImageView = (MyBaseFrameView) findViewById(R.id.face_sv);
                initHandler();
                if (PermissionUtil.setActivePermissions(this)) {
                        handler.sendEmptyMessage(0);
                        Log.e(TAG, "sendEmptyMessage: joinGroup");
                }
                Log.e(TAG, "handleMessage: joinGroup1");

        }

        byte[] startArr = {0x0a, 0x0a, 0x00, 0x00, 0x00, 0x00};

        byte[] openArr = {0x0c, 0x0c, 0x0c, 0x0c, 0x0c, 0x0c};

        private void initHandler() {
                androidDecode = new AndroidDecode();
                androidDecode.setCallBack(data -> {
                        circleImageView.setNv21(data, ImageUtil.OR_270);
                });
                HandlerThread handlerThread = new HandlerThread("MainAct");
                handlerThread.start();
                handler = new Handler(handlerThread.getLooper()) {
                        @Override
                        public void handleMessage(Message msg) {
                                switch (msg.what) {
                                        case 0:

                                                Log.e(TAG, "handleMessage: joinGroup");
                                                try {
                                                        group = InetAddress.getByName(multicastHost);
                                                        ds = new MulticastSocket(port);
                                                        ds.joinGroup(group);
                                                        sendEmptyMessage(1);
                                                        Log.e(TAG, "joinGroup: success");
                                                } catch (IOException e) {
                                                        e.printStackTrace();
                                                        removeMessages(0);
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                }
                                                break;
                                        case 1:
                                                byte[] buffers = new byte[6];
                                                DatagramPacket dp = new DatagramPacket(buffers, buffers.length);
                                                Log.e(TAG, "handleMessage: receive");
                                                try {
                                                        ds.receive(dp);
                                                        byte[] data = dp.getData();
                                                        if (!checkDatum(data, dp.getLength(), dp.getAddress())) {
                                                                sendEmptyMessage(1);
                                                        }
                                                } catch (IOException e) {
                                                        e.printStackTrace();
                                                        removeMessages(0);
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                        return;
                                                }
                                                break;
                                        default:
                                }
                        }
                };
        }

        private boolean checkDatum(byte[] data, int len, InetAddress address) {
                boolean checked = false;
                Log.e(TAG, "checkDatum: " + Arrays.toString(data));
                if (len == openArr.length) {
                        checked = true;
                        for (int i = 0; i < len; i++) {
                                if (openArr[i] != data[i]) {
                                        checked = false;
                                        break;
                                }
                        }
                }
                if (checked) {
                        initSocket(address);
                }
                return checked;
        }

        private void initSocket(InetAddress address) {
                ThreadPoolProxyFactory.getNormalThreadPoolProxy().execute(() -> {
                        try {
                                Socket socket = new Socket(address, port2);
                                DataInputStream dis = new DataInputStream(socket.getInputStream());
                                while (!isDestroyed()) {
                                        byte[] byteHead = new byte[2];
                                        dis.readFully(byteHead);
                                        if (byteHead[0] == startArr[0] && byteHead[1] == startArr[1]) {
                                                int int32 = SocketStreamUtils.readInt32(dis);
                                                Log.e(TAG, "readFully: " + int32);
                                                byte[] bytes = new byte[int32];
                                                dis.readFully(bytes);
                                                decodeBytes(bytes);
                                        } else {
                                                Log.e(TAG, "readFully: " + byteHead[0] + "-- " + byteHead[1]);
                                        }
                                }
                        } catch (
                                IOException e) {
                                e.printStackTrace();
                        }
                });
        }

        private void decodeBytes(byte[] bytes) {
                androidDecode.onDecodeData(bytes);
        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                if (requestCode == PermissionUtil.MY_PERMISSION_REQUEST_CODE) {
                        if (PermissionUtil.checkPermissionResult(grantResults)) {
                                handler.sendEmptyMessage(0);
                        }
                }
        }

        @Override
        protected void onDestroy() {
                super.onDestroy();
                if (ds != null) {
                        try {
                                ds.close();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        ds = null;
                }
                androidDecode.releaseVideoEncode();
                handler.removeCallbacksAndMessages(null);
        }

}
