package com.victjava.scales;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.database.Cursor;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.*;
import com.victjava.scales.provider.TypeTable;

/*
 * Created with IntelliJ IDEA.
 * User: Kostya
 * Date: 02.10.13
 * Time: 16:57
 * To change this template use File | Settings | File Templates.
 */
public class ActivityType extends ListActivity implements View.OnClickListener {
    //private ListView listView;
    private EditText input;
    private AlertDialog.Builder dialog;
    private TypeTable typeTable;

    //private long entryID;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.type);
        setTitle(getString(R.string.Type));

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
        typeTable = new TypeTable(this);
        dialog = new AlertDialog.Builder(this);
        dialog.setMessage(getString(R.string.Enter_type));
        input = new EditText(this);
        dialog.setView(input);
        dialog.setPositiveButton(getString(R.string.OK), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                if (input.getText() != null) {
                    typeTable.insertEntryType(input.getText().toString());
                    updateList();
                }
            }
        });

        dialog.setNegativeButton(getString(R.string.Close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        findViewById(R.id.buttonBack).setOnClickListener(this);
        findViewById(R.id.buttonNew).setOnClickListener(this);

        updateList();
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.buttonBack:
                onBackPressed();
                break;
            case R.id.buttonNew:
                input = new EditText(getBaseContext());
                dialog.setView(input);
                dialog.show();
                break;
            default:
        }
    }

    void updateList() {
        try {
            Cursor cursor = typeTable.getNotSystemEntries();
            String[] from = {TypeTable.KEY_TYPE};
            int[] to = {R.id.topText};
            ListAdapter adapter = new SimpleCursorAdapter(this, R.layout.list_item_type, cursor, from, to);
            setListAdapter(adapter);
        }catch (Exception e){}

    }

    public void removeAtomPayOnClickHandler(View view) {

        ListView list = getListView();
        int position = list.getPositionForView(view);
        try {
            Cursor cursor = ((CursorAdapter) list.getAdapter()).getCursor();
            cursor.moveToPosition(position);
            int id = cursor.getInt(cursor.getColumnIndex(TypeTable.KEY_ID));
            typeTable.removeEntry(id);
        }catch (Exception e){
            return;
        }
        updateList();
    }
}
