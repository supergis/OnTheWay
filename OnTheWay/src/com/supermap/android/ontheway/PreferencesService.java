package com.supermap.android.ontheway;

import java.util.HashMap;
import java.util.Map;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;

public class PreferencesService {
    private Context context;

    public PreferencesService(Context context) {
        super();
        this.context = context;
    }

    /**
     * 将用户名跟性别等存储到指定文件中
     * @param fileName 指定的文件
     * @param userName  用户昵称
     * @param sex 用户性别
     */
    public void saveUserInfo(String fileName, String userName, String sex) {
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString("userName", userName);
        editor.putString("sex", sex);
        editor.commit();
    }

    /**
     * 从指定的文件中获取用户昵称及性别信息
     * @param fileName 指定的文件
     * @return Map对象，包含用户名、性别
     */
    public Map<String, String> getUserInfo(String fileName) {
        Map<String, String> params = new HashMap<String, String>();
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        params.put("userName", preferences.getString("userName", ""));
        params.put("sex", preferences.getString("sex", ""));

        return params;
    }

    /**
     * 保存退出前进入过的班车号
     * @param fileName 指定保存文件
     * @param routeId  班车号
     */
    public void saveRouteInfo(String fileName, int routeId) {
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putInt("routeId", routeId);
        editor.commit();
    }

    /**
     * 从指定的文件中获取班车号
     * @param fileName 指定文件
     * @return 班车号
     */
    public int getRouteInfo(String fileName) {
        int routeId;
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        routeId = preferences.getInt("routeId", -1);
        return routeId;
    }

    /**
     * 从指定的文件中获取地图状态
     * @param fileName 指定文件
     * @param x 地图中心点的x坐标
     * @param y 地图中心点的y坐标
     * @param level 地图的比例尺级别
     */
    public void saveMapStatus(String fileName, double x, double y, int level) {
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        Editor editor = preferences.edit();
        editor.putString("x", String.valueOf(x));
        editor.putString("y", String.valueOf(y));
        editor.putInt("level", level);
        editor.commit();
    }

    /**
     * @param fileName 指定文件
     * @return Map对象 包括地图的中心点坐标及比例尺级别
     */
    public Map<String, String> getMapStatus(String fileName) {
        Map<String, String> params = new HashMap<String, String>();
        SharedPreferences preferences = context.getSharedPreferences(fileName, Context.MODE_PRIVATE);
        params.put("x", preferences.getString("x", ""));
        params.put("y", preferences.getString("y", ""));
        params.put("level", String.valueOf(preferences.getInt("level", -1)));

        return params;

    }

}
