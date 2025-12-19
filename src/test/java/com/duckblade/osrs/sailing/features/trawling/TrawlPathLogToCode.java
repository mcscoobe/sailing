package com.duckblade.osrs.sailing.features.trawling;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TrawlPathLogToCode
{
	public static void main(String[] args) throws IOException
	{
		//pathlog.txt should be in the local directory of the plugin. It is a copy of the client log or console log
		File file = new File("pathlog.txt");
		if (!file.exists()) {
			return;
		}

		String[] lines = Files.readAllLines(file.toPath()).toArray(new String[0]);
		int start = -1;
		int end = -1;
		for (int i = 0; i < lines.length; i++) {
			String line = lines[i];
			if (start < 0 && line.contains("public static final WorldPoint[] SHOAL")) {
				start = i;
			}

			if (end < 0 && line.contains("Stop points: ")) {
				end = i - 2;
			}

			String search = "AREA = ";
			int j = line.indexOf(search);
			if (j >= 0) {
				j += search.length();
				String[] parts = line.substring(j).trim().split(", ");
				int x1 = Integer.parseInt(parts[0]);
				int x2 = Integer.parseInt(parts[1]);
				int y1 = Integer.parseInt(parts[2]);
				int y2 = Integer.parseInt(parts[3]);
				System.out.println("new WorldArea("+x1+", "+y1+", "+(x2-x1)+", "+(y2-y1)+", 0)");
			}

			search = "STOP_INDICES = [";
			j = line.indexOf(search);
			if (j >= 0) {
				j += search.length();
				String[] parts = line.substring(j, line.lastIndexOf("]")).trim().split(", ");
				System.out.print("new int[]{");
				System.out.print(Integer.parseInt(parts[0]));
				for (int k = 1; k < parts.length; k++) {
					System.out.print(", "+Integer.parseInt(parts[k]));
				}
				System.out.println("}");
			}
		}

		for (int i = start; i <= end; i++) {
			lines[i] = lines[i].replaceAll(".*ShoalPathTracker - ", "");
			System.out.println(lines[i]);
		}
	}
}
