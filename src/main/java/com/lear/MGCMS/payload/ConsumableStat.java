package com.lear.MGCMS.payload;

public class ConsumableStat {

    /*
    for this query :
    SELECT machine, YEAR(date) as year, DATEPART(week, date) AS WeekNumber, count(*) as total, AVG(count) as count, AVG(value) as value, AVG(value1) as value1,  AVG(value2) as value2, AVG(value3) as value3, AVG(value4) as value4
  FROM [dbo].[Consumable]
  where type = 'Blade'
  group by machine, YEAR(date), DATEPART(week, date)
  order by machine, YEAR(date), DATEPART(week, date)
     */
    private String machine;
    private Integer year;
    private Integer weekNumber;
    private Integer total;
    private Double count;
    private Double value;
    private Double value1;
    private Double value2;
    private Double value3;
    private Double value4;

    public ConsumableStat() {
    }

    public String getMachine() {
        return machine;
    }

    public void setMachine(String machine) {
        this.machine = machine;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public void setWeekNumber(Integer weekNumber) {
        this.weekNumber = weekNumber;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(Integer total) {
        this.total = total;
    }

    public Double getCount() {
        return count;
    }

    public void setCount(Double count) {
        this.count = count;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public Double getValue1() {
        return value1;
    }

    public void setValue1(Double value1) {
        this.value1 = value1;
    }

    public Double getValue2() {
        return value2;
    }

    public void setValue2(Double value2) {
        this.value2 = value2;
    }

    public Double getValue3() {
        return value3;
    }

    public void setValue3(Double value3) {
        this.value3 = value3;
    }

    public Double getValue4() {
        return value4;
    }

    public void setValue4(Double value4) {
        this.value4 = value4;
    }
}
