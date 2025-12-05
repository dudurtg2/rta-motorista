package com.example.rta_app.SOLID.entities.utils;

public enum PointType {
    ENTRADA("Entrada"),
    ALMOCO_INICIO("Almoço"),
    ALMOCO_FIM("Saída"),
    FIM("Fim");

    private final String label;

    PointType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
