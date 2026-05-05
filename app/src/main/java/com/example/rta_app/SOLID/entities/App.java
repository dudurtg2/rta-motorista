package com.example.rta_app.SOLID.entities;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;


@AllArgsConstructor
@Data

public class App {
    private long id;
    private String versao;
    private String link;
    private Boolean atualizado;

    public App(String versao, String link, Boolean atualizado) {
        this.versao = versao;
        this.link = link;
        this.atualizado = atualizado;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getVersao() {
        return versao;
    }

    public void setVersao(String versao) {
        this.versao = versao;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Boolean getAtualizado() {
        return atualizado;
    }

    public void setAtualizado(Boolean atualizado) {
        this.atualizado = atualizado;
    }
}

