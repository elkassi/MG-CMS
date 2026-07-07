package com.lear.MGCMS.payload;

public class FirstCheckStats {
    /*
    SELECT
  machine, shift , decision, COUNT(*)
  FROM [dbo].[FirstCheck]
  where [date] = '2024-01-13' and category = 'Matelassage'
  Group by machine, shift , decision
  order by machine, shift , decision
     */
    private String machine;
    private String shift;
    private String decision;
    private Long count;

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public String getShift() {
        return shift;
    }

    public void setShift(String shift) {
        this.shift = shift;
    }

    public String getDecision() {
        return decision;
    }

    public void setDecision(String decision) {
        this.decision = decision;
    }

    public Long getCount() {
        return count;
    }

    public void setCount(Long count) {
        this.count = count;
    }
}
