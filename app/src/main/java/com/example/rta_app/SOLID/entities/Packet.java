package com.example.rta_app.SOLID.entities;

public class Packet {
    private String codigo;
    private String entregador;
    private String data;

    public Packet() {
    }

    public Packet(String codigo, String entregador,  String data) {
        this.codigo = codigo;
        this.entregador = entregador;
        this.data = data;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getEntregador() {
        return entregador;
    }

    public void setEntregador(String entregador) {
        this.entregador = entregador;
    }



    public String getData(String data) {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
