package com.example.rta_app.SOLID.entities;

public class Users {

    private String name;
    private String telefone;
    private String base;
    private String uid;

    private boolean frete;
    private int baseid;

    public Users(String name, String uid, String telefone, String base, int baseid, boolean frete) {
        this.name = name;
        this.uid = uid;
        this.telefone = telefone;
        this.base = base;
        this.baseid = baseid;
        this.frete = frete;




    }

    public boolean isFrete() {
        return frete;
    }

    public void setFrete(boolean frete) {
        this.frete = frete;
    }

    public int getBaseid() {
        return baseid;
    }

    public void setBaseid(int baseid) {
        this.baseid = baseid;
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
