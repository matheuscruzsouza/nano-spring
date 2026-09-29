package com.github.matheuscruzsouza.nanospring.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Log;

import com.github.matheuscruzsouza.nanospring.server.Environment;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SqliteMigrator {
    private static final String TAG = "SqliteMigrator";
    private static final String DEFAULT_LOCATION = "db/migration";
    private static final String HISTORY_TABLE = "nano_schema_history";
    private static final Pattern MIGRATION_FILE_PATTERN = Pattern.compile("^V([0-9]+)__(.+)\\.sql$", Pattern.CASE_INSENSITIVE);

    public static class MigrationFile implements Comparable<MigrationFile> {
        private final int version;
        private final String description;
        private final String fileName;
        private final String fullPath;

        public MigrationFile(int version, String description, String fileName, String fullPath) {
            this.version = version;
            this.description = description;
            this.fileName = fileName;
            this.fullPath = fullPath;
        }

        public int getVersion() { return version; }
        public String getDescription() { return description; }
        public String getFileName() { return fileName; }
        public String getFullPath() { return fullPath; }

        @Override
        public int compareTo(MigrationFile other) {
            return Integer.compare(this.version, other.version);
        }
    }

    public static void migrate(Context context, SQLiteDatabase db) {
        String location = Environment.getProperty("nano.datasource.migration.location", DEFAULT_LOCATION);
        migrate(context, db, location);
    }

    public static void migrate(Context context, SQLiteDatabase db, String location) {
        if (context == null || db == null) return;

        createHistoryTableIfNotExists(db);
        Set<Integer> appliedVersions = getAppliedVersions(db);
        int currentRank = getMaxRank(db);

        List<MigrationFile> migrationFiles = scanMigrationFiles(context, location);
        Collections.sort(migrationFiles);

        for (MigrationFile file : migrationFiles) {
            if (appliedVersions.contains(file.getVersion())) {
                continue;
            }

            Log.i(TAG, "Migrating to version " + file.getVersion() + " - " + file.getDescription());
            long startTime = System.currentTimeMillis();

            String sqlContent;
            try {
                sqlContent = readAssetFile(context, file.getFullPath());
            } catch (IOException e) {
                throw new MigrationException("Could not read migration script: " + file.getFullPath(), e);
            }

            List<String> statements = splitSqlStatements(sqlContent);

            db.beginTransaction();
            try {
                for (String statement : statements) {
                    if (!statement.trim().isEmpty()) {
                        db.execSQL(statement);
                    }
                }

                currentRank++;
                long executionTime = System.currentTimeMillis() - startTime;
                recordMigration(db, currentRank, file.getVersion(), file.getDescription(), file.getFileName(), executionTime, 1);

                db.setTransactionSuccessful();
                Log.i(TAG, "Successfully applied migration V" + file.getVersion() + " (" + executionTime + "ms)");
            } catch (Exception e) {
                Log.e(TAG, "Migration V" + file.getVersion() + " failed: " + e.getMessage(), e);
                throw new MigrationException("Migration V" + file.getVersion() + " (" + file.getFileName() + ") failed: " + e.getMessage(), e);
            } finally {
                db.endTransaction();
            }
        }
    }

    private static void createHistoryTableIfNotExists(SQLiteDatabase db) {
        String sql = "CREATE TABLE IF NOT EXISTS " + HISTORY_TABLE + " ("
                + "installed_rank INTEGER PRIMARY KEY, "
                + "version INTEGER NOT NULL UNIQUE, "
                + "description TEXT, "
                + "script TEXT NOT NULL, "
                + "installed_on TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                + "execution_time INTEGER NOT NULL, "
                + "success INTEGER NOT NULL"
                + ");";
        db.execSQL(sql);
    }

    private static Set<Integer> getAppliedVersions(SQLiteDatabase db) {
        Set<Integer> versions = new HashSet<>();
        try (Cursor cursor = db.rawQuery("SELECT version FROM " + HISTORY_TABLE + " WHERE success = 1", null)) {
            while (cursor.moveToNext()) {
                versions.add(cursor.getInt(0));
            }
        }
        return versions;
    }

    private static int getMaxRank(SQLiteDatabase db) {
        try (Cursor cursor = db.rawQuery("SELECT COALESCE(MAX(installed_rank), 0) FROM " + HISTORY_TABLE, null)) {
            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        }
        return 0;
    }

    private static void recordMigration(SQLiteDatabase db, int rank, int version, String description, String script, long executionTime, int success) {
        String insertSql = "INSERT INTO " + HISTORY_TABLE + " (installed_rank, version, description, script, execution_time, success) VALUES (?, ?, ?, ?, ?, ?)";
        try (SQLiteStatement stmt = db.compileStatement(insertSql)) {
            stmt.bindLong(1, rank);
            stmt.bindLong(2, version);
            if (description != null) {
                stmt.bindString(3, description);
            } else {
                stmt.bindNull(3);
            }
            stmt.bindString(4, script);
            stmt.bindLong(5, executionTime);
            stmt.bindLong(6, success);
            stmt.executeInsert();
        }
    }

    public static List<MigrationFile> scanMigrationFiles(Context context, String location) {
        List<MigrationFile> files = new ArrayList<>();
        try {
            String[] assetList = context.getAssets().list(location);
            if (assetList == null) return files;

            for (String fileName : assetList) {
                Matcher matcher = MIGRATION_FILE_PATTERN.matcher(fileName);
                if (matcher.matches()) {
                    int version = Integer.parseInt(matcher.group(1));
                    String description = matcher.group(2).replace('_', ' ');
                    String fullPath = location.isEmpty() ? fileName : location + "/" + fileName;
                    files.add(new MigrationFile(version, description, fileName, fullPath));
                }
            }
        } catch (IOException e) {
            Log.w(TAG, "No migration folder found in assets/" + location);
        }
        return files;
    }

    private static String readAssetFile(Context context, String path) throws IOException {
        try (InputStream is = context.getAssets().open(path);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        }
    }

    public static List<String> splitSqlStatements(String script) {
        List<String> statements = new ArrayList<>();
        if (script == null || script.trim().isEmpty()) {
            return statements;
        }

        StringBuilder currentStatement = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        int length = script.length();
        for (int i = 0; i < length; i++) {
            char c = script.charAt(i);
            char next = (i + 1 < length) ? script.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                    currentStatement.append(c);
                }
                continue;
            }

            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++; // skip '/'
                }
                continue;
            }

            if (inSingleQuote) {
                currentStatement.append(c);
                if (c == '\'') {
                    if (next == '\'') {
                        // Escaped quote ''
                        currentStatement.append(next);
                        i++;
                    } else {
                        inSingleQuote = false;
                    }
                }
                continue;
            }

            if (inDoubleQuote) {
                currentStatement.append(c);
                if (c == '"') {
                    inDoubleQuote = false;
                }
                continue;
            }

            // Not inside quotes or comments
            if (c == '-' && next == '-') {
                inLineComment = true;
                i++; // skip second '-'
                continue;
            }

            if (c == '/' && next == '*') {
                inBlockComment = true;
                i++; // skip '*'
                continue;
            }

            if (c == '\'') {
                inSingleQuote = true;
                currentStatement.append(c);
                continue;
            }

            if (c == '"') {
                inDoubleQuote = true;
                currentStatement.append(c);
                continue;
            }

            if (c == ';') {
                String stmt = currentStatement.toString().trim();
                if (!stmt.isEmpty()) {
                    statements.add(stmt);
                }
                currentStatement.setLength(0);
                continue;
            }

            currentStatement.append(c);
        }

        String remaining = currentStatement.toString().trim();
        if (!remaining.isEmpty()) {
            statements.add(remaining);
        }

        return statements;
    }
}
