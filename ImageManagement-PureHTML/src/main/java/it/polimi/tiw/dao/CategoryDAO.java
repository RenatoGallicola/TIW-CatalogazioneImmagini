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
					Category BP = new Category();
					BP.setId(result.getInt("id"));
					BP.setName(result.getString("name"));
					categories.add(BP);
				}
			}
		}
		return categories;
	}
	
	
	public List<Category> findTopCategoriesAndSubtrees() throws SQLException {
		List<Category> categories = new ArrayList<Category>();
		try (PreparedStatement pstatement = connection
				.prepareStatement("SELECT * FROM image_management.category WHERE id NOT IN (select child FROM image_management.subcategory)");) {
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					Category C = new Category();
					C.setId(result.getInt("id"));
					C.setName(result.getString("name"));
					C.setIsTop(true);
					categories.add(C);
				}
				
				for (Category p : categories) {
					findSubparts(p);
				}
			}
		}
		return categories;
	}
	
	public void findSubparts(Category p) throws SQLException {
		Category C = null;
		try (PreparedStatement pstatement = connection.prepareStatement(
				"SELECT P.id, P.name FROM image_management.subcategory S JOIN image_management.category P on P.id = S.child WHERE S.father = ?");) {
			pstatement.setInt(1, p.getId());
			try (ResultSet result = pstatement.executeQuery();) {
				while (result.next()) {
					C = new Category();
					C.setId(result.getInt("id"));
					C.setName(result.getString("name"));
					findSubparts(C);
					p.addSubCategory(C);
				}
			}
		}

	}
	

}
