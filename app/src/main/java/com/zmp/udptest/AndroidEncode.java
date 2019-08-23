package com.zmp.udptest;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;

import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * android 自带硬编码
 * @author zmp
 */

public class AndroidEncode {

        private MediaCodec codec = null;

        private int videoW;

        private int videoH;

        private int videoBitrate;

        private int videoFrameRate;

        private static final String TAG = "Encode";

        private IEncoderListener encoderListener;

        private boolean isStart;

        public AndroidEncode(int videoW, int videoH, int videoBitrate, int videoFrameRate, IEncoderListener encoderListener) {
                this.videoW = videoW;
                this.videoH = videoH;
                this.videoBitrate = videoBitrate;
                this.videoFrameRate = videoFrameRate;
                this.encoderListener = encoderListener;
                getMediaCodecList();
                startMediaCodec();
        }

        public void getMediaCodecList() {
                //获取解码器列表
                int numCodecs = MediaCodecList.getCodecCount();
                MediaCodecInfo codecInfo = null;
                for (int i = 0; i < numCodecs && codecInfo == null; i++) {
                        MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
                        if (!info.isEncoder()) {
                                continue;
                        }
                        String[] types = info.getSupportedTypes();
                        boolean found = false;
                        //轮训所要的解码器
                        for (int j = 0; j < types.length && !found; j++) {
                                if ("video/avc".equals(types[j])) {
                                        System.out.println("found");
                                        found = true;
                                }
                        }
                        if (!found) {
                                continue;
                        }
                        codecInfo = info;
                }
                Log.d(TAG, "found" + codecInfo.getName() + "supporting" + " video/avc");


                //检查所支持的colorspace
                int colorFormat = 0;
                MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
                System.out.println("length-" + capabilities.colorFormats.length + "==" + Arrays.toString(capabilities.colorFormats));
                for (int i = 0; i < capabilities.colorFormats.length && colorFormat == 0; i++) {
                        int format = capabilities.colorFormats[i];
                        switch (format) {
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                                        System.out.println("COLOR_FormatYUV420Planar");
                                        break;
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                                        System.out.println("COLOR_FormatYUV420PackedPlanar");
                                        break;
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                                        System.out.println("COLOR_FormatYUV420SemiPlanar");
                                        break;
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                                        System.out.println("COLOR_FormatYUV420PackedSemiPlanar");
                                        break;
                                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible:
                                        colorFormat = format;
                                        System.out.println("COLOR_TI_FormatYUV420PackedSemiPlanar");
                                        break;
                                default:
                                        Log.d(TAG, "Skipping unsupported color format " + format);
                                        break;
                        }
                }
                Log.d(TAG, "color format " + colorFormat);
        }

        public synchronized void startMediaCodec() {
                try {
                        if (codec == null) {
                                codec = MediaCodec.createEncoderByType(CameraConfig.MINE_TYPE);
                        }
                        MediaFormat format = MediaFormat.createVideoFormat(CameraConfig.MINE_TYPE, videoW, videoH);
                        format.setInteger(MediaFormat.KEY_BIT_RATE, videoBitrate);
                        format.setInteger(MediaFormat.KEY_FRAME_RATE, videoFrameRate);
                        format.setInteger(MediaFormat.KEY_COLOR_FORMAT,
                                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
                        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, CameraConfig.IFRAME_INTERVAL);
                        codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
                        codec.start();
                        isStart = true;
                } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "startMediaCodec: ", e);
                }
        }

        public synchronized void encoderYUV420(byte[] nv12) {
                if (!isStart || codec == null) {
                        return;
                }
                //                byte[] nv12 = new byte[input.length];
                ////                NV21ToNV12(input, nv12, CameraConfig.WIDTH, CameraConfig.HEIGHT); //nv12转nv21
                //                nv12 = nv21ToI420(input, CameraConfig.WIDTH, CameraConfig.HEIGHT);
                try {
                        int inputBufferIndex = codec.dequeueInputBuffer(-1);
                        if (inputBufferIndex >= 0) {
                                ByteBuffer inputBuffer = codec.getInputBuffer(inputBufferIndex);
                                inputBuffer.clear();
                                inputBuffer.put(nv12);
                                codec.queueInputBuffer(inputBufferIndex, 0, nv12.length, System.currentTimeMillis(), 0);
                        }

                        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                        int outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
                        while (outputBufferIndex >= 0) {
                                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferIndex);
                                byte[] outData = new byte[outputBuffer.remaining()];
                                outputBuffer.get(outData, 0, outData.length);
                                if (encoderListener != null) {
                                        encoderListener.onH264(outData);
                                }
                                codec.releaseOutputBuffer(outputBufferIndex, false);
                                outputBufferIndex = codec.dequeueOutputBuffer(bufferInfo, 0);
                        }
                } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "encoderYUV420: ", e);
                }
        }


        public byte[] nv21ToI420(byte[] data, int width, int height) {
                byte[] ret = new byte[data.length];
                int total = width * height;

                ByteBuffer bufferY = ByteBuffer.wrap(ret, 0, total);
                ByteBuffer bufferU = ByteBuffer.wrap(ret, total, total / 4);
                ByteBuffer bufferV = ByteBuffer.wrap(ret, total + total / 4, total / 4);

                bufferY.put(data, 0, total);
                for (int i = total; i < data.length; i += 2) {
                        bufferV.put(data[i]);
                        bufferU.put(data[i + 1]);
                }

                return ret;
        }

        public synchronized void releaseMediaCodec() {
                if (codec != null) {
                        codec.stop();
                        codec.release();
                        codec = null;
                }
        }


        public synchronized void resetMediaCodec() {
                if (codec != null) {
                        codec.stop();
                        codec.reset();
                }
                isStart = false;
        }

        public interface IEncoderListener {

                void onH264(byte[] data);

        }

        private byte[] rotateYUVDegree90(byte[] data, int imageWidth, int imageHeight) {
                byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
                // Rotate the Y luma
                int i = 0;
                for (int x = 0; x < imageWidth; x++) {
                        for (int y = imageHeight - 1; y >= 0; y--) {
                                yuv[i] = data[y * imageWidth + x];
                                i++;
                        }
                }
                // Rotate the U and V color components
                i = imageWidth * imageHeight * 3 / 2 - 1;
                for (int x = imageWidth - 1; x > 0; x = x - 2) {
                        for (int y = 0; y < imageHeight / 2; y++) {
                                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + x];
                                i--;
                                yuv[i] = data[(imageWidth * imageHeight) + (y * imageWidth) + (x - 1)];
                                i--;
                        }
                }
                return yuv;
        }

        private byte[] rotateYUVDegree270AndMirror(byte[] data, int imageWidth, int imageHeight) {
                byte[] yuv = new byte[imageWidth * imageHeight * 3 / 2];
                // Rotate and mirror the Y luma
                int i = 0;
                int maxY = 0;
                for (int x = imageWidth - 1; x >= 0; x--) {
                        maxY = imageWidth * (imageHeight - 1) + x * 2;
                        for (int y = 0; y < imageHeight; y++) {
                                yuv[i] = data[maxY - (y * imageWidth + x)];
                                i++;
                        }
                }
                // Rotate and mirror the U and V color components
                int uvSize = imageWidth * imageHeight;
                i = uvSize;
                int maxUV = 0;
                for (int x = imageWidth - 1; x > 0; x = x - 2) {
                        maxUV = imageWidth * (imageHeight / 2 - 1) + x * 2 + uvSize;
                        for (int y = 0; y < imageHeight / 2; y++) {
                                yuv[i] = data[maxUV - 2 - (y * imageWidth + x - 1)];
                                i++;
                                yuv[i] = data[maxUV - (y * imageWidth + x)];
                                i++;
                        }
                }
                return yuv;
        }

        /**
         * nv21 转nv12
         *
         * @param nv21
         * @param nv12
         * @param width
         * @param height
         */
        private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
                if (nv21 == null || nv12 == null) {
                        return;
                }
                int framesize = width * height;
                int i = 0, j = 0;
                System.arraycopy(nv21, 0, nv12, 0, framesize);
                for (i = 0; i < framesize; i++) {
                        nv12[i] = nv21[i];
                }
                for (j = 0; j < framesize / 2; j += 2) {
                        nv12[framesize + j - 1] = nv21[j + framesize];
                }
                for (j = 0; j < framesize / 2; j += 2) {
                        nv12[framesize + j] = nv21[j + framesize - 1];
                }
        }


        public static byte[] rotateYUV420Degree90(byte[] input, int width, int height, int rotation) {
                int frameSize = width * height;
                int qFrameSize = frameSize / 4;
                byte[] output = new byte[frameSize + 2 * qFrameSize];


                boolean swap = (rotation == 90 || rotation == 270);
                boolean yflip = (rotation == 90 || rotation == 180);
                boolean xflip = (rotation == 270 || rotation == 180);
                for (int x = 0; x < width; x++) {
                        for (int y = 0; y < height; y++) {
                                int xo = x, yo = y;
                                int w = width, h = height;
                                int xi = xo, yi = yo;
                                if (swap) {
                                        xi = w * yo / h;
                                        yi = h * xo / w;
                                }
                                if (yflip) {
                                        yi = h - yi - 1;
                                }
                                if (xflip) {
                                        xi = w - xi - 1;
                                }
                                output[w * yo + xo] = input[w * yi + xi];
                                int fs = w * h;
                                int qs = (fs >> 2);
                                xi = (xi >> 1);
                                yi = (yi >> 1);
                                xo = (xo >> 1);
                                yo = (yo >> 1);
                                w = (w >> 1);
                                h = (h >> 1);
                                // adjust for interleave here
                                int ui = fs + (w * yi + xi) * 2;
                                int uo = fs + (w * yo + xo) * 2;
                                // and here
                                int vi = ui + 1;
                                int vo = uo + 1;
                                output[uo] = input[ui];
                                output[vo] = input[vi];
                        }
                }
                return output;
        }


}
