package com.supermap.android.ontheway;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.app.Dialog;
import android.app.ListActivity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

/**
 * 班车路线类。用户可通过该类选择要进入的班车路线。
 *
 */
public class BusLineActivity extends ListActivity implements OnItemClickListener {
    private static String LOG_TAG = "com.supermap.android.ontheway.BusLineActivity";
    private ListView listView;
    private Button backBtn;
    private TextView currentTimeTextView;
    private ImageView joinImageView;
    private TextView userCountTextView;
    private static final int CONFIRM_DIALOG = 0;
    private UserNameSetDialog confirmDialog;
    public int position = -1;
    private String userName;
    private PreferencesService preferencesService;
    private List<Map<String, Object>> data;
    private int count;// 记录当前班车群的人数
    private Resources res;
    private int savedRouteId;
    private boolean isFirst = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        setContentView(R.layout.list);
        preferencesService = new PreferencesService(this);
        res = getResources();
        savedRouteId = preferencesService.getRouteInfo(CommonUtils.ROUTEINFO_FIEL);
        confirmDialog = new UserNameSetDialog(this, R.style.dialogTheme);
        Bundle bundle = this.getIntent().getExtras();
        if (bundle != null) {
            count = bundle.getInt("count");
            isFirst = true;
        }
        data = getData();
        MyListAdapter adapter = new MyListAdapter(this, data);
        setListAdapter(adapter);
        listView = this.getListView();
        listView.setOnItemClickListener(this);
        backBtn = (Button) findViewById(R.id.button_back);
        OnClickListener clickListener = new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (v.getId() == R.id.button_back) {
                    back();
                }
            }
        };
        backBtn.setOnClickListener(clickListener);
        currentTimeTextView = (TextView) findViewById(R.id.currentTimeTextView);
        currentTimeTextView.setText(getCurrentDate());
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (this.position != -1 && this.position != position) {
            // 发送退出的消息
            sendDeleteMessage();
            // 关闭资源
            RabbitmqUtils.closeResouce();
            if (userCountTextView != null) {
                if (count == 0) {
                    // 避免出现负数
                    userCountTextView.setText(String.valueOf(0));
                } else {
                    userCountTextView.setText(String.valueOf(count - 1));
                }
            }
        }
        if (joinImageView != null) {
            joinImageView.setVisibility(View.GONE);
        }

        if (isFirst) {
            isFirst = false;
            if (savedRouteId != position) {
                // 发送退出的消息
                sendDeleteMessage();
                // 关闭资源
                RabbitmqUtils.closeResouce();
            }

            View v = parent.getChildAt(savedRouteId);
            if (v != null) {
                TextView tv = (TextView) v.findViewById(R.id.busPeoSum);
                if (tv != null) {
                    if (count == 0) {
                        // 避免出现负数
                        tv.setText(String.valueOf(0));
                    } else {
                        tv.setText(String.valueOf(count - 1));
                    }
                }
                ImageView iv = (ImageView) v.findViewById(R.id.joinin);
                if (iv != null) {
                    iv.setVisibility(View.GONE);
                }
            }
        }

        userCountTextView = (TextView) view.findViewById(R.id.busPeoSum);
        if (!"".equals(userCountTextView.getText())) {
            int curCount = Integer.valueOf(userCountTextView.getText().toString());
            if (this.position != position) {
                userCountTextView.setText(String.valueOf(curCount + 1));
            }
        }

        joinImageView = (ImageView) view.findViewById(R.id.joinin);
        joinImageView.setVisibility(View.VISIBLE);
        this.position = position;
        userName = getUserName();
        preferencesService.saveRouteInfo(CommonUtils.ROUTEINFO_FIEL, this.position);
        if (StringUtils.isEmpty(userName)) {
            showDialog(CONFIRM_DIALOG);
        } else {
            Bundle bundle = new Bundle();
            bundle.putInt("position", this.position);
            // bundle.putExtra("textViewLabel", title);
            Intent intent = new Intent();
            intent.putExtras(bundle);
            intent.setClass(this, ShareLocationActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            this.startActivity(intent);
        }
    }

    /**
     * 发送删除用户信息
     */
    private void sendDeleteMessage() {
        String deviceID = CommonUtils.getDeviceUuid(this);
        CommonUtils.sendDeleteMessage(preferencesService, deviceID, this.position);
    }

    /**
     * 获取系统日期
     */
    private String getCurrentDate() {
        Calendar c = Calendar.getInstance();
        String time = c.get(Calendar.YEAR) + "-" + (c.get(Calendar.MONTH) + 1) + "-" + c.get(Calendar.DAY_OF_MONTH);
        return time;
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
        case CONFIRM_DIALOG:
            if (confirmDialog != null) {
                return confirmDialog;
            }
            break;
        default:
            break;
        }
        return super.onCreateDialog(id);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch (id) {
        case CONFIRM_DIALOG:
            if (confirmDialog != null) {
                Log.d(LOG_TAG, "confirmDialog onPrepareDialog!");
            }
            break;
        default:
            break;
        }
        super.onPrepareDialog(id, dialog);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.d(LOG_TAG, "BusLineActivity onNewIntent");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            count = bundle.getInt("count");
            if (userCountTextView != null) {
                userCountTextView.setText(String.valueOf(count));
            }
        }
    }

    /**
     * 获取用户名
     */
    private String getUserName() {
        String name = null;
        Map<String, String> params = preferencesService.getUserInfo(CommonUtils.USERINFO_FIEL);
        if (params != null) {
            name = params.get("userName");
        }
        return name;
    }

    @Override
    public void onBackPressed() {
        back();
    }

    /**
     * 返回上一步操作
     */
    private void back() {
        // 发送退出的消息
        sendDeleteMessage();
        // 关闭资源
        RabbitmqUtils.closeResouce();
        Intent intent = new Intent();
        intent.setClass(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
    }

    /**
     * 保存用户信息
     */
    public void saveUserName(String userName) {
        if (StringUtils.isEmpty(userName)) {
            return;
        }
        preferencesService.saveUserInfo(CommonUtils.USERINFO_FIEL, userName, "男");
    }

    /**
     * 获取list中的数据内容
     */
    private List<Map<String, Object>> getData() {
        List<Map<String, Object>> lists = new ArrayList<Map<String, Object>>();
        String[] lines_stops = res.getStringArray(R.array.lines_stops);
        for (int i = 0; i < lines_stops.length; i++) {
            String[] item = lines_stops[i].split("\\-");
            Map<String, Object> map = new HashMap<String, Object>();
            map.put("num", item[0]);
            map.put("busName", item[1]);
            map.put("line", item[0]);
            map.put("sum", "0");
            if (i == savedRouteId) {
                map.put("sum", String.valueOf(count));
            }
            lists.add(map);
        }
        return lists;
    }

}
