package com.lear.MGCMS.payload;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;


public class ScheduleMachine {

    /*
    object for this query :
    SELECT TOP (1000) [ID_Schedule_Machine]
      ,[ID_Machine_Schedule]
	  ,h.ligne
      ,[Date_Schedule_Machine]
      ,[Shift_Schedule_Machine]
      ,[Operate_Schedule_Machine]
      ,[Comment_Schedule_Machine]
      ,[CreatedDate_Schedule_Machine]
      ,[CreatedHour_Schedule_Machine]
      ,[UserName_Schedule_Machine]
      ,[HostName_Schedule_Machine]
      ,[Session_W_Schedule_Machine]
  FROM [dbo].[Schedule_Machine] as sm
  Left Join [dbo].hostname as h on h.id = sm.ID_Machine_Schedule
  where Date_Schedule_Machine <= '2024-05-16'
  order by Date_Schedule_Machine desc, Shift_Schedule_Machine desc

     */

    private Integer ID_Schedule_Machine;
    private Integer ID_Machine_Schedule;
    private String ligne;
    private LocalDate Date_Schedule_Machine;
    private String Shift_Schedule_Machine;
    private Boolean Operate_Schedule_Machine;
    private String Comment_Schedule_Machine;
    private LocalDate CreatedDate_Schedule_Machine;
    private LocalTime CreatedHour_Schedule_Machine;
    private String UserName_Schedule_Machine;
    private String HostName_Schedule_Machine;
    private String Session_W_Schedule_Machine;

    public Integer getID_Schedule_Machine() {
        return ID_Schedule_Machine;
    }

    public void setID_Schedule_Machine(Integer ID_Schedule_Machine) {
        this.ID_Schedule_Machine = ID_Schedule_Machine;
    }

    public Integer getID_Machine_Schedule() {
        return ID_Machine_Schedule;
    }

    public void setID_Machine_Schedule(Integer ID_Machine_Schedule) {
        this.ID_Machine_Schedule = ID_Machine_Schedule;
    }

    public String getLigne() {
        return ligne;
    }

    public void setLigne(String ligne) {
        this.ligne = ligne;
    }

    public LocalDate getDate_Schedule_Machine() {
        return Date_Schedule_Machine;
    }

    public void setDate_Schedule_Machine(LocalDate date_Schedule_Machine) {
        Date_Schedule_Machine = date_Schedule_Machine;
    }

    public String getShift_Schedule_Machine() {
        return Shift_Schedule_Machine;
    }

    public void setShift_Schedule_Machine(String shift_Schedule_Machine) {
        Shift_Schedule_Machine = shift_Schedule_Machine;
    }

    public Boolean getOperate_Schedule_Machine() {
        return Operate_Schedule_Machine;
    }

    public void setOperate_Schedule_Machine(Boolean operate_Schedule_Machine) {
        Operate_Schedule_Machine = operate_Schedule_Machine;
    }

    public String getComment_Schedule_Machine() {
        return Comment_Schedule_Machine;
    }

    public void setComment_Schedule_Machine(String comment_Schedule_Machine) {
        Comment_Schedule_Machine = comment_Schedule_Machine;
    }

    public LocalDate getCreatedDate_Schedule_Machine() {
        return CreatedDate_Schedule_Machine;
    }

    public void setCreatedDate_Schedule_Machine(LocalDate createdDate_Schedule_Machine) {
        CreatedDate_Schedule_Machine = createdDate_Schedule_Machine;
    }

    public LocalTime getCreatedHour_Schedule_Machine() {
        return CreatedHour_Schedule_Machine;
    }

    public void setCreatedHour_Schedule_Machine(LocalTime createdHour_Schedule_Machine) {
        CreatedHour_Schedule_Machine = createdHour_Schedule_Machine;
    }

    public String getUserName_Schedule_Machine() {
        return UserName_Schedule_Machine;
    }

    public void setUserName_Schedule_Machine(String userName_Schedule_Machine) {
        UserName_Schedule_Machine = userName_Schedule_Machine;
    }

    public String getHostName_Schedule_Machine() {
        return HostName_Schedule_Machine;
    }

    public void setHostName_Schedule_Machine(String hostName_Schedule_Machine) {
        HostName_Schedule_Machine = hostName_Schedule_Machine;
    }

    public String getSession_W_Schedule_Machine() {
        return Session_W_Schedule_Machine;
    }

    public void setSession_W_Schedule_Machine(String session_W_Schedule_Machine) {
        Session_W_Schedule_Machine = session_W_Schedule_Machine;
    }
}
