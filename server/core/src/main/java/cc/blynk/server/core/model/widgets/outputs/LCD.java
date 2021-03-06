package cc.blynk.server.core.model.widgets.outputs;

import cc.blynk.server.core.model.Pin;
import cc.blynk.server.core.model.widgets.MultiPinWidget;
import cc.blynk.utils.StringUtils;
import io.netty.channel.Channel;

import java.util.HashMap;
import java.util.Map;

import static cc.blynk.server.core.protocol.enums.Command.*;
import static cc.blynk.utils.ByteBufUtil.*;

/**
 * The Blynk Project.
 * Created by Dmitriy Dumanskiy.
 * Created on 21.03.15.
 */
public class LCD extends MultiPinWidget implements FrequencyWidget {

    public boolean advancedMode;

    public String textFormatLine1;

    public String textFormatLine2;

    //todo remove after migration.
    public boolean textLight;

    public boolean textLightOn;

    private int frequency;

    private transient Map<String, Long> lastRequestTS = new HashMap<>();

    @Override
    public void sendSyncOnActivate(Channel appChannel, int dashId) {
        if (pins == null) {
            return;
        }
        if (!advancedMode) {
            for (Pin pin : pins) {
                if (pin.notEmpty()) {
                    String body = dashId + StringUtils.BODY_SEPARATOR_STRING + pin.makeHardwareBody();
                    appChannel.write(makeStringMessage(SYNC, 1111, body), appChannel.voidPromise());
                }
            }
        } else {
            if (pins[0].notEmpty()) {
                String body = dashId + StringUtils.BODY_SEPARATOR_STRING + pins[0].makeHardwareBody();
                appChannel.write(makeStringMessage(SYNC, 1111, body), appChannel.voidPromise());
            }
        }
    }

    @Override
    public boolean isSplitMode() {
        return !advancedMode;
    }

    @Override
    public final int getFrequency() {
        return frequency;
    }

    @Override
    public final long getLastRequestTS(String body) {
        return lastRequestTS.getOrDefault(body, 0L);
    }

    @Override
    public final void setLastRequestTS(String body, long now) {
        this.lastRequestTS.put(body, now);
    }

    @Override
    public String getModeType() {
        return "in";
    }

    @Override
    public int getPrice() {
        return 400;
    }
}
