package net.user1.union.example.datasource;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.apache.log4j.Logger;
import net.user1.union.api.Account;
import net.user1.union.api.Datasource;
import net.user1.union.api.Room;
import net.user1.union.api.Server;
import net.user1.union.core.attribute.Attribute;
import net.user1.union.core.context.DatasourceContext;
import net.user1.union.core.exception.DatasourceException;

/**
 * Example of a Datasource that stores "score" and "title" attribute for 
 * Accounts backed by a MySQL database. It particularly allows for non-String data to be 
 * more easily stored and queried. 
 * 
 * This example is meant to illustrate the Datasource methods that should be implemented and 
 * does not focus on the database connectivity and should not be considered a reference
 * implementation of MySql database connectivity.
 * 
 * The database tables are assumed to exist and are not created in the example.
 *
 */
public class MySqlAccountDatasource implements Datasource {
    private static Logger log = Logger.getLogger(MySqlAccountDatasource.class); 
    private String dbURL;
    private String dbUsername;
    private String dbPassword;
    
    public boolean init(DatasourceContext ctx) {
        // load the database details which need to be provided in the union.xml datasource
        // declaration
        // we are just checking for the presence of an attribute but a more detailed 
        // implementation could do additional checking such as ensuring the attribute isn't 
        // just empty spaces or meets a particular format
        if ((dbURL = getAttribute(ctx, "dbURL")) == null) {
            log.fatal("Datasource MySqlAccountDatasource requires attribute [dbURL].");
            return false;
        }
        if ((dbUsername = getAttribute(ctx, "dbUsername")) == null) {
            log.fatal("Datasource MySqlAccountDatasource requires attribute [dbUsername].");
            return false;
        }
        if ((dbPassword = getAttribute(ctx, "dbPassword")) == null) {
            log.fatal("Datasource MySqlAccountDatasource requires attribute [dbPassword].");
            return false;
        }

        // load the driver class
        try {
            Class.forName("com.mysql.jdbc.Driver").newInstance();
        } catch(ClassNotFoundException e) {
            // could not load the driver class
            log.fatal("Could not load mysql driver.", e);
            return false;          
        } catch (InstantiationException e) {
            // could not load the driver class
            log.fatal("Could not load mysql driver.", e);
            return false; 
        } catch (IllegalAccessException e) {
            // could not load the driver class
            log.fatal("Could not load mysql driver.", e);
            return false; 
        }
        
        // everything OK
        return true;
    }
    
    /**
     * Return a datasource attribute defined in union.xml.
     */
    private String getAttribute(DatasourceContext ctx, String name) {
        Object attr = ctx.getAttributes().get(name);
        if (attr != null) {
            return attr.toString();
        } else {
            return null;
        }
    }
    
    /**
     * Return a connection to the MySql database. A more efficient implementation would use
     * a connection pool.
     */
    private Connection getConnection()
    throws SQLException {
        return DriverManager.getConnection(dbURL, dbUsername, dbPassword);
    }
    
    /**
     * Close the given resources.
     */
    private void close(Connection con, PreparedStatement ps, ResultSet rs) {
        if (con != null) {
            try {
                con.close();
            } catch (SQLException e) {
                log.error("Could not close connection.", e);
            }
        }
        if (ps != null) {
            try {
                ps.close();
            } catch (SQLException e) {
                log.error("Could not close prepared statement.", e);
            }
        }
        if (rs != null) {
            try {
                rs.close();
            } catch (SQLException e) {
                log.error("Could not close result set.", e);
            }
        }
    }

    public void loadAccountGlobalAttributes(Account account)
    throws DatasourceException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {
            con = getConnection();
            
            // scores
            ps = con.prepareStatement("SELECT score FROM scores " + 
                    "WHERE user = ?");
            ps.setString(1, account.getUserID());
            rs = ps.executeQuery();
            if (rs.next()) {
                try {
                    account.setAttribute("score", new Integer(rs.getInt("score")), Attribute.SCOPE_GLOBAL, 
                            Attribute.FLAG_SERVER_ONLY);
                } catch (Exception e) {
                    throw new DatasourceException(e);
                } 
            } 
            close(null, ps, rs);
            
            // title
            ps = con.prepareStatement("SELECT title FROM titles " + 
            "WHERE user = ?");
            ps.setString(1, account.getUserID());
            rs = ps.executeQuery();
            if (rs.next()) {
                try {
                    account.setAttribute("title", rs.getString("title"), Attribute.SCOPE_GLOBAL, 
                            Attribute.FLAG_SERVER_ONLY);
                } catch (Exception e) {
                    throw new DatasourceException(e);
                } 
            } 
        } catch (SQLException e) {
            throw new DatasourceException(e);
        } finally {
            close(con, ps, rs);
        }   
    }
    
    public void saveAccountAttribute(Account account, Attribute attr)
    throws DatasourceException {
        Connection con = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        
        try {                       
            // set up prepared statement according to the attribute
            if ("score".equals(attr.getName())) {
                con = getConnection();
                ps = con.prepareStatement("INSERT INTO scores (user, score) VALUES " + 
                        "(?, ?) ON DUPLICATE KEY UPDATE score = ?");
                ps.setString(1, account.getUserID());
                ps.setInt(2, (Integer)attr.getValue());
                ps.setInt(3, (Integer)attr.getValue());
                ps.executeUpdate();
            } else if ("title".equals(attr.getName())) {
                con = getConnection();
                ps = con.prepareStatement("INSERT INTO titles (user, title) VALUES " + 
                "(?, ?) ON DUPLICATE KEY UPDATE title = ?");
                ps.setString(1, account.getUserID());
                ps.setString(2, attr.getValue().toString());
                ps.setString(3, attr.getValue().toString());
                ps.executeUpdate();
            } else {
                // datasource entry in union.xml not configured properly 
                throw new DatasourceException("Datasource method called for an attribute [" +
                        attr.getName() + "] it is not able to handle. Check union.xml configuration.");
            }
        } catch (SQLException e) {
            throw new DatasourceException(e);
        } finally {
            close(con, ps, rs);
        } 
    }
    
    public void removeAccountAttribute(Account account, Attribute attr)
    throws DatasourceException {
        Connection con = null;
        PreparedStatement ps = null;
        
        try {
            con = getConnection();
            
            // set up prepared statement according to the attribute
            if ("score".equals(attr.getName())) {
                ps = con.prepareStatement("REMOVE FROM scores " + 
                        "WHERE user = ?");
            } else if ("title".equals(attr.getName())) {
                ps = con.prepareStatement("REMOVE FROM titles " + 
                "WHERE user = ?");
            } else {
                // datasource entry in union.xml not configured properly 
                throw new DatasourceException("Datasource method called for an attribute it " + 
                        "is not able to handle. Check union.xml configuration.");
            }
            ps.setString(1, account.getUserID());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatasourceException(e);
        } finally {
            close(con, ps, null);
        } 
    }
    
    public boolean containsAccount(String userID)
    throws DatasourceException {   
        // this datasource does not handle account
        return false;
    }

    public String createAccount(String userID, String password)
    throws DatasourceException {   
        // this datasource does not handle account
        return null;
    }
    
    public String saveAccount(Account account)
    throws DatasourceException {
        // this datasource does not handle account
        return null;
    }

    public List<String> getAccounts()
    throws DatasourceException {
        // this datasource does not handle account
        return null;
    }

    public String getPassword(String userID)
    throws DatasourceException {      
        // this datasource does not handle account
        return null;
    }

    public void loadAccount(Account account)
    throws DatasourceException {
        // this datasource does not handle account
    }
    
    public String removeAccount(String userID)
    throws DatasourceException {
        // this datasource does not handle account        
        return null;
    }
    
    public void loadAccountRoomAttributes(Account account, String roomID)
    throws DatasourceException {
        // this datasource does not handle room scoped attributes
    }

    public void loadAllAccountAttributes(Account account)
    throws DatasourceException {
        // this datasource only stores two global attributes (score and title)
        loadAccountGlobalAttributes(account);
    }

    public void loadRoomAttributes(Room room)
    throws DatasourceException {
        // this datasource does not handle room attributes
    }

    public void loadServerAttributes(Server server)
    throws DatasourceException {
        // this datasource does not handle server attributes
    }

    public void removeRoomAttribute(Room room, Attribute attr)
    throws DatasourceException {
        // this datasource does not handle room attributes
    }

    public void removeServerAttribute(Attribute attr)
    throws DatasourceException {
        // this datasource does not handle server attributes
    }
    
    public void saveRoomAttribute(Room room, Attribute attr)
    throws DatasourceException {
        // this datasource does not handle room attributes
    }

    public void saveServerAttribute(Attribute attr)
    throws DatasourceException {
        // this datasource does not handle server attributes
    }

 
    public void shutdown() {
        // since we create a new connection for each call rather than using resource such as 
        // a pool there is nothing to clean up
    }
}
