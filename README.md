# AndroidH264
AndroidH264

## 知识点
### 1. Camera2 + ImageReader获取摄像头预览数据
### 2. H264编码预览数据
### 3. udp发送预览数据(分包)
### 4. udp接受预览数据(合并包)
### 5. H264解码预览数据

##Camera2 + ImageReader获取摄像头预览数据

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

##H264编码预览数据
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


