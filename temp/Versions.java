package com.victjava.scales;

import com.victjava.scales.provider.CheckDBAdapter;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Created by Kostya on 29.03.2015.
 */
public class Versions {
    protected final Map<Integer, ScaleInterface> mapVersion = new LinkedHashMap();

    {
        mapVersion.put(1, new V1());
        mapVersion.put(2, new V1());
        mapVersion.put(3, new V2());
        mapVersion.put(4, new V4());
        //mapVersion = Collections.unmodifiableMap(mapVersion);
    }

    public interface ScaleInterface {

        String CMD_VERSION = "VRS";            //получить версию весов
        String CMD_HARDWARE = "HRW";            //получить версию hardware
        String CMD_NAME = "SNA";            //установить имя весов
        String CMD_SENSOR = "DCH";            //получить показание датчика веса
        String CMD_SENSOR_OFFSET = "DCO";            //получить показание датчика веса минус офсет
        String CMD_SET_OFFSET = "SCO";            //установить offset
        String CMD_CALL_BATTERY = "CBT";            //каллибровать процент батареи
        String CMD_CALL_TEMP = "CTM";            //каллибровать температуру
        String CMD_BATTERY = "GBT";            //получить передать заряд батареи
        String CMD_FILTER = "FAD";            //получить/установить АЦП-фильтр
        String CMD_TIMER = "TOF";            //получить/установить таймер выключения весов
        String CMD_SPEED = "BST";            //получить/установить скорость передачи данных
        String CMD_DATA = "DAT";            //считать/записать данные весов
        String CMD_DATA_TEMP = "DTM";            //считать/записать данные температуры
        String CMD_SPREADSHEET = "SGD";            //считать/записать имя таблици созданой в google disc
        String CMD_G_USER = "UGD";            //считать/записать account google disc
        String CMD_G_PASS = "PGD";            //считать/записать password google disc
        String CMD_PHONE = "PHN";            //считать/записать phone for sms boss

        String CMD_GET_OFFSET = "GCO";

        String CMD_DATA_CFA = "cfa";               //коэфициэнт А
        String CMD_DATA_CFB = "cfb";               //коэфициэнт Б
        String CMD_DATA_WGM = "wgm";               //вес максимальный
        String CMD_DATA_LMT = "lmt";               //лимит тензодатчика

        String CR_LF = "\r\n";

        boolean load() throws Exception;//Загрузить данные весов

        int getWeightScale();//Получить показания датчика веса

        int getSensorScale();//Получить показания датчика веса

        boolean setOffsetScale();//Установить ноль

        boolean writeDataScale();//Записать данные

        boolean isDataValid(String d);//Проверить и установить правельные данные

        int getLimitTenzo();

        int getMarginTenzo();

        int getSensorTenzo();

        boolean isLimit();

        boolean isMargin();

        boolean isCapture();

        boolean setScaleNull();

        boolean backupPreference();

        boolean restorePreferences();

        ScaleInterface getVersion();
    }

    ScaleInterface getVersion(int version) throws Exception {
        return mapVersion.get(Integer.valueOf(version)).getVersion();
    }

    public static class V1 extends Scales implements ScaleInterface {

        @Override
        public boolean load() throws Exception { //загрузить данные
            //======================================================================
            String str = command(CMD_FILTER); //временная строка
            if (str.isEmpty() || !isInteger(str)) {
                throw new Exception("Фильтер АЦП не установлен в настройках");
            }
            filter = Integer.valueOf(str);
            if (filter < 0 || filter > Main.default_adc_filter) {
                command(CMD_FILTER + 8);
                filter = 8;
            }
            //======================================================================
            String command = command(CMD_TIMER);
            if (!isInteger(command)) {
                throw new Exception("Таймер выключения не установлен в настройках");
            }
            timer = Integer.valueOf(command);
            if (timer < Main.default_min_time_off || timer > Main.default_max_time_off) {
                if (command(CMD_TIMER + Main.default_min_time_off).equals(CMD_TIMER)) {
                    timer = Main.default_min_time_off;
                } else {
                    throw new Exception("Таймер выключения не установлен в настройках");
                }
            }
            //======================================================================
            try {
                step = Main.preferencesScale.read(ActivityPreferences.KEY_STEP, Main.default_max_step_scale);
                autoCapture = Main.preferencesScale.read(ActivityPreferences.KEY_AUTO_CAPTURE, Main.default_max_auto_capture);
                timerNull = Main.preferencesScale.read(ActivityPreferences.KEY_TIMER_NULL, Main.default_max_time_auto_null);
                CheckDBAdapter.day = Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CHECK_DELETE, Main.default_day_delete_check);
                CheckDBAdapter.day_closed = Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CLOSED_CHECK, Main.default_day_close_check);
                weightError = Main.preferencesScale.read(ActivityPreferences.KEY_MAX_NULL, Main.default_limit_auto_null);
            } catch (Exception e) {
                throw new Exception("Установить настройки ограничений");
            }
            //======================================================================
            if (!isDataValid(command(CMD_DATA))) {
                return false;
            }

            weightMargin = (int) (weightMax * 1.2);
            marginTenzo = (int) ((weightMax / coefficientA) * 1.2);
            return true;
        }

        @Override
        public synchronized int getWeightScale() {
            String str = command(CMD_SENSOR);
            if (str.isEmpty()) {
                sensorTenzo = weight = Integer.MIN_VALUE;
            } else {
                sensorTenzo = Integer.valueOf(str);
                weight = (int) (coefficientA * sensorTenzo + coefficientB);
                weight = weight / step * step;
            }
            return weight;
        }

        @Override
        public synchronized int getSensorScale() {
            String sensor = command(CMD_SENSOR);
            sensorTenzo = sensor.isEmpty() ? Integer.MIN_VALUE : Integer.valueOf(sensor);
            return sensorTenzo;
        }

        @Override
        public synchronized boolean setOffsetScale() { //обнуление
            String str = command(CMD_SENSOR);
            if (str.isEmpty()) {
                return false;
            }
            coefficientB = -coefficientA * Integer.parseInt(str);
            return true;
        }

        @Override
        public boolean writeDataScale() {
            return command(CMD_DATA + 'S' + coefficientA + ' ' + coefficientB + ' ' + weightMax).equals(CMD_DATA);
        }

        @Override
        public synchronized boolean isDataValid(String d) {
            StringBuilder dataBuffer = new StringBuilder(d);
            if (dataBuffer.toString().isEmpty()) {
                return false;
            }
            dataBuffer.deleteCharAt(0);
            if (dataBuffer.indexOf(" ") == -1) {
                return false;
            }
            String str = dataBuffer.substring(0, dataBuffer.indexOf(" "));
            if (!isFloat(str)) {
                return false;
            }
            coefficientA = Float.valueOf(str);
            dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
            if (dataBuffer.indexOf(" ") == -1) {
                return false;
            }
            String substring = dataBuffer.substring(0, dataBuffer.indexOf(" "));
            if (!isFloat(substring)) {
                return false;
            }
            coefficientB = Float.valueOf(substring);
            dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
            if (!isInteger(dataBuffer.toString())) {
                return false;
            }
            weightMax = Integer.valueOf(dataBuffer.toString());
            if (weightMax <= 0) {
                weightMax = 1000;
                //writeData();
            }
            return true;
        }

        @Override
        public int getLimitTenzo() {
            return weightMax;
        }

        @Override
        public int getMarginTenzo() {
            return marginTenzo;
        }

        @Override
        public int getSensorTenzo() {
            return weight;
        }

        @Override
        public boolean isLimit() {
            return Math.abs(weight) > weightMax;
        }

        @Override
        public boolean isMargin() {
            return Math.abs(weight) < weightMargin;
        }

        @Override
        public boolean isCapture() {
            boolean capture = false;
            while (getWeightScale() > Scales.autoCapture) {
                if (!capture) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                    capture = true;
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean setScaleNull() {
            String str = command(ScaleInterface.CMD_SENSOR);
            if (str.isEmpty()) {
                return false;
            }

            if (setOffsetScale()) {
                if (writeDataScale()) {
                    sensorTenzo = Integer.valueOf(str);
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean backupPreference() {
            //SharedPreferences.Editor editor = context.getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE).edit();

            Main.preferencesUpdate.write(CMD_FILTER, String.valueOf(filter));
            Main.preferencesUpdate.write(CMD_TIMER, String.valueOf(timer));
            Main.preferencesUpdate.write(CMD_BATTERY, String.valueOf(battery));
            Main.preferencesUpdate.write(CMD_DATA_CFA, String.valueOf(coefficientA));
            Main.preferencesUpdate.write(CMD_DATA_CFB, String.valueOf(coefficientB));
            Main.preferencesUpdate.write(CMD_DATA_WGM, String.valueOf(weightMax));

            //editor.apply();
            return true;
        }

        @Override
        public boolean restorePreferences() {
            return false;
        }

        @Override
        public ScaleInterface getVersion() {
            return this;
        }
    }

    public static class V2 extends Scales implements ScaleInterface {

        @Override
        public boolean load() throws Exception { //загрузить данные
            //======================================================================
            String str = command(CMD_FILTER); //временная строка
            if (str.isEmpty() || !isInteger(str)) {
                throw new Exception("Фильтер АЦП не установлен в настройках");
            }
            filter = Integer.valueOf(str);
            if (filter < 0 || filter > Main.default_adc_filter) {
                command(CMD_FILTER + 8);
                filter = 8;
            }
            //======================================================================
            String command = command(CMD_TIMER);
            if (!isInteger(command)) {
                throw new Exception("Таймер выключения не установлен в настройках");
            }
            timer = Integer.valueOf(command);
            if (timer < Main.default_min_time_off || timer > Main.default_max_time_off) {
                command(CMD_TIMER + Main.default_min_time_off);
                timer = Main.default_min_time_off;
            }
            //======================================================================
        /*str = command(CMD_SPEED);
        if(!isInteger(str))
            return false;*/
            //======================================================================
            command = command(CMD_CALL_TEMP);
            if (!isFloat(command)) {
                return false;
            }
            coefficientTemp = Float.valueOf(command);
            //======================================================================
            command = command(CMD_SPREADSHEET);
            if (command.isEmpty()) {
                return false;
            }
            spreadsheet = command;
            //======================================================================
            command = command(CMD_G_USER);
            if (command.isEmpty()) {
                return false;
            }
            username = command;
            //======================================================================
            command = command(CMD_G_PASS);
            if (command.isEmpty()) {
                return false;
            }
            password = command;
            //======================================================================
            try {
                step = Main.preferencesScale.read(ActivityPreferences.KEY_STEP, Main.default_max_step_scale);
                autoCapture = Main.preferencesScale.read(ActivityPreferences.KEY_AUTO_CAPTURE, Main.default_max_auto_capture);
                timerNull = Main.preferencesScale.read(ActivityPreferences.KEY_TIMER_NULL, Main.default_max_time_auto_null);
                CheckDBAdapter.day = Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CHECK_DELETE, Main.default_day_delete_check);
                CheckDBAdapter.day_closed = Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CLOSED_CHECK, Main.default_day_close_check);
                weightError = Main.preferencesScale.read(ActivityPreferences.KEY_MAX_NULL, Main.default_limit_auto_null);
            } catch (Exception e) {
                throw new Exception("Установить настройки ограничений");
            }
            //======================================================================
            if (!isDataValid(command(CMD_DATA))) {
                return false;
            }

            weightMargin = (int) (weightMax * 1.2);
            marginTenzo = (int) ((weightMax / coefficientA) * 1.2);
            limitTenzo = (int) (weightMax / coefficientA);
            return true;
        }

        @Override
        public synchronized int getWeightScale() {
            String str = command(CMD_SENSOR_OFFSET);
            if (str.isEmpty()) {
                sensorTenzoOffset = weight = Integer.MIN_VALUE;
            } else {
                sensorTenzoOffset = Integer.valueOf(str);
                weight = (int) (coefficientA * sensorTenzoOffset);
                weight = weight / step * step;
            }
            return weight;
        }

        @Override
        public synchronized int getSensorScale() {
            String sensor = command(CMD_SENSOR);
            sensorTenzo = sensor.isEmpty() ? Integer.MIN_VALUE : Integer.valueOf(sensor);
            return sensorTenzo;
        }

        @Override
        public synchronized boolean setOffsetScale() { //обнуление
            return command(CMD_SET_OFFSET).equals(CMD_SET_OFFSET);
        }

        @Override
        public boolean writeDataScale() {
            return command(CMD_DATA + 'S' + coefficientA + ' ' + weightMax).equals(CMD_DATA);
        }

        @Override
        public synchronized boolean isDataValid(String d) {
            StringBuilder dataBuffer = new StringBuilder(d);
            if (dataBuffer.toString().isEmpty()) {
                return false;
            }
            dataBuffer.deleteCharAt(0);
            if (dataBuffer.indexOf(" ") == -1) {
                return false;
            }
            String str = dataBuffer.substring(0, dataBuffer.indexOf(" "));
            if (!isFloat(str)) {
                return false;
            }
            coefficientA = Float.valueOf(str);
            dataBuffer.delete(0, dataBuffer.indexOf(" ") + 1);
            if (!isInteger(dataBuffer.toString())) {
                return false;
            }
            weightMax = Integer.valueOf(dataBuffer.toString());
            if (weightMax <= 0) {
                weightMax = 1000;
                //writeData();
            }
            return true;
        }

        @Override
        public int getLimitTenzo() {
            return limitTenzo;
        }

        @Override
        public int getMarginTenzo() {
            return marginTenzo;
        }

        @Override
        public int getSensorTenzo() {
            return sensorTenzoOffset;
        }

        @Override
        public boolean isLimit() {
            return Math.abs(sensorTenzoOffset) > limitTenzo;
        }

        @Override
        public boolean isMargin() {
            return Math.abs(sensorTenzoOffset) > marginTenzo;
        }

        @Override
        public boolean isCapture() {
            boolean capture = false;
            while (getWeightScale() > Scales.autoCapture) {
                if (!capture) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                    capture = true;
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean setScaleNull() {
            return setOffsetScale();
        }

        @Override
        public boolean backupPreference() {
            //SharedPreferences.Editor editor = context.getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE).edit();

            Main.preferencesUpdate.write(CMD_FILTER, String.valueOf(filter));
            Main.preferencesUpdate.write(CMD_TIMER, String.valueOf(timer));
            Main.preferencesUpdate.write(CMD_BATTERY, String.valueOf(battery));
            Main.preferencesUpdate.write(CMD_CALL_TEMP, String.valueOf(coefficientTemp));
            Main.preferencesUpdate.write(CMD_SPREADSHEET, spreadsheet);
            Main.preferencesUpdate.write(CMD_G_USER, username);
            Main.preferencesUpdate.write(CMD_G_PASS, password);
            Main.preferencesUpdate.write(CMD_DATA_CFA, String.valueOf(coefficientA));
            Main.preferencesUpdate.write(CMD_DATA_WGM, String.valueOf(weightMax));

            //editor.apply();
            return true;
        }

        @Override
        public boolean restorePreferences() {
            return false;
        }

        @Override
        public ScaleInterface getVersion() {
            return this;
        }
    }

    public static class V4 extends Scales implements ScaleInterface {

        @Override
        public boolean load() throws Exception { //загрузить данные
            //======================================================================
            try {
                filter = Integer.valueOf(command(CMD_FILTER));
                if (filter < 0 || filter > Main.default_adc_filter) {
                    if (!command(CMD_FILTER + Main.default_adc_filter).equals(CMD_FILTER))
                        throw new Exception("Фильтер АЦП не установлен в настройках");
                    filter = Main.default_adc_filter;
                }
            } catch (Exception e) {
                throw new Exception("Фильтер АЦП не установлен в настройках");
            }
            //======================================================================
            try {
                timer = Integer.valueOf(command(CMD_TIMER));
                if (timer < Main.default_min_time_off || timer > Main.default_max_time_off) {
                    if (!command(CMD_TIMER + Main.default_min_time_off).equals(CMD_TIMER))
                        throw new Exception("Таймер выключения не установлен в настройках");
                    timer = Main.default_min_time_off;
                }
            } catch (Exception e) {
                throw new Exception("Таймер выключения не установлен в настройках");
            }
            //======================================================================
            try {
                offset = Integer.parseInt(command(CMD_GET_OFFSET));
            } catch (Exception e) {
                throw new Exception("Сделать обнуление в настройках");
            }
            //======================================================================
            try {
                coefficientTemp = Float.valueOf(command(CMD_CALL_TEMP));
            } catch (Exception e) {
                throw new Exception("Неправильная константа каллибровки температуры");
            }
            //======================================================================
            try {
                spreadsheet = command(CMD_SPREADSHEET);
                username = command(CMD_G_USER);
                password = command(CMD_G_PASS);
                phone = command(CMD_PHONE);
            } catch (Exception e) {
                return false;
            }
            //======================================================================
            try {
                step = Main.preferencesScale.read(ActivityPreferences.KEY_STEP, Main.default_max_step_scale);
                autoCapture = Main.preferencesScale.read(ActivityPreferences.KEY_AUTO_CAPTURE, Main.default_max_auto_capture);
                timerNull = Main.preferencesScale.read(ActivityPreferences.KEY_TIMER_NULL, Main.default_max_time_auto_null);
                CheckDBAdapter.day = Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CHECK_DELETE, Main.default_day_delete_check);
                CheckDBAdapter.day_closed = Main.preferencesScale.read(ActivityPreferences.KEY_DAY_CLOSED_CHECK, Main.default_day_close_check);
                weightError = Main.preferencesScale.read(ActivityPreferences.KEY_MAX_NULL, Main.default_limit_auto_null);
            } catch (Exception ignored) {
                throw new Exception("Установить настройки ограничений");
            }
            //======================================================================
            if (!isDataValid(command(CMD_DATA))) {
                return false;
            }
            weightMargin = (int) (weightMax * 1.2);
            marginTenzo = (int) ((weightMax / coefficientA) * 1.2);
            return true;
        }

        @Override
        public synchronized int getWeightScale() {
            try {
                sensorTenzoOffset = Integer.valueOf(command(CMD_SENSOR_OFFSET));
                weight = (int) (coefficientA * sensorTenzoOffset);
                weight = weight / step * step;
            } catch (Exception e) {
                sensorTenzoOffset = weight = Integer.MIN_VALUE;
            }
            return weight;
        }

        @Override
        public synchronized int getSensorScale() {
            try {
                sensorTenzo = Integer.valueOf(command(CMD_SENSOR));
            } catch (Exception e) {
                sensorTenzo = Integer.MIN_VALUE;
            }
            return sensorTenzo;
        }

        synchronized int getOffsetScale() {
            try {
                offset = Integer.valueOf(command(CMD_GET_OFFSET));
            } catch (Exception e) {
                offset = -1;
            }
            return offset;
        }

        @Override
        public synchronized boolean setOffsetScale() { //обнуление
            if (command(CMD_SET_OFFSET).equals(CMD_SET_OFFSET)) {
                if (getOffsetScale() != -1) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean writeDataScale() {
            return command(CMD_DATA +
                    CMD_DATA_CFA + '=' + coefficientA + ' ' +
                    CMD_DATA_WGM + '=' + weightMax + ' ' +
                    CMD_DATA_LMT + '=' + limitTenzo).equals(CMD_DATA);
        }

        @Override
        public synchronized boolean isDataValid(String d) {
            String[] parts = d.split(" ", 0);
            SimpleCommandLineParser data = new SimpleCommandLineParser(parts, "=");
            Iterator<String> iteratorData = data.getKeyIterator();
            try {
                while (iteratorData.hasNext()) {
                    switch (iteratorData.next()) {
                        case CMD_DATA_CFA:
                            coefficientA = Float.valueOf(data.getValue(CMD_DATA_CFA));//получаем коэфициент
                            if (coefficientA == 0)
                                return false;
                            break;
                        case CMD_DATA_CFB:
                            coefficientB = Float.valueOf(data.getValue(CMD_DATA_CFB));//получить offset
                            break;
                        case CMD_DATA_WGM:
                            weightMax = Integer.parseInt(data.getValue(CMD_DATA_WGM));//получаем макимальнай вес
                            if (weightMax <= 0)
                                return false;
                            break;
                        case CMD_DATA_LMT:
                            limitTenzo = Integer.parseInt(data.getValue(CMD_DATA_LMT));//получаем макимальнай показание перегруза
                            break;
                        default:
                    }
                }
            } catch (Exception e) {
                return false;
            }
            return true;
        }

        @Override
        public int getLimitTenzo() {
            return limitTenzo;
        }

        @Override
        public int getMarginTenzo() {
            return marginTenzo;
        }

        @Override
        public int getSensorTenzo() {
            return sensorTenzoOffset + offset;
        }

        @Override
        public boolean isLimit() {
            return Math.abs(getSensorTenzo()) > limitTenzo;
        }

        @Override
        public boolean isMargin() {
            return Math.abs(getSensorTenzo()) > marginTenzo;
        }

        @Override
        public boolean isCapture() {
            boolean capture = false;
            while (getWeightScale() > Scales.autoCapture) {
                if (!capture) {
                    try {
                        TimeUnit.SECONDS.sleep(1);
                    } catch (InterruptedException ignored) {
                    }
                    capture = true;
                } else {
                    return true;
                }
            }
            return false;
        }

        @Override
        public boolean setScaleNull() {
            return setOffsetScale();
        }

        @Override
        public boolean backupPreference() {
            //SharedPreferences.Editor editor = context.getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE).edit();

            Main.preferencesUpdate.write(CMD_FILTER, String.valueOf(filter));
            Main.preferencesUpdate.write(CMD_TIMER, String.valueOf(timer));
            Main.preferencesUpdate.write(CMD_BATTERY, String.valueOf(battery));
            Main.preferencesUpdate.write(CMD_CALL_TEMP, String.valueOf(coefficientTemp));
            Main.preferencesUpdate.write(CMD_SPREADSHEET, spreadsheet);
            Main.preferencesUpdate.write(CMD_G_USER, username);
            Main.preferencesUpdate.write(CMD_G_PASS, password);
            Main.preferencesUpdate.write(CMD_DATA_CFA, String.valueOf(coefficientA));
            Main.preferencesUpdate.write(CMD_DATA_WGM, String.valueOf(weightMax));

            //editor.apply();
            return true;
        }

        @Override
        public boolean restorePreferences() {
            if (Scales.isScales()) {
                //Preferences.load(context.getSharedPreferences(Preferences.PREF_UPDATE, Context.MODE_PRIVATE));
                command(CMD_FILTER + Main.preferencesUpdate.read(CMD_FILTER, String.valueOf(Main.default_adc_filter)));
                command(CMD_TIMER + Main.preferencesUpdate.read(CMD_TIMER, String.valueOf(Main.default_max_time_off)));
                command(CMD_CALL_BATTERY + Main.preferencesUpdate.read(CMD_BATTERY, String.valueOf(Main.default_max_battery)));
                command(CMD_CALL_TEMP + Main.preferencesUpdate.read(CMD_CALL_TEMP, "0"));
                command(CMD_SPREADSHEET + Main.preferencesUpdate.read(CMD_SPREADSHEET, "weightscale"));
                command(CMD_G_USER + Main.preferencesUpdate.read(CMD_G_USER, "kreogen.lg@gmail.com"));
                command(CMD_G_PASS + Main.preferencesUpdate.read(CMD_G_PASS, "htcehc25"));

                coefficientA = Float.valueOf(Main.preferencesUpdate.read(CMD_DATA_CFA, "0"));
                weightMax = Integer.valueOf(Main.preferencesUpdate.read(CMD_DATA_WGM, String.valueOf(Main.default_max_weight)));
                limitTenzo = (int) (weightMax / coefficientA);
                writeDataScale();
            }
            return true;
        }

        @Override
        public ScaleInterface getVersion() {
            return this;
        }
    }

}
