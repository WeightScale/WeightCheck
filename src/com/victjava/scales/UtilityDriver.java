/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales;

import android.content.Context;
import com.google.android.gms.auth.GoogleAuthException;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.Drive.Files;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.ParentList;
import com.google.api.services.drive.model.ParentReference;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Класс для рабоы с google disk
 *
 * @author Kostya
 */
public class UtilityDriver {

    static final String folderMIME = "application/vnd.google-apps.folder";
    public static final int REQUEST_AUTHORIZATION = 1990;

    /**
     * Удомтоверение
     */
    private final GoogleAccountCredential credential;
    /**
     * Сервис Google drive
     */
    private final Drive driveService;

    /**
     * Конструктор
     *
     * @param context     Контекст
     * @param accountName Имя учетной записи.
     */
    public UtilityDriver(Context context, String accountName) {
        credential = GoogleAccountCredential.usingOAuth2(context, Collections.singleton(DriveScopes.DRIVE_FILE));
        //credential = GoogleAccountCredential.usingOAuth2(context, Arrays.asList(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccountName(accountName);
        driveService = getDriveService(credential);
    }

    /**
     * Получить сервис google drive.
     *
     * @param credential Удостоверение учетной записи.
     * @return Google Drive.
     */
    private Drive getDriveService(GoogleAccountCredential credential) {
        return new Drive.Builder(AndroidHttp.newCompatibleTransport(), new GsonFactory(), credential).build();
    }

    public File getFile(String id) throws IOException {
        return driveService.files().get(id).execute();
    }

    public ParentList getParents(String id) throws IOException {
        return driveService.parents().list(id).execute();
    }

    public List<File> getFileList(String q) throws IOException {

        List<File> result = new ArrayList<>();
        Drive.Files.List listRequest = driveService.files().list();
        if (q != null && !q.isEmpty())
            listRequest.setQ(q);

        do {
            FileList fList = listRequest.execute();
            result.addAll(fList.getItems());
            listRequest.setPageToken(fList.getNextPageToken());

        } while (listRequest.getPageToken() != null && !listRequest.getPageToken().isEmpty());

        return result;

    }

    public void deleteFile(String fileId) throws IOException {

        driveService.files().delete(fileId).execute();

    }

    public InputStream downloadFile(File file) throws IOException {
        HttpResponse resp = driveService.getRequestFactory().buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
        return resp.getContent();
    }

    public File uploadFile(String title, String desc, String parentId, String mime, String fName) throws IOException {

        File body = new File();
        body.setTitle(title);
        body.setDescription(desc);
        body.setMimeType(mime);

        if (parentId != null && !parentId.isEmpty()) {

            ParentReference parent = new ParentReference();
            parent.setId(parentId);
            body.setParents(Collections.singletonList(parent));

        }

        java.io.File content = new java.io.File(fName);
        FileContent fContent = new FileContent(mime, content);

        return driveService.files().insert(body, fContent).execute();
    }

    /**
     * Выгрузить фаил на drive.
     *
     * @param title    Имя файла.
     * @param parentId Индекс родительской папки google drive.
     * @param mime     Миме тип.
     * @param fName    Содержание файла для загрузки.
     * @return Загруженый фаил.
     * @throws IOException Ошибки при загрузке.
     */
    public File uploadFile(String title, /*String desc,*/ String parentId, String mime, java.io.File fName) throws IOException {

        File body = new File();
        body.setTitle(title);
        //body.setDescription(desc);
        body.setMimeType(mime);

        if (parentId != null && !parentId.isEmpty()) {

            ParentReference parent = new ParentReference();
            parent.setId(parentId);
            body.setParents(Collections.singletonList(parent));

        }

        FileContent fContent = new FileContent(mime, fName);

        return driveService.files().insert(body, fContent).execute();
    }

    /**
     * Создание папки.
     *
     * @param parentId   Индек родительской папки.
     * @param folderName Имя папки.
     * @return Фаил папки.
     * @throws IOException Ошибки при создании.
     */
    public File createFolder(String parentId, String folderName) throws IOException {
        File body = new File();
        body.setTitle(folderName);
        body.setMimeType(folderMIME);

        if (parentId != null && !parentId.isEmpty()) {

            ParentReference parent = new ParentReference();
            parent.setId(parentId);
            body.setParents(Collections.singletonList(parent));
        }

        return driveService.files().insert(body).execute();
    }

    public File updateFile(String fileId, String newTitle, String newDesc, String newMime, String newFileName) throws IOException {

        File oldFile = driveService.files().get(fileId).execute();
        oldFile.setTitle(newTitle);
        oldFile.setDescription(newDesc);
        oldFile.setMimeType(newMime);

        java.io.File newFile = new java.io.File(newFileName);
        FileContent newContent = new FileContent(newMime, newFile);

        return driveService.files().update(fileId, oldFile, newContent).execute();

    }

    /**
     * Полусить папку.
     *
     * @param folder   Имя папки.
     * @param parentId Индек родительской папки.
     * @return Папку.
     * @throws IOException Ошибки при получении.
     */
    public File getFolder(String folder, String parentId) throws IOException {
        List<File> result = new ArrayList<>();
        Files.List listRequest = driveService.files().list();
        //listRequest.setMaxResults(10);
        StringBuilder stringBuilder = new StringBuilder("mimeType = 'application/vnd.google-apps.folder'");
        stringBuilder.append(" and title = " + "\'").append(folder).append("\'");
        stringBuilder.append(" and trashed = false");
        if (parentId != null)
            stringBuilder.append(" and " + "\'").append(parentId).append("\'").append(" in parents");
        //listRequest.setQ("mimeType = 'application/vnd.google-apps.folder' and title = " + "\'" + folder + "\'" + " and trashed = false");
        listRequest.setQ(stringBuilder.toString());
        do {
            FileList fList = listRequest.execute();
            result.addAll(fList.getItems());
            listRequest.setPageToken(fList.getNextPageToken());

        } while (listRequest.getPageToken() != null && !listRequest.getPageToken().isEmpty());

        if (result.isEmpty()) {
            return createFolder(parentId, folder);
        }

        return result.get(0);
    }
}
