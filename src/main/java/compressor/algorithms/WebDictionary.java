package compressor.algorithms;

import java.util.HashMap;
import java.util.Map;

public class WebDictionary {

    private static final Map<String, String> HTML_TAGS = new HashMap<>();
    private static final Map<String, String> CSS_PROPERTIES = new HashMap<>();
    private static final Map<String, String> JS_KEYWORDS = new HashMap<>();
    private static final Map<String, String> COMMON_ATTRIBUTES = new HashMap<>();

    static {
        // HTML标签映射（使用单个字符替换）
        HTML_TAGS.put("<html>", "\u0001");
        HTML_TAGS.put("</html>", "\u0002");
        HTML_TAGS.put("<head>", "\u0003");
        HTML_TAGS.put("</head>", "\u0004");
        HTML_TAGS.put("<body>", "\u0005");
        HTML_TAGS.put("</body>", "\u0006");
        HTML_TAGS.put("<div>", "\u0007");
        HTML_TAGS.put("</div>", "\u0008");
        HTML_TAGS.put("<span>", "\u0009");
        HTML_TAGS.put("</span>", "\u000F");
        HTML_TAGS.put("<p>", "\u0010");
        HTML_TAGS.put("</p>", "\u0011");
        HTML_TAGS.put("<a ", "\u0012");
        HTML_TAGS.put("<img ", "\u0013");
        HTML_TAGS.put("<ul>", "\u0014");
        HTML_TAGS.put("</ul>", "\u0015");
        HTML_TAGS.put("<li>", "\u0016");
        HTML_TAGS.put("</li>", "\u0017");
        HTML_TAGS.put("<table>", "\u0018");
        HTML_TAGS.put("</table>", "\u0019");
        HTML_TAGS.put("<tr>", "\u001A");
        HTML_TAGS.put("</tr>", "\u001B");
        HTML_TAGS.put("<td>", "\u001C");
        HTML_TAGS.put("</td>", "\u001D");
        HTML_TAGS.put("<form ", "\u001E");
        HTML_TAGS.put("<input ", "\u001F");
        HTML_TAGS.put("<button", "\u0020");
        HTML_TAGS.put("<script>", "\u0021");
        HTML_TAGS.put("</script>", "\u005B");
        HTML_TAGS.put("<style>", "\u0023");
        HTML_TAGS.put("</style>", "\u0024");
        HTML_TAGS.put("<link ", "\u0025");
        HTML_TAGS.put("<meta ", "\u0026");
        HTML_TAGS.put("<title>", "\u0027");
        HTML_TAGS.put("</title>", "\u0028");
        HTML_TAGS.put("<header>", "\u0029");
        HTML_TAGS.put("</header>", "\u002A");
        HTML_TAGS.put("<nav>", "\u002B");
        HTML_TAGS.put("</nav>", "\u002C");
        HTML_TAGS.put("<footer>", "\u002D");
        HTML_TAGS.put("</footer>", "\u002E");
        HTML_TAGS.put("<main>", "\u002F");
        HTML_TAGS.put("</main>", "\u0030");
        HTML_TAGS.put("<section>", "\u0031");
        HTML_TAGS.put("</section>", "\u0032");
        HTML_TAGS.put("<article>", "\u0033");
        HTML_TAGS.put("</article>", "\u0034");
        HTML_TAGS.put("<aside>", "\u0035");
        HTML_TAGS.put("</aside>", "\u0036");
        HTML_TAGS.put("<h1>", "\u0037");
        HTML_TAGS.put("</h1>", "\u0038");
        HTML_TAGS.put("<h2>", "\u0039");
        HTML_TAGS.put("</h2>", "\u003A");
        HTML_TAGS.put("<h3>", "\u003B");
        HTML_TAGS.put("</h3>", "\u003C");
        HTML_TAGS.put("<h4>", "\u003D");
        HTML_TAGS.put("</h4>", "\u003E");
        HTML_TAGS.put("<br>", "\u003F");
        HTML_TAGS.put("<hr>", "\u0040");

        // CSS属性映射
        CSS_PROPERTIES.put("class=", "\u0041");
        CSS_PROPERTIES.put("id=", "\u0042");
        CSS_PROPERTIES.put("style=", "\u0043");
        CSS_PROPERTIES.put("src=", "\u0044");
        CSS_PROPERTIES.put("href=", "\u0045");
        CSS_PROPERTIES.put("type=", "\u0046");
        CSS_PROPERTIES.put("name=", "\u0047");
        CSS_PROPERTIES.put("value=", "\u0048");
        CSS_PROPERTIES.put("placeholder=", "\u0049");
        CSS_PROPERTIES.put("disabled", "\u004A");
        CSS_PROPERTIES.put("required", "\u004B");
        CSS_PROPERTIES.put("checked", "\u004C");
        CSS_PROPERTIES.put("selected", "\u004D");
        CSS_PROPERTIES.put("hidden", "\u004E");
        CSS_PROPERTIES.put("readonly", "\u004F");
        CSS_PROPERTIES.put("autocomplete=", "\u0050");
        CSS_PROPERTIES.put("maxlength=", "\u0051");
        CSS_PROPERTIES.put("data-", "\u0052");
        CSS_PROPERTIES.put("aria-", "\u0053");
        CSS_PROPERTIES.put("onclick=", "\u0054");
        CSS_PROPERTIES.put("onchange=", "\u0055");
        CSS_PROPERTIES.put("onsubmit=", "\u0056");
        CSS_PROPERTIES.put("onload=", "\u0057");
        CSS_PROPERTIES.put("onerror=", "\u0058");

        // JavaScript关键字映射
        JS_KEYWORDS.put("function", "\u0059");
        JS_KEYWORDS.put("var ", "\u005A");
        JS_KEYWORDS.put("let ", "\u005B");
        JS_KEYWORDS.put("const ", "\u007D");
        JS_KEYWORDS.put("return ", "\u005D");
        JS_KEYWORDS.put("if (", "\u005E");
        JS_KEYWORDS.put("else {", "\u005F");
        JS_KEYWORDS.put("for (", "\u0060");
        JS_KEYWORDS.put("while (", "\u0061");
        JS_KEYWORDS.put("switch (", "\u0062");
        JS_KEYWORDS.put("case ", "\u0063");
        JS_KEYWORDS.put("break;", "\u0064");
        JS_KEYWORDS.put("continue;", "\u0065");
        JS_KEYWORDS.put("try {", "\u0066");
        JS_KEYWORDS.put("catch (", "\u0067");
        JS_KEYWORDS.put("finally {", "\u0068");
        JS_KEYWORDS.put("throw ", "\u0069");
        JS_KEYWORDS.put("async ", "\u006A");
        JS_KEYWORDS.put("await ", "\u006B");
        JS_KEYWORDS.put("=>", "\u006C");
        JS_KEYWORDS.put("new ", "\u006D");
        JS_KEYWORDS.put("this.", "\u006E");
        JS_KEYWORDS.put("null", "\u006F");
        JS_KEYWORDS.put("undefined", "\u0070");
        JS_KEYWORDS.put("true", "\u0071");
        JS_KEYWORDS.put("false", "\u0072");

        // 通用文本片段
        COMMON_ATTRIBUTES.put("http://", "\u0073");
        COMMON_ATTRIBUTES.put("https://", "\u0074");
        COMMON_ATTRIBUTES.put("www.", "\u0075");
        COMMON_ATTRIBUTES.put("<!--", "\u0076");
        COMMON_ATTRIBUTES.put("-->", "\u0077");
        COMMON_ATTRIBUTES.put("charset=", "\u0078");
        COMMON_ATTRIBUTES.put("viewport", "\u0079");
        COMMON_ATTRIBUTES.put("utf-8", "\u007A");
        COMMON_ATTRIBUTES.put("text/html", "\u007B");
        COMMON_ATTRIBUTES.put("application/json", "\u007C");
    }

    public static Map<String, String> getHtmlTags() {
        return HTML_TAGS;
    }

    public static Map<String, String> getCssProperties() {
        return CSS_PROPERTIES;
    }

    public static Map<String, String> getJsKeywords() {
        return JS_KEYWORDS;
    }

    public static Map<String, String> getCommonAttributes() {
        return COMMON_ATTRIBUTES;
    }

    public static Map<String, String> getAllMappings() {
        Map<String, String> all = new HashMap<>();
        all.putAll(HTML_TAGS);
        all.putAll(CSS_PROPERTIES);
        all.putAll(JS_KEYWORDS);
        all.putAll(COMMON_ATTRIBUTES);
        return all;
    }

    public static int getTotalTagCount() {
        return HTML_TAGS.size() + CSS_PROPERTIES.size() + JS_KEYWORDS.size() + COMMON_ATTRIBUTES.size();
    }
}
