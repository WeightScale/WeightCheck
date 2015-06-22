package com.victjava.scales.bootloader;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

/*
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.12.13
 * Time: 18:10
 * To change this template use File | Settings | File Templates.
 */
public class HEXFile {
    private final Context context;
    private final Handler handler;
    //ActivityBootloader activityBootloader;
    private final byte[] data; // Holds the data bytes.
    private int start;
    private int end; // Used data range.
    private final int size; // Size of databuffer.

    void parseRecord(final String hexLine, HEXRecord recp) throws Exception {
        Utility Util = new Utility();

        if (hexLine.length() < 11) // At least 11 characters.
        {
            throw new Exception("Wrong HEX file format, missing fields! " + "Line from file was: (" + hexLine + ").");
        }

	    /* Check format for line */
        if (hexLine.charAt(0) != ':') // Always start with colon.
        {
            throw new Exception("Wrong HEX file format, does not start with colon! " + "Line from file was: (" + hexLine + ").");
        }

	    /* Parse length, offset and type */
        recp.setLength(Util.convertHex(hexLine.substring(1, 3)));
        recp.setOffset(Util.convertHex(hexLine.substring(3, 7)));
        recp.setType(Util.convertHex(hexLine.substring(7, 9)));

	    /* We now know how long the record should be */
        if (hexLine.length() < 11 + (recp.getLength() << 1)) {
            throw new Exception("Wrong HEX file format, missing fields! " + "Line from file was: (" + hexLine + ").");
        }

	    /* Process checksum */
        int checksum = recp.getLength();
        checksum += recp.getOffset() >> 8 & 0xff;
        checksum += recp.getOffset() & 0xff;
        checksum += recp.getType();

	    /* Parse data fields */
        if (recp.getLength() != 0) {

            recp.setData(new byte[recp.getLength()]);
            /* Read data from record */
            for (long recordPos = 0; // Position inside record data fields.
                 recordPos < recp.getLength(); recordPos++) {
                recp.getData()[(int) recordPos] = (byte) Util.convertHex(hexLine.substring((int) (9 + (recordPos << 1)), (int) (9 + (recordPos << 1)) + 2));
                checksum += recp.getData()[(int) recordPos];
            }
        }

	    /* Correct checksum? */
        checksum += Util.convertHex(hexLine.substring(9 + (recp.getLength() << 1), 9 + (recp.getLength() << 1) + 2));
        if ((checksum & 0xff) != 0) {
            throw new Exception("Wrong checksum for HEX record! " + "Line from file was: (" + hexLine + ").");
        }
    }

    /* Constructor */
    public HEXFile(int buffersize, byte value, Context con, Handler _handler) throws Exception {
        context = con;
        handler = _handler;
        if (buffersize <= 0) {
            throw new Exception("Cannot have zero-size HEX buffer!");
        }

        data = new byte[buffersize];
        size = buffersize;
        clearAll(value);
    }

    /* Methods */
    void readFile(final String _filename) throws Exception { // Read data from HEX file.

        HEXRecord rec = new HEXRecord(); // Temp record.
        AssetManager manager = context.getAssets();
        InputStream input = manager.open(_filename);
        BufferedReader br = new BufferedReader(new InputStreamReader(input));

        /* Prepare */
        start = size;
        end = 0;
        /* Parse records */
        try {
            int baseAddress = 0; // Base address for extended addressing modes.
            String hexLine; // Contains one line of the HEX file.
            while ((hexLine = br.readLine()) != null) {

                handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_UPDATE_DIALOG, end, 0));
            /* Process record according to type */
                parseRecord(hexLine, rec);
                switch (rec.getType()) {
                    case 0x00: // Data record ?
                    /* Copy data */
                        if (baseAddress + rec.getOffset() + rec.getLength() > size) {
                            //br.close();
                            throw new Exception("HEX file defines data outside buffer limits! " +
                                    "Make sure file does not contain data outside device " +
                                    "memory limits. " +
                                    "Line from file was: (" + hexLine + ").");
                        }

                        for (long dataPos = 0; // Data position in record.
                             dataPos < rec.getLength(); dataPos++) {
                            data[(int) (baseAddress + rec.getOffset() + dataPos)] = rec.getData()[(int) dataPos];
                        }

				    /* Update byte usage */
                        if (baseAddress + rec.getOffset() < start) {
                            start = baseAddress + rec.getOffset();
                        }

                        if (baseAddress + rec.getOffset() + rec.getLength() - 1 > end) {
                            end = baseAddress + rec.getOffset() + rec.getLength() - 1;
                        }

                        break;
                    case 0x02: // Extended segment address record ?
                        baseAddress = rec.getData()[0] << 8 | rec.getData()[1];
                        baseAddress <<= 4;
                        break;
                    case 0x03: // Start segment address record ?

                        break; // Ignore it, since we have no influence on execution start address.
                    case 0x04: // Extended linear address record ?
                        baseAddress = rec.getData()[0] << 8 | rec.getData()[1];
                        baseAddress <<= 16;
                        break;
                    case 0x05: // Start linear address record ?

                        break; // Ignore it, since we have no influence on exectuion start address.
                    case 0x01: // End of file record ?
                        br.close();
                        handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_CLOSE_DIALOG));
                        return;
                    default:
                        //br.close();
                        throw new Exception("Unsupported HEX record format! " + "Line from file was: (" + hexLine + ").");
                }
            }
        } catch (Exception e) {
            throw new Exception(e);
        } finally{
            br.close();
        }
        //br.close();
        handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_CLOSE_DIALOG));
        /* We should not end up here */
        throw new Exception("Premature end of file encountered! Make sure file " + "contains an EOF-record.");
    }

    void setUsedRange(int _start, int _end) throws Exception {// Sets the used range.
        if (_start < 0 || _end >= size || _start > _end) {
            throw new Exception("Invalid range! Start must be 0 or larger, end must be " + "inside allowed memory range.");
        }

        start = _start;
        end = _end;
    }

    void clearAll(byte value) {// Set databuffer to this value.
        for (int i = 0; i < size; i++) {
            data[i] = (byte) (value & 0xff);
        }
    }

    int getRangeStart() {
        return start;
    }

    int getRangeEnd() {
        return end;
    }

    byte getData(int address) throws Exception {
        if (address < 0 || address >= size) {
            throw new Exception("Address outside legal range!");
        }
        return data[address];
    }

    void setData(long address, byte value) throws Exception {
        if (address < 0 || address >= size) {
            throw new Exception("Address outside legal range!");
        }

        data[(int) address] = value;
    }

    /*long getSize() { return size; }*/
}

class HEXRecord {// Intel HEX file record
    private int length; // Record length in number of data bytes.
    private int offset; // Offset address.
    private int type; // Record type.
    private byte[] data; // Optional data bytes.

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte... data) {
        this.data = data;
    }
}


