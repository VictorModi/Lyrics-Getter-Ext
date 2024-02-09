package statusbar.finder.provider.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UnicodeUtil {
    // https://blog.csdn.net/JodenHe/article/details/77343197
    public static String unicodeStr2String(String unicodeStr) {
        int length = unicodeStr.length();
        int count = 0;
        //正则匹配条件，可匹配“\\u”1到4位，一般是4位可直接使用 String regex = "\\\\u[a-f0-9A-F]{4}";
        String regex = "\\\\u[a-f0-9A-F]{1,4}";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(unicodeStr);
        StringBuffer sb = new StringBuffer();

        while(matcher.find()) {
            String oldChar = matcher.group();//原本的Unicode字符
            String newChar = unicode2String(oldChar);//转换为普通字符
            // int index = unicodeStr.indexOf(oldChar);
            // 在遇见重复出现的unicode代码的时候会造成从源字符串获取非unicode编码字符的时候截取索引越界等
            int index = matcher.start();

            sb.append(unicodeStr.substring(count, index));//添加前面不是unicode的字符
            sb.append(newChar);//添加转换后的字符
            count = index+oldChar.length();//统计下标移动的位置
        }
        sb.append(unicodeStr.substring(count, length));//添加末尾不是Unicode的字符
        return sb.toString();
    }

    /**
     * 字符串转换unicode
     * @param string
     * @return
     */
    public static String string2Unicode(String string) {
        StringBuilder unicode = new StringBuilder();
        for (int i = 0; i < string.length(); i++) {
            // 取出每一个字符
            char c = string.charAt(i);
            // 转换为unicode
            unicode.append("\\u").append(Integer.toHexString(c));
        }

        return unicode.toString();
    }

    /**
     * unicode 转字符串
     * @param unicode 全为 Unicode 的字符串
     * @return
     */
    public static String unicode2String(String unicode) {
        StringBuilder string = new StringBuilder();
        String[] hex = unicode.split("\\\\u");

        for (int i = 1; i < hex.length; i++) {
            // 转换出每一个代码点
            int data = Integer.parseInt(hex[i], 16);
            // 追加成string
            string.append((char) data);
        }

        return string.toString();

    }
}
