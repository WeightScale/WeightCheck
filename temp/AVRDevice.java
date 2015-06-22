package com.victjava.scales.bootloader;


import android.content.Context;
import android.os.Handler;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;

//import com.kostya.weightcheckadmin.Utility;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.12.13
 * Time: 11:28
 * To change this template use File | Settings | File Templates.
 */
class AVRDevice {
    private final Context context;
    private final Handler handler;

    protected final String fileName;
    private int flashSize; // Size of Flash memory in bytes.
    private int eepromSize; // Size of EEPROM memory in bytes.
    private int signature0;
    private int signature1;
    private int signature2; // The three signature bytes, read from XML PartDescriptionFiles.
    private int pageSize; // Flash page size.

    /* Constructor */
    public AVRDevice(final String _deviceName, Context _context, Handler _handler) {
        context = _context;
        handler = _handler;
        fileName = _deviceName;
        flashSize = eepromSize = 0;
        signature0 = signature1 = signature2 = 0;
        pageSize = -1;
    }

    /* Methods */
    void readParametersFromAVRStudio() throws Exception {
        Utility Util = new Utility();
        if (fileName != null && !fileName.isEmpty()) {

	        /* Parse the file for required info */
            handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG, "Parsing '" + fileName + "'..."));
            XmlPullParser xpp = XmlPullParserFactory.newInstance().newPullParser();

            flashSize = Integer.parseInt(getValue(xpp, "PROG_FLASH"));
            eepromSize = Integer.parseInt(getValue(xpp, "EEPROM"));

            if (exists(xpp, "BOOT_CONFIG")) {
                pageSize = Integer.parseInt(getValue(xpp, "PAGESIZE"));
                pageSize <<= 1; // We want pagesize in bytes.
            }

            signature0 = Util.convertHex(new StringBuilder(getValue(xpp, "ADDR000")).deleteCharAt(0).toString());
            signature1 = Util.convertHex(new StringBuilder(getValue(xpp, "ADDR001")).deleteCharAt(0).toString());
            signature2 = Util.convertHex(new StringBuilder(getValue(xpp, "ADDR002")).deleteCharAt(0).toString());

            handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG, "Saving cached XML parameters..."));
        }
    }

    String getValue(XmlPullParser xpp, String tag) throws IOException, XmlPullParserException {
        //AssetManager manager = context.getAssets();
        InputStream input = context.getAssets().open(fileName);
        xpp.setInput(input, null);
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (xpp.getName().equals(tag)) {
                    return xpp.nextText();
                }
            }
            eventType = xpp.next();
        }
        return "";
    }

    boolean exists(XmlPullParser xpp, String tag) throws IOException, XmlPullParserException {
        //AssetManager manager = context.getAssets();
        InputStream input = context.getAssets().open(fileName);
        xpp.setInput(input, null);
        int eventType = xpp.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                if (xpp.getName().equals(tag)) {
                    return true;
                }
            }
            eventType = xpp.next();
        }
        return false;
    }

    int getFlashSize() {
        return flashSize;
    }

    int getEEPROMSize() {
        return eepromSize;
    }

    long getPageSize() {
        return pageSize;
    }

    /*long getSignature0() {
        return signature0;
    }*/

    /*long getSignature1() {
        return signature1;
    }*/

    /*long getSignature2() {
        return signature2;
    }*/

}
