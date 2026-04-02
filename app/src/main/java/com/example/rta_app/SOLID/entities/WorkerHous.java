package com.example.rta_app.SOLID.entities;

public class WorkerHous {
    private String date;
    private String hour_first;
    private String hour_dinner;
    private String hour_finish;
    private String hour_stop;
    private String hour_after;

    private String latitude_first;
    private String longitude_first;
    private String latitude_dinner;
    private String longitude_dinner;
    private String latitude_stop;
    private String longitude_stop;
    private String latitude_finish;
    private String longitude_finish;


    private Boolean carroInicial;

    private Boolean carroFinal;

    private Long idVerificardor;
    private Long idCarro;

    public WorkerHous(String date,
                      String hour_first,
                      String hour_dinner,
                      String hour_finish,
                      String hour_stop,
                      String hour_after,
                      String latitude_first,
                      String longitude_first,
                      String latitude_dinner,
                      String longitude_dinner,
                      String latitude_stop,
                      String longitude_stop,
                      String latitude_finish,
                      String longitude_finish
                      ) {
        this.date = date;
        this.hour_first = hour_first;
        this.hour_dinner = hour_dinner;
        this.hour_finish = hour_finish;
        this.hour_stop = hour_stop;
        this.hour_after = hour_after;
        this.latitude_first = latitude_first;
        this.longitude_first = longitude_first;
        this.latitude_dinner = latitude_dinner;
        this.longitude_dinner = longitude_dinner;
        this.latitude_stop = latitude_stop;
        this.longitude_stop = longitude_stop;
        this.latitude_finish = latitude_finish;
        this.longitude_finish = longitude_finish;


    }
    public WorkerHous(String date, String hour_first, String hour_dinner, String hour_finish, String hour_stop, String hour_after, String latitude_first, String longitude_first, String latitude_dinner, String longitude_dinner, String latitude_stop, String longitude_stop, String latitude_finish, String longitude_finish, Boolean carroInicial, Boolean carroFinal, Long idVerificardor, Long idCarro) {
        this.date = date;
        this.hour_first = hour_first;
        this.hour_dinner = hour_dinner;
        this.hour_finish = hour_finish;
        this.hour_stop = hour_stop;
        this.hour_after = hour_after;
        this.latitude_first = latitude_first;
        this.longitude_first = longitude_first;
        this.latitude_dinner = latitude_dinner;
        this.longitude_dinner = longitude_dinner;
        this.latitude_stop = latitude_stop;
        this.longitude_stop = longitude_stop;
        this.latitude_finish = latitude_finish;
        this.longitude_finish = longitude_finish;
        this.carroInicial = carroInicial;
        this.carroFinal = carroFinal;
        this.idVerificardor = idVerificardor;
        this.idCarro = idCarro;


    }

    public String getLatitude_first() {
        return latitude_first;
    }

    public void setLatitude_first(String latitude_first) {
        this.latitude_first = latitude_first;
    }

    public String getLongitude_first() {
        return longitude_first;
    }

    public void setLongitude_first(String longitude_first) {
        this.longitude_first = longitude_first;
    }

    public String getLatitude_dinner() {
        return latitude_dinner;
    }

    public void setLatitude_dinner(String latitude_dinner) {
        this.latitude_dinner = latitude_dinner;
    }

    public String getLongitude_dinner() {
        return longitude_dinner;
    }

    public void setLongitude_dinner(String longitude_dinner) {
        this.longitude_dinner = longitude_dinner;
    }

    public String getLatitude_stop() {
        return latitude_stop;
    }

    public void setLatitude_stop(String latitude_stop) {
        this.latitude_stop = latitude_stop;
    }

    public String getLongitude_stop() {
        return longitude_stop;
    }

    public void setLongitude_stop(String longitude_stop) {
        this.longitude_stop = longitude_stop;
    }

    public String getLatitude_finish() {
        return latitude_finish;
    }

    public void setLatitude_finish(String latitude_finish) {
        this.latitude_finish = latitude_finish;
    }

    public String getLongitude_finish() {
        return longitude_finish;
    }

    public void setLongitude_finish(String longitude_finish) {
        this.longitude_finish = longitude_finish;
    }

    public Long getIdCarro() {
        return idCarro;
    }

    public void setIdCarro(Long idCarro) {
        this.idCarro = idCarro;
    }

    public Long getIdVerificardor() {
        return idVerificardor;
    }

    public void setIdVerificardor(Long idVerificardor) {
        this.idVerificardor = idVerificardor;
    }

    public Boolean getCarroInicial() {
        if (carroInicial == null) {
            return false;
        }

        if (carroInicial) {
            return true;
        }

        return false;
    }

    public void setCarroInicial(Boolean carroInicial) {
        this.carroInicial = carroInicial;
    }

    public Boolean getCarroFinal() {
        if (carroFinal == null) {
            return false;
        }
        if (carroFinal) {
            return true;
        }

        return false;
    }

    public void setCarroFinal(Boolean carroFinal) {

        this.carroFinal = carroFinal;
    }

    public String getHour_after() {
        return hour_after;
    }

    public void setHour_after(String hour_after) {
        this.hour_after = hour_after;
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

    @Override
    public String toString() {
        return "WorkerHous{" +
                "date='" + date + '\'' +
                ", hour_first='" + hour_first + '\'' +
                ", hour_dinner='" + hour_dinner + '\'' +
                ", hour_finish='" + hour_finish + '\'' +
                ", hour_stop='" + hour_stop + '\'' +
                ", hour_after='" + hour_after + '\'' +
                ", latitude_first='" + latitude_first + '\'' +
                ", longitude_first='" + longitude_first + '\'' +
                ", latitude_dinner='" + latitude_dinner + '\'' +
                ", longitude_dinner='" + longitude_dinner + '\'' +
                ", latitude_stop='" + latitude_stop + '\'' +
                ", longitude_stop='" + longitude_stop + '\'' +
                ", latitude_finish='" + latitude_finish + '\'' +
                ", longitude_finish='" + longitude_finish + '\'' +
                ", carroInicial=" + carroInicial +
                ", carroFinal=" + carroFinal +

                ", idVerificardor=" + idVerificardor +
                ", idCarro=" + idCarro +
                '}';
    }
}
