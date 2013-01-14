package dementiev.jira.plugin.action;


import com.atlassian.jira.bc.issue.IssueService;
import com.atlassian.jira.bc.issue.comment.CommentService;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.SubTaskManager;
import com.atlassian.jira.exception.IssueNotFoundException;
import com.atlassian.jira.exception.IssuePermissionException;
import com.atlassian.jira.issue.IssueInputParameters;
import com.atlassian.jira.issue.IssueInputParametersImpl;
import com.atlassian.jira.issue.customfields.OperationContext;
import com.atlassian.jira.issue.fields.layout.field.FieldLayoutManager;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderLayoutItem;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderTab;
import com.atlassian.jira.issue.fields.screen.FieldScreenRenderer;
import com.atlassian.jira.issue.fields.screen.FieldScreenRendererFactory;
import com.atlassian.jira.issue.link.IssueLinkManager;
import com.atlassian.jira.issue.operation.IssueOperation;
import com.atlassian.jira.issue.operation.IssueOperations;
import com.atlassian.jira.security.xsrf.RequiresXsrfCheck;
import com.atlassian.jira.web.action.issue.AbstractCommentableAssignableIssue;
import com.atlassian.jira.workflow.WorkflowManager;
import com.opensymphony.module.propertyset.PropertySet;
import dementiev.jira.plugin.util.PropertySetUtil;
import webwork.action.ActionContext;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author dmitry dementiev
 *         When user change duedate - counter of duedates is incremented
 */

public class MyEditIssue extends AbstractCommentableAssignableIssue implements OperationContext {
    private final ConstantsManager constantsManager;
    private final FieldLayoutManager fieldLayoutManager;
    private final WorkflowManager workflowManager;

    private FieldScreenRenderer fieldScreenRenderer;
    private FieldScreenRendererFactory fieldScreenRendererFactory;
    private final IssueService issueService;
    private SortedSet tabsWithErrors;
    private int selectedTab;
    private IssueService.UpdateValidationResult updateValidationResult;

    private Collection ignoreFieldIds = new LinkedList();

    private Timestamp oldDate;

    public MyEditIssue(IssueLinkManager issueLinkManager, SubTaskManager subTaskManager,
                       ConstantsManager constantsManager, FieldLayoutManager fieldLayoutManager, WorkflowManager workflowManager,
                       FieldScreenRendererFactory fieldScreenRendererFactory, CommentService commentService, final IssueService issueService) {
        super(issueLinkManager, subTaskManager, fieldScreenRendererFactory, commentService);
        this.constantsManager = constantsManager;
        this.fieldLayoutManager = fieldLayoutManager;
        this.workflowManager = workflowManager;
        this.fieldScreenRendererFactory = fieldScreenRendererFactory;
        this.issueService = issueService;
    }

    public String doDefault() throws Exception {
        try {
            if (!isEditable()) {
                return ERROR;
            }
        }
        catch (IssuePermissionException ipe) {
            //do not show error messages since the view will take care of it
            getErrorMessages().clear();
            return ERROR;
        }
        catch (IssueNotFoundException e) {
            // Error is added above
            return ERROR;
        }

        for (Iterator iterator = getFieldScreenRenderer().getFieldScreenRenderTabs().iterator(); iterator.hasNext();) {
            FieldScreenRenderTab fieldScreenRenderTab = (FieldScreenRenderTab) iterator.next();
            for (Iterator iterator1 = fieldScreenRenderTab.getFieldScreenRenderLayoutItems().iterator(); iterator1.hasNext();) {
                FieldScreenRenderLayoutItem fieldScreenRenderLayoutItem = (FieldScreenRenderLayoutItem) iterator1.next();
                if (fieldScreenRenderLayoutItem.isShow(getIssueObject())) {
                    fieldScreenRenderLayoutItem.populateFromIssue(getFieldValuesHolder(), getIssueObject());
                }
            }
        }

        return super.doDefault();
    }

    protected FieldScreenRenderer getFieldScreenRenderer() {
        if (fieldScreenRenderer == null) {
            fieldScreenRenderer = fieldScreenRendererFactory.getFieldScreenRenderer(getRemoteUser(), getIssueObject(), IssueOperations.EDIT_ISSUE_OPERATION, false);
        }

        return fieldScreenRenderer;
    }

    protected void doValidation() {
        setOldDate(getIssueObject().getDueDate());
        final IssueInputParameters issueInputParameters = new IssueInputParametersImpl(ActionContext.getParameters());
        updateValidationResult = issueService.validateUpdate(getRemoteUser(), getIssueObject().getId(), issueInputParameters);
        issueObject = updateValidationResult.getIssue();
        setFieldValuesHolder(updateValidationResult.getFieldValuesHolder());
        if (!updateValidationResult.isValid()) {
            addErrorCollection(updateValidationResult.getErrorCollection());
        }
    }

    @RequiresXsrfCheck
    protected String doExecute() throws Exception {
        try {
            final IssueService.IssueResult issueResult = issueService.update(getRemoteUser(), updateValidationResult);
            if (!issueResult.isValid()) {
                addErrorCollection(issueResult.getErrorCollection());
            } else {
                //counter of dueDate editing
                if (!issueResult.getIssue().getDueDate().toString().equalsIgnoreCase(getOldDate().toString())) {
                    PropertySet propertySet = PropertySetUtil.getPropertySet("Issue", getIssueObject().getId());
                    int oldValue = propertySet.getInt("dueDateCounter");
                    oldValue++;
                    propertySet.setInt("dueDateCounter", oldValue);
                }
            }

            if (inline) {
                return returnComplete();
            }

            return getRedirect(getViewUrl());
        }
        catch (Throwable e) {
            addErrorMessage(getText("admin.errors.issues.exception.occured", e));
            log.error("Exception occurred editing issue: " + e, e);
            return ERROR;
        }
    }

    public List getFieldScreenRenderTabs() {
        return getFieldScreenRenderer().getFieldScreenRenderTabs();
    }

    public Collection getTabsWithErrors() {
        if (tabsWithErrors == null) {
            initTabsWithErrors();
        }

        return tabsWithErrors;
    }

    private void initTabsWithErrors() {
        tabsWithErrors = new TreeSet();
        if (getErrors() != null && !getErrors().isEmpty()) {
            // Record the tabs which have fields with errors on them
            for (Iterator iterator = getErrors().keySet().iterator(); iterator.hasNext();) {
                String fieldId = (String) iterator.next();
                tabsWithErrors.add(getFieldScreenRenderer().getFieldScreenRenderTabPosition(fieldId));
            }

            // Add 1 as the status' counts in WW iterators start at 1 (not 0)
            selectedTab = ((FieldScreenRenderTab) tabsWithErrors.first()).getPosition() + 1;
        } else {
            selectedTab = 1;
        }
    }

    public int getSelectedTab() {
        // Init tabs - as the first tab with error will be calculated then.
        if (tabsWithErrors == null) {
            initTabsWithErrors();
        }

        return selectedTab;
    }

    public IssueOperation getIssueOperation() {
        return IssueOperations.EDIT_ISSUE_OPERATION;
    }

    public ConstantsManager getConstantsManager() {
        return constantsManager;
    }

    public FieldLayoutManager getFieldLayoutManager() {
        return fieldLayoutManager;
    }

    public WorkflowManager getWorkflowManager() {
        return workflowManager;
    }

    public Collection getIgnoreFieldIds() {
        return ignoreFieldIds;
    }

    public Timestamp getOldDate() {
        return oldDate;
    }

    public void setOldDate(Timestamp oldDate) {
        this.oldDate = oldDate;
    }
}

