package com.github.matheuscruzsouza.nanospring.server;

import android.content.Context;
import java.util.Map;

public interface ApplicationInitializer {
    void initialize(Context context, Map<Class<?>, Object> services, String basePackage);
    void onStart(Context context, int port);
    void onStop();
}
