package com.victjava.scales;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.CharArrayBuffer;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v4.app.NotificationCompat;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.auth.UserRecoverableAuthException;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.google.gdata.client.batch.BatchInterruptedException;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.ILink;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.ServiceException;
import com.konst.module.ScaleModule;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.*;

/*
 * Created by Kostya on 02.03.14.
 */
public abstract class GoogleSpreadsheets extends AsyncTask<Void, Void, String[]> {
    Context context;
    //String token = null;
    /**
     * Экземпляр атестата
     */
    GoogleCredential credential;
    private final SpreadsheetService spreadsheetService;
    private List<WorksheetEntry> worksheets;
    private SpreadsheetEntry spreadsheetEntry;
    //private final String spreadsheetName = "";
    /**
     * Client ID созданый для application в  https://console.developers.google.com/project/
     */
    final String CLIENT_ID = "104626362323-b410it7dt7gad5e1sp9v8aum9nm00biu.apps.googleusercontent.com";
    /**
     * Client secret созданый для application в  https://console.developers.google.com/project/
     */
    final String CLIENT_SECRET = "zLeF20E7Dl1GRlCY-Hfpf4lB";
    /**
     * Redirect URIs созданый для application в  https://console.developers.google.com/project/
     */
    //final String REDIRECT_URI = "https://www.example.com/oauth2callback";
    /**
     * Email address в созданом клиенте на  https://console.developers.google.com/project/
     */
    final String CLIENT_EMAIL = "64738785707-t6gad1u92rpbqleq42lphl13pj0i0f6f@developer.gserviceaccount.com";
    static final String[] SCOPE_ARRAY = {"https://spreadsheets.google.com/feeds ", "https://spreadsheets.google.com/feeds/spreadsheets/private/full ", "https://docs.google.com/feeds "};
    final List<String> SCOPES_LIST = Arrays.asList(SCOPE_ARRAY);
    protected static final String SCOPE = SCOPE_ARRAY[0] + SCOPE_ARRAY[1];

    /**
     * Конструктор GoogleSpreadsheets.
     *
     * @param service Имя сервиса GoogleSpreadsheets.
     */
    protected GoogleSpreadsheets(String service) {
        spreadsheetService = new SpreadsheetService(service);
        spreadsheetService.setProtocolVersion(SpreadsheetService.Versions.V3);
    }

    /**
     * Конструктор GoogleSpreadsheets
     *
     * @param service Имя сервиса GoogleSpreadsheets
     * @param p12     Ключ сгенерированый для моего приложения https://console.developers.google.com/project/
     *                в настройках API создать нового клиента Service account Generate new P12 key
     *                java.io.File p12 = new File(Environment.getExternalStorageDirectory(),"WeightCheck-d76949a134dd.p12")
     * @throws GeneralSecurityException
     * @throws IOException
     */
    protected GoogleSpreadsheets(String service, java.io.File p12) throws GeneralSecurityException, IOException {
        /** Экземпляр атестата */
        credential = new GoogleCredential.Builder()
                .setTransport(new NetHttpTransport())
                .setJsonFactory(new JacksonFactory())
                        /** Нужно зашарить spreadsheet таблицу этим email */
                .setServiceAccountId(CLIENT_EMAIL)
                .setServiceAccountScopes(SCOPES_LIST)
                .setServiceAccountPrivateKeyFromP12File(p12)
                .build();

        spreadsheetService = new SpreadsheetService(service);
        spreadsheetService.setProtocolVersion(SpreadsheetService.Versions.V3);
        spreadsheetService.setOAuth2Credentials(credential);
    }

    /**
     * Конструктор GoogleSpreadsheets
     *
     * @param context     Контекст приложения.
     * @param service     Имя сервиса GoogleSpreadsheets.
     * @param accountName Имя account в google.
     * @throws GoogleAuthException
     * @throws IOException
     */
    protected GoogleSpreadsheets(Context context, String service, String accountName) throws GoogleAuthException, IOException, IllegalArgumentException {
        this.context = context;
        spreadsheetService = new SpreadsheetService(service);
        spreadsheetService.setProtocolVersion(SpreadsheetService.Versions.V3);
        spreadsheetService.setAuthSubToken(getToken(accountName));
    }

    @Override
    protected String[] doInBackground(Void... params) {
        try {
            return new String[]{fetchToken(), ""};
        } catch (Exception e) {
            return new String[]{null, e.getMessage()};
        }
    }

    @Override
    protected void onPostExecute(String... s) {
        if (s[0] != null) {
            spreadsheetService.setAuthSubToken(s[0]);
            tokenIsReceived();
        } else
            tokenIsFalse(s[1]);
    }

    /**
     * Вызываем если токен получен.
     */
    protected abstract void tokenIsReceived();

    /**
     * Вызываем если ошибка получения токена
     *
     * @param error Причина ошибки получения токена
     */
    protected abstract void tokenIsFalse(String error);

    /**
     * Получить токен.
     *
     * @return Взвращяем токен.
     * @throws IOException
     * @throws GoogleAuthException
     * @throws IllegalArgumentException
     */
    protected abstract String fetchToken() throws IOException, GoogleAuthException, IllegalArgumentException;

    /**
     * Разренеие на доступ получено
     */
    protected abstract void permissionIsObtained();

    GoogleCredential getCredentials(String token) throws IOException, GoogleAuthException {

        //token = GoogleAuthUtil.getToken(context, account, "oauth2:" + SCOPE_ARRAY[0]+SCOPE_ARRAY[1]+SCOPE_ARRAY[2]);
        //GoogleAuthUtil.invalidateToken(context, token);
        return new GoogleCredential.Builder()
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                .setJsonFactory(new JacksonFactory())
                .setTransport(new NetHttpTransport()).build()
                .setAccessToken(token);
    }

    GoogleCredential getCredentialsWeb(String token) throws IOException, GoogleAuthException {

        //token = GoogleAuthUtil.getToken(context, account, "oauth2:" + SCOPE);
        //GoogleAuthUtil.invalidateToken(context, token);
        return new GoogleCredential.Builder()
                .setClientSecrets(CLIENT_ID, CLIENT_SECRET)
                .setJsonFactory(new JacksonFactory())
                .setTransport(new NetHttpTransport()).build()
                .setAccessToken(token);
    }

    String getToken(String account) throws IOException, GoogleAuthException, IllegalArgumentException {
        //return token = GoogleAuthUtil.getToken(mContext, account, "oauth2:" + SCOPE);

        //Intent returnIntent = new Intent(context, ActivityScales.class);
        //PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, returnIntent, 0);
        return GoogleAuthUtil.getTokenWithNotification(context, account, "oauth2:" + SCOPE, null, makeCallback());
    }

    protected Intent makeCallback() {
        Intent intent = new Intent();
        intent.setAction("com.victjava.scales.CallbackReceiver");
        //intent.putExtra(HelloActivity.EXTRA_ACCOUNTNAME, accountName);
        //intent.putExtra(HelloActivity.TYPE_KEY, HelloActivity.Type.BACKGROUND.name());
        return intent;
    }

    public SpreadsheetEntry getSheetEntry(String nameSheet) throws Exception {

        String URL_FEED = "https://spreadsheets.google.com/feeds/spreadsheets/private/full";
        SpreadsheetFeed feed = spreadsheetService.getFeed(new URL(URL_FEED), SpreadsheetFeed.class);
        List<SpreadsheetEntry> spreadsheets = feed.getEntries();
        if (!spreadsheets.isEmpty()) {
            for (SpreadsheetEntry spreadsheet : spreadsheets) {
                if (spreadsheet.getTitle().getPlainText().equals(nameSheet)) {
                    spreadsheetEntry = spreadsheet;
                    return spreadsheetEntry;
                }
            }
        }
        throw new Exception("Нет Таблицы с именем " + nameSheet);
    }

    public void UpdateListWorksheets() throws IOException, ServiceException {
        worksheets = spreadsheetEntry.getWorksheets();
    }

    WorksheetEntry getWorksheetEntry(String worksheet_entry) {
        for (WorksheetEntry worksheet : worksheets) {
            if (worksheet.getTitle().getPlainText().equals(worksheet_entry)) {
                return worksheet;
            }
        }
        return null;
    }

    public void addRow(Cursor cursor, String nameWorksheet) throws Exception {

        if (spreadsheetEntry == null) {
            throw new Exception("Spreadsheet is null");
        }

        ArrayList<CharArrayBuffer> buffer = new ArrayList<>();

        for (int i = 0; i < cursor.getColumnCount(); i++) {
            CharArrayBuffer b = new CharArrayBuffer(1);
            cursor.copyStringToBuffer(i, b);
            buffer.add(b);
        }

        WorksheetEntry worksheetEntry;

        try {
            worksheetEntry = getWorksheetEntry(nameWorksheet);
            if (worksheetEntry == null) {
                worksheetEntry = addNewWorksheet(nameWorksheet, spreadsheetEntry, cursor.getColumnNames()); //добавление нового листа
                if (worksheetEntry == null) {
                    throw new Exception("Worksheet is null");//todo удалить WorksheetEntry
                }
                UpdateListWorksheets();
                //todo проверить добавлены все названия столбцов
            }
        } catch (ServiceException e) {
            throw new Exception("506 " + e.getMessage());//new ErrorDBAdapter(context).insertNewEntry("506", e.getMessage());
        } catch (Exception ignored) {
            throw new Exception("507 " + ignored.getMessage());//new ErrorDBAdapter(context).insertNewEntry("507", ignored.getMessage());
        }
        try {
            URL listFeedUrl = worksheetEntry.getListFeedUrl();
            ListEntry row = new ListEntry();
            for (int i = 0; i < buffer.size(); i++) {
                String str = String.copyValueOf(buffer.get(i).data, 0, buffer.get(i).sizeCopied);
                row.getCustomElements().setValueLocal(cursor.getColumnName(i), str);
            }

            /*row =*/
            spreadsheetService.insert(listFeedUrl, row);
        } catch (ServiceException e) {
            //new ErrorDBAdapter(context).insertNewEntry("508", e.getMessage());
            try {
                UpdateListWorksheets();
            } catch (IOException e1) {
                throw new Exception("509 " + e1.getMessage());//new ErrorDBAdapter(context).insertNewEntry("509", e1.getMessage());
            } catch (ServiceException e1) {
                throw new Exception("510 " + e1.getMessage());//new ErrorDBAdapter(context).insertNewEntry("510", e1.getMessage());
            }
            throw new Exception("508 " + e.getMessage());
        } catch (Exception ignored) {
            throw new Exception("511 " + ignored.getMessage());//new ErrorDBAdapter(context).insertNewEntry("511", ignored.getMessage());
        }
    }

    WorksheetEntry addNewWorksheet(String name, SpreadsheetEntry spreadsheet, String... columns) throws IOException {

        WorksheetEntry worksheet = new WorksheetEntry();
        worksheet.setTitle(new PlainTextConstruct(name));
        worksheet.setColCount(columns.length);
        worksheet.setRowCount(1);

        //Добавление нового листа в таблицу
        URL worksheetFeedUrl = spreadsheet.getWorksheetFeedUrl();
        try {
            worksheet = spreadsheetService.insert(worksheetFeedUrl, worksheet);
        } catch (ServiceException e) {
            throw new IOException("Ошибка добавления нового листа " + e);
            //todo сделать что то при ошибке
        }

        //Получить feed для листа
        URL cellFeedUrl = worksheet.getCellFeedUrl();
        //
        CellFeed cellFeed;
        try {
            cellFeed = spreadsheetService.getFeed(cellFeedUrl, CellFeed.class);
        } catch (ServiceException e) {
            try {
                worksheet.delete();
            } catch (ServiceException ignored) {
                return null;
            }
            return null;
        }

        // Список адресов яцеек для заполнения названием столбцов
        Collection<CellAddress> cellAddresses = new ArrayList<>();
        int length = columns.length;
        for (int row = 1; row <= 1; ++row) {
            for (int col = 1; col <= length; ++col) {
                cellAddresses.add(new CellAddress(row, col));
            }
        }

        // Prepare the update
        // getCellEntryMap is what makes the update fast.
        try {
            Map<String, CellEntry> cellEntries = getCellEntryMap(spreadsheetService, cellFeedUrl, cellAddresses);
            int ii = 0;
            CellFeed batchRequest = new CellFeed();
            for (CellAddress cellAddress : cellAddresses) {
                CellEntry batchEntry = new CellEntry(cellEntries.get(cellAddress.idString));
                batchEntry.changeInputValueLocal(columns[ii++]);
                BatchUtils.setBatchId(batchEntry, cellAddress.idString);
                BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.UPDATE);
                batchRequest.getEntries().add(batchEntry);
            }

            // Submit the update
            Link batchLink = cellFeed.getLink(ILink.Rel.FEED_BATCH, ILink.Type.ATOM);
            CellFeed batchResponse = spreadsheetService.batch(new URL(batchLink.getHref()), batchRequest);


            for (CellEntry entry : batchResponse.getEntries()) {
                if (!BatchUtils.isSuccess(entry)) {
                    worksheet.delete();
                    return null;
                }
            }

        } catch (BatchInterruptedException e) {
            e.printStackTrace();
        } catch (ServiceException e) {
            try {
                worksheet.delete();
                return null;
            } catch (ServiceException ignored) {
                return null;
            }
        }

        return worksheet;
    }

    private static Map<String, CellEntry> getCellEntryMap(SpreadsheetService ssSvc, URL cellFeedUrl, Collection<CellAddress> cellAddresses) throws ServiceException {
        CellFeed batchRequest = new CellFeed();
        for (CellAddress cellId : cellAddresses) {
            CellEntry batchEntry = new CellEntry(cellId.row, cellId.col, cellId.idString);
            batchEntry.setId(String.format("%s/%s", cellFeedUrl.toString(), cellId.idString));
            BatchUtils.setBatchId(batchEntry, cellId.idString);
            BatchUtils.setBatchOperationType(batchEntry, BatchOperationType.QUERY);
            batchRequest.getEntries().add(batchEntry);
        }

        CellFeed queryBatchResponse = null;
        try {
            CellFeed cellFeed = ssSvc.getFeed(cellFeedUrl, CellFeed.class);
            queryBatchResponse = ssSvc.batch(new URL(cellFeed.getLink(ILink.Rel.FEED_BATCH, ILink.Type.ATOM).getHref()), batchRequest);

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            throw new ServiceException("Ошибка " + e);
        }

        Map<String, CellEntry> cellEntryMap = new HashMap<>(cellAddresses.size());
        assert queryBatchResponse != null;
        for (CellEntry entry : queryBatchResponse.getEntries()) {
            cellEntryMap.put(BatchUtils.getBatchId(entry), entry);
        }

        return cellEntryMap;
    }

    private static class CellAddress {
        public final int row;
        public final int col;
        public final String idString;

        /**
         * Constructs a CellAddress representing the specified {@code row} and
         * {@code col}.  The idString will be set in 'RnCn' notation.
         */
        public CellAddress(int row, int col) {
            this.row = row;
            this.col = col;
            idString = String.format("R%sC%s", row, col);
        }
    }

    /**
     * Обратный вызов при получении разрешения для токена
     */
    public class CallbackReceiver extends BroadcastReceiver {
        //public static final String TAG = "CallbackReceiver";

        @Override
        public void onReceive(Context context, Intent callback) {
            execute();
        }
    }

    private void tokenNotify(UserRecoverableAuthException exception) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        Intent authorizationIntent = exception.getIntent();
        authorizationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK).addFlags(Intent.FLAG_FROM_BACKGROUND);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, authorizationIntent, 0);
        NotificationCompat.Builder notification = new NotificationCompat.Builder(context)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setTicker("Permission requested")
                .setContentTitle("Permission requested")
                .setContentText("for account " + ScaleModule.getUserName())
                .setContentIntent(pendingIntent).setAutoCancel(true);
        notificationManager.notify(0, notification.build());
    }

}
