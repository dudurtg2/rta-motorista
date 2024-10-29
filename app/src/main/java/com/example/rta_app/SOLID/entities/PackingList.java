package com.example.rta_app.SOLID.entities;

import java.util.List;

public class PackingList {
    private String funcionario;
    private String entregador;
    private String telefone;
    private String local;
    private String codigodeficha;
    private String horaedia;
    private String quantidade;
    private String status;
    private String motorista;
    private List<String> codigosinseridos;
    private String downloadlink;
    private String empresa;


    public PackingList() {
    }

    public PackingList(String empresa, String funcionario, String entregador, String telefone, String local, String codigodeficha, String horaedia, String quantidade, String status, String motorista, List<String> codigosinseridos, String downloadlink) {
        this.funcionario = funcionario;
        this.entregador = entregador;
        this.telefone = telefone;
        this.local = local;
        this.codigodeficha = codigodeficha;
        this.horaedia = horaedia;
        this.quantidade = quantidade;
        this.status = status;
        this.motorista = motorista;
        this.codigosinseridos = codigosinseridos;
        this.downloadlink = downloadlink;
        this.empresa = empresa;
    }

    public String getEmpresa() {
        return empresa;
    }

    public void setEmpresa(String empresa) {
        this.empresa = empresa;
    }

    public String getFuncionario() {
        return this.funcionario;
    }

    public void setFuncionario(String funcionario) {
        this.funcionario = funcionario;
    }

    public String getEntregador() {
        return this.entregador;
    }

    public void setEntregador(String entregador) {
        this.entregador = entregador;
    }

    public String getTelefone() {
        return this.telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getLocal() {
        return this.local;
    }

    public void setLocal(String local) {
        this.local = local;
    }

    public String getCodigodeficha() {
        return this.codigodeficha;
    }

    public void setCodigodeficha(String codigodeficha) {
        this.codigodeficha = codigodeficha;
    }

    public String getHoraedia() {
        return this.horaedia;
    }

    public void setHoraedia(String horaedia) {
        this.horaedia = horaedia;
    }

    public String getQuantidade() {
        return this.quantidade;
    }

    public void setQuantidade(String quantidade) {
        this.quantidade = quantidade;
    }

    public String getStatus() {
        return this.status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMotorista() {
        return this.motorista;
    }

    public void setMotorista(String motorista) {
        this.motorista = motorista;
    }

    public List<String> getCodigosinseridos() {
        return this.codigosinseridos;
    }

    public void setCodigosinseridos(List<String> codigosinseridos) {
        this.codigosinseridos = codigosinseridos;
    }

    public String getDownloadlink() {
        return this.downloadlink;
    }

    public void setDownloadlink(String downloadlink) {
        this.downloadlink = downloadlink;
    }


    
}
