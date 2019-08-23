package com.zmp.udptest;

import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.zmp.udptest.camera2.Camera2FaceService;
import com.zmp.udptest.camera2.ImageUtil;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class MainActivity extends AppCompatActivity {

        private AndroidDecode androidHradwareDecode;

        private MyBaseFrameView viewById;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                setContentView(R.layout.activity_main);
                EventBus.getDefault().register(this);
                viewById = findViewById(R.id.cIV);
                if (PermissionUtil.setActivePermissions(this)) {
                        startService(new Intent(this, Camera2FaceService.class)
                                .putExtra(Camera2FaceService.ACTION_KEY, Camera2FaceService.ACTION_OPEN_CAMERA));
                }
                androidHradwareDecode = new AndroidDecode();
                androidHradwareDecode.setCallBack(new AndroidDecode.ICallBack() {
                        @Override
                        public void onBack(byte[] data) {
                                viewById.setNv21(data, ImageUtil.OR_0);
                        }
                });
        }

        @Subscribe(threadMode = ThreadMode.BACKGROUND)
        public void onEvent(byte[] bytes) {
                androidHradwareDecode.onDecodeData(bytes);
//                viewById.setNv21(bytes, ImageUtil.OR_270);

        }

        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                if (requestCode == PermissionUtil.MY_PERMISSION_REQUEST_CODE) {
                        if (PermissionUtil.checkPermissionResult(grantResults)) {
                                startService(new Intent(this, Camera2FaceService.class)
                                        .putExtra(Camera2FaceService.ACTION_KEY, Camera2FaceService.ACTION_OPEN_CAMERA));
                        }
                }
        }


        @Override
        protected void onDestroy() {
                super.onDestroy();
                EventBus.getDefault().unregister(this);
                androidHradwareDecode.releaseMediaCodec();
//                startService(new Intent(this, Camera2FaceService.class)
//                        .putExtra(Camera2FaceService.ACTION_KEY, Camera2FaceService.ACTION_CLOSE_CAMERA));
                stopService(new Intent(this,Camera2FaceService.class));
        }

}
