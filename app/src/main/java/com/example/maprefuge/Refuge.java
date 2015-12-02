package com.example.maprefuge;

public class Refuge {
    private String name;
    private String address;
    private double latitude;
    private double longitude;
    private boolean near;
    private int distance;

    public Refuge(String name,String address,double latitude,double longitude){
        this.name = name;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public boolean isNear() {
        return near;
    }

    public void setNear(boolean near) {
        this.near = near;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(float distance) {
        this.distance = (int)distance;
    }
}
