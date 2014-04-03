package com.supermap.android.ontheway;

import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import android.content.Context;

/**
 * 通过gps或wifi定位到当前位置，默认wifi定位优先，并返回当前的地理坐标
 * 
 */
public class LocationUtil {
    private static Context context;
    private static LocationClient mLocationClient;
    private static LocationClientOption option;
    private static boolean isStarted = false;

    public static void initLocationClient(Context con, BDLocationListener listener) {
        if (con == null || listener == null) {
            return;
        }
        context = con;
        mLocationClient = new LocationClient(context);
        if (option == null) {
            option = initDefLocationClientOption();
        }
        mLocationClient.setLocOption(option);
        mLocationClient.registerLocationListener(listener);
    }

    public static LocationClient getLocationClient() {
        return mLocationClient;
    }

    public static LocationClientOption initDefLocationClientOption() {
        LocationClientOption option = new LocationClientOption();
        option.setOpenGps(true); // 打开gps
        option.setCoorType("gcj02"); // 设置坐标类型为gcj02
        option.setPriority(LocationClientOption.GpsFirst); // 设置gps优先
        option.setProdName("SuperMapLoction"); // 设置产品线名称
        option.setScanSpan(30000); // 定时定位，每隔30秒钟定位一次
        option.setAddrType("all");
        return option;
    }

    public static void setOption(boolean isOpenGps, CoorType coorType, Priority priority, int scanSpanTime) {
        option = new LocationClientOption();
        option.setOpenGps(isOpenGps); // 是否打开gps
        if (CoorType.bd0911.equals(coorType)) {
            option.setCoorType(coorType.toString()); // 设置坐标类型为bd09ll
        } else if (CoorType.bd09.equals(coorType)) {
            option.setCoorType(coorType.toString());
        } else if (CoorType.gcj02.equals(coorType)) {
            option.setCoorType(coorType.toString());
        } else if (CoorType.gps.equals(coorType)) {
            option.setCoorType(coorType.toString());
        }
        if (Priority.NetWorkFirst.equals(priority)) {
            option.setPriority(2); // 设置网络优先
        } else if (Priority.GpsFirst.equals(priority)) {
            option.setPriority(1); // 设置Gps优先
        } else if (Priority.MIN_SCAN_SPAN.equals(priority)) {
            option.setPriority(1000); // 设置最短的扫描时间优先
        }
        option.setProdName("SuperMapLoction"); // 设置产品线名称
        option.setScanSpan(scanSpanTime * 1000); // 定时定位，每隔scanSpanTime秒钟定位一次。
    }

    public static void stopLoction() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop();
            isStarted = false;
        }
    }

    public static void disposeLoction() {
        if (mLocationClient != null && mLocationClient.isStarted()) {
            mLocationClient.stop();
            isStarted = false;
            mLocationClient = null;
        }
    }

    public static void startLoction() {
        if (mLocationClient != null && !mLocationClient.isStarted()) {
            mLocationClient.start();
            isStarted = true;
        }
    }

    /**
     * 坐标系类型
     * 
     */
    public static enum CoorType {
        gcj02, bd09, bd0911, gps
    }

    /**
     * 使用何种方式定位的优先级设置
     *  
     */
    public static enum Priority {
        NetWorkFirst, GpsFirst, MIN_SCAN_SPAN
    }

    public static boolean isStarted() {
        return isStarted;
    }

}
