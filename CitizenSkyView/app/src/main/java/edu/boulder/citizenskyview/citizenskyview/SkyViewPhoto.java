package edu.boulder.citizenskyview.citizenskyview;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
/**
 * Created by Nicholas on 7/20/2017.
 */
@DynamoDBTable(tableName = "Events")
class SkyViewPhoto {
    //size" (in bytes), "timestamp" (UTC), "filetype" (JPEG), "sizex", "sizey", "heading" (compass in degrees), "phonemodel", "lat", "lon"
    private int size;
    private String timestamp;
    private String filetype;
    private int sizex;
    private int sizey;
    private String heading;
    private String phonemodel;
    private String lat;
    private String lon;

    @DynamoDBIndexRangeKey(attributeName = "Heading")
    public String getHeading() {
        return heading;
    }

    public void setHeading(String heading) {
        this.heading = heading;
    }

    @DynamoDBAttribute(attributeName = "size")
    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    @DynamoDBAttribute(attributeName = "TimeStamp")
    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @DynamoDBAttribute(attributeName = "FileType")
    public String getFiletype() {
        return filetype;
    }

    public void setFiletype(String filetype) {
        this.filetype = filetype;
    }

    @DynamoDBAttribute(attributeName = "sizex")
    public int getSizex() {
        return sizex;
    }

    public void setSizex(int sizex) {
        this.sizex = sizex;
    }

    @DynamoDBAttribute(attributeName = "sizey")
    public int getSizey() {
        return sizey;
    }

    public void setSizey(int sizey) {
        this.sizey = sizey;
    }

    @DynamoDBAttribute(attributeName = "PhoneModel")
    public String getPhoneModel() {
        return phonemodel;
    }

    public void setPhoneModel(String phoneModel) {
        this.phonemodel = phoneModel;
    }

    @DynamoDBAttribute(attributeName = "Lat")
    public String getLat() {
        return lat;
    }

    public void setLat(String lat) {
        this.lat = lat;
    }

    @DynamoDBAttribute(attributeName = "Lon")
    public String getLon() {
        return lon;
    }

    public void setLon(String lon) {
        this.lon = lon;
    }


}

