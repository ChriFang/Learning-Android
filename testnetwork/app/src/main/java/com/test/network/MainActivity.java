package com.test.network;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.telephony.*;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Enumeration;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // sim signal
        Button mobileSignalBtn = findViewById(R.id.get_mobile_signal_button);
        mobileSignalBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getSimSignal();
            }
        });

        // Wi-Fi signal
        Button wifiSignalBtn = findViewById(R.id.get_wifi_signal_button);
        wifiSignalBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getWifiSignal();
            }
        });

        // test http
        Button httpGetBtn = findViewById(R.id.http_get_button);
        httpGetBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                httpGet();
            }
        });

        // get all net interface
        Button netInterfaceBtn = findViewById(R.id.get_net_interface_button);
        netInterfaceBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                getNetInterface();
            }
        });

        // tcp connect
        Button tcpConnBtn = findViewById(R.id.tcp_conn_button);
        tcpConnBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                tcpConnect();
            }
        });
    }

    void getSimSignal() {
        if (hasSimCard()) {
            TelephonyManager mTelephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
            if (mTelephonyManager != null) {
                mTelephonyManager.listen(new PhoneStateListener() {
                    @Override
                    public void onSignalStrengthsChanged(SignalStrength signalStrength) {
                        super.onSignalStrengthsChanged(signalStrength);
                        int asu = signalStrength.getGsmSignalStrength();
                        int lastSignal = -113 + 2 * asu;
                        Log.i("networktest", "signal: " + lastSignal + " dBm");
                    }
                }, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS);
            }
        }
    }

    void getWifiSignal() {
        class MyReceiver extends BroadcastReceiver {
            @Override
            public void onReceive(Context context, Intent intent) {
                int rssi = intent.getIntExtra(WifiManager.EXTRA_NEW_RSSI, -1);
                //这函数可以计算出信号的等级
                int strength = WifiManager.calculateSignalLevel(rssi, 5);
                Log.i("networktest", "Wi-Fi signal: " + strength);
            }
        }

        MyReceiver receiver = new MyReceiver();
        IntentFilter intentFilter = new IntentFilter(WifiManager.RSSI_CHANGED_ACTION);
        registerReceiver(receiver, intentFilter);
    }

    void getNetInterface() {
        ArrayList<String> availableInterface = new ArrayList<>();
        try {
            Enumeration nis = NetworkInterface.getNetworkInterfaces();
            InetAddress ia = null;
            while (nis.hasMoreElements()) {
                NetworkInterface ni = (NetworkInterface) nis.nextElement();
                Enumeration<InetAddress> ias = ni.getInetAddresses();
                while (ias.hasMoreElements()) {
                    ia = ias.nextElement();
                    String ip = ia.getHostAddress();
                    Log.i("networktest","getAllNetInterface,available interface:"+ni.getName()+",address:"+ip+",UP:"+ni.isUp());
                    availableInterface.add(ni.getName());
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        Log.i("networktest","all interface:" + availableInterface.toString());
    }

    void httpGet() {
        OkHttpClient client = new OkHttpClient();
        //RequestBody body =  RequestBody.create(MediaType.parse("application/json; charset=utf-8"), "");
        Request request = new Request.Builder()
                .url("https://video-qn.51miz.com/preview/video/00/00/14/22/V-142261-61188B78.mp4")
                .get()
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                Log.i("networktest", "http get failed");
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String message = "";
                if (response.isSuccessful()) {
                    if (response.code() == 200) {
                        String result = response.body().string();
                        Log.i("networktest", "http response, length " + result.length());
                    }
                } else {
                    Log.i("networktest", "http response, code " + response.code());
                }
            }
        });
    }

    void tcpConnect() {
        String sendMsg = "Hi~";
        new Thread() {
            @Override
            public void run() {
                super.run();
                try {
                    //1.创建监听指定服务器地址以及指定服务器监听的端口号
                    Socket socket = new Socket("172.29.173.74", 6666);//111.111.11.11为我这个本机的IP地址，端口号为12306.
                    //2.拿到客户端的socket对象的输出流发送给服务器数据
                    OutputStream os = socket.getOutputStream();
                    //写入要发送给服务器的数据
                    os.write(sendMsg.getBytes());
                    os.flush();
                    socket.shutdownOutput();
//                    //拿到socket的输入流，这里存储的是服务器返回的数据
//                    InputStream is = socket.getInputStream();
//                    //解析服务器返回的数据
//                    InputStreamReader reader = new InputStreamReader(is);
//                    BufferedReader bufReader = new BufferedReader(reader);
//                    String s = null;
//                    final StringBuffer sb = new StringBuffer();
//                    while((s = bufReader.readLine()) != null){
//                        sb.append(s);
//                    }
//                    runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            tv.setText(sb.toString());
//                        }
//                    });
//                    //3、关闭IO资源（注：实际开发中需要放到finally中）
//                    bufReader.close();
//                    reader.close();
//                    is.close();
                    os.close();
                    socket.close();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    boolean hasSimCard() {
        Context context = getApplicationContext();
        TelephonyManager telMgr = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telMgr.getSimState();
        boolean result = true;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
            case TelephonyManager.SIM_STATE_UNKNOWN:
                result = false; // 没有SIM卡
                break;
        }
        return result;
    }

//    public int getNetWorkType() {
//        int mNetWorkType = -1;
//        Context context = getApplicationContext();
//        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
//        NetworkInfo networkInfo = manager.getActiveNetworkInfo();
//        if (networkInfo != null && networkInfo.isConnected()) {
//            String type = networkInfo.getTypeName();
//            if (type.equalsIgnoreCase("WIFI")) {
//                mNetWorkType = NETWORKTYPE_WIFI;
//            } else if (type.equalsIgnoreCase("MOBILE")) {
//                return isFastMobileNetwork(context) ? NETWORKTYPE_4G : NETWORKTYPE_2G;
//            }
//        } else {
//            mNetWorkType = NETWORKTYPE_NONE;//没有网络
//        }
//        return mNetWorkType;
//    }
}