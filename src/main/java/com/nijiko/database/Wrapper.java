package com.nijiko.database;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Simple wrapper for database abstraction.
 *
 * @author Nijiko
 */
public class Wrapper {
    private static final Logger log = Logger.getLogger("Minecraft");

    /**
     * Database Types
     */
    public enum Type {
	SQLITE,
	MYSQL
    };

    /**
     * Fetch type from string.
     * 
     * @param type
     * @return
     */
    public static Type getType(String type) {
	for(Type dbType : Type.values()) {
	    if(dbType.toString().equalsIgnoreCase(type)) {
		return dbType;
	    }
	}

	return Type.SQLITE;
    }

    /*
     * Database Settings
     */
    public Type database = null;

    /*
     * Database Connection Settings
     */
    private String db;
    private String user;
    private String pass;

    /*
     * Database Memory
     */
    private Connection connection = null;
    private PreparedStatement Statement = null;
    private ResultSet ResultSet = null;

    /**
     * Create a new instance of the wrapper for usage.
     *
     * @param database
     * @param db
     * @param user
     * @param pass
     */
    public Wrapper(Type database, String db, String user, String pass) {
	this.database = database;
	this.db = db;
	this.user = user;
	this.pass = pass;
    }

    /**
     * Initialize the wrapper
     *
     * @return Connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    public void initialize() {
	try {
	    this.connection();
	} catch (SQLException ex) {
	    log.severe("["+this.database.toString()+" Database] Failed to connect: " + ex);
	} catch (ClassNotFoundException e) {
	    log.severe("["+this.database.toString()+" Database] Connector not found: " + e);
	}
    }

    /**
     * Connect to the database, return connection.
     *
     * @return Connection
     * @throws ClassNotFoundException
     * @throws SQLException
     */
    private Connection connection() throws ClassNotFoundException, SQLException {
	if (this.database.equals(database.SQLITE)) {
	    Class.forName("org.sqlite.JDBC");
	    this.connection = DriverManager.getConnection(this.db);
	} else {
	    Class.forName("com.mysql.jdbc.Driver");
	    this.connection = DriverManager.getConnection(this.db, this.user, this.pass);
	}

	return this.connection;
    }

    /**
     * Check to see if table exists.
     *
     * @param table
     * @return boolean
     */
    public boolean checkTable(String table) {
	try {
	    DatabaseMetaData dbm = this.connection.getMetaData();
	    this.ResultSet = dbm.getTables(null, null, table, null);
	    return this.ResultSet.next();
	} catch (SQLException ex) {
	    log.severe("["+this.database.toString()+" Database] Table check failed: " + ex);
	} finally {
	    this.close();
	}

	return false;
    }

    /**
     * Execute pure SQL String.
     *
     * @param query
     * @return
     */
    public ResultSet executeQuery(String query) {
	try {
	    this.Statement = this.connection.prepareStatement(query);

	    return this.Statement.executeQuery();
	} catch (SQLException ex) {
	    log.severe("["+this.database.toString()+" Database] Could not execute query: " + ex);
	}

	return null;
    }

    /**
     * Execute Query with variables.
     *
     * @param query
     * @param variables
     * @return
     */
    public ResultSet executeQuery(String query, Object[] variables) {
	try {
	    this.Statement = this.connection.prepareStatement(query);
	    int i = 1;
	    for (Object obj : variables) {
		this.Statement.setObject(i, obj);
		i++;
	    }
	    return this.Statement.executeQuery();
	} catch (SQLException ex) {
	    log.severe("["+this.database.toString()+" Database] Could not execute query: " + ex);
	}

	return null;
    }

    /**
     * Execute pure SQL String.
     *
     * @param query
     * @return
     */
    public int executeUpdate(String query) {
	try {
	    this.Statement = this.connection.prepareStatement(query);
	    return this.Statement.executeUpdate();
	} catch (SQLException ex) {
	    log.severe("["+this.database.toString()+" Database] Could not execute query: " + ex);
	}

	return 0;
    }

    /**
     * Execute Query with variables.
     *
     * @param query
     * @param variables
     * @return
     */
    public int executeUpdate(String query, Object[] variables) {
	try {
	    this.Statement = this.connection.prepareStatement(query);
	    int i = 1;

	    for (Object obj : variables) {
		this.Statement.setObject(i, obj);
		i++;
	    }

	    return this.Statement.executeUpdate();
	} catch (SQLException ex) {
	    log.severe("["+this.database.toString()+" Database] Could not execute query: " + ex);
	}

	return 0;
    }

    public void close() {
	try {
	    if(this.Statement != null) {
		this.Statement.close();
	    }

	    if(this.ResultSet != null) {
		this.ResultSet.close();
	    }

	    if(this.connection != null) {
		this.connection.close();
	    }

	} catch (SQLException ex) {
	    log.severe("["+this.database.toString()+" Database] Failed to close connection: " + ex);

	    // Close anyway.
	    this.connection = null;
	    this.Statement = null;
	    this.ResultSet = null;
	}
    }

    protected void finalize() {
	close();
    }
}
