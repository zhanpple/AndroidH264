# AndroidH264
AndroidH264

## 知识点
### 1. Camera2 + ImageReader获取摄像头预览数据
### 2. H264编码预览数据
### 3. udp发送预览数据(分包)
### 4. udp接受预览数据(合并包)
### 5. H264解码预览数据

## Camera2 + ImageReader获取摄像头预览数据
[Camera2FaceService](https://github.com/zhanpple/AndroidH264/blob/master/app/src/main/java/com/zmp/udptest/camera2/Camera2FaceService.java)
```java
    /**
    * 初始化ImageReader
    */
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



    /**
    * 初始化相机
    */

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

```

## H264编码预览数据
[AndroidEncode](https://github.com/zhanpple/AndroidH264/blob/master/app/src/main/java/com/zmp/udptest/AndroidEncode.java)
```java
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

```

## udp发送数据
[Camera2FaceService](https://github.com/zhanpple/AndroidH264/blob/master/app/src/main/java/com/zmp/udptest/camera2/Camera2FaceService.java)
```java
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
```

## udp接受数据
[MainActivity](https://github.com/zhanpple/AndroidH264/blob/master/client/src/main/java/com/zmp/client/MainActivity.java)
```
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

```

## H264解码
[AndroidDecode](https://github.com/zhanpple/AndroidH264/blob/master/client/src/main/java/com/zmp/client/AndroidDecode.java)
```java
 androidDecode = new AndroidDecode();
                androidDecode.setCallBack(data -> {
                        if (h264) {
                                circleImageView.setNv21(data, ImageUtil.OR_270);
                        }
                });
```

## 其他配置可参考 [CameraConfig](https://github.com/zhanpple/AndroidH264/blob/master/client/src/main/java/com/zmp/client/CameraConfig.java)

### 参考文章 https://www.cnblogs.com/renhui/p/7452572.html

## 有任何疑问或建议可随时联系邮箱: zhanpples@qq.com




