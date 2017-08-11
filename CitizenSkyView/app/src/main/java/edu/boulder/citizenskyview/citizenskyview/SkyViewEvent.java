package edu.boulder.citizenskyview.citizenskyview;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
/**
 * Created by Nicholas on 7/20/2017.
 */
@DynamoDBTable(tableName = "Events")
class SkyViewEvent {

    private String eventName;
    private String end;
    private String start;
    private String name;

    @DynamoDBHashKey(attributeName = "EventName")
    public String getEvetName() {
        return eventName;
    }

    public void setEventName(String name) {
        this.eventName = name;
    }


    @DynamoDBAttribute(attributeName = "End")
    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    @DynamoDBAttribute(attributeName = "Start")
    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    @DynamoDBAttribute(attributeName = "Name")
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}