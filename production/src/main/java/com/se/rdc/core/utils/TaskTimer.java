package com.se.rdc.core.utils;

import java.util.Date;
import java.util.HashMap;

//TODO: change the design into non-static class return based
public class TaskTimer {
	private static HashMap<String, TaskTime> timeMap = new HashMap<>();

	public static void startTrack(String tag) {
		timeMap.put(tag, new TaskTime());
	}

	public static void endTrack(String tag) {
		timeMap.get(tag).calculateExecTime(tag, false);
		timeMap.remove(tag);
	}

	public static void endTrackLaps(String tag) {
		timeMap.get(tag).calculateExecTime(tag, true);
	}

	private static class TaskTime {
		Date sTime = new Date();
		Date eTime;

		// Calculate time difference
		void calculateExecTime(String tag, boolean isLaps) {
			eTime = new Date();

			System.out.println(
					"\n\n================Time analysis====================");
			System.out.println("Started at:" + sTime.toString());
			System.out.println("End at:" + eTime.toString());

			long diff = eTime.getTime() - sTime.getTime();
			float time = (float) diff / (float) (1000);// sec chk
			int h = 0, m = 0;
			float s = 0;
			if (time < 60) {
				s = time;
			} else {
				time /= 60; // min chk
				if (time < 60) {
					m = (int) time;
					s = (time - m) * 60;
				} else {
					time /= 60; // hr chk
					h = (int) time;
					time = (time - h) * 60;
					m = (int) time;
					s = time - m;
				}
			}
			System.out.println(
					"Execution time for " + tag + ": " + h + " hour(s) " + m + " minute(s) " + s + " second(s)");

			if (isLaps) {
				sTime = eTime;
			}
		}

	}

}
