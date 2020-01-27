package org.dcache.collectdata;

import diskCacheV111.poolManager.CostModule;
import diskCacheV111.pools.PoolCostInfo;
import diskCacheV111.vehicles.PoolManagerGetPoolMonitor;
import org.dcache.cells.CellStub;
import org.dcache.poolmanager.PoolMonitor;
import org.dcache.util.Version;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.*;

public class InstanceData {
    private CellStub poolManagerStub;
    private String siteid;

    private Map<String, Double> location = new HashMap<String, Double>();
    private String version;
    private double storage;

    private static final Logger LOGGER = LoggerFactory.getLogger(InstanceData.class);

    @Required
    public void setPoolManagerStub(CellStub poolManagerStub) {
        this.poolManagerStub = poolManagerStub;
    }

    @Required
    public void setSiteId(String siteid) {
        this.siteid = siteid;
    }

    @Required
    public void setLon(double lon) {
        location.put("longitude", lon);
    }

    @Required
    public void setLat(double lat) {
        location.put("latitude", lat);
    }

    public InstanceData () {
        this.version = getVersion();
        this.storage = getStorage();
    }

    @Override
    public String toString() {
        String storageUnit = "-";
        double storageShort = this.storage;
        if(this.storage < 1000) {
            storageUnit = "B";
        }
        else if((this.storage / 1000) < 1000) {
            storageUnit = "kB";
            storageShort = this.storage / 1000;
        }
        else if ((this.storage / 1000000) < 1000) {
            storageShort = this.storage / 1000000;
            storageUnit = "MB";
        } else if ((this.storage / 1000000000) < 1000) {
            storageShort = this.storage / 1000000000;
            storageUnit = "GB";
        } else if ((this.storage / 1000000000000L) < 1000) {
            storageShort = this.storage / 1000000000000L;
            storageUnit = "TB";
        }
        return "Version: " + this.version + "\nLocation: " + this.location + "\nAvailable Storage: " + storageShort +
                " " + storageUnit;
    }

    public String toJson() {
        return "{\n\t\"version\":\t\"" + this.version + "\",\n\t\"siteid\":\t\"" + this.siteid + "\",\n\t\"location\":\n\t{" +
                "\n\t\t\"lon\":" + this.location.get("longitude") + ",\n\t\t\"lat\":" + this.location.get("latitude") +
                "\n\t}," + "\n\t\"storage\":\t" + this.storage + ",\n\t\"@timestamp\":\t\""+
                new SimpleDateFormat("YYYY-MM-dd HH:mm:ss").format(new Timestamp(System.currentTimeMillis())) +
                "\"\n}";
    }

    private String getVersion() {
        Version version = Version.of(this);
        return version.getVersion();
    }

    private double getStorage() {
        PoolMonitor monitor;
        double space = 0.0;

        try {
            monitor = poolManagerStub.sendAndWait(new PoolManagerGetPoolMonitor()).getPoolMonitor();
            CostModule costModule = monitor.getCostModule();
            Collection<PoolCostInfo> costInfos = costModule.getPoolCostInfos();

            for (PoolCostInfo info: costInfos) {
                space += info.getSpaceInfo().getTotalSpace();
            }
            return space;

        } catch (Exception e) {
            LOGGER.error(e.toString());
            return -1.0;
        }
    }

    public String getSiteId() {
        return this.siteid;
    }

    public void refreshData() {
        this.storage = getStorage();
    }
}
