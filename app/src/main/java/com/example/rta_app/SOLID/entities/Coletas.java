package com.example.rta_app.SOLID.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;



@Data
@Builder
public class Coletas {
    private String codigos;
    private String entregador;
    private String Qtd;

    public Coletas(String codigos, String entregador, String qtd) {
        this.codigos = codigos;
        this.entregador = entregador;
        Qtd = qtd;
    }
    public Coletas() {
    }

    public String getCodigos() {
        return codigos;
    }

    public void setCodigos(String codigos) {
        this.codigos = codigos;
    }

    public String getEntregador() {
        return entregador;
    }

    public void setEntregador(String entregador) {
        this.entregador = entregador;
    }

    public String getQtd() {
        return Qtd;
    }

    public void setQtd(String qtd) {
        Qtd = qtd;
    }
}
