package com.victjava.scales;

import android.database.CharArrayBuffer;
import android.database.Cursor;
import com.google.gdata.client.batch.BatchInterruptedException;
import com.google.gdata.client.spreadsheet.SpreadsheetService;
import com.google.gdata.data.ILink;
import com.google.gdata.data.Link;
import com.google.gdata.data.PlainTextConstruct;
import com.google.gdata.data.batch.BatchOperationType;
import com.google.gdata.data.batch.BatchUtils;
import com.google.gdata.data.spreadsheet.*;
import com.google.gdata.util.AuthenticationException;
import com.google.gdata.util.ServiceException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/*
 * Created by Kostya on 02.03.14.
 */
public class GoogleSpreadsheets {
    private final String username;
    private final String password;
    private final SpreadsheetService spreadsheetService;
    private List<WorksheetEntry> worksheets;
    private SpreadsheetEntry spreadsheetEntry;
    private String spreadsheetName = "";

    public GoogleSpreadsheets(String user, String pass, String spreadsheetName, String service) {
        //Context context = cxt;
        username = user;
        password = pass;
        this.spreadsheetName = spreadsheetName;

        spreadsheetService = new SpreadsheetService(service);
        spreadsheetService.setProtocolVersion(SpreadsheetService.Versions.V3);

        //spreadsheetService.setUserCredentials(username, password);
        //feed = spreadsheetService.getFeed(new URL(URL_FEED), SpreadsheetFeed.class);
        //spreadsheets = getSpreadsheets();

        //spreadsheetEntry = getSpreadsheetEntry(spreadsheetName);//Получаем таблицу по имени

        //UpdateListWorksheets(spreadsheetEntry);

    }

    public void login() throws AuthenticationException {
        // Authenticate
        spreadsheetService.setUserCredentials(username, password);
    }

    /*public void setSpreadsheetName(String Name) {
        spreadsheetName = Name;
    }*/

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

    /*String getSpreadsheetName() {
        return spreadsheetName;
    }*/

    /*SpreadsheetService getSpreadsheetService() {
        return spreadsheetService;
    }*/

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

}
