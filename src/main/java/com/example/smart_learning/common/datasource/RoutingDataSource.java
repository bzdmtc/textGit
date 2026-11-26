package com.example.smart_learning.common.datasource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final Logger log = LoggerFactory.getLogger(RoutingDataSource.class);

    @Override
    protected Object determineCurrentLookupKey() {
        Route forced = DataSourceContextHolder.current();
        if (forced != null) {
            return forced;
        }
        // LazyConnectionDataSourceProxy delays physical connection acquisition until the
        // transaction is active, so the readOnly flag is reliable here.
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {
            return Route.READ;
        }
        return Route.MASTER;
    }

    @Override
    public Connection getConnection() throws SQLException {
        try {
            return super.getConnection();
        } catch (SQLException ex) {
            return fallbackToMaster(ex);
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {
        try {
            return super.getConnection(username, password);
        } catch (SQLException ex) {
            return fallbackToMaster(ex);
        }
    }

    private Connection fallbackToMaster(SQLException ex) throws SQLException {
        if (Route.READ.equals(determineCurrentLookupKey())) {
            DataSource master = getResolvedDefaultDataSource();
            if (master != null) {
                log.warn("Read datasource unavailable, falling back to master: {}", ex.getMessage());
                return master.getConnection();
            }
        }
        throw ex;
    }
}
