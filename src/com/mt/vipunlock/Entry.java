package com.mt.vipunlock;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * MT Manager VIP gate unlock (research).
 *
 * Two static native boolean gates in the obfuscated class act as the
 * central VIP checks used across the app (recycle bin, octal conversion,
 * hex editor save, plugins, SMB/SFTP/FTPS/WebDav, custom icons, ...).
 *
 * The real names are non-printable, so they are built from code points
 * to keep this source ASCII-only.
 */
public class Entry implements IXposedHookLoadPackage {

    private static final String TARGET_PKG = "bin.mt.plus";

    /* l.\u06df\u1a7b\u06e8 */
    private static final String GATE_CLASS =
            "l." + new String(new char[]{0x06df, 0x1a7b, 0x06e8});

    /* public static native boolean \u06d6() */
    private static final String GATE_VIP1 = new String(new char[]{0x06d6});

    /* public static native boolean \u06e1() */
    private static final String GATE_VIP2 = new String(new char[]{0x06e1});

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PKG.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log("[MTVIP] target loaded, pid-package=" + lpparam.packageName);

        Class<?> gate;
        try {
            gate = XposedHelpers.findClass(GATE_CLASS, lpparam.classLoader);
        } catch (Throwable t) {
            XposedBridge.log("[MTVIP] gate class lookup failed: " + t);
            return;
        }

        hookGate(gate, GATE_VIP1, "vip1");
        hookGate(gate, GATE_VIP2, "vip2");
    }

    private void hookGate(Class<?> gateClass, String methodName, String label) {
        try {
            XposedHelpers.findAndHookMethod(
                    gateClass, methodName,
                    XC_MethodReplacement.returnConstant(Boolean.TRUE));
            XposedBridge.log("[MTVIP] hooked " + label);
        } catch (Throwable t) {
            XposedBridge.log("[MTVIP] hook failed " + label + ": " + t);
        }
    }
}
