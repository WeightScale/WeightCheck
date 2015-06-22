//Класс весов
package com.victjava.scales;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;


import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.*;

import com.victjava.scales.Versions.ScaleInterface;

public class Scales {
    private static BluetoothDevice device;                  //чужое устройство
    private static final BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private static BluetoothSocket socket;                  //соединение
    private static OutputStream outputStream;             //поток отправки
    private static OutputStreamWriter outputStreamWriter;
    private static InputStream is;                          //поток получения
    public static ScaleInterface vScale;                    //Интерфейс для разных версий весов
    protected static String version;                                  // версия весов
    public static float coefficientA;                              // калибровочный коэффициент a
    protected static float coefficientTemp;                          // калибровочный коэффициент температуры
    public static float coefficientB;                              // калибровочный коэффициент b
    public static int sensorTenzo;                                // показание датчика веса
    protected static int sensorTenzoOffset;                         // показание датчика веса минус offset
    protected static int offset;                                      // offset
    //static int sensor_temp;                                 // показание датчика температуры
    public static int battery;                                     // процент батареи (0-100%)
    public static int filter;                                      // АЦП-фильтр (0-15)
    public static int timer;                                       // таймер выключения весов
    protected static int speed;                                       // скорость передачи данных
    public static int step;                                        // шаг измерения (округление)
    public static int autoCapture;                                 // шаг захвата (округление)
    protected static int weight;                                      // реальный вес
    public static int weightMax;                                   // максимальный вес
    protected static int limitTenzo;                                  // максимальное показание датчика
    protected static int marginTenzo;                                 // предельное показани датчика
    protected static int weightMargin;                                // предельный вес
    protected static int weightError;                                 // ошибка авто нуля
    protected static int timerNull;                                   // время срабатывания авто нуля

    static final int COUNT_STABLE = 16;                            //колличество раз стабильно был вес
    static final int DIVIDER_AUTO_NULL = 3;                         //делитель для авто ноль
    static final int TIMEOUT_GET_BYTE = 2000;                       //время задержки для получения байта

    private static int tempWeight;
    protected static int numStable;
    protected static boolean flagStable;
    private static final UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static String spreadsheet = "WeightCheck";
    public static String username = "weight.check.lg@gmail.com";
    public static String password = "htcehc26";
    public static String phone = "+380503285426";


    static boolean stable(int weight) {
        if (tempWeight - step <= weight && tempWeight + step >= weight) {
            if (++numStable >= COUNT_STABLE) {
                return true;
            }
        } else {
            numStable = 0;
        }
        tempWeight = weight;
        return false;
    }

    public static synchronized boolean connect(BluetoothDevice bd) { //соединиться с весами
        disconnect();
        device = bd;
        BluetoothSocket bluetoothSocket = null;
        // Get a BluetoothSocket for a connection with the given BluetoothDevice
        try {
            bluetoothSocket = bd.createRfcommSocketToServiceRecord(uuid);
        } catch (IOException ignored) {
            //
        }
        socket = bluetoothSocket;
        //if(BluetoothAdapter.getDefaultAdapter().isDiscovering())
        if (!bluetoothAdapter.isEnabled()) {
            return false;
        }
        bluetoothAdapter.cancelDiscovery();


        try {
            socket.connect();
            is = socket.getInputStream();
            outputStreamWriter = new OutputStreamWriter(socket.getOutputStream());
            outputStream = socket.getOutputStream();
        } catch (IOException e) {
            disconnect();
            return false;
        }
        return true;
    }

    public static void disconnect() { //рассоединиться
        try {
            if (socket != null) {
                socket.close();
            }
            if (is != null) {
                is.close();
            }
            if (outputStreamWriter != null) {
                outputStreamWriter.close();
            }
            if (outputStream != null) {
                outputStream.close();
            }
        } catch (IOException ioe) {
            socket = null;
            //return;
        }
        is = null;
        outputStreamWriter = null;
        outputStream = null;
        socket = null;
    }

    public static synchronized String command(String cmd) { //послать команду и получить ответ
        if (outputStreamWriter != null) {
            try {
                int t = is.available();
                if (t > 0) {
                    is.read(new byte[t]);
                }
                outputStreamWriter.write(cmd);
                outputStreamWriter.write(ScaleInterface.CR_LF);
                outputStreamWriter.flush();
                String cmd_rtn = ""; //возвращённая команда
                for (int i = 0; i < 450 && cmd_rtn.length() < 129; i++) {
                    Thread.sleep(1);
                    if (is.available() > 0) {
                        i = 0;
                        char ch = (char) is.read(); //временный символ (байт)
                        if (ch == 0xffff) {
                            connect(device);
                            break;
                        }
                        if (ch == '\r') {
                            continue;
                        }
                        if (ch == '\n') {
                            if (cmd_rtn.startsWith(cmd.substring(0, 3))) {
                                if (cmd_rtn.replace(cmd.substring(0, 3), "").isEmpty()) {
                                    return cmd.substring(0, 3);
                                } else {
                                    return cmd_rtn.replace(cmd.substring(0, 3), "");
                                }
                            } else {
                                return "";
                            }
                        }
                        cmd_rtn += ch;
                    }
                }
            } catch (IOException ignored) {
                connect(device);
            } catch (InterruptedException ignored) {
            }
        } else {
            connect(device);
        }
        return "";
    }

    public static boolean isScales() { //Является ли весами и какой версии
        String vrs = command(ScaleInterface.CMD_VERSION); //Получаем версию весов
        if (vrs.startsWith(Main.versionName)) {
            version = vrs.replace(Main.versionName, "");
            try {
                vScale = new Versions().getVersion(Integer.valueOf(version));
            } catch (Exception e) {
                return false;
            }
            return true;
        }
        return false;
    }

    static CharSequence getName() throws Exception {
        return device.getName();
    }

    public static String getAddress() {
        try {
            return device.getAddress();
        } catch (Exception e) {
            return e.getMessage();
        }
    }

    static int readBattery() {
        //String str = command(Versions.ScaleInterface.CMD_BATTERY);
        try {
            battery = Integer.valueOf(command(Versions.ScaleInterface.CMD_BATTERY));
        } catch (Exception e) {
            battery = 0;
        }
        //battery = str.isEmpty() ? 0 : Integer.valueOf(str);
        return battery;
    }

    public static int getTemp() { //Получить температуру
        String str = command(ScaleInterface.CMD_DATA_TEMP);
        try {
            return (int) ((float) ((Integer.valueOf(command(ScaleInterface.CMD_DATA_TEMP)) - 0x800000) / 7169) / 0.81) - 273;
        } catch (Exception e) {
            return -273;
        }
        /*if (str.isEmpty()) {
            return -273;
        }
        return (int) ((float) ((Integer.valueOf(str) - 0x800000) / 7169) / 0.81) - 273;*/
    }

    static boolean isInteger(String str) {
        try {
            Integer.valueOf(str);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    static boolean isFloat(String str) {
        try {
            Float.valueOf(str);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    //==============================================================================
    public static synchronized boolean sendByte(byte ch) {
        if (outputStream != null) {
            try {
                int t = is.available();
                if (t > 0) {
                    is.read(new byte[t]);
                }
                outputStream.write(ch);
                outputStream.flush(); //что этот метод делает?
                return true;
            } catch (IOException ioe) {
                connect(device);
            }
        } else {
            connect(device);
        }
        return false;
    }

    public static synchronized int getByte() {
        if (is != null) {
            try {
                for (int i = 0; i < TIMEOUT_GET_BYTE; i++) {
                    if (is.available() > 0) {
                        return is.read(); //временный символ (байт)
                    }
                    Thread.sleep(1);
                }
                return 0;
            } catch (IOException | InterruptedException ioe) {
                connect(device);
            }
        } else {
            connect(device);
        }
        return 0;
    }

}
