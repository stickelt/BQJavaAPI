package com.example.bqjavaapi;

public class Record {
    private final String uuid;
    private final String rxDataId;

    public Record(String uuid, String rxDataId) {
        this.uuid = uuid;
        this.rxDataId = rxDataId;
    }

    public String getUuid() {
        return uuid;
    }

    public String getRxDataId() {
        return rxDataId;
    }

    @Override
    public String toString() {
        return "Record{" +
                "uuid='" + uuid + '\'' +
                ", rxDataId='" + rxDataId + '\'' +
                '}';
    }
}
