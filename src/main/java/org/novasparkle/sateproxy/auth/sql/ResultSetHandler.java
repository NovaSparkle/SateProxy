package org.novasparkle.sateproxy.auth.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

@FunctionalInterface
public interface ResultSetHandler<R> {
    R handle(ResultSet resultSet) throws SQLException;
}
