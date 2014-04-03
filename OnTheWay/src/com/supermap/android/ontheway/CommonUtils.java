package com.supermap.android.ontheway;

import java.io.UnsupportedEncodingException;
import java.util.UUID;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings.Secure;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

/**
 * 工具类。通过Android设备唯一标识获取用户详细信息及删除信息等。
 *
 */
public class CommonUtils {
    private static String LOG_TAG = "com.supermap.android.ontheway.CommonUtils";
    public static final String EXCHANGE_NAME = "myExchange";// 定义交换机名称，唯一
    public static final String USERINFO_FIEL = "userInfo";
    public static final String ROUTEINFO_FIEL = "routeInfo";
    public static final String MAPSTATUSINFO_FIEL = "mapStatusInfo";
    public static final String USERNAME = "userName";
    public static final String SEX = "sex";
    public static final String PREFS_FILE = "device_id.xml";
    public static final String PREFS_DEVICE_ID = "deviceId";

    public static ConnectionFactory getDefConnectionFactory() {
        return getConnectionFactory("42.121.15.217", 5672);
    }

    public static ConnectionFactory getConnectionFactory(String host, int port) {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("admin");
        factory.setPassword("123456");
        factory.setVirtualHost("my_mq");
        factory.setHost(host);
        factory.setPort(port);
        return factory;
    }

    /**
     *  Android 手机上获取物理唯一标识码
     *  为每个设备产生唯一的UUID，以ANDROID_ID为基础，在获取失败时以TelephonyManager.getDeviceId()为备选方法，如果再失败，使用UUID的生成策略。
     * @param context
     * @return
     */
    public static String getDeviceUuid(Context context) {
        String deviceUuid = null;
        if (context != null) {
            final SharedPreferences prefs = context.getSharedPreferences(PREFS_FILE, 0);
            final String id = prefs.getString(PREFS_DEVICE_ID, null);
            if (id != null) {
                // Use the ids previously computed and stored in the prefs file
                deviceUuid = id;
            } else {
                // 以ANDROID_ID为基础
                final String androidId = Secure.getString(context.getContentResolver(), Secure.ANDROID_ID);
                // Use the Android ID unless it's broken, in which case fallback on deviceId,unless it's not available, then fallback on a random number which we store to a prefs file
                UUID uuid = UUID.randomUUID();
                try {
                    // 在主流厂商生产的设备上，有一个很经常的bug，就是每个设备都会产生相同的ANDROID_ID：9774d56d682e549c
                    if (!"9774d56d682e549c".equals(androidId)) {
                        uuid = UUID.nameUUIDFromBytes(androidId.getBytes("utf8"));
                    } else {
                        // 以TelephonyManager.getDeviceId()为备选方法
                        final String deviceId = ((TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE)).getDeviceId();
                        // 最后使用UUID的生成策略
                        uuid = deviceId != null ? UUID.nameUUIDFromBytes(deviceId.getBytes("utf8")) : UUID.randomUUID();
                    }
                } catch (UnsupportedEncodingException e) {
                    throw new RuntimeException(e);
                }
                deviceUuid = uuid.toString();
                // Write the value out to the prefs file
                prefs.edit().putString(PREFS_DEVICE_ID, deviceUuid).commit();
            }
        } else {
            deviceUuid = UUID.randomUUID().toString();
        }
        Log.d(LOG_TAG, "deviceUuid:" + deviceUuid);
        return deviceUuid;
    }

    /**
     * 获取用户的详细信息
     * @param mesStr
     * @return
     */
    public static MyMessage getMyMessage(String mesStr) {
        MyMessage myMessage = null;
        if (mesStr != null && !"".equals(mesStr)) {
            String[] mesStrs = mesStr.split("\\|");
            if (mesStrs != null && mesStrs.length > 0) {
                myMessage = new MyMessage();
                for (int i = 0; i < mesStrs.length; i++) {
                    String s = mesStrs[i];
                    if (s == null || "".equals(s)) {
                        continue;
                    }
                    if (s.contains("id=")) {
                        String value = s.substring(s.indexOf("id=") + "id=".length());
                        if (value != null && !"".equals(value)) {
                            myMessage.id = value;
                        }
                    } else if (s.contains("type=")) {
                        String value = s.substring(s.indexOf("type=") + "type=".length());
                        if (value != null && !"".equals(value)) {
                            myMessage.type = value;
                        }
                    } else if (s.contains("name=")) {
                        String value = s.substring(s.indexOf("name=") + "name=".length());
                        if (value != null && !"".equals(value)) {
                            myMessage.locationMessage.name = value;
                        }
                    } else if (s.contains("address=")) {
                        String value = s.substring(s.indexOf("address=") + "address=".length());
                        myMessage.locationMessage.address = value;
                    } else if (s.contains("time=")) {
                        String value = s.substring(s.indexOf("time=") + "time=".length());
                        myMessage.time = Long.valueOf(value);
                    } else if (s.contains("y=")) {
                        String value = s.substring(s.indexOf("y=") + "y=".length());
                        if (value != null && !"".equals(value)) {
                            myMessage.locationMessage.y = Double.valueOf(value);
                        }
                    } else if (s.contains("sex=")) {
                        String value = s.substring(s.indexOf("sex=") + "sex=".length());
                        if (value != null && !"".equals(value)) {
                            myMessage.locationMessage.sex = value;
                        }
                    } else if (s.contains("x=")) {// 这个放在最后,因为"sex="也会进入这个分支
                        String value = s.substring(s.indexOf("x=") + "x=".length());
                        if (value != null && !"".equals(value)) {
                            myMessage.locationMessage.x = Double.valueOf(value);
                        }
                    }
                }
            }
        }
        return myMessage;
    }

    public static String getMessage(MyMessage myMessage) {
        String mesStr = null;
        if (myMessage != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("id=" + myMessage.id).append('|');
            sb.append("type=" + myMessage.type).append('|');
            sb.append("name=" + myMessage.locationMessage.name).append('|');
            sb.append("address=" + myMessage.locationMessage.address).append('|');
            sb.append("x=" + myMessage.locationMessage.x).append('|');
            sb.append("y=" + myMessage.locationMessage.y).append('|');
            sb.append("sex=" + myMessage.locationMessage.sex).append('|');
            sb.append("time=" + myMessage.time);
            mesStr = sb.toString();
        }
        return mesStr;
    }

    /**
     * 发送删除用户信息
     * @param preferencesService
     * @param deviceID  Android 手机上获取物理唯一标识码
     * @param routeId   班车路线编号
     */
    public static void sendDeleteMessage(PreferencesService preferencesService, String deviceID, int routeId) {
        try {
            ConnectionFactory factory = CommonUtils.getDefConnectionFactory();
            Connection conn = factory.newConnection();
            Channel channel = conn.createChannel();
            String userName = preferencesService.getUserInfo(CommonUtils.USERINFO_FIEL).get(CommonUtils.USERNAME);
            MyMessage myMessage = new MyMessage(deviceID, userName, "", "", 0.0, 0.0, "delete");
            String message = CommonUtils.getMessage(myMessage);
            byte[] messageBodyBytes = message.getBytes();
            // 发送“退出”的消息
            channel.basicPublish(CommonUtils.EXCHANGE_NAME, "r" + routeId, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBodyBytes);
            Log.d(LOG_TAG, "Delete Message Sented '" + message + "'");
            channel.close();
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
   
}
