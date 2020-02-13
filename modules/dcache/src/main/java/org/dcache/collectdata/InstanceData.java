package org.dcache.collectdata;

import com.fasterxml.jackson.annotation.JsonInclude;
import diskCacheV111.poolManager.CostModule;
import diskCacheV111.pools.PoolCostInfo;
import diskCacheV111.vehicles.PoolManagerGetPoolMonitor;
import dmg.cells.nucleus.CellEndpoint;
import dmg.cells.nucleus.CellLifeCycleAware;
import dmg.util.command.Option;
import org.checkerframework.checker.formatter.FormatUtil;
import org.dcache.cells.CellStub;
import org.dcache.poolmanager.PoolMonitor;
import org.dcache.util.Version;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

import java.time.Instant;
import java.util.*;

/**
 * This class contains information about the dCache-instance. These are the storage, version, an ID and a location.
 * Location and ID are read from the .properties-file, storage and version are collected from dCache itself. This
 * class is used by SendData.
 */

@JsonInclude(JsonInclude.Include.NON_NULL)
public class InstanceData implements CellLifeCycleAware {
    private CellStub poolManagerStub;

    private String siteName;
    private OptionalDouble longitude;
    private OptionalDouble latitude;
    final private String version = loadVersion();
    private OptionalLong storage;

    private static final Logger _log = LoggerFactory.getLogger(InstanceData.class);

    @Required
    public void setPoolManagerStub(CellStub poolManagerStub) {
        this.poolManagerStub = poolManagerStub;
    }

    @Required
    public void setSiteName(String siteName) {
        if(!siteName.equals("")) {
            this.siteName = siteName;
        } else {
            throw new IllegalArgumentException("Please provide a siteName.");
        }
    }

    @Required
    public void setLongitude(String longitude) {
        if (!longitude.equals("")) {
            try {
                //Long +- 180
                this.longitude = OptionalDouble.of(Double.parseDouble(longitude));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("The given value for longitude is in wrong format. Double expected.");
            }
            if (this.longitude.getAsDouble() < -180 || this.longitude.getAsDouble() > 180) {
                throw new IllegalArgumentException("Longitude must be between -180 and 180. "
                        + this.longitude.getAsDouble() + " does not match this range.");
            }
        } else {
            this.longitude = OptionalDouble.empty();
        }
    }

    @Required
    public void setLatitude(String latitude) {

        if(!latitude.equals("")) {
            try {
                //Lat +- 90
                this.latitude = OptionalDouble.of(Double.parseDouble(latitude));
            } catch (NumberFormatException nfe) {
                throw new IllegalArgumentException("The given value for latitude is in wrong format. Double expected.");
            }
            if (this.latitude.getAsDouble() < -90 || this.latitude.getAsDouble() > 90) {
                throw new IllegalArgumentException("Latitude must be between -90 and 90. "
                        + this.latitude.getAsDouble() + " does not match this range.");
            }
        } else {
            this.latitude = OptionalDouble.empty();
        }
    }

    @Override
    public void afterStart() {
//        this.version = getVersion();
        this.storage = loadStorage();
    }

    /**
     * toJson() transforms the information to a database compliant JSON-format.
     * @return Information as JSON-formatted String
     */

//    public String toJson() {
//        JSONObject o = new JSONObject();
//        o.put("version", this.version);
//        o.put("timestamp", Instant.now().toEpochMilli());
//        o.put("siteid", this.siteid);
//        if (this.storage.isPresent()) {
//            o.put("storage", this.storage.getAsLong());
//        }
//
//        if (longitude.isPresent() && latitude.isPresent()) {
//            JSONObject loc = new JSONObject();
//            loc.put("lon", longitude.getAsDouble());
//            loc.put("lat", latitude.getAsDouble());
//            o.put("location", loc);
//        }
//
//        return o.toString();
//    }

    private String loadVersion() {
        Version version = Version.of(this);
        return version.getVersion();
    }

    private OptionalLong loadStorage() {
        PoolMonitor monitor;
        OptionalLong space = OptionalLong.empty();
        try {
            monitor = poolManagerStub.sendAndWait(new PoolManagerGetPoolMonitor(), 20000,
                    CellEndpoint.SendFlag.RETRY_ON_NO_ROUTE_TO_CELL).getPoolMonitor();
            CostModule costModule = monitor.getCostModule();
            Collection<PoolCostInfo> costInfos = costModule.getPoolCostInfos();

            space = OptionalLong.of(costModule.getPoolCostInfos().stream()
                    .map(PoolCostInfo::getSpaceInfo)
                    .mapToLong(PoolCostInfo.PoolSpaceInfo::getTotalSpace)
                    .sum());

        } catch (Exception e) {
            _log.error("Could not get storage information; set storage to -1.0. This was caused by: " + e);
        }

        return space;
    }

    public void updateData() {
        this.storage = loadStorage();
    }

    public Long getTimestamp () {
        return Instant.now().toEpochMilli();
    }

    public String getSiteName() {
        return this.siteName;
    }

    public HashMap<String, Double> getLocation() {
        HashMap<String, Double> location = new HashMap<>();
        if (this.latitude.isPresent()) {
            location.put("lat", this.latitude.getAsDouble());
        } else {
            return null;
        }

        if (this.longitude.isPresent()) {
            location.put("lon", this.longitude.getAsDouble());
        } else {
            return null;
        }
        return location;
    }

    public long getStorage() {
        return this.storage.getAsLong();
    }

    public String getVersion() {
        return this.version;
    }
}
