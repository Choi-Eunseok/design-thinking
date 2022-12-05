package com.example.ponyo;

public class Meme {
    String ko;
    String en;

    public Meme(){}

    public String getKo() {
        return ko;
    }

    public void setKo(String _ko) {
        this.ko = _ko;
    }

    public String getEn() {
        return en;
    }

    public void setEn(String _en) {
        this.en = _en;
    }

    public Meme(String _ko, String _en){
        this.ko = _ko;
        this.en = _en;
    }
}