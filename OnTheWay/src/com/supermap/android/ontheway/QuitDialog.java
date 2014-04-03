package com.supermap.android.ontheway;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

/**
 * 退出共享时弹出的对话框，用于提示是否确定退出。
 *
 */
public class QuitDialog extends Dialog{

    private Context context;
    private SetActivity setActivity;
    private Button confirmQuitBtn;
    private Button cancelBtn;

    public QuitDialog(Context context) {
        super(context);
        this.context = context;
        setActivity =(SetActivity)context;
    }
    
    public QuitDialog(Context context, int theme) {
        super(context, theme);
        this.context = context;
        setActivity = (SetActivity) context;
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.quit_dialog);
        confirmQuitBtn = (Button) this.findViewById(R.id.button_confirmQuitShare);
        confirmQuitBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
             // 发送退出的消息
                setActivity.sendDeleteMessage();
                // 关闭资源
                RabbitmqUtils.closeResouce();
                Intent intent = new Intent();
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                intent.setClass(context, MainActivity.class);
                context.startActivity(intent);
               
            }
        });
        
        cancelBtn =(Button) this.findViewById(R.id.button_cancel);
        cancelBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
            
        }
        
    }


