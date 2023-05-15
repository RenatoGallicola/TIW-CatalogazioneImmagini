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

@WebServlet("/CopySubTree")
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

		String idSource = request.getParameter("idSource");
		String idDestination = request.getParameter("idDestination");
		int source = -1, destination = -1;
		CategoryDAO cService = new CategoryDAO(connection);

		String error_message = null;
		Boolean error = false;

		try {
			source = Integer.parseInt(idSource);
			destination = Integer.parseInt(idDestination);
		} catch (NumberFormatException e) {
			error = true;
			error_message = "The category id format is invalid";
		}

		if (!error) {
			// Check if id_source and id_destination are valid
			if (cService.validCategory(source) && (cService.validCategory(destination) || destination == 0)) { // destination = 0 if destination is root
				Category cSource = cService.getSpecificCategory(source);

				if (cSource != null) {
					// Now get subParts of this category:
					try {
						cService.findSubparts(cSource, false, -1);

						// Now check if there is space under the destination:
						if (cService.isThereSpace(destination) != -1) {
							// Now check if id destination is not an id of a category in this subTree:
							if (cService.checkIdDestination(cSource, destination)) {
								// Now copy, insert into the DB the new nodes:
								try {
									cService.copySubTree(cSource, destination);

								} catch (SQLException e) {
									error = true;
									error_message = "An error occurred while copying the selected category subtree";
								}

								if(!error) {
									// redirect to HomePage
									String ctxpath = getServletContext().getContextPath();
									String path = ctxpath + "/GoToHomePage";
									response.sendRedirect(path);
									return;
								}

							} else {
								error = true;
								error_message = "The chosen destination belongs to the category subtree to copy";
							}
						} else {
							error = true;
							error_message = "The limit of subcategories for the chosen destination has already been reached";
						}

					} catch (SQLException e) {
						error = true;
						error_message = "An error occurred while building the subcategory tree";
					}
				} else {
					error = true;
					error_message = "The chosen category to copy is non-existent";
				}

			} else {
				error = true;
				error_message = "Either the chosen category to copy or the destination is non-existent";
			}
		}
		
		if(error) {
			String path = "/GoToErrorPage";
			request.setAttribute("error", error_message);
			RequestDispatcher dispatcher = request.getRequestDispatcher(path);
			dispatcher.forward(request, response);
		}
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
