package com.mally.poopy.ui.list;

public class ListItem {

    private String item_id, date, stat, lv;

    public ListItem(){}

    public ListItem(String date, String stat, String lv, String item_id){
        this.date = date;
        this.stat = stat;
        this.lv = lv;
        this.item_id = item_id;
    }

    public String getItem_id() {
        return item_id;
    }

    public void setItem_id(String item_id) {
        this.item_id = item_id;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getStat() {
        return stat;
    }

    public void setStat(String stat) {
        this.stat = stat;
    }

    public String getLv() {
        return lv;
    }

    public void setLv(String lv) {
        this.lv = lv;
    }

}
