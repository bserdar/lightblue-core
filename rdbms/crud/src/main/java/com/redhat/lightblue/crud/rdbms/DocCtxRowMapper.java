package com.redhat.lightblue.crud.rdbms;

import com.redhat.lightblue.common.rdbms.RowMapper;
import com.redhat.lightblue.crud.DocCtx;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by lcestari on 6/10/14.
 */
public class DocCtxRowMapper implements RowMapper<DocCtx> {
    @Override
    public DocCtx map(ResultSet resultSet) throws SQLException {
        return null;
    }
}
