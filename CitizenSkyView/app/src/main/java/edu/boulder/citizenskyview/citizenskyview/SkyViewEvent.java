package edu.boulder.citizenskyview.citizenskyview;
import com.amazonaws.mobileconnectors.dynamodbv2.dynamodbmapper.*;
/**
 * Created by Nicholas on 7/20/2017.
 */
@DynamoDBTable(tableName = "Event")
public class SkyViewEvent {

    private String eventName;
    private String end;
    private String start;
    private String num;

    @DynamoDBIndexRangeKey(attributeName = "EventName")
    public String getEventName() {
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

    @DynamoDBHashKey(attributeName = "Num")
    public String getNum() {
        return num;
    }

    public void setNum(String num) {
        this.num = num;
    }
}