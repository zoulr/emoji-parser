package me.zoulr.emoji.parser;

import java.util.List;

public interface Callable {
	String assemble(List<Integer> emojiCodePoints);
}