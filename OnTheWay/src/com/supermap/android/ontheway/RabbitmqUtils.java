package com.supermap.android.ontheway;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;
import com.rabbitmq.client.QueueingConsumer;
import com.supermap.android.maps.BoundingBox;
import com.supermap.android.maps.DefaultItemizedOverlay;
import com.supermap.android.maps.ItemizedOverlay;
import com.supermap.android.maps.OverlayItem;
import com.supermap.android.maps.Point2D;
import com.supermap.android.maps.Util;

/**
 * 分频道订阅信息工具类。
 * 
 */
public class RabbitmqUtils {
    private static String LOG_TAG = "com.supermap.android.ontheway.RabbitmqUtils";
    private static final boolean durable = false;// 消息队列持久化
    private Map<String, LocationMessage> messageMap = new HashMap<String, LocationMessage>();
    private ShareLocationActivity shareLocationActivity;
    private DefaultItemizedOverlay overlay;
    private String deviceID;
    private PreferencesService preferencesService;
    private static int subscriberOneTime = 0;
    private static SubscribThread subscribThread;
    private Handler myHandler = null;
    private List<Point2D> list = new ArrayList<Point2D>();
    private int currentSize;
    private Drawable man;
    private Drawable woman;
    private Drawable mylocation;
    private long currentTime;

    public RabbitmqUtils(ShareLocationActivity shareLocationActivity) {
        this.shareLocationActivity = shareLocationActivity;
        mylocation = shareLocationActivity.getResources().getDrawable(R.drawable.my_location);
        woman = shareLocationActivity.getResources().getDrawable(R.drawable.woman_location);
        man = shareLocationActivity.getResources().getDrawable(R.drawable.man_location);
        overlay = new DefaultItemizedOverlay(man);
        overlay.setOnClickListener(new MyOnClickListener());
        if (!shareLocationActivity.mapView.getOverlays().contains(overlay)) {
            shareLocationActivity.mapView.getOverlays().add(overlay);
        }
        preferencesService = new PreferencesService(shareLocationActivity);
        LocationUtil.initLocationClient(shareLocationActivity, new MyBDLocationListener());
        deviceID = CommonUtils.getDeviceUuid(shareLocationActivity);
        myHandler = new Handler() {
            public void handleMessage(Message msg) {
                switch (msg.what) {
                case 1:
                    show(msg.obj);
                    break;
                default:
                    break;
                }
            };
        };
    }

    public void startRabbitmq() {
        Publisher();
    }

    /**
     * 发布消息
     */
    private void Publisher() {
        // 定位并构造MyMessage对象
        if (!LocationUtil.isStarted()) {
            LocationUtil.startLoction();
        }
        subscriberOneTime = 0;
        currentTime = System.currentTimeMillis();
        messageMap.clear();
        list.clear();
        currentSize = 0;
        overlay.clear();
        shareLocationActivity.mapView.postInvalidate();
    }

    /**
     * 订阅消息
     */
    private void Subscriber() {
        if (subscribThread == null) {
            subscribThread = new SubscribThread();
            subscribThread.start();
        }
    }

    /**
     *订阅消息的线程，实现分频道订阅消息
     *
     */
    class SubscribThread extends Thread {
        @Override
        public void run() {
            try {
                ConnectionFactory factory = CommonUtils.getDefConnectionFactory();
                Connection conn = factory.newConnection();
                Channel channel = conn.createChannel();
                channel.exchangeDeclare(CommonUtils.EXCHANGE_NAME, "direct", durable);
                String routeKey = "r" + shareLocationActivity.routeId;
                String queueName = routeKey + "_" + deviceID;
                channel.queueDeclare(queueName, durable, false, false, null);
                channel.queueBind(queueName, CommonUtils.EXCHANGE_NAME, routeKey);
                Log.d(LOG_TAG, " [" + queueName + "] Waiting for messages.");
                boolean noAck = false;
                QueueingConsumer consumer = new QueueingConsumer(channel);
                channel.basicConsume(queueName, noAck, consumer);
                while (!Thread.currentThread().isInterrupted()) {
                    QueueingConsumer.Delivery delivery;
                    try {
                        delivery = consumer.nextDelivery();
                    } catch (InterruptedException ie) {
                        break;
                    }
                    String mesStr = new String(delivery.getBody());
                    Log.d(LOG_TAG, routeKey + " Message received" + mesStr);
                    MyMessage mes = CommonUtils.getMyMessage(mesStr);
                    doMessage(mes);
                    Log.d(LOG_TAG, "getDeliveryTag:" + delivery.getEnvelope().getDeliveryTag());
                    channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                }
                Log.d(LOG_TAG, "线程 " + Thread.currentThread().getName() + " 已停止,跳出死循环了");
                channel.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * MyMessage解析，根据信息判断的和id做出不同的消息处理
     */
    private void doMessage(MyMessage mes) {
        if (mes == null) {
            return;
        }
        if ((currentTime - mes.time) >= 30000) {
            return;

        }
        String type = mes.type;
        currentSize = messageMap.size();
        if ("add".equalsIgnoreCase(type)) {// 要么就是添加一条记录，要么就是更新数据
            messageMap.put("r" + shareLocationActivity.routeId + "_" + mes.id, mes.locationMessage);
        } else if ("delete".equalsIgnoreCase(type)) {// 客户端退出，发送消息给所有客户端删除该人的记录
            if (shareLocationActivity.routeIds != null && shareLocationActivity.routeIds.size() > 0) {
                Iterator it = shareLocationActivity.routeIds.iterator();
                while (it.hasNext()) {
                    int id = (Integer) it.next();
                    messageMap.remove("r" + id + "_" + mes.id);
                }
            } else {
                messageMap.remove("r" + shareLocationActivity.routeId + "_" + mes.id);
            }
        }

        if (myHandler != null) {
            Message msg = new Message();
            msg.what = 1;
            msg.obj = mes.locationMessage.name;
            myHandler.sendMessage(msg);
        }
    }

    /**
     * 实时打印用户进入或退出线路的消息
     */
    private void show(Object obj) {
        if (obj instanceof String) {
            String name = (String) obj;
            if (currentSize < messageMap.size()) {
                String[] item = shareLocationActivity.lines_stops[shareLocationActivity.routeId].split("\\-");
                String routeStr = item[0].substring(0, item[0].indexOf("号"));
                Toast.makeText(shareLocationActivity, name + " 加入" + routeStr + "号班车群.", Toast.LENGTH_SHORT).show();
            } else if (currentSize > messageMap.size()) {
                String[] item = shareLocationActivity.lines_stops[shareLocationActivity.routeId].split("\\-");
                String routeStr = item[0].substring(0, item[0].indexOf("号"));
                Toast.makeText(shareLocationActivity, name + " 退出" + routeStr + "号班车群.", Toast.LENGTH_SHORT).show();
            }
        }
        try {
            if (messageMap.size() > 0) {
                overlay.clear();
                Set<Entry<String, LocationMessage>> entrySet = messageMap.entrySet();
                Iterator<Entry<String, LocationMessage>> it = entrySet.iterator();
                list.clear();
                while (it.hasNext()) {
                    Entry<String, LocationMessage> entry = it.next();
                    String key = entry.getKey();
                    String[] myId = key.split("\\_");
                    Log.d(LOG_TAG, "key" + myId[1]);
                    Log.d(LOG_TAG, "deviceID" + deviceID);
                    // 保证key是以"r"+id+"_"开头
                    if (key.lastIndexOf("r" + shareLocationActivity.routeId + "_") != 0) {
                        continue;
                    }
                    LocationMessage locationMessage = entry.getValue();
                    // 经纬度转换成墨卡托
                    Point2D gp = Util.lonLat2Mercator(locationMessage.x, locationMessage.y);
                    list.add(gp);
                    String title = locationMessage.name;
                    OverlayItem overlayItem = new OverlayItem(gp, title, locationMessage.address);
                    Drawable drawable = null;
                    if (myId[1].equals(deviceID)) {
                        drawable = mylocation;
                    }

                    if (!myId[1].equals(deviceID) && locationMessage.sex.equals("女")) {
                        drawable = woman;

                    }
                    overlayItem.setMarker(drawable);
                    overlay.addItem(overlayItem);
                }
                // 重新onDraw一次,可以没有
                shareLocationActivity.mapView.postInvalidate();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(shareLocationActivity, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 飞跃到可见所有overlay的比例尺级别
     */
    public void showAllOverlay() {
        if (list.size() > 0) {
            BoundingBox bb = getBoundingBox(list);
            int level = -1;
            Point2D centerP = null;
            if (bb != null) {
                centerP = bb.getCenter();
                level = shareLocationActivity.mapView.getProjection().calculateZoomLevel(bb);
            } else {
                centerP = list.get(0);
            }
            if (centerP != null) {
                shareLocationActivity.mapView.getController().animateTo(centerP);
            }
            if (level != -1) {
                shareLocationActivity.mapView.getController().setZoom(level);
            }
            // 重新onDraw一次
            shareLocationActivity.mapView.postInvalidate();
        }
    }

    private BoundingBox getBoundingBox(List<Point2D> list) {
        BoundingBox bb = null;
        if (list != null && list.size() > 1) {
            double l = list.get(0).x;
            double t = list.get(0).y;
            double r = list.get(0).x;
            double b = list.get(0).y;
            for (int i = 1; i < list.size(); i++) {
                Point2D p = list.get(i);
                if (p.x < l) {
                    l = p.x;
                }
                if (p.x > r) {
                    r = p.x;
                }
                if (p.y < t) {
                    t = p.y;
                }
                if (p.y > b) {
                    b = p.y;
                }
            }
            bb = new BoundingBox(new Point2D(l, t), new Point2D(r, b));
        }
        return bb;
    }

    class MyOnClickListener implements ItemizedOverlay.OnClickListener {

        @Override
        public void onClicked(ItemizedOverlay overlay, OverlayItem item) {
            // 弹出气泡展示消息
            shareLocationActivity.showPopupWindow(item);
        }
    }

    /**
     * 百度定位监听器
     */
    class MyBDLocationListener implements BDLocationListener {
        @Override
        public void onReceiveLocation(BDLocation location) {
            if (location == null)
                return;
            if (subscriberOneTime < 1) {
                Subscriber();
                subscriberOneTime++;
            }
            StringBuilder sb = new StringBuilder();
            if (location.getLocType() == BDLocation.TypeGpsLocation) {
                sb.append("gps定位");
            } else if (location.getLocType() == BDLocation.TypeNetWorkLocation) {
                sb.append(location.getAddrStr());
            } else if (location.getLocType() == BDLocation.TypeOffLineLocation) {
                sb.append("离线定位");
            }

            // sb.append(location.getLocType());
            Map<String, String> params = preferencesService.getUserInfo(CommonUtils.USERINFO_FIEL);
            MyMessage myMessage = new MyMessage(deviceID, params.get(CommonUtils.USERNAME), params.get(CommonUtils.SEX), sb.toString(),
                    location.getLongitude(), location.getLatitude(), "add");
            // 一开始就展示自己的定位
            doMessage(myMessage);

            String routeKey = "r" + shareLocationActivity.routeId;
            try {
                Thread.sleep(100);
                ConnectionFactory factory = CommonUtils.getDefConnectionFactory();
                Connection conn = factory.newConnection();
                Channel channel = conn.createChannel();
                String message = CommonUtils.getMessage(myMessage);
                byte[] messageBodyBytes = message.getBytes();
                channel.basicPublish(CommonUtils.EXCHANGE_NAME, routeKey, MessageProperties.PERSISTENT_TEXT_PLAIN, messageBodyBytes);
                Log.d(LOG_TAG, routeKey + " Message Sented '" + message + "'");
                channel.close();
                conn.close();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
            }
        }

        public void onReceivePoi(BDLocation location) {

        }
    }

    /**
     * 获取用户人数
     */
    public int getUserCount() {
        return messageMap.size();
    }

    /**
     * 关闭资源
     */
    public static void closeResouce() {
        if (subscribThread != null) {
            // 保证退出run中的死循环，这样才可以停止线程
            subscribThread.interrupt();
            subscribThread = null;
        }
        subscriberOneTime = 0;
        LocationUtil.stopLoction();
    }

}
