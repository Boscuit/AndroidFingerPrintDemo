package com.createchance.fingerprintdemo;

import android.app.Activity;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.SimpleExpandableListAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class DeviceControlActivity extends Activity {
    private final static String TAG = DeviceControlActivity.class.getSimpleName();

    public static final String EXTRAS_DEVICE_NAME = "DEVICE_NAME";
    public static final String EXTRAS_DEVICE_ADDRESS = "DEVICE_ADDRESS";

    private TextView mConnectionState;
    private TextView mDataField;
    private String mDeviceName;
    private String mDeviceAddress;
    private ExpandableListView mGattServicesList;
    private BluetoothLeService mBluetoothLeService;
    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics =
            new ArrayList<ArrayList<BluetoothGattCharacteristic>>();
    private boolean mConnected = false;
    private BluetoothGattCharacteristic mNotifyCharacteristic;

    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        //当蓝牙连接的时候 这里可以得到服务 ，以及使用服务的方法。假如初始化失败的话就退出
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder service) {
            System.out.println("******** onServiceConnected *********");
            //这里可以得到服务类的服务========== 如果初始化失败就退出  初始化的结果是得到一个GATT===============
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if (!mBluetoothLeService.initialize()) {
                finish();
            }

            // Automatically connects to the device upon successful start-up initialization.
            //这里是连接设备===============
            mBluetoothLeService.connect(mDeviceAddress);
        }
        //当服务断开的时候 就将蓝牙服务释放掉
        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };//end of serviceconnection====================

    //这个是广播接收器 接收从Service 发过来的广播  发现服务 断开 连接 有接收到数据时的通知
    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @SuppressLint("NewApi") @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (BluetoothLeService.ACTION_GATT_CONNECTED.equals(action)) {
                System.out.println(" control connected");
                mConnected = true;
                updateConnectionState(R.string.connected);
                invalidateOptionsMenu();
            } else if (BluetoothLeService.ACTION_GATT_DISCONNECTED.equals(action)) {
                System.out.println("control disconnected");
                mConnected = false;
                updateConnectionState(R.string.disconnected);
                invalidateOptionsMenu();
                clearUI();
            } else if (BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED.equals(action)) {
                // Show all the supported services and characteristics on the user interface.
                displayGattServices(mBluetoothLeService.getSupportedGattServices());
                UUID servuuid2 = mBluetoothLeService.getSupportedGattServices().get(2).getUuid();

                System.out.println("serv uuid 2  "+ servuuid2);
//
//                UUID charuuid0 = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(0).getUuid();
//                UUID charuuid1 = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(1).getUuid();
//
//                System.out.println("char 0"+charuuid0);
//                System.out.println("char 1"+charuuid1);
//
//                mBluetoothLeService.setCharacteristicNotification(mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(0),true);

                BluetoothGatt mygatt =  mBluetoothLeService.KgetGatt();
                BluetoothGattCharacteristic kencharwrite;
                kencharwrite = mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(1);
                System.out.println(kencharwrite.getUuid()+"%%%%%%%%%%%%%%%%%%%%%%%");
                byte[] a = {0,10,20};
                kencharwrite.setValue(a);
                mygatt.writeCharacteristic(mBluetoothLeService.getSupportedGattServices().get(2).getCharacteristics().get(1));
            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                System.out.println("recieve data :"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };


    //这个是点击服务 特征值列表是的 回调
    private final ExpandableListView.OnChildClickListener servicesListClickListener =
            new ExpandableListView.OnChildClickListener() {
                @SuppressLint("NewApi") @Override
                public boolean onChildClick(ExpandableListView parent, View v, int groupPosition,
                                            int childPosition, long id) {
                    //System.out.println("expandlist i am click ");
                    if (mGattCharacteristics != null) {

                        //根据点击到的列表的特征值 得到相应的特征值。 先得到服务 再得到特征值==========
                        final BluetoothGattCharacteristic characteristic =
                                mGattCharacteristics.get(groupPosition).get(childPosition);
                        //得到特征值的描述  可读 可写  可notify
                        final int charaProp = characteristic.getProperties();
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_READ) > 0) {
                            if (mNotifyCharacteristic != null) {
                                mBluetoothLeService.setCharacteristicNotification(
                                        mNotifyCharacteristic, false);
                                mNotifyCharacteristic = null;
                            }
                            //读取可读的特征值
                            System.out.println("Readable");
                            mBluetoothLeService.readCharacteristic(characteristic);
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
                        {
//                            if (mNotifyCharacteristic != null) {
//                                mBluetoothLeService.setCharacteristicNotification(
//                                        mNotifyCharacteristic, false);
//                                mNotifyCharacteristic = null;
//                            }
//                            //写数据
//                            byte[] senddatas =new byte[]{1,2,3,4,34,56,78,90};
//                            characteristic.setValue(senddatas);
//                            System.out.println("Writable");
//                            mBluetoothLeService.writeCharacteristic(characteristic);
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0) {
                                // If there is an active notification on a characteristic, clear
                                // it first so it doesn't update the data field on the user interface.
                                if (mNotifyCharacteristic != null) {
                                    System.out.println("Write Act Notified");
                                    mBluetoothLeService.setCharacteristicNotification(
                                            mNotifyCharacteristic, false);
                                    mNotifyCharacteristic = null;
                                }
                                //characteristic.setValue(bytes);
                                characteristic.setValue("testing");
                                //characteristic.setWriteType(BluetoothGattCharacteristic.PERMISSION_WRITE);
                                System.out.println("Writable");
                                mBluetoothLeService.writeCharacteristic(characteristic);
                            }
                        }
                        if ((charaProp | BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0)
                        {
                            System.out.println("Notifiable");
                            mNotifyCharacteristic = characteristic;
                            mBluetoothLeService.setCharacteristicNotification(
                                    characteristic, true);
                        }
                        return true;
                    }
                    return false;
                }
            };

    private void clearUI() {
        mGattServicesList.setAdapter((SimpleExpandableListAdapter) null);
        mDataField.setText(R.string.no_data);
    }//清除掉UI，将特征值服务列表都清除为空。

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.gatt_services_characteristics);

        //取得从 sacnactivity传过来的 设备名字 还有 地址==============
        final Intent intent = getIntent();
        mDeviceName = intent.getStringExtra(EXTRAS_DEVICE_NAME);
        mDeviceAddress = intent.getStringExtra(EXTRAS_DEVICE_ADDRESS);

        // Sets up UI references.
        ((TextView) findViewById(R.id.device_address)).setText(mDeviceAddress);
        mGattServicesList = (ExpandableListView) findViewById(R.id.gatt_services_list);
        mGattServicesList.setOnChildClickListener(servicesListClickListener);
        mConnectionState = (TextView) findViewById(R.id.connection_state);
        mDataField = (TextView) findViewById(R.id.data_value);

//        getActionBar().setTitle("device :" + mDeviceName);
//        getActionBar().setDisplayHomeAsUpEnabled(true);
        //设置intent 绑定服务===================
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);
    }//end of oncreate=================

    //每次到这个页面都会被调用一次的函数。而oncreate不会
    @Override
    protected void onResume() {
        super.onResume();
        //向系统注册广播接收 当有数据时会通知 当断开连接 连接时有通知
        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
        if (mBluetoothLeService != null) {
            final boolean result = mBluetoothLeService.connect(mDeviceAddress);
            Log.d(TAG, "Connect request result=" + result);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mGattUpdateReceiver);
        //当此页面被暂停掉的时候 不再接收系统的广播
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
        //当页面被干掉的时候就将蓝牙服务释放掉，并干掉服务===========
    }

    //这个是状态栏 显示断开还有连接状态。
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.gatt_services, menu);
        if (mConnected) {
            menu.findItem(R.id.menu_connect).setVisible(false);
            menu.findItem(R.id.menu_disconnect).setVisible(true);
        } else {
            menu.findItem(R.id.menu_connect).setVisible(true);
            menu.findItem(R.id.menu_disconnect).setVisible(false);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_connect:
                //连接蓝牙 传入的参数是蓝牙的地址。
                mBluetoothLeService.connect(mDeviceAddress);
                return true;
            case R.id.menu_disconnect:
                //断开蓝牙连接
                mBluetoothLeService.disconnect();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    //更新连接状态显示 开启了一条线程==================
    private void updateConnectionState(final int resourceId) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mConnectionState.setText(resourceId);
            }
        });
    }//end of updateconnectionstate=============

    //显示 接收到的数据 这个方法被 广播接收器 调用
    private void displayData(String data) {
        if (data != null) {
            mDataField.setText(data);
        }
    }

    @SuppressLint("NewApi") private void displayGattServices(List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        String uuid = null;
        String unknownServiceString = getResources().getString(R.string.unknown_service);
        String unknownCharaString = getResources().getString(R.string.unknown_characteristic);

        ArrayList<HashMap<String, String>> gattServiceData = new ArrayList<HashMap<String, String>>();
        ArrayList<ArrayList<HashMap<String, String>>> gattCharacteristicData
                = new ArrayList<ArrayList<HashMap<String, String>>>();
        mGattCharacteristics = new ArrayList<ArrayList<BluetoothGattCharacteristic>>();

        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            HashMap<String, String> currentServiceData = new HashMap<String, String>();
            uuid = gattService.getUuid().toString();
            currentServiceData.put(
                    LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
            currentServiceData.put(LIST_UUID, uuid);
            gattServiceData.add(currentServiceData);

            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
                    new ArrayList<HashMap<String, String>>();
            List<BluetoothGattCharacteristic> gattCharacteristics =
                    gattService.getCharacteristics();
            ArrayList<BluetoothGattCharacteristic> charas =
                    new ArrayList<BluetoothGattCharacteristic>();

            // Loops through available Characteristics.
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                charas.add(gattCharacteristic);
                HashMap<String, String> currentCharaData = new HashMap<String, String>();
                uuid = gattCharacteristic.getUuid().toString();
                currentCharaData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownCharaString));
                currentCharaData.put(LIST_UUID, uuid);
                gattCharacteristicGroupData.add(currentCharaData);
            }
            mGattCharacteristics.add(charas);
            gattCharacteristicData.add(gattCharacteristicGroupData);
        }

        SimpleExpandableListAdapter gattServiceAdapter = new SimpleExpandableListAdapter(
                this,
                gattServiceData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 },
                gattCharacteristicData,
                android.R.layout.simple_expandable_list_item_2,
                new String[] {LIST_NAME, LIST_UUID},
                new int[] { android.R.id.text1, android.R.id.text2 }
        );
        mGattServicesList.setAdapter(gattServiceAdapter);
    }

    // 广播接收过滤器  当GATT连接时 当GATT断开连接时 发现服务时 当从机有数据过来时。
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }
}
