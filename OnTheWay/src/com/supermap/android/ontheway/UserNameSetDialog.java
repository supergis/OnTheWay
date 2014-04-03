package com.supermap.android.ontheway;

import org.apache.commons.lang3.StringUtils;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

/**
 * 用户名设置对话框
 *
 */
public class UserNameSetDialog extends Dialog {
    private Context context;
    private BusLineActivity busLineActivity;
    private EditText userNameText;
    private Button confirmBtn;

    public UserNameSetDialog(Context context) {
        super(context);
        this.context = context;
        busLineActivity = (BusLineActivity) context;
    }

    public UserNameSetDialog(Context context, int theme) {
        super(context, theme);
        this.context = context;
        busLineActivity = (BusLineActivity) context;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.set_dialog);

        confirmBtn = (Button) this.findViewById(R.id.button_confirm);
        confirmBtn.setClickable(false);
        confirmBtn.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String userName = getUserName();
                if (userName != null && userName.length() > 0) {
                    dismiss();
                    busLineActivity.saveUserName(userName);
                    Bundle bundle = new Bundle();
                    bundle.putInt("position", busLineActivity.position);
                    Intent intent = new Intent();
                    intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                    intent.putExtras(bundle);
                    intent.setClass(context, ShareLocationActivity.class);
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "昵称不能为空", Toast.LENGTH_SHORT).show();
                }
            }
        });

        userNameText = (EditText) findViewById(R.id.EditText03);
        userNameText.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!StringUtils.isEmpty(s)) {
                    confirmBtn.setClickable(true);
                } else {
                    confirmBtn.setClickable(false);
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    /**
     * 获取用户名
     */
    public String getUserName() {
        String userName = null;
        if (userNameText != null) {
            userName = userNameText.getText().toString();
        }
        return userName;
    }
   

}
