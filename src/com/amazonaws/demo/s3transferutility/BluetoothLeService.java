package com.amazonaws.demo.s3transferutility;



import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.UUID;


/**
 * Service for managing connection and data communication with a GATT server hosted on a
 * given Bluetooth LE device.
 */

public class BluetoothLeService extends Service {
    private final static String TAG = BluetoothLeService.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;
    private int mConnectionState = STATE_DISCONNECTED;
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;
    public final static String ACTION_GATT_CONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_CONNECTED";
    public final static String ACTION_GATT_DISCONNECTED =
            "com.example.bluetooth.le.ACTION_GATT_DISCONNECTED";
    public final static String ACTION_GATT_SERVICES_DISCOVERED =
            "com.example.bluetooth.le.ACTION_GATT_SERVICES_DISCOVERED";
    public final static String ACTION_DATA_AVAILABLE =
            "com.example.bluetooth.le.ACTION_DATA_AVAILABLE";
    public final static String ACTION_READY_TOSEND =
            "com.example.bluetooth.le.ACTION_READY_TOSEND";
    public final static String EXTRA_DATA =
            "com.example.bluetooth.le.EXTRA_DATA";
    public final static String IO_FILENAME=
            "accelerometerData.txt";
    public final static int LIMIT_CHARACTERS=500;
    public final static int DEFAULT_BYTES_VIA_BLE=20;
    public final static int INITIAL_MESSAGE_PACKET_LENGTH = 2;
    public final static int DEFAULT_BYTES_IN_CONTINUE_PACKET = DEFAULT_BYTES_VIA_BLE - INITIAL_MESSAGE_PACKET_LENGTH;
    public final static byte INITIAL_MESSAGE_PACKET = 0x03;
    public final static byte SENDING_CONTINUE_PACKET = 0x01;
    public final static byte SENDING_LAST_PACKET = 0x00;
    private static final UUID CONFIG_DESCRIPTOR =
            UUID.fromString(GattAttributes.CONFIG_DESCRIPTOR);
    public final static UUID UUID_SEND =
            UUID.fromString(GattAttributes.SEND);
    public final static UUID UUID_RECEIVE =
            UUID.fromString(GattAttributes.RECEIVE);
    public final static UUID UUID_X =
            UUID.fromString(GattAttributes.X_CHAR);
    public final static UUID UUID_Y =
            UUID.fromString(GattAttributes.Y_CHAR);
    public final static UUID UUID_Z =
            UUID.fromString(GattAttributes.Z_CHAR);
    public final static UUID UUID_PROC_PITCH_CHAR =
            UUID.fromString(GattAttributes.PROC_PITCH_CHAR);
    public final static UUID UUID_PROC_ROLL_CHAR =
            UUID.fromString(GattAttributes.PROC_ROLL_CHAR);

    /*public final static UUID UUID_Y =
            UUID.fromString(GattAttributes.Y_CHAR);
    public final static UUID UUID_Z =
            UUID.fromString(GattAttributes.Z_CHAR);*/

    // Implements callback methods for GATT events that the app cares about.  For example,
    // connection change and services discovered.
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String intentAction;
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                intentAction = ACTION_GATT_CONNECTED;
                mConnectionState = STATE_CONNECTED;
                broadcastUpdate(intentAction);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                intentAction = ACTION_GATT_DISCONNECTED;
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
                broadcastUpdate(intentAction);
            }
        }
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }


        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
        }
    };
    private void broadcastUpdate(final String action) {
        final Intent intent = new Intent(action);
        sendBroadcast(intent);
    }

    //
    private void broadcastUpdate(final String action,
                                 final BluetoothGattCharacteristic characteristic) {
        final Intent intent = new Intent(action);
        // This is special handling for the Heart Rate Measurement profile.  Data parsing is
        // carried out as per profile specifications:
        // http://developer.bluetooth.org/gatt/characteristics/Pages/CharacteristicViewer.aspx?u=org.bluetooth.characteristic.heart_rate_measurement.xml
        String str = characteristic.getUuid().toString();
        if (UUID_RECEIVE.equals(characteristic.getService().getUuid())) {

            if (UUID_X.equals(characteristic.getUuid())) {
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(TAG, "INPUT format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(TAG, "IMPUT rate format UINT8.");
                }
                byte[] x = new byte[4];
                for(int i=0; i<4;i++) {
                    x[i] = characteristic.getValue()[i];
                    int d=+x[i];
                    Log.d(TAG, String.format("Received X value: %d", x[i]));
                }
                intent.putExtra("COOR","X");
                intent.putExtra(EXTRA_DATA+"X", Integer.toString(x[1]));
                //x= characteristic.getIntValue(format,0);

            }
        } else if (UUID_RECEIVE.equals(characteristic.getService().getUuid())) {

                if (UUID_Y.equals(characteristic.getUuid())) {
                    int flag = characteristic.getProperties();
                    int format = -1;
                    if ((flag & 0x01) != 0) {
                        format = BluetoothGattCharacteristic.FORMAT_UINT16;
                        Log.d(TAG, "INPUT format UINT16.");
                    } else {
                        format = BluetoothGattCharacteristic.FORMAT_UINT8;
                        Log.d(TAG, "IMPUT rate format UINT8.");
                    }
                    byte[] x = new byte[4];
                    for (int i = 0; i < 4; i++) {
                        x[i] = characteristic.getValue()[i];
                        int d = +x[i];
                        Log.d(TAG, String.format("Received X value: %d", x[i]));
                    }
                    intent.putExtra("COOR","Y");
                    intent.putExtra(EXTRA_DATA + "Y", Integer.toString(x[1]));
                    //x= characteristic.getIntValue(format,0);
                }else{
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
            }

        }else if (UUID_RECEIVE.equals(characteristic.getService().getUuid())) {

            if (UUID_Z.equals(characteristic.getUuid())) {
                int flag = characteristic.getProperties();
                int format = -1;
                if ((flag & 0x01) != 0) {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(TAG, "INPUT format UINT16.");
                } else {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(TAG, "IMPUT rate format UINT8.");
                }
                byte[] x = new byte[4];
                for (int i = 0; i < 4; i++) {
                    x[i] = characteristic.getValue()[i];
                    int d = +x[i];
                    Log.d(TAG, String.format("Received X value: %d", x[i]));
                }
                intent.putExtra("COOR","Z");
                intent.putExtra(EXTRA_DATA + "Z", Integer.toString(x[1]));
                //x= characteristic.getIntValue(format,0);
            }else{
                // For all other profiles, writes the data formatted in HEX.
                final byte[] data = characteristic.getValue();
                if (data != null && data.length > 0) {
                    final StringBuilder stringBuilder = new StringBuilder(data.length);
                    for (byte byteChar : data)
                        stringBuilder.append(String.format("%02X ", byteChar));
                    intent.putExtra(EXTRA_DATA, new String(data) + "\n" + stringBuilder.toString());
                }
            }

        } else if (UUID_SEND.equals(characteristic.getService().getUuid())){
            if(UUID_PROC_PITCH_CHAR.equals(characteristic.getUuid())) {
                //Uri uri = Uri.parse((new File(Environment.getExternalStorageDirectory().toString() + "/" + IO_FILENAME)).toString());
                //String data = readTextFile(uri);
                //writeCharacteristic(characteristic, "21");

                byte[] byt;
                String sr = "21";
                byt = sr.getBytes();
                characteristic.setValue(byt);
                if(mBluetoothGatt.writeCharacteristic(characteristic))
                    Log.v("Writing: ", "YES");
                else
                    Log.v("Writing: ", "NO");

            }
        }
        Log.v("uooooooooooooooo",str);
        sendBroadcast(intent);
    }


    private String readTextFile(Uri uri){
        BufferedReader reader = null;
        StringBuilder dataBld= null;
        try {
            reader = new BufferedReader(new InputStreamReader(getContentResolver().openInputStream(uri)));

            String line = "";

            while ((line = reader.readLine()) != null) {
                dataBld.append(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null){
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return dataBld.toString();

    }

    private void sendMessage(BluetoothGattCharacteristic characteristic, String CHARACTERS){
        byte[] initial_packet = new byte[2];
        /**
         * Indicate byte
         */
        initial_packet[0] = BluetoothLeService.INITIAL_MESSAGE_PACKET;
        if (Long.valueOf(
                String.valueOf(CHARACTERS.length() + initial_packet.length))
                > BluetoothLeService.DEFAULT_BYTES_VIA_BLE) {
            sendingContinuePacket(characteristic, initial_packet, CHARACTERS);
        } else {
            sendingContinuePacket(characteristic, initial_packet, CHARACTERS);          // sendingLastPacket
        }
    }


    private void sendingContinuePacket(BluetoothGattCharacteristic characteristic,
                                       byte[] initial_packet, String CHARACTERS){
        /**
         * TODO If data length > Default data can sent via BLE : 20 bytes
         */
        // Check the data length is large how many times with Default Data (BLE)
        int times = Byte.valueOf(String.valueOf(
                CHARACTERS.length() / BluetoothLeService.DEFAULT_BYTES_IN_CONTINUE_PACKET));

        Log.i(TAG, "CHARACTERS.length() " + CHARACTERS.length());
        Log.i(TAG, "times " + times);

        // TODO
        // 100 : Success
        // 101 : Error
        byte[] sending_continue_hex = new byte[BluetoothLeService.DEFAULT_BYTES_IN_CONTINUE_PACKET];
        for (int time = 0; time <= times; time++) {
            /**
             * Wait second before sending continue packet
             */
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            if (time == times) {
                Log.i(TAG, "LAST PACKET ");

                /**
                 * If can not have enough characters to send continue packet,
                 * This is the last packet will be sent to the band
                 */

                /**
                 * Packet length byte :
                 */
                /**
                 * Length of last packet
                 */
                int character_length = CHARACTERS.length()
                        - BluetoothLeService.DEFAULT_BYTES_IN_CONTINUE_PACKET*times;

                initial_packet[0] = Byte.valueOf(String.valueOf(BluetoothLeService.INITIAL_MESSAGE_PACKET));
                initial_packet[1] = BluetoothLeService.SENDING_LAST_PACKET;

                Log.i(TAG, "character_length " + character_length);

                /**
                 * Message
                 */
                // Hex file
                byte[] sending_last_hex = new byte[character_length];

                // Hex file : Get next bytes
                for (int i = 0; i < sending_last_hex.length; i++) {
                    sending_last_hex[i] =
                            CHARACTERS.getBytes()[sending_continue_hex.length*time + i];
                }

                // Merge byte[]
                byte[] last_packet =
                        new byte[character_length + BluetoothLeService.INITIAL_MESSAGE_PACKET_LENGTH];
                System.arraycopy(initial_packet, 0, last_packet,
                        0, initial_packet.length);
                System.arraycopy(sending_last_hex, 0, last_packet,
                        initial_packet.length, sending_last_hex.length);

                // Set value for characteristic
                characteristic.setValue(last_packet);
            } else {
                Log.i(TAG, "CONTINUE PACKET ");
                /**
                 * If have enough characters to send continue packet,
                 * This is the continue packet will be sent to the band
                 */
                /**
                 * Packet length byte
                 */
                int character_length = sending_continue_hex.length;

                /**
                 * TODO Default Length : 20 Bytes
                 */
                initial_packet[0] = Byte.valueOf(String.valueOf(BluetoothLeService.INITIAL_MESSAGE_PACKET));

                /**
                 * If sent data length > 20 bytes (Default : BLE allow send 20 bytes one time)
                 * -> set 01 : continue sending next packet
                 * else or if after sent until data length < 20 bytes
                 * -> set 00 : last packet
                 */
                initial_packet[1] = BluetoothLeService.SENDING_CONTINUE_PACKET;
                /**
                 * Message
                 */
                // Hex file : Get first 17 bytes
                for (int i = 0; i < sending_continue_hex.length; i++) {
                    Log.i(TAG, "Send stt : "
                            + (sending_continue_hex.length*time + i));

                    // Get next bytes
                    sending_continue_hex[i] =
                            CHARACTERS.getBytes()[sending_continue_hex.length*time + i];
                }

                // Merge byte[]
                byte[] sending_continue_packet =
                        new byte[character_length + BluetoothLeService.INITIAL_MESSAGE_PACKET_LENGTH];
                System.arraycopy(initial_packet, 0, sending_continue_packet,
                        0, initial_packet.length);
                System.arraycopy(sending_continue_hex, 0, sending_continue_packet,
                        initial_packet.length, sending_continue_hex.length);

                // Set value for characteristic
                characteristic.setValue(sending_continue_packet);
            }

            // Write characteristic via BLE
            mBluetoothGatt.writeCharacteristic(characteristic);

            Log.v("value sent: ",(characteristic.getIntValue(BluetoothGattCharacteristic.FORMAT_UINT8,1).toString()));
        }
    }

    public boolean writeCharacteristic(BluetoothGattCharacteristic characteristic,
                                       String data) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }

            if (data.length() <= BluetoothLeService.LIMIT_CHARACTERS) {
                sendMessage(characteristic, data);

                return true;
            } else {


                return false;
            }

    }




    public class LocalBinder extends Binder {
        BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }
    @Override
    public boolean onUnbind(Intent intent) {
        // After using a given device, you should make sure that BluetoothGatt.close() is called
        // such that resources are cleaned up properly.  In this particular example, close() is
        // invoked when the UI is disconnected from the Service.
        close();
        return super.onUnbind(intent);
    }
    private final IBinder mBinder = new LocalBinder();
    /**
     * Initializes a reference to the local Bluetooth adapter.
     *
     * @return Return true if the initialization is successful.
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter through
        // BluetoothManager.
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        return true;
    }
    /**
     * Connects to the GATT server hosted on the Bluetooth LE device.
     *
     * @param address The device address of the destination device.
     *
     * @return Return true if the connection is initiated successfully. The connection result
     *         is reported asynchronously through the
     *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     *         callback.
     */
    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }
        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(this, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }
    /**
     * Disconnects an existing connection or cancel a pending connection. The disconnection result
     * is reported asynchronously through the
     * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)}
     * callback.
     */
    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.disconnect();
    }
    /**
     * After using a given BLE device, the app must call this method to ensure resources are
     * released properly.
     */
    public void close() {
        if (mBluetoothGatt == null) {
            return;
        }
        mBluetoothGatt.close();
        mBluetoothGatt = null;
    }
    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported
     * asynchronously through the {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.readCharacteristic(characteristic);
    }


    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled If true, enable notification.  False otherwise.
     */
    public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic,
                                              boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.setCharacteristicNotification(characteristic, enabled);

        if (UUID_X.equals(characteristic.getUuid()) || UUID_Y.equals(characteristic.getUuid()) || UUID_Z.equals(characteristic.getUuid())) {
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(CONFIG_DESCRIPTOR);
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            mBluetoothGatt.writeDescriptor(descriptor);
        }else if(UUID_PROC_PITCH_CHAR.equals(characteristic.getUuid()) && enabled){
            //byte[] byt;

            byte[] bytes = ByteBuffer.allocate(4).putInt(33).array();
            //String sr = Integer.toString(bytes);
            //byt = sr.getBytes();
            characteristic.setValue(bytes);

            if(mBluetoothGatt.writeCharacteristic(characteristic))
                Log.v("Writing: ", "YES");
            else
                Log.v("Writing: ", "NO");
        }
    }
    /**
     * Retrieves a list of supported GATT services on the connected device. This should be
     * invoked only after {@code BluetoothGatt#discoverServices()} completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;
        List<BluetoothGattService> list = mBluetoothGatt.getServices();

        return mBluetoothGatt.getServices();
    }
}