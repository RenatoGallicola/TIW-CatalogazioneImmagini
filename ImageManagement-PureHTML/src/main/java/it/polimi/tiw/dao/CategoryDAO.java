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
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT C.id, C.name FROM image_management.subcategory S JOIN image_management.category C on C.id = S.child WHERE S.father = ?");) 
		{
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
	
	//Check if idFather is an integer in the servlet, and if the user wants to create a root node idFather = -1;
	public void insertCategory(String cat, int idFather) throws SQLException
	{
		int numSubCategories;
		
		connection.setAutoCommit(false);
		
		try
		{
			if(idFather!=-1)
			{
				//Check now if father exists:
				try (PreparedStatement pstatement = connection.prepareStatement("SELECT * FROM image_management.category WHERE id = ?");)
				{
					pstatement.setInt(1, idFather);
					try (ResultSet result = pstatement.executeQuery();) 
					{
						if (!result.isBeforeFirst()) // no results, father doesn't exists 
							throw new SQLException();
						else
						{
							//Check now if there's a free slot for a child under this father
							numSubCategories = isThereSpace(idFather);
							if(numSubCategories>=0)
							{
								//Check now if the category name is valid:
								if(isValidName(cat))
								{
									//Insert now Category "cat":
									try (PreparedStatement newstatement = connection.prepareStatement("insert into image_management.category values(?,?);");)
									{
										//Create 'id' for the new category:
										String s1 = Integer.toString(idFather);
								        String s2 = Integer.toString(numSubCategories + 1);
								        String s = s1 + s2;
								        int c = Integer.parseInt(s);
										
										newstatement.setInt(1, c);
										newstatement.setString(2, cat);
										
										newstatement.executeUpdate();
									}
									
								}
								else
									throw new SQLException();
								
							}
							else
								throw new SQLException();
						}
					}
				}		
			}
			else
			{
				int quantity;
				
				//Check for space in root and insert:
				try (PreparedStatement pstatement = connection.prepareStatement("SELECT count(*) as quantity FROM image_management.category WHERE id NOT IN (select child FROM image_management.subcategory);");)
				{
					try (ResultSet result = pstatement.executeQuery();)
					{
						result.next();
						quantity = result.getInt("quantity");
						if(quantity < 9)
						{
							//You can insert in root
							//Insert now Category "cat":
							try (PreparedStatement newstatement = connection.prepareStatement("insert into image_management.category values(?,?);");)
							{
								newstatement.setInt(1, quantity + 1);
								newstatement.setString(2, cat);
								
								newstatement.executeUpdate();
							}
						}
						else
						{
							//You can NOT insert in root
							throw new SQLException();
						}
					}
				}
			}
			
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
	
	
	private int isThereSpace(int idFather) throws SQLException {
		
		try (PreparedStatement pstatement = connection.prepareStatement("SELECT count(*) as quantity FROM image_management.subcategory S WHERE S.father = ?;");)
		{
			try (ResultSet result = pstatement.executeQuery();) {
				result.next();
				int numSubCategories = result.getInt("quantity");
				
				if(numSubCategories < 9)
					return numSubCategories;
				else
					return -1;
			}
		}
	}
	
	private boolean isValidName(String name)
	{
		 Pattern p = Pattern.compile("^[ A-Za-z]+$");
	     Matcher m = p.matcher(name);
	     return (m.matches());
	}
	

}
