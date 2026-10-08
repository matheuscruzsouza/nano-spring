with open('nano-spring-core/src/main/java/com/github/matheuscruzsouza/nanospring/server/Environment.java', 'r') as f:
    text = f.read()

import re

# Add imports
text = text.replace('import java.util.Properties;', 'import java.util.Properties;\nimport android.app.ActivityManager;\nimport android.util.Log;')

# Add addActiveProfile method
add_profile_method = """
    public static void addActiveProfile(String profile) {
        if (!hasActiveProfile(profile)) {
            String[] newProfiles = new String[activeProfiles.length + 1];
            System.arraycopy(activeProfiles, 0, newProfiles, 0, activeProfiles.length);
            newProfiles[activeProfiles.length] = profile;
            activeProfiles = newProfiles;
        }
    }

    private static void detectHardwareCapabilities(Context context) {
        if (context == null) return;
        try {
            ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null) {
                ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
                am.getMemoryInfo(memInfo);
                
                // Hardware is considered low memory if total RAM is <= 2GB, or if Android OS flags it as low-memory
                boolean isLowMemoryDevice = memInfo.lowMemory || memInfo.totalMem <= (2L * 1024 * 1024 * 1024);
                
                if (isLowMemoryDevice) {
                    addActiveProfile("low-memory");
                    Log.i("Environment", "Detected low-memory device. Enabled 'low-memory' profile.");
                }
            }
        } catch (Exception e) {
            Log.w("Environment", "Could not detect hardware capabilities.", e);
        }
    }
"""
text = re.sub(r'public static void setActiveProfiles\(String... profiles\) \{', add_profile_method + '\n    public static void setActiveProfiles(String... profiles) {', text)

# Add to init
text = text.replace('        resolveActiveProfiles(context);\n\n        initialized = true;', '        resolveActiveProfiles(context);\n        detectHardwareCapabilities(context);\n\n        initialized = true;')

with open('nano-spring-core/src/main/java/com/github/matheuscruzsouza/nanospring/server/Environment.java', 'w') as f:
    f.write(text)
