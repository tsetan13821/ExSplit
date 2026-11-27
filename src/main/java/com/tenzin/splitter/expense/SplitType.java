package com.tenzin.splitter.expense;

public enum SplitType {

	/** Server divides the amount; client sends participantIds. */
	EQUAL,

	/** Client sends each person's share; they must add up to the amount. */
	EXACT

}
