package converter.business.converters;

import converter.business.Converter;
import converter.entity.ParseArray;
import converter.entity.ParseObject;

import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

public class JSON extends Converter {

    private final StringBuilder sb = new StringBuilder();
    private String parent = "";

    public JSON(Object tokens) {
        super(tokens);
    }

    public JSON(Object tokens, String parent) {
        super(tokens);
        this.parent = parent;
    }

    @Override
    public String parse() {
        input = ((ParseObject) input).filterJsonInput(null);
        return ((ParseObject) input).toString(4);
    }

    public String parseWithoutFilter() {
        if (input instanceof ParseObject)
            return parseWithoutFilter((ParseObject) input);
        else
            return parseWithoutFilter((ParseArray) input);
    }

    public String parseWithoutFilter(ParseObject po) {
        if (po.containsKey("content") && po.get("content") instanceof String) {
            String value = '"' + (String) po.get("content") + '"';
            sb.append("value = ").append(value).append("\n");
            po.remove("content");
        }
        var iter = po.iterator();
        ParseObject attributes = new ParseObject();
        while (iter.hasNext()) {
            var current = iter.next();
            if (current.getKey().startsWith("@")) {
                attributes.put(current.getKey().substring(1), String.valueOf(current.getValue()));
                iter.remove();
            }
        }
        if (!attributes.isEmpty())
            parseAttributes(attributes);
        else if (!Objects.equals(parent, ""))
            sb.append("\n");
        iter = po.iterator();
        while (iter.hasNext()) {
            parseDeeper(iter.next());
        }
        return sb.toString();
    }

    private String parseWithoutFilter(ParseArray pa) {
        Iterator<Object> iter;

        if (!Objects.equals(parent, ""))
            sb.append("\n");
        iter = pa.iterator();
        while (iter.hasNext()) {
            parseDeeper(iter.next());
        }
        return sb.toString();
    }

    private void parseAttributes(ParseObject attributes) {
        sb.append("attributes:\n");
        for (var entry : attributes.entrySet()) {
            sb.append(entry.getKey()).append(" = ");
            sb.append('"').append(entry.getValue()).append('"').append("\n");
        }
        sb.append("\n");
    }

    public void parseDeeper(Map.Entry<String, Object> entry) {
        String fullPath = parent;
        String path = entry.getKey();
        if (!path.equals("content")) {
            fullPath += path;
            sb.append("Element:\n").append("path = ").append(fullPath).append("\n");
            fullPath += ", ";
        }
        if (entry.getValue() instanceof ParseObject || entry.getValue() instanceof ParseArray) {
            sb.append(new JSON(
                    entry.getValue(), fullPath
            ).parseWithoutFilter());
        } else {
            sb.append("value = ");
            sb.append('"').append(entry.getValue()).append('"').append("\n\n");
        }
    }

    private void parseDeeper(Object entry) {
        String fullPath = parent;
        String path = "element";

        fullPath += path;
        sb.append("Element:\n").append("path = ").append(fullPath).append("\n");
        fullPath += ", ";

        if (entry instanceof ParseObject || entry instanceof ParseArray) {
            sb.append(new JSON(
                    entry, fullPath
            ).parse());
        } else {
            sb.append("value = ");
            sb.append('"').append(entry).append('"').append("\n\n");
        }
    }
}