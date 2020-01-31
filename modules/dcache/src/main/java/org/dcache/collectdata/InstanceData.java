package org.dcache.collectdata;

import diskCacheV111.poolManager.CostModule;
import diskCacheV111.pools.PoolCostInfo;
import diskCacheV111.vehicles.PoolManagerGetPoolMonitor;
import dmg.cells.nucleus.CellEndpoint;
import org.dcache.cells.CellStub;
import org.dcache.poolmanager.PoolMonitor;
import org.dcache.util.Version;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.util.*;

/**
 * This class contains information about the dCache-instance. These are the storage, version, an ID and a location.
 * Location and ID are read from the .properties-file, storage and version are collected from dCache itself. This
 * class is used by SendData.
 */

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
    public void setSiteid(String siteid) {
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

    public void init() {
        this.version = getVersion();
        this.storage = getStorage();
    }

    /**
     * toJson() transforms the information to a database compliant JSON-format.
     * @return Information as JSON-formatted String
     */

    public String toJson() {
        return "{\n\t\"version\":\t\"" + this.version + "\",\n\t\"siteid\":\t\"" + this.siteid + "\",\n\t\"location\":\n\t{" +
                "\n\t\t\"lon\":" + this.location.get("longitude") + ",\n\t\t\"lat\":" + this.location.get("latitude") +
                "\n\t},\n\t\"storage\":\t" + this.storage + "\n}";
    }

    private String getVersion() {
        Version version = Version.of(this);
        return version.getVersion();
    }

    private double getStorage() {
        PoolMonitor monitor;
        double space = 0.0;
        try {
            monitor = poolManagerStub.sendAndWait(new PoolManagerGetPoolMonitor(), 20000,
                    CellEndpoint.SendFlag.RETRY_ON_NO_ROUTE_TO_CELL).getPoolMonitor();
            CostModule costModule = monitor.getCostModule();
            Collection<PoolCostInfo> costInfos = costModule.getPoolCostInfos();

            for (PoolCostInfo info: costInfos) {
                space += info.getSpaceInfo().getTotalSpace();
            }
            return space;

        } catch (Exception e) {
            LOGGER.error("Could not get storage information; set storage to -1.0. This was caused by: " + e);
            return -1.0;
        }
    }

    public void updateData() {
        this.storage = getStorage();
    }
}
