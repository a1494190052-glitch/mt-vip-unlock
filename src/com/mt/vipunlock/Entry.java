package com.mt.vipunlock;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * MT Manager VIP gate unlock (local research build).
 *
 * The two gates are static native boolean methods inside an obfuscated
 * class whose identifier consists of non-printable code points. They act
 * as the central VIP checks used across the app (recycle bin, octal
 * conversion, hex editor save, plugins, SMB/SFTP/FTPS/WebDav, custom
 * icons, extra search dictionaries, ...).
 *
 * Notes on the identifiers:
 *   - Java processes \uXXXX escapes before lexing, so these char/string
 *     literals fold into compile-time constants even though this source
 *     file itself is pure ASCII. The resulting descriptors are written
 *     into the dex constant pool verbatim, which we can verify statically.
 *   - Descriptor of the gate class: Ll/U+06DF U+1A7B U+06E8;
 *   - gate 1 name: U+06D6, gate 2 name: U+06E1.
 */
public class Entry implements IXposedHookLoadPackage {

    private static final String TARGET_PKG = "bin.mt.plus";

    private static final String GATE_CLASS = "l." + '\u06df' + '\u1a7b' + '\u06e8';

    private static final String GATE_VIP1 = "\u06d6";
    private static final String GATE_VIP2 = "\u06e1";

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (!TARGET_PKG.equals(lpparam.packageName)) {
            return;
        }

        XposedBridge.log("[MTVIP] target loaded: " + lpparam.packageName);

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
