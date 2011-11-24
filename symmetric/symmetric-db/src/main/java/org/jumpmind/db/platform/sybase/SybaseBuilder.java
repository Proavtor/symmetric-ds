package org.jumpmind.db.platform.sybase;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jumpmind.db.IDatabasePlatform;
import org.jumpmind.db.alter.AddColumnChange;
import org.jumpmind.db.alter.AddPrimaryKeyChange;
import org.jumpmind.db.alter.ColumnAutoIncrementChange;
import org.jumpmind.db.alter.ColumnChange;
import org.jumpmind.db.alter.ColumnDefaultValueChange;
import org.jumpmind.db.alter.IModelChange;
import org.jumpmind.db.alter.PrimaryKeyChange;
import org.jumpmind.db.alter.RemoveColumnChange;
import org.jumpmind.db.alter.RemovePrimaryKeyChange;
import org.jumpmind.db.alter.TableChange;
import org.jumpmind.db.model.Column;
import org.jumpmind.db.model.Database;
import org.jumpmind.db.model.ForeignKey;
import org.jumpmind.db.model.Index;
import org.jumpmind.db.model.Table;
import org.jumpmind.db.platform.AbstractDdlBuilder;
import org.jumpmind.db.util.Jdbc3Utils;
import org.jumpmind.util.Log;

/*
 * The SQL Builder for Sybase.
 */
public class SybaseBuilder extends AbstractDdlBuilder {

    public SybaseBuilder(Log log, IDatabasePlatform platform) {
        super(log, platform);
        addEscapedCharSequence("'", "''");
    }

    @Override
    public void createTable(Database database, Table table, StringBuilder ddl) {
        writeQuotationOnStatement(ddl);
        super.createTable(database, table, ddl);
    }

    @Override
    protected void writeColumn(Table table, Column column, StringBuilder ddl) {
        printIdentifier(getColumnName(column), ddl);
        ddl.append(" ");
        ddl.append(getSqlType(column));
        writeColumnDefaultValueStmt(table, column, ddl);
        // Sybase does not like NULL/NOT NULL and IDENTITY together
        if (column.isAutoIncrement()) {
            ddl.append(" ");
            writeColumnAutoIncrementStmt(table, column, ddl);
        } else {
            ddl.append(" ");
            if (column.isRequired()) {
                writeColumnNotNullableStmt(ddl);
            } else {
                // we'll write a NULL for all columns that are not required
                writeColumnNullableStmt(ddl);
            }
        }
    }

    @Override
    protected String getNativeDefaultValue(Column column) {
        if ((column.getTypeCode() == Types.BIT)
                || (Jdbc3Utils.supportsJava14JdbcTypes() && (column.getTypeCode() == Jdbc3Utils
                        .determineBooleanTypeCode()))) {
            return getDefaultValueHelper().convert(column.getDefaultValue(), column.getTypeCode(),
                    Types.SMALLINT).toString();
        } else {
            return super.getNativeDefaultValue(column);
        }
    }

    @Override
    public void dropTable(Table table, StringBuilder ddl) {
        writeQuotationOnStatement(ddl);
        ddl.append("IF EXISTS (SELECT 1 FROM sysobjects WHERE type = 'U' AND name = ");
        printAlwaysSingleQuotedIdentifier(getTableName(table), ddl);
        println(")", ddl);
        println("BEGIN", ddl);
        printIndent(ddl);
        ddl.append("DROP TABLE ");
        printlnIdentifier(getTableName(table), ddl);
        ddl.append("END");
        printEndOfStatement(ddl);
    }

    @Override
    protected void writeExternalForeignKeyDropStmt(Table table, ForeignKey foreignKey,
            StringBuilder ddl) {
        String constraintName = getForeignKeyName(table, foreignKey);

        ddl.append("IF EXISTS (SELECT 1 FROM sysobjects WHERE type = 'RI' AND name = ");
        printAlwaysSingleQuotedIdentifier(constraintName, ddl);
        println(")", ddl);
        printIndent(ddl);
        ddl.append("ALTER TABLE ");
        printIdentifier(getTableName(table), ddl);
        ddl.append(" DROP CONSTRAINT ");
        printIdentifier(constraintName, ddl);
        printEndOfStatement(ddl);
    }

    @Override
    public void writeExternalIndexDropStmt(Table table, Index index, StringBuilder ddl) {
        ddl.append("DROP INDEX ");
        printIdentifier(getTableName(table), ddl);
        ddl.append(".");
        printIdentifier(getIndexName(index), ddl);
        printEndOfStatement(ddl);
    }

    @Override
    public void dropExternalForeignKeys(Table table, StringBuilder ddl) {
        writeQuotationOnStatement(ddl);
        super.dropExternalForeignKeys(table, ddl);
    }

    @Override
    public String getSelectLastIdentityValues(Table table) {
        return "SELECT @@IDENTITY";
    }

    /*
     * Returns the SQL to enable identity override mode.
     * 
     * @param table The table to enable the mode for
     * 
     * @return The SQL
     */
    protected String getEnableIdentityOverrideSql(Table table) {
        StringBuffer result = new StringBuffer();

        result.append("SET IDENTITY_INSERT ");
        result.append(getDelimitedIdentifier(getTableName(table)));
        result.append(" ON");

        return result.toString();
    }

    /*
     * Returns the SQL to disable identity override mode.
     * 
     * @param table The table to disable the mode for
     * 
     * @return The SQL
     */
    protected String getDisableIdentityOverrideSql(Table table) {
        StringBuffer result = new StringBuffer();

        result.append("SET IDENTITY_INSERT ");
        result.append(getDelimitedIdentifier(getTableName(table)));
        result.append(" OFF");

        return result.toString();
    }

    /*
     * Returns the statement that turns on the ability to write delimited
     * identifiers.
     * 
     * @return The quotation-on statement
     */
    protected String getQuotationOnStatement() {
        if (platform.isDelimitedIdentifierModeOn()) {
            return "SET quoted_identifier on";
        } else {
            return "";
        }
    }

    /*
     * Writes the statement that turns on the ability to write delimited
     * identifiers.
     */
    private void writeQuotationOnStatement(StringBuilder ddl) {
        ddl.append(getQuotationOnStatement());
        printEndOfStatement(ddl);
    }

    /*
     * Prints the given identifier with enforced single quotes around it
     * regardless of whether delimited identifiers are turned on or not.
     * 
     * @param identifier The identifier
     */
    private void printAlwaysSingleQuotedIdentifier(String identifier, StringBuilder ddl) {
        ddl.append("'");
        ddl.append(identifier);
        ddl.append("'");
    }

    @Override
    public void writeCopyDataStatement(Table sourceTable, Table targetTable, StringBuilder ddl) {
        boolean hasIdentity = targetTable.getAutoIncrementColumns().length > 0;

        if (hasIdentity) {
            ddl.append("SET IDENTITY_INSERT ");
            printIdentifier(getTableName(targetTable), ddl);
            ddl.append(" ON");
            printEndOfStatement(ddl);
        }
        super.writeCopyDataStatement(sourceTable, targetTable, ddl);
        if (hasIdentity) {
            ddl.append("SET IDENTITY_INSERT ");
            printIdentifier(getTableName(targetTable), ddl);
            ddl.append(" OFF");
            printEndOfStatement(ddl);
        }
    }

    @Override
    protected void writeCastExpression(Column sourceColumn, Column targetColumn, StringBuilder ddl) {
        String sourceNativeType = getBareNativeType(sourceColumn);
        String targetNativeType = getBareNativeType(targetColumn);

        if (sourceNativeType.equals(targetNativeType)) {
            printIdentifier(getColumnName(sourceColumn), ddl);
        } else {
            ddl.append("CONVERT(");
            ddl.append(getNativeType(targetColumn));
            ddl.append(",");
            printIdentifier(getColumnName(sourceColumn), ddl);
            ddl.append(")");
        }
    }

    @Override
    protected void processChanges(Database currentModel, Database desiredModel,
            List<IModelChange> changes, StringBuilder ddl) {
        if (!changes.isEmpty()) {
            writeQuotationOnStatement(ddl);
        }
        super.processChanges(currentModel, desiredModel, changes, ddl);
    }

    @Override
    protected void processTableStructureChanges(Database currentModel, Database desiredModel,
            Table sourceTable, Table targetTable, List<TableChange> changes, StringBuilder ddl) {
        // First we drop primary keys as necessary
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();) {
            TableChange change = (TableChange) changeIt.next();

            if (change instanceof RemovePrimaryKeyChange) {
                processChange(currentModel, desiredModel, (RemovePrimaryKeyChange) change, ddl);
                changeIt.remove();
            } else if (change instanceof PrimaryKeyChange) {
                PrimaryKeyChange pkChange = (PrimaryKeyChange) change;
                RemovePrimaryKeyChange removePkChange = new RemovePrimaryKeyChange(
                        pkChange.getChangedTable(), pkChange.getOldPrimaryKeyColumns());

                processChange(currentModel, desiredModel, removePkChange, ddl);
            }
        }

        HashMap columnChanges = new HashMap();

        // Next we add/remove columns
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();) {
            TableChange change = (TableChange) changeIt.next();

            if (change instanceof AddColumnChange) {
                AddColumnChange addColumnChange = (AddColumnChange) change;

                // Sybase can only add not insert columns
                if (addColumnChange.isAtEnd()) {
                    processChange(currentModel, desiredModel, addColumnChange, ddl);
                    changeIt.remove();
                }
            } else if (change instanceof RemoveColumnChange) {
                processChange(currentModel, desiredModel, (RemoveColumnChange) change, ddl);
                changeIt.remove();
            } else if (change instanceof ColumnAutoIncrementChange) {
                // Sybase has no way of adding or removing an IDENTITY
                // constraint
                // Thus we have to rebuild the table anyway and can ignore all
                // the other
                // column changes
                columnChanges = null;
            } else if ((change instanceof ColumnChange) && (columnChanges != null)) {
                // we gather all changed columns because we can use the ALTER
                // TABLE ALTER COLUMN
                // statement for them
                ColumnChange columnChange = (ColumnChange) change;
                ArrayList changesPerColumn = (ArrayList) columnChanges.get(columnChange
                        .getChangedColumn());

                if (changesPerColumn == null) {
                    changesPerColumn = new ArrayList();
                    columnChanges.put(columnChange.getChangedColumn(), changesPerColumn);
                }
                changesPerColumn.add(change);
            }
        }
        if (columnChanges != null) {
            for (Iterator changesPerColumnIt = columnChanges.entrySet().iterator(); changesPerColumnIt
                    .hasNext();) {
                Map.Entry entry = (Map.Entry) changesPerColumnIt.next();
                Column sourceColumn = (Column) entry.getKey();
                ArrayList changesPerColumn = (ArrayList) entry.getValue();

                // Sybase does not like us to use the ALTER TABLE ALTER
                // statement if we don't actually
                // change the datatype or the required constraint but only the
                // default value
                // Thus, if we only have to change the default, we use a
                // different handler
                if ((changesPerColumn.size() == 1)
                        && (changesPerColumn.get(0) instanceof ColumnDefaultValueChange)) {
                    processChange(currentModel, desiredModel,
                            (ColumnDefaultValueChange) changesPerColumn.get(0), ddl);
                } else {
                    Column targetColumn = targetTable.findColumn(sourceColumn.getName(),
                            platform.isDelimitedIdentifierModeOn());

                    processColumnChange(sourceTable, targetTable, sourceColumn, targetColumn, ddl);
                }
                for (Iterator changeIt = changesPerColumn.iterator(); changeIt.hasNext();) {
                    ((ColumnChange) changeIt.next()).apply(currentModel,
                            platform.isDelimitedIdentifierModeOn());
                }
            }
        }
        // Finally we add primary keys
        for (Iterator changeIt = changes.iterator(); changeIt.hasNext();) {
            TableChange change = (TableChange) changeIt.next();

            if (change instanceof AddPrimaryKeyChange) {
                processChange(currentModel, desiredModel, (AddPrimaryKeyChange) change, ddl);
                changeIt.remove();
            } else if (change instanceof PrimaryKeyChange) {
                PrimaryKeyChange pkChange = (PrimaryKeyChange) change;
                AddPrimaryKeyChange addPkChange = new AddPrimaryKeyChange(
                        pkChange.getChangedTable(), pkChange.getNewPrimaryKeyColumns());

                processChange(currentModel, desiredModel, addPkChange, ddl);
                changeIt.remove();
            }
        }
    }

    /*
     * Processes the addition of a column to a table.
     */
    protected void processChange(Database currentModel, Database desiredModel,
            AddColumnChange change, StringBuilder ddl) {
        ddl.append("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()), ddl);
        printIndent(ddl);
        ddl.append("ADD ");
        writeColumn(change.getChangedTable(), change.getNewColumn(), ddl);
        printEndOfStatement(ddl);
        change.apply(currentModel, platform.isDelimitedIdentifierModeOn());
    }

    /*
     * Processes the removal of a column from a table.
     */
    protected void processChange(Database currentModel, Database desiredModel,
            RemoveColumnChange change, StringBuilder ddl) {
        ddl.append("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()), ddl);
        printIndent(ddl);
        ddl.append("DROP ");
        printIdentifier(getColumnName(change.getColumn()), ddl);
        printEndOfStatement(ddl);
        change.apply(currentModel, platform.isDelimitedIdentifierModeOn());
    }

    /*
     * Processes the removal of a primary key from a table.
     * 
     * @param currentModel The current database schema
     * 
     * @param desiredModel The desired database schema
     * 
     * @param change The change object
     */
    protected void processChange(Database currentModel, Database desiredModel,
            RemovePrimaryKeyChange change, StringBuilder ddl) {
        // TODO: this would be easier when named primary keys are supported
        // because then we can use ALTER TABLE DROP
        String tableName = getTableName(change.getChangedTable());
        String tableNameVar = "tn" + createUniqueIdentifier();
        String constraintNameVar = "cn" + createUniqueIdentifier();

        println("BEGIN", ddl);
        println("  DECLARE @" + tableNameVar + " nvarchar(60), @" + constraintNameVar
                + " nvarchar(60)", ddl);
        println("  WHILE EXISTS(SELECT sysindexes.name", ddl);
        println("                 FROM sysindexes, sysobjects", ddl);
        ddl.append("                 WHERE sysobjects.name = ");
        printAlwaysSingleQuotedIdentifier(tableName, ddl);
        println(" AND sysobjects.id = sysindexes.id AND (sysindexes.status & 2048) > 0)", ddl);
        println("  BEGIN", ddl);
        println("    SELECT @" + tableNameVar + " = sysobjects.name, @" + constraintNameVar
                + " = sysindexes.name", ddl);
        println("      FROM sysindexes, sysobjects", ddl);
        ddl.append("      WHERE sysobjects.name = ");
        printAlwaysSingleQuotedIdentifier(tableName, ddl);
        ddl.append(" AND sysobjects.id = sysindexes.id AND (sysindexes.status & 2048) > 0");
        println("    EXEC ('ALTER TABLE '+@" + tableNameVar + "+' DROP CONSTRAINT '+@"
                + constraintNameVar + ")", ddl);
        println("  END", ddl);
        ddl.append("END");
        printEndOfStatement(ddl);
        change.apply(currentModel, platform.isDelimitedIdentifierModeOn());
    }

    /*
     * Processes the change of the default value of a column. Note that this
     * method is only used if it is the only change to that column.
     */
    protected void processChange(Database currentModel, Database desiredModel,
            ColumnDefaultValueChange change, StringBuilder ddl) {
        ddl.append("ALTER TABLE ");
        printlnIdentifier(getTableName(change.getChangedTable()), ddl);
        printIndent(ddl);
        ddl.append("REPLACE ");
        printIdentifier(getColumnName(change.getChangedColumn()), ddl);

        Table curTable = currentModel.findTable(change.getChangedTable().getName(),
                platform.isDelimitedIdentifierModeOn());
        Column curColumn = curTable.findColumn(change.getChangedColumn().getName(),
                platform.isDelimitedIdentifierModeOn());

        ddl.append(" DEFAULT ");
        if (isValidDefaultValue(change.getNewDefaultValue(), curColumn.getTypeCode())) {
            printDefaultValue(change.getNewDefaultValue(), curColumn.getTypeCode(), ddl);
        } else {
            ddl.append("NULL");
        }
        printEndOfStatement(ddl);
        change.apply(currentModel, platform.isDelimitedIdentifierModeOn());
    }

    /*
     * Processes a change to a column.
     */
    protected void processColumnChange(Table sourceTable, Table targetTable, Column sourceColumn,
            Column targetColumn, StringBuilder ddl) {
        Object oldParsedDefault = sourceColumn.getParsedDefaultValue();
        Object newParsedDefault = targetColumn.getParsedDefaultValue();
        String newDefault = targetColumn.getDefaultValue();
        boolean defaultChanges = ((oldParsedDefault == null) && (newParsedDefault != null))
                || ((oldParsedDefault != null) && !oldParsedDefault.equals(newParsedDefault));

        // Sybase does not like it if there is a default spec in the ALTER TABLE
        // ALTER
        // statement; thus we have to change the default afterwards
        if (newDefault != null) {
            targetColumn.setDefaultValue(null);
        }
        if (defaultChanges) {
            // we're first removing the default as it might make problems when
            // the
            // datatype changes
            ddl.append("ALTER TABLE ");
            printlnIdentifier(getTableName(sourceTable), ddl);
            printIndent(ddl);
            ddl.append("REPLACE ");
            printIdentifier(getColumnName(sourceColumn), ddl);
            ddl.append(" DEFAULT NULL");
            printEndOfStatement(ddl);
        }
        ddl.append("ALTER TABLE ");
        printlnIdentifier(getTableName(sourceTable), ddl);
        printIndent(ddl);
        ddl.append("MODIFY ");
        writeColumn(sourceTable, targetColumn, ddl);
        printEndOfStatement(ddl);
        if (defaultChanges) {
            ddl.append("ALTER TABLE ");
            printlnIdentifier(getTableName(sourceTable), ddl);
            printIndent(ddl);
            ddl.append("REPLACE ");
            printIdentifier(getColumnName(sourceColumn), ddl);
            if (newDefault != null) {
                writeColumnDefaultValueStmt(sourceTable, targetColumn, ddl);
            } else {
                ddl.append(" DEFAULT NULL");
            }
            printEndOfStatement(ddl);
        }
    }
}
