package org.neurovillage.main;

import java.util.HashMap;

public class NeuroUtils {
	
	public static HashMap<String,Float> getHexLookUpTable(float startVal, float endVal, int val)
	{
		HashMap<String,Float> lookUpTable = new HashMap<String, Float>();
		
		float range = 0;
		if (((startVal<0) && (endVal<0)) || ((startVal>0) && (endVal>0)))
			range = Math.abs(startVal - endVal);
		else
			range = Math.abs(startVal) +  Math.abs(endVal); 
		
		float step = range / (float)val;
		float curVal = startVal;
				
		for (int i = 0; i < val+1; i++)
		{
		    String hex = Integer.toHexString(i);
		    while (hex.length()<3)
		    	hex="0"+hex;
//		    System.out.println(hex + " : " + curVal);
		    lookUpTable.put(hex, curVal);
		    curVal += step;
		}
		return lookUpTable;
	}
	
	public static void main(String[] args) {
		
		
		System.out.println(Long.parseLong("FFFFFE", 16)/1677.7215f);
	
		
	}
	
	 public static long parseUnsignedHex(String text) {
//	        if (text.length() == 16) {
//	            return (parseUnsignedHex(text.substring(0, 1)) << 60)
//	                    | parseUnsignedHex(text.substring(1));
//	        }
		 try
		 {
			 return Long.parseLong(text, 16);
		 }
		 catch (NumberFormatException e)
		 {
			System.err.println("tried to interpret a non-valid hexadecimal value! (string: '" + text + "')");
		 }
		 return 0l;
	    }

	public static HashMap<String, Float> getBrainduinoDefaultLookupTable() {
		int val = 1023;
		float startVal = -100f;
		float endVal = 100f;
		HashMap<String,Float> lookUpTable = getHexLookUpTable(startVal, endVal, val);
		return lookUpTable;
	}

}
