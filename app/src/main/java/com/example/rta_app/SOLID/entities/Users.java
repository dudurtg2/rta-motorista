package com.example.rta_app.SOLID.entities;

public class Users {

    private String name;
    private String telefone;
    private String base;
    private String uid;

    public Users(String name, String uid, String telefone, String base) {
        this.name = name;
        this.uid = uid;
        this.telefone = telefone;
        this.base = base;


    }

    public String getTelefone() {
        return telefone;
    }

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }
}
