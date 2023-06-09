package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.UnavailableException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;

import it.polimi.tiw.beans.Category;
import it.polimi.tiw.beans.User;
import it.polimi.tiw.dao.CategoryDAO;

@WebServlet("/SelectSubtreeToCopy")
public class SelectSubtreeToCopy extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;

	public SelectSubtreeToCopy() {
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

		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String id_source = request.getParameter("idSource");
		String error_message = null;
		Boolean bad_request = false, error = false;
		int int_source = -1;

		List<Category> allCategories = null;
		List<Category> topCategories = null;

		if (id_source == null)
			bad_request = true;
		else {
			try {
				int_source = Integer.parseInt(id_source);

				if (int_source <= 0)
					bad_request = true;
			} catch (NumberFormatException e) {
				bad_request = true;
			}
		}

		if (bad_request) {
			error_message = "The category id format is invalid";
			String path = "/GoToErrorPage";
			request.setAttribute("error", error_message);
			RequestDispatcher dispatcher = request.getRequestDispatcher(path);
			dispatcher.forward(request, response);
			return;
		}

		CategoryDAO cService = new CategoryDAO(connection);

		if (cService.validCategory(int_source)) {
			try {
				allCategories = cService.findAllCategories();
			} catch (SQLException e1) {
				error = true;
				error_message = "An error occurred while retrieving the categories from the database";
			}
			if(!error) {
				try {
					topCategories = cService.findTopCategoriesAndSubtrees(int_source, true);
				} catch (SQLException e2) {
					error = true;
					error_message = "An error occurred while searching for the categories to copy";
				}
			}
		} else {
			error = true;
			error_message = "The chosen category is non-existent";
		}
		
		if(error) {			
			String path = "/GoToErrorPage";
			request.setAttribute("error", error_message);
			RequestDispatcher dispatcher = request.getRequestDispatcher(path);
			dispatcher.forward(request, response);
			return;
		}

		String username = ((User) ((HttpServletRequest) request).getSession().getAttribute("user")).getUser();

		// Redirect to the HomePage and add categories to the parameters
		String path = "/WEB-INF/home.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("allCategories", allCategories);
		ctx.setVariable("topCategories", topCategories);
		ctx.setVariable("username", username);
		ctx.setVariable("showCopy", false); // show 'copy here' button beside certain categories only
		ctx.setVariable("idSource", int_source); // id of the subtree root to copy
		ctx.setVariable("showForm", false); // disable form
		templateEngine.process(path, ctx, response.getWriter());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
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
