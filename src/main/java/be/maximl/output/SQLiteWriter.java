/*-
 * #%L
 * SCIP: Single-cell image processing
 * %%
 * Copyright (C) 2020 Maxim Lippeveld
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */
package be.maximl.output;

import be.maximl.feature.FeatureVectorFactory;
import org.hibernate.type.descriptor.sql.JdbcTypeJavaClassMappings;
import org.scijava.app.StatusService;
import org.scijava.log.LogService;

import java.sql.*;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class SQLiteWriter extends FeatureVecWriter {

    private Connection connection;
    private PreparedStatement rowStatement;

    public SQLiteWriter(LogService log, StatusService statusService, CompletionService<FeatureVectorFactory.FeatureVector> completionService, String file) {
        super(log, statusService, completionService);

        connection = null;

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection("jdbc:sqlite:"+file);
            log.info("Opened database successfully @ " + connection.getMetaData().getURL());
        } catch ( Exception e ) {
            log.error( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
    }

    private void setupTable(FeatureVectorFactory.FeatureVector vec) throws SQLException {

        StringBuilder sql = new StringBuilder("CREATE TABLE ").append("data").append(" (");

        Iterator<Map.Entry<String, Object>> it = vec.getMap().entrySet().iterator();
        while(it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();

            sql.append(entry.getKey());
            sql.append(" ");

            if(entry.getKey().equals("meta:id")) {
                sql.append("PRIMARY KEY ");
            }

            int type = JdbcTypeJavaClassMappings.INSTANCE.determineJdbcTypeCodeForJavaClass(entry.getValue().getClass());
            sql.append(JDBCType.values()[type]);

            if (it.hasNext()) {
                sql.append(",");
            }
        }
        sql.append(")");

        Statement statement = connection.createStatement();
        statement.executeUpdate(sql.toString());
        statement.close();
    }

    private PreparedStatement prepareRowStatement(FeatureVectorFactory.FeatureVector vec) throws SQLException {
        StringBuilder sql = new StringBuilder("INSERT INTO ").append("data").append(" (");
        StringBuilder placeholders = new StringBuilder();

        for (Iterator<String> iter = vec.getMap().keySet().iterator(); iter.hasNext();) {
            sql.append(iter.next());
            placeholders.append("?");

            if (iter.hasNext()) {
                sql.append(",");
                placeholders.append(",");
            }
        }

        sql.append(") VALUES (").append(placeholders).append(")");
        return connection.prepareStatement(sql.toString());
    }

    @Override
    public void run() {

        try {
            try {
                Future<FeatureVectorFactory.FeatureVector> vec;
                int internalCounter = 0;
                int delta = 1000;
                while (!Thread.currentThread().isInterrupted()) {
                    synchronized (completionService) {
                        vec = completionService.take();
                        completionService.notify();
                    }

                    if (internalCounter == 0) {
                        // initialize SQLite table
                        setupTable(vec.get());
                        rowStatement = prepareRowStatement(vec.get());
                    }

                    rowStatement.clearParameters();
                    int i = 1;
                    for (Map.Entry<String, Object> entry: vec.get().getMap().entrySet()) {
                        rowStatement.setObject(i++, entry.getValue());
                    }
                    rowStatement.addBatch();
                    internalCounter++;

                    if (internalCounter % delta == 0) {
                        rowStatement.executeBatch();
                    }

                    synchronized (handleCount) {
                        int c = handleCount.incrementAndGet();
                        handleCount.notify();
                        if (c % 500 == 0) {
                            log.info("Submmited " + c + " vectors to database.");
                        }
                    }

                }

            } catch (ExecutionException e) {
                log.error("Exception while computing a feature vector.");
                e.printStackTrace();
            } catch (InterruptedException e) {
                log.error("Interrupted while waiting");
            } finally {
                log.info("Finalize writer");
                if (rowStatement != null)
                    rowStatement.executeBatch();
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}
