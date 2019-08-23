package com.zmp.client;

import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.Image;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * Created by zmp on 2017/3/31.
 */
public final class ImageUtil {

        public static final int OR_0 = 0;

        public static final int OR_90 = 1;

        public static final int OR_180 = 2;

        public static final int OR_270 = 3;

        public static final int OR_DEFAULT = 3;

        public static byte[] imageToByteArray(Image image) {
                byte[] data = null;
                if (image == null) {
                        return null;
                }
                if (image.getFormat() == ImageFormat.JPEG) {
                        Image.Plane[] planes = image.getPlanes();
                        ByteBuffer buffer = planes[0].getBuffer();
                        data = new byte[buffer.capacity()];
                        buffer.get(data);
                        return data;
                }
                else if (image.getFormat() == ImageFormat.YUV_420_888) {
                        data = NV21toJPEG(
                                YUV_420_888toNV21(image),
                                image.getWidth(), image.getHeight(), OR_DEFAULT);
                }
                return data;
        }

        public static byte[] YUV_420_888toNV21(Image image) {
                byte[] nv21;
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();

                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();
                byte[] u = new byte[uSize];
                byte[] v = new byte[vSize];
                uBuffer.get(u);
                vBuffer.get(v);
                nv21 = new byte[ySize + uSize + vSize];
                //U and V are swapped
                yBuffer.get(nv21, 0, ySize);
                for (int i = 0; i < vSize; i++) {
                        nv21[ySize + (i << 1)] = v[i];
                        nv21[ySize + (i << 1) + 1] = u[i];
                }
                return nv21;
        }


        public static byte[] YV12(Image image) {
                byte[] nv12;
                ByteBuffer yBuffer = image.getPlanes()[0].getBuffer();
                ByteBuffer uBuffer = image.getPlanes()[1].getBuffer();
                ByteBuffer vBuffer = image.getPlanes()[2].getBuffer();
                int ySize = yBuffer.remaining();
                int uSize = uBuffer.remaining();
                int vSize = vBuffer.remaining();
                nv12 = new byte[ySize + uSize + vSize];
                yBuffer.get(nv12, 0, ySize);
                vBuffer.get(nv12, ySize, uSize);
                uBuffer.get(nv12, ySize + uSize, vSize);
                return nv12;
        }

        public static byte[] NV21toJPEG(byte[] nv21, int width, int height, int o) {
                byte[] bytes = nv21;
                int w = width;
                int h = height;
                switch (o) {
                        case 0:
                                break;
                        case 1:
                                w = height;
                                h = width;
                                bytes = nv21Rotate90(nv21, width, height);
                                break;
                        case 2:
                                bytes = nv21Rotate180(nv21, width, height);
                                break;
                        case 3:
                                w = height;
                                h = width;
                                bytes = nv21Rotate270(nv21, width, height);
                                break;
                        default:
                                break;
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                YuvImage yuv = new YuvImage(bytes, ImageFormat.NV21, w, h, null);
                yuv.compressToJpeg(new Rect(0, 0, w, h), 100, out);
                return out.toByteArray();
        }

        public static void writeFrame(String fileName, byte[] data) {
                try {
                        BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(fileName));
                        bos.write(data);
                        bos.flush();
                        bos.close();
                }
                catch (IOException e) {
                        e.printStackTrace();
                }
        }

        public static byte[] nv21Rotate270(byte[] src, int srcWidth, int height) {
                byte[] dst = new byte[src.length];
                int wh = srcWidth * height;
                int uvHeight = height >> 1;//uvHeight = height / 2

                //旋转Y
                int k = 0;
                for (int i = 0; i < srcWidth; i++) {
                        int nPos = srcWidth - 1;
                        for (int j = 0; j < height; j++) {
                                dst[k] = src[nPos - i];
                                k++;
                                nPos += srcWidth;
                        }
                }

                for (int i = 0; i < srcWidth; i += 2) {
                        int nPos = wh + srcWidth - 1;
                        for (int j = 0; j < uvHeight; j++) {
                                dst[k] = src[nPos - i - 1];
                                dst[k + 1] = src[nPos - i];
                                k += 2;
                                nPos += srcWidth;
                        }
                }

                return dst;
        }

        private static byte[] nv21Rotate90(byte[] src, int width, int height) {
                byte[] des = new byte[src.length];

                int wh = width * height;
                int uvHeight = height >> 1;
                //旋转Y
                int k = 0;
                for (int i = 0; i < width; i++) {
                        int nPos = width * (height - 1);
                        for (int j = 0; j < height; j++) {
                                des[k] = src[nPos + i];
                                k++;
                                nPos -= width;
                        }
                }

                k = wh * 3 / 2 - 1;
                for (int x = width - 1; x > 0; x = x - 2) {
                        int nPos = 0;
                        for (int j = 0; j < uvHeight; j++) {
                                des[k] = src[wh + nPos + x];
                                k--;
                                des[k] = src[wh + nPos + (x - 1)];
                                k--;
                                nPos += width;
                        }
                }

                return des;
        }


        private static byte[] nv21Rotate180(byte[] data, int width, int height) {
                byte[] des = new byte[data.length];
                int i;
                int k = 0;
                int wh = width * height;
                int uvHeight = height >> 1;
                for (i = wh - 1; i >= 0; i--) {
                        des[k] = data[i];
                        k++;
                }

                for (i = wh * 3 / 2 - 1; i >= wh; i -= 2) {
                        des[k++] = data[i - 1];
                        des[k++] = data[i];
                }
                return des;
        }


        //NV21: YYYY VUVU

        byte[] NV21_mirror(byte[] nv21_data, int width, int height) {
                int i;
                int left, right;
                byte temp;
                int startPos = 0;

                // mirror Y
                for (i = 0; i < height; i++) {
                        left = startPos;
                        right = startPos + width - 1;
                        while (left < right) {
                                temp = nv21_data[left];
                                nv21_data[left] = nv21_data[right];
                                nv21_data[right] = temp;
                                left++;
                                right--;
                        }
                        startPos += width;
                }


                // mirror U and V
                int offset = width * height;
                startPos = 0;
                for (i = 0; i < height / 2; i++) {
                        left = offset + startPos;
                        right = offset + startPos + width - 2;
                        while (left < right) {
                                temp = nv21_data[left];
                                nv21_data[left] = nv21_data[right];
                                nv21_data[right] = temp;
                                left++;
                                right--;

                                temp = nv21_data[left];
                                nv21_data[left] = nv21_data[right];
                                nv21_data[right] = temp;
                                left++;
                                right--;
                        }
                        startPos += width;
                }
                return nv21_data;
        }

}