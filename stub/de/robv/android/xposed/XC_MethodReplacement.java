package de.robv.android.xposed;

public abstract class XC_MethodReplacement extends XC_MethodHook {

    public XC_MethodReplacement() {
        super();
    }

    public XC_MethodReplacement(int priority) {
        super(priority);
    }

    protected abstract Object replaceHookedMethod(MethodHookParam param) throws Throwable;

    public static XC_MethodHook returnConstant(final Object result) {
        return new XC_MethodReplacement() {
            @Override
            protected Object replaceHookedMethod(MethodHookParam param) {
                return result;
            }
        };
    }
}
