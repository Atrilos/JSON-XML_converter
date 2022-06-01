package converter.business.tokenizer;

import converter.entity.ParseArray;
import converter.entity.ParseObject;

public class JsonTokenizer {

    private int index;

    private String processString;

    private Object map = null;

    public JsonTokenizer(String string) {
        this.tokenize(string);
    }

    public void tokenize(String str) {
        Object map;
        index = 0;
        processString = sanitizeInput(str);
        char c = processString.charAt(index);

        if (c != '{' && c != '[')
            throw new RuntimeException("Wrong structure of json");
        else if (c == '{')
            map = new ParseObject();
        else
            map = new ParseArray();

        String key = null;
        Object value;
        for (index = 1; index < processString.length(); index++) {
            c = processString.charAt(index);
            if (c <= ' ' || c == ',' || c == '}' || c == ']')
                continue;
            if (key == null && map instanceof ParseObject) {
                key = this.nextKey(c);
            } else {
                if (c == ':')
                    continue;
                value = this.nextValue(c);
                if (!"".equals(value)
                        && (((String) value).charAt(0) == '{' || ((String) value).charAt(0) == '['))
                    value = new JsonTokenizer(String.valueOf(value)).getMap();
                this.put(key, value, map);
                key = null;
            }
        }
        this.map = map;
    }

    private void put(String key, Object value, Object map) {
        if (map instanceof ParseObject)
            ((ParseObject) map).put(key, value);
        else
            ((ParseArray) map).put(value);
    }

    private String sanitizeInput(String str) {
        return str.replaceAll("\\s{2,}", "");
    }

    private String nextValue(final char quote) {
        StringBuilder sb = new StringBuilder();
        char c;
        if (quote != '{' && quote != '[')
            return this.nextKey(quote);
        else {
            int count = 0;
            final char closingQuote = quote == '{' ? '}' : ']';
            for (; index < processString.length(); index++) {
                c = processString.charAt(index);
                if (c == closingQuote) {
                    count--;
                    sb.append(c);
                } else if (c == quote) {
                    count++;
                    sb.append(c);
                } else {
                    sb.append(c);
                }
                if (count == 0)
                    break;
            }
        }
        return sb.toString().matches("^[{\\[]\\s*[}\\]]$") ? "" : sb.toString();
    }

    private String nextKey(char quote) {
        StringBuilder sb = new StringBuilder();
        if (quote == '"' || quote == '\'') {
            index++;
            for (; ; ) {
                char c = processString.charAt(index);
                if (c == quote) {
                    break;
                }
                sb.append(c);
                index++;
            }
        } else {
            for (; ; ) {
                char c = processString.charAt(index);
                if (
                        c == ',' || c == ' ' || c == '\n' || c == '}' || c == '\r' || c == ']'
                ) {
                    break;
                }
                sb.append(c);
                index++;
            }
        }
        return sb.toString();
    }

    public Object getMap() {
        return map;
    }
}
