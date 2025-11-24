package com.example.rta_app.SOLID.entities;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@AllArgsConstructor
@Data

public class Carro {
    private long id;
    private String placa;
    private String marca;
    private String modelo;
    private String tipo;
    private String cor;
    private String status;

    // NOVO: lista de URLs das imagens
    private List<String> imagens;

    public Carro() {
    }

    public Carro(long id, String placa, String marca, String modelo, String tipo, String cor, String status) {
        this.id = id;
        this.placa = placa;
        this.marca = marca;
        this.modelo = modelo;
        this.tipo = tipo;
        this.cor = cor;
        this.status = status;
    }

    // getters e setters

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getPlaca() { return placa; }
    public void setPlaca(String placa) { this.placa = placa; }

    public String getMarca() { return marca; }
    public void setMarca(String marca) { this.marca = marca; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getCor() { return cor; }
    public void setCor(String cor) { this.cor = cor; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public List<String> getImagens() { return imagens; }
    public void setImagens(List<String> imagens) { this.imagens = imagens; }

    @Override
    public String toString() {
        return "Carro{" +
                "id=" + id +
                ", placa='" + placa + '\'' +
                ", marca='" + marca + '\'' +
                ", modelo='" + modelo + '\'' +
                ", tipo='" + tipo + '\'' +
                ", cor='" + cor + '\'' +
                ", status='" + status + '\'' +
                ", imagens=" + imagens +
                '}';
    }
}

