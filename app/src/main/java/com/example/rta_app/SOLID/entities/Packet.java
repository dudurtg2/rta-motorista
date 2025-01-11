package com.example.rta_app.SOLID.entities;

public class Packet {
    private String codigo;
    private String entregador;
    private String data;
    private String rta;

    public Packet() {
    }

    public Packet(String codigo, String entregador,  String data, String rta) {
        this.codigo = codigo;
        this.entregador = entregador;
        this.data = data;
        this.rta = rta;
    }

    public String getRta() {
        return rta;
    }

    public void setRta(String rta) {
        this.rta = rta;
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

    public String getData() {
        return this.data;
    }

    public void setData(String data) {
        this.data = data;
    }
}
