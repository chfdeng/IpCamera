package org.zero.ipcamera;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import org.zero.ipcamera.media.widget.IpCameraView;
import org.zero.ipcamera.utils.IpcUtils;
import org.zero.ipcamera.utils.XmlParserUtils;

import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Locale;
import java.util.UUID;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private IpCameraView ipCameraView;

    static {
        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    private void initView() {
        ipCameraView = findViewById(R.id.ipc_view);
        searchIpc();
    }

    private void searchIpc() {
        Toast.makeText(this, "搜索摄像头中，请稍候...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    DatagramSocket socket = new DatagramSocket();
                    socket.setSoTimeout(10000);
                    socket.setBroadcast(true);
                    byte[] discoveryByte = IpcUtils.readFromAsset(IpCameraView.WSDL_DISCOVERY);
                    //有的ipc相同的uuid只会回应一次搜索，所以用随机生成的
                    String uuid = UUID.randomUUID().toString();
                    byte[] request = String.format(new String(discoveryByte), uuid).getBytes();
                    if (TextUtils.isEmpty(getBroadcastIp())) {
                        Toast.makeText(MainActivity.this, "请检查网络连接", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    DatagramPacket datagramPacket = new DatagramPacket(request, 0, request.length, InetAddress.getByName(getBroadcastIp()), IpCameraView.BROADCAST_SERVER_PORT);
                    socket.send(datagramPacket);
                    byte[] receiveByte = new byte[4096];
                    DatagramPacket receivePacket = new DatagramPacket(receiveByte, 0, receiveByte.length);
                    while (true) {
                        socket.receive(receivePacket);
                        if (receivePacket.getLength() > 0) {
                            String serviceUrl = XmlParserUtils.getIpcUrl(receivePacket.getData(), "XAddrs");
                            ipCameraView.setServiceUrl(serviceUrl);
                            ipCameraView.play();
                            break;
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static String getBroadcastIp() throws SocketException {
        System.setProperty("java.net.preferIPv4Stack", "true");
        for (Enumeration<NetworkInterface> niEnum = NetworkInterface
                .getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
            NetworkInterface ni = niEnum.nextElement();
            if (!ni.isLoopback()) {
                for (InterfaceAddress interfaceAddress : ni
                        .getInterfaceAddresses()) {
                    if (interfaceAddress.getBroadcast() != null) {
                        return interfaceAddress.getBroadcast().toString()
                                .substring(1);
                    }
                }
            }
        }
        return null;
    }

    public void startRecord(View view) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
        } else {
            record();
        }
    }

    private void record() {
        File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/RecordVideos");
        if (!file.exists() && !file.mkdirs()) {
            Log.e(TAG, "录像保存路径错误");
            return;
        }
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(new Date(System.currentTimeMillis()));
        String path = file.getAbsolutePath() + "/" + date + ".mp4";
        ipCameraView.startRecord(path);
    }

    public void stopRecord(View view) {
        ipCameraView.stopRecord();
    }

    public void screenShot(View view) {
        ipCameraView.screenShot();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            record();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        ipCameraView.stop();
        IjkMediaPlayer.native_profileEnd();
    }
}
