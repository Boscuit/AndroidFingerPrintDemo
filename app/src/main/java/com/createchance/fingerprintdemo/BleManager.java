package com.createchance.fingerprintdemo;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.util.Log;
import android.os.Handler;

import java.util.UUID;

import static android.content.ContentValues.TAG;

public class BleManager {
    //3、获取本地ble对象（BluetoothAdapter），它对应本地Android设备的蓝牙模块，可能这么称呼为本地ble对象不太准确，但我老人家开心这么称呼。
    //从此段代码开始可以把这些有关ble通信的代码写到一个class中当做一个ble工具class，以便代码清晰查看和方便调用。这里我们就当这个工具类叫BleManager

    private static final int STOP_LESCAN = 200;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGatt mBluetoothGatt;
    private Context mContext;
    private boolean isScanning = false;

//以上所定义的对象在下面的方法中都有用到，（建议在看蓝牙这方面的东西时，不管看谁的文章，都先把以上或者还有些蓝牙基本用的对象先熟悉是什么意思和基本作用）。



    private void CallBleManager(Context context)
    {
        this.mContext = context;
        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);  //BluetoothManager只在android4.3以上有
        if (bluetoothManager == null)
        {
            Log.e(TAG, "Unable to initialize BluetoothManager.");
            return;
        }
        mBluetoothAdapter = bluetoothManager.getAdapter();
    }



//4、既然获得了BluetoothAdapter对象，那么接下来就可以搜索ble设备了，这时就需要用到BluetoothAdapter的startLeScan()这个方法了

    public void startLeScan()
    {
        if (mBluetoothAdapter == null) {
            return;
        }
        if (isScanning) {
            return;
        }
        isScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);   //此mLeScanCallback为回调函数
        mHandler.sendEmptyMessageDelayed(STOP_LESCAN, 10000);  //这个搜索10秒，如果搜索不到则停止搜索
    }

//在4.3之前的api是通过注册广播来处理搜索时发生的一些事件，而支持ble的新的api中，是通过回调的方式来处理的，而mLeScanCallback就是一个接口对象

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback()
    {
        @Override
        public void onLeScan(BluetoothDevice device, int arg1, byte[] arg2) {
            Log.i(TAG, "onLeScan() DeviceName------>"+device.getName());  //在这里可通过device这个对象来获取到搜索到的ble设备名称和一些相关信息
            if(device.getName() == null){
                return;
            }
            if (device.getName().contains("Ble_Name")) {    //判断是否搜索到你需要的ble设备
                Log.i(TAG, "onLeScan() DeviceAddress------>"+device.getAddress());
                mBluetoothDevice = device;   //获取到周边设备
                /*下一行代码经过奇怪的修改*/
                mBluetoothAdapter.stopLeScan(mLeScanCallback);   //1、当找到对应的设备后，立即停止扫描；2、不要循环搜索设备，为每次搜索设置适合的时间限制。避免设备不在可用范围的时候持续不停扫描，消耗电量。
                connect();  //连接
            }
        }
    };

//

    private Handler mHandler = new Handler() {
        public void handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case STOP_LESCAN:
                    T.showLong(mContext, mContext.getResources().getString(R.string.msg_connect_failed));
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    //broadcastUpdate(Bitmap.Config.ACTION_GATT_DISCONNECTED);
                    isScanning = false;
                    Log.i(TAG, "Scan time is up");
                    break;
            }
        }
    };



//5、搜索到当然就是连接了，就是上面那个connect()方法了

    public boolean connect() {
        if (mBluetoothDevice == null) {
            Log.i(TAG, "BluetoothDevice is null.");
            return false;
        }
        //两个设备通过BLE通信，首先需要建立GATT连接。这里我们讲的是Android设备作为client端，连接GATT Server
        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, mGattCallback);  //mGattCallback为回调接口
        if (mBluetoothGatt != null) {
            if (mBluetoothGatt.connect()) {
                Log.d(TAG, "Connect succeed.");
                return true;
            } else {
                Log.d(TAG, "Connect fail.");
                return false;
            }
        } else {
            Log.d(TAG, "BluetoothGatt null.");
            return false;
        }
    }



    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                gatt.discoverServices(); //执行到这里其实蓝牙已经连接成功了
                Log.i(TAG, "Connected to GATT server.");
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                if(mBluetoothDevice != null){
                    Log.i(TAG, "Reconnect");
                    connect();
                }else{
                    Log.i(TAG, "Disconnected from GATT server.");
                }
            }
        }


        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "onServicesDiscovered");
                getBatteryLevel();  //获取电量
            } else {
                Log.i(TAG, "onServicesDiscovered status------>" + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead------>" + bytesToHexString(characteristic.getValue()));
            //判断UUID是否相等
            if (Values.UUID_KEY_BATTERY_LEVEL_CHARACTERISTICS.equals(characteristic.getUuid().toString()))
            {
                //what should be in here ?????????????
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            Log.d(TAG, "onCharacteristicChanged------>" + bytesToHexString(characteristic.getValue()));
            //判断UUID是否相等
            if (Values.UUID_KEY_BATTERY_LEVEL_CHARACTERISTICS.equals(characteristic.getUuid().toString()))
            {
                //what should be in here ?????????????
            }
        }

        //接受Characteristic被写的通知,收到蓝牙模块的数据后会触发onCharacteristicWrite
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG,"status = " + status);
            Log.d(TAG, "onCharacteristicWrite------>" + bytesToHexString(characteristic.getValue()));
        }
    };



    public void getBatteryLevel() {
        BluetoothGattCharacteristic batteryLevelGattC = BluetoothGatt.getCharcteristic(
                Values.UUID_KEY_BATTERY_LEVEL_SERVICE,
                Values.UUID_KEY_BATTERY_LEVEL_CHARACTERISTICS);
        if (batteryLevelGattC != null)
        {
            readCharacteristic(batteryLevelGattC);
            setCharacteristicNotification(batteryLevelGattC, true); //设置当指定characteristic值变化时，发出通知。
        }
    }



//6、获取服务与特征

//a.获取服务

    public BluetoothGattService getService(UUID uuid) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return null;
        }
        return mBluetoothGatt.getService(uuid);
    }

//b.获取特征

    private BluetoothGattCharacteristic getCharacteristic(String serviceUUID, String characteristicUUID) {

        //得到服务对象
        BluetoothGattService service = getService(UUID.fromString(serviceUUID));  //调用上面获取服务的方法
        if (service == null)
        {
            Log.e(TAG, "Can not find 'BluetoothGattService'");
            return null;
        }

        //得到此服务结点下Characteristic对象
        final BluetoothGattCharacteristic gattCharacteristic = service.getCharacteristic(UUID.fromString(characteristicUUID));
        if (gattCharacteristic != null) {
            return gattCharacteristic;
        } else {
            Log.e(TAG, "Can not find 'BluetoothGattCharacteristic'");
            return null;
        }
    }



//获取数据

    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }



    public boolean setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        return mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);
    }



//7、写入数据，在上面的方法中我们已经得到了设备服务和服务里的特征characteristic，那么就可以对这个特征写入或者说是赋值

    public void write(byte[] data) {   //一般都是传byte
        //得到可写入的characteristic Utils.isAIRPLANE(mContext) &&
//        if(!mBleManager.isEnabled()){
//            Log.e(TAG, "writeCharacteristic switch on flight mode");
//            //mBluetoothGatt.close();
//            return;
//        }
        BluetoothGattCharacteristic writeCharacteristic = getCharacteristic(Values.UUID_KEY_SERVICE, Values.UUID_KEY_WRITE);  //这个UUID都是根据协议号的UUID
        if (writeCharacteristic == null) {
            Log.e(TAG, "Write failed. GattCharacteristic is null.");
            return;
        }
        writeCharacteristic.setValue(data); //为characteristic赋值
        writeCharacteristicWrite(writeCharacteristic);
    }



    public void writeCharacteristicWrite(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.e(TAG, "BluetoothAdapter not initialized");
            return;
        }
        Log.e(TAG, "BluetoothAdapter writeData");
        boolean isBoolean = false;
        isBoolean = mBluetoothGatt.writeCharacteristic(characteristic);
        Log.e(TAG, "BluetoothAdapter_writeCharacteristic = " +isBoolean);  //如果isBoolean返回的是true则写入成功
    }

    public String bytesToHexString(byte[] bArr) {
        StringBuffer sb = new StringBuffer(bArr.length);
        String sTmp;
        for (int i = 0; i < bArr.length; i++) {
            sTmp = Integer.toHexString(0xFF & bArr[i]);
            if (sTmp.length() < 2)
                sb.append(0);
            sb.append(sTmp.toUpperCase());
        }
        return sb.toString();
    }
}
