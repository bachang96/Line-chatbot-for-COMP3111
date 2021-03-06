package com.example.bot.spring;

import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.*;
import java.net.URISyntaxException;
import java.net.URI;

@Slf4j
public class SQLDatabaseEngine extends DatabaseEngine {
	@Override
	String search(String text) throws Exception {
		Connection connection = null;
		PreparedStatement pstmtUpdate = null;
		PreparedStatement pstmtQuery = null;
		ResultSet rs = null;
		String result = null;

		try {
			// Establish connection
			connection = getConnection();

			// Prepare update statement
			pstmtUpdate = connection.prepareStatement(
				"UPDATE automatedreply " +
				"SET hits = hits+1 " +
				"WHERE ? LIKE concat('%', LOWER(keyword), '%')"
			);
			pstmtUpdate.setString(1, text.toLowerCase());

			// Execute update
			pstmtUpdate.executeUpdate();

			// Prepare query statement
			pstmtQuery = connection.prepareStatement(
				"SELECT response, hits FROM automatedreply " +
				"WHERE ? LIKE concat('%', LOWER(keyword), '%')"
			);
			pstmtQuery.setString(1, text.toLowerCase());

			// Execute query
			rs = pstmtQuery.executeQuery();

			// Obtain results
			while(result == null && rs.next()) {
				result = rs.getString(1) + " " + String.valueOf(rs.getInt(2));
			}
		} catch (SQLException e) {
			log.info("Exception while connecting to database: {}", e.toString());
		} finally {
			// Close connection
			try {
				if (rs != null) {rs.close();}
				if (pstmtQuery != null) {pstmtQuery.close();}
				if (pstmtUpdate != null) {pstmtUpdate.close();}
				if (connection != null) {connection.close();}
			} catch (SQLException ex) {
				log.info("Exception while closing connection to database: {}", ex.toString());
			}
		}
		if (result != null)
			return result;
		throw new Exception("NOT FOUND");
	}
	
	
	private Connection getConnection() throws URISyntaxException, SQLException {
		Connection connection;
		URI dbUri = new URI(System.getenv("DATABASE_URL"));

		String username = dbUri.getUserInfo().split(":")[0];
		String password = dbUri.getUserInfo().split(":")[1];
		String dbUrl = "jdbc:postgresql://" + dbUri.getHost() + ':' + dbUri.getPort() + dbUri.getPath() +  "?ssl=true&sslfactory=org.postgresql.ssl.NonValidatingFactory";

		log.info("Username: {} Password: {}", username, password);
		log.info ("dbUrl: {}", dbUrl);
		
		connection = DriverManager.getConnection(dbUrl, username, password);

		return connection;
	}

}
