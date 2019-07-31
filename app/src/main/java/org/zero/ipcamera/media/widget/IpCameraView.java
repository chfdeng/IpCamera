package org.zero.ipcamera.media.widget;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.Toast;

import org.zero.ipcamera.model.AuthInfo;
import org.zero.ipcamera.model.Profile;
import org.zero.ipcamera.utils.IpcUtils;
import org.zero.ipcamera.utils.XmlParserUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;

/**
 * Created by cfd on 2019/7/30.
 */
public class IpCameraView extends FrameLayout {
    private static final String TAG = "IpCameraView";
    public static final int BROADCAST_SERVER_PORT = 3702;  // IPC摄像头固定用的是239.255.255.250（端口3702）
    public static final String WSDL_DISCOVERY = "Discovery";
    public static final String WSDL_GET_DEVICE_INFO = "GetDeviceInfo";
    public static final String WSDL_GET_CAPABILITIES = "GetCapabilities";
    public static final String WSDL_GET_PROFILES = "GetProfiles";
    public static final String WSDL_GET_STREAM_URL = "GetRtspUrl";
    public static final String WSDL_AUTH_INFO = "AuthInfo";
    private Handler mHandler = new Handler();
    private String mServiceUrl;
    private AuthInfo mAuthInfo;
    private String mName = "admin";
    private String mPassword = "123456";
    private HashMap<String, String> password = new HashMap<>();
    private IjkVideoView mIjkVideoView;
    private String mRtspUrl;
    private String mManufacturer;//制造商

    public IpCameraView(@NonNull Context context) {
        this(context, null);
    }

    public IpCameraView(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public IpCameraView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    private void init(Context context) {
        password.put("http://192.168.1.64/onvif/device_service", "a1234567");
        password.put("http://192.168.1.108/onvif/device_service", "admin");
        password.put("http://192.168.1.138:80/onvif/device_service", "123456");
        mIjkVideoView = new IjkVideoView(context);
        addView(mIjkVideoView, new ViewGroup.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

        TableLayout tabLayout = new TableLayout(context);
        tabLayout.setBackgroundColor(0x80000000);
        LayoutParams layoutParams = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.gravity = Gravity.CENTER_VERTICAL;
        addView(tabLayout, layoutParams);
        mIjkVideoView.setHudView(tabLayout);
    }

    public void setServiceUrl(String url) {
        if (TextUtils.isEmpty(url)) {
            Toast.makeText(getContext(), "service地址为空", Toast.LENGTH_SHORT).show();
            return;
        }
        mServiceUrl = url;
    }

    public void play() {
        if (TextUtils.isEmpty(mServiceUrl)) {
            return;
        }
        getRtspUrl();
    }

    public void stop() {
        if (mIjkVideoView == null) {
            return;
        }
        if (!mIjkVideoView.isBackgroundPlayEnabled()) {
            mIjkVideoView.stopPlayback();
            mIjkVideoView.release(true);
            mIjkVideoView.stopBackgroundPlay();
        } else {
            mIjkVideoView.enterBackground();
        }
    }

    private void getRtspUrl() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String mediaUrl = getMediaUrl();
                List<Profile> profiles = getProfiles(mediaUrl);
                mRtspUrl = null;
                for (Profile profile : profiles) {
                    String streamUrl = getStreamUrl(mediaUrl, profile.getToken());
                    if (!TextUtils.isEmpty(streamUrl)) {
                        mRtspUrl = streamUrl;
                        break;
                    }
                }
                getDeviceInfo();
                mHandler.post(mPlayRunnable);
            }
        }).start();
    }

    private void getDeviceInfo() {
        String authInfo = "";
        if (mAuthInfo != null) {
            authInfo = getAuthInfo();
        }
        String request = String.format(new String(IpcUtils.readFromAsset(WSDL_GET_DEVICE_INFO)), authInfo);
        String response = httpRequest(mServiceUrl, request.getBytes());
        mManufacturer = XmlParserUtils.getIpcUrl(response.getBytes(), "Manufacturer");
    }

    private Runnable mPlayRunnable = new Runnable() {
        @Override
        public void run() {
            if (TextUtils.isEmpty(mRtspUrl)) {
                Toast.makeText(getContext(), "rtsp路径获取失败", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!mRtspUrl.startsWith("rtsp://")) {
                Toast.makeText(getContext(), "rtsp路径格式错误", Toast.LENGTH_SHORT).show();
                return;
            }
            if (mAuthInfo != null) {
                mRtspUrl = "rtsp://" + mName + ":" + mPassword + "@" + mRtspUrl.substring(7);
            }
            Log.e(TAG, "rtsp地址：" + mRtspUrl);
            mIjkVideoView.setVideoPath(mRtspUrl);
            mIjkVideoView.start();
        }
    };

    private String getMediaUrl() {
        String response = httpRequest(mServiceUrl, IpcUtils.readFromAsset(WSDL_GET_CAPABILITIES));
        return XmlParserUtils.getIpcUrl(response.getBytes(), "Media", "XAddr");
    }

    private List<Profile> getProfiles(String mediaUrl) {
        byte[] bytes = IpcUtils.readFromAsset(WSDL_GET_PROFILES);
        String format = String.format(new String(bytes), getAuthInfo());
        String response = httpRequest(mediaUrl, format.getBytes());
        if ("401".equals(response)) {
            Log.e(TAG, "需要鉴权");
            //未鉴权
            mPassword = password.get(mServiceUrl);
            mAuthInfo = IpcUtils.createAuthInfo(mName, mPassword);
            String request = String.format(new String(bytes), getAuthInfo());
            response = httpRequest(mediaUrl, request.getBytes());
        } else {
            Log.e(TAG, "需要鉴权");
        }
        return XmlParserUtils.getMediaProfiles(response.getBytes());
    }

    private String getStreamUrl(String mediaUrl, String token) {
        byte[] request = IpcUtils.readFromAsset(WSDL_GET_STREAM_URL);
        request = String.format(new String(request), getAuthInfo(), token).getBytes();
        String response = httpRequest(mediaUrl, request);
        return XmlParserUtils.getIpcUrl(response.getBytes(), "Uri");
    }

    private String getAuthInfo() {
        String authInfo = "";
        if (mAuthInfo != null) {
            authInfo = String.format(new String(IpcUtils.readFromAsset(WSDL_AUTH_INFO)), mAuthInfo.getName(), mAuthInfo.getAuthPassword(), mAuthInfo.getNonce(), mAuthInfo.getCreate());
        }
        return authInfo;
    }

    private String httpRequest(String url, byte[] request) {
        StringBuilder sb = new StringBuilder();
        try {
            HttpURLConnection connection = newConnection(url);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/soap+xml");
            OutputStream os = connection.getOutputStream();
            os.write(request, 0, request.length);
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                InputStream is = connection.getInputStream();
                byte[] receiveByte = new byte[4096];
                int length;
                while ((length = is.read(receiveByte)) != -1) {
                    sb.append(new String(receiveByte, 0, length));
                }
            } else {
                return String.valueOf(responseCode);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }

    private HttpURLConnection newConnection(String url) {
        HttpURLConnection urlConnection = null;
        try {
            URL url1 = new URL(url);
            urlConnection = (HttpURLConnection) url1.openConnection();
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setInstanceFollowRedirects(true);
            urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return urlConnection;
    }

    public void startRecord(String path) {
        mIjkVideoView.startRecord(path);
    }

    public void stopRecord() {
        mIjkVideoView.stopRecord();
    }

    public Bitmap screenShot() {
        return mIjkVideoView.capture();
    }
}
