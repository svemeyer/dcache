package org.dcache.collectdata;

import dmg.cells.nucleus.CellCommandListener;
import org.dcache.util.CDCScheduledExecutorServiceDecorator;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.*;

/**
 * This is a cell, that collects information about the dCache-instance and sends it once per hour to a central database.
 * It captures the version and the storage from dCache directly. The location and a siteid are read from
 * collectdata.properties.
 * The location consists of latitude and longitude. It's possible to set the values to 0 to omit the real location.
 * Sending the information regularly is implemented with a ScheduledExecutor, which executes sendData()
 * at an interval of one hour.
 */

public class SendData implements CellCommandListener {
    private static final Logger _log = LoggerFactory.getLogger(SendData.class);
    private ScheduledExecutorService sendDataExecutor;
    private InstanceData instanceData;
    private String urlStr;

    //self-signed-certificate workaround TODO remove
    static {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> hostname.equals("xxxxx"));
    }

    @Required
    public void setUrlStr(String url) {
        this.urlStr = url;
    }

    @Required
    public void setInstanceData(InstanceData instanceData) {
        this.instanceData = instanceData;
    }


    public SendData() {
        sendDataExecutor = new CDCScheduledExecutorServiceDecorator( Executors.newScheduledThreadPool(1));
    }

    public void init() {
        _log.warn("Sending information about dCache-instance is activated.");
        sendDataExecutor.scheduleAtFixedRate(this::sendData,
                0, 1, TimeUnit.HOURS);
    }

    public void shutdown() {
        sendDataExecutor.shutdown();
    }

    /**
     * sendData() sends the data the information are updated and converted to a JSON-formatted string first.
     */

    private void sendData() {
        instanceData.updateData();

        String json_body = instanceData.toJson();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        URL url;
        HttpsURLConnection conn;
        int response_code = 0;

        try {
            url = new URL(urlStr + "?date=" + format.format(new Date()));
        } catch (MalformedURLException mue) {
            _log.error("URL is in wrong format: " + mue);
            return;
        }

        try {
            conn = (HttpsURLConnection) url.openConnection();
        } catch (java.io.IOException ioe) {
            _log.error("Connection to collector failed: " + ioe);
            return;
        }

        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException pe) {
            _log.error("Error with Protocol while setting request method: " + pe);
            return;
        }
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try {
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(json_body);
            out.close();
            response_code = conn.getResponseCode();
            if (response_code != 200 && response_code != 201) {
                _log.error("Error sending the data. Response: " + response_code + " " + conn.getResponseMessage());
            } else {
                _log.info("Information successfully sent to collector");
            }
        } catch (java.io.IOException ioe) {
            _log.error("Error while sending to collector: " + ioe);
            return;
        }

        try {
            response_code = conn.getResponseCode();
        } catch (java.io.IOException ioe) {
            _log.error("Error getting response code: " + ioe);
        }
    }
}
