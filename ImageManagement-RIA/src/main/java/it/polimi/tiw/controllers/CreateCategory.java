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

import com.google.gson.Gson;

import it.polimi.tiw.dao.CategoryDAO;

@WebServlet("/CreateCategory")
@MultipartConfig
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
		Boolean badRequest = false, name_error = false, category_error = false;
		String error_message = null;
		
		try {
			name = request.getParameter("nameForm");
			if(name.isEmpty() || name.isBlank()) {
				badRequest = true;
				name_error = true;
				error_message = "No name entered for the new category";
			}				
		} catch (NullPointerException e) {
			badRequest = true;
			name_error = true;
			error_message = "No name entered for the new category";
		}
		
		if (!badRequest) {
			try {
				f_id = Integer.parseInt(request.getParameter("categoryIdForm"));
				if (f_id < 0) {
					badRequest = true;
					category_error = true;
					error_message = "Invalid parent category";
				}
			} catch (NullPointerException | NumberFormatException e) {
				badRequest = true;
				category_error = true;
				error_message = "Parent category either invalid or not entered";
			}
		}

		if(!badRequest) {
			CategoryDAO cService = new CategoryDAO(connection);
			try {
				cService.insertCategory(name, f_id);
			} catch (SQLException e) {
				badRequest = true;
				name_error = true;
				category_error = true;
				error_message = "Either entered name is in an invalid format or the selected parent is unavailable";
			}
		}
		
		if(badRequest) {
			String[] res = new String[3]; 
			res[0] = error_message;
			res[1] = name_error.toString();
			res[2] = category_error.toString();
			
			String json = new Gson().toJson(res);
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.setContentType("application/json");
			response.setCharacterEncoding("UTF-8");
			response.getWriter().write(json);
		} else {
			response.setStatus(HttpServletResponse.SC_OK);
		}
	}

	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e) {

			}
		}
	}

}
