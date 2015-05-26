package com.victjava.scales;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.database.CursorWrapper;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Vibrator;
import android.provider.ContactsContract;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import com.victjava.scales.provider.CheckTable;

/*
 * Created with IntelliJ IDEA.
 * User: VictorJava
 * Date: 25.09.13
 * Time: 16:59
 * To change this template use File | Settings | File Templates.
 */
public class ActivityContact extends ListActivity implements View.OnClickListener {

    private CheckTable checkTable;
    private Vibrator vibrator; //вибратор
    private EditText textSearch;
    private LinearLayout layoutSearch;
    private SimpleCursorAdapter adapter;
    //static final int PICK_CONTACT = 2;

    //private String contactID;     // contacts unique ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.contact);
        String action = getIntent().getAction();
        if ("check".equals(action))
            setTitle("Выберите контакт для чека");
        else
            setTitle(getString(R.string.Contacts));

        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);

        checkTable = new CheckTable(this);

        ListView listView = getListView();
        listView.setLongClickable(true);
        listView.setClickable(true);
        listView.setItemsCanFocus(false);
        listView.setOnItemLongClickListener(onItemLongClickListener);
        listView.setOnItemClickListener(onItemClickListener);

        textSearch = (EditText) findViewById(R.id.textSearch);
        textSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.getFilter().filter(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        layoutSearch = (LinearLayout) findViewById(R.id.layoutSearch);
        layoutSearch.setVisibility(View.GONE);


        findViewById(R.id.buttonBack).setOnClickListener(this);
        findViewById(R.id.buttonSearch).setOnClickListener(this);
        findViewById(R.id.buttonNew).setOnClickListener(this);
        findViewById(R.id.closedSearch).setOnClickListener(this);

        setupList();

        try {
            String contact = getIntent().getStringExtra("nameFilter");
            if (!contact.isEmpty()) {
                layoutSearch.setVisibility(View.VISIBLE);
                textSearch.setText(contact);
                textSearch.requestFocus();
            }
        } catch (Exception e) {

        }


    }

    private String retrieveContactName(Uri uriContact) {

        Cursor cursor = getContentResolver().query(uriContact, null, null, null, null);
        if (cursor == null) {
            return null;
        }
        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
        }
        cursor.close();
        return contactName;
    }

    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    void setupList() {
        String[] from;
        from = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ? new String[]{ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.PHOTO_URI} : new String[]{ContactsContract.Contacts.DISPLAY_NAME, ContactsContract.Contacts.Photo.CONTENT_DIRECTORY};
        int[] to = {R.id.contactName, R.id.contactPhoto};
        adapter = new SimpleCursorAdapter(this, R.layout.item_contact, getContact(), from, to);
        adapter.setFilterQueryProvider(new FilterQueryProvider() {
            @Override
            public Cursor runQuery(CharSequence constraint) {
                return new FilterCursorWrapper(getContact(), constraint.toString(), ContactsContract.Contacts.DISPLAY_NAME);
            }
        });
        setListAdapter(adapter);
    }

    Cursor getContact() {
        return getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, null, null, null, ContactsContract.Contacts.DISPLAY_NAME + " ASC");
    }

    private final AdapterView.OnItemLongClickListener onItemLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
            vibrator.vibrate(100L);
            CharSequence[] colors = {getString(R.string.CHANGE), getString(R.string.DELETE), getString(R.string.SELECT), getString(R.string.BACK)};
            TextView textTop = (TextView) view.findViewById(R.id.contactName);
            int contactID = (int) id;
            String name = " ";
            AlertDialog.Builder builder = new AlertDialog.Builder(ActivityContact.this);
            if (textTop.getText() != null) {
                name = textTop.getText().toString();
            }
            builder.setTitle(getString(R.string.Vendor) + ' ' + name);
            builder.setCancelable(false);
            final int finalContactID = contactID;
            final String finalName = name;
            builder.setItems(colors, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, finalContactID);
                    Intent intent;
                    switch (which) {
                        case 0:
                            intent = new Intent(Intent.ACTION_VIEW, contactUri);
                            startActivity(intent);
                            break;
                        case 1:
                            AlertDialog.Builder deleteDialog = new AlertDialog.Builder(ActivityContact.this);
                            deleteDialog.setTitle(getString(R.string.Removing) + finalName);
                            deleteDialog.setMessage(getString(R.string.Do_you_want_to_remove));
                            deleteDialog.setCancelable(false);
                            deleteDialog.setPositiveButton(getString(R.string.Yes), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    getContentResolver().delete(contactUri, null, null);
                                    adapter.changeCursor(getContact());
                                }
                            });
                            deleteDialog.setNegativeButton(getString(R.string.No), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });
                            deleteDialog.show();
                            break;
                        case 2:
                            insertNewCheck(finalContactID);
                            break;
                        case 3:
                            dialog.cancel();
                            break;
                        default:
                    }
                    // the user clicked on colors[which]
                }
            });
            builder.show();
            return false;
        }
    };

    private final AdapterView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            insertNewCheck((int) id);
        }
    };

    private void insertNewCheck(int id) {
        vibrator.vibrate(100);
        Uri contactUri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
        String action = getIntent().getAction();
        if (action != null) {
            switch (action) {
                case "check":
                    String name = retrieveContactName(contactUri);
                    String entryID = checkTable.insertNewEntry(name, id, CheckTable.DIRECT_DOWN).getLastPathSegment();
                    startActivity(new Intent().setClass(getApplicationContext(), ActivityCheck.class).putExtra("id", entryID));
                    finish();
                    break;
                /*case "down":
                    String name = retrieveContactName(contactUri);
                    String entryID = checkDBAdapter.insertNewEntry(name, id, CheckDBAdapter.DIRECT_DOWN).getLastPathSegment();
                    //startActivity(new Intent().setClass(getApplicationContext(), ActivityInputCheck.class).putExtra("id", entryID));
                    startActivity(new Intent().setClass(getApplicationContext(), ActivityCheck.class).putExtra("id", entryID));
                    finish();
                break;
                case "up":
                    String name = retrieveContactName(contactUri);
                    String entryID = checkDBAdapter.insertNewEntry(name, id, CheckDBAdapter.DIRECT_UP).getLastPathSegment();
                    startActivity(new Intent().setClass(getApplicationContext(), ActivityOutputCheck.class).putExtra("id", entryID));
                    finish();
                break;*/
                case "contact":
                    Intent intent = new Intent(Intent.ACTION_VIEW, contactUri);
                    startActivity(intent);
                    break;
            }
        }
    }

    @Override
    public void onClick(View v) {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        switch (v.getId()) {
            case R.id.buttonBack:
                onBackPressed();
                break;
            case R.id.buttonSearch:
                layoutSearch.setVisibility(View.VISIBLE);
                textSearch.requestFocus();
                imm.showSoftInput(textSearch, InputMethodManager.SHOW_IMPLICIT);
                break;
            case R.id.buttonNew:
                Intent intent = new Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI);
                startActivity(intent);
                break;
            case R.id.closedSearch:
                imm.hideSoftInputFromWindow(textSearch.getWindowToken(), 0);
                adapter.getFilter().filter("");
                textSearch.setText("");
                layoutSearch.setVisibility(View.GONE);
                break;
        }
    }

    private static class FilterCursorWrapper extends CursorWrapper {

        private final String filter;
        private final int column;
        private final int[] index;
        private int count;
        private int pos;

        FilterCursorWrapper(Cursor cursor, String filter, String column) {
            super(cursor);
            this.filter = filter.toLowerCase();
            this.column = cursor.getColumnIndex(column);
            if (this.filter.isEmpty()) {
                count = super.getCount();
                index = new int[count];
                for (int i = 0; i < count; i++) {
                    index[i] = i;
                }
            } else {
                count = super.getCount();
                index = new int[count];
                for (int i = 0; i < count; i++) {
                    super.moveToPosition(i);
                    String[] split = getString(this.column).toLowerCase().split(" ");
                    for (String str : split) {
                        if (str.trim().startsWith(this.filter)) {
                            index[pos++] = i;
                        }
                    }
                }
                count = pos;
                pos = 0;
                super.moveToFirst();
            }
        }

        @Override
        public boolean move(int offset) {
            return moveToPosition(pos + offset);
        }

        @Override
        public boolean moveToNext() {
            return moveToPosition(pos + 1);
        }

        @Override
        public boolean moveToPrevious() {
            return moveToPosition(pos - 1);
        }

        @Override
        public boolean moveToFirst() {
            return moveToPosition(0);
        }

        @Override
        public boolean moveToLast() {
            return moveToPosition(count - 1);
        }

        @Override
        public boolean moveToPosition(int position) {
            return !(position >= count || position < 0) && super.moveToPosition(index[position]);
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public int getPosition() {
            return pos;
        }

    }
}

