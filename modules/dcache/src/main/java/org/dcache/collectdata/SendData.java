package org.dcache.collectdata;

import dmg.cells.nucleus.CellCommandListener;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Required;

public class CollectData implements CellCommandListener {
    private InstanceData instanceData;
    private static final Logger LOGGER = LoggerFactory.getLogger(CollectData.class);

    @Required
    public void setInstanceData(InstanceData instanceData) {
        this.instanceData = instanceData;
    }

}
