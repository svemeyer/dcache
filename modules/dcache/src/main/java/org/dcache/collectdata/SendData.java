package org.dcache.collectdata;

import dmg.cells.nucleus.CellCommandListener;
import dmg.util.command.Command;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;

public class SendData implements CellCommandListener {
    private Timer timer;
    private InstanceData instanceData;
    private static final Logger LOGGER = LoggerFactory.getLogger(SendData.class);

    @Required
    public void setInstanceData(InstanceData instanceData) {
        this.instanceData = instanceData;
    }

    private int sendData() throws java.io.IOException {
        instanceData.refreshData();
        String json_body = instanceData.toJson();
        URL url = new URL("https://xxxxxxxx/collector");

        HttpURLConnection conn = (HttpURLConnection) url.openConnection();

        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
        out.write(json_body);
        out.close();

        return conn.getResponseCode();
    }

    @Command(name = "send", hint = "Start sending data", description = "Start collecting instance information and " +
            "sending them to collector")
    public class StartSend implements Callable<String> {
        public String call() {
            try {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        try {
                            sendData();
                        } catch (Exception e) {
                            timer.cancel();
                        }
                    }
                }, 60000);
            } catch (Exception e) {
                return e.toString();
            }
            return "-";
        }
    }
}
