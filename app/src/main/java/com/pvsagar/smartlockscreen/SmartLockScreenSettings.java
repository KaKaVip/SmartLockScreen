package com.pvsagar.smartlockscreen;

import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import com.pvsagar.smartlockscreen.adapters.NavigationDrawerListAdapter;
import com.pvsagar.smartlockscreen.applogic_objects.User;
import com.pvsagar.smartlockscreen.backend_helpers.Utility;
import com.pvsagar.smartlockscreen.fragments.ManageEnvironmentFragment;
import com.pvsagar.smartlockscreen.fragments.OverlappingEnvironmentsFragment;
import com.pvsagar.smartlockscreen.fragments.SetMasterPasswordFragment;
import com.pvsagar.smartlockscreen.frontend_helpers.OneTimeInitializer;
import com.pvsagar.smartlockscreen.receivers.AdminActions;
import com.pvsagar.smartlockscreen.services.BaseService;
import com.pvsagar.smartlockscreen.services.NotificationService;
import com.readystatesoftware.systembartint.SystemBarTintManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The main settings activity with a navigation drawer.
 */
public class SmartLockScreenSettings extends ActionBarActivity
        implements SetMasterPasswordFragment.MasterPasswordSetListener,
        ManageEnvironmentFragment.ActionModeListener{
    private static final String LOG_TAG = SmartLockScreenSettings.class.getSimpleName();

    private static final int INDEX_MANAGE_ENVIRONMENTS = 0;
    private static final int INDEX_ENVIRONMENT_OVERLAP = 1;
    private static final int INDEX_MASTER_PASSWORD = 2;
    private static final int INDEX_SETTINGS = 0;
    private static final int INDEX_HELP = 1;
    private static final int INDEX_ABOUT = 2;

    private static final int MASTER_PASSWORD_REQUEST = 41;

    private static int mPaddingTop = 0, mPaddingBottom = 0;

    SystemBarTintManager tintManager;

    DrawerLayout drawerLayout;
    ListView navDrawerListView;
    NavigationDrawerListAdapter listAdapter;
    private ActionBarDrawerToggle actionBarDrawerToggle;
    int actionBarColor;

    List<String> mainItemList;

    String mTitle;
    int position, prevPosition = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_smart_lock_screen_settings);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, new ManageEnvironmentFragment())
                    .commit();
        }
        OneTimeInitializer.initialize(this, MASTER_PASSWORD_REQUEST);

        startService(BaseService.getServiceIntent(this, null, null));
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2 && !NotificationService.isInstanceCreated()){
            Intent intent = new Intent(this,NotificationService.class);
            startService(intent);
        }

        setUpActionBar();
        setUpNavDrawer();

        drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout_main_settings);
        actionBarDrawerToggle = new ActionBarDrawerToggle(this, drawerLayout, 0, 0) {

            /** Called when a drawer has settled in a completely closed state. */
            public void onDrawerClosed(View view) {
                super.onDrawerClosed(view);
                if(position == prevPosition){
                    if(mTitle != null) {
                        setTitle(mTitle);
                    }
                    return;
                }
                switch (listAdapter.getItemViewType(position)){
                    case NavigationDrawerListAdapter.ITEM_TYPE_PROFILE:
                        listAdapter.setSelectedProfileIndex(listAdapter.getItemArrayIndex(position));
                        break;
                    case NavigationDrawerListAdapter.ITEM_TYPE_NEW_PROFILE:
                        //TODO after user profiles are enabled
                        break;
                    case NavigationDrawerListAdapter.ITEM_TYPE_MAIN:
                        FragmentManager fragmentManager = getFragmentManager();
                        FragmentTransaction ft;
                        boolean isValid = true;
                        int itemArrayIndex = listAdapter.getItemArrayIndex(position);
                        switch (itemArrayIndex){
                            case INDEX_MANAGE_ENVIRONMENTS:
                                ft = fragmentManager.beginTransaction();
                                ft.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
                                ft.replace(R.id.container, new ManageEnvironmentFragment())
                                        .commit();
                                break;
                            case INDEX_ENVIRONMENT_OVERLAP:
                                ft = fragmentManager.beginTransaction();
                                ft.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
                                ft.replace(R.id.container, new OverlappingEnvironmentsFragment())
                                        .commit();
                                break;
                            case INDEX_MASTER_PASSWORD:
                                ft = fragmentManager.beginTransaction();
                                ft.setCustomAnimations(R.animator.fade_in, R.animator.fade_out);
                                ft.replace(R.id.container, new SetMasterPasswordFragment())
                                        .commit();
                                break;
                            default:
                                isValid = false;
                        }
                        if(isValid){
                            mTitle = mainItemList.get(itemArrayIndex);
                            listAdapter.setSelectedMainItemIndex(itemArrayIndex);
                            listAdapter.notifyDataSetChanged();
                        }
                        if(position != -1) {
                            prevPosition = position;
                            position = -1;
                        }
                        break;
                    case NavigationDrawerListAdapter.ITEM_TYPE_SECONDARY:
                        switch (listAdapter.getItemArrayIndex(position)){
                            case INDEX_SETTINGS:
                                startActivity(new Intent(SmartLockScreenSettings.this, GeneralSettingsActivity.class));
                                break;
                            //TODO launch appropriate activities
                            case INDEX_ABOUT:
                            case INDEX_HELP:
                                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                                        Uri.parse("http://forum.xda-developers.com/android/apps-games/app-smartlockscreen-android-enjoy-t2919989"));
                                startActivity(browserIntent);
                            default:
                                Toast.makeText(SmartLockScreenSettings.this, "Not yet implemented", Toast.LENGTH_SHORT).show();
                        }
                        break;
                    default:
                        return;
                }
                if(mTitle != null) {
                    setTitle(mTitle);
                }
            }

            /** Called when a drawer has settled in a completely open state. */
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mTitle = getTitle().toString();
                setTitle(getString(R.string.title_activity_smart_lock_screen_settings));
            }
        };
        // Set the drawer toggle as the DrawerListener
        drawerLayout.setDrawerListener(actionBarDrawerToggle);

        drawerLayout.openDrawer(Gravity.START);
    }

    @Override
    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            super.onPostCreate(savedInstanceState, persistentState);
        }
        actionBarDrawerToggle.syncState();
    }

    private void setUpActionBar(){
        ActionBar actionBar = getSupportActionBar();
        actionBarColor = getResources().getColor(R.color.action_bar_settings);
        if(!Utility.checkForNullAndWarn(actionBar, LOG_TAG)){
            actionBar.setBackgroundDrawable(new ColorDrawable(actionBarColor));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT){
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                    WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS | WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION);
        }

        tintManager = new SystemBarTintManager(this);
        tintManager.setStatusBarTintEnabled(true);
        tintManager.setTintColor(actionBarColor);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            mPaddingTop = tintManager.getConfig().getPixelInsetTop(true);
            if(Build.VERSION.SDK_INT == Build.VERSION_CODES.KITKAT){
                mPaddingTop += 16;
            }
            mPaddingBottom = tintManager.getConfig().getNavigationBarHeight();
        }
    }

    private void setUpNavDrawer(){
        List<User> userList = new ArrayList<User>(Arrays.asList(new User[]{User.getDefaultUser(this)}));
        mainItemList = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.nav_drawer_main_items)));
        List<Integer> mainItemRIds = new ArrayList<Integer>();
        mainItemRIds.add(R.drawable.ic_environment);
        mainItemRIds.add(R.drawable.ic_env_overlap);
        mainItemRIds.add(R.drawable.ic_master_password);
        List<String> secondaryItemList = new ArrayList<String>(Arrays.asList(getResources().getStringArray(R.array.nav_drawer_secondary_items)));
        List<Integer> secondaryItemIds = new ArrayList<Integer>();
        secondaryItemIds.add(R.drawable.ic_settings);
        secondaryItemIds.add(R.drawable.ic_help);
        secondaryItemIds.add(R.drawable.ic_about);
        listAdapter = new NavigationDrawerListAdapter(this, userList, mainItemList, mainItemRIds, secondaryItemList, secondaryItemIds);

        navDrawerListView = (ListView) findViewById(R.id.drawer_list_view);
        switch (getResources().getConfiguration().orientation){
            case Configuration.ORIENTATION_UNDEFINED:
            case Configuration.ORIENTATION_PORTRAIT:
                View footerView = new View(this);
                footerView.setLayoutParams(new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, mPaddingBottom));
                footerView.setBackgroundColor(Color.TRANSPARENT);
                navDrawerListView.addFooterView(footerView, null, false);
                break;
        }
        navDrawerListView.setPadding(navDrawerListView.getPaddingLeft(), navDrawerListView.getPaddingTop() + mPaddingTop,
                navDrawerListView.getPaddingRight(), navDrawerListView.getPaddingBottom());
        navDrawerListView.setAdapter(listAdapter);

        navDrawerListView.setSelection(userList.size() + 2);
        navDrawerListView.setOnItemClickListener(new DrawerItemClickListener());
        listAdapter.setSelectedProfileIndex(0);
        listAdapter.setSelectedMainItemIndex(0);
        mTitle = mainItemList.get(0);
        position = prevPosition = navDrawerListView.getSelectedItemPosition();

    }

    @Override
    public void onMasterPasswordSet() {
        Toast.makeText(this, "Master passphrase changed", Toast.LENGTH_SHORT).show();
        drawerLayout.openDrawer(Gravity.START);
    }

    @Override
    public void onCancelSetMasterPassword() {
        drawerLayout.openDrawer(Gravity.START);
    }

    @Override
    public void onActionModeDestroyed() {
        tintManager.setTintColor(actionBarColor);
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
    }

    @Override
    public void onActionModeCreated() {
//        tintManager.setTintColor(getResources().getColor(R.color.action_mode));
        tintManager.setTintColor(Color.BLACK); //TODO ActionMode color not working after updating to API 21. Will revert after finding a fix
        drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
    }

    private class DrawerItemClickListener implements ListView.OnItemClickListener{
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            SmartLockScreenSettings.this.position = position;
            drawerLayout.closeDrawers();
        }
    }

    @Override
    public void onBackPressed() {
        if(drawerLayout.isDrawerOpen(Gravity.START)){
            drawerLayout.closeDrawers();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        actionBarDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        actionBarDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Pass the event to ActionBarDrawerToggle, if it returns
        // true, then it has handled the app icon touch event
        if (actionBarDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == MASTER_PASSWORD_REQUEST){
            if(!(resultCode == RESULT_OK && AdminActions.isAdminEnabled())){
                finish();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}
