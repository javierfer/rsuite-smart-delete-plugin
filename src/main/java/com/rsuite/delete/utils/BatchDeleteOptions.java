package com.rsuite.delete.utils;

import java.util.ArrayList;
import java.util.List;

public class BatchDeleteOptions {

	/**
	 * Include top level content assembly in the batch deletion.
	 */
	private boolean includeTopCaForDeletion = false;

	/**
	 * List of file extensions to exclude for deletion.
	 */
	private List<String> extensionsExcluded = new ArrayList<>();

	public boolean isIncludeTopCaForDeletion() {
		return includeTopCaForDeletion;
	}

	public void setIncludeTopCaForDeletion(boolean includeTopCaForDeletion) {
		this.includeTopCaForDeletion = includeTopCaForDeletion;
	}

	public boolean isExtensionExcluded(String fileExtension) {
		return extensionsExcluded.contains(fileExtension);
	}

	public void addExtensionExcluded(String fileExtension) {
		extensionsExcluded.add(fileExtension);
	}

}
