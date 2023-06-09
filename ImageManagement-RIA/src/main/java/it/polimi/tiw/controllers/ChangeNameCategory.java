package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.polimi.tiw.dao.CategoryDAO;

@WebServlet("/ChangeNameCategory")
@MultipartConfig
public class ChangeNameCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public ChangeNameCategory() {
		super();
	}

	public void init() throws ServletException {
		try {
			ServletContext context = getServletContext();
			String driver = context.getInitParameter("dbDriver");
			String url = context.getInitParameter("dbUrl");
			String user = context.getInitParameter("dbUser");
			String password = context.getInitParameter("dbPassword");
			Class.forName(driver);
			connection = DriverManager.getConnection(url, user, password);
		} catch (ClassNotFoundException e) {
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			throw new UnavailableException("Couldn't get db connection");
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {

		String idStr = request.getParameter("id");
		String name = request.getParameter("name");
		Boolean bad_request = false;
		int id = -1;

		CategoryDAO catDAO = new CategoryDAO(connection);

		if (idStr == null || idStr.isEmpty() || name == null || name.isEmpty()) {
			bad_request = true;
		}

		if (!bad_request) {
			try {
				id = Integer.parseInt(idStr);
			} catch (NumberFormatException e) {
				bad_request = true;
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Error parameters");
			}
		} else {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("Error during changing name");
		}

		if (!bad_request) {
			// Check if name is valid and if specific category exists:
			if (catDAO.isValidName(name) && catDAO.validCategory(id)) {
				// Change name now:
				try {
					catDAO.changeName(id, name);
				} catch (SQLException e) {
					bad_request = true;
					response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
					response.getWriter().println("Server error during changing name");
				}
			} else {
				bad_request = true;
				response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				response.getWriter().println("Selected category doesn't exist");
			}

		}

		if (!bad_request) {
			response.setStatus(HttpServletResponse.SC_OK);
			response.getWriter().println("Name changed");
		}

	}

	public void destroy() {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException sqle) {
		}
	}

}
