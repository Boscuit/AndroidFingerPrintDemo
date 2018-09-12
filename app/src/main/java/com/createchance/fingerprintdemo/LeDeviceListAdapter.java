package com.createchance.fingerprintdemo;

import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import static android.content.ContentValues.TAG;

public class LeDeviceListAdapter extends BaseAdapter {

    private Context mContext;
    private ArrayList<BluetoothDevice> mLeDevices;
    private LayoutInflater mInflator;

    //构造方法  创建这个类的时候就 创建一个蓝牙列表  还有创建  一个XML======================
    public LeDeviceListAdapter(Context mContext) {
        super();
        this.mContext = mContext;
        mLeDevices = new ArrayList<BluetoothDevice>();
        mInflator = LayoutInflater.from(mContext);
    }

    //================蓝牙设备列表 添加 蓝牙  这个方法被scancallback调用 添加设备
    public void addDevice(BluetoothDevice device) {
        if(!mLeDevices.contains(device)) {
            mLeDevices.add(device);
        }
        notifyDataSetChanged();
    }
    //得到蓝牙设备 传入的是listadepter的position 返回的是设备列表中的第 position个设备
    public BluetoothDevice getDevice(int position) {
        return mLeDevices.get(position);
    }
    //clear 蓝牙列表===================
    public void clear() {
        mLeDevices.clear();
    }
    //得到蓝牙列表的数量
    @Override
    public int getCount() {
        return mLeDevices.size();
    }
    //返回第I个device
    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }
    //这个函数是得到点击的 是第几个item
    @Override
    public long getItemId(int i) {
        return i;
    }
    //这个getview 里面的内容就是将设备的名字以及地址写入了text中
    @Override
    public View getView(final int i, View view, ViewGroup viewGroup) {
        ViewHolder mViewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_device, null);
            mViewHolder = new ViewHolder();
            mViewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            mViewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            mViewHolder.mLogOutBtn = (Button) view.findViewById(R.id.log_out);
            view.setTag(mViewHolder);
        } else {
            mViewHolder = (ViewHolder) view.getTag();

        }
        //这里就是关键的地方了，可以得到蓝牙list中的某个设备
        BluetoothDevice device = mLeDevices.get(i);
        final String deviceName = device.getName();
        if (deviceName != null && deviceName.length() > 0)
        {
            mViewHolder.deviceName.setText(deviceName);
        }
        else
        {
            mViewHolder.deviceName.setText(R.string.unknown_device);
        }
        mViewHolder.deviceAddress.setText(device.getAddress());

        return view;
    }

    static class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        Button mLogOutBtn;
    }
}//end of ledevicelistadapter
