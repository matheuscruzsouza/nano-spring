with open('nano-spring-web/src/main/java/com/github/matheuscruzsouza/nanospring/server/Server.java', 'r') as f:
    text = f.read()

import re

imports = """import android.content.ComponentCallbacks2;
import android.content.res.Configuration;"""

text = text.replace('import android.content.Context;', 'import android.content.Context;\n' + imports)

trim_memory_logic = """
        if (context != null) {
            context.getApplicationContext().registerComponentCallbacks(new ComponentCallbacks2() {
                @Override
                public void onTrimMemory(int level) {
                    if (level >= ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
                        Log.w("SERVER_MEMORY", "Android requested to trim memory (level " + level + "). Clearing caches and suggesting GC.");
                        System.gc();
                    }
                }
                @Override
                public void onConfigurationChanged(Configuration newConfig) {}
                @Override
                public void onLowMemory() {
                    Log.w("SERVER_MEMORY", "Android signaled LOW MEMORY! Suggesting GC.");
                    System.gc();
                }
            });
        }
"""
text = text.replace('        Environment.init(context);', '        Environment.init(context);\n' + trim_memory_logic)

with open('nano-spring-web/src/main/java/com/github/matheuscruzsouza/nanospring/server/Server.java', 'w') as f:
    f.write(text)
