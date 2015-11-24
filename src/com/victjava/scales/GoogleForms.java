/*
 * Copyright (c) 2015. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package com.victjava.scales;

import android.content.Context;
import android.text.TextUtils;
import org.apache.http.message.BasicNameValuePair;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Kostya
 */
public class GoogleForms {
    private final Document document;
    private Form form;

    GoogleForms(Context context, int xmlRawResource) throws IOException, SAXException, ParserConfigurationException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = documentBuilder.parse(context.getResources().openRawResource(xmlRawResource));
    }

    GoogleForms(InputStream inputStream) throws ParserConfigurationException, IOException, SAXException {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        document = documentBuilder.parse(inputStream);
    }

    public Form createForm(String name) throws Exception {
        Form form = new Form();
        Node node = document.getElementsByTagName(name).item(0);
        if(node == null)
            throw new Exception("Нет формы с именем " + name + " в файле disk.xml");
        form.setHttp(node.getAttributes().getNamedItem("http").getNodeValue());
        for (int i=0; i < node.getChildNodes().getLength() ; i++){
            Node entrys = node.getChildNodes().item(i);
            if("Entrys".equals(entrys.getNodeName())){
                for (int e=0; e < entrys.getChildNodes().getLength(); e++){
                    Node table = entrys.getChildNodes().item(e);
                    if("Table".equals(table.getNodeName())){
                        form.setTable(table.getAttributes().getNamedItem("name").getNodeValue());
                        for (int t=0; t < table.getChildNodes().getLength(); t++){
                            Node columns = table.getChildNodes().item(t);
                            if("Columns".equals(columns.getNodeName())){
                                NamedNodeMap map = columns.getAttributes();
                                Collection<BasicNameValuePair> collection = new ArrayList<>();
                                for (int m=0; m < map.getLength(); m++){
                                    collection.add(new BasicNameValuePair(map.item(m).getNodeName(), map.item(m).getNodeValue()));
                                }
                                form.setEntrys(collection);
                                return form;
                            }
                        }
                    }
                }
            }
        }
        return form;
    }

    public static class Form{
        private String http = "";
        private String table = "";
        private Collection<BasicNameValuePair> entrys = new ArrayList<>();

        public String getHttp() {
            return http;
        }

        public void setHttp(String http) {
            this.http = http;
        }

        public Collection<BasicNameValuePair> getEntrys() {
            return entrys;
        }

        public void setEntrys(Collection<BasicNameValuePair> entrys) {
            this.entrys = entrys;
        }

        public String getTable() {
            return table;
        }

        public void setTable(String table) {
            this.table = table;
        }

        public String getParams(){
            return TextUtils.join(" ", entrys);
        }

        public String[] getArrayParams(){
            String text = getParams();
            return text.split(" ");
        }

    }
}
