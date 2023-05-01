package it.polimi.tiw.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import it.polimi.tiw.beans.Category;

public class CategoryDAO {
	
	private Connection connection;
	
	public CategoryDAO(Connection connection)
	{
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
	
	
	public List<Category> findTopCategoriesAndSubtrees() throws SQLException {
		List<Category> categories = new ArrayList<Category>();
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id NOT IN (select child FROM image_management.subcategory)");) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					c.setIsTop(true);
					categories.add(c);
				}
				
				for (Category cat : categories) {
					findSubparts(cat);
				}
			}
		}
		return categories;
	}
	
	public void findSubparts(Category cat) throws SQLException {
		Category c = null;
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT C.id, C.name FROM image_management.subcategory S JOIN image_management.category C on C.id = S.child WHERE S.father = ?");) {
			pstatement.setInt(1, cat.getId());
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					c = new Category();
					c.setId(result.getInt("id"));
					c.setName(result.getString("name"));
					findSubparts(c);
					cat.addSubCategory(c);
				}
			}
		}

	}
	

}
