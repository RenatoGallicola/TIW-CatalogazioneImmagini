package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import it.polimi.tiw.dao.CategoryDAO;

@WebServlet("/CreateCategory")
public class CreateCategory extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public CreateCategory() {
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
			e.printStackTrace();
			throw new UnavailableException("Can't load database driver");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new UnavailableException("Couldn't get db connection");
		}
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String name = null;
		int f_id = -1;
		boolean badRequest = false;

		try {

			name = request.getParameter("name");
			f_id = Integer.parseInt(request.getParameter("categoryId"));

			if (name.isEmpty() || f_id < 0) {
				badRequest = true;
			}

		} catch (NullPointerException | NumberFormatException e) {
			badRequest = true;
		}

		if (badRequest) {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing or incorrect parameters");
			return;
		}

		CategoryDAO cService = new CategoryDAO(connection);

		try {
			cService.insertCategory(name, f_id);
		} catch (SQLException e) {
			response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in creating the category in the database");
			return;
		}

		String ctxpath = getServletContext().getContextPath();
		String path = ctxpath + "/GoToHomePage";
		response.sendRedirect(path);
	}

	@Override
	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {

			}
		}
	}
}
