package org.dcache.collectdata;

import dmg.cells.nucleus.CellCommandListener;
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

public class SendData implements CellCommandListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(SendData.class);
    private ScheduledExecutorService sendDataExecutor;
    private InstanceData instanceData;

    @Required
    public void setInstanceData(InstanceData instanceData) {
        this.instanceData = instanceData;
    }

    public SendData() {
        sendDataExecutor = Executors.newScheduledThreadPool(1);
    }

    public void init() {
        LOGGER.warn("Sending information about dCache-instance will be activated");
        ScheduledFuture sendDataExecFuture = sendDataExecutor.scheduleAtFixedRate(this::sendData,
                0, 1, TimeUnit.HOURS);
    }

    public void shutdown() {
        sendDataExecutor.shutdown();
    }

    private int sendData() {
        instanceData.refreshData();

        String json_body = instanceData.toJson();
        SimpleDateFormat format = new SimpleDateFormat("MM-dd-yyyy");
        URL url;
        HttpsURLConnection conn;
        int response_code = 0;
        try {
            url = new URL("https://xxxxxxxx/collector?date=" + format.format(new Date()));
        } catch (MalformedURLException mue) {
            LOGGER.error("URL is in wrong format.");
            return 0;
        }

        try {
            conn = (HttpsURLConnection) url.openConnection();
        } catch (java.io.IOException ioe) {
            LOGGER.error("Connection failed");
            return 0;
        }

        try {
            conn.setRequestMethod("POST");
        } catch (ProtocolException pe) {
            LOGGER.error("Error with Protocol");
            return 0;
        }
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        try {
            OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
            out.write(json_body);
            out.close();
            response_code = conn.getResponseCode();
            if (response_code != 200 && response_code != 201) {
                LOGGER.error("Error sending the data. Response: " + response_code + " " + conn.getResponseMessage());
            } else {
                LOGGER.warn("Information sent to collector.");
            }
        } catch (java.io.IOException ioe) {
            LOGGER.error("Error while sending to collector: " + ioe);
            return 0;
        }

        try {
            response_code = conn.getResponseCode();
        } catch (java.io.IOException ioe) {
            LOGGER.error("Error getting response code: " + ioe);
            return 0;
        }
        return response_code;
    }
}
