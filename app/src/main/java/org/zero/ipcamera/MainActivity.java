package org.zero.ipcamera;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.widget.Toast;

import org.zero.ipcamera.media.widget.IpCameraView;
import org.zero.ipcamera.utils.IpcUtils;
import org.zero.ipcamera.utils.XmlParserUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.UUID;

import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity {
    private List<String> mLstIpc = new ArrayList<>();
    private List<IpCameraView> mLstCamera = new ArrayList<>();

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
        IpCameraView ipCameraView1 = findViewById(R.id.ipc_view1);
        IpCameraView ipCameraView2 = findViewById(R.id.ipc_view2);
        IpCameraView ipCameraView3 = findViewById(R.id.ipc_view3);
        IpCameraView ipCameraView4 = findViewById(R.id.ipc_view4);
        mLstCamera.add(ipCameraView1);
        mLstCamera.add(ipCameraView2);
        mLstCamera.add(ipCameraView3);
        mLstCamera.add(ipCameraView4);
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
                            if (!mLstIpc.contains(serviceUrl)) {
                                mLstIpc.add(serviceUrl);
                                IpCameraView cameraView = mLstCamera.get(mLstIpc.size() - 1);
                                cameraView.setServiceUrl(serviceUrl);
                                cameraView.play();
                            }
                            if (mLstIpc.size() >= 4) {
                                break;
                            }
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

    @Override
    protected void onStop() {
        super.onStop();
        for (IpCameraView ipCameraView : mLstCamera) {
            ipCameraView.stop();
        }
        IjkMediaPlayer.native_profileEnd();
    }
}
