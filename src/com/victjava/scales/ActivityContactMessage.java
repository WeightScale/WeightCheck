package com.victjava.scales;

import android.app.Activity;
import android.content.*;
import android.database.Cursor;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.*;
import android.view.View;
import android.widget.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

/**
 * @author Kostya
 */
public class ActivityContactMessage extends Activity implements View.OnClickListener{
    ListView listViewData;
    ContentValues values = new ContentValues();
    Map<String, ContentValues> mapData;
    int contactId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact_message);
        if(getIntent() != null){
            contactId = getIntent().getIntExtra("contact_id", 0);
        }
        setTitle(getNameContact(contactId));

        ContentQueryMap mQueryMap = new ContentQueryMap(getDataContact(contactId), BaseColumns._ID, true, null);
        mapData = mQueryMap.getRows();

        listViewData = (ListView)findViewById(R.id.listViewData);
        listViewData.setOnItemClickListener(onItemClickListener);

        findViewById(R.id.buttonSelectAll).setOnClickListener(this);
        findViewById(R.id.buttonUnselect).setOnClickListener(this);
        findViewById(R.id.buttonBack).setOnClickListener(this);

        setupDataList();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonSelectAll:
                selectedAll();
                break;
            case R.id.buttonUnselect:
                unselectedAll();
                break;
            case R.id.buttonBack:
                onBackPressed();
                break;
            default:
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
    }

    void setupDataList() {

        String[] from = {Data.DATA1 };
        int[] to = {R.id.text1};
        SimpleCursorAdapter adapterData = new SimpleCursorAdapter(this, R.layout.item_list_message, getDataContact(contactId), from, to);
        adapterData.setViewBinder(new ListMessageBinder());
        listViewData.setAdapter(adapterData);
    }

    /** Получить даные контакта DATA5.
     * DATA5 используем для галочки чтобы хранить выбор.
     * @return Курсор даных.
     */
    Cursor getDataContact(int id){
        return getContentResolver().query(Data.CONTENT_URI, new String[] {BaseColumns._ID, Data.DATA1, Data.DATA5,Data.MIMETYPE},
                Data.CONTACT_ID + "=?" + " and (" + Data.MIMETYPE + "='" + CommonDataKinds.Phone.CONTENT_ITEM_TYPE + '\''
                        +" or "+ Data.MIMETYPE + "='" + CommonDataKinds.Email.CONTENT_ITEM_TYPE + '\'' + ')',new String[] {String.valueOf(id)}, null);
    }

    private class ListMessageBinder implements SimpleCursorAdapter.ViewBinder {
        int enable;
        String text;

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {

            switch (view.getId()) {
                case R.id.text1:
                    enable = cursor.getInt(cursor.getColumnIndex(Data.DATA5));
                    text = cursor.getString(cursor.getColumnIndex(Data.DATA1));
                    setViewText((TextView) view, text);
                    if(enable > 0)
                        ((Checkable) view).setChecked(true);
                    else
                        ((Checkable) view).setChecked(false);
                break;
                default:
                    return false;
            }
            return true;
        }

        public void setViewText(TextView v, CharSequence text) {
            v.setText(text);
        }
    }

    final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Checkable v = (Checkable) view;
            v.toggle();
            updateData5(id, Data.DATA5, 1);
            if (v.isChecked())
                updateData5((int) id, Data.DATA5, 1);
            else
                updateData5((int) id, Data.DATA5, 0);
        }
    };

    public void updateData5(long _rowIndex, String key, int value) {
        ArrayList<ContentProviderOperation> ops = new ArrayList<>();

        ops.add(ContentProviderOperation.newUpdate(Data.CONTENT_URI)
                .withSelection(BaseColumns._ID + "=?", new String[]{String.valueOf(_rowIndex)})
                .withValue(key, value)
                .build());
        try {
            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
        } catch (RemoteException | OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    private String getNameContact(long contactId){
        String name = "null";
        Cursor cursor = getContentResolver().query(Contacts.CONTENT_URI, new String[] {BaseColumns._ID, Contacts.DISPLAY_NAME},
                Contacts._ID + "=?",new String[] {String.valueOf(contactId)}, null);
        if(cursor.moveToFirst()){
            name = cursor.getString(cursor.getColumnIndex(Contacts.DISPLAY_NAME));
        }
        return name;
    }

    private void selectedAll(){
        Cursor cursor = getDataContact(contactId);
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(Data._ID));
                    updateData5((int) id, Data.DATA5, 1);
                } while (cursor.moveToNext());
            }
        }catch (Exception e){ }
    }

    private void unselectedAll(){
        Cursor cursor = getDataContact(contactId);
        try {
            cursor.moveToFirst();
            if (!cursor.isAfterLast()) {
                do {
                    int id = cursor.getInt(cursor.getColumnIndex(Data._ID));
                    updateData5((int) id, Data.DATA5, 0);
                } while (cursor.moveToNext());
            }
        }catch (Exception e){ }
    }

}
