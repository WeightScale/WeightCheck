package com.victjava.scales.bootloader;

import android.os.Handler;
import com.konst.module.ScaleModule;

/**
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 23.12.13
 * Time: 16:56
 * To change this template use File | Settings | File Templates.
 */
public class AVRProgrammer {
    private final Handler handler;
    private long pagesize; // Flash page size.
    //private static final int MEM_PROGRESS_GRANULARITY = 256; // For use with progress indicator.

    public
    /* Constructor */
    AVRProgrammer(Handler _handler) {
        handler = _handler;
    }

    /* Methods */
    void setPagesize(long _pagesize) {
        pagesize = _pagesize;
    }

    boolean chipErase() throws Exception {
        /* Send command 'e' */
        ScaleModule.sendByte((byte) 'e');
        /* Should return CR */
        if (ScaleModule.getByte() != '\r') {
            throw new Exception("Chip erase failed! Programmer did not return CR after 'e'-command.");
        }
        return true; // Indicate supported command.
    }

    void readSignature(Integer... sig) {
        /* Send command 's' */
        ScaleModule.sendByte((byte) 's');
        /* Get actual signature */
        sig[2] = ScaleModule.getByte();
        sig[1] = ScaleModule.getByte();
        sig[0] = ScaleModule.getByte();
    }

    byte readPartCode() {
	    /* Send command 't' */
        ScaleModule.sendByte((byte) 't');
        return (byte) ScaleModule.getByte();
    }

    boolean checkSignature(long sig0, long sig1, long sig2) throws Exception {
        Integer[] sig = new Integer[3];
	    /* Get signature */
        readSignature(sig);
	    /* Compare signature */
        if (sig[0] != sig0 || sig[1] != sig1 || sig[2] != sig2) {
            throw new Exception("Signature does not match selected device! ");
        }
        return true; // Indicate supported command.
    }

    void writeFlashPage() throws Exception {
        ScaleModule.sendByte((byte) 'm');

        if (ScaleModule.getByte() != '\r') {
            throw new Exception("Writing Flash page failed! " + "Programmer did not return CR after 'm'-command.");
        }
    }

    boolean writeFlash(HEXFile data) throws Exception {

	    /* Check that pagesize is set */
        if (pagesize == -1) {
            throw new Exception("Programmer pagesize is not set!");
        }

	    /* Check block write support */
        ScaleModule.sendByte((byte) 'b');

        if (ScaleModule.getByte() == 'Y') {
            handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG, "Using block mode..."));
            return writeFlashBlock(data); // Finished writing.
        }

	    /* Get range from HEX file */
        int start = data.getRangeStart();
        int end = data.getRangeEnd(); // Data address range.

	    /* Check autoincrement support */
        ScaleModule.sendByte((byte) 'a');

        boolean autoincrement = ScaleModule.getByte() == 'Y'; // Bootloader supports address autoincrement?

	    /* Set initial address */
        setAddress(start >> 1); // Flash operations use word addresses.

	    /* Need to write one odd byte first? */
        int address = start;
        if ((address & 1) == 1) {
		    /* Use only high byte */
            writeFlashLowByte((byte) 0xff); // No-write in low byte.
            writeFlashHighByte(data.getData(address));
            address++;

		    /* Need to write page? */
            if (address % pagesize == 0 || address > end) {// Just passed page limit or no more bytes to write?

                setAddress(address - 2 >> 1); // Set to an address inside the page.
                writeFlashPage();
                setAddress(address >> 1);
            }
        }

	    /* Write words */
        while (end - address + 1 >= 2) {// More words left?

		    /* Need to set address again? */
            if (!autoincrement) {
                setAddress(address >> 1);
            }

		    /* Write words */
            writeFlashLowByte(data.getData(address));
            writeFlashHighByte(data.getData(address + 1));
            address += 2;

            /*if( address % MEM_PROGRESS_GRANULARITY == 0 )
                handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,"#"));*/

		    /* Need to write page? */
            if (address % pagesize == 0 || address > end) {// Just passed a page limit or no more bytes to write?

                setAddress(address - 2 >> 1); // Set to an address inside the page.
                writeFlashPage();
                setAddress(address >> 1);
            }
        }

	    /* Need to write one even byte before finished? */
        if (address == end) {
		    /* Use only low byte */
            writeFlashLowByte(data.getData(address));
            writeFlashHighByte((byte) 0xff); // No-write in high byte.
            address += 2;

		    /* Write page */
            setAddress(address - 2 >> 1); // Set to an address inside the page.
            writeFlashPage();
        }

        handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG, ""));
        return true; // Indicate supported command.
    }

    void writeFlashHighByte(byte value) throws Exception {
        ScaleModule.sendByte((byte) 'C');
        ScaleModule.sendByte(value);

        if (ScaleModule.getByte() != '\r') {
            throw new Exception("Writing Flash high byte failed! " + "Programmer did not return CR after 'C'-command.");
        }
    }

    void writeFlashLowByte(byte value) throws Exception {
        ScaleModule.sendByte((byte) 'c');
        ScaleModule.sendByte(value);

        if (ScaleModule.getByte() != '\r') {
            throw new Exception("Writing Flash low byte failed! " + "Programmer did not return CR after 'c'-command.");
        }
    }

    boolean writeFlashBlock(HEXFile data) throws Exception {

	    /* Get block size, assuming command 'b' just issued and 'Y' has been read */
        int blocksize = ScaleModule.getByte() << 8 | ScaleModule.getByte(); // Bootloader block size.

	    /* Get range from HEX file */
        int start = data.getRangeStart();
        int end = data.getRangeEnd(); // Data address range.

	    /* Need to write one odd byte first? */
        int address = start;
        if ((address & 1) == 1) {
            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Use only high byte */
            writeFlashLowByte((byte) 0xff); // No-write in low byte.
            writeFlashHighByte(data.getData(address));
            address++;

		    /* Need to write page? */
            if (address % pagesize == 0 || address > end) {// Just passed page limit or no more bytes to write?

                setAddress(address - 2 >> 1); // Set to an address inside the page.
                writeFlashPage();
                setAddress(address >> 1);
            }
        }

	    /* Need to write from middle to end of block first? */
        int bytecount;
        if (address % blocksize > 0) {// In the middle of a block?

            bytecount = blocksize - address % blocksize; // Bytes left in block.

            if (address + bytecount - 1 > end) {// Is that past the write range?

                bytecount = end - address + 1; // Bytes left in write range.
                bytecount &= ~0x01; // Adjust to word count.
            }

            if (bytecount > 0) {
                setAddress(address >> 1); // Flash operations use word addresses.

			    /* Start Flash block write */
                ScaleModule.sendByte((byte) 'B');
                ScaleModule.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
                ScaleModule.sendByte((byte) bytecount);
                ScaleModule.sendByte((byte) 'F'); // Flash memory.

                while (bytecount > 0) {

                    ScaleModule.sendByte(data.getData(address));
                    address++;
                    bytecount--;
                }

                if (ScaleModule.getByte() != '\r') {
                    throw new Exception("Writing Flash block failed! " + "Programmer did not return CR after 'BxxF'-command.");
                }

                //handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,"#")); // Advance progress indicator.
            }
        }

	    /* More complete blocks to write? */
        while (end - address + 1 >= blocksize) {
            bytecount = blocksize;

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block write */
            ScaleModule.sendByte((byte) 'B');
            ScaleModule.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            ScaleModule.sendByte((byte) bytecount);
            ScaleModule.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                ScaleModule.sendByte(data.getData(address));
                address++;
                bytecount--;
            }

            if (ScaleModule.getByte() != '\r') {
                throw new Exception("Writing Flash block failed! " + "Programmer did not return CR after 'BxxF'-command.");
            }

            handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_UPDATE_DIALOG, address, 0));
        }

	    /* Any bytes left in last block */
        if (end - address + 1 >= 1) {
            bytecount = end - address + 1; // Get bytes left to write.
            if ((bytecount & 1) == 1) {
                bytecount++; // Align to next word boundary.
            }

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block write */
            ScaleModule.sendByte((byte) 'B');
            ScaleModule.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            ScaleModule.sendByte((byte) bytecount);
            ScaleModule.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                if (address > end) {
                    ScaleModule.sendByte((byte) 0xff); // Don't write outside write range.
                } else {
                    ScaleModule.sendByte(data.getData(address));
                }

                address++;
                bytecount--;
            }

            if (ScaleModule.getByte() != '\r') {
                throw new Exception("Writing Flash block failed! " + "Programmer did not return CR after 'BxxF'-command.");
            }

            //handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,"#"));
        }

        //handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,""));
        return true; // Indicate supported command.
    }

    boolean readFlash(HEXFile data) throws Exception {

        if (pagesize == -1) {
            throw new Exception("Programmer pagesize is not set!");
        }

	    /* Check block read support */
        ScaleModule.sendByte((byte) 'b');

        if (ScaleModule.getByte() == 'Y') {
            handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG, "Using block mode..."));
            return readFlashBlock(data); // Finished writing.
        }

	    /* Get range from HEX file */
        long start = data.getRangeStart();
        long end = data.getRangeEnd(); // Data address range.

	    /* Check autoincrement support */
        ScaleModule.sendByte((byte) 'a');

        boolean autoincrement = ScaleModule.getByte() == 'Y'; // Bootloader supports address autoincrement?

	    /* Set initial address */
        setAddress(start >> 1); // Flash operations use word addresses.

	    /* Need to read one odd byte first? */
        long address = start;
        if ((address & 1) == 1) {
		    /* Read both, but use only high byte */
            ScaleModule.sendByte((byte) 'R');

            data.setData(address, (byte) ScaleModule.getByte()); // High byte.
            ScaleModule.getByte(); // Dont use low byte.
            address++;
        }

	    /* Get words */
        while (end - address + 1 >= 2) {
		    /* Need to set address again? */
            if (!autoincrement) {
                setAddress(address >> 1);
            }

		    /* Get words */
            ScaleModule.sendByte((byte) 'R');

            data.setData(address + 1, (byte) ScaleModule.getByte()); // High byte.
            data.setData(address, (byte) ScaleModule.getByte()); // Low byte.
            address += 2;

            /*if( address % MEM_PROGRESS_GRANULARITY == 0 )
                handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,"#"));// Advance progress indicator.*/


        }

	    /* Need to read one even byte before finished? */
        if (address == end) {
		    /* Read both, but use only low byte */
            ScaleModule.sendByte((byte) 'R');

            ScaleModule.getByte(); // Dont use high byte.
            data.setData(address, (byte) ScaleModule.getByte()); // Low byte.
        }

        //handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,""));
        return true; // Indicate supported command.
    }

    boolean readFlashBlock(HEXFile data) throws Exception {

	    /* Get block size, assuming command 'b' just issued and 'Y' has been read */
        int blocksize = ScaleModule.getByte() << 8 | ScaleModule.getByte(); // Bootloader block size.

	    /* Get range from HEX file */
        int start = data.getRangeStart();
        int end = data.getRangeEnd(); // Data address range.

	    /* Need to read one odd byte first? */
        int address = start;
        if ((address & 1) == 1) {
            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Use only high word */
            ScaleModule.sendByte((byte) 'R');

            data.setData(address, (byte) ScaleModule.getByte()); // High byte.
            ScaleModule.getByte(); // Low byte.
            address++;
        }

	    /* Need to read from middle to end of block first? */
        int bytecount;
        if (address % blocksize > 0) { // In the middle of a block?

            bytecount = blocksize - address % blocksize; // Bytes left in block.

            if (address + bytecount - 1 > end) {// Is that past the read range?

                bytecount = end - address + 1; // Bytes left in read range.
                bytecount &= ~0x01; // Adjust to word count.
            }

            if (bytecount > 0) {
                setAddress(address >> 1); // Flash operations use word addresses.

			    /* Start Flash block read */
                ScaleModule.sendByte((byte) 'g');
                ScaleModule.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
                ScaleModule.sendByte((byte) bytecount);
                ScaleModule.sendByte((byte) 'F'); // Flash memory.

                while (bytecount > 0) {
                    data.setData(address, (byte) ScaleModule.getByte());
                    address++;
                    bytecount--;
                }

                //handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,"#"));// Advance progress indicator.
            }
        }

	    /* More complete blocks to read? */
        while (end - address + 1 >= blocksize) {
            bytecount = blocksize;

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block read */
            ScaleModule.sendByte((byte) 'g');
            ScaleModule.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            ScaleModule.sendByte((byte) bytecount);
            ScaleModule.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                data.setData(address, (byte) ScaleModule.getByte());
                address++;
                bytecount--;
            }
            handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_UPDATE_DIALOG, address, 0));
        }

	    /* Any bytes left in last block */
        if (end - address + 1 >= 1) {
            bytecount = end - address + 1; // Get bytes left to read.
            if ((bytecount & 1) == 1) {
                bytecount++; // Align to next word boundary.
            }

            setAddress(address >> 1); // Flash operations use word addresses.

		    /* Start Flash block read */
            ScaleModule.sendByte((byte) 'g');
            ScaleModule.sendByte((byte) (bytecount >> 8)); // Size, MSB first.
            ScaleModule.sendByte((byte) bytecount);
            ScaleModule.sendByte((byte) 'F'); // Flash memory.

            while (bytecount > 0) {
                if (address > end) {
                    ScaleModule.getByte(); // Don't read outside write range.
                } else {
                    data.setData(address, (byte) ScaleModule.getByte());
                }

                address++;
                bytecount--;
            }

            //handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,"#"));
        }

        //handler.sendMessage(handler.obtainMessage(ActivityBootloader.MSG_LOG,"\r\n"));
        return true; // Indicate supported command.
    }

    public String readProgrammerID() {
        /* Send 'S' command to programmer */
        ScaleModule.sendByte((byte) 'S');
	    /* Read 7 characters */
        char[] id = new char[7]; // Reserve 7 characters.
        int length = id.length;
        for (int i = 0; i < length; i++) {
            id[i] = (char) ScaleModule.getByte();
        }
        return String.valueOf(id);
    }

    public boolean isProgrammerId() { //Является ли программатором
        //String str = AVRProgrammer.readProgrammerID();
        return "AVRBOOT".equals(readProgrammerID());
    }

    void setAddress(long address) throws Exception {
	    /* Set current address */
        if (address < 0x10000) {
            ScaleModule.sendByte((byte) 'A');
            ScaleModule.sendByte((byte) (address >> 8));
            ScaleModule.sendByte((byte) address);
        } else {
            ScaleModule.sendByte((byte) 'H');
            ScaleModule.sendByte((byte) (address >> 16));
            ScaleModule.sendByte((byte) (address >> 8));
            ScaleModule.sendByte((byte) address);
        }

	    /* Should return CR */
        if (ScaleModule.getByte() != '\r') {
            throw new Exception("Setting address for programming operations failed! " + "Programmer did not return CR after 'A'-command.");
        }
    }
}
