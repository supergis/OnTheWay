package com.supermap.android.ontheway;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.supermap.android.maps.CloudLayerView;
import com.supermap.android.maps.MapView;
import com.supermap.android.maps.OverlayItem;
import com.supermap.android.maps.Point2D;

/**
 * 位置共享信息类。进入同一个班车路线的人可实现位置共享。
 *
 */
public class ShareLocationActivity extends Activity {
    private static String LOG_TAG = "com.supermap.android.ontheway.ShareLocationActivity";
    private CloudLayerView cloudLayerView;
    protected MapView mapView;
    private TextView titleNameTextView;
    private Button setBtn;
    private Button backBtn;
    public int routeId;
    public Set routeIds = new HashSet();
    private PreferencesService preferencesService;
    private RabbitmqUtils trmq;
    private PopWindow popWindow;
    private Button overViewBtn;
    private Resources res;
    protected String[] lines_stops;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        res = getResources();
        LayoutInflater inflater = getLayoutInflater();
        View popView = inflater.inflate(R.layout.share_location, null);
        setContentView(popView);
        popWindow = new PopWindow(popView);
        preferencesService = new PreferencesService(this);
        titleNameTextView = (TextView) findViewById(R.id.titleName);
        lines_stops = res.getStringArray(R.array.lines_stops);
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            routeId = bundle.getInt("position");
            String[] item = lines_stops[routeId].split("\\-");
            String routeStr = item[0].substring(0, item[0].indexOf("号"));
            titleNameTextView.setText(routeStr + "号班车群");
            routeIds.add(routeId);
        }

        // 创建地图窗口
        mapView = (MapView) this.findViewById(R.id.mapview);
        cloudLayerView = new CloudLayerView(this);
        mapView.addLayer(cloudLayerView);
        // 增加监听器
        mapView.addMapViewEventListener(new MapViewEventAdapter());
        mapView.setBuiltInZoomControls(true);
        // 设置地图视图的中心点和比例尺,如果文件中保存了地图状态，则从文件中读取
        Map<String, String> params = preferencesService.getMapStatus(CommonUtils.MAPSTATUSINFO_FIEL);
        if (params != null && Integer.parseInt(params.get("level")) >= 0 && !"".equals(params.get("x")) && !"".equals(params.get("y"))) {
            mapView.getController().setCenter(new Point2D(Double.parseDouble(params.get("x")), Double.parseDouble(params.get("y"))));
            mapView.getController().setZoom(Integer.parseInt(params.get("level")));
        } else {
            mapView.getController().setCenter(new Point2D(12957797.029368, 4864377.117917));
            mapView.getController().setZoom(12);
        }

        setBtn = (Button) findViewById(R.id.button_set);
        backBtn = (Button) findViewById(R.id.button_back);
        overViewBtn = (Button) findViewById(R.id.button_overview);
        overViewBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                trmq.showAllOverlay();

            }
        });

        OnClickListener clickListener = new OnClickListener() {
            public void onClick(View v) {
                CommonButtonProcess(v);
            }
        };
        setBtn.setOnClickListener(clickListener);
        backBtn.setOnClickListener(clickListener);

        trmq = new RabbitmqUtils(this);
        trmq.startRabbitmq();
    }

    void CommonButtonProcess(View v) {
        if (v.getId() == R.id.button_back) {
            back();
        } else if (v.getId() == R.id.button_set) {
            Bundle bundle = new Bundle();
            if (routeIds != null && routeIds.size() > 0) {
                int[] ids = new int[routeIds.size()];
                Iterator it = routeIds.iterator();
                int i = 0;
                while (it.hasNext()) {
                    ids[i] = (Integer) it.next();
                    i++;
                }
                bundle.putIntArray("routeIds", ids);
            } else {
                bundle.putIntArray("routeIds", new int[] { routeId });
            }
            Intent intent = new Intent(this, SetActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            intent.putExtras(bundle);
            startActivity(intent);
        }
    };

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(LOG_TAG, "ShareLocationActivity onNewIntent");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            int position = bundle.getInt("position");
            if (routeId != position) {
                trmq.startRabbitmq();
            }
            routeId = position;
            String[] item = lines_stops[position].split("\\-");
            String routeStr = item[0].substring(0, item[0].indexOf("号"));
            titleNameTextView.setText(routeStr + "号班车群");
            routeIds.add(routeId);
        }
    }

    private void back() {
        // 如果弹出窗口还在,退出前关闭
        popWindow.closePopupWindow();
        // 保存地图状态，包括地图当前的中心点以及比例尺级别
        preferencesService.saveMapStatus(CommonUtils.MAPSTATUSINFO_FIEL, mapView.getCenter().x, mapView.getCenter().y, mapView.getZoomLevel());
        Bundle bundle = new Bundle();
        bundle.putInt("count", trmq.getUserCount());
        Intent intent = new Intent();
        intent.putExtras(bundle);
        intent.setClass(this, BusLineActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(intent);
    }

    @Override
    public void onBackPressed() {
        back();
    }

    /**
     * 弹出气泡窗口
     */
    public void showPopupWindow(OverlayItem item) {
        popWindow.showPopupWindow(mapView, item);
    }

    /**
     * 更新气泡窗口
     */
    public void updatePopupWindow() {
        popWindow.updatePopupWindow(mapView);
    }

    /**
     * 给MapView添加监听事件
     * */
    class MapViewEventAdapter implements MapView.MapViewEventListener {

        @Override
        public void longTouch(MapView paramMapView) {
            popWindow.closePopupWindow();
        }

        @Override
        public void mapLoaded(MapView paramMapView) {
        }

        @Override
        public void move(MapView paramMapView) {
            updatePopupWindow();
        }

        @Override
        public void moveEnd(MapView paramMapView) {
            updatePopupWindow();
        }

        @Override
        public void moveStart(MapView paramMapView) {
            updatePopupWindow();
        }

        @Override
        public void touch(MapView paramMapView) {
        }

        @Override
        public void zoomEnd(MapView paramMapView) {
            updatePopupWindow();
        }

        @Override
        public void zoomStart(MapView paramMapView) {
        }
    }
}
