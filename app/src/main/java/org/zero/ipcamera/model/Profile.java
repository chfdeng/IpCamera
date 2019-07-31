package org.zero.ipcamera.model;

/**
 * Created by cfd on 2019/7/9.
 */
public class Profile {
    private String token;
    private int width;
    private int height;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public String toString() {
        return "Profile{" +
                "token='" + token + '\'' +
                ", width=" + width +
                ", height=" + height +
                '}';
    }
}
