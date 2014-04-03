package com.supermap.android.ontheway;

/**
 * 定位信息类
 */
public class LocationMessage {
    public String name = "***";// 用户名
    public String sex;// 性别
    public String address;// 地理位置信息描述
    public double x;// 坐标x
    public double y;// 坐标y

    public LocationMessage(String name, String sex, String address, double x, double y) {
        super();
        this.name = name;
        this.sex = sex;
        this.address = address;
        this.x = x;
        this.y = y;
    }

    public LocationMessage() {
    }

}
