package org.dcache.collectdata;

import com.fasterxml.jackson.databind.ObjectMapper;
import dmg.cells.nucleus.CellCommandListener;
import dmg.cells.nucleus.CellLifeCycleAware;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.dcache.util.CDCScheduledExecutorServiceDecorator;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import javax.net.ssl.HttpsURLConnection;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
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

public class SendData implements CellCommandListener, CellLifeCycleAware {
    private static final Logger _log = LoggerFactory.getLogger(SendData.class);

    private ScheduledExecutorService sendDataExecutor;
    private InstanceData instanceData;
    private String urlStr;
    
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

    @Override
    public void beforeStop() {
        sendDataExecutor.shutdown();
    }

    @Override
    public void afterStart() {
        _log.warn("Sending information about dCache-instance to {} is activated.", urlStr);
        sendDataExecutor.scheduleAtFixedRate(this::sendData,
                0, 1, TimeUnit.HOURS);
    }

    /**
     * sendData() sends the data the information are updated and converted to a JSON-formatted string first.
     */

    private void sendData() {
        instanceData.updateData();

        ObjectMapper jackson = new ObjectMapper();
//        try {
//            _log.error("Jackson vs JSON Object:\n{}\n{}", jackson.writeValueAsString(instanceData), instanceData.toJson());
//        } catch (Exception e) {
//            _log.error("Jackson error: ", e);
//        }
//        _log.error("JSON Object:\n{}", instanceData.toJson());

//        String json_body = instanceData.toJson();
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");

        try {

            CloseableHttpClient httpClient = HttpClients.createDefault();

            URI uri = new URIBuilder(this.urlStr)
                    .setParameter("date", format.format(new Date()))
                    .build();

            HttpPost httpPost = new HttpPost(uri);

            httpPost.setEntity(new StringEntity(jackson.writeValueAsString(instanceData)));
            httpPost.setHeader("Content-Type", "application/json");
            CloseableHttpResponse response = httpClient.execute(httpPost);

            response.close();

            if (response.getStatusLine().getStatusCode() != 201 && response.getStatusLine().getStatusCode() != 200) {
                _log.error("Error sending data to {}. Response: {}", uri.toString(), response.toString());
            } else {
                _log.info("Information successfully sent to {}", uri.toString());
            }
        } catch (Exception e) {
            _log.error("Sending information to collector failed. Reason: {}", e.toString());
        }
    }
}
