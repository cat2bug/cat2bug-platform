package com.cat2bug.web.service.upgrade;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 通过 JDBC 导出整库 SQL，并按语句逐条执行还原（不依赖 mysqldump/mysql 等外部命令）。
 */
final class JdbcDatabaseSqlBackupSupport
{
    enum Dialect
    {
        MYSQL,
        H2
    }

    private JdbcDatabaseSqlBackupSupport()
    {
    }

    static void exportDatabase(Connection connection, String schema, Path target, Dialect dialect)
            throws SQLException, IOException
    {
        Files.createDirectories(target.getParent());
        try (BufferedWriter writer = Files.newBufferedWriter(target, StandardCharsets.UTF_8);
                Statement statement = connection.createStatement())
        {
            writer.write("-- Cat2Bug database backup");
            writer.newLine();
            writer.write("-- dialect: " + dialect.name().toLowerCase(Locale.ROOT));
            writer.newLine();
            writer.write("-- schema: " + schema);
            writer.newLine();
            writer.newLine();

            if (dialect == Dialect.MYSQL)
            {
                writer.write("SET NAMES utf8mb4;");
                writer.newLine();
                writer.write("SET FOREIGN_KEY_CHECKS=0;");
                writer.newLine();
            }
            else
            {
                writer.write("SET REFERENTIAL_INTEGRITY FALSE;");
                writer.newLine();
            }
            writer.newLine();

            List<String> views = listViews(statement, schema, dialect);
            for (String view : views)
            {
                writer.write("DROP VIEW IF EXISTS ");
                writer.write(quoteIdentifier(view, dialect));
                writer.write(";");
                writer.newLine();
            }
            writer.newLine();

            List<String> tables = listTables(statement, schema, dialect);
            for (String table : tables)
            {
                writer.write("DROP TABLE IF EXISTS ");
                writer.write(quoteIdentifier(table, dialect));
                writer.write(";");
                writer.newLine();
            }
            writer.newLine();

            for (String table : tables)
            {
                exportTableStructure(statement, writer, table, dialect);
                exportTableData(statement, writer, table, dialect);
                writer.newLine();
            }

            for (String view : views)
            {
                exportViewDefinition(statement, writer, schema, view, dialect);
                writer.newLine();
            }

            if (dialect == Dialect.MYSQL)
            {
                writer.write("SET FOREIGN_KEY_CHECKS=1;");
            }
            else
            {
                writer.write("SET REFERENTIAL_INTEGRITY TRUE;");
            }
            writer.newLine();
        }
    }

    static void restoreDatabase(Connection connection, Path backupFile, Dialect dialect)
            throws SQLException, IOException
    {
        String sql = Files.readString(backupFile, StandardCharsets.UTF_8);
        List<String> statements = splitSqlStatements(sql);
        try (Statement statement = connection.createStatement())
        {
            for (String chunk : statements)
            {
                String trimmed = chunk.trim();
                if (trimmed.isEmpty() || isCommentOnly(trimmed))
                {
                    continue;
                }
                statement.execute(trimmed);
            }
        }
    }

    static List<String> splitSqlStatements(String sql)
    {
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inSingleQuote = false;
        boolean inDoubleQuote = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;

        for (int i = 0; i < sql.length(); i++)
        {
            char ch = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

            if (inLineComment)
            {
                current.append(ch);
                if (ch == '\n')
                {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment)
            {
                current.append(ch);
                if (ch == '*' && next == '/')
                {
                    current.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && !inBacktick)
            {
                if (ch == '-' && next == '-')
                {
                    current.append(ch).append(next);
                    i++;
                    inLineComment = true;
                    continue;
                }
                if (ch == '/' && next == '*')
                {
                    current.append(ch).append(next);
                    i++;
                    inBlockComment = true;
                    continue;
                }
            }

            if (!inDoubleQuote && !inBacktick && ch == '\'')
            {
                inSingleQuote = !inSingleQuote;
                current.append(ch);
                continue;
            }
            if (!inSingleQuote && !inBacktick && ch == '"')
            {
                inDoubleQuote = !inDoubleQuote;
                current.append(ch);
                continue;
            }
            if (!inSingleQuote && !inDoubleQuote && ch == '`')
            {
                inBacktick = !inBacktick;
                current.append(ch);
                continue;
            }

            if (ch == ';' && !inSingleQuote && !inDoubleQuote && !inBacktick)
            {
                statements.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(ch);
        }

        if (!current.isEmpty())
        {
            statements.add(current.toString());
        }
        return statements;
    }

    private static List<String> listTables(Statement statement, String schema, Dialect dialect)
            throws SQLException
    {
        List<String> tables = new ArrayList<>();
        String sql = dialect == Dialect.MYSQL
                ? "SELECT TABLE_NAME FROM information_schema.TABLES "
                        + "WHERE TABLE_SCHEMA = ? AND TABLE_TYPE = 'BASE TABLE' ORDER BY TABLE_NAME"
                : "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE UPPER(TABLE_SCHEMA) = UPPER(?) AND TABLE_TYPE IN ('BASE TABLE', 'TABLE') "
                        + "ORDER BY TABLE_NAME";
        try (var ps = statement.getConnection().prepareStatement(sql))
        {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private static List<String> listViews(Statement statement, String schema, Dialect dialect)
            throws SQLException
    {
        List<String> views = new ArrayList<>();
        String sql = dialect == Dialect.MYSQL
                ? "SELECT TABLE_NAME FROM information_schema.VIEWS WHERE TABLE_SCHEMA = ? ORDER BY TABLE_NAME"
                : "SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES "
                        + "WHERE UPPER(TABLE_SCHEMA) = UPPER(?) AND TABLE_TYPE = 'VIEW' ORDER BY TABLE_NAME";
        try (var ps = statement.getConnection().prepareStatement(sql))
        {
            ps.setString(1, schema);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    views.add(rs.getString(1));
                }
            }
        }
        return views;
    }

    private static void exportTableStructure(
            Statement statement,
            BufferedWriter writer,
            String table,
            Dialect dialect) throws SQLException, IOException
    {
        if (dialect == Dialect.H2)
        {
            String schema = resolveH2Schema(statement.getConnection());
            writer.write(buildH2CreateTableDdl(statement, schema, table));
            writer.write(";");
            writer.newLine();
            writer.newLine();
            return;
        }

        String quoted = quoteIdentifier(table, dialect);
        try (ResultSet rs = statement.executeQuery("SHOW CREATE TABLE " + quoted))
        {
            if (!rs.next())
            {
                return;
            }
            String ddl = rs.getString(2);
            writer.write(ddl);
            writer.write(";");
            writer.newLine();
            writer.newLine();
        }
    }

    private static String buildH2CreateTableDdl(Statement statement, String schema, String table)
            throws SQLException
    {
        StringBuilder ddl = new StringBuilder("CREATE TABLE ");
        ddl.append(quoteIdentifier(table, Dialect.H2)).append(" (");
        List<String> columnDefs = new ArrayList<>();
        String columnsSql = "SELECT COLUMN_NAME, DATA_TYPE, CHARACTER_MAXIMUM_LENGTH, IS_NULLABLE, COLUMN_DEFAULT "
                + "FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE UPPER(TABLE_SCHEMA) = UPPER(?) AND TABLE_NAME = ? "
                + "ORDER BY ORDINAL_POSITION";
        try (var ps = statement.getConnection().prepareStatement(columnsSql))
        {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    columnDefs.add(buildH2ColumnDefinition(rs));
                }
            }
        }
        ddl.append(String.join(", ", columnDefs));

        List<String> primaryKeys = listPrimaryKeyColumns(statement, schema, table);
        if (!primaryKeys.isEmpty())
        {
            ddl.append(", PRIMARY KEY (");
            ddl.append(primaryKeys.stream()
                    .map(name -> quoteIdentifier(name, Dialect.H2))
                    .reduce((a, b) -> a + ", " + b)
                    .orElse(""));
            ddl.append(")");
        }
        ddl.append(")");
        return ddl.toString();
    }

    private static String buildH2ColumnDefinition(ResultSet rs) throws SQLException
    {
        StringBuilder column = new StringBuilder();
        column.append(quoteIdentifier(rs.getString("COLUMN_NAME"), Dialect.H2));
        column.append(" ");
        column.append(formatH2ColumnType(rs));
        if ("NO".equalsIgnoreCase(rs.getString("IS_NULLABLE")))
        {
            column.append(" NOT NULL");
        }
        String defaultValue = rs.getString("COLUMN_DEFAULT");
        if (defaultValue != null && !defaultValue.isBlank())
        {
            column.append(" DEFAULT ").append(defaultValue);
        }
        return column.toString();
    }

    private static String formatH2ColumnType(ResultSet rs) throws SQLException
    {
        String dataType = rs.getString("DATA_TYPE");
        Number length = (Number) rs.getObject("CHARACTER_MAXIMUM_LENGTH");
        if (length != null && length.longValue() > 0 && dataType != null && dataType.contains("CHARACTER"))
        {
            return dataType + "(" + length.longValue() + ")";
        }
        return dataType;
    }

    private static List<String> listPrimaryKeyColumns(Statement statement, String schema, String table)
            throws SQLException
    {
        List<String> columns = new ArrayList<>();
        String sql = "SELECT kcu.COLUMN_NAME FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE kcu "
                + "JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc "
                + "ON kcu.CONSTRAINT_SCHEMA = tc.CONSTRAINT_SCHEMA "
                + "AND kcu.CONSTRAINT_NAME = tc.CONSTRAINT_NAME "
                + "WHERE UPPER(kcu.TABLE_SCHEMA) = UPPER(?) AND kcu.TABLE_NAME = ? "
                + "AND tc.CONSTRAINT_TYPE = 'PRIMARY KEY' "
                + "ORDER BY kcu.ORDINAL_POSITION";
        try (var ps = statement.getConnection().prepareStatement(sql))
        {
            ps.setString(1, schema);
            ps.setString(2, table);
            try (ResultSet rs = ps.executeQuery())
            {
                while (rs.next())
                {
                    columns.add(rs.getString(1));
                }
            }
        }
        return columns;
    }

    private static void exportViewDefinition(
            Statement statement,
            BufferedWriter writer,
            String schema,
            String view,
            Dialect dialect) throws SQLException, IOException
    {
        if (dialect == Dialect.MYSQL)
        {
            String quoted = quoteIdentifier(view, dialect);
            try (ResultSet rs = statement.executeQuery("SHOW CREATE VIEW " + quoted))
            {
                if (!rs.next())
                {
                    return;
                }
                writer.write(rs.getString(2));
                writer.write(";");
                writer.newLine();
            }
            return;
        }

        String sql = "SELECT VIEW_DEFINITION FROM INFORMATION_SCHEMA.VIEWS "
                + "WHERE UPPER(TABLE_SCHEMA) = UPPER(?) AND TABLE_NAME = ?";
        try (var ps = statement.getConnection().prepareStatement(sql))
        {
            ps.setString(1, schema);
            ps.setString(2, view);
            try (ResultSet rs = ps.executeQuery())
            {
                if (!rs.next())
                {
                    return;
                }
                writer.write("CREATE VIEW ");
                writer.write(quoteIdentifier(view, dialect));
                writer.write(" AS ");
                writer.write(rs.getString(1));
                writer.write(";");
                writer.newLine();
            }
        }
    }

    private static void exportTableData(
            Statement statement,
            BufferedWriter writer,
            String table,
            Dialect dialect) throws SQLException, IOException
    {
        String quoted = quoteIdentifier(table, dialect);
        try (ResultSet rs = statement.executeQuery("SELECT * FROM " + quoted))
        {
            ResultSetMetaData meta = rs.getMetaData();
            int columnCount = meta.getColumnCount();
            if (columnCount == 0)
            {
                return;
            }
            String columnList = buildColumnList(meta, columnCount, dialect);
            while (rs.next())
            {
                writer.write("INSERT INTO ");
                writer.write(quoted);
                writer.write(" (");
                writer.write(columnList);
                writer.write(") VALUES (");
                for (int i = 1; i <= columnCount; i++)
                {
                    if (i > 1)
                    {
                        writer.write(", ");
                    }
                    writer.write(formatSqlValue(rs, i, meta.getColumnType(i)));
                }
                writer.write(");");
                writer.newLine();
            }
        }
    }

    private static String buildColumnList(ResultSetMetaData meta, int columnCount, Dialect dialect)
            throws SQLException
    {
        StringBuilder columns = new StringBuilder();
        for (int i = 1; i <= columnCount; i++)
        {
            if (i > 1)
            {
                columns.append(", ");
            }
            columns.append(quoteIdentifier(meta.getColumnName(i), dialect));
        }
        return columns.toString();
    }

    static String formatSqlValue(ResultSet rs, int columnIndex, int sqlType) throws SQLException
    {
        Object value = rs.getObject(columnIndex);
        if (value == null || rs.wasNull())
        {
            return "NULL";
        }
        if (sqlType == Types.BIT || sqlType == Types.BOOLEAN)
        {
            return Boolean.TRUE.equals(value) || "1".equals(String.valueOf(value)) ? "1" : "0";
        }
        if (sqlType == Types.TINYINT || sqlType == Types.SMALLINT || sqlType == Types.INTEGER
                || sqlType == Types.BIGINT || sqlType == Types.FLOAT || sqlType == Types.REAL
                || sqlType == Types.DOUBLE || sqlType == Types.NUMERIC || sqlType == Types.DECIMAL)
        {
            return value.toString();
        }
        if (sqlType == Types.BINARY || sqlType == Types.VARBINARY || sqlType == Types.LONGVARBINARY
                || sqlType == Types.BLOB)
        {
            byte[] bytes = rs.getBytes(columnIndex);
            return bytes == null ? "NULL" : "X'" + toHex(bytes) + "'";
        }
        return "'" + escapeSqlString(String.valueOf(value)) + "'";
    }

    static String escapeSqlString(String value)
    {
        return value.replace("\\", "\\\\").replace("'", "''");
    }

    static String quoteIdentifier(String identifier, Dialect dialect)
    {
        String quote = "`";
        return quote + identifier.replace(quote, quote + quote) + quote;
    }

    static String resolveH2Schema(Connection connection) throws SQLException
    {
        try (Statement statement = connection.createStatement();
                ResultSet rs = statement.executeQuery("SELECT SCHEMA()"))
        {
            if (rs.next())
            {
                String schema = rs.getString(1);
                if (schema != null && !schema.isBlank())
                {
                    return schema;
                }
            }
        }
        return "PUBLIC";
    }

    private static boolean isCommentOnly(String sql)
    {
        String trimmed = sql.trim();
        return trimmed.startsWith("--") || trimmed.startsWith("/*");
    }

    private static String toHex(byte[] bytes)
    {
        StringBuilder hex = new StringBuilder(bytes.length * 2);
        for (byte value : bytes)
        {
            hex.append(String.format(Locale.ROOT, "%02X", value));
        }
        return hex.toString();
    }
}
