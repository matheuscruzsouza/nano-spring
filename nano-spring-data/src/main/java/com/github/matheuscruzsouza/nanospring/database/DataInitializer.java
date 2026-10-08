package com.github.matheuscruzsouza.nanospring.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.server.ApplicationInitializer;
import com.github.matheuscruzsouza.nanospring.server.Environment;

import java.util.Map;

public class DataInitializer implements ApplicationInitializer {

    @Override
    public void initialize(Context context, Map<Class<?>, Object> services, String basePackage) {
        if (context == null) return;
        boolean migrationEnabled = Boolean.parseBoolean(Environment.getProperty("nano.datasource.migration.enabled", "true"));
        if (!migrationEnabled) return;

        String dbName = Environment.getProperty("nano.datasource.name", "nanospring.db");
        try {
            SQLiteDatabase db = context.openOrCreateDatabase(dbName, Context.MODE_PRIVATE, null);
            boolean walEnabled = Boolean.parseBoolean(Environment.getProperty("nano.datasource.wal.enabled", "true"));
            if (walEnabled) {
                try {
                    boolean walSuccess = db.enableWriteAheadLogging();
                    Log.d("DATABASE_WAL", "SQLite WAL mode habilitado: " + walSuccess);
                } catch (Exception e) {
                    Log.w("DATABASE_WAL", "Nao foi possivel ativar WAL mode: " + e.getMessage());
                }
            }
            SqliteMigrator.migrate(context, db);
            services.put(SQLiteDatabase.class, db);
            Log.d("DATABASE_OK", "Banco de dados SQLite inicializado com sucesso: " + dbName);
        } catch (Exception e) {
            Log.e("DATABASE_FAIL", "Erro ao inicializar banco de dados SQLite ou executar migrações", e);
            e.printStackTrace();
        }
    }

    @Override
    public void onStart(Context context, int port) {}

    @Override
    public void onStop() {}
}
