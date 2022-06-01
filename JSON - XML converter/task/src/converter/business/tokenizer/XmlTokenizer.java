package converter.business.tokenizer;

import converter.entity.ParseObject;

import java.util.Map;

public class XmlTokenizer {

    public static final Character EQ = '=';
    public static final Character GT = '>';
    public static final Character LT = '<';
    public static final Character SLASH = '/';
    public static final Character QUEST = '?';

    private final String processString;

    private ParseObject map = new ParseObject();

    private int index;

    public XmlTokenizer(String string) {
        this.processString = string.trim();
        this.tokenize();
    }

    public boolean parse(ParseObject context, String name) {
        Object token;
        String tagName;
        String string;
        ParseObject xmlObject;

        token = nextToken();
        if (token == QUEST) {
            // <?
            skipPast("?>");
            return false;
        } else if (token == SLASH) {
            // Close tag </
            token = nextToken();
            if (name == null) {
                throw new RuntimeException("Mismatched close tag " + token);
            }
            if (!token.equals(name)) {
                throw new RuntimeException("Mismatched " + name + " and " + token);
            }
            index++;
            return true;
        } else {
            tagName = (String) token;
            token = null;
            xmlObject = new ParseObject();

            for (; ; ) {
                if (token == null) {
                    token = nextToken();
                }
                // attribute = value
                if (token instanceof String) {
                    string = (String) token;
                    token = nextToken();
                    if (token == EQ) {
                        token = nextToken();
                        xmlObject.accumulate("@" + string, token);
                        token = null;
                    } else {
                        xmlObject.accumulate(string, "");
                    }

                } else if (token == SLASH) {
                    // Empty tag <.../>
                    index++;
                    if (xmlObject.size() > 0 && xmlObject.get("#" + tagName) == null) {
                        xmlObject.accumulate("#" + tagName, "null");
                        context.accumulate(tagName, xmlObject);
                    } else {
                        context.accumulate(tagName, "null");
                    }
                    return false;

                } else if (token == GT) {
                    // Content, between <...> and </...>
                    for (; ; ) {
                        token = nextContent();
                        if (token == null) {
                            return false;
                        } else if (token instanceof String) {
                            string = (String) token;
                            xmlObject.accumulate("#" + tagName, string);

                        } else if (token == LT) {
                            // Nested element
                            if (parse(xmlObject, tagName)) {
                                ParseObject tmp = new ParseObject(xmlObject);
                                if (xmlObject.isEmpty()) {
                                    context.accumulate(tagName, "");
                                } else if (xmlObject.size() == 1
                                        && xmlObject.get("#" + tagName) != null) {
                                    context.accumulate(tagName, xmlObject.get("#" + tagName));
                                } else if (xmlObject.keySet().stream().allMatch(s -> s.startsWith("@"))) {
                                    xmlObject.accumulate("#" + tagName, "");
                                    context.accumulate(tagName, xmlObject);
                                } else if (tmp.entrySet().stream().anyMatch(e -> e.getKey().startsWith("@"))
                                        && tmp.entrySet().stream().noneMatch(e -> e.getKey().startsWith("#"))) {
                                    tmp = new ParseObject();
                                    for (Map.Entry<String, Object> entry : xmlObject) {
                                        if (entry.getKey().startsWith("@"))
                                            tmp.accumulate(entry.getKey(), entry.getValue());
                                        else
                                            tmp.accumulate('#' + tagName, new ParseObject(entry));
                                    }
                                    context.accumulate(tagName, tmp);
                                } else {
                                    context.accumulate(tagName, xmlObject);
                                }
                                return false;
                            }
                        }
                    }
                }
            }
        }
    }

    public void tokenize() {
        ParseObject map = new ParseObject();
        index = 0;

        while (more()) {
            skipPast("<");
            if (more()) {
                parse(map, null);
            }
        }

        this.map = map;
    }

    private boolean more() {
        return index < processString.length();
    }

    private void skipPast(String to) {
        boolean b;
        char c;
        int i;
        int j;
        int offset = 0;
        int length = to.length();
        char[] circle = new char[length];

        /*
         * First fill the circle buffer with as many characters as are in the
         * to string. If we reach an early end, bail.
         */

        for (i = 0; i < length; i += 1) {
            c = processString.charAt(index++);
            if (c == 0) {
                return;
            }
            circle[i] = c;
        }

        /* We will loop, possibly for all the remaining characters. */

        for (; ; ) {
            j = offset;
            b = true;

            /* Compare the circle buffer with the to string. */

            for (i = 0; i < length; i += 1) {
                if (circle[j] != to.charAt(i)) {
                    b = false;
                    break;
                }
                j += 1;
                if (j >= length) {
                    j -= length;
                }
            }

            /* If we exit the loop with b intact, then victory is ours. */

            if (b) {
                return;
            }

            /* Get the next character. If there isn't one, then defeat is ours. */

            c = processString.charAt(index++);
            if (c == 0) {
                return;
            }
            /*
             * Shove the character in the circle buffer and advance the
             * circle offset. The offset is mod n.
             */
            circle[offset] = c;
            offset += 1;
            if (offset >= length) {
                offset -= length;
            }
        }
    }

    private Object nextToken() {
        char c, q;
        StringBuilder sb;
        do {
            c = processString.charAt(index++);
        } while (Character.isWhitespace(c));
        switch (c) {
            case '>':
                return GT;
            case '/':
                return SLASH;
            case '=':
                return EQ;
            case '?':
                return QUEST;

            case '"':
            case '\'':
                q = c;
                sb = new StringBuilder();
                for (; ; ) {
                    c = processString.charAt(index++);
                    if (c == q)
                        return sb.toString();
                    else
                        sb.append(c);
                }
            default:
                sb = new StringBuilder();
                for (; ; ) {
                    sb.append(c);
                    c = processString.charAt(index++);
                    if (Character.isWhitespace(c)) {
                        return sb.toString();
                    }
                    switch (c) {
                        case 0:
                            return sb.toString();
                        case '>':
                        case '/':
                        case '=':
                            --index;
                            return sb.toString();
                    }
                }
        }
    }

    public Object nextContent() {
        char c;
        StringBuilder sb;
        do {
            c = processString.charAt(index++);
        } while (Character.isWhitespace(c));
        if (c == 0) {
            return null;
        }
        if (c == '<') {
            return LT;
        }
        sb = new StringBuilder();
        for (; ; ) {
            if (c == 0) {
                return sb.toString().trim();
            }
            if (c == '<') {
                --index;
                return sb.toString().trim();
            }
            sb.append(c);
            c = processString.charAt(index++);
        }
    }

    public ParseObject getMap() {
        return map;
    }
}
