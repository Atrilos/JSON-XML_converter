package converter.entity;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class ParseObject implements Iterable<Map.Entry<String, Object>> {

    private Map<String, Object> map;

    public ParseObject() {
        this.map = new LinkedHashMap<>();
    }

    public ParseObject(ParseObject po) {
        this.map = po.map;
    }

    public ParseObject(Map.Entry<String, Object> entry) {
        this();
        this.map.put(entry.getKey(), entry.getValue());
    }

    static void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    static void writeValue(Writer writer, Object value,
                           int indentFactor, int indent) throws IOException {
        if (value instanceof String && value.equals("null")) {
            writer.write("null");
        } else if (value instanceof String) {
            writer.write('"');
            writer.write(value.toString());
            writer.write('"');
        } else if (value instanceof ParseObject) {
            ((ParseObject) value).write(writer, indentFactor, indent);
        } else if (value instanceof ParseArray) {
            ((ParseArray) value).write(writer, indentFactor, indent);
        } else {
            quote(value.toString(), writer);
        }
    }

    public static String quote(String string) {
        StringWriter sw = new StringWriter();
        synchronized (sw.getBuffer()) {
            try {
                return quote(string, sw).toString();
            } catch (IOException ignored) {
                // will never happen - we are writing to a string writer
                return "";
            }
        }
    }

    public static Writer quote(String string, Writer w) throws IOException {
        if (string == null || string.isEmpty()) {
            w.write("\"\"");
            return w;
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.write('\\');
                    w.write(c);
                    break;
                case '/':
                    if (b == '<') {
                        w.write('\\');
                    }
                    w.write(c);
                    break;
                case '\b':
                    w.write("\\b");
                    break;
                case '\t':
                    w.write("\\t");
                    break;
                case '\n':
                    w.write("\\n");
                    break;
                case '\f':
                    w.write("\\f");
                    break;
                case '\r':
                    w.write("\\r");
                    break;
                default:
                    if (c < ' ' || (c >= '\u0080' && c < '\u00a0')
                            || (c >= '\u2000' && c < '\u2100')) {
                        w.write("\\u");
                        hhhh = Integer.toHexString(c);
                        w.write("0000", 0, 4 - hhhh.length());
                        w.write(hhhh);
                    } else {
                        w.write(c);
                    }
            }
        }
        w.write('"');
        return w;
    }

    public void put(String key, Object value) {
        if (value != null) {
            this.map.put(key, value);
        } else {
            this.remove(key);
        }
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return map.entrySet().iterator();
    }

    public int size() {
        return map.size();
    }

    public Object get(String key) {
        return map.get(key);
    }

    public boolean isEmpty() {
        return map.size() == 0;
    }

    public Set<String> keySet() {
        return map.keySet();
    }

    public boolean containsKey(String key) {
        return map.containsKey(key);
    }

    public void remove(String key) {
        map.remove(key);
    }

    @Override
    public String toString() {
        try {
            return this.toString(0);
        } catch (Exception e) {
            return null;
        }
    }

    public String toString(int indentFactor) {
        StringWriter w = new StringWriter();
        synchronized (w.getBuffer()) {
            return this.write(w, indentFactor, 0).toString();
        }
    }

    private Writer write(Writer writer, int indentFactor, int indent) {
        try {
            boolean needsComma = false;
            final int length = this.size();
            writer.write("{\n");

            if (length == 1) {
                final Map.Entry<String, ?> entry = this.iterator().next();
                final String key = entry.getKey();
                writer.write(quote(key));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                writeValue(writer, entry.getValue(), indentFactor, indent);
            } else if (length != 0) {
                final int newIndent = indent + indentFactor;
                for (final Map.Entry<String, ?> entry : this.entrySet()) {
                    if (needsComma) {
                        writer.write(',');
                    }
                    if (indentFactor > 0) {
                        writer.write('\n');
                    }
                    indent(writer, newIndent);
                    final String key = entry.getKey();
                    writer.write(quote(key));
                    writer.write(':');
                    if (indentFactor > 0) {
                        writer.write(' ');
                    }
                    writeValue(writer, entry.getValue(), indentFactor, newIndent);

                    needsComma = true;
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                indent(writer, indent);
            }
            writer.write('}');
            return writer;
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    public ParseObject filterInput(String parent) {
        ParseObject result = new ParseObject();
        String key;
        Object value;
        boolean violation = false;
        for (var iter = this.iterator();
             iter.hasNext(); ) {
            var entry = iter.next();
            key = entry.getKey();
            value = entry.getValue();
            if ("".equals(key) || "@".equals(key) || "#".equals(key)) {
                violation = true;
                iter.remove();
            } else if (!this.containsKey("#" + parent) && !"".equals(parent)) {
                violation = true;
            } else if (!(key.startsWith("#")) && !(key.startsWith("@"))) {
                violation = true;
            } else if (key.startsWith("@") && (value instanceof ParseObject || value instanceof ParseArray)) {
                violation = true;
            }
        }
        if (violation)
            this.map = fixException(this.map);
        for (Map.Entry<String, Object> entry : this.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if (value instanceof ParseObject) {
                value = new ParseObject((ParseObject) value).filterInput(key);
                if (((ParseObject) value).isEmpty())
                    value = "";
            } else if (value instanceof ParseArray) {
                value = new ParseArray((ParseArray) value).filterInput();
                if (((ParseArray) value).isEmpty())
                    value = "";
            }
            if (key.equals("#" + parent))
                key = "content";
            result.put(key, value);
        }
        return result;
    }

    private Map<String, Object> fixException(Map<String, Object> jm) {
        String key;
        Object value;
        Map<String, Object> tempMap = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : jm.entrySet()) {
            key = entry.getKey();
            if (key.startsWith("#") || key.startsWith("@")) {
                key = key.replaceAll("^#|^@", "");
                if (jm.containsKey(key))
                    continue;
            }
            value = entry.getValue();
            tempMap.put(key, value);
        }
        return tempMap;
    }

    public void accumulate(String key, Object value) {
        Object object = this.opt(key);
        if (object == null) {
            this.put(key,
                    value instanceof ParseArray ? new ParseArray().put(value)
                            : value);
        } else if (object instanceof ParseArray) {
            ((ParseArray) object).put(value);
        } else {
            this.put(key, new ParseArray().put(object).put(value));
        }
    }

    private Object opt(String key) {
        return key == null ? null : this.map.get(key);
    }

    public Object filterJsonInput(String parent) {
        ParseObject result = new ParseObject();
        String key;
        Object value;
        for (Map.Entry<String, Object> entry : this.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            if (value instanceof ParseObject) {
                value = new ParseObject((ParseObject) value).filterJsonInput(key);
            } else if (value instanceof ParseArray) {
                value = new ParseArray((ParseArray) value).filterJsonInput();
                if (this.size() == 1)
                    return value;
                else
                    key = '#' + parent;
            }
            result.put(key, value);
        }
        return result;
    }
}
