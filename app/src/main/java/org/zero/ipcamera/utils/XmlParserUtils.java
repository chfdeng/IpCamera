package org.zero.ipcamera.utils;

import android.text.TextUtils;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.zero.ipcamera.model.Profile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by cfd on 2019/7/9.
 */
public class XmlParserUtils {
    public static String getIpcUrl(byte[] data, String tag) {
        return getIpcUrl(data, null, tag);
    }

    public static String getIpcUrl(byte[] data, String parentTag, String tag) {
        if (TextUtils.isEmpty(tag)) {
            return null;
        }
        XmlPullParser parser = Xml.newPullParser();
        String url = null;
        try {
            parser.setInput(new ByteArrayInputStream(data), "utf-8");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if (TextUtils.isEmpty(parentTag)) {
                            if (tag.equals(parser.getName())) {
                                url = parser.nextText();
                            }
                        } else {
                            if (parentTag.equals(parser.getName())) {
                                while (!(eventType == XmlPullParser.END_TAG && parentTag.equals(parser.getName()))) {
                                    if (eventType == XmlPullParser.START_TAG && tag.equals(parser.getName())) {
                                        url = parser.nextText();
                                    }
                                    eventType = parser.next();
                                }
                            }
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return url;
    }

    public static List<Profile> getMediaProfiles(byte[] data) {
        List<Profile> lstProfile = new ArrayList<>();
        try {
            XmlPullParser parser = Xml.newPullParser();
            parser.setInput(new ByteArrayInputStream(data), "utf-8");
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                switch (eventType) {
                    case XmlPullParser.START_TAG:
                        if ("Profiles".equals(parser.getName())) {
                            Profile profile = new Profile();
                            profile.setToken(parser.getAttributeValue(null, "token"));
                            while (!(eventType == XmlPullParser.END_TAG && "Profiles".equals(parser.getName()))) {
                                if (eventType == XmlPullParser.START_TAG) {
                                    switch (parser.getName()) {
                                        case "Width":
                                            profile.setWidth(Integer.parseInt(parser.nextText()));
                                            break;
                                        case "Height":
                                            profile.setHeight(Integer.parseInt(parser.nextText()));
                                            break;
                                    }
                                }
                                eventType = parser.next();
                            }
                            lstProfile.add(profile);
                        }
                        break;
                }
                eventType = parser.next();
            }
        } catch (XmlPullParserException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lstProfile;
    }
}
