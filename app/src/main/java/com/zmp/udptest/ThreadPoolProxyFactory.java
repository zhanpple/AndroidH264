package com.zmp.udptest;


public class ThreadPoolProxyFactory {

        static ThreadPoolProxy mNormalThreadPoolProxy;

        static ThreadPoolProxy mDownLoadThreadPoolProxy;

        public static ThreadPoolProxy getNormalThreadPoolProxy() {
                if (mNormalThreadPoolProxy == null) {
                        synchronized (ThreadPoolProxyFactory.class) {
                                if (mNormalThreadPoolProxy == null) {
                                        mNormalThreadPoolProxy = new ThreadPoolProxy(5, 5);
                                }
                        }
                }
                return mNormalThreadPoolProxy;
        }


}
