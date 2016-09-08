import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import me.zoulr.emoji.parser.Callable;
import me.zoulr.emoji.parser.EmojiParser;


public class EmojiParserTest {
	private Callable caller;
	
	@Before
	public void init() {
		caller = new Callable() {
			@Override
			public String assemble(List<Integer> emojiCodePoints) {
				StringBuilder builder = new StringBuilder("<img src='http://v14.pcbaby.com.cn/emoji/");
				for (int i = 0; i < emojiCodePoints.size(); i++) {
					int cp = emojiCodePoints.get(i);
					if (cp > 0xFFFF) {
						builder.append(Integer.toHexString(emojiCodePoints.get(i)));
					} else {
						builder.append(String.format("%1$04x", emojiCodePoints.get(i)));
					}
					if (i < emojiCodePoints.size() - 1) {
						builder.append("-");
					}
				}
				builder.append(".png' />");
				return builder.toString();
			}
		};
	}
	
	@Test
	public void emoji2String() {
		StringBuilder builder = new StringBuilder();
     	int[] cps = {0x1F6C0, 0x1F3FD, 0x1F6C0, 0x1F3FD};
     	for (int cp : cps) {
     		builder.append(Character.toChars(cp));
     	}
     	
     	System.out.println(EmojiParser.parse(builder.toString(), caller).toString());
     	//128105,8205,10084,65039,8205,128139,8205,128104
     	String result = "<img src='http://v14.pcbaby.com.cn/emoji/1f6c0-1f3fd.png' />"
     			+ "<img src='http://v14.pcbaby.com.cn/emoji/1f6c0-1f3fd.png' />";
     	Assert.assertEquals(EmojiParser.parse(builder.toString(), caller).toString(), result);
	}
}
