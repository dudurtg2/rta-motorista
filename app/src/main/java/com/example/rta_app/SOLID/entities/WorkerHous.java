package com.example.rta_app.SOLID.entities;

public class WorkerHous {
    private String date;
    private String hour_first;
    private String hour_dinner;
    private String hour_finish;
    private String hour_stop;

    public WorkerHous(String date, String hour_first, String hour_dinner, String hour_finish, String hour_stop) {
        this.date = date;
        this.hour_first = hour_first;
        this.hour_dinner = hour_dinner;
        this.hour_finish = hour_finish;
        this.hour_stop = hour_stop;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getHour_first() {
        return hour_first;
    }

    public void setHour_first(String hour_first) {
        this.hour_first = hour_first;
    }

    public String getHour_dinner() {
        return hour_dinner;
    }

    public void setHour_dinner(String hour_dinner) {
        this.hour_dinner = hour_dinner;
    }

    public String getHour_finish() {
        return hour_finish;
    }

    public void setHour_finish(String hour_finish) {
        this.hour_finish = hour_finish;
    }

    public String getHour_stop() {
        return hour_stop;
    }

    public void setHour_stop(String hour_stop) {
        this.hour_stop = hour_stop;
    }
}
