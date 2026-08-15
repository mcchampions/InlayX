package me.qscbm.inlayx.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.jspecify.annotations.NonNull;

/**
 * 保存时保留配置文件注释的Yaml实例
 * 会以KV的形式存储
 * ”'comment   # 注释内容': comment“的格式存储
 */
public class CommentConfiguration extends YamlConfiguration {
    protected static String commentPrefixSymbol = "'comment ";
    protected static String commentSuffixSymbol = "': comment";
    protected static String fromRegex = "( *)(#.*)";
    protected static Pattern fromPattern = Pattern.compile(fromRegex);
    protected static String toRegex =
            "( *)(- )*" + "(" + commentPrefixSymbol + ")" + "(#.*)" + "(" + commentSuffixSymbol + ")";
    protected static Pattern toPattern = Pattern.compile(toRegex);
    protected static Pattern countSpacePattern = Pattern.compile("( *)(- )*(.*)");
    protected static int commentSplitWidth = 90;

    private static String[] split(String string, int partLength) {
        String[] array = new String[string.length() / partLength + 1];
        for (int i = 0; i < array.length; i++) {
            int beginIndex = i * partLength;
            int endIndex = beginIndex + partLength;
            if (endIndex > string.length()) {
                endIndex = string.length();
            }
            array[i] = string.substring(beginIndex, endIndex);
        }
        return array;
    }

    @Override
    public void loadFromString(String contents) throws InvalidConfigurationException {
        String[] parts = contents.split("\n");
        List<String> lastComments = new ArrayList<>();
        StringBuilder builder = new StringBuilder();
        for (String part : parts) {
            Matcher matcher = fromPattern.matcher(part);
            if (matcher.find()) {
                String originComment = matcher.group(2);
                String[] splitComments = split(originComment, commentSplitWidth);
                for (int i = 0; i < splitComments.length; i++) {
                    String comment = splitComments[i];
                    if (i == 0) {
                        comment = comment.substring(1);
                    }
                    comment = "# " + comment;
                    lastComments.add(
                            comment.replaceAll("\\.", "．").replace("'", "＇").replace(":", "："));
                }
            } else {
                matcher = countSpacePattern.matcher(part);
                if (matcher.find() && !lastComments.isEmpty()) {
                    for (String comment : lastComments) {
                        builder.append(matcher.group(1));
                        builder.append(this.checkNull(matcher.group(2)));
                        builder.append(commentPrefixSymbol);
                        builder.append(comment);
                        builder.append(commentSuffixSymbol);
                        builder.append("\n");
                    }
                    lastComments.clear();
                }
                builder.append(part);
                builder.append("\n");
            }
        }
        super.loadFromString(builder.toString());
    }

    @Override
    public @NonNull String saveToString() {
        String contents = super.saveToString();
        StringBuilder savcontent = new StringBuilder();
        String[] parts = contents.split("\n");
        for (String part : parts) {
            Matcher matcher = toPattern.matcher(part);
            if (matcher.find() && matcher.groupCount() == 5) {
                part = this.checkNull(matcher.group(1)) + matcher.group(4);
            }
            savcontent.append(part.replace("．", ".").replace("＇", "'").replace("：", ":"));
            savcontent.append("\n");
        }
        return savcontent.toString();
    }

    /**
     * 获取指定 key 同级且位于其上方的注释.
     */
    public String getComment(String key) {
        List<String> comments = collectCommentLines(key);
        return comments.isEmpty() ? null : String.join("\n", comments);
    }

    private List<String> collectCommentLines(String key) {
        if (key == null || key.isEmpty()) {
            return List.of();
        }
        int lastDot = key.lastIndexOf('.');
        ConfigurationSection section;
        String keyName;
        if (lastDot < 0) {
            section = this;
            keyName = key;
        } else {
            section = getConfigurationSection(key.substring(0, lastDot));
            keyName = key.substring(lastDot + 1);
        }
        if (section == null) {
            return List.of();
        }
        String loadedCommentPrefix = commentPrefixSymbol.substring(1);
        List<String> comments = new ArrayList<>();
        for (String name : section.getKeys(false)) {
            if (name.equals(keyName)) {
                return comments;
            }
            if (name.startsWith(loadedCommentPrefix) && "comment".equals(section.get(name))) {
                comments.add(name.substring(loadedCommentPrefix.length())
                        .replace("．", ".")
                        .replace("＇", "'")
                        .replace("：", ":"));
            } else {
                comments.clear();
            }
        }
        return List.of();
    }

    private String checkNull(String string) {
        return string == null ? "" : string;
    }
}
