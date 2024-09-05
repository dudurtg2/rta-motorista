package com.example.rta_app.SOLID.entities;


public class ListRTADTO {
    private String name;
    private String status;
    private String date;
    private String city;
    private String enteprise;

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }
    public ListRTADTO(String city) {
        this.city = city;
    }
    public ListRTADTO(String name, String status, String date, String city, String enteprise) {
        this.name = name;
        this.status = status;
        this.date = date;
        this.city = city;
        this.enteprise = enteprise;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getEnteprise() {
        return enteprise;
    }

    public void setEnteprise(String enteprise) {
        this.enteprise = enteprise;
    }
}
