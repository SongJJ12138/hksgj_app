package com.lsyy.ditu;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.CoordType;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatus;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class MainActivity extends Activity {
    private String TAG = "===Client===";
    private TextView tv1 = null;
    Handler mhandlerSend;
    private String date="";
    boolean isRun = true;
    private Context ctx;
    SocThread socketThread;
    private MapView mMapView;
    private BaiduMap mBaiduMap;
    private  TextView tv_time;
    //显示定位点
    private BitmapDescriptor mMarker;
    //定位类
    private LocationClient mLocationClient;
    //是否是第一次定位
    private boolean isFirstLoc = true;
    Handler  mhandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            try {
                Log.i(TAG, "mhandler接收到msg=" + msg.what);
                if (msg.obj != null) {
                    String s = msg.obj.toString();
                    if (!s.equals(date)){
                        if (s.trim().length() > 0) {
                            switch (s){
                                case "去夏威夷\n":
                                    showLoc(31.2453045690,121.5065669346,100);
                                    tv_time.setText("22:01");
                                    break;
                                case "去埃菲尔铁塔\n":
                                    showLoc(39.9152108931,116.4039006839,100);
                                    tv_time.setText("17:05");
                                    break;
                                case "去迪拜塔\n":
                                    showLoc(30.2316321821,120.1315039824,100);
                                    tv_time.setText("09:33");
                                    break;
                                case "返回\n":
                                    BDLocation bdLocation=mLocationClient.getLastKnownLocation();
                                    showLoc(bdLocation.getLatitude(),bdLocation.getLongitude(),bdLocation.getRadius());
                                    Calendar calendars = Calendar.getInstance();
                                    calendars.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
                                    String hour = String.valueOf(calendars.get(Calendar.HOUR));
                                    String min = String.valueOf(calendars.get(Calendar.MINUTE));
                                    tv_time=findViewById(R.id.tv_time);
                                    tv_time.setText(hour+":"+min);
                                    break;
                            }
                        } else {
                            Log.i(TAG, "没有数据返回不更新");
                        }
                    }
                }
            } catch (Exception ee) {
                Log.i(TAG, "加载过程出现异常");
                ee.printStackTrace();
            }
        }
    };
    //定位回调
    private BDLocationListener mBDLocationListener = new     BDLocationListener() {
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {

            Log.d(TAG, "SUCCESS: ");
          showLoc(bdLocation.getLatitude(),bdLocation.getLongitude(),bdLocation.getRadius());

        }
    };

    private void showLoc(double latitude, double longitude, float radius) {
        MyLocationData data = new MyLocationData.Builder()
                .accuracy(radius)//定位精度
                .latitude(latitude)//纬度
                .longitude(longitude)//经度
                .direction(100)//方向 可利用手机方向传感器获取 此处为方便写死
                .build();
        //设置定位数据
        mBaiduMap.setMyLocationData(data);
        MyLocationConfiguration configuration = new MyLocationConfiguration(
                MyLocationConfiguration.LocationMode.NORMAL, false, mMarker);
        mBaiduMap.setMyLocationConfiguration(configuration);
        LatLng ll = new LatLng(latitude, longitude);
        //第一次定位需要更新下地图显示状态
        MapStatus.Builder builder = new MapStatus.Builder()
                .target(ll)//地图缩放中心点
                .zoom(18f);//缩放倍数 百度地图支持缩放21级 部分特殊图层为20级
        //改变地图状态
        mBaiduMap.setMapStatus(MapStatusUpdateFactory.newMapStatus(builder.build()));

    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        super.onCreate(savedInstanceState);
        SDKInitializer.initialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        SDKInitializer.setCoordType(CoordType.BD09LL);
        ctx = MainActivity.this;
        startSocket();
        initView();
        initLoc();
        myRequetPermission();

    }

    private void myRequetPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(),"没有权限,请手动开启定位权限",Toast.LENGTH_SHORT).show();
            // 申请一个（或多个）权限，并提供用于回调返回的获取码（用户定义）
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.READ_PHONE_STATE}, 1);
        }else{
            mLocationClient.start();
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            // requestCode即所声明的权限获取码，在checkSelfPermission时传入
            case 1:
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // 获取到权限，作相应处理（调用定位SDK应当确保相关权限均被授权，否则可能引起定位失败）
                    mLocationClient.start();
                } else {
                    // 没有获取到权限，做特殊处理
                    Toast.makeText(getApplicationContext(), "获取位置权限失败，请手动开启", Toast.LENGTH_SHORT).show();
                }
                break;
            default:
                break;
        }
    }


    private void initLoc() {
            //开启定位图层
            mBaiduMap.setMyLocationEnabled(true);
            //定位相关参数设置
            LocationClientOption option = new LocationClientOption();
            option.setLocationMode(LocationClientOption.LocationMode.Hight_Accuracy);
            option.setCoorType("bd09ll");
//            int span = 1000;
//            option.setScanSpan(span);
            option.setIsNeedAddress(true);
            option.setOpenGps(true);
            option.setLocationNotify(true);
            option.setIsNeedLocationDescribe(true);
            option.setIsNeedLocationPoiList(true);
            option.setIgnoreKillProcess(false);
            option.SetIgnoreCacheException(false);
            option.setEnableSimulateGps(false);
            mLocationClient.setLocOption(option);
    }

    private void initView() {
        mMapView = (MapView) findViewById(R.id.bmapView);
        mBaiduMap = mMapView.getMap();
        mLocationClient = new LocationClient(this);
        mLocationClient.registerLocationListener(mBDLocationListener);
        mMarker = BitmapDescriptorFactory.fromResource(R.drawable.ic_launcher_background);
        Calendar calendars = Calendar.getInstance();
        calendars.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));
        String hour = String.valueOf(calendars.get(Calendar.HOUR));
        String min = String.valueOf(calendars.get(Calendar.MINUTE));
        tv_time=findViewById(R.id.tv_time);
        tv_time.setText(hour+":"+min);
    }

    public void startSocket() {
        socketThread = new SocThread(mhandler, mhandlerSend, ctx);
        socketThread.start();
    }


    private void stopSocket() {
        socketThread.isRun = false;
        socketThread.close();
        socketThread = null;
        Log.i(TAG, "Socket已终止");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(TAG, "start onStart~~~");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.e(TAG, "start onRestart~~~");
    }

    @Override
    protected void onResume() {
        super.onResume();
        mMapView.onResume();
        Log.e(TAG, "start onResume~~~");
    }

    @Override
    protected void onPause() {
        super.onPause();
        mMapView.onPause();
        Log.e(TAG, "start onPause~~~");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.e(TAG, "start onStop~~~");
        stopSocket();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
        Log.e(TAG, "start onDestroy~~~");

    }

}
