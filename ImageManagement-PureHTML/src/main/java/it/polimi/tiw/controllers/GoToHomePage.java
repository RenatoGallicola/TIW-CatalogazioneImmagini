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
import javax.servlet.http.HttpSession;

import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.WebContext;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ServletContextTemplateResolver;
import it.polimi.tiw.beans.Category;
import it.polimi.tiw.dao.CategoryDAO;
import it.polimi.tiw.beans.User;

@WebServlet("/GoToHomePage")
public class GoToHomePage extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private TemplateEngine templateEngine;
	private Connection connection = null;
       
    public GoToHomePage() {
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
		List<Category> allCategories = null;
		List<Category> topCategories = null;
		String error_message = null;
		Boolean error = false;
		
		CategoryDAO cService = new CategoryDAO(connection);
		
		try {
			allCategories = cService.findAllCategories();
		} catch (SQLException e1) {
			error = true;
			error_message = "An error occurred while retrieving the categories from the database";
		}
		if(!error) {
			try {
				topCategories = cService.findTopCategoriesAndSubtrees(-1, false);
			} catch (SQLException e2) {
				error = true;
				error_message = "An error occurred while building the category tree";
			}
		}
		
		if(error) {
			request.removeAttribute("error_message");
			request.removeAttribute("name_error");
			request.removeAttribute("category_error");
			
			String path = "/GoToErrorPage";	
			request.setAttribute("error", error_message);
			RequestDispatcher dispatcher = request.getRequestDispatcher(path);
			dispatcher.forward(request, response);
			return;
		}
		
		String username = ((User)((HttpServletRequest)request).getSession().getAttribute("user")).getUser();
		
		// Redirect to the Home page and add categories to the parameters
		String path = "/WEB-INF/home.html";
		ServletContext servletContext = getServletContext();
		final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
		ctx.setVariable("allCategories", allCategories);
		ctx.setVariable("topCategories", topCategories);
		ctx.setVariable("username", username);
		ctx.setVariable("showCopy", true); // show 'copy' button beside each category
		ctx.setVariable("showForm", true); // enable form
		
		if(request.getAttribute("error_message") != null)
			ctx.setVariable("error_message", request.getAttribute("error_message"));
		else
			ctx.setVariable("error_message", null);
		
		if(request.getAttribute("name_error") != null)
			ctx.setVariable("name_error", request.getAttribute("name_error"));
		else
			ctx.setVariable("name_error", false);
		
		if(request.getAttribute("category_error") != null)
			ctx.setVariable("category_error", request.getAttribute("category_error"));
		else
			ctx.setVariable("category_error", false);
		
		templateEngine.process(path, ctx, response.getWriter());	
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		doGet(request, response);
	}
	
	@Override
	public void destroy() {
		if (connection != null) {
			try {
				connection.close();
			} catch (SQLException e){
				
			}
		}
	}
}
