package org.pytorch.demo;

import java.io.IOException;
import java.util.List;

import android.content.Context;
import android.location.Address;
import android.location.Criteria;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;

public class AndroidLocationManager {
    private static AndroidLocationManager mInstance;
    private Context mContext;
    private LocationManager mLocationManager;
    private Handler mHandler;

    private LocationResultEntry mLastLocationEntry;

    private AndroidLocationManager(Context ctx) {
        this.mContext = ctx;
        mLocationManager = (LocationManager) ctx
                .getSystemService(Context.LOCATION_SERVICE);
        mHandler = new Handler(mContext.getMainLooper());
    }

    public static AndroidLocationManager getInstance(Context ctx) {
        if (mInstance == null) {
            mInstance = new AndroidLocationManager(ctx);
        }
        return mInstance;
    }

    /**
     * 开始定位
     */
    public void startLocation() {
        requestGPSLocation();

        requestAddress(getLocation());
    }

    private Location getLocation() {
        Criteria criteria = new Criteria();
        criteria.setAccuracy(Criteria.ACCURACY_FINE);// 高精度
        criteria.setAltitudeRequired(false);// 无海拔要求
        criteria.setBearingRequired(false);// 无方位要求
        criteria.setCostAllowed(true);// 允许产生资费
        criteria.setPowerRequirement(Criteria.POWER_LOW);// 低功耗

        // 获取最佳服务对象
        String provider = mLocationManager.getBestProvider(criteria, true);
        //获取最后已知定位信息
        Location loc = mLocationManager.getLastKnownLocation(provider);
        return loc;
    }

    private GPSLocationListener mGpsListener;

    /**
     * 请求GPS定位
     */
    private void requestGPSLocation() {
        if (mGpsListener == null) {
            mGpsListener = new GPSLocationListener();
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                1000 * 2, 50, mGpsListener);
    }

    private NetWorkLocationListener mNetworkListener;

    /**
     * 请求网络定位
     */
    private void requestNetworkLocation() {
        if (mNetworkListener == null) {
            mNetworkListener = new NetWorkLocationListener();
        }
        mLocationManager.requestLocationUpdates(
                LocationManager.NETWORK_PROVIDER, 0, 0, mNetworkListener);
    }

    /**
     * 停止定位
     */
    public void stop() {
        if (mGpsListener != null) {
            mLocationManager.removeUpdates(mGpsListener);
            mGpsListener = null;
        }
        if (mNetworkListener != null) {
            mLocationManager.removeUpdates(mNetworkListener);
            mNetworkListener = null;
        }
    }

    /**
     * 请求地址
     * @param loc
     */
    private void requestAddress(Location loc) {
        if (loc == null) {
            notifyListener(false, null);
            return;
        }
        Geocoder gc = new Geocoder(mContext);
        List<Address> addresses = null;
        try {
            //请求地址
            addresses = gc.getFromLocation(loc.getLatitude(),
                    loc.getLongitude(), 10);
        } catch (IOException e) {
            e.printStackTrace();
        }
        LocationResultEntry result = new LocationResultEntry();
        result.setLongitude(loc.getLongitude());
        result.setLatitude(loc.getLatitude());
        if (addresses.size() > 0) {
            result.setLocType(loc.getProvider());
            result.setAddress(addresses.get(0).getAddressLine(0));
            result.setCity(addresses.get(0).getLocality());
        }
        notifyListener(true, result);
        mLastLocationEntry = result;
    }

    public LocationResultEntry getLastLocationEntry() {
        return mLastLocationEntry;
    }

    /**
     * 回调监听
     * @param hasResult
     * @param result
     */
    private void notifyListener(final boolean hasResult,
                                final LocationResultEntry result) {
        mHandler.post(new Runnable() {

            @Override
            public void run() {
                if (mResultListener != null) {
                    mResultListener.onResult(hasResult, result);
                }
            }
        });
    }

    private class NetWorkLocationListener implements LocationListener {
        public void onLocationChanged(Location location) {
            //更新地址
            requestAddress(getLocation());
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //状态无效则请求GPS定位
            if (LocationProvider.OUT_OF_SERVICE == status) {
                requestGPSLocation();
            }
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    // GPS监听的回调函数
    private class GPSLocationListener implements LocationListener {

        @Override
        public void onLocationChanged(Location location) {
            //更新地址
            requestAddress(getLocation());
        }

        @Override
        public void onProviderDisabled(String provider) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
            //状态无效则请求GPS定位
            if (LocationProvider.OUT_OF_SERVICE == status) {
                requestNetworkLocation();
            }
        }
    }

    private OnLocationListener mResultListener;

    public void setOnLocationListener(OnLocationListener l) {
        this.mResultListener = l;
    }

    public interface OnLocationListener {
        void onResult(boolean hasResult, LocationResultEntry entry);
    }

    //数据实体类
    public class LocationResultEntry {
        private String locType;
        private String address;
        private String city;
        private double longitude;
        private double latitude;

        public String getLocType() {
            return locType;
        }

        public void setLocType(String locType) {
            this.locType = locType;
        }

        public String getAddress() {
            return address;
        }

        public void setAddress(String address) {
            this.address = address;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public double getLongitude() {
            return longitude;
        }

        public void setLongitude(double longitude) {
            this.longitude = longitude;
        }

        public double getLatitude() {
            return latitude;
        }

        public void setLatitude(double latitude) {
            this.latitude = latitude;
        }
    }
}