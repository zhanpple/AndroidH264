package com.zmp.client;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * @author zmp
 */
public class MainActivity extends AppCompatActivity {

        private MulticastSocket ds;

        private String multicastHost = "224.0.0.1";

        private final int port = 8005;

        private MyBaseFrameView circleImageView;

        private Handler handler;

        private final String TAG = "MainActivity";

        private int index = -1;

        private AndroidDecode androidHradwareDecode;

        private InetAddress group;

        private boolean h264 = true;


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

        byte[] endArr = {0x0b, 0x0b, 0x00, 0x00, 0x00, 0x00};

        byte[] openArr = {0x0c, 0x0c, 0x0c, 0x0c, 0x0c, 0x0c};

        private void initHandler() {
                androidHradwareDecode = new AndroidDecode();
                androidHradwareDecode.setCallBack(data -> {
                        if (h264) {
                                circleImageView.setNv21(data, ImageUtil.OR_270);
                        }
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
                                                        if (h264) {
                                                                sendEmptyMessageDelayed(1, 1000);
                                                                sendEmptyMessageDelayed(2, 1000);
                                                        } else {
                                                                sendEmptyMessageDelayed(1, 1000);
                                                        }
                                                        Log.e(TAG, "joinGroup: success");
                                                } catch (IOException e) {
                                                        e.printStackTrace();
                                                        removeMessages(0);
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                }
                                                break;
                                        case 1:
                                                byte[] buffers = new byte[1024];
                                                DatagramPacket dp = new DatagramPacket(buffers, buffers.length);
                                                Log.e(TAG, "handleMessage: receive");
                                                try {
                                                        ds.receive(dp);
                                                        byte[] data = dp.getData();
                                                        mergeDatum(data, dp.getLength());
                                                } catch (IOException e) {
                                                        e.printStackTrace();
                                                        removeMessages(0);
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                        return;
                                                }
                                                sendEmptyMessage(1);
                                                break;

                                        case 2:
                                                final DatagramPacket dp2 = new DatagramPacket(
                                                        openArr, openArr.length, group, port);
                                                try {
                                                        ds.send(dp2);
                                                } catch (IOException e) {
                                                        e.printStackTrace();
                                                        removeMessages(0);
                                                        sendEmptyMessageDelayed(0, 10 * 1000);
                                                }
                                                break;
                                        default:
                                }
                        }
                };
        }

        byte[] dataArr;

        private void mergeDatum(byte[] data, int len) {
                Log.e(TAG, "mergeDatum: " + len);
                if (len == startArr.length) {
                        if (checkIsOpen(data)) {
                                return;
                        }

                        if (data[0] == startArr[0] && data[1] == startArr[1]) {
                                byte[] bytes = Arrays.copyOfRange(data, 2, 6);
                                ByteBuffer buf = ByteBuffer.wrap(bytes);
                                int length = buf.order(ByteOrder.LITTLE_ENDIAN).getInt();
                                dataArr = new byte[length];
                                index = 0;
                                Log.e(TAG, "mergeStartDatumAll: " + length);
                                return;
                        }

                        if (data[0] == endArr[0] && data[1] == endArr[1]) {
                                byte[] bytes = Arrays.copyOfRange(data, 2, 6);
                                ByteBuffer buf = ByteBuffer.wrap(bytes);
                                int length = buf.order(ByteOrder.LITTLE_ENDIAN).getInt();
                                Log.e(TAG, "mergeEndDatumAll: " + length);
                                if (dataArr == null) {
                                        index = -1;
                                        return;
                                }
                                if (dataArr.length == index && dataArr.length == length) {
                                        if (h264) {
                                                androidHradwareDecode.onDecodeData(dataArr);
                                        } else {
                                                circleImageView.setNv21(dataArr, ImageUtil.OR_0);
                                        }
                                }
                                index = -1;
                                return;
                        }
                }
                if (dataArr == null) {
                        return;
                }
                if (index != -1) {
                        for (int i = 0; i < len; i++) {
                                if (index >= dataArr.length) {
                                        Log.e(TAG, "mergeEndDatumAll:" + len + "-- - " + dataArr.length);
                                        index = -1;
                                        return;
                                }
                                dataArr[index++] = data[i];
                        }
                }
        }

        private boolean checkIsOpen(byte[] data) {
                if (data.length == openArr.length) {
                        for (int i = 0; i < openArr.length; i++) {
                                if (data[i] != openArr[i]) {
                                        return false;
                                }
                        }
                        return true;
                }
                return false;
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
                androidHradwareDecode.releaseVideoEncode();
                handler.removeCallbacksAndMessages(null);
        }

}
