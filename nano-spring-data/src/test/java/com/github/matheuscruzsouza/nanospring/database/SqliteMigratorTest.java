package com.github.matheuscruzsouza.nanospring.database;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;

public class SqliteMigratorTest {

    @Test
    public void testSplitSqlStatementsSimple() {
        String sql = "CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT);";
        List<String> stmts = SqliteMigrator.splitSqlStatements(sql);
        assertEquals(1, stmts.size());
        assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY, name TEXT)", stmts.get(0));
    }

    @Test
    public void testSplitSqlStatementsMultiple() {
        String sql = "CREATE TABLE users (id INTEGER PRIMARY KEY);\n" +
                     "CREATE TABLE orders (id INTEGER PRIMARY KEY, user_id INTEGER);\n" +
                     "INSERT INTO users (id) VALUES (1);";
        List<String> stmts = SqliteMigrator.splitSqlStatements(sql);
        assertEquals(3, stmts.size());
        assertEquals("CREATE TABLE users (id INTEGER PRIMARY KEY)", stmts.get(0));
        assertEquals("CREATE TABLE orders (id INTEGER PRIMARY KEY, user_id INTEGER)", stmts.get(1));
        assertEquals("INSERT INTO users (id) VALUES (1)", stmts.get(2));
    }

    @Test
    public void testSplitSqlStatementsWithCommentsAndSemicolonsInQuotes() {
        String sql = "-- Este é um comentário com ponto e vírgula: ; ignore!\n" +
                     "/* Comentário em bloco\n" +
                     "   com ; ponto e vírgula no meio */\n" +
                     "INSERT INTO configs (key, value) VALUES ('greeting', 'Hello; World!');\n" +
                     "UPDATE configs SET value = 'It''s a test; yes!' WHERE key = 'greeting';";

        List<String> stmts = SqliteMigrator.splitSqlStatements(sql);
        assertEquals(2, stmts.size());
        assertEquals("INSERT INTO configs (key, value) VALUES ('greeting', 'Hello; World!')", stmts.get(0));
        assertEquals("UPDATE configs SET value = 'It''s a test; yes!' WHERE key = 'greeting'", stmts.get(1));
    }

    @Test
    public void testSplitSqlStatementsWithoutTrailingSemicolon() {
        String sql = "SELECT * FROM users";
        List<String> stmts = SqliteMigrator.splitSqlStatements(sql);
        assertEquals(1, stmts.size());
        assertEquals("SELECT * FROM users", stmts.get(0));
    }

    @Test
    public void testMigrationFileSorting() {
        List<SqliteMigrator.MigrationFile> list = new ArrayList<>();
        list.add(new SqliteMigrator.MigrationFile(10, "add column", "V10__add_column.sql", "db/migration/V10__add_column.sql"));
        list.add(new SqliteMigrator.MigrationFile(1, "init", "V1__init.sql", "db/migration/V1__init.sql"));
        list.add(new SqliteMigrator.MigrationFile(2, "add orders", "V2__add_orders.sql", "db/migration/V2__add_orders.sql"));

        Collections.sort(list);

        assertEquals(1, list.get(0).getVersion());
        assertEquals(2, list.get(1).getVersion());
        assertEquals(10, list.get(2).getVersion());
    }

    @Test
    public void testMigrateNullSafe() {
        SqliteMigrator.migrate(null, null);
        SqliteMigrator.migrate(null, null, "custom/path");
    }
}
