package com.supermap.android.ontheway;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

/**
 * 用户设置个人信息类。用户可通过该类修改自己的用户名及性别信息等
 *
 */
public class SetActivity extends Activity {
	private static String LOG_TAG = "com.supermap.android.ontheway.SetActivity";
	private Button backBtn;
	private Button quitShareBtn;
	private EditText nameEditText;
	private EditText sexEditText;
	private String nameEditTextValue;
	private String sexEditTextValue;
	private PreferencesService preferencesService;
	private int[] routeIds;
	private String deviceID;
	private TextView nameTextView;
	private static final int QUIT_DIALOG = 0;
	private QuitDialog quitDialog;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.set);
		nameTextView = (TextView) findViewById(R.id.nameTextView);
		setFocuse();
		preferencesService = new PreferencesService(this);
		quitDialog = new QuitDialog(this, R.style.dialogTheme);
		Bundle bundle = this.getIntent().getExtras();
		if (bundle != null) {
			routeIds = bundle.getIntArray("routeIds");
		}
		deviceID = CommonUtils.getDeviceUuid(this);
		nameEditText = (EditText) findViewById(R.id.nameEditText);
		nameEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (!StringUtils.isEmpty(s)) {
					nameEditTextValue = s.toString();
					preferencesService.saveUserInfo(CommonUtils.USERINFO_FIEL,
							nameEditTextValue, sexEditTextValue);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		sexEditText = (EditText) findViewById(R.id.sexEditText);
		sexEditText.addTextChangedListener(new TextWatcher() {
			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				if (!StringUtils.isEmpty(s)) {
					sexEditTextValue = s.toString();
					preferencesService.saveUserInfo(CommonUtils.USERINFO_FIEL,
							nameEditTextValue, sexEditTextValue);
				}
			}

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
			}

			@Override
			public void afterTextChanged(Editable s) {
			}
		});
		setUserInfo();

		quitShareBtn = (Button) findViewById(R.id.button_quitShare);
		backBtn = (Button) findViewById(R.id.button_back);

		OnClickListener clickListener = new OnClickListener() {
			public void onClick(View v) {
				CommonButtonProcess(v);
			}

		};
		quitShareBtn.setOnClickListener(clickListener);
		backBtn.setOnClickListener(clickListener);

	}

	/**
	 * 设置用户名、性别
	 */
	private void setUserInfo() {
		Map<String, String> params = preferencesService
				.getUserInfo(CommonUtils.USERINFO_FIEL);
		if (params != null) {
			if (nameEditText != null) {
				nameEditTextValue = params.get("userName");
				nameEditText.setText(params.get("userName"));
			}
			if (sexEditText != null) {
				sexEditTextValue = params.get("sex");
				sexEditText.setText(params.get("sex"));
			}
		}
	}

	void CommonButtonProcess(View v) {
		if (v.getId() == R.id.button_back) {
			Intent intent = new Intent();
			intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
			intent.setClass(this, ShareLocationActivity.class);
			startActivity(intent);
		} else if (v.getId() == R.id.button_quitShare) {
			showDialog(QUIT_DIALOG);

		}
	};

	/**
	 * 发送删除消息
	 */
	public void sendDeleteMessage() {
		try {
			if (routeIds == null || routeIds.length <= 0) {
				return;
			}
			// Thread.sleep(100);
			ConnectionFactory factory = CommonUtils.getDefConnectionFactory();
			Connection conn = factory.newConnection();
			Channel channel = conn.createChannel();
			String userName = preferencesService.getUserInfo(
					CommonUtils.USERINFO_FIEL).get(CommonUtils.USERNAME);
			MyMessage myMessage = new MyMessage(deviceID, userName, "", "",
					0.0, 0.0, "delete");
			String message = CommonUtils.getMessage(myMessage);
			byte[] messageBodyBytes = message.getBytes();
			// 应该是多个routeId，通知每个routeId中的所有消费者“退出”的消息
			for (int i = 0; i < routeIds.length; i++) {
				int routeId = routeIds[i];
				channel.basicPublish(CommonUtils.EXCHANGE_NAME, "r" + routeId,
						MessageProperties.PERSISTENT_TEXT_PLAIN,
						messageBodyBytes);
			}

			Log.d(LOG_TAG, "Delete Message Sented '" + message + "'");
			channel.close();
			conn.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	protected void onNewIntent(Intent intent) {
		Log.d(LOG_TAG, "ShareLocationActivity onNewIntent");
		setFocuse();
		setUserInfo();
		Bundle bundle = intent.getExtras();
		if (bundle != null) {
			routeIds = bundle.getIntArray("routeIds");
		}
	}

	/**
	 * 让焦点指到任一个textView中
	 */
	private void setFocuse() {
		if (nameTextView != null) {
			nameTextView.setFocusable(true);
			nameTextView.setFocusableInTouchMode(true);
			nameTextView.requestFocus(); // 初始不让EditText得焦点
			nameTextView.requestFocusFromTouch();
		}
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
		case QUIT_DIALOG:
			if (quitDialog != null) {
				return quitDialog;
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
		case QUIT_DIALOG:
			if (quitDialog != null) {
				Log.d(LOG_TAG, "quitDialog onPrepareDialog!");
			}
			break;
		default:
			break;
		}
		super.onPrepareDialog(id, dialog);
	}
}
