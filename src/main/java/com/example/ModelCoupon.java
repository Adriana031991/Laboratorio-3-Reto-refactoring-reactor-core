package com.example;

public class ModelCoupon {
    String codigo;
    String fecha;

    public ModelCoupon(String codigo, String fecha) {
        this.codigo = codigo;
        this.fecha = fecha;
    }

    public ModelCoupon() {
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public String getFecha() {
        return fecha;
    }

    public void setFecha(String fecha) {
        this.fecha = fecha;
    }
}
