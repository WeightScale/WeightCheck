package com.victjava.scales;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.*;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.widget.*;
import com.victjava.scales.provider.TaskTable;

/*
 * Created by Kostya on 09.01.2015.
 */
public class TaskMessageDialog extends TaskTable {
    //final TaskTable taskTable;
    protected final Context mContext;
    protected final int mContactId;
    protected final int mCheckId;
    protected final Dialog dialog;
    protected final ContentResolver contentResolver;

    public TaskMessageDialog(Context context, int contactId, int checkId) {
        super(context);
        mContext = context;
        mContactId = contactId;
        mCheckId = checkId;
        dialog = new Dialog(mContext);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        contentResolver = mContext.getContentResolver();
        //taskTable = new TaskTable(context);
    }

    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void openListEmailDialog() {

        final Cursor emails = contentResolver.query(ContactsContract.CommonDataKinds.Email.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Email.CONTACT_ID + " = " + mContactId, null, null);
        if (emails == null) {
            return;
        }
        if (emails.moveToFirst()) {
            String[] columns = {ContactsContract.CommonDataKinds.Email.DATA};
            int[] to = {R.id.title};
            SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(mContext, R.layout.item_list_dialog, emails, columns, to);
            cursorAdapter.setViewBinder(new MyViewBinder(R.drawable.mail1));
            //LayoutInflater layoutInflater = mContext.getLayoutInflater();
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View convertView = layoutInflater.inflate(R.layout.dialog_list, null);
            ListView listView = (ListView) convertView.findViewById(R.id.component_list);
            TextView dialogTitle = (TextView) convertView.findViewById(R.id.dialog_title);
            dialogTitle.setText(mContext.getString(R.string.Send_mail));
            listView.setAdapter(cursorAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.CommonDataKinds.Email.CONTENT_URI, id);
                    Cursor result = contentResolver.query(uri, new String[]{ContactsContract.CommonDataKinds.Email.DATA}, null, null, null);
                    if (result != null) {
                        if (result.moveToFirst()) {
                            String str = result.getString(result.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_MAIL_CONTACT, mCheckId, mContactId, str);
                        }
                    }
                    dialog.dismiss();
                }
            });
            dialog.setContentView(convertView);
            dialog.setCancelable(false);
            Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
            Button buttonAll = (Button) dialog.findViewById(R.id.buttonAll);
            buttonAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    emails.moveToFirst();
                    if (!emails.isAfterLast()) {
                        do {
                            String str = emails.getString(emails.getColumnIndex(ContactsContract.CommonDataKinds.Email.DATA));
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_MAIL_CONTACT, mCheckId, mContactId, str);
                        } while (emails.moveToNext());
                    }
                    dialog.dismiss();
                }
            });
            Button buttonNew = (Button) dialog.findViewById(R.id.buttonNew);
            buttonNew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    createEmailDialog();
                }
            });
            dialog.show();
        } else {
            createEmailDialog();
        }
    }

    private void createEmailDialog() {
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setMessage(mContext.getString(R.string.Enter_address));
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        alertDialog.setView(input);
        alertDialog.setIcon(mContext.getResources().getDrawable(R.drawable.mail1));
        alertDialog.setPositiveButton(mContext.getString(R.string.Send), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!input.getText().toString().isEmpty()) {
                    insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_MAIL_CONTACT, mCheckId, mContactId, input.getText().toString());
                    dialog.dismiss();
                }
            }
        });
        alertDialog.setNegativeButton(mContext.getString(R.string.Close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertDialog.setNeutralButton(mContext.getString(R.string.Save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().isEmpty()) {
                    if (addEmailToContact(input.getText().toString()) != null) {
                        openListEmailDialog();
                    } else {
                        Toast.makeText(mContext, mContext.getString(R.string.TEXT_MESSAGE11), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        alertDialog.show();


    }

    //@TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private Uri addEmailToContact(String mail) {
        Cursor cursor = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts.CONTACT_ID + " = " + mContactId, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            int rawContactId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            ContentValues values = new ContentValues();
            values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
                values.put(ContactsContract.CommonDataKinds.Email.ADDRESS, mail);
            else
                values.put(ContactsContract.CommonDataKinds.Email.DISPLAY_NAME, mail);
            values.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
            return contentResolver.insert(ContactsContract.Data.CONTENT_URI, values);
        }
        return null;
    }

    public void openListPhoneDialog() {
        final Cursor phones = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = " + mContactId, null, null);
        if (phones == null) {
            return;
        }
        if (phones.moveToFirst()) {
            String[] columns = {ContactsContract.CommonDataKinds.Phone.NUMBER};
            int[] to = {R.id.title};
            SimpleCursorAdapter cursorAdapter = new SimpleCursorAdapter(mContext, R.layout.item_list_dialog, phones, columns, to);
            cursorAdapter.setViewBinder(new MyViewBinder(R.drawable.messages));
            LayoutInflater layoutInflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View convertView = layoutInflater.inflate(R.layout.dialog_list, null);
            ListView listView = (ListView) convertView.findViewById(R.id.component_list);
            TextView dialogTitle = (TextView) convertView.findViewById(R.id.dialog_title);
            dialogTitle.setText(mContext.getString(R.string.Send) + " SMS");
            listView.setAdapter(cursorAdapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Uri uri = ContentUris.withAppendedId(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, id);
                    Cursor result = contentResolver.query(uri, new String[]{ContactsContract.CommonDataKinds.Phone.DATA}, null, null, null);
                    if (result != null) {
                        if (result.moveToFirst()) {
                            String str = result.getString(result.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_SMS_CONTACT, mCheckId, mContactId, str);
                        }
                    }

                    dialog.dismiss();
                }
            });
            dialog.setContentView(convertView);
            dialog.setCancelable(false);
            Button positiveButton = (Button) dialog.findViewById(R.id.positive_button);
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    dialog.dismiss();
                }
            });
            Button buttonAll = (Button) dialog.findViewById(R.id.buttonAll);
            buttonAll.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    phones.moveToFirst();
                    if (!phones.isAfterLast()) {
                        do {
                            String str = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DATA));
                            insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_SMS_CONTACT, mCheckId, mContactId, str);
                        } while (phones.moveToNext());
                    }
                    dialog.dismiss();
                }
            });
            Button buttonNew = (Button) dialog.findViewById(R.id.buttonNew);
            buttonNew.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View arg0) {
                    createPhoneDialog();
                }
            });
            dialog.show();
        } else {
            createPhoneDialog();
        }
    }

    private void createPhoneDialog() {
        AlertDialog.Builder alertDialog = new AlertDialog.Builder(mContext);
        alertDialog.setMessage(mContext.getString(R.string.Enter_phone));
        final EditText input = new EditText(mContext);
        input.setInputType(InputType.TYPE_CLASS_PHONE);
        alertDialog.setView(input);
        alertDialog.setIcon(mContext.getResources().getDrawable(R.drawable.messages));
        alertDialog.setPositiveButton(mContext.getString(R.string.Send), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (!input.getText().toString().isEmpty()) {
                    insertNewTask(TaskCommand.TaskType.TYPE_CHECK_SEND_SMS_CONTACT, mCheckId, mContactId, input.getText().toString());
                    dialog.dismiss();
                }
            }
        });
        alertDialog.setNegativeButton(mContext.getString(R.string.Close), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertDialog.setNeutralButton(mContext.getString(R.string.Save), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (!input.getText().toString().isEmpty()) {
                    if (addPhoneToContact(input.getText().toString()) != null) {
                        openListPhoneDialog();
                    } else {
                        Toast.makeText(mContext, mContext.getString(R.string.TEXT_MESSAGE12), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
        alertDialog.show();
    }

    private Uri addPhoneToContact(String phone) {
        Cursor cursor = contentResolver.query(ContactsContract.RawContacts.CONTENT_URI, null, ContactsContract.RawContacts.CONTACT_ID + " = " + mContactId, null, null);
        if (cursor == null) {
            return null;
        }
        if (cursor.moveToFirst()) {
            int rawContactId = cursor.getInt(cursor.getColumnIndex(BaseColumns._ID));
            ContentValues values = new ContentValues();
            values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
            values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
            values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phone);
            values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
            return contentResolver.insert(ContactsContract.Data.CONTENT_URI, values);
        }
        return null;
    }

    private static class MyViewBinder implements SimpleCursorAdapter.ViewBinder {
        protected final int res;

        public MyViewBinder(int res) {
            this.res = res;
        }

        @Override
        public boolean setViewValue(View view, Cursor cursor, int columnIndex) {
            if (view.getId() == R.id.title) {
                ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(res, 0, 0, 0);
                ((TextView) view).setText(cursor.getString(columnIndex));
            }
            return true;
        }
    }
}
