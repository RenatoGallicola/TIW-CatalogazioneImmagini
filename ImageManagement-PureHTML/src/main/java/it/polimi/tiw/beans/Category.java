package it.polimi.tiw.beans;

import java.util.ArrayList;
import java.util.List;

public class Category {
	
	private int id;
	private String name;
	private Boolean isTop;
	private List<Category> subCategories;
	
	public Category()
	{
		this.isTop =false;
		subCategories = new ArrayList<Category>();
	}
	
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public Boolean getIsTop() {
		return isTop;
	}
	
	public void setIsTop(Boolean isTop) {
		this.isTop = isTop;
	}
	
	public List<Category> getSubCategories(){
		return subCategories;
	}
	
	public void addSubCategory(Category part) {
		subCategories.add(part);
	}
	
	@Override
	public String toString() {
		return "Category [id=" + id + ", name=" + name + ", isTop=" + isTop + "]";
	}

}
