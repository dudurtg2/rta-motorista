package com.example.rta_app.SOLID.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Carro {
    private long id;
    private String placa;
    private String marca;
    private String modelo;
    private String tipo;
    private String cor;
    private String status;
}
