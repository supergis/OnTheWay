package com.supermap.android.ontheway;

/**
 * 发布消息类
 */
public class MyMessage {
    public String id;// 唯一识别一个移动设备
    public String type = "add";// delete,add,update
    public LocationMessage locationMessage;// 位置信息类对象
    public long time;

    public MyMessage(String id, String name, String sex, String address, double x, double y, String type) {
        super();
        this.id = id;
        this.locationMessage = new LocationMessage(name, sex, address, x, y);
        this.type = type;
        this.time = System.currentTimeMillis();
    }

    public MyMessage(String id, String type, LocationMessage locationMessage) {
        super();
        this.id = id;
        this.type = type;
        this.locationMessage = locationMessage;
        this.time = System.currentTimeMillis();
    }

    public MyMessage() {
        this.locationMessage = new LocationMessage();
    }

}
