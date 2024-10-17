package com.mara.zoic.annohttp.http;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import com.mara.zoic.annohttp.lifecycle.AnnoHttpLifecycle;

class AnnoHttpLifecycleInstancesCahce {
	
	private static List<AnnoHttpLifecycle> ANNOHTTP_LIFECYCLE_LIST = new LinkedList<>();
	
	static void addAnnoHttpLifecycleInstances(AnnoHttpLifecycle... annoHttpLifecycles) {
		if (annoHttpLifecycles != null) {
			for (AnnoHttpLifecycle annoHttpLifecycle : annoHttpLifecycles) {
				ANNOHTTP_LIFECYCLE_LIST.add(annoHttpLifecycle);
			}
		}
	}
	
	static List<AnnoHttpLifecycle> getAnnoHttpLifecycleInstances() {
		return Collections.unmodifiableList(ANNOHTTP_LIFECYCLE_LIST);
	}
}
