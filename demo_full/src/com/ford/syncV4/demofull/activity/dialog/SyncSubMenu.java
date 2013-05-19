package com.ford.syncV4.demofull.activity.dialog;

public class SyncSubMenu {
    private String menuName;
    private int id;

    public void setSubMenuId(int parentID) {
        this.id = parentID;
    }

    public int getSubMenuId() {
        return this.id;
    }

    public void setName(String name) {
        this.menuName = name;
    }

    public String getName() {
        return this.menuName;
    }

    @Override
    public String toString() {
        return "(" + getSubMenuId() + ") " + getName();
    }

}
