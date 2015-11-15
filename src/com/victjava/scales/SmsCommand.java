package com.victjava.scales;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import com.konst.module.InterfaceVersions;
import com.konst.module.ScaleModule;
import com.victjava.scales.provider.CommandTable;
import com.victjava.scales.provider.ErrorTable;
import com.victjava.scales.provider.SenderTable;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Класс смс команд.
 *
 * @author Kostya
 */
public class SmsCommand extends SenderTable {
    final Context mContext;
    final ScaleModule scaleModule;
    //List<BasicNameValuePair> results;
    //final List<SmsCommander.Command> commandList;
    Map<String, String> mapCommand;
    //final SenderTable senderTable;

    /**
     * Получить ошибки параметр количество.
     */
    static final String SMS_CMD_GETERR = "geterr";

    /**
     * Удалить ошибки параметр количество.
     */
    static final String SMS_CMD_DELERR = "delerr";

    /**
     * Номер телефона межд. формат для отправки чеков для босса.
     */
    static final String SMS_CMD_NUMSMS = "numsms";

    /**
     * Максимальный вес.
     */
    static final String SMS_CMD_WGHMAX = "wghmax";

    /**
     * Коэфициент вес.
     */
    static final String SMS_CMD_COFFA = "coffa";

    /**
     * Коэфициент офсет.
     */
    static final String SMS_CMD_COFFB = "coffb";

    /**
     * Учетнное имя google account.
     */
    static final String SMS_CMD_GOGUSR = "gogusr";

    /**
     * Пароль google account.
     */
    static final String SMS_CMD_GOGPSW = "gogpsw";

    /**
     * Телефон для отправки смс.
     */
    static final String SMS_CMD_PHNSMS = "phnsms";

    /**
     * Записать данные в весы функция writeDataScale() wrtdat=wgm_5000|cfa_0.00019.
     */
    static final String SMS_CMD_WRTDAT = "wrtdat";

    /** Условия отправки чеков sndchk=0-1,1-1,2-1,3-1.
     * После тире параметр для KEY_SYS TYPE_GOOGLE_DISK-(KEY_SYS).TYPE_HTTP_POST-(KEY_SYS).TYPE_SMS-(KEY_SYS).TYPE_EMAIL-(KEY_SYS).
     */
    static final String SMS_CMD_SNDCHK = "sndchk";

    static final String RESPONSE_OK = "ok";
    /**
     * Отложено.
     */
    static final String POSTPONED = "postponed";

    /**
     * Контейнер смс команд.
     */
    private final Map<String, InterfaceSmsCommand> cmdMap = new LinkedHashMap<>();

    interface InterfaceSmsCommand {
        BasicNameValuePair execute(String value) throws Exception;
    }

    public SmsCommand(Context context, Map<String,String> map) {
        super(context);
        mContext = context;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        //this.commandList = commandList;
        mapCommand = map;
        //senderTable = new SenderTable(context);
        cmdMap.put(SMS_CMD_GETERR, new CmdGetError());      //получить ошибки параметр количество
        cmdMap.put(SMS_CMD_DELERR, new CmdDeleteError());   //удалить ошибки параметр количество
        cmdMap.put(SMS_CMD_NUMSMS, new CmdNumSmsAdmin());   //номер телефона босса
        cmdMap.put(SMS_CMD_WGHMAX, new CmdWeightMax());     //максимальный вес
        cmdMap.put(SMS_CMD_COFFA, new CmdCoefficientA());   //коэфициент вес
        cmdMap.put(SMS_CMD_COFFB, new CmdCoefficientB());   //
        cmdMap.put(SMS_CMD_GOGUSR, new CmdGoogleUser());    //учетнное имя google account
        cmdMap.put(SMS_CMD_GOGPSW, new CmdGooglePassword());//пароль google account
        cmdMap.put(SMS_CMD_PHNSMS, new CmdPhoneSms());      //телефон для отправки смс
        cmdMap.put(SMS_CMD_WRTDAT, new CmdWeightData());    //записать данные в весы функция writeDataScale() wrtdat=wgm_5000|cfa_0.00019
        cmdMap.put(SMS_CMD_SNDCHK, new CmdSenderCheck());   //установка отсылателей чеков
        //cmdMap = Collections.unmodifiableMap(cmdMap);
    }

    public SmsCommand(Context context) {
        super(context);
        mContext = context;
        scaleModule = ((Main)mContext.getApplicationContext()).getScaleModule();
        cmdMap.put(SMS_CMD_GETERR, new CmdGetError());      //получить ошибки параметр количество
        cmdMap.put(SMS_CMD_DELERR, new CmdDeleteError());   //удалить ошибки параметр количество
        cmdMap.put(SMS_CMD_NUMSMS, new CmdNumSmsAdmin());   //номер телефона босса
        cmdMap.put(SMS_CMD_WGHMAX, new CmdWeightMax());     //максимальный вес
        cmdMap.put(SMS_CMD_COFFA, new CmdCoefficientA());   //коэфициент вес
        cmdMap.put(SMS_CMD_COFFB, new CmdCoefficientB());   //
        cmdMap.put(SMS_CMD_GOGUSR, new CmdGoogleUser());    //учетнное имя google account
        cmdMap.put(SMS_CMD_GOGPSW, new CmdGooglePassword());//пароль google account
        cmdMap.put(SMS_CMD_PHNSMS, new CmdPhoneSms());      //телефон для отправки смс
        cmdMap.put(SMS_CMD_WRTDAT, new CmdWeightData());    //записать данные в весы функция writeDataScale() wrtdat=wgm_5000|cfa_0.00019
        cmdMap.put(SMS_CMD_SNDCHK, new CmdSenderCheck());   //установка отсылателей чеков
    }

    public void execute(String cmd, String value){
        try {
            cmdMap.get(cmd).execute(value);
        } catch (Exception e) { }
    }

    /** Выполнить команды в смс сообщении.
     * @return Результат выполнения.
     */
    public StringBuilder process() {
        StringBuilder textSent = new StringBuilder();
        for (Map.Entry<String,String> entry : mapCommand.entrySet()){
            try {
                textSent.append(cmdMap.get(entry.getKey()).execute(entry.getValue()));
            } catch (Exception e) {
                textSent.append(entry.getKey()).append('=').append(e.getMessage());
            }
            textSent.append(' ');
        }

        return textSent;
    }

    /**Сохранить команду как отсроченую.
     * @param cmd   Имя команды.
     * @param value Параметр команды.
     * @param mime  Миме тип команды.
     */
    private void cmdProrogue(String cmd, String value, String mime) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(new Date());
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(CommandTable.KEY_MIME, mime);
        newTaskValues.put(CommandTable.KEY_COMMAND, cmd);
        newTaskValues.put(CommandTable.KEY_VALUE, value);
        newTaskValues.put(CommandTable.KEY_DATE, date);
        new CommandTable(mContext).insertNewTask(newTaskValues);
    }

    /**Получения ошибок из памяти сохраненых в программе.
     * без параметра возвращяет последних 50 записей.
     * параметр указивает количество последних
     */
    private class CmdGetError implements InterfaceSmsCommand {//получить ошибки параметр количество

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            return value.isEmpty() ? new BasicNameValuePair(SMS_CMD_GETERR, new ErrorTable(mContext).getErrorToString(50))
                    : new BasicNameValuePair(SMS_CMD_GETERR, new ErrorTable(mContext).getErrorToString(Integer.valueOf(value)));
        }
    }

    /**Удаления ошибок сохраненных в памяти программы.
     * Без параметра удаляем все,
     * параметр определяет сколько удалять последних ошибок в памяти
     */
    private class CmdDeleteError implements InterfaceSmsCommand {//удалить ошибки параметр количество

        /**Выполнить команду удаление ошибок.
         * @param value Параметр команды.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            return value.isEmpty() ? new BasicNameValuePair(SMS_CMD_DELERR, String.valueOf(new ErrorTable(mContext).deleteAll()))
                    : new BasicNameValuePair(SMS_CMD_DELERR, String.valueOf(new ErrorTable(mContext).deleteRows(Integer.valueOf(value))));
        }
    }

    /**Номер телефона для отправки смс отчетов весовых чеков.
     * номер телефона в международном формате +380xx xxxxxxx
     */
    private class CmdNumSmsAdmin implements InterfaceSmsCommand {//номер телефона межд. формат для отправки чеков для босса

        /**Выполнить команду номер телефона
         * @param value Параметр команды.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_NUMSMS, Preferences.read(Preferences.KEY_NUMBER_SMS, ""));
            }
            if (ActivityScales.isScaleConnect) {
                if (scaleModule.setModulePhone(value)) {
                    scaleModule.setPhone(value);
                    return new BasicNameValuePair(SMS_CMD_NUMSMS, RESPONSE_OK);
                }
            }
            cmdProrogue(SMS_CMD_NUMSMS, value, CommandTable.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_NUMSMS, POSTPONED);

            /*Preferences.write(Preferences.KEY_NUMBER_SMS, value);
            return new BasicNameValuePair(SMS_CMD_NUMSMS, RESPONSE_OK);*/
        }
    }

    /**
     * Устанавливаем максимальный вес предела взвешивания весов.
     */
    private class CmdWeightMax implements InterfaceSmsCommand {

        /** Выполнить команду максимальный вес.
         * @param value Параметр команды.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_WGHMAX, String.valueOf(scaleModule.getWeightMax()));
            }
            scaleModule.setWeightMax(Integer.valueOf(value));
            return new BasicNameValuePair(SMS_CMD_WGHMAX, RESPONSE_OK);
        }
    }

    /**Получаем или записываем коэффициет для расчета веса.
     * Определяется во время каллибровки весов.
     * (ноль вес - конт. вес) / (ацп ноль веса - ацп кон. веса)
     */
    private class CmdCoefficientA implements InterfaceSmsCommand {//номер телефона межд. формат для отправки чеков для босса

        /** Выполнить команду коэфициент А.
         * @param value Параметр команды если параметр есть тогда сохраняем парамет, иначе возвращяем.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_COFFA, String.valueOf(scaleModule.getCoefficientA()));
            }
            scaleModule.setCoefficientA(Float.valueOf(value));
            return new BasicNameValuePair(SMS_CMD_COFFA, RESPONSE_OK);
        }
    }

    /** Получаем или записываем коэффициент оффсет (старая команда)
     * ноль вес - Scales.coefficientA * ацп ноль веса.
     */
    private class CmdCoefficientB implements InterfaceSmsCommand {

        /** Выполнить команду коэфициент В.
         * @param value Параметр команды если параметр есть тогда сохраняем парамет, иначе возвращяем.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_COFFB, String.valueOf(scaleModule.getCoefficientB()));
            }
            scaleModule.setCoefficientB(Float.valueOf(value));
            return new BasicNameValuePair(SMS_CMD_COFFB, RESPONSE_OK);
        }
    }

    /** Аккаунт Google.
     * Имя акаунта созданого в google
     */
    private class CmdGoogleUser implements InterfaceSmsCommand {

        /** Выполнить команду аккаунт Google.
         * @param value Параметр команды если параметр есть тогда сохраняем парамет, иначе возвращяем.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_GOGUSR, scaleModule.getUserName());
            }
            if (ActivityScales.isScaleConnect) {
                if (scaleModule.setModuleUserName(value)) {
                    scaleModule.setUserName(value);
                    return new BasicNameValuePair(SMS_CMD_GOGUSR, RESPONSE_OK);
                }
            }
            cmdProrogue(SMS_CMD_GOGUSR, value, CommandTable.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_GOGUSR, POSTPONED);
        }

    }

    /**Пароль акаунта Google.
     * Пароль акаунта созданого в google
     */
    private class CmdGooglePassword implements InterfaceSmsCommand {

        /**Выполнить команду пароль аккаунта Google.
         * @param value Параметр команды если параметр есть тогда сохраняем парамет, иначе возвращяем.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_GOGPSW, scaleModule.getPassword());
            }
            if (ActivityScales.isScaleConnect) {
                if (scaleModule.setModulePassword(value)) {
                    scaleModule.setPassword(value);
                    return new BasicNameValuePair(SMS_CMD_GOGPSW, RESPONSE_OK);
                }
            }
            cmdProrogue(SMS_CMD_GOGPSW, value, CommandTable.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_GOGPSW, POSTPONED);
        }

    }

    /**Телефон для смс в международном формате +380ххххххххх.
     * Номер телефона для отправки чеков смс
     */
    private class CmdPhoneSms implements InterfaceSmsCommand {

        /** Выполнить команду телефон для смс.
         * @param value Параметр команды если параметр есть тогда сохраняем парамет, иначе возвращяем.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_PHNSMS, scaleModule.getPhone());
            }
            if (ActivityScales.isScaleConnect) {
                if (scaleModule.setModulePhone(value)) {
                    scaleModule.setPhone(value);
                    return new BasicNameValuePair(SMS_CMD_PHNSMS, RESPONSE_OK);
                }
            }
            cmdProrogue(SMS_CMD_PHNSMS, value, CommandTable.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_PHNSMS, POSTPONED);
        }

    }

    /** Данные настройки весового модуля.
     * Без параметра возвращяет запить раннее сохраненые.
     * формат команды wrtdat=wgm_5000:cfa_0.00019
     */
    private final class CmdWeightData implements InterfaceSmsCommand {

        /**
         * Контейнер команд.
         */
        private final Map<String, Data> mapDate = new LinkedHashMap<>();

        private CmdWeightData() {
            mapDate.put(InterfaceVersions.CMD_DATA_CFA, new CoefficientA());
            mapDate.put(InterfaceVersions.CMD_DATA_WGM, new WeightMax());
            //mapDate = Collections.unmodifiableMap(mapDate);
        }

        /** Выполнить команду данные настроек модуля.
         * @param value Параметр команды если параметр есть тогда сохраняем парамет, иначе возвращяем.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                try {
                    if (scaleModule.writeData()) {
                        return new BasicNameValuePair(SMS_CMD_WRTDAT, RESPONSE_OK);
                    }
                } catch (Exception e) {
                }

            } else {
                if (ActivityScales.isScaleConnect) {
                    try {
                        Map<String, String> values = parseValueDataScale(value);
                        for (Map.Entry<String, String> entry : values.entrySet()) {
                            try {
                                mapDate.get(entry.getKey()).setValue(entry.getValue());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        scaleModule.setLimitTenzo((int) (scaleModule.getWeightMax() * scaleModule.getCoefficientA()));
                        //scaleModule.setLimitTenzo((int) (scaleModule.getWeightMax() / scaleModule.getCoefficientA()));
                        if (scaleModule.getLimitTenzo() > 0xffffff) {
                            scaleModule.setLimitTenzo(0xffffff);
                            scaleModule.setWeightMax((int) (0xffffff / scaleModule.getCoefficientA()));
                            //scaleModule.setWeightMax((int) (0xffffff * scaleModule.getCoefficientA()));
                        }
                        if (scaleModule.writeData()) {
                            return new BasicNameValuePair(SMS_CMD_WRTDAT, RESPONSE_OK);
                        }
                    } catch (Exception e) {
                        return new BasicNameValuePair(SMS_CMD_WRTDAT, e.getMessage());
                    }
                }
            }
            cmdProrogue(SMS_CMD_WRTDAT, value, CommandTable.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_WRTDAT, POSTPONED);
        }

        /**
         * Абстрактный класс для установки значений команды Data.
         */
        private abstract class Data {
            abstract void setValue(Object v);
        }

        /**
         * Установить коэфициент А.
         */
        private class CoefficientA extends Data {
            @Override
            public void setValue(Object v) {
                scaleModule.setCoefficientA(Float.valueOf(v.toString()));
            }
        }

        /**
         * Установить максимальный предел взвешивания.
         */
        private class WeightMax extends Data {
            @Override
            public void setValue(Object v) {
                scaleModule.setWeightMax(Integer.valueOf(v.toString()));
            }
        }

        /** Парсер значений команд внутри команды Data.
         * @param value Параметр команды Data.
         * @return Возвращяет контейнер команд.
         * @throws Exception Ошибка парсинга.
         */
        Map<String, String> parseValueDataScale(String value) throws Exception {
            if (value.isEmpty())
                throw new Exception("value is empty");
            String[] commands = value.split(":");
            Map<String, String> results = new HashMap<>();
            for (String s : commands) {
                String[] array = s.split("_");
                if (array.length == 2) {
                    results.put(array[0], array[1]);
                } else if (array.length == 1) {
                    results.put(array[0], "");
                }
            }
            return results;
        }
    }

    /** Установка параметров для сендера SenderDBAdapter.
     * Сендеры - отсылатели сообщений (email, sms, disk, http).
     * Формат параметра [ [[значение 1]-[параметр 2]]_[[значение 2]-[параметр 2]]_[[значение n]-[параметр n]]
     * Значение TYPE_SENDER - TYPE_GOOGLE_DISK, TYPE_HTTP_POST, TYPE_SMS TYPE_EMAIL.
     * Параметр KEY_SYS - 0 или 1.
     */
    private class CmdSenderCheck implements InterfaceSmsCommand {

        /** Выполнить команду параметры сендера.
         * @param value Параметр команды если параметр есть тогда сохраняем парамет, иначе возвращяем.
         * @return Возвращяем результат выполнения команды.
         * @throws Exception Исключение при выполнении.
         */
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                StringBuilder sender = new StringBuilder();
                Cursor cursor = getAllEntries();
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        if (!cursor.isAfterLast()) {
                            do {
                                int type = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_TYPE));
                                int sys = cursor.getInt(cursor.getColumnIndex(SenderTable.KEY_SYS));
                                sender.append(type).append('-').append(sys).append('.');
                            } while (cursor.moveToNext());
                        }
                    }
                    return new BasicNameValuePair(SMS_CMD_SNDCHK, sender.toString());
                } catch (Exception e) {
                    return new BasicNameValuePair(SMS_CMD_SNDCHK, "");
                }
            }

            String[] pairs = value.split("_");
            for (String pair : pairs) {
                try {
                    String[] val = pair.split("-");
                    if (val.length > 1) {
                        Cursor sender = getTypeItem(Integer.valueOf(val[0]));
                        sender.moveToFirst();
                        int id = sender.getInt(sender.getColumnIndex(SenderTable.KEY_ID));
                        updateEntry(id, SenderTable.KEY_SYS, Integer.valueOf(val[1]) & 1);
                    }
                } catch (Exception e) {

                }
            }
            //cmdProrogue(SMS_CMD_SNDCHK, value, CommandTable.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_GOGPSW, RESPONSE_OK);
        }
    }
}
