package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import it.polimi.tiw.beans.Category;
import java.util.regex.Pattern;

import com.mysql.cj.protocol.a.TextRowFactory;

import java.util.regex.Matcher;

public class CategoryDAO {

	private Connection connection;

	public CategoryDAO(Connection connection) {
		this.connection = connection;
	}

	public List<Category> findAllCategories() throws SQLException{
		List<Category> categories = new ArrayList<Category>();
		PreparedStatement pstatement = null;
		String query = "SELECT * FROM image_management.category";

		try {
			pstatement = connection.prepareStatement(query);
			ResultSet result = null;
			try {
				result = pstatement.executeQuery();
				while (result.next()) {
					Category c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					categories.add(c);
				}
			} finally {
				result.close();
			}
		} finally {
			pstatement.close();
		}

		return categories;
	}

	public List<Category> findTopCategoriesAndSubtrees(int selected_c, boolean switch_selected) throws SQLException {
		List<Category> categories = new ArrayList<Category>();
		boolean found_selected = false; // 'true' if the selected category to copy is located in the tree root
		PreparedStatement pstatement = null;
		String query = "SELECT * FROM image_management.category WHERE id NOT IN (select child FROM image_management.subcategory)";

		try {
			pstatement = connection.prepareStatement(query);
			ResultSet result = null;
			try {
				result = pstatement.executeQuery();
				while (result.next()) {
					Category c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					c.setIsTop(true);

					if (switch_selected && c.getId() == selected_c) { // current category has been selected to be copied
						c.setSelected(true);
						found_selected = true;
					}

					categories.add(c);
				}

				for (Category cat : categories) {
					if (switch_selected && found_selected && cat.getId() == selected_c) // current category is the selected one to copy
						findSubparts(cat, true, -1);
					else if (switch_selected && found_selected && cat.getId() != selected_c) // found selected category to copy but it's not the current one
						findSubparts(cat, false, -1);
					else if (switch_selected && !found_selected) // selected category to copy not in the tree root
						findSubparts(cat, true, selected_c);
					else
						findSubparts(cat, false, -1);
				}
			} finally {
				result.close();
			}
		} finally {
			pstatement.close();
		}
		return categories;
	}

	// switch_selected : 'true' if the attribute 'cat.selected' should be set to 'true', 'false' otherwise
	// selected_c : the id of the selected subtree root category to copy.
	// 				'-1' if no category has been selected or if the selected category has already
	// 				been found in an upper level of the tree (according to the value of 'switch
	// 				selected')
	public void findSubparts(Category cat, boolean switch_selected, int selected_c) throws SQLException {
		Category c = null;
		PreparedStatement pstatement = null;
		String query = "SELECT C.id, C.name FROM image_management.subcategory S JOIN image_management.category C on C.id = S.child WHERE S.father = ?";
		
		try {
			pstatement = connection.prepareStatement(query);
			pstatement.setInt(1, cat.getId());
			ResultSet result = null;
			try {
				result = pstatement.executeQuery();
				while (result.next()) {
					c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));

					if (switch_selected && selected_c == -1) { // current category is the child of a selected one to copy
						c.setSelected(true);
						findSubparts(c, true, -1);
					} else if (switch_selected && c.getId() == selected_c) { // current category is the selected one to copy
						c.setSelected(true);
						findSubparts(c, true, -1);
					} else if (switch_selected && c.getId() != selected_c) { // current category is not the selected one to copy
						c.setSelected(false);
						findSubparts(c, true, selected_c);
					} else { // no categories should be copied
						c.setSelected(false);
						findSubparts(c, false, -1);
					}

					cat.addSubCategory(c);
				}
			} finally {
				result.close();
			}
		} finally {
			pstatement.close();
		}
	}

	// Check if idFather is an integer in the servlet, and if the user wants to
	// create a root node idFather = 0;
	public void realInsertCategory(String cat, int idFather) throws SQLException {
		int numSubCategories;
		ResultSet result = null;
		PreparedStatement pstatement = null;
		String query = "SELECT * FROM image_management.category WHERE id = ?";

		if (idFather != 0) {
			// Check now if father exists:
			try {
				pstatement = connection.prepareStatement(query);
				pstatement.setInt(1, idFather);
				try {
					result = pstatement.executeQuery();
					if (!result.isBeforeFirst()) // no results, father doesn't exists
						throw new SQLException();
					else {
						// Check now if there's a free slot for a child under this father
						numSubCategories = isThereSpace(idFather);
						if (numSubCategories >= 0) {
							// Check now if the category name is valid:
							if (isValidName(cat)) {
								// Insert now Category "cat":
								PreparedStatement newstatement = null;
								query = "insert into image_management.category values(?,?);";
								try {
									newstatement = connection.prepareStatement(query);
									// Create 'id' for the new category:
									String s1 = Integer.toString(idFather);
									String s2 = Integer.toString(numSubCategories + 1);
									String s = s1 + s2;
									int c = Integer.parseInt(s);

									newstatement.setInt(1, c);
									newstatement.setString(2, cat);

									newstatement.executeUpdate();

									PreparedStatement sub_cat_statement = null;
									String sub_cat_query = "insert into image_management.subcategory values(?,?);";
									try {
										sub_cat_statement = connection.prepareStatement(sub_cat_query);
										sub_cat_statement.setInt(1, idFather);
										sub_cat_statement.setInt(2, c);
										sub_cat_statement.executeUpdate();
									} finally {
										sub_cat_statement.close();
									}
								} finally {
									newstatement.close();
								}

							} else
								throw new SQLException();

						} else
							throw new SQLException();
					}
				} finally {
					result.close();
				}
			} finally {
				pstatement.close();
			}
		} else {

			int quantity;
			PreparedStatement newstatement = null;

			if (isValidName(cat)) {
				// Check for space in root and insert:
				quantity = isThereSpace(idFather);
				if (quantity != -1) {
					// You can insert in root
					// Insert now Category "cat":
					try {
						newstatement = connection.prepareStatement("insert into image_management.category values(?,?);");
						newstatement.setInt(1, quantity + 1);
						newstatement.setString(2, cat);

						newstatement.executeUpdate();
					} finally {
						newstatement.close();
					}
				} else {
					// You can NOT insert in root
					throw new SQLException();
				}
			} else {
				throw new SQLException();
			}
		}
	}

	public void insertCategory(String cat, int idFather) throws SQLException {

		try {

			connection.setAutoCommit(false);
			realInsertCategory(cat, idFather);
			connection.commit();

		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}
	}

	public int isThereSpace(int idFather) throws SQLException {

		PreparedStatement pstatement = null;
		ResultSet result = null;

		if (idFather != 0) { // father is not root:
			try {
				pstatement = connection.prepareStatement("SELECT count(*) as quantity FROM image_management.subcategory S WHERE S.father = ?;");
				pstatement.setInt(1, idFather);
				try {
					result = pstatement.executeQuery();
					result.next();
					int numSubCategories = result.getInt("quantity");

					if (numSubCategories < 9)
						return numSubCategories;
					else
						return -1;
				} finally {
					result.close();
				}

			} finally {
				pstatement.close();
			}
		} else {
			// Father is root:
			int quantity;
			try {
				pstatement = connection.prepareStatement("SELECT count(*) as quantity FROM image_management.category WHERE id NOT IN (select child FROM image_management.subcategory);");
				try {
					result = pstatement.executeQuery();
					result.next();
					quantity = result.getInt("quantity");

					if (quantity < 9)
						return quantity;
					else
						return -1;
				} finally {
					result.close();
				}
			} finally {
				pstatement.close();
			}

		}
	}

	public boolean isValidName(String name) {
		Pattern p = Pattern.compile("^[ A-Za-z]+$");
		Matcher m = p.matcher(name);
		return (m.matches() && !(name.isBlank()));
	}

	// Check if a Category Exists
	public boolean validCategory(int c_id) {

		PreparedStatement pstatement = null;
		ResultSet result = null;
		boolean resultMethod = false;
		
		try {
			pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id = ?");
			pstatement.setInt(1, c_id);
			try {
				result = pstatement.executeQuery();

				if (!result.isBeforeFirst()) // category doesn't exist
					resultMethod = false;
				else
					resultMethod = true;

			} catch (SQLException e) {
				resultMethod = false;

			} finally {
				result.close();
			}

		} catch (SQLException f) {
			resultMethod = false;
		} finally {
			try {
				pstatement.close();
			} catch (SQLException g) {
				resultMethod = false;
			}
		}

		return resultMethod;
	}

	public Category getSpecificCategory(int idCategory) {
		Category c = null;
		PreparedStatement pstatement = null;
		ResultSet result = null;

		try {
			try {
				pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id = ?");
				pstatement.setInt(1, idCategory);
				try {
					result = pstatement.executeQuery();
					if (!result.isBeforeFirst()) // category doesn't exist
					{
						c = null;
					} else {
						c = new Category();
						result.next();
						c.setId(idCategory);
						c.setName(result.getString("name"));
					}

				} catch (SQLException e) {
					c = null;
				} finally {
					result.close();
				}

			} catch (SQLException f) {
				c = null;
			} finally {
				pstatement.close();
			}
		} catch (SQLException e) {
			c = null;
		}

		return c;
	}

	
	public boolean checkIdDestination(Category cat, int idDestination) {
		boolean result = true;
		if (cat.getId() != idDestination) {
			List<Category> sub = cat.getSubCategories();

			for (int i = 0; i < sub.size() && result; i++)
				result = checkIdDestination(sub.get(i), idDestination);

			return result;

		} else
			return false;
	}

	public void copySubTree(int[] src, int[] dst) throws SQLException {
		int len = src.length;
		boolean bad_req = false;
		
		try {
			connection.setAutoCommit(false);
			
			for(int i=0; i<len && !bad_req; i++) {
				if(validCategory(src[i]) && (validCategory(dst[i]) || dst[i] == 0)) {
					Category c_src = getSpecificCategory(src[i]);
					if(c_src != null) {
						try {
							findSubparts(c_src, false, -1);
							if (isThereSpace(dst[i]) != -1) {
								if(checkIdDestination(c_src, dst[i])) {
									try {
										realCopySubTree(c_src, dst[i]);
									} catch (SQLException e) {
										bad_req = true;
									}
								} else {
									bad_req = true;
								}
							} else {
								bad_req = true;
							}
						} catch (SQLException e) {
							bad_req = true;
						}
					} else {
						bad_req = true;
					}
				} else {
					bad_req = true;
				}
			}
			
			if (bad_req)
				throw new SQLException();
			
			connection.commit();
		} catch (SQLException e) {
			connection.rollback();
			throw e;
		} finally {
			connection.setAutoCommit(true);
		}
	}

	private void realCopySubTree(Category cat, int idDestination) throws SQLException {
		List<Category> sub = cat.getSubCategories();
		int numChildrenOfDestination = isThereSpace(idDestination); // Get number of children under destination

		// Calculate the id of the category I have to insert (needed for next copy)
		int idOfNewCategory = Integer.parseInt(Integer.toString(idDestination) + Integer.toString(numChildrenOfDestination + 1));

		realInsertCategory(cat.getName(), idDestination);

		for (Category c : sub) {
			// Build id destination of cat now:
			realCopySubTree(c, idOfNewCategory);
		}
	}
	
	
	public void changeName(int id_cat, String newName) throws SQLException
	{	
		
		try
		{
			connection.setAutoCommit(false);
			//Change name now:
			realChangeName(id_cat, newName);
			
			connection.commit();
		}
		catch(SQLException e)
		{
			connection.rollback();
			throw e;
		}
		finally
		{
			connection.setAutoCommit(true);
		}
	}
	
	
	private void realChangeName(int id_cat, String newName) throws SQLException
	{
		PreparedStatement pstatement = null;
		try
		{
			pstatement = connection.prepareStatement("UPDATE image_management.category SET name = ? WHERE id = ?");
			
			//Set parameters in query now:
			pstatement.setString(1, newName);
			pstatement.setInt(2, id_cat);
			
			//Execute query:
			pstatement.executeUpdate();
			
		}
		finally
		{
			pstatement.close();
		}
		
	}
}