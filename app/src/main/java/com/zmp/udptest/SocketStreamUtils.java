package com.zmp.udptest;

/**
 * Created by Administrator on 2017/4/17.
 */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by Liyan on 2016/7/25.
 * Socket Byte 流工具类
 */
public class SocketStreamUtils {

        public static short readInt16(DataInputStream inputstream) throws IOException {
                byte[] byte_arr = new byte[2];
                inputstream.readFully(byte_arr);
                ByteBuffer buf = ByteBuffer.wrap(byte_arr);
                short result = buf.order(ByteOrder.LITTLE_ENDIAN).getShort();
                return result;
        }

        public static int readInt32(DataInputStream inputstream) throws IOException {
                byte[] byte_arr = new byte[4];
                inputstream.readFully(byte_arr);
                ByteBuffer buf = ByteBuffer.wrap(byte_arr);
                int result = buf.order(ByteOrder.LITTLE_ENDIAN).getInt();
                return result;
        }

        public static long readLong64(DataInputStream inputstream) throws IOException {
                byte[] byte_arr = new byte[8];
                inputstream.readFully(byte_arr);
                ByteBuffer buf = ByteBuffer.wrap(byte_arr);
                long result = buf.order(ByteOrder.LITTLE_ENDIAN).getLong();
                return result;
        }

        public static byte[] readBlob(DataInputStream inputStream, int length) throws IOException {
                byte[] content = new byte[length];
                inputStream.readFully(content);
                return content;
        }

        public static void writeInt32(DataOutputStream outputStream, int value) throws IOException {
                outputStream.write(getInt32(value));
        }

        public static byte[] getInt32(int value) {
                byte[] result = new byte[4];
                ByteBuffer buffer = ByteBuffer.wrap(result);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.asIntBuffer().put(value);
                return buffer.array();
        }

        public static void writeLong64(DataOutputStream outputStream, long value) throws IOException {
                byte[] result = new byte[8];
                ByteBuffer buffer = ByteBuffer.wrap(result);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.asLongBuffer().put(value);
                outputStream.write(buffer.array());
        }

        public static void writeInt16(DataOutputStream outputStream, short value) throws IOException {
                byte[] result = new byte[2];
                ByteBuffer buffer = ByteBuffer.wrap(result);
                buffer.order(ByteOrder.LITTLE_ENDIAN);
                buffer.asShortBuffer().put(value);
                outputStream.write(buffer.array());
        }

        public static void writeBlob(DataOutputStream outputStream, byte[] buf) throws IOException {
                outputStream.write(buf);
        }

}
