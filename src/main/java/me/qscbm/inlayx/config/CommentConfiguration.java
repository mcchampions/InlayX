package me.qscbm.inlayx.config;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.NonNull;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

/**
 * 保存时保留配置文件注释的Yaml实例
 * 会以KV的形式存储
 * ”'__comment__ # 注释内容': comment“的格式存储
 */
public class CommentConfiguration extends YamlConfiguration {
    protected static final String COMMENT_PREFIX_SYMBOL = "'__comment__";
    protected static final String COMMENT_SUFFIX_SYMBOL = "': comment";
    protected static final String COMMENT_INDEX_SEPARATOR = " ";
    protected static final String COMMENT_BODY_PREFIX = "# ";
    protected static final Pattern LOADED_COMMENT_PATTERN = Pattern.compile("^" + COMMENT_PREFIX_SYMBOL.substring(1)
            + "(\\d+)" + COMMENT_INDEX_SEPARATOR + COMMENT_BODY_PREFIX + "(.*)$");
    protected static final String FROM_REGEX = "( *)(#.*)";
    protected static final Pattern FROM_PATTERN = Pattern.compile(FROM_REGEX);
    protected static final String TO_REGEX =
            "( *)(- )*" + COMMENT_PREFIX_SYMBOL + "(\\d+)" + COMMENT_INDEX_SEPARATOR + "(# .*)" + COMMENT_SUFFIX_SYMBOL;
    protected static final Pattern TO_PATTERN = Pattern.compile(TO_REGEX);
    protected static final Pattern COUNT_SPACE_PATTERN = Pattern.compile("( *)(- )*(.*)");
    protected static final int COMMENT_SPLIT_WIDTH = 250;
    private int commentIndex;

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
        int nextCommentIndex = 0;
        for (String part : parts) {
            Matcher matcher = FROM_PATTERN.matcher(part);
            if (matcher.find()) {
                String originComment = matcher.group(2);
                String[] splitComments = split(originComment, COMMENT_SPLIT_WIDTH);
                for (int i = 0; i < splitComments.length; i++) {
                    String comment = splitComments[i];
                    if (i == 0) {
                        comment = "#" + comment.substring(1);
                    } else {
                        comment = "# " + comment;
                    }
                    lastComments.add(comment.replace(".", "．").replace("'", "＇").replace(":", "："));
                }
            } else {
                matcher = COUNT_SPACE_PATTERN.matcher(part);
                if (matcher.find() && !lastComments.isEmpty()) {
                    for (String comment : lastComments) {
                        builder.append(matcher.group(1));
                        builder.append(this.checkNull(matcher.group(2)));
                        builder.append(COMMENT_PREFIX_SYMBOL);
                        builder.append(nextCommentIndex++);
                        builder.append(COMMENT_INDEX_SEPARATOR);
                        builder.append(comment);
                        builder.append(COMMENT_SUFFIX_SYMBOL);
                        builder.append("\n");
                    }
                    lastComments.clear();
                }
                builder.append(part);
                builder.append("\n");
            }
        }
        this.commentIndex = nextCommentIndex;
        super.loadFromString(builder.toString());
    }

    @Override
    public @NonNull String saveToString() {
        String contents = super.saveToString();
        StringBuilder savcontent = new StringBuilder();
        String[] parts = contents.split("\n");
        for (String part : parts) {
            Matcher matcher = TO_PATTERN.matcher(part);
            if (matcher.find() && matcher.groupCount() == 4) {
                part = this.checkNull(matcher.group(1)) + this.checkNull(matcher.group(2)) + matcher.group(4);
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

    /**
     * 设置注释
     */
    public void insertComment(String key, String value) {
        if (key == null || key.isEmpty() || value == null) {
            return;
        }
        int lastDot = key.lastIndexOf('.');
        ConfigurationSection section;
        if (lastDot < 0) {
            section = this;
        } else {
            section = getConfigurationSection(key.substring(0, lastDot));
        }
        if (section == null) {
            return;
        }
        List<String> comments = List.of(value.split("\n"));
        for (String comment : comments) {
            comment = comment.replace(".", "．").replace("'", "＇").replace(":", "：");
            section.set(
                    COMMENT_PREFIX_SYMBOL.substring(1)
                            + nextCommentIndex(section)
                            + COMMENT_INDEX_SEPARATOR
                            + COMMENT_BODY_PREFIX
                            + comment,
                    "comment");
        }
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
        List<String> comments = new ArrayList<>();
        for (String name : section.getKeys(false)) {
            if (name.equals(keyName)) {
                return comments;
            }
            Matcher matcher = LOADED_COMMENT_PATTERN.matcher(name);
            if (matcher.matches() && "comment".equals(section.get(name))) {
                comments.add(
                        matcher.group(2).replace("．", ".").replace("＇", "'").replace("：", ":"));
            } else {
                comments.clear();
            }
        }
        return List.of();
    }

    public static boolean isCommentByKeys(String key) {
        return key != null && LOADED_COMMENT_PATTERN.matcher(key).matches();
    }

    private int nextCommentIndex(ConfigurationSection section) {
        int next = commentIndex;
        for (String name : section.getKeys(false)) {
            Matcher matcher = LOADED_COMMENT_PATTERN.matcher(name);
            if (matcher.matches()) {
                try {
                    next = Math.max(next, Integer.parseInt(matcher.group(1)) + 1);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        commentIndex = next;
        return commentIndex++;
    }

    private String checkNull(String string) {
        return string == null ? "" : string;
    }
}
