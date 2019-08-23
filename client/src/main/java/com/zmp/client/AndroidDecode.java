package com.zmp.client;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

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
                startVideoEncode();
        }

        private synchronized void startVideoEncode() {
                if (isStartDecode) {
                        return;
                }
                try {
                        if (vDeCodec == null) {
                                vDeCodec = MediaCodec.createDecoderByType(CameraConfig.MINE_TYPE);
                        }
                        MediaFormat format = MediaFormat.createVideoFormat(CameraConfig.MINE_TYPE, CameraConfig.WIDTH, CameraConfig.HEIGHT);
                        //                format.setInteger(MediaFormat.KEY_ROTATION, 90);
                        format.setInteger(MediaFormat.KEY_BIT_RATE, CameraConfig.vbitrate);
                        format.setInteger(MediaFormat.KEY_FRAME_RATE, CameraConfig.framerate);
                        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5);

                        vDeCodec.configure(format, null, null, 0);
                        //
                        vDeCodec.start();
                        isStartDecode = true;
                } catch (Exception e) {
                        e.printStackTrace();
                }
        }

        public synchronized void resetVideoEncode() {
                if (vDeCodec != null) {
                        try {
                                vDeCodec.stop();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        vDeCodec.reset();
                }
                isStartDecode = false;
        }


        public synchronized void releaseVideoEncode() {
                if (vDeCodec != null) {
                        try {
                                vDeCodec.stop();
                                vDeCodec.release();
                        } catch (Exception e) {
                                e.printStackTrace();
                        }
                        vDeCodec = null;
                }
        }


        public synchronized void onDecodeData(byte[] codeData) {
                Log.e("ggh1", "解码前");
                if (!isStartDecode) {
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
                                Log.e("ggh1", " info.size:" + info.size);
                                Log.e("ggh1", " info.flags:" + info.flags);
                                Log.e("ggh1", " info.offset:" + info.offset);
                                Log.e("ggh1", " info.presentationTimeUs:" + info.presentationTimeUs);
                                vDeCodec.releaseOutputBuffer(outputIndex, true);
                                Log.e("ggh1", "解码后");
                        }
                } catch (MediaCodec.CryptoException e) {
                        e.printStackTrace();
                        Log.e("ggh1", "解码后", e);
                } catch (Exception e) {
                        e.printStackTrace();
                        Log.e("ggh1", "解码后", e);
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
