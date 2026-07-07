package com.lear.MGCMS.payload;

import com.lear.MGCMS.domain.CuttingPlan.CuttingPlan;

import java.util.List;

public class AlertCuttingPlan {

    //alertMessages, arrTable, responsableList
    private List<String> alertMessages;
    private String content;
    private List<String> responsableList;

    private CuttingPlan cuttingPlan;

    public CuttingPlan getCuttingPlan() {
        return cuttingPlan;
    }

    public void setCuttingPlan(CuttingPlan cuttingPlan) {
        this.cuttingPlan = cuttingPlan;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public List<String> getResponsableList() {
        return responsableList;
    }

    public void setResponsableList(List<String> responsableList) {
        this.responsableList = responsableList;
    }

    public List<String> getAlertMessages() {

        return alertMessages;
    }

    public void setAlertMessages(List<String> alertMessages) {
        this.alertMessages = alertMessages;
    }
}
