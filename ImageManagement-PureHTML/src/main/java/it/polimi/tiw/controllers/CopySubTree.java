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

/**
 * Servlet implementation class CopySubTree
 */
@WebServlet("/CopySubTree")
public class CopySubTree extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Connection connection = null;
	private TemplateEngine templateEngine;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
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
		
		ServletContext servletContext = getServletContext();
		ServletContextTemplateResolver templateResolver = new ServletContextTemplateResolver(servletContext);
		templateResolver.setTemplateMode(TemplateMode.HTML);
		this.templateEngine = new TemplateEngine();
		this.templateEngine.setTemplateResolver(templateResolver);
		templateResolver.setSuffix(".html");
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		//response.getWriter().append("Served at: ").append(request.getContextPath());
		
		String idSource = request.getParameter("idSource");
		String idDestination = request.getParameter("idDestination");
		int source, destination;
		CategoryDAO cService = new CategoryDAO(connection);
		
		try
		{
			source = Integer.parseInt(idSource);
			destination = Integer.parseInt(idDestination);
		}
		catch(NumberFormatException e)
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you must insert an Integer as id");
			return;
		}
		
		//Check if id_soruce and id_destination are valid
		if(cService.validCategory(source) && (cService.validCategory(destination) || destination == 0)) //destination = 0 if destination is root
		{
			Category cSource = cService.getSpecificCategory(source);
			
			if(cSource != null)
			{
				//Now get subParts of this category:
				try {
					cService.findSubparts(cSource, false, -1);
					
					//Now check if there is space under the destination:
					if(cService.isThereSpace(destination)!=-1)
					{
						//Now check if id destination is not an id of a category in this subTree:
						if(cService.checkIdDestination(cSource, destination))
						{
							//Now copy, insert into the DB the new nodes:
							try
							{
								cService.copySubTree(cSource, destination);
								
							}catch(SQLException e)
							{
								response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error Copying subTree");
								return;
							}							
							
							//redirect to HomePage:
							List<Category> allCategories = null;
							List<Category> topCategories = null;
							String username = ((User)((HttpServletRequest)request).getSession().getAttribute("user")).getUser();
							
							try {
								allCategories = cService.findAllCategories();
								topCategories = cService.findTopCategoriesAndSubtrees(-1, false);
							} catch (Exception e) {
								e.printStackTrace();
								response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Error in retrieving products from the database");
								return;
							}
							
							// Redirect to the Home page and add categories to the parameters
							String path = "/WEB-INF/home.html";
							ServletContext servletContext = getServletContext();
							final WebContext ctx = new WebContext(request, response, servletContext, request.getLocale());
							ctx.setVariable("allCategories", allCategories);
							ctx.setVariable("topCategories", topCategories);
							ctx.setVariable("username", username);
							ctx.setVariable("showCopy", true); // show 'copy' button beside each category 
							templateEngine.process(path, ctx, response.getWriter());
							
						}
						else
						{
							response.sendError(HttpServletResponse.SC_BAD_REQUEST, "you can't copy this subTree under a Categorty inside the subTree itself");
							return;
						}
					}
					else
					{
						response.sendError(HttpServletResponse.SC_BAD_REQUEST, "there is no space under this category");
						return;
					}
					
				} catch (SQLException e) {
					response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error during operation");
					return;
				}
			}
			else
			{
				response.sendError(HttpServletResponse.SC_BAD_REQUEST, " A category does not exist in the database");
				return;
			}
			
		}
		else
		{
			response.sendError(HttpServletResponse.SC_BAD_REQUEST, " A category does not exist in the database");
			return;
		}
		
		
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
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
