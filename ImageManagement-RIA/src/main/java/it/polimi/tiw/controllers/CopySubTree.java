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

@WebServlet("/CopySubTree")
@MultipartConfig
public class CopySubTree extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;

	public CopySubTree() {
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
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doPost(request, response);
	}
	
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String[] s = request.getParameterValues("idSource");
		String[] d = request.getParameterValues("idDestination");
		
		int[] src = null;
		int[] dst = null;
		int len = -1;
		
		Boolean bad_req = false;
		
		CategoryDAO cService = new CategoryDAO(connection);
		
		if(s == null || s.length == 0 || d == null || d.length == 0 || s.length != d.length)
			bad_req = true;
		else {
			src = new int[s.length];
			dst = new int[d.length];
			len = s.length;
		}
		
		if(!bad_req) {
			for(int i=0; i<len; i++) {
				try {
					src[i] = Integer.parseInt(s[i]);
					dst[i] = Integer.parseInt(d[i]);
				} catch (NumberFormatException e) {
					bad_req = true;
				}
			}
		}
		
		if(!bad_req) {
			try {
				cService.copySubTree(src, dst);
				response.setStatus(HttpServletResponse.SC_OK);
				return;
			} catch (SQLException e) {
				bad_req = true;
			}
		}
		
		if(bad_req) {
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			response.getWriter().println("An error occurred while copying the selected category subtrees");
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
