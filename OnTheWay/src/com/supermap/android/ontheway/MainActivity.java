package com.supermap.android.ontheway;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;

/**
 * 程序的入口类。
 *
 */
public class MainActivity extends Activity implements OnClickListener {
    /** Called when the activity is first created. */
    Button login;
    long exitTime = 0; 
    private PreferencesService preferencesService;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);
        preferencesService = new PreferencesService(this);
        login = (Button) findViewById(R.id.btn);
        login.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {   
        int routeId =preferencesService.getRouteInfo(CommonUtils.ROUTEINFO_FIEL);
        	if(routeId==-1){ 
        	    Intent intent = new Intent();
        	    intent.setClass(MainActivity.this, BusLineActivity.class);
        	    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        	    startActivity(intent);
            }else{           
                Bundle bundle = new Bundle();
                bundle.putInt("position", routeId);
                Intent intent = new Intent();
                intent.putExtras(bundle);
                intent.setClass(MainActivity.this, ShareLocationActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                
            }
          
        }
        
     @Override
     public void onBackPressed() {  
         long currentTime = System.currentTimeMillis();   		
    	 if((currentTime-exitTime)>=2000){
    	     Toast.makeText(this, "再按一次返回键退出程序",Toast.LENGTH_SHORT).show();
    	     exitTime =currentTime;
    	 }else{     	
    	     super.onBackPressed();  		 
    	 }

     }
     
     
}