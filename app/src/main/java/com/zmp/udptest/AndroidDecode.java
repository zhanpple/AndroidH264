package com.zmp.udptest;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import com.zmp.udptest.camera2.ImageUtil;

import java.nio.ByteBuffer;


/**
 * @author zmp
 */
public class AndroidDecode {

        private static final String TAG = "AndroidDecode";


        private MediaCodec vDeCodec = null;

        MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();

        private boolean isStartDecode = false;


        public AndroidDecode() {
                initVideoEncode();
        }

        public boolean initVideoEncode() {
                MediaFormat format = MediaFormat.createVideoFormat(CameraConfig.MINE_TYPE, CameraConfig.WIDTH, CameraConfig.HEIGHT);
                //                format.setInteger(MediaFormat.KEY_ROTATION, 90);
                format.setInteger(MediaFormat.KEY_BIT_RATE, CameraConfig.vbitrate);
                format.setInteger(MediaFormat.KEY_FRAME_RATE, CameraConfig.framerate);
                format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                        MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);
                try {
                        // Get an instance of MediaCodec and give it its Mime type
                        vDeCodec = MediaCodec.createDecoderByType(CameraConfig.MINE_TYPE);
                        // Configure the codec
                        vDeCodec.configure(format, null, null, 0);
                        //
                        vDeCodec.start();
                } catch (Exception e) {
                        e.printStackTrace();
                }
                return true;
        }


        public void onDecodeData(byte[] codeData) {
                Log.e("ggh1", "解码前");
                if (vDeCodec == null) {
                        return;
                }
                try {
                        ByteBuffer[] inputBuffer = vDeCodec.getInputBuffers();
                        int inputIndex = vDeCodec.dequeueInputBuffer(0);

                        if (inputIndex >= 0) {
                                ByteBuffer buffer = inputBuffer[inputIndex];

                                try {
                                        buffer.put(codeData);
                                } catch (NullPointerException e) {
                                        e.printStackTrace();
                                }

                                vDeCodec.queueInputBuffer(inputIndex, 0, codeData.length, 0, 0);
                        }

                        int outputIndex = vDeCodec.dequeueOutputBuffer(info, 0);
                        if (outputIndex >= 0) {
                                Image outputImage = vDeCodec.getOutputImage(outputIndex);

                                if (outputImage != null) {
                                        int format = outputImage.getFormat();
                                        byte[] bytes;
                                        if (CameraConfig.is3399) {
                                                bytes = ImageUtil.YV12(outputImage);
                                        } else {
                                                bytes = ImageUtil.YUV_420_888toNV21(outputImage);
                                        }
                                        Log.e("ggh1", " info.size:" + format);
                                        Log.e("ggh1", " info.size:" + bytes.length);
                                        if (iCallBack != null) {
                                                iCallBack.onBack(bytes);
                                        }
                                        outputImage.close();
                                }
                                vDeCodec.releaseOutputBuffer(outputIndex, true);
                        }
                } catch (MediaCodec.CryptoException e) {
                        e.printStackTrace();
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public void releaseMediaCodec() {
                if (vDeCodec != null) {
                        vDeCodec.stop();
                        vDeCodec.release();
                        vDeCodec = null;
                }
        }

        public void setCallBack(ICallBack iCallBack) {
                this.iCallBack = iCallBack;
        }

        private ICallBack iCallBack;

        interface ICallBack {

                void onBack(byte[] data);

        }

}
