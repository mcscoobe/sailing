package com.duckblade.osrs.sailing.features.facilities;

/**
 * Represents a facility row in the sailing interface with its widget range.
 */
public class FacilityRow
{
	private final String name;
	private final int startIndex;
	private final int endIndex;
	
	public FacilityRow(String name, int startIndex, int endIndex)
	{
		this.name = name;
		this.startIndex = startIndex;
		this.endIndex = endIndex;
	}
	
	/**
	 * Gets the display name of this facility row.
	 */
	public String getName()
	{
		return name;
	}
	
	/**
	 * Gets the starting widget index for this row.
	 */
	public int getStartIndex()
	{
		return startIndex;
	}
	
	/**
	 * Gets the ending widget index for this row.
	 */
	public int getEndIndex()
	{
		return endIndex;
	}
	
	@Override
	public String toString()
	{
		return name;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		FacilityRow that = (FacilityRow) obj;
		return startIndex == that.startIndex && 
		       endIndex == that.endIndex && 
		       name.equals(that.name);
	}
	
	@Override
	public int hashCode()
	{
		return name.hashCode() + startIndex * 31 + endIndex * 37;
	}
}