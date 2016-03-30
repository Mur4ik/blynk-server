package cc.blynk.server.core.model.widgets.others;

import cc.blynk.server.core.model.widgets.Widget;
import cc.blynk.utils.JsonParser;
import org.junit.Test;

import java.time.ZoneOffset;

import static org.junit.Assert.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 30.03.16.
 */
public class RTCSerializationTest {

    @Test
    public void tesDeSerializationIsCorrect() {
        String widgetString = "{\"id\":1, \"x\":1, \"y\":1, \"type\":\"RTC\", \"timezone\":\"+03:00\"}";
        Widget widget = JsonParser.parseWidget(widgetString, 1);

        assertNotNull(widget);

        RTC rtc = (RTC) widget;
        assertNotNull(rtc.timezone);
        assertEquals(ZoneOffset.of("+03:00"), rtc.timezone);
    }

    @Test
    public void tesDeSerializationIsCorrectForNull() {
        String widgetString = "{\"id\":1, \"x\":1, \"y\":1, \"type\":\"RTC\"}";
        Widget widget = JsonParser.parseWidget(widgetString, 1);

        assertNotNull(widget);

        RTC rtc = (RTC) widget;
        assertNull(rtc.timezone);
    }

    @Test
    public void tesSerializationIsCorrect() throws Exception {
        RTC rtc = new RTC();
        rtc.timezone = ZoneOffset.of("+03:00");

        String widgetString = JsonParser.mapper.writeValueAsString(rtc);

        assertNotNull(widgetString);
        assertEquals("{\"type\":\"RTC\",\"id\":0,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"pin\":-1," +
                "\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0," +
                "\"timezone\":\"+03:00\"}", widgetString);
    }

    @Test
    public void tesSerializationIsCorrectForNull() throws Exception {
        RTC rtc = new RTC();
        rtc.timezone = null;

        String widgetString = JsonParser.mapper.writeValueAsString(rtc);

        assertNotNull(widgetString);
        assertEquals("{\"type\":\"RTC\",\"id\":0,\"x\":0,\"y\":0,\"color\":0,\"width\":0,\"height\":0,\"pin\":-1," +
                "\"pwmMode\":false,\"rangeMappingOn\":false,\"min\":0,\"max\":0}", widgetString);
    }

}