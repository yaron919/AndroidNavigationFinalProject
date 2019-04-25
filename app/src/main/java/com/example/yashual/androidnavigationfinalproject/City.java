package com.example.yashual.androidnavigationfinalproject;

public class City {

    int code = 0;
    String name = null;
    boolean selected = false;

    public City(int code, String name, boolean selected) {
        this.code = code;
        this.name = name;
        this.selected = selected;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }





}
