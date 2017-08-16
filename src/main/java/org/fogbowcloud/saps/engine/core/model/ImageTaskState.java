package org.fogbowcloud.saps.engine.core.model;

import java.util.ArrayList;
import java.util.List;

public enum ImageTaskState {

	NOT_DOWNLOADED("not_downloaded"), SELECTED("selected"), DOWNLOADING(
			"downloading"), DOWNLOADED("downloaded"), QUEUED("queued"), RUNNING(
			"running"), FINISHED("finished"), FETCHING("fetching"), FETCHED(
			"fetched"), CORRUPTED("corrupted"), ERROR("error");

	private String value;

	private ImageTaskState(String value) {
		this.value = value;
	}

	public String getValue() {
		return value;
	}
	
	public boolean in(ImageTaskState... imageStates) {
		for (ImageTaskState imageState : imageStates) {
			if (imageState.equals(this)){
				return true;
			}
		}
		return false;
	}
	
	public static ImageTaskState getStateFromStr(String stateStr) {
		ImageTaskState[] elements = values();
		for (ImageTaskState currentState : elements) {
			if (currentState.getValue().equals(stateStr) ||
					currentState.name().equals(stateStr)) {
				return currentState;
			}
		}
		return null;
	}
	
	public static List<String> getAllValues(){
		
		List<String> values = new ArrayList<String>();
		
		for (ImageTaskState currentState : values()) {
			values.add(currentState.name());
		}
		
		return values;
	}
	
}