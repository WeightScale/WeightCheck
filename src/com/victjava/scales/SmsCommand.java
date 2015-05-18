package com.victjava.scales;

import android.app.ActivityManager;
import android.app.Service;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import com.konst.module.InterfaceVersions;
import com.konst.module.ScaleModule;
import com.victjava.scales.provider.CommandDBAdapter;
import com.victjava.scales.provider.ErrorDBAdapter;
import com.victjava.scales.provider.SenderDBAdapter;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.text.SimpleDateFormat;
import java.util.*;

/*
 * Created by Kostya on 03.04.2015.
 */
public class SmsCommand {
    final Context mContext;
    List<BasicNameValuePair> results;
    SenderDBAdapter senderTable;

    static final String SMS_CMD_GETERR = "geterr"; //получить ошибки параметр количество
    static final String SMS_CMD_DELERR = "delerr"; //удалить ошибки параметр количество
    static final String SMS_CMD_CHGDSC = "chgdsc"; //изменить сервис передачи параметр form-(ипользуется сервис ServiceGetDateServer) sheet-(ипользуется сервис ServiceSendDateServer)
    static final String SMS_CMD_NUMSMS = "numsms"; //номер телефона межд. формат для отправки чеков для босса

    static final String SMS_CMD_WGHMAX = "wghmax"; //максимальный вес
    static final String SMS_CMD_COFFA = "coffa";  //коэфициент вес
    static final String SMS_CMD_COFFB = "coffb";  //коэфициент офсет
    static final String SMS_CMD_GOGUSR = "gogusr"; //учетнное имя google account
    static final String SMS_CMD_GOGPSW = "gogpsw"; //пароль google account
    static final String SMS_CMD_PHNSMS = "phnsms"; //телефон для отправки смс
    static final String SMS_CMD_WRTDAT = "wrtdat";  //записать данные в весы функция writeDataScale() wrtdat=wgm_5000|cfa_0.00019
    static final String SMS_CMD_SNDCHK = "sndchk"; //условия отправки чеков sndchk=0-1,1-1,2-1,3-1 после тире параметр для KEY_SYS TYPE_GOOGLE_DISK-(KEY_SYS).TYPE_HTTP_POST-(KEY_SYS).TYPE_SMS-(KEY_SYS).TYPE_EMAIL-(KEY_SYS)


    static final String RESPONSE_OK = "ok";
    static final String POSTPONED = "postponed";

    private final Map<String, InterfaceSmsCommand> cmdMap = new LinkedHashMap<>();

    {
        cmdMap.put(SMS_CMD_GETERR, new CmdGetError());      //получить ошибки параметр количество
        cmdMap.put(SMS_CMD_DELERR, new CmdDeleteError());   //удалить ошибки параметр количество
        cmdMap.put(SMS_CMD_CHGDSC, new CmdChangeService()); //изменить сервис передачи параметр form-(ипользуется сервис ServiceGetDateServer) sheet-(ипользуется сервис ServiceSendDateServer)
        cmdMap.put(SMS_CMD_NUMSMS, new CmdNumSmsAdmin());
        cmdMap.put(SMS_CMD_WGHMAX, new CmdWeightMax());     //максимальный вес
        cmdMap.put(SMS_CMD_COFFA, new CmdCoefficientA());   //коэфициент вес
        cmdMap.put(SMS_CMD_COFFB, new CmdCoefficientB());
        cmdMap.put(SMS_CMD_GOGUSR, new CmdGoogleUser());    //учетнное имя google account
        cmdMap.put(SMS_CMD_GOGPSW, new CmdGooglePassword());//пароль google account
        cmdMap.put(SMS_CMD_PHNSMS, new CmdPhoneSms());      //телефон для отправки смс
        cmdMap.put(SMS_CMD_WRTDAT, new CmdWeightDate());    //записать данные в весы функция writeDataScale() wrtdat=wgm_5000|cfa_0.00019
        cmdMap.put(SMS_CMD_SNDCHK, new CmdSenderCheck());
        //cmdMap = Collections.unmodifiableMap(cmdMap);
    }

    interface InterfaceSmsCommand {
        BasicNameValuePair execute(String value) throws Exception;
    }

    SmsCommand(Context context) {
        mContext = context;
        senderTable = new SenderDBAdapter(context);
    }

    public SmsCommand(Context context, String msg) throws Exception {
        mContext = context;
        results = parsingSmsCommand(msg);
        senderTable = new SenderDBAdapter(context);
    }

    private List<BasicNameValuePair> parsingSmsCommand(String message) throws Exception {
        if (message.isEmpty()) {
            throw new Exception("message is empty");
        }
        String[] commands = message.split(" ");
        List<BasicNameValuePair> results = new ArrayList<>();
        for (String s : commands) {
            String[] array = s.split("=");
            if (array.length == 2) {
                results.add(new BasicNameValuePair(array[0], array[1]));
            } else if (array.length == 1) {
                results.add(new BasicNameValuePair(array[0], ""));
            }
        }
        return results;
    }

    public StringBuilder commandsExt() {
        StringBuilder textSent = new StringBuilder();
        for (NameValuePair result : results) {
            try {
                textSent.append(cmdMap.get(result.getName()).execute(result.getValue()));
            } catch (Exception e) {
                textSent.append(result.getName()).append('=').append(e.getMessage());
            }
            textSent.append(' ');
        }
        return textSent;
    }

    public List<BasicNameValuePair> getResults() {
        return results;
    }

    private void cmdProrogue(String cmd, String value, String mime) {
        String date = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss").format(new Date());
        ContentValues newTaskValues = new ContentValues();
        newTaskValues.put(CommandDBAdapter.KEY_MIME, mime);
        newTaskValues.put(CommandDBAdapter.KEY_COMMAND, cmd);
        newTaskValues.put(CommandDBAdapter.KEY_VALUE, value);
        newTaskValues.put(CommandDBAdapter.KEY_DATE, date);
        new CommandDBAdapter(mContext).insertNewTask(newTaskValues);
    }

    /*  Команда получения ошибок из памяти
    *   без параметра возвращяет последних 50
    *   параметр указивает количество последних*/
    private class CmdGetError implements InterfaceSmsCommand {//получить ошибки параметр количество

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {//если нет параметра получаем 50 последних
                return new BasicNameValuePair(SMS_CMD_GETERR, new ErrorDBAdapter(mContext).getErrorToString(50));
            } else {
                return new BasicNameValuePair(SMS_CMD_GETERR, new ErrorDBAdapter(mContext).getErrorToString(Integer.valueOf(value)));
            }
        }
    }

    /*  Комманда удаления ошибок сохраненных в памяти
    *   без параметра удаляем все, параметр определяет
    *   сколько удалять последних ошибок в памяти
    * */
    private class CmdDeleteError implements InterfaceSmsCommand {//удалить ошибки параметр количество

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {//если нет параметра удаляем все ошибки
                return new BasicNameValuePair(SMS_CMD_DELERR, String.valueOf(new ErrorDBAdapter(mContext).deleteAll()));
            } else {
                return new BasicNameValuePair(SMS_CMD_DELERR, String.valueOf(new ErrorDBAdapter(mContext).deleteRows(Integer.valueOf(value))));
            }
        }
    }

    /*  Сервис для передачи данных
    *   парамметр form_date (ипользуется сервис ServiceGetDateServer)
    *   sheet_date (ипользуется сервис ServiceSendDateServer)
    * */
    private class CmdChangeService implements InterfaceSmsCommand {


        private final Map<String, Service> serviceMap = new LinkedHashMap<>();

        {
            //serviceMap.put(SERVICE_FORM_DISK, new ServiceSentFormServer());
            //serviceMap.put(SERVICE_SHEET_DISK, new ServiceSentSheetServer());
            //serviceMap = Collections.unmodifiableMap(serviceMap);
        }

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_CHGDSC, Preferences.read(Preferences.KEY_SENT_SERVICE, ""));
            }
            Service service = serviceMap.get(value);
            if (!service.equals(Main.cloud)) {
                if (isMyServiceRunning(Main.cloud.getClass())) {
                    Main.cloud.stopSelf();
                    Main.cloud = service;
                    mContext.startService(new Intent(mContext, Main.cloud.getClass()));
                } else {
                    Main.cloud = service;
                }
            }

            Preferences.write(Preferences.KEY_SENT_SERVICE, value);
            return new BasicNameValuePair(SMS_CMD_CHGDSC, RESPONSE_OK);
        }

        private boolean isMyServiceRunning(Class<?> serviceClass) {
            ActivityManager manager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
            for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
            return false;
        }

    }

    /*  Номер телефона для отправки смс отчетов взвешеного веса
    *   номер в международном формате +380*********
    * */
    private class CmdNumSmsAdmin implements InterfaceSmsCommand {//номер телефона межд. формат для отправки чеков для босса

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_NUMSMS, Preferences.read(Preferences.KEY_NUMBER_SMS, ""));
            }
            Preferences.write(Preferences.KEY_NUMBER_SMS, value);
            return new BasicNameValuePair(SMS_CMD_NUMSMS, RESPONSE_OK);
        }
    }

    /*  Максимальный вес который взвешивают весы*/
    private class CmdWeightMax implements InterfaceSmsCommand {
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_WGHMAX, String.valueOf(ScaleModule.getWeightMax()));
            }
            ScaleModule.setWeightMax(Integer.valueOf(value));
            return new BasicNameValuePair(SMS_CMD_WGHMAX, RESPONSE_OK);
        }
    }

    /*  Кэффициет для расчета веса
    *   определенный во время каллибровки весов
    *   (ноль вес - конт. вес) / (ацп ноль веса - ацп кон. веса)
    *   получить или записать */
    private class CmdCoefficientA implements InterfaceSmsCommand {//номер телефона межд. формат для отправки чеков для босса

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_COFFA, String.valueOf(ScaleModule.getCoefficientA()));
            }
            ScaleModule.setCoefficientA(Float.valueOf(value));
            return new BasicNameValuePair(SMS_CMD_COFFA, RESPONSE_OK);
        }
    }

    /*  Коэффициент оффсет старая команда
    *   ноль вес - Scales.coefficientA * ацп ноль веса */
    private class CmdCoefficientB implements InterfaceSmsCommand {
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_COFFB, String.valueOf(ScaleModule.getCoefficientB()));
            }
            ScaleModule.setCoefficientB(Float.valueOf(value));
            return new BasicNameValuePair(SMS_CMD_COFFB, RESPONSE_OK);
        }
    }

    /*  Google user
    *   имя акаунта созданого в google */
    private class CmdGoogleUser implements InterfaceSmsCommand {
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_GOGUSR, ScaleModule.getUserName());
            }
            if (ActivityScales.isScaleConnect) {
                if (ScaleModule.setModuleUserName(value)) {
                    ScaleModule.setUserName(value);
                    return new BasicNameValuePair(SMS_CMD_GOGUSR, RESPONSE_OK);
                }
            }
            cmdProrogue(SMS_CMD_GOGUSR, value, CommandDBAdapter.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_GOGUSR, POSTPONED);
        }

    }

    /*  Google password
    *   пароль акаунта созданого в google */
    private class CmdGooglePassword implements InterfaceSmsCommand {
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_GOGPSW, ScaleModule.getPassword());
            }
            if (ActivityScales.isScaleConnect) {
                if (ScaleModule.setModulePassword(value)) {
                    ScaleModule.setPassword(value);
                    return new BasicNameValuePair(SMS_CMD_GOGPSW, RESPONSE_OK);
                }
            }
            cmdProrogue(SMS_CMD_GOGPSW, value, CommandDBAdapter.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_GOGPSW, POSTPONED);
        }

    }

    /*  телефон для смс в международном формате +380*********
    *   номер телефона для отправки чеков смс */
    private class CmdPhoneSms implements InterfaceSmsCommand {
        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                return new BasicNameValuePair(SMS_CMD_PHNSMS, ScaleModule.getPhone());
            }
            if (ActivityScales.isScaleConnect) {
                if (ScaleModule.setModulePhone(value)) {
                    ScaleModule.setPhone(value);
                    return new BasicNameValuePair(SMS_CMD_PHNSMS, RESPONSE_OK);
                }
            }
            cmdProrogue(SMS_CMD_PHNSMS, value, CommandDBAdapter.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_PHNSMS, POSTPONED);
        }

    }

    /*  Данные для весов
    *   без параметра вызывает запить сохраненные ранее
    *   с параметром записывает новые данные
    *   формат команды wrtdat=wgm_5000:cfa_0.00019*/
    private class CmdWeightDate implements InterfaceSmsCommand {

        private final Map<String, Data> mapDate = new LinkedHashMap<>();

        {
            mapDate.put(InterfaceVersions.CMD_DATA_CFA, new CoefficientA());
            mapDate.put(InterfaceVersions.CMD_DATA_WGM, new WeightMax());
            //mapDate = Collections.unmodifiableMap(mapDate);
        }

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                try {
                    if (ScaleModule.writeData()) {
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
                        ScaleModule.setLimitTenzo((int) (ScaleModule.getWeightMax() / ScaleModule.getCoefficientA()));
                        if (ScaleModule.getLimitTenzo() > 0xffffff) {
                            ScaleModule.setLimitTenzo(0xffffff);
                            ScaleModule.setWeightMax((int) (0xffffff * ScaleModule.getCoefficientA()));
                        }
                        if (ScaleModule.writeData()) {
                            return new BasicNameValuePair(SMS_CMD_WRTDAT, RESPONSE_OK);
                        }
                    } catch (Exception e) {
                        return new BasicNameValuePair(SMS_CMD_WRTDAT, e.getMessage());
                    }
                }
            }
            cmdProrogue(SMS_CMD_WRTDAT, value, CommandDBAdapter.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_WRTDAT, POSTPONED);
        }

        /*interface InterfaceDate {
            void setValue(Object v);
        }*/

        private abstract class Data{
            abstract void setValue(Object v);
        }

        private class CoefficientA extends Data {
            @Override
            public void setValue(Object v) {
                ScaleModule.setCoefficientA(Float.valueOf(v.toString()));
            }
        }

        private class WeightMax extends Data {
            @Override
            public void setValue(Object v) {
                ScaleModule.setWeightMax(Integer.valueOf(v.toString()));
            }
        }

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

    /*
    * Установка параметров для сендера SenderDBAdapter
    *   0-1,1-1,2-1,3-1 формат параметры через запятую
    *
    *   с лева TYPE_SENDER(TYPE_GOOGLE_DISK TYPE_HTTP_POST TYPE_SMS TYPE_EMAIL) с права KEY_SYS(0 или 1)
    *
    */
    private class CmdSenderCheck implements InterfaceSmsCommand {

        @Override
        public BasicNameValuePair execute(String value) throws Exception {
            if (value.isEmpty()) {
                StringBuilder sender = new StringBuilder();
                Cursor cursor = senderTable.getAllEntries();
                try {
                    if (cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        if (!cursor.isAfterLast()) {
                            do {
                                int type = cursor.getInt(cursor.getColumnIndex(SenderDBAdapter.KEY_TYPE));
                                int sys = cursor.getInt(cursor.getColumnIndex(SenderDBAdapter.KEY_SYS));
                                sender.append(type).append('-').append(sys).append('.');
                            } while (cursor.moveToNext());
                        }
                    }
                    return new BasicNameValuePair(SMS_CMD_SNDCHK, sender.toString());
                } catch (Exception e) {
                    return new BasicNameValuePair(SMS_CMD_SNDCHK, "");
                }
            }

            String[] pairs = value.split(",");
            for (String pair : pairs) {
                try {
                    String[] val = pair.split("-");
                    if (val.length > 1) {
                        Cursor sender = senderTable.getTypeItem(Integer.valueOf(val[0]));
                        sender.moveToFirst();
                        int id = sender.getInt(sender.getColumnIndex(SenderDBAdapter.KEY_ID));
                        senderTable.updateEntry(id, SenderDBAdapter.KEY_SYS, Integer.valueOf(val[1]) & 1);
                    }
                } catch (Exception e) {

                }
            }
            cmdProrogue(SMS_CMD_SNDCHK, value, CommandDBAdapter.MIME_SCALE);
            return new BasicNameValuePair(SMS_CMD_SNDCHK, POSTPONED);
        }
    }
}
