package dementiev.jira.plugin.customfield;

import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.customfields.SortableCustomField;
import com.atlassian.jira.issue.customfields.converters.StringConverterImpl;
import com.atlassian.jira.issue.customfields.impl.CalculatedCFType;
import com.atlassian.jira.issue.customfields.impl.FieldValidationException;
import com.atlassian.jira.issue.fields.CustomField;
import com.atlassian.jira.issue.fields.config.FieldConfig;
import com.opensymphony.module.propertyset.PropertySet;
import org.apache.log4j.Logger;
import dementiev.jira.plugin.util.PropertySetUtil;

/**
 * @author dmitry dementiev
 */

public class DuedateUpdatingCounterCF extends CalculatedCFType implements SortableCustomField {
    Logger log = Logger.getLogger(DuedateUpdatingCounterCF.class);

    public int compare(final String customFieldObjectValue1, final String customFieldObjectValue2, final FieldConfig fieldConfig) {
        return customFieldObjectValue1.compareTo(customFieldObjectValue2);
    }

    public String getStringFromSingularObject(Object singularObject) {
        if (singularObject != null) {
            return singularObject.toString();
        }
        return null;
    }

    public Object getSingularObjectFromString(String value) throws FieldValidationException {
        assertObjectImplementsType(String.class, value);
        return StringConverterImpl.convertNullToEmpty((String) value);
    }

    public Object getValueFromIssue(CustomField field, Issue issue) {
        int result = 0;
        PropertySet propertySet = null;
        propertySet = PropertySetUtil.getPropertySet("Issue", issue.getId());
        if (propertySet != null) {
            result = propertySet.getInt("dueDateCounter");
        }
        return result;
    }
}