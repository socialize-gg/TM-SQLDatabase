package net.thenova.titan.module.sqldatabase.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface SQLConsumer {

    /**
     * Consumer<ResultSet> replacement
     *
     * @param res ResultSet passed
     * @throws SQLException SQLException for handling failure within the accept
     */
    void accept(final ResultSet res) throws SQLException;
}
