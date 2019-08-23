package com.zmp.client;

import android.media.MediaFormat;

public class CameraConfig {

        public static int WIDTH = 640;

        public static int HEIGHT = 480;

        public static final String VCODEC = "video/avc";

        public static final int vbitrate = 1000000;

        public static final int framerate = 30;

        public static final int IFRAME_INTERVAL = 1;

        public static final boolean is3399 = true;

        private static final String H264_MIME = MediaFormat.MIMETYPE_VIDEO_AVC;

        private static final String H265_MIME = MediaFormat.MIMETYPE_VIDEO_HEVC;

        public static final String MINE_TYPE = H264_MIME;

}