package com.etechd.l3mon.loader;

import android.view.accessibility.AccessibilityNodeInfo;
import com.etechd.l3mon.managers.ATSManager;

public interface IATSModule {
    String getModuleName();
    void execute(AccessibilityNodeInfo root, ATSManager.ATSConfig config);
}