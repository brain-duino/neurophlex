package org.neurovillage.tools;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

public class Utils {

	public static int countLines(String filename) throws IOException {
		return countLines(new File(filename));
	}
	public static int countLines(File file) {
		try {
			InputStream is;
			is = new BufferedInputStream(new FileInputStream(file));
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = is.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}
			is.close();
			return (count == 0 && !empty) ? 1 : count;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		return -1;
	}
}
