package com.example.rta_app.SOLID.entities;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Builder
@Data
public class VerificadoresDoCarro {


    private String status;
    private Boolean verificadorInicial;

    private Boolean verificadorFinal;

    private LocalDateTime dataInicial;

    private LocalDateTime dataFinal;

    private Boolean finalizado;

    private String frenteInicial;

    private String frenteFinal;

    private String atrasInicio;

    private String atrasFinal;

    private String latariaEsquerdaInicio;

    private String latariaEsquerdaFinal;

    private String latariaDireitaInicio;

    private String latariaDireitaFinal;

    private String painelInicio;

    private String painelFinal;

    private String observacoesAdicionaisInicio;

    private String observacoesAdicionaisFinal;

    private Long carro;
    private Long motorista;

    public VerificadoresDoCarro(String status, Boolean verificadorInicial, Boolean verificadorFinal, LocalDateTime dataInicial, LocalDateTime dataFinal, Boolean finalizado, String frenteInicial, String frenteFinal, String atrasInicio, String atrasFinal, String latariaEsquerdaInicio, String latariaEsquerdaFinal, String latariaDireitaInicio, String latariaDireitaFinal, String painelInicio, String painelFinal, String observacoesAdicionaisInicio, String observacoesAdicionaisFinal, Long carro, Long motorista) {
        this.status = status;
        this.verificadorInicial = verificadorInicial;
        this.verificadorFinal = verificadorFinal;
        this.dataInicial = dataInicial;
        this.dataFinal = dataFinal;
        this.finalizado = finalizado;
        this.frenteInicial = frenteInicial;
        this.frenteFinal = frenteFinal;
        this.atrasInicio = atrasInicio;
        this.atrasFinal = atrasFinal;
        this.latariaEsquerdaInicio = latariaEsquerdaInicio;
        this.latariaEsquerdaFinal = latariaEsquerdaFinal;
        this.latariaDireitaInicio = latariaDireitaInicio;
        this.latariaDireitaFinal = latariaDireitaFinal;
        this.painelInicio = painelInicio;
        this.painelFinal = painelFinal;
        this.observacoesAdicionaisInicio = observacoesAdicionaisInicio;
        this.observacoesAdicionaisFinal = observacoesAdicionaisFinal;
        this.carro = carro;
        this.motorista = motorista;
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

    public String getFrenteInicial() {
        return frenteInicial;
    }

    public void setFrenteInicial(String frenteInicial) {
        this.frenteInicial = frenteInicial;
    }

    public String getFrenteFinal() {
        return frenteFinal;
    }

    public void setFrenteFinal(String frenteFinal) {
        this.frenteFinal = frenteFinal;
    }

    public String getAtrasInicio() {
        return atrasInicio;
    }

    public void setAtrasInicio(String atrasInicio) {
        this.atrasInicio = atrasInicio;
    }

    public String getAtrasFinal() {
        return atrasFinal;
    }

    public void setAtrasFinal(String atrasFinal) {
        this.atrasFinal = atrasFinal;
    }

    public String getLatariaEsquerdaInicio() {
        return latariaEsquerdaInicio;
    }

    public void setLatariaEsquerdaInicio(String latariaEsquerdaInicio) {
        this.latariaEsquerdaInicio = latariaEsquerdaInicio;
    }

    public String getLatariaEsquerdaFinal() {
        return latariaEsquerdaFinal;
    }

    public void setLatariaEsquerdaFinal(String latariaEsquerdaFinal) {
        this.latariaEsquerdaFinal = latariaEsquerdaFinal;
    }

    public String getLatariaDireitaInicio() {
        return latariaDireitaInicio;
    }

    public void setLatariaDireitaInicio(String latariaDireitaInicio) {
        this.latariaDireitaInicio = latariaDireitaInicio;
    }

    public String getLatariaDireitaFinal() {
        return latariaDireitaFinal;
    }

    public void setLatariaDireitaFinal(String latariaDireitaFinal) {
        this.latariaDireitaFinal = latariaDireitaFinal;
    }

    public String getPainelInicio() {
        return painelInicio;
    }

    public void setPainelInicio(String painelInicio) {
        this.painelInicio = painelInicio;
    }

    public String getPainelFinal() {
        return painelFinal;
    }

    public void setPainelFinal(String painelFinal) {
        this.painelFinal = painelFinal;
    }


    public String getObservacoesAdicionaisFinal() {
        return observacoesAdicionaisFinal;
    }

    public void setObservacoesAdicionaisFinal(String observacoesAdicionaisFinal) {
        this.observacoesAdicionaisFinal = observacoesAdicionaisFinal;
    }

    public String getObservacoesAdicionaisInicio() {
        return observacoesAdicionaisInicio;
    }

    public void setObservacoesAdicionaisInicio(String observacoesAdicionaisInicio) {
        this.observacoesAdicionaisInicio = observacoesAdicionaisInicio;
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
