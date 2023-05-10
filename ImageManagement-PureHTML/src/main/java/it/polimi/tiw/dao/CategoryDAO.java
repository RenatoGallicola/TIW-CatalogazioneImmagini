package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import it.polimi.tiw.beans.Category;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class CategoryDAO {

	private Connection connection;

	public CategoryDAO(Connection connection) {
		this.connection = connection;
	}

	public List<Category> findAllCategories() throws SQLException {
		List<Category> categories = new ArrayList<Category>();
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM image_management.category");) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					categories.add(c);
				}
			}
		}
		return categories;
	}

	public List<Category> findTopCategoriesAndSubtrees(int selected_c, boolean switch_selected) throws SQLException {
		List<Category> categories = new ArrayList<Category>();
		boolean found_selected = false; // 'true' if the selected category to copy is located in the tree root 
		
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id NOT IN (select child FROM image_management.subcategory)");) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					c.setIsTop(true);
					
					if(switch_selected && c.getId() == selected_c) { // current category has been selected to be copied
						c.setSelected(true);
						found_selected = true;
					}
					
					categories.add(c);
				}

				for (Category cat : categories) {
					
					if(switch_selected && found_selected && cat.getId() == selected_c) // current category is the selected one to copy
						findSubparts(cat, true, -1);
					else if (switch_selected && found_selected && cat.getId() != selected_c) // found selected category to copy but it's not the current one
						findSubparts(cat, false, -1);
					else if (switch_selected && !found_selected) // selected category to copy not in the tree root
						findSubparts(cat, true, selected_c);
					else 
						findSubparts(cat, false, -1);
					
				}
			}
		}
		return categories;
	}

	// switch_selected : 'true' if the attribute 'cat.selected' should be set to 'true', 'false' otherwise
	// selected_c : the id of the selected subtree root category to copy. 
	// 				'-1' if no category has been selected or if the selected category has already been found in an upper level of the tree (according to the value of 'switch selected')
	public void findSubparts(Category cat, boolean switch_selected, int selected_c) throws SQLException {
		Category c = null;
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT C.id, C.name FROM image_management.subcategory S JOIN image_management.category C on C.id = S.child WHERE S.father = ?");) {
			pstatement.setInt(1, cat.getId());
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					
					if(switch_selected && selected_c == -1) { // current category is the child of a selected one to copy
						c.setSelected(true);
						findSubparts(c, true, -1);
					}else if(switch_selected && c.getId() == selected_c) { // current category is the selected one to copy
						c.setSelected(true);
						findSubparts(c, true, -1);
					}else if(switch_selected && c.getId() != selected_c) { // current category is not the selected one to copy
						c.setSelected(false);
						findSubparts(c, true, selected_c);
					}else { // no categories should be copied
						c.setSelected(false);
						findSubparts(c, false, -1);
					}
					
					cat.addSubCategory(c);
				}
			}
		}

	}

	// Check if idFather is an integer in the servlet, and if the user wants to
	// create a root node idFather = 0;
	public void insertCategory(String cat, int idFather) throws SQLException {
		int numSubCategories;
		
		
		if (idFather != 0) {
			// Check now if father exists:
			try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id = ?");) {
				pstatement.setInt(1, idFather);
				try (ResultSet result = pstatement.executeQuery();) {
					if (!result.isBeforeFirst()) // no results, father doesn't exists
						throw new SQLException();
					else {
						// Check now if there's a free slot for a child under this father
						numSubCategories = isThereSpace(idFather);
						if (numSubCategories >= 0) {
							// Check now if the category name is valid:
							if (isValidName(cat)) {
								// Insert now Category "cat":
								try (PreparedStatement newstatement = connection.prepareStatement("insert into image_management.category values(?,?);");) {
									// Create 'id' for the new category:
									String s1 = Integer.toString(idFather);
									String s2 = Integer.toString(numSubCategories + 1);
									String s = s1 + s2;
									int c = Integer.parseInt(s);

									newstatement.setInt(1, c);
									newstatement.setString(2, cat);

									newstatement.executeUpdate();

									String sub_cat_query = "insert into image_management.subcategory values(?,?);";
									try (PreparedStatement sub_cat_statement = connection.prepareStatement(sub_cat_query);) {
										sub_cat_statement.setInt(1, idFather);
										sub_cat_statement.setInt(2, c);
										sub_cat_statement.executeUpdate();
									}
								}

							} else
								throw new SQLException();

						} else
							throw new SQLException();
					}
				}
			}
		} else {
			
			int quantity;
			
			if(isValidName(cat))
			{
				// Check for space in root and insert:
				try (PreparedStatement pstatement = connection.prepareStatement("SELECT count(*) as quantity FROM image_management.category WHERE id NOT IN (select child FROM image_management.subcategory);");) {
					try (ResultSet result = pstatement.executeQuery();) {
						result.next();
						quantity = result.getInt("quantity");
						if (quantity < 9) {
							// You can insert in root
							// Insert now Category "cat":
							try (PreparedStatement newstatement = connection.prepareStatement("insert into image_management.category values(?,?);");) {
								newstatement.setInt(1, quantity + 1);
								newstatement.setString(2, cat);

								newstatement.executeUpdate();
							}
						} else {
							// You can NOT insert in root
							throw new SQLException();
						}
					}
				}
			}
			else
			{
				throw new SQLException();
			}
		}		
	}
	

	public int isThereSpace(int idFather) throws SQLException {

		try (PreparedStatement pstatement = connection.prepareStatement("SELECT count(*) as quantity FROM image_management.subcategory S WHERE S.father = ?;");) {
			pstatement.setInt(1, idFather);
			try (ResultSet result = pstatement.executeQuery();) {
				result.next();
				int numSubCategories = result.getInt("quantity");

				if (numSubCategories < 9)
					return numSubCategories;
				else
					return -1;
			}
		}
	}

	private boolean isValidName(String name) {
		Pattern p = Pattern.compile("^[ A-Za-z]+$");
		Matcher m = p.matcher(name);
		return (m.matches() && !(name.isBlank()));
		
	}

	public boolean validCategory(int c_id) {
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id = ?");) {
			pstatement.setInt(1, c_id);
			try (ResultSet result = pstatement.executeQuery();) {
				if (!result.isBeforeFirst()) // category doesn't exist
					return false;
				return true;
			}catch(SQLException e) {
				return false;
			}
		}catch(SQLException f) {
			return false;
		}
	}
	
	public Category getSpecificCategory(int idCategory)
	{
		Category c = new Category();
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id = ?");) {
			pstatement.setInt(1, idCategory);
			try (ResultSet result = pstatement.executeQuery();) {
				if (!result.isBeforeFirst()) // category doesn't exist
					return null;
				
				result.next();
				c.setId(idCategory);
				c.setName(result.getString("name"));
				return c;
				
			}catch(SQLException e) {
				return null;
			}
		}catch(SQLException f) {
			return null;
		}
	}
	
	
	public boolean checkIdDestination(Category cat, int idDestination)
	{
		boolean result = true;
		if(cat.getId()!=idDestination)
		{
			List<Category> sub = cat.getSubCategories();
			
			for(int i=0; i<sub.size() && result; i++)
				result = checkIdDestination(sub.get(i), idDestination);
			
			return result;
			
		}
		else
			return false;
	}
	
	
	public void copySubTree(Category cat, int idDestination) throws SQLException
	{
		try
		{
			connection.setAutoCommit(false);
			
			realCopySubTree(cat, idDestination);
			
			connection.commit();
		}
		catch(SQLException e)
		{
			connection.rollback();
		}
		finally
		{
			connection.setAutoCommit(true);
		}
	}
	
	
	private void realCopySubTree(Category cat, int idDestination) throws SQLException
	{
		List<Category> sub = cat.getSubCategories();
		int numChildsOfDestination = isThereSpace(idDestination); //Get number of childs under destination
		
		//Calculate the id of the category I have to insert (needed for next copy)
		int idOfNewCategory = Integer.parseInt(Integer.toString(idDestination) + Integer.toString(numChildsOfDestination + 1));
		
		insertCategory(cat.getName(), idDestination);
		
		for(Category c: sub)
		{
			//Build id destination of cat now:
			realCopySubTree(c, idOfNewCategory);
		}
	}

}