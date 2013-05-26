package com.sreader.leksem;

import java.util.ArrayList;
import java.util.StringTokenizer;

import android.util.Log;

public class Chapter
{
	private String name;
	private ArrayList<String> words;

	public Chapter(String setname, String settext)
	{
		this.name = setname;
		
		performText(settext);
	}
	
	/**
	 * Fill words array
	 */
	private void performText(String settext)
	{
		words = new ArrayList<String>();
		String text = settext.replace("\n", " ");
		StringTokenizer tokens = null;
		String stradd;
		tokens = new StringTokenizer(text, " ");
		
		while (tokens.hasMoreTokens())
		{
			stradd = tokens.nextToken();
			if ((stradd.length() < 2) && (tokens.hasMoreTokens()))
				stradd += " " + tokens.nextToken();
			words.add(stradd);
		}
	}

	public String getName()
	{
		return name;
	}

	public void setName(String name)
	{
		this.name = name;
	}

	public String getWord(int id)
	{
		return words.get(id);
	}

	public ArrayList<String> getWords()
	{
		return words;
	}
}
