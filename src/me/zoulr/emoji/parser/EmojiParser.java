package me.zoulr.emoji.parser;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import me.zoulr.emoji.parser.Callable;

/**
 * Emoji解析器，扫描普通文本并转换其中的Emoji字符
 * @author zoulr@qq.com
 */
public abstract class EmojiParser {
	static enum State {
		TEXT, EMOJI, KEYCAP, ZWJ, FLAG
	}

	/**
	 * 解析过程的主方法，通过一次扫描完成整个处理过程
	 * @param content 包含emoji的字符串
	 * @param callable 解析到emoji字符时回调结果并拼接到解析结果
	 * @return 
	 */
	public static StringBuilder parse(String content, Callable callable) {
		StringBuilder result = new StringBuilder();
		List<Integer> codes = new ArrayList<Integer>(8);
		
		// 解析过程中的当前状态
		State state = State.TEXT;

		// 逐个扫描codePoint
	    char[] chars = null;
	    char[] ach = content.toCharArray();
	    
		for (int i = 0; i < ach.length; i += chars.length) {
			int cp = Character.codePointAt(ach, i);
			chars = Character.toChars(cp);
			
			switch (state) {
				case KEYCAP:
					if (isCDMS(cp)) { // KEYCAP表情完成
						codes.add(cp);
						result.append(callable.assemble(codes));
						codes.clear();
						state = State.TEXT;
						break;
					} else if (isVS(cp)) {
						codes.add(cp);
						break;
					} else {
						appendCodePoints(result, codes); // 前面的数字当做普通字符处理
						codes.clear();
						state = State.TEXT;
					}
				case EMOJI:
					if (!isEmoji(cp)) {
						if (isZWJ(cp)) {
							codes.add(cp);
							state = State.ZWJ;
							break;
						} else if (isVS(cp)) {
							codes.add(cp);
							break;
						} else {
							result.append(callable.assemble(codes));
							codes.clear();
							state = State.TEXT;
						}
					} else {
						if (isSkinCode(cp)) {
							codes.add(cp);
							break;
						} else {
							// 两个emoji实体字符，不做组合，先结束前一个
							result.append(callable.assemble(codes));
							codes.clear();
							state = State.TEXT;
						}
					}
				case ZWJ:
					if (isEmoji(cp)) {
						if (isFlag(cp)) {
							result.append(callable.assemble(codes));
							codes.clear();
							state = State.TEXT;
						} else if (isSkinCode(cp)) {
							result.append(callable.assemble(codes));
							codes.clear();
							state = State.TEXT;
						} else {
							codes.add(cp);
							state = State.EMOJI;
							break;
						}
					} else {
						result.append(callable.assemble(codes));
						codes.clear();
						state = State.TEXT;
					}
				case FLAG:
					if (isFlag(cp)) {
						codes.add(cp);
						result.append(callable.assemble(codes));
						codes.clear();
						state = State.TEXT;
						break;
					} else {
						appendCodePoints(result, codes); // 前面的数字当做普通字符处理
						codes.clear();
						state = State.TEXT;
					}
				case TEXT:
					if (!isEmoji(cp)) {
						result.append(chars);
					} else {
						codes.add(cp);
						if (isKeyCap(cp)) {
							state = State.KEYCAP;
						} else if (isFlag(cp)) {
							state = State.FLAG;
						} else {
							state = State.EMOJI;
						}
					}
					break;
			}
		}
		if (state != State.TEXT) {
			switch (state) {
				case FLAG:
					appendCodePoints(result, codes); // 前面的字符当做普通字符处理
					break;
				case ZWJ:
					result.append(callable.assemble(codes));
					break;
				case EMOJI:
					result.append(callable.assemble(codes));
					break;
				case KEYCAP:
					appendCodePoints(result, codes); // 前面的数字当做普通字符处理
					break;
			}
		}
		return result;
	}
	
	private static void appendCodePoints(StringBuilder builder, List<Integer> cps) {
		for (int cp : cps) {
			builder.append(Character.toChars(cp));
		}
	}
	
	/**
	 * 是否为颜色(皮肤)值 [0x1F3FB - 0x1F3FF]
	 * @return
	 */
	private static boolean isSkinCode(int code) {
		return code >= 0x1F3FB && code <= 0x1F3FF;
	}
	
	private static boolean isEmoji(int codePoint) {
		return Arrays.binarySearch(EmojiCharacter.EMOJI, codePoint) >= 0;
	}
	
	/**
	 * 是否零宽度联接器字符 (Zero-Width-Joiner)
	 * <br> 见http://emojipedia.org/zero-width-joiner/
	 * @return
	 */
	private static boolean isZWJ(int code) {
		return code == 0x200D;
	}
	
	/**
	 * 是否组合用记号字符 (Combining Diacritics Marks for Symbols)
	 * <br> 如U+20E3
	 * <br> 见http://baike.baidu.com/link?url=MOMCYxwbym2HvarYsQsUf0eJaozA-vwz3pgOPIMzae_PPZEemGKw2Sehue_5TA2kdn-TRXBPmj9watRxobUV_q
	 * <br> Combining marks may be applied to emoji, just like they can be applied to other characters. When that is done, the combination should take on an emoji presentation. For example, a 1⃣ is represented as the sequence "1" plus an emoji variation selector plus U+20E3 COMBINING ENCLOSING KEYCAP. Systems are unlikely, however, to support arbitrary combining marks with arbitrary emoji
	 * @return
	 */
	private static boolean isCDMS(int code) {
		//return code >= 0x20D0 && code <= 0x20FF;
		return code == 0x20E0 || code == 0x20E3;
	}
	
	/**
	 * 是否变量选择符 (Variation Selector)
	 * @return
	 */
	private static boolean isVS(int code) {
		return code >= 0xFE00 && code <= 0xFE0F;
	}
	
	/**
	 * 国旗表示符
	 * <br>全世界的国家数少于256，这个范围应该可以满足
	 * <br>国旗emoji用两个以下范围的字符表示
	 * @return
	 */
	private static boolean isFlag(int code) {
		return code >= 0x1F1E6 && code <= 0x1F1FF;
	}
	
	/**
	 *  字符*#0123456789
	 * @return
	 */
	private static boolean isKeyCap(int code) {
		return Arrays.binarySearch(EmojiCharacter.KEYCAP, code) >= 0;
	}
	
}