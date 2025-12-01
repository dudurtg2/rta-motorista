package com.example.rta_app.SOLID.entities;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor

public class VerificadoresDoCarro {


    private String status;
    private Boolean verificadorInicial;
    private Boolean verificadorFinal;
    private LocalDateTime dataInicial;
    private LocalDateTime dataFinal;
    private Boolean finalizado;
    private String combustivelInicial;
    private String combustivelFinal;
    private String parabrisaInicio;
    private String parabrisaFinal;
    private String LatariaInicio;
    private String LatariaFinal;
    private String KilometragemInicio;
    private String KilometragemFinal;
    private String ObservacoesAdicionaisInicio;
    private String ObservacoesAdicionaisFinal;
    private Long carro;
    private Long motorista;

    public VerificadoresDoCarro(String status,
                                Boolean verificadorInicial,
                                Boolean verificadorFinal,
                                LocalDateTime dataInicial,
                                LocalDateTime dataFinal,
                                Boolean finalizado,
                                String combustivelInicial,
                                String combustivelFinal,
                                String parabrisaInicio,
                                String parabrisaFinal,
                                String latariaInicio,
                                String latariaFinal,
                                String kilometragemInicio,
                                String kilometragemFinal,
                                String observacoesAdicionaisInicio,
                                String observacoesAdicionaisFinal,
                                Long carro) {
        this.status = status;
        this.verificadorInicial = verificadorInicial;
        this.verificadorFinal = verificadorFinal;
        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.finalizado = finalizado;
        this.combustivelInicial = combustivelInicial;
        this.combustivelFinal = combustivelFinal;
        this.parabrisaInicio = parabrisaInicio;
        this.parabrisaFinal = parabrisaFinal;
        LatariaInicio = latariaInicio;
        LatariaFinal = latariaFinal;
        KilometragemInicio = kilometragemInicio;
        KilometragemFinal = kilometragemFinal;
        ObservacoesAdicionaisInicio = observacoesAdicionaisInicio;
        ObservacoesAdicionaisFinal = observacoesAdicionaisFinal;
        this.carro = carro;
    }

    public VerificadoresDoCarro() {
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Boolean getVerificadorInicial() {
        return verificadorInicial;
    }

    public void setVerificadorInicial(Boolean verificadorInicial) {
        this.verificadorInicial = verificadorInicial;
    }

    public Boolean getVerificadorFinal() {
        return verificadorFinal;
    }

    public void setVerificadorFinal(Boolean verificadorFinal) {
        this.verificadorFinal = verificadorFinal;
    }

    public LocalDateTime getDataInicial() {
        return dataInicial;
    }

    public void setDataInicial(LocalDateTime dataInicial) {
        this.dataInicial = dataInicial;
    }

    public LocalDateTime getDataFinal() {
        return dataFinal;
    }

    public void setDataFinal(LocalDateTime dataFinal) {
        this.dataFinal = dataFinal;
    }

    public Boolean getFinalizado() {
        return finalizado;
    }

    public void setFinalizado(Boolean finalizado) {
        this.finalizado = finalizado;
    }

    public String getCombustivelInicial() {
        return combustivelInicial;
    }

    public void setCombustivelInicial(String combustivelInicial) {
        this.combustivelInicial = combustivelInicial;
    }

    public String getCombustivelFinal() {
        return combustivelFinal;
    }

    public void setCombustivelFinal(String combustivelFinal) {
        this.combustivelFinal = combustivelFinal;
    }

    public String getParabrisaInicio() {
        return parabrisaInicio;
    }

    public void setParabrisaInicio(String parabrisaInicio) {
        this.parabrisaInicio = parabrisaInicio;
    }

    public String getParabrisaFinal() {
        return parabrisaFinal;
    }

    public void setParabrisaFinal(String parabrisaFinal) {
        this.parabrisaFinal = parabrisaFinal;
    }

    public String getLatariaInicio() {
        return LatariaInicio;
    }

    public void setLatariaInicio(String latariaInicio) {
        LatariaInicio = latariaInicio;
    }

    public String getLatariaFinal() {
        return LatariaFinal;
    }

    public void setLatariaFinal(String latariaFinal) {
        LatariaFinal = latariaFinal;
    }

    public String getKilometragemInicio() {
        return KilometragemInicio;
    }

    public void setKilometragemInicio(String kilometragemInicio) {
        KilometragemInicio = kilometragemInicio;
    }

    public String getKilometragemFinal() {
        return KilometragemFinal;
    }

    public void setKilometragemFinal(String kilometragemFinal) {
        KilometragemFinal = kilometragemFinal;
    }

    public String getObservacoesAdicionaisInicio() {
        return ObservacoesAdicionaisInicio;
    }

    public void setObservacoesAdicionaisInicio(String observacoesAdicionaisInicio) {
        ObservacoesAdicionaisInicio = observacoesAdicionaisInicio;
    }

    public String getObservacoesAdicionaisFinal() {
        return ObservacoesAdicionaisFinal;
    }

    public void setObservacoesAdicionaisFinal(String observacoesAdicionaisFinal) {
        ObservacoesAdicionaisFinal = observacoesAdicionaisFinal;
    }

    public Long getCarro() {
        return carro;
    }

    public void setCarro(Long carro) {
        this.carro = carro;
    }

    public Long getMotorista() {
        return motorista;
    }

    public void setMotorista(Long motorista) {
        this.motorista = motorista;
    }
}
