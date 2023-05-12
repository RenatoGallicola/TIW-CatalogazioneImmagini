package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import it.polimi.tiw.beans.User;

public class UserDAO {

	private Connection con;

	public UserDAO(Connection connection) {
		this.con = connection;
	}

	public User checkCredentials(String usrn, String pwd) {
		String query = "SELECT  username, password FROM user  WHERE username = ? AND password =?";
		PreparedStatement pstatement = null;
		try {
			pstatement = con.prepareStatement(query);
			pstatement.setString(1, usrn);
			pstatement.setString(2, pwd);
			ResultSet result = null;
			try {
				result = pstatement.executeQuery();
				if (!result.isBeforeFirst()) // no results, credentials check failed
					return null;
				else {
					result.next();
					User user = new User();
					user.setPassword(result.getString("password"));
					user.setUser(result.getString("username"));
					return user;
				}
			} catch (SQLException er) {
				// ...
				return null;
			} finally {
				try {
					result.close();
				} catch (Exception er2) {
					// ...
				}
			}
		} catch (SQLException ep) {
			// ...
			return null;
		} finally {
			try {
				pstatement.close();
			} catch (Exception ep2) {
				// ...
			}
		}
	}
}
