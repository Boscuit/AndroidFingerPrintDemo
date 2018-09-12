package com.createchance.fingerprintdemo;


import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.app.ActivityCompat;
import android.support.v4.hardware.fingerprint.FingerprintManagerCompat;
import android.support.v4.os.CancellationSignal;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private final String TAG = "MainActivity";
    private TextView mResultInfo = null;
    private Button mCancelBtn = null;
    private Button mStartBtn = null;
    private Button mLogOutBtn = null;
    private Button mOpenBtn = null;

    private FingerprintManagerCompat fingerprintManager = null;
    private MyAuthCallback myAuthCallback = null;
    private CancellationSignal cancellationSignal = null;

    private Context mContext = MainActivity.this;
    private ListView mDeviceList;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mBluetoothLeScanner;
    private List<ScanFilter> bleScanFilters;
    private ScanSettings bleScanSettings;
    private boolean mScanning;
    private boolean mConnected = false;
    private TextView mConnectionState;
    private TextView mDataField;
    private TextView mNameField;
    private String AppID;
    private String mDeviceName;
    private String mDeviceAddress;
    private BluetoothLeService mBluetoothLeService;
    private BluetoothGattCharacteristic mNotifyCharacteristic;
//    private ArrayList<ArrayList<BluetoothGattCharacteristic>> mGattCharacteristics;

    private Handler handler = null;
    private final String LIST_NAME = "NAME";
    private final String LIST_UUID = "UUID";
    public static final int MSG_AUTH_SUCCESS = 100;
    public static final int MSG_AUTH_FAILED = 101;
    public static final int MSG_AUTH_ERROR = 102;
    public static final int MSG_AUTH_HELP = 103;
    private static final int REQUEST_ENABLE_BT = 1;
    private static final int REQUEST_ENABLE_LOCATION = 2;
    private static final int OPEN_COMMAND= 10;
    private static final int LOGOUT_COMMAND = 11;

    // 10秒后停止查找搜索.
    private static final long SCAN_PERIOD = 10000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mResultInfo = (TextView) this.findViewById(R.id.fingerprint_status);
        mCancelBtn = (Button) this.findViewById(R.id.cancel_button);
        mStartBtn = (Button) this.findViewById(R.id.start_button);

        mConnectionState = (TextView) this.findViewById(R.id.connection_state);
        mDataField = (TextView) this.findViewById(R.id.data_value);
        mNameField = (TextView) this.findViewById(R.id.device_name);
        mDeviceList = (ListView) this.findViewById(R.id.devices_list);


        mCancelBtn.setEnabled(false);
        mStartBtn.setEnabled(true);


        // set button listeners
        mCancelBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // set button state
                mCancelBtn.setEnabled(false);
                mStartBtn.setEnabled(true);

                // cancel fingerprint auth here.
                cancellationSignal.cancel();
                cancellationSignal = null;
            }
        });

        mStartBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // reset result info.
                mResultInfo.setText(R.string.fingerprint_hint);
                mResultInfo.setTextColor(getColor(R.color.hint_color));

                //disconnect previous connection
                if (mBluetoothLeService != null){
                    mBluetoothLeService.disconnect();
                }
                //clear UI
                clearUI();

                // start fingerprint auth here.
                try {
                    CryptoObjectHelper cryptoObjectHelper = new CryptoObjectHelper();
                    if (cancellationSignal == null) {
                        cancellationSignal = new CancellationSignal();
                    }
                    fingerprintManager.authenticate(cryptoObjectHelper.buildCryptoObject(), 0,
                            cancellationSignal, myAuthCallback, null);
                    // set button state.
                    mStartBtn.setEnabled(false);
                    mCancelBtn.setEnabled(true);
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Fingerprint init failed! Try again!", Toast.LENGTH_SHORT).show();
                }
            }
        });


         handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);

                Log.d(TAG, "msg: " + msg.what + " ,arg1: " + msg.arg1);
                switch (msg.what) {
                    case MSG_AUTH_SUCCESS:
                        /*Obtain Device_Id*/
                        Installation mInstallation = new Installation();
                        AppID = mInstallation.id(getApplicationContext());
//                        dynamicSetAppIDInfo(AppID);//dynamic text
                        setResultInfo(R.string.fingerprint_success);
                        //*******Device_Id Obtained********
                        /*Bluetooth*/
                        mLeDeviceListAdapter.clear();
                        mLeDeviceListAdapter.notifyDataSetChanged();
                        mDeviceList.setAdapter(mLeDeviceListAdapter);
                        scanLeDevice(true);
                        mCancelBtn.setEnabled(false);
                        mStartBtn.setEnabled(true);
                        cancellationSignal = null;
                        break;
                    case MSG_AUTH_FAILED:
                        setResultInfo(R.string.fingerprint_not_recognized);
//                        dynamicSetAppIDInfo("No Access");//dynamic text
                        mCancelBtn.setEnabled(false);
                        mStartBtn.setEnabled(true);
                        cancellationSignal = null;
                        break;
                    case MSG_AUTH_ERROR:
                        handleErrorCode(msg.arg1);
                        break;
                    case MSG_AUTH_HELP:
                        handleHelpCode(msg.arg1);
                        break;
                }
            }
        };

        // init fingerprint.
        fingerprintManager = FingerprintManagerCompat.from(this);

        if (!fingerprintManager.isHardwareDetected()) {
            // no fingerprint sensor is detected, show dialog to tell user.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.no_sensor_dialog_title);
            builder.setMessage(R.string.no_sensor_dialog_message);
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.cancel_btn_dialog, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            // show this dialog.
            builder.create().show();
        } else if (!fingerprintManager.hasEnrolledFingerprints()) {
            // no fingerprint image has been enrolled.
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.no_fingerprint_enrolled_dialog_title);
            builder.setMessage(R.string.no_fingerprint_enrolled_dialog_message);
            builder.setIcon(android.R.drawable.stat_sys_warning);
            builder.setCancelable(false);
            builder.setNegativeButton(R.string.cancel_btn_dialog, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            // show this dialog
            builder.create().show();
        } else {
            try {
                myAuthCallback = new MyAuthCallback(handler);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // 检查当前手机是否支持ble 蓝牙,如果不支持退出程序
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
        }

        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();

        // 检查设备上是否支持蓝牙
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.error_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        //获取地理位置权限
        ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},REQUEST_ENABLE_LOCATION);

        // add a filter to only scan for advertisers with the given service UUID
        bleScanFilters = new ArrayList<>();
        bleScanFilters.add(
                new ScanFilter.Builder().setDeviceName("HC-08").build()
        );
        bleScanSettings = new ScanSettings.Builder().build();
        System.out.println("Starting scanning with settings:" + bleScanSettings + " and filters:" + bleScanFilters);

    }
    //end of oncreate

    @Override
    protected void onResume() {
        super.onResume();
        //其实这个是 每次回到这个界面 或刚启动的时候都会执行到的一句话。
        System.out.println("ON Resume ");

        // Ϊ为了确保设备上蓝牙能使用, 如果当前蓝牙设备没启用,弹出对话框向用户要求授予权限来启用。
        if (!mBluetoothAdapter.isEnabled()) {
            if (!mBluetoothAdapter.isEnabled()) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
            }
        }


        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(mContext);
        mDeviceList.setAdapter(mLeDeviceListAdapter);

        // Set list item click event
        mDeviceList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final int pos = position;
                if (mConnected){
                    mBluetoothLeService.disconnect();
                    mConnected = false;
                    return;
                }
                System.out.println("try to connect");
                //扫描到的是一个蓝牙列表 通过位置得到 要连接的是哪一个。
                final BluetoothDevice device = mLeDeviceListAdapter.getDevice(position);//点击再根据点击位置获取device
                if (device == null) return;
                //Set up UI reference
                mDeviceName = device.getName();
                mDeviceAddress = device.getAddress();
                mNameField.setText(mDeviceName);
                //设置intent 绑定服务===================
                Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
                bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);//mServiceConnection 连接蓝牙
                //向系统注册广播接收 当有数据时会通知 当断开连接 连接时有通知
                registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());
                if (mBluetoothLeService != null) {
                    final boolean result = mBluetoothLeService.connect(mDeviceAddress);
                    Log.d(TAG, "Connect request result=" + result);
                }
                if (mScanning) {
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mScanning = false;
                }

                //Set OnClickListener for open button
                mOpenBtn = view.findViewById(R.id.open);
                mOpenBtn.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        System.out.print("LOGOUT onclick!! Position: " + pos + '\n');
                        //点击open按钮，发送开锁指令
                        sendCommand(OPEN_COMMAND);
                    }
                });

                //Set OnClickListener for log_out button
                mLogOutBtn = view.findViewById(R.id.log_out);
                mLogOutBtn.setOnClickListener(new View.OnClickListener(){
                    @Override
                    public void onClick(View v) {
                        System.out.print("LOGOUT onclick!! Position: " + pos + '\n');
                        //点击logout按钮，发送注销指令
                        sendCommand(LOGOUT_COMMAND);
                    }
                });

            }
        });

//        mLeDeviceListAdapter.getItem();
//        mLogOutBtn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                System.out.print("Log Out Button onclick!! Position: " + i + '\n'
//                        + "item name: " + mLeDevices.get(i).getName());
//            }
//        });


    }//end of onresume

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (!mStartBtn.isEnabled() && cancellationSignal != null) {
            cancellationSignal.cancel();
        }
        mBluetoothLeService.close();//close ble service

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }//end of onActivityResult

    @Override
    protected void onPause() {
        super.onPause();
        scanLeDevice(false);
        mLeDeviceListAdapter.clear();
        mLeDeviceListAdapter.notifyDataSetChanged();
        clearUI();
    }

    //这个函数可以启动 蓝牙搜索 十秒之后结束
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    //mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    invalidateOptionsMenu();
                }
            }, SCAN_PERIOD);

            mScanning = true;

            //Scan without filter
            //mBluetoothLeScanner.startScan(mScanCallback);

            //Scan with filters
            mBluetoothLeScanner.startScan(bleScanFilters,bleScanSettings,mScanCallback);

            //mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
            //mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }
        invalidateOptionsMenu();
    }

    public void DynamicSetTextTool(int stringId, Object changeText, int viewId) {// 动态文本工具方法
        String RefreshTime = getResources().getString(stringId);
        String FinalRefreshTime = String.format(RefreshTime, changeText);
        TextView RefreshTextObject = (TextView) findViewById(viewId);
        RefreshTextObject.setText(FinalRefreshTime);
    }

    private void handleHelpCode(int code) {
        switch (code) {
            case FingerprintManager.FINGERPRINT_ACQUIRED_GOOD:
                setResultInfo(R.string.AcquiredGood_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_IMAGER_DIRTY:
                setResultInfo(R.string.AcquiredImageDirty_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_INSUFFICIENT:
                setResultInfo(R.string.AcquiredInsufficient_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_PARTIAL:
                setResultInfo(R.string.AcquiredPartial_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_TOO_FAST:
                setResultInfo(R.string.AcquiredTooFast_warning);
                break;
            case FingerprintManager.FINGERPRINT_ACQUIRED_TOO_SLOW:
                setResultInfo(R.string.AcquiredToSlow_warning);
                break;
        }
    }

    private void handleErrorCode(int code) {
        switch (code) {
            case FingerprintManager.FINGERPRINT_ERROR_CANCELED:
                setResultInfo(R.string.ErrorCanceled_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_HW_UNAVAILABLE:
                setResultInfo(R.string.ErrorHwUnavailable_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_LOCKOUT:
                setResultInfo(R.string.ErrorLockout_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_NO_SPACE:
                setResultInfo(R.string.ErrorNoSpace_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_TIMEOUT:
                setResultInfo(R.string.ErrorTimeout_warning);
                break;
            case FingerprintManager.FINGERPRINT_ERROR_UNABLE_TO_PROCESS:
                setResultInfo(R.string.ErrorUnableToProcess_warning);
                break;
        }
    }

    //fingerprint result
    private void setResultInfo(int stringId) {
        if (mResultInfo != null) {
            if (stringId == R.string.fingerprint_success) {
                mResultInfo.setTextColor(getColor(R.color.success_color));
            } else {
                mResultInfo.setTextColor(getColor(R.color.warning_color));
            }
            mResultInfo.setText(stringId);
        }
    }

//    private void dynamicSetAppIDInfo(String AppID){
//        DynamicSetTextTool(R.string.app_id,AppID,R.id.app_id);
//    }


    private ScanCallback mScanCallback = new ScanCallback() {

        @Override
        public void onScanResult(int callbackType, final ScanResult result) {
            System.out.println("******************************************");
            System.out.println( "The scan result " + result);
            System.out.println("------------------------------------------");
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mLeDeviceListAdapter.addDevice(result.getDevice());
                    mLeDeviceListAdapter.notifyDataSetChanged();
                }
            });
        }
    };//===============end of lescancallback 这个callback 函数 会将device 传过来 这里可以添加设备

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

    private void clearUI() {
        mDeviceList.setAdapter(null);
        mNameField.setText(R.string.no_name);
        mDataField.setText(R.string.no_data);
    }//清除掉DataUI

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
                System.out.println("control connected");
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
                System.out.println("Service discover");
                //连接后，显示open和log_out按钮
                mOpenBtn.setVisibility(View.VISIBLE);
                mLogOutBtn.setVisibility(View.VISIBLE);
                //自动发送开锁指令
                sendCommand(OPEN_COMMAND);

            } else if (BluetoothLeService.ACTION_DATA_AVAILABLE.equals(action)) {
                System.out.println("data ava");
                displayData(intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
                System.out.println("recieve data :"+intent.getStringExtra(BluetoothLeService.EXTRA_DATA));
            }
        }
    };

    // 广播接收过滤器  当GATT连接时 当GATT断开连接时 发现服务时 当从机有数据过来时。
    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BluetoothLeService.ACTION_DATA_AVAILABLE);
        return intentFilter;
    }

    private void sendCommand(int command){
        if (mConnected){
//                    BluetoothGattService BroadcastService = new BluetoothGattService((UUID.fromString()))
            List<BluetoothGattService> ServiceList = mBluetoothLeService.getSupportedGattServices();
            if (ServiceList == null) return;
            String uuid = null;
            String unknownServiceString = getResources().getString(R.string.unknown_service);
            String unknownCharaString = getResources().getString(R.string.unknown_characteristic);
            for (BluetoothGattService gattService : ServiceList) {
                HashMap<String, String> currentServiceData = new HashMap<String, String>();
                uuid = gattService.getUuid().toString();
                currentServiceData.put(
                        LIST_NAME, SampleGattAttributes.lookup(uuid, unknownServiceString));
                currentServiceData.put(LIST_UUID, uuid);
                //gattServiceData.add(currentServiceData);
                if (currentServiceData.get(LIST_NAME) == "Data Broadcast Service"){
//                            ArrayList<HashMap<String, String>> gattCharacteristicGroupData =
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
                        if (currentCharaData.get(LIST_NAME) == "Broadcast Data") {
                            //得到特征值的描述  可读 可写  可notify
                            final int charaProp = gattCharacteristic.getProperties();
                            if ((charaProp | BluetoothGattCharacteristic.PROPERTY_WRITE) > 0)
                            {
                                //characteristic.setValue(bytes);
                                String AppIDMiddle = AppID.substring(9,23);
                                switch(command){
                                    case OPEN_COMMAND: gattCharacteristic.setValue("O/"+AppIDMiddle+".");//Max 20 bytes
                                        break;
                                    case LOGOUT_COMMAND: gattCharacteristic.setValue("D/"+AppIDMiddle+".");//Max 20 bytes
                                        break;
                                }
                                //characteristic.setWriteType(BluetoothGattCharacteristic.PERMISSION_WRITE);
                                System.out.println("Writable");
                                mBluetoothLeService.writeCharacteristic(gattCharacteristic);
                                System.out.println("Notifiable");
                                mNotifyCharacteristic = gattCharacteristic;
                                mBluetoothLeService.setCharacteristicNotification(
                                        gattCharacteristic, true);

                            }
                        }
                    }
                }
            }
        }
    }

}


