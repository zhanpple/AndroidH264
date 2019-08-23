package com.zmp.client;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

public class PermissionUtil {

        private static final String TAG = "PermissionUtil";

        private static final String[] permissions = new String[]{
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_NETWORK_STATE,
                Manifest.permission.ACCESS_WIFI_STATE,
                Manifest.permission.CAMERA,
                Manifest.permission.CHANGE_WIFI_STATE,
                Manifest.permission.DISABLE_KEYGUARD,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WAKE_LOCK,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        };

        public static final int MY_PERMISSION_REQUEST_CODE = 10000;

        public static boolean setActivePermissions(Activity activity) {
                boolean isAllGranted = checkPermissionAllGranted(activity, permissions);
                if (isAllGranted) {
                        return true;
                }
                // 一次请求多个权限, 如果其他有权限是已经授予的将会自动忽略掉
                ActivityCompat.requestPermissions(activity, permissions, MY_PERMISSION_REQUEST_CODE);
                return false;
        }

        /**
         * 检查是否拥有指定的所有权限
         */
        public static boolean checkPermissionAllGranted(Activity activity, String[] permissions) {
                for (String permission : permissions) {
                        if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                                // 只要有一个权限没有被授予, 则直接返回 false
                                return false;
                        }
                }
                return true;
        }

        public static boolean checkPermissionResult(int[] grantResults) {
                boolean isAllGranted = true;
                // 判断是否所有的权限都已经授予了
                for (int grant : grantResults) {
                        if (grant != PackageManager.PERMISSION_GRANTED) {
                                isAllGranted = false;
                                break;
                        }
                }
                if (isAllGranted) {
                        // 如果所有的权限都授予了, 则执行代码
                        Log.d(TAG, "onRequestPermissionsResult: 权限全部授权成功！");
                } else {
                        // 弹出对话框告诉用户需要权限的原因, 并引导用户去应用权限管理中手动打开权限按钮

                        Log.d(TAG, "onRequestPermissionsResult: 有未获得的权限");
                }
                return isAllGranted;
        }

}
