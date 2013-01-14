package dementiev.jira.plugin.util;

import com.opensymphony.module.propertyset.PropertyException;
import com.opensymphony.module.propertyset.PropertySet;
import com.opensymphony.module.propertyset.PropertySetManager;

import java.util.HashMap;
import java.util.Map;

/**
 * @author dmitry dementiev
 */

public class PropertySetUtil {
    /**
     * This method build property set
     *
     * @param entityName
     * @param entityId
     * @return property map
     */
    private static Map buildPropertySet(String entityName, Long entityId) {
        HashMap ofbizArgs = new HashMap();
        ofbizArgs.put("delegator.name", "default");
        ofbizArgs.put("entityName", entityName);
        ofbizArgs.put("entityId", entityId);
        return ofbizArgs;
    }

    /**
     * @param entityName
     * @param entityId
     * @return property set object
     */
    public static PropertySet getPropertySet(String entityName, Long entityId) {
        PropertySet ofbizPs = PropertySetManager.getInstance("ofbiz", buildPropertySet(entityName, entityId));
        HashMap args = new HashMap();
        args.put("PropertySet", ofbizPs);
        args.put("bulkload", new Boolean(true));
        return PropertySetManager.getInstance("cached", args);
    }

    /**
     * This method removes property set
     *
     * @param entityName
     * @param entityId
     * @param dataName
     */
    public static void removePropertySet(String entityName, Long entityId, String dataName) {
        try {
            PropertySet ofbizPs = PropertySetManager.getInstance("ofbiz", buildPropertySet(entityName, entityId));
            if (ofbizPs != null)
                ofbizPs.remove(dataName);
        }
        catch (PropertyException e) {
        }
    }
}