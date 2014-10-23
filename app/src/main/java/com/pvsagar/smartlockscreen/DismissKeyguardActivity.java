package com.pvsagar.smartlockscreen;

import android.app.Activity;
import android.app.KeyguardManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;

import com.pvsagar.smartlockscreen.applogic.EnvironmentDetector;
import com.pvsagar.smartlockscreen.services.BaseService;


public class DismissKeyguardActivity extends Activity {
    private static final String LOG_TAG = DismissKeyguardActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dismiss_keyguard);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

        int systemUiVisibilityFlags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            systemUiVisibilityFlags = systemUiVisibilityFlags | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }
        getWindow().getDecorView().setSystemUiVisibility(systemUiVisibilityFlags);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        EnvironmentDetector.manageEnvironmentDetectionCriticalSection.release();
        startService(BaseService.getServiceIntent(this, null, BaseService.ACTION_DETECT_ENVIRONMENT));
        finish();
        overridePendingTransition(0, 0);

    }

    @Override
    protected void onPause() {
        super.onPause();
        new WaitBeforeDismiss().execute();
    }

    private class WaitBeforeDismiss extends AsyncTask<Void, Void, Void>{
        @Override
        protected Void doInBackground(Void... params) {
            try {
                while(true){
                    Thread.sleep(100);
                    if(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT){
                        return null;
                    }
                    KeyguardManager keyguardManager = (KeyguardManager) DismissKeyguardActivity.this.getSystemService(KEYGUARD_SERVICE);
                    if(keyguardManager.inKeyguardRestrictedInputMode()){
                        Log.d(LOG_TAG, "keyguard locked");
                    } else {
                        Log.d(LOG_TAG, "keyguard unlocked");
                        return null;
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            startService(BaseService.getServiceIntent(getBaseContext(), null, BaseService.ACTION_DISMISS_PATTERN_OVERLAY));
        }

        @Override
        protected void onCancelled(Void aVoid) {
            startService(BaseService.getServiceIntent(getBaseContext(), null, BaseService.ACTION_DISMISS_PATTERN_OVERLAY));
        }
    }
}
