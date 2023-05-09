package it.polimi.tiw.controllers;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.List;

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
		Boolean bad_request = false;
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
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Parameter id with format number is required");
			return;
		}

		CategoryDAO cService = new CategoryDAO(connection);

		if (cService.validCategory(int_source)) {
			try {
				allCategories = cService.findAllCategories();
				topCategories = cService.findTopCategoriesAndSubtrees(int_source, true);
			} catch (Exception e) {
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
						"Error in deleting the product in the database");
				return;
			}
		} else {
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Category does not exist in the database");
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
		templateEngine.process(path, ctx, response.getWriter());
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}

}
