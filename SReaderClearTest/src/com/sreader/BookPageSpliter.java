package com.sreader;

import java.util.ArrayList;
import java.util.Arrays;
import android.util.FloatMath;
import android.util.Log;

import com.sreader.leksem.Book;
import com.sreader.leksem.Chapter;
import com.sreader.store.SetChapter;

/**
 * Splits text from book for pages. Caller should implement TextWidthMeasurer
 * which measure text.
 */
class BookPageSpliter
{
	private final Book book;
	private final ArrayList<String> pages;
	private final ArrayList<SetChapter> chapters;
	private final TextWidthMeasurer textMeasurer;

	interface TextWidthMeasurer
	{
		boolean isSinglelineText(String text);

		float[] getCharactersWidths(String text);

		int getMaxPageWidth();
		
		float getCharacterWidth(char character);
	}

	BookPageSpliter(Book book, ArrayList<String> pages, ArrayList<SetChapter> chapters, TextWidthMeasurer textMeasurer)
	{
		AssertExeption._assertParamNull(book, "book");
		AssertExeption._assertParamNull(pages, "pages");
		AssertExeption._assertParamNull(chapters, "chapters");
		AssertExeption._assertParamNull(textMeasurer, "textMeasurer");

		this.book = book;
		this.pages = pages;
		this.chapters = chapters;
		this.textMeasurer = textMeasurer;
	}

	void split()
	{		
		try
		{
			splitFast();
			//splitStrictly();
		} catch (OutOfMemoryError e)
		{
			Log.d(getClass().getSimpleName(), "Get OutOfMemoryError exception. Use splitStrictly()");
			splitStrictly();
 }
	}

	private void splitFast()
	{
		FastSplitter fs = new FastSplitter();
		fs.split();
	}

	/**
	 * Works fast, but use much memory. Module measure each character width, remember it and reuse.
	 * As character width in Android system represent by float, this class use scaled to integer value of character width. Scale factor is {@link #WIDTH_SCALE_FACTOR}.
	 * More details about float usage here <a href="http://developer.android.com/training/articles/perf-tips.html#AvoidFloat">Avoid Using Floating-Point</a>
	 * 
	 * At first I try use HashMap for storing characters and associated widths but it works slow... So I use classic array of integers {@code widthForCharCode}.
	 * 
	 * It seems that in this implementation will works fine on texts which use Basic Multilingual Plane characters but it should be enough. More details look here: 
	 *  <a href="http://ru.wikipedia.org/wiki/%D0%A1%D0%B8%D0%BC%D0%B2%D0%BE%D0%BB%D1%8B,_%D0%BF%D1%80%D0%B5%D0%B4%D1%81%D1%82%D0%B0%D0%B2%D0%BB%D0%B5%D0%BD%D0%BD%D1%8B%D0%B5_%D0%B2_%D0%AE%D0%BD%D0%B8%D0%BA%D0%BE%D0%B4%D0%B5#.D0.91.D0.B0.D0.B7.D0.BE.D0.B2.D0.B0.D1.8F_.D0.BC.D0.BD.D0.BE.D0.B3.D0.BE.D1.8F.D0.B7.D1.8B.D0.BA.D0.BE.D0.B2.D0.B0.D1.8F_.D0.BF.D0.BB.D0.BE.D1.81.D0.BA.D0.BE.D1.81.D1.82.D1.8C">Символы, представленные в Юникоде</a> 
	 * 
	 * If Supplementary Characters are valid input data - proper check must be add and module should switch to old variant of the algorithm. 
	 *  
	 */
	private class FastSplitter	 
	{
		private final static int WIDTH_SCALE_FACTOR = 1000;
		private final static int NOT_INITED_WIDTH_VAL = -1;
		
		/*
		 * For maximum speed performance optimization there is sense make this array static and allocate once. 
		 * But it will eat 260 KBytes of memory persistently.
		 */
		private final int[] widthForCharCode;	 					
		private final int maxWidth;

		private final int SPACE_WIDTH;
		
		FastSplitter()
		{
			widthForCharCode = new int[Character.MAX_VALUE + 1];
			
			//Mark all character widths as not_inited 
			Arrays.fill(widthForCharCode, NOT_INITED_WIDTH_VAL); 
			
			maxWidth = WIDTH_SCALE_FACTOR * textMeasurer.getMaxPageWidth();
			
			//Calculate space width. Use for speed optimization, to avoid previous initialization check.
			SPACE_WIDTH = getCharacterSize(' ');
		}
		
		void split()
		{
			splitBookByPages();
		}

		/**
		 * Optimize code by avoid passing buffer as method parameter
		 */
		private StringBuilder strToAdd;
		private int curLineWidth;

		private void splitBookByPages()
		{
			strToAdd = new StringBuilder(32);
			curLineWidth = 0;

			boolean checkAdd = false;

			int setIdChapter = 0;
			int setIdWord = 0;
			int setColWord = 0;
			try
			{
				for (Chapter curentchapter : book)
				{
					pages.add(curentchapter.getName() + "#");
					chapters.add(new SetChapter(setIdChapter, setIdWord, setColWord));
					
					int count_words = curentchapter.getWords().size();
					for (int i = 0; i < count_words; i++)
					{
						//Always add first word in line.
						String firstWord = curentchapter.getWord(i);
						strToAdd.append(firstWord);
						int tempLen = firstWord.length();
						for (int curCharNum = 0; curCharNum < tempLen ; curCharNum++)
						{
							curLineWidth += getCharacterSize(firstWord.charAt(curCharNum));
						}
									
						setIdWord = i;
						checkAdd = false;

						while (!checkAdd)
						{
							if (i + 1 < count_words)
							{
								boolean isSingleLine = isSinglelineText(curentchapter.getWord(i + 1));
								if (isSingleLine)
								{
									i++;
									setColWord++;
									strToAdd.append(curentchapter.getWord(i));
									strToAdd.append(' ');
								}
								else
								{
									checkAdd = true;
								}
							}
							else
							{
								checkAdd = true;
							}
						}
						// Add new measured page
						pages.add(strToAdd.toString());
						chapters.add(new SetChapter(setIdChapter, setIdWord, setColWord));
						// strToAdd = null;
						strToAdd = new StringBuilder(32);
						curLineWidth = 0;

						setColWord++;
					}
					setIdChapter++;
				}

			} catch (Exception e)
			{
			}
		}
		
		/**
		 * Get character size from cache or measure it if it wasn't meat before. 
		 */
		private final int getCharacterSize(char character)
		{	
			int width = widthForCharCode[(int)character];  
			if(width == NOT_INITED_WIDTH_VAL)
			{
				//Ensure width and cache it.
				width = (int) (FloatMath.ceil(WIDTH_SCALE_FACTOR * textMeasurer.getCharacterWidth(character)));
				widthForCharCode[(int)character] = width;
			}
			
			return width;
		}

		/**
		 * Check will by text still single line after add {@code newWord}. Add ' ' character after last word.
		 * Note! Always change {@#link curLineWidth} even if text is not single line. OOP suffers for performance...
		 */
		private boolean isSinglelineText(String newWord)
		{
			final int len = newWord.length();

			curLineWidth += SPACE_WIDTH; 
			for (int i = 0; i < len; i++)
			{
				curLineWidth += getCharacterSize(newWord.charAt(i));
			}

			if (curLineWidth > maxWidth)
				return false;
			else
				return true;
		}
		
		/*  		
		HashMap<Character, Float> widthForCharacter;
		String allUniqueCharacter;
		private void calculateChracterWidth()
		{
			StringBuilder sb = new StringBuilder();

			// Bypass all book one time, get all unique characters entries.
			for (Chapter curentchapter : book)
			{
				int wordsNum = curentchapter.getWords().size();
				for (int i = 0; i < wordsNum; i++)
				{
					String curWord = curentchapter.getWord(i);
					// So here we iterate each word from book by characters

					int wordLen = curWord.length();
					for (int j = 0; j < wordLen; j++)
					{
						if (!widthForCharacter.containsKey(curWord.charAt(j)))
						{
							char ch = curWord.charAt(j);
							// Put in map to determine founded characters. Use
							// HashMap for quick search.
							widthForCharacter.put(ch, null);
							sb.append(ch);
						}
					}
				}
			}

			allUniqueCharacter = sb.toString();
		}

		private void measureCharacters()
		{
			// Measure all characters
			float[] widths = textMeasurer.getCharactersWidths(allUniqueCharacter);
			AssertExeption._assert(widths.length == allUniqueCharacter.length(), "Logic error: lengths must match");

			// Fill width for each character
			int len = allUniqueCharacter.length();
			for (int i = 0; i < len; i++)
			{
				char curCh = allUniqueCharacter.charAt(i);
				widthForCharacter.put(curCh, widths[i]);
			}
		}
		 
		 */
	}

	/**
	 * Original implementation.
	 * Don't require much memory, but works slow.
	 */
	private void splitStrictly()
	{
		String strToAdd;
		boolean checkAdd = false;

		int set_id_chapter = 0;
		int set_id_word = 0;
		int set_col_word = 0;
		try
		{
			for (Chapter curentchapter : book)
			{
				pages.add(curentchapter.getName() + "#");
				chapters.add(new SetChapter(set_id_chapter, set_id_word, set_col_word));
				int count_words = curentchapter.getWords().size();
				for (int i = 0; i < count_words; i++)
				{
					strToAdd = curentchapter.getWord(i);
					set_id_word = i;

					checkAdd = false;

					while (!checkAdd)
					{
						if (i + 1 < count_words)
						{
							//NOTE!!!!!! In old implementation there is a bug! Concatenated for measure string didn't contains space " ".  
							boolean isSingleLine = textMeasurer.isSinglelineText(strToAdd + " " + curentchapter.getWord(i + 1));
							if (isSingleLine)
							{
								i++;
								set_col_word++;
								strToAdd = strToAdd + " " + curentchapter.getWord(i);
							}
							else
							{
								checkAdd = true;
							}
						}
						else
						{
							checkAdd = true;
						}
					}
					// Add new measured page
					pages.add(strToAdd);
					chapters.add(new SetChapter(set_id_chapter, set_id_word, set_col_word));
					// strToAdd = null;

					set_col_word++;
				}
				set_id_chapter++;
			}

		} catch (Exception e)
		{
			AssertExeption._assert(false, "Unexpected exception was thrown!", e);
		}
	}

}
